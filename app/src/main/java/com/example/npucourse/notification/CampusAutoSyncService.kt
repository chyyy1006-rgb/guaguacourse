package com.example.npucourse.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Presentation
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.npucourse.MainActivity
import com.example.npucourse.R
import com.example.npucourse.data.AppDatabase
import com.example.npucourse.data.CampusServiceStore
import com.example.npucourse.data.CourseRepository
import com.example.npucourse.data.settings.SettingsRepository
import com.example.npucourse.importer.CourseImportMapper
import com.example.npucourse.importer.EduCourseRecord
import com.example.npucourse.importer.NwpuSemesterDetector
import com.example.npucourse.importer.NwpuSemesterInfo
import com.example.npucourse.importer.NwpuTimetableExtractor
import com.example.npucourse.ui.screens.ELECTRICITY_COLLECTION_SCRIPT
import com.example.npucourse.ui.screens.parseElectricityCollectionResult
import com.example.npucourse.widget.TodayScheduleWidgetUpdater
import java.util.ArrayDeque
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CampusAutoSyncService : JobService() {
    private enum class Target { ELECTRICITY, SCHEDULE }

    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val targets = ArrayDeque<Target>()
    private var running = false
    private var evaluating = false
    private var attempts = 0
    private var currentTarget: Target? = null
    private var collector: Runnable? = null
    private var webView: WebView? = null
    private var webHost: FrameLayout? = null
    private var webPresentation: Presentation? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var activeJob: JobParameters? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartJob(params: JobParameters): Boolean {
        if (running) return false

        activeJob = params
        running = true
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "GuaguaCourse:CampusAutoSync"
        ).apply { acquire(5L * 60L * 1000L) }

        val now = System.currentTimeMillis()
        if (now - CampusServiceStore.lastElectricitySync(this) >= ELECTRICITY_DUE_MILLIS &&
            !isElectricitySettlementWindow()
        ) {
            targets.add(Target.ELECTRICITY)
        }
        if (now - CampusServiceStore.lastScheduleSync(this) >= SCHEDULE_INTERVAL_MILLIS) {
            targets.add(Target.SCHEDULE)
        }

        if (targets.isEmpty()) {
            running = false
            activeJob = null
            releaseWakeLock()
            return false
        }

        return runCatching {
            startNextTarget()
            true
        }.getOrElse { error ->
            Log.e(TAG, "Unable to start campus auto sync job", error)
            running = false
            activeJob = null
            currentTarget = null
            targets.clear()
            destroyHeadlessWebView()
            releaseWakeLock()
            false
        }
    }

    override fun onStopJob(params: JobParameters): Boolean {
        val shouldRetry = currentTarget != null
        running = false
        activeJob = null
        currentTarget = null
        targets.clear()
        destroyHeadlessWebView()
        releaseWakeLock()
        return shouldRetry
    }

    override fun onDestroy() {
        running = false
        activeJob = null
        currentTarget = null
        targets.clear()
        destroyHeadlessWebView()
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startNextTarget() {
        destroyHeadlessWebView()
        val target = targets.pollFirst()
        if (target == null) {
            finishService()
            return
        }
        currentTarget = target
        attempts = 0
        evaluating = false
        runCatching {
            createHeadlessWebView(target)
        }.onFailure { error ->
            Log.e(TAG, "Unable to create background WebView for $target", error)
            completeTarget(failed = true)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createHeadlessWebView(target: Target) {
        prepareHeadlessDisplay()
        val context = webPresentation?.context ?: this
        val view = WebView(context)
        webView = view
        view.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            loadsImagesAutomatically = true
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = true
            userAgentString = userAgentString
                .replace("; wv", "")
                .replace(" Version/4.0", "")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) safeBrowsingEnabled = true
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(view, true)
            flush()
        }
        view.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                evaluating = false
            }

            override fun onPageFinished(view: WebView, url: String) {
                CookieManager.getInstance().flush()
                layoutHeadlessWebView()
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean = shouldBlockUrl(request.url.toString())

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
                shouldBlockUrl(url)
        }
        attachHeadlessWebView(view)
        view.onResume()
        view.resumeTimers()
        view.loadUrl(if (target == Target.ELECTRICITY) ELECTRICITY_SSO_URL else COURSE_TABLE_URL)
        startCollector()
    }

    private fun startCollector() {
        val task = object : Runnable {
            override fun run() {
                val view = webView ?: return
                if (!running || currentTarget == null) return
                if (++attempts > MAX_ATTEMPTS) {
                    completeTarget(authenticationRequired = isAuthenticationPage(view.url.orEmpty()))
                    return
                }
                if (isAuthenticationPage(view.url.orEmpty()) && attempts >= AUTH_WAIT_ATTEMPTS) {
                    showAuthenticationRequiredNotification()
                    completeTarget(authenticationRequired = true)
                    return
                }
                if (view.progress < 60 || evaluating) {
                    handler.postDelayed(this, POLL_INTERVAL_MILLIS)
                    return
                }
                when (currentTarget) {
                    Target.ELECTRICITY -> collectElectricity(view, this)
                    Target.SCHEDULE -> collectSchedule(view, this)
                    else -> Unit
                }
            }
        }
        collector = task
        handler.postDelayed(task, 700L)
    }

    private fun collectElectricity(view: WebView, task: Runnable) {
        evaluating = true
        view.evaluateJavascript(ELECTRICITY_COLLECTION_SCRIPT) { raw ->
            evaluating = false
            if (!running || currentTarget != Target.ELECTRICITY) return@evaluateJavascript
            val result = parseElectricityCollectionResult(raw)
            if (result.phase == "balance" && result.balance != null) {
                val balance = result.balance
                CampusServiceStore.saveElectricityBalance(this, balance)
                showElectricityBalanceNotification(
                    displayedBalance = balance,
                    moneyBalance = result.moneyBalance
                )
                completeTarget()
            } else {
                handler.postDelayed(task, POLL_INTERVAL_MILLIS)
            }
        }
    }

    private fun collectSchedule(view: WebView, task: Runnable) {
        val uri = runCatching { Uri.parse(view.url.orEmpty()) }.getOrNull()
        if (uri?.host?.lowercase() != "jwxt.nwpu.edu.cn" ||
            uri.path?.contains("/student/for-std/course-table") != true
        ) {
            handler.postDelayed(task, POLL_INTERVAL_MILLIS)
            return
        }

        evaluating = true
        NwpuSemesterDetector.detect(view) { semester ->
            NwpuTimetableExtractor.extract(view) { extraction ->
                evaluating = false
                val records = extraction.getOrNull().orEmpty()
                if (records.isEmpty()) {
                    handler.postDelayed(task, POLL_INTERVAL_MILLIS)
                } else {
                    saveSchedule(records, semester)
                }
            }
        }
    }

    private fun saveSchedule(records: List<EduCourseRecord>, detectedSemester: NwpuSemesterInfo?) {
        if (currentTarget != Target.SCHEDULE) return
        evaluating = true
        serviceScope.launch {
            val success = runCatching {
                val database = AppDatabase.getInstance(this@CampusAutoSyncService)
                val semesters = database.semesterDao().getAllSemestersOnce()
                val normalizedLabel = detectedSemester?.label.orEmpty().replace(" ", "")
                val semester = semesters.firstOrNull {
                    it.name.replace(" ", "").equals(normalizedLabel, ignoreCase = true)
                } ?: semesters.firstOrNull() ?: error("没有可同步的本地学期")

                val mapped = CourseImportMapper.map(records, semester.campus)
                if (mapped.courses.isEmpty()) error("教务课表没有可导入课程")

                val repository = CourseRepository(
                    dao = database.courseDao(),
                    taskDao = database.taskDao(),
                    database = database
                )
                val previousSuccessfulSync =
                    CampusServiceStore.lastScheduleSync(this@CampusAutoSyncService)
                val trackedCourseIds = CampusServiceStore.trackedCourseIds(
                    this@CampusAutoSyncService,
                    semester.id
                )
                val existingCourses = database.courseDao().getCoursesForSemester(semester.id)
                val previousSchedule = existingCourses
                    .filter { trackedCourseIds.isEmpty() || it.id in trackedCourseIds }
                    .map(::scheduleSnapshotOf)
                val refreshedSchedule = mapped.courses.map(::scheduleSnapshotOf)
                val scheduleChanges = detectScheduleChanges(previousSchedule, refreshedSchedule)

                val trackedIds = repository.syncSemesterCoursesInBackground(
                    semesterId = semester.id,
                    courses = mapped.courses,
                    trackedCourseIds = trackedCourseIds
                )
                CampusServiceStore.setTrackedCourseIds(
                    this@CampusAutoSyncService,
                    semester.id,
                    trackedIds
                )
                CampusServiceStore.markScheduleSynced(this@CampusAutoSyncService)

                if (previousSuccessfulSync > 0L && scheduleChanges.hasChanges) {
                    showScheduleChangeNotification(scheduleChanges)
                }

                val settings = SettingsRepository(this@CampusAutoSyncService).settings.first()
                val refreshedCourses = repository.courses.first().filter { it.semesterId == semester.id }
                CourseAlarmScheduler.rescheduleAll(
                    context = this@CampusAutoSyncService,
                    courses = refreshedCourses,
                    semesterStartMillis = semester.startMillis,
                    campus = semester.campus,
                    reminderMinutes = settings.reminderMinutes
                )
                TodayScheduleWidgetUpdater.updateAll(this@CampusAutoSyncService)
            }.isSuccess

            withContext(Dispatchers.Main) {
                evaluating = false
                completeTarget(failed = !success)
            }
        }
    }

    private fun completeTarget(
        failed: Boolean = false,
        authenticationRequired: Boolean = false
    ) {
        if (!running) return
        if (failed && authenticationRequired) {
            showAuthenticationRequiredNotification()
        } else if (!failed) {
            getSystemService(NotificationManager::class.java).cancel(AUTH_NOTIFICATION_ID)
        }
        handler.post { startNextTarget() }
    }

    private fun shouldBlockUrl(rawUrl: String): Boolean {
        val uri = runCatching { Uri.parse(rawUrl) }.getOrNull() ?: return true
        val host = uri.host?.lowercase()
        return uri.scheme?.lowercase() != "https" ||
            !(host == "nwpu.edu.cn" || host?.endsWith(".nwpu.edu.cn") == true)
    }

    private fun isAuthenticationPage(rawUrl: String): Boolean {
        val uri = runCatching { Uri.parse(rawUrl) }.getOrNull() ?: return false
        val host = uri.host?.lowercase().orEmpty()
        return host == "uis.nwpu.edu.cn" ||
            host == "authserver.nwpu.edu.cn"
    }

    private fun showElectricityBalanceNotification(
        displayedBalance: Double,
        moneyBalance: Double?
    ) {
        val alertValue = moneyBalance ?: displayedBalance
        val low = alertValue <= LOW_BALANCE_THRESHOLD
        if (hasNotificationPermission()) {
            val valueText = if (moneyBalance != null) {
                "当前电费余额 ${formatNumber(moneyBalance)} 元"
            } else {
                "当前剩余电量 ${formatNumber(displayedBalance)} 度"
            }
            val text = if (low) "$valueText，请及时充值" else valueText
            val notification = NotificationCompat.Builder(this, ELECTRICITY_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(if (low) "宿舍电费余额不足" else "宿舍电费已刷新")
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(
                    if (low) NotificationCompat.PRIORITY_HIGH
                    else NotificationCompat.PRIORITY_DEFAULT
                )
                .setAutoCancel(true)
                .setContentIntent(openServicesPendingIntent())
                .build()
            getSystemService(NotificationManager::class.java)
                .notify(ELECTRICITY_NOTIFICATION_ID, notification)
        }
        CampusServiceStore.setElectricityAlertActive(this, low)
    }

    private fun showScheduleChangeNotification(changes: ScheduleChangeSummary) {
        if (!hasNotificationPermission()) return
        val detail = changes.description()
        val notification = NotificationCompat.Builder(this, SCHEDULE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("课程表有变化")
            .setContentText(detail)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openServicesPendingIntent())
            .build()
        getSystemService(NotificationManager::class.java)
            .notify(SCHEDULE_NOTIFICATION_ID, notification)
    }

    private fun showAuthenticationRequiredNotification() {
        val now = System.currentTimeMillis()
        if (now - CampusServiceStore.lastAuthenticationNotice(this) < AUTH_NOTICE_INTERVAL_MILLIS ||
            !hasNotificationPermission()
        ) return
        CampusServiceStore.markAuthenticationNotice(this, now)
        val notification = NotificationCompat.Builder(this, AUTH_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("校园服务需要重新登录")
            .setContentText("统一身份认证已失效，请打开应用完成登录后恢复自动刷新")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openServicesPendingIntent())
            .build()
        getSystemService(NotificationManager::class.java).notify(AUTH_NOTIFICATION_ID, notification)
    }

    private fun openServicesPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_campus_services", true)
        }
        return PendingIntent.getActivity(
            this,
            52_031,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                ELECTRICITY_CHANNEL_ID,
                "电费余额提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "每次自动刷新后告知宿舍电费余额，余额不足时提醒充值"
                enableVibration(true)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                SCHEDULE_CHANNEL_ID,
                "课程表变化提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "自动刷新发现课程安排变化时提醒"
                enableVibration(true)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                AUTH_CHANNEL_ID,
                "校园服务登录状态",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "后台刷新需要重新完成学校认证时提醒"
            }
        )
    }

    private fun finishService() {
        running = false
        currentTarget = null
        targets.clear()
        destroyHeadlessWebView()
        releaseWakeLock()
        activeJob?.let { jobFinished(it, false) }
        activeJob = null
    }

    private fun prepareHeadlessDisplay() {
        val metrics = resources.displayMetrics
        val width = maxOf(360, metrics.widthPixels)
        val height = maxOf(640, metrics.heightPixels)
        val density = maxOf(160, metrics.densityDpi)
        runCatching {
            val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            imageReader = reader
            reader.setOnImageAvailableListener({ source ->
                var image: Image? = null
                try {
                    image = source.acquireLatestImage()
                } finally {
                    image?.close()
                }
            }, handler)
            val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val display = displayManager.createVirtualDisplay(
                "GuaguaCampusBackground",
                width,
                height,
                density,
                reader.surface,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
            ) ?: error("无法创建后台显示")
            virtualDisplay = display
            val presentation = Presentation(this, display.display)
            presentation.setCancelable(false)
            webPresentation = presentation
        }.onFailure {
            releaseHeadlessDisplay()
        }
    }

    private fun attachHeadlessWebView(view: WebView) {
        val presentation = webPresentation
        if (presentation != null) {
            runCatching {
                val host = FrameLayout(presentation.context)
                webHost = host
                host.addView(
                    view,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                presentation.setContentView(host)
                presentation.show()
                layoutHeadlessWebView()
                return
            }
        }

        val host = FrameLayout(this)
        webHost = host
        host.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        layoutHeadlessWebView()
    }

    private fun layoutHeadlessWebView() {
        val view = webView ?: return
        val host = webHost ?: return
        val metrics = resources.displayMetrics
        val width = maxOf(360, metrics.widthPixels)
        val height = maxOf(640, metrics.heightPixels)
        val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        host.measure(widthSpec, heightSpec)
        host.layout(0, 0, width, height)
        view.measure(widthSpec, heightSpec)
        view.layout(0, 0, width, height)
    }

    private fun destroyHeadlessWebView() {
        collector?.let(handler::removeCallbacks)
        collector = null
        evaluating = false
        webView?.apply {
            onPause()
            stopLoading()
            if (parent is ViewGroup) (parent as ViewGroup).removeView(this)
            destroy()
        }
        webView = null
        webHost = null
        releaseHeadlessDisplay()
    }

    private fun releaseHeadlessDisplay() {
        runCatching { webPresentation?.dismiss() }
        webPresentation = null
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    private fun isElectricitySettlementWindow(): Boolean =
        Calendar.getInstance().get(Calendar.HOUR_OF_DAY) == 0

    private fun formatNumber(value: Double): String =
        String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')

    companion object {
        private const val TAG = "CampusAutoSync"
        private const val ELECTRICITY_SSO_URL =
            "https://yktapp.nwpu.edu.cn/berserker-auth/cas/login/supwisdom?targetUrl=https%3A%2F%2Fyktapp.nwpu.edu.cn%2Fplat"
        private const val COURSE_TABLE_URL =
            "https://jwxt.nwpu.edu.cn/student/for-std/course-table"
        // JobScheduler 的 30 分钟任务可能在最后 5 分钟弹性窗口内运行；
        // 到期阈值与该窗口对齐，避免轻微提前导致整轮跳过、实际变成约 60 分钟一次。
        private const val ELECTRICITY_DUE_MILLIS = 25L * 60L * 1000L
        private const val SCHEDULE_INTERVAL_MILLIS = 24L * 60L * 60L * 1000L
        private const val POLL_INTERVAL_MILLIS = 1_000L
        private const val MAX_ATTEMPTS = 120
        private const val AUTH_WAIT_ATTEMPTS = 15
        private const val AUTH_NOTICE_INTERVAL_MILLIS = 12L * 60L * 60L * 1000L
        private const val LOW_BALANCE_THRESHOLD = 5.0
        private const val ELECTRICITY_CHANNEL_ID = "electricity_balance_alerts"
        private const val SCHEDULE_CHANNEL_ID = "schedule_change_alerts"
        private const val AUTH_CHANNEL_ID = "campus_authentication"
        private const val ELECTRICITY_NOTIFICATION_ID = 52_031
        private const val AUTH_NOTIFICATION_ID = 52_032
        private const val SCHEDULE_NOTIFICATION_ID = 52_033
    }
}

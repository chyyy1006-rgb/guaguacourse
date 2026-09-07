package com.example.npucourse.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Presentation
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
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

class CampusAutoSyncService : Service() {
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

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startAsForeground("正在准备校园服务自动刷新")
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "GuaguaCourse:CampusAutoSync"
        ).apply { acquire(5L * 60L * 1000L) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (running) return START_NOT_STICKY
        running = true

        val now = System.currentTimeMillis()
        if (now - CampusServiceStore.lastElectricitySync(this) >= ELECTRICITY_INTERVAL_MILLIS &&
            !isElectricitySettlementWindow()
        ) {
            targets.add(Target.ELECTRICITY)
        }
        if (now - CampusServiceStore.lastScheduleSync(this) >= SCHEDULE_INTERVAL_MILLIS) {
            targets.add(Target.SCHEDULE)
        }

        startNextTarget()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        running = false
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
        if (target == Target.SCHEDULE) {
            // 课程表无论成功与否都只在约 24 小时后再次尝试。
            CampusServiceStore.markScheduleSynced(this)
        }
        attempts = 0
        evaluating = false
        updateForeground(
            if (target == Target.ELECTRICITY) "正在刷新宿舍电费" else "正在刷新课程表"
        )
        createHeadlessWebView(target)
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
                updateLowElectricityNotification(
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
                val trackedIds = repository.syncSemesterCoursesInBackground(
                    semesterId = semester.id,
                    courses = mapped.courses,
                    trackedCourseIds = CampusServiceStore.trackedCourseIds(
                        this@CampusAutoSyncService,
                        semester.id
                    )
                )
                CampusServiceStore.setTrackedCourseIds(
                    this@CampusAutoSyncService,
                    semester.id,
                    trackedIds
                )
                CampusServiceStore.markScheduleSynced(this@CampusAutoSyncService)

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
        if (failed && authenticationRequired) showAuthenticationRequiredNotification()
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

    private fun updateLowElectricityNotification(
        displayedBalance: Double,
        moneyBalance: Double?
    ) {
        val alertValue = moneyBalance ?: displayedBalance
        val low = alertValue <= LOW_BALANCE_THRESHOLD
        val wasActive = CampusServiceStore.electricityAlertActive(this)
        if (low && !wasActive && hasNotificationPermission()) {
            val text = if (moneyBalance != null) {
                "电费余额剩余 ${formatNumber(moneyBalance)} 元，请及时充值"
            } else {
                "学校仅返回剩余电量 ${formatNumber(displayedBalance)} 度，数值已不高于 5，请及时充值"
            }
            val notification = NotificationCompat.Builder(this, ELECTRICITY_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("宿舍电费余额不足")
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(openServicesPendingIntent())
                .build()
            getSystemService(NotificationManager::class.java)
                .notify(ELECTRICITY_NOTIFICATION_ID, notification)
        }
        CampusServiceStore.setElectricityAlertActive(this, low)
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
                SYNC_CHANNEL_ID,
                "校园服务后台刷新",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "课程表和宿舍电费后台刷新期间显示状态"
                setShowBadge(false)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                ELECTRICITY_CHANNEL_ID,
                "电费余额提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "宿舍电费剩余值不高于 5 时提醒充值"
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

    private fun foregroundNotification(status: String): Notification =
        NotificationCompat.Builder(this, SYNC_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("瓜瓜课程表")
            .setContentText(status)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setContentIntent(openServicesPendingIntent())
            .build()

    private fun startAsForeground(status: String) {
        val notification = foregroundNotification(status)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                SYNC_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(SYNC_NOTIFICATION_ID, notification)
        }
    }

    private fun updateForeground(status: String) {
        getSystemService(NotificationManager::class.java)
            .notify(SYNC_NOTIFICATION_ID, foregroundNotification(status))
    }

    private fun finishService() {
        running = false
        destroyHeadlessWebView()
        releaseWakeLock()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
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
        private const val ELECTRICITY_SSO_URL =
            "https://yktapp.nwpu.edu.cn/berserker-auth/cas/login/supwisdom?targetUrl=https%3A%2F%2Fyktapp.nwpu.edu.cn%2Fplat"
        private const val COURSE_TABLE_URL =
            "https://jwxt.nwpu.edu.cn/student/for-std/course-table"
        private const val ELECTRICITY_INTERVAL_MILLIS = 30L * 60L * 1000L
        private const val SCHEDULE_INTERVAL_MILLIS = 24L * 60L * 60L * 1000L
        private const val POLL_INTERVAL_MILLIS = 1_000L
        private const val MAX_ATTEMPTS = 120
        private const val AUTH_WAIT_ATTEMPTS = 15
        private const val AUTH_NOTICE_INTERVAL_MILLIS = 12L * 60L * 60L * 1000L
        private const val LOW_BALANCE_THRESHOLD = 5.0
        private const val SYNC_CHANNEL_ID = "campus_auto_sync"
        private const val ELECTRICITY_CHANNEL_ID = "electricity_balance_alerts"
        private const val AUTH_CHANNEL_ID = "campus_authentication"
        private const val SYNC_NOTIFICATION_ID = 52_030
        private const val ELECTRICITY_NOTIFICATION_ID = 52_031
        private const val AUTH_NOTIFICATION_ID = 52_032
    }
}

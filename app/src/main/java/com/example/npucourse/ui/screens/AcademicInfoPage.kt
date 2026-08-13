package com.example.npucourse.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.npucourse.data.academic.AcademicAnalytics
import com.example.npucourse.data.academic.AcademicCacheStore
import com.example.npucourse.data.academic.AcademicPreferencesStore
import com.example.npucourse.data.academic.AcademicTimeParser
import com.example.npucourse.importer.NwpuAcademicDirectClient
import com.example.npucourse.importer.NwpuAcademicExtractor
import com.example.npucourse.importer.NwpuAcademicJavascriptBridge
import com.example.npucourse.importer.NwpuExamQueryResult
import com.example.npucourse.importer.NwpuExamRecord
import com.example.npucourse.importer.NwpuGradeQueryResult
import com.example.npucourse.importer.NwpuGradeRecord
import com.example.npucourse.notification.AcademicNotificationHelper
import com.example.npucourse.notification.AcademicSyncScheduler
import com.example.npucourse.notification.ExamAlarmScheduler
import com.example.npucourse.sharing.AcademicExportManager
import com.example.npucourse.widget.AcademicOverviewWidgetUpdater
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val ACADEMIC_PORTAL_URL = "https://ecampus.nwpu.edu.cn/"
private const val ACADEMIC_HOME_URL = "https://jwxt.nwpu.edu.cn/student/home"
private const val ACADEMIC_COOKIE_URL = "https://jwxt.nwpu.edu.cn"

private enum class AcademicInfoTab { GRADES, EXAMS, ANALYTICS }

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AcademicInfoPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val webViewHolder = remember { arrayOfNulls<WebView>(1) }

    var selectedTab by remember { mutableStateOf(AcademicInfoTab.GRADES) }
    var currentUrl by remember { mutableStateOf(ACADEMIC_PORTAL_URL) }
    var loading by remember { mutableStateOf(false) }
    var querying by remember { mutableStateOf(false) }
    var academicOpened by remember { mutableStateOf(false) }
    var showAcademicWebView by remember { mutableStateOf(false) }
    var collapseWebViewAfterLogin by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var pageError by remember { mutableStateOf<String?>(null) }

    val cachedGradesAtStart = remember { AcademicCacheStore.loadGrades(context) }
    val cachedExamsAtStart = remember { AcademicCacheStore.loadExams(context) }
    var gradeResult by remember { mutableStateOf<NwpuGradeQueryResult?>(cachedGradesAtStart?.result) }
    var examResult by remember { mutableStateOf<NwpuExamQueryResult?>(cachedExamsAtStart?.result) }
    var gradeUpdatedAt by remember { mutableLongStateOf(cachedGradesAtStart?.updatedAt ?: 0L) }
    var examUpdatedAt by remember { mutableLongStateOf(cachedExamsAtStart?.updatedAt ?: 0L) }
    var selectedSemesterId by remember {
        mutableLongStateOf(
            cachedGradesAtStart?.result?.semesters?.firstOrNull { semester ->
                cachedGradesAtStart.result.grades.any { it.semesterId == semester.id }
            }?.id ?: cachedGradesAtStart?.result?.semesters?.firstOrNull()?.id ?: 0L
        )
    }
    var showFinishedExams by remember { mutableStateOf(false) }

    val storedPrefs = remember { AcademicPreferencesStore.get(context) }
    var examRemindersEnabled by remember { mutableStateOf(storedPrefs.examRemindersEnabled) }
    var gradeNotificationsEnabled by remember { mutableStateOf(storedPrefs.gradeNotificationsEnabled) }
    var backgroundSyncEnabled by remember { mutableStateOf(storedPrefs.backgroundSyncEnabled) }
    var maskExportEnabled by remember { mutableStateOf(storedPrefs.maskExportEnabled) }
    var showAcademicSettings by remember { mutableStateOf(false) }

    var pendingGalleryBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingGalleryName by remember { mutableStateOf("") }
    val legacyStoragePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val bitmap = pendingGalleryBitmap
        if (granted && bitmap != null) {
            AcademicExportManager.saveBitmapToGallery(context, bitmap, pendingGalleryName)
                .onSuccess { statusText = "图片已保存到相册 GuaguaCourse" }
                .onFailure { statusText = "保存失败：${it.message ?: "未知错误"}" }
        } else if (!granted) {
            statusText = "未获得存储权限，无法保存到相册"
        }
        pendingGalleryBitmap = null
    }

    fun afterGradeQuery(result: NwpuGradeQueryResult) {
        gradeResult = result
        selectedSemesterId = result.semesters.firstOrNull { semester ->
            result.grades.any { it.semesterId == semester.id }
        }?.id ?: result.semesters.firstOrNull()?.id ?: 0L
        val now = System.currentTimeMillis()
        val changes = AcademicCacheStore.saveGrades(context, result, now)
        gradeUpdatedAt = now
        if (gradeNotificationsEnabled) AcademicNotificationHelper.showGradeChanges(context, changes)
        AcademicOverviewWidgetUpdater.updateAll(context)
        statusText = if (result.warnings.isNotEmpty()) "部分数据可能未完整读取" else null
    }

    fun afterExamQuery(result: NwpuExamQueryResult) {
        examResult = result
        val now = System.currentTimeMillis()
        AcademicCacheStore.saveExams(context, result, now)
        examUpdatedAt = now
        ExamAlarmScheduler.rescheduleAll(context, result.exams, examRemindersEnabled)
        AcademicOverviewWidgetUpdater.updateAll(context)
        statusText = null
    }

    val bridge = remember {
        NwpuAcademicJavascriptBridge(
            onGradesPayload = { json ->
                NwpuAcademicExtractor.parseGradePayload(json)
                    .onSuccess { afterGradeQuery(it) }
                    .onFailure { statusText = "成绩查询失败：${it.message ?: "未知错误"}" }
                querying = false
            },
            onExamsPayload = { json ->
                NwpuAcademicExtractor.parseExamPayload(json)
                    .onSuccess { afterExamQuery(it) }
                    .onFailure { statusText = "考试查询失败：${it.message ?: "未知错误"}" }
                querying = false
            }
        )
    }

    fun fallbackToWebView(tab: AcademicInfoTab) {
        val webView = webViewHolder[0]
        if (webView == null || !isAcademicQueryPage(currentUrl)) {
            querying = false
            statusText = "查询失败，请先登录翱翔教务后重试"
            return
        }
        statusText = "正在查询……"
        when (tab) {
            AcademicInfoTab.GRADES, AcademicInfoTab.ANALYTICS -> NwpuAcademicExtractor.queryGrades(webView)
            AcademicInfoTab.EXAMS -> NwpuAcademicExtractor.queryExams(webView)
        }
    }

    fun runQuery(tab: AcademicInfoTab) {
        if (querying) return
        val cookie = CookieManager.getInstance().getCookie(ACADEMIC_COOKIE_URL).orEmpty()
        val userAgent = webViewHolder[0]?.settings?.userAgentString.orEmpty()
        querying = true
        statusText = when (tab) {
            AcademicInfoTab.GRADES, AcademicInfoTab.ANALYTICS -> "正在读取成绩……"
            AcademicInfoTab.EXAMS -> "正在读取考试安排……"
        }
        scope.launch {
            when (tab) {
                AcademicInfoTab.GRADES, AcademicInfoTab.ANALYTICS -> {
                    val result = withContext(Dispatchers.IO) { NwpuAcademicDirectClient.queryGrades(cookie, userAgent) }
                    result.onSuccess { afterGradeQuery(it); querying = false }
                        .onFailure { fallbackToWebView(tab) }
                }
                AcademicInfoTab.EXAMS -> {
                    val result = withContext(Dispatchers.IO) { NwpuAcademicDirectClient.queryExams(cookie, userAgent) }
                    result.onSuccess { afterExamQuery(it); querying = false }
                        .onFailure { fallbackToWebView(tab) }
                }
            }
        }
    }

    fun exportCurrent(asShare: Boolean) {
        val tab = selectedTab
        scope.launch {
            val bitmapAndName = withContext(Dispatchers.Default) {
                when (tab) {
                    AcademicInfoTab.GRADES -> {
                        val result = gradeResult ?: return@withContext null
                        AcademicExportManager.renderGradesBitmap(result, selectedSemesterId, maskExportEnabled) to
                            "GuaguaCourse_成绩_${System.currentTimeMillis()}.png"
                    }
                    AcademicInfoTab.EXAMS -> {
                        val result = examResult ?: return@withContext null
                        AcademicExportManager.renderExamsBitmap(result.exams, showFinishedExams) to
                            "GuaguaCourse_考试_${System.currentTimeMillis()}.png"
                    }
                    AcademicInfoTab.ANALYTICS -> {
                        val result = gradeResult ?: return@withContext null
                        AcademicExportManager.renderAnalyticsBitmap(result, maskExportEnabled) to
                            "GuaguaCourse_学业分析_${System.currentTimeMillis()}.png"
                    }
                }
            }
            val pair = bitmapAndName ?: run {
                statusText = "当前没有可导出的数据"
                return@launch
            }
            val (bitmap, name) = pair
            if (asShare) {
                AcademicExportManager.shareBitmap(
                    context,
                    bitmap,
                    name,
                    when (tab) {
                        AcademicInfoTab.GRADES -> "分享成绩图片"
                        AcademicInfoTab.EXAMS -> "分享考试安排"
                        AcademicInfoTab.ANALYTICS -> "分享学业分析"
                    }
                )
                statusText = "已生成分享图片"
            } else {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                ) {
                    pendingGalleryBitmap = bitmap
                    pendingGalleryName = name
                    legacyStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else {
                    AcademicExportManager.saveBitmapToGallery(context, bitmap, name)
                        .onSuccess { statusText = "图片已保存到相册 GuaguaCourse" }
                        .onFailure { statusText = "保存失败：${it.message ?: "未知错误"}" }
                }
            }
        }
    }

    BackHandler {
        val webView = webViewHolder[0]
        when {
            showAcademicWebView && webView != null && webView.canGoBack() -> webView.goBack()
            showAcademicWebView -> {
                showAcademicWebView = false
                collapseWebViewAfterLogin = false
                pageError = null
            }
            else -> onBack()
        }
    }

    LaunchedEffect(backgroundSyncEnabled) {
        AcademicSyncScheduler.schedule(context, backgroundSyncEnabled)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onBack) { Text("← 返回") }
            Column(Modifier.weight(1f)) {
                Text("考试与成绩", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    "成绩、考试安排与学业分析",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selectedTab == AcademicInfoTab.GRADES, { selectedTab = AcademicInfoTab.GRADES }, { Text("成绩") })
            FilterChip(selectedTab == AcademicInfoTab.EXAMS, { selectedTab = AcademicInfoTab.EXAMS }, { Text("考试") })
            FilterChip(selectedTab == AcademicInfoTab.ANALYTICS, { selectedTab = AcademicInfoTab.ANALYTICS }, { Text("学业分析") })
        }

        if ((loading && showAcademicWebView) || querying) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        if (showAcademicWebView) {
            pageError?.let {
                Spacer(Modifier.height(6.dp)); Text("页面异常：$it", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }
        }
        statusText?.let {
            Spacer(Modifier.height(6.dp)); Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(8.dp))
        QueryActionBar(
            querying = querying,
            hasData = when (selectedTab) {
                AcademicInfoTab.GRADES, AcademicInfoTab.ANALYTICS -> gradeResult != null
                AcademicInfoTab.EXAMS -> examResult != null
            },
            academicOpened = academicOpened,
            onQuery = { runQuery(selectedTab) },
            onOpenAcademic = {
                showAcademicWebView = true
                pageError = null
                if (academicOpened) {
                    collapseWebViewAfterLogin = false
                    statusText = null
                    webViewHolder[0]?.loadUrl(ACADEMIC_HOME_URL)
                } else {
                    collapseWebViewAfterLogin = true
                    statusText = "请完成教务登录"
                    openAcademicForQuery(webViewHolder[0]) { result ->
                        if (result != "CLICKED") webViewHolder[0]?.loadUrl(ACADEMIC_HOME_URL)
                    }
                }
            },
            onShare = { exportCurrent(true) },
            onSave = { exportCurrent(false) },
            onSettings = { showAcademicSettings = true }
        )

        Spacer(Modifier.height(8.dp))
        when (selectedTab) {
            AcademicInfoTab.GRADES -> GradeResultPane(
                result = gradeResult,
                selectedSemesterId = selectedSemesterId,
                onSemesterSelected = { selectedSemesterId = it },
                updatedAt = gradeUpdatedAt,
                modifier = Modifier.weight(1f)
            )
            AcademicInfoTab.EXAMS -> ExamResultPane(
                result = examResult,
                showFinished = showFinishedExams,
                onShowFinishedChanged = { showFinishedExams = it },
                updatedAt = examUpdatedAt,
                modifier = Modifier.weight(1f)
            )
            AcademicInfoTab.ANALYTICS -> AcademicAnalyticsPane(
                result = gradeResult,
                modifier = Modifier.weight(1f)
            )
        }
        if (showAcademicWebView) Spacer(Modifier.height(8.dp))
        AcademicWebView(
            modifier = if (showAcademicWebView) {
                Modifier.fillMaxWidth().height(300.dp)
            } else {
                Modifier.size(1.dp)
            },
            bridge = bridge,
            onWebViewCreated = { webViewHolder[0] = it },
            onPageStarted = { currentUrl = it; loading = true; pageError = null },
            onPageFinished = { _, url ->
                currentUrl = url
                loading = false
                CookieManager.getInstance().flush()
                if (isAcademicQueryPage(url)) {
                    academicOpened = true
                    statusText = null
                    if (collapseWebViewAfterLogin) {
                        showAcademicWebView = false
                        collapseWebViewAfterLogin = false
                    }
                }
            },
            onPageError = { loading = false; pageError = it }
        )
    }

    if (showAcademicSettings) {
        AcademicOptionsDialog(
            examRemindersEnabled = examRemindersEnabled,
            gradeNotificationsEnabled = gradeNotificationsEnabled,
            backgroundSyncEnabled = backgroundSyncEnabled,
            maskExportEnabled = maskExportEnabled,
            onDismiss = { showAcademicSettings = false },
            onExamReminders = {
                examRemindersEnabled = it
                AcademicPreferencesStore.setExamReminders(context, it)
                ExamAlarmScheduler.rescheduleAll(context, examResult?.exams.orEmpty(), it)
            },
            onGradeNotifications = {
                gradeNotificationsEnabled = it
                AcademicPreferencesStore.setGradeNotifications(context, it)
            },
            onBackgroundSync = {
                backgroundSyncEnabled = it
                AcademicPreferencesStore.setBackgroundSync(context, it)
            },
            onMaskExport = {
                maskExportEnabled = it
                AcademicPreferencesStore.setMaskExport(context, it)
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            CookieManager.getInstance().flush()
            webViewHolder[0]?.apply {
                stopLoading(); removeJavascriptInterface(NwpuAcademicExtractor.BRIDGE_NAME); removeAllViews(); destroy()
            }
            webViewHolder[0] = null
        }
    }
}

@Composable
private fun QueryActionBar(
    querying: Boolean,
    hasData: Boolean,
    academicOpened: Boolean,
    onQuery: () -> Unit,
    onOpenAcademic: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onSettings: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onQuery, modifier = Modifier.weight(1f), enabled = !querying) {
                Text(if (hasData) "刷新" else "查询")
            }
            OutlinedButton(onClick = onOpenAcademic) {
                Text(if (academicOpened) "教务首页" else "登录教务")
            }
            OutlinedButton(onClick = onSettings) { Text("设置") }
        }
        if (hasData) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) { Text("分享图片") }
                OutlinedButton(onClick = onSave, modifier = Modifier.weight(1f)) { Text("保存相册") }
            }
        }
    }
}

@Composable
private fun GradeResultPane(
    result: NwpuGradeQueryResult?,
    selectedSemesterId: Long,
    onSemesterSelected: (Long) -> Unit,
    updatedAt: Long,
    modifier: Modifier = Modifier
) {
    if (result == null) {
        EmptyAcademicCard("暂无成绩", "请登录翱翔教务后查询成绩。", modifier)
        return
    }
    val rows = result.grades.filter { selectedSemesterId == 0L || it.semesterId == selectedSemesterId }
    Column(modifier) {
        if (result.semesters.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                result.semesters.forEach { semester ->
                    FilterChip(
                        selected = selectedSemesterId == semester.id,
                        onClick = { onSemesterSelected(semester.id) },
                        label = { Text(semester.name) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        GradeSummaryCard(result, rows, updatedAt)
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (rows.isEmpty()) {
                item { EmptyAcademicCard("当前学期暂无成绩", "可能尚未发布，或该学期没有可读取记录。") }
            } else {
                items(rows, key = { "${it.semesterId}-${it.courseId}-${it.courseCode}-${it.courseName}" }) { GradeCard(it) }
            }
            if (result.warnings.isNotEmpty()) item { AcademicWarningCard(result.warnings) }
        }
    }
}

@Composable
private fun GradeSummaryCard(result: NwpuGradeQueryResult, rows: List<NwpuGradeRecord>, updatedAt: Long) {
    val credits = rows.mapNotNull { it.credits }.sum()
    val semesterGpa = AcademicAnalytics.overview(
        result.copy(grades = rows, semesters = result.semesters.filter { s -> rows.any { it.semesterId == s.id } })
    ).stats.firstOrNull()?.gpa
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("GPA", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatAcademicNumber(semesterGpa ?: result.gpa ?: 0.0), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("已显示学分", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatAcademicNumber(credits), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(6.dp))
            val rankText = result.rank?.let { " · GPA 排名 $it" }.orEmpty()
            Text("${rows.size} 门课程$rankText · ${updatedAtText(updatedAt)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GradeCard(grade: NwpuGradeRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(grade.courseName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                val detail = buildString {
                    grade.credits?.let { append("${formatAcademicNumber(it)} 学分") }
                    grade.gradePoint?.let { if (isNotEmpty()) append(" · "); append("绩点 ${formatAcademicNumber(it)}") }
                    grade.classRank?.takeIf { it.isNotBlank() }?.let { if (isNotEmpty()) append(" · "); append("教学班 $it") }
                    grade.obligatory?.let { if (isNotEmpty()) append(" · "); append(if (it) "必修" else "非必修") }
                }
                if (detail.isNotBlank()) Text(detail, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(grade.grade, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ExamResultPane(
    result: NwpuExamQueryResult?,
    showFinished: Boolean,
    onShowFinishedChanged: (Boolean) -> Unit,
    updatedAt: Long,
    modifier: Modifier = Modifier
) {
    if (result == null) {
        EmptyAcademicCard("暂无考试安排", "请登录翱翔教务后查询考试安排。", modifier)
        return
    }
    val rows = result.exams.filter { showFinished || !it.finished }
        .sortedWith(compareBy<NwpuExamRecord> { AcademicTimeParser.parseStartMillis(it.timeText) == null }
            .thenBy { AcademicTimeParser.parseStartMillis(it.timeText) ?: Long.MAX_VALUE })
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("${result.exams.count { !it.finished }} 场未结束", fontWeight = FontWeight.SemiBold)
                Text(updatedAtText(updatedAt), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilterChip(showFinished, { onShowFinishedChanged(!showFinished) }, { Text(if (showFinished) "含已结束" else "仅未结束") })
        }
        Spacer(Modifier.height(7.dp))
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (rows.isEmpty()) item { EmptyAcademicCard("暂无考试安排", "若学校尚未发布排考，这是正常情况。") }
            else items(rows, key = { "${it.courseName}-${it.timeText}-${it.location}" }) { ExamCard(it) }
            if (result.warnings.isNotEmpty()) item { AcademicWarningCard(result.warnings) }
        }
    }
}

@Composable
private fun ExamCard(exam: NwpuExamRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Text(exam.courseName.ifBlank { "未命名考试" }, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                val countdown = if (exam.finished) "已结束" else AcademicTimeParser.countdownText(exam.timeText).orEmpty()
                if (countdown.isNotBlank()) Text(countdown, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (exam.finished) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(6.dp))
            Text(exam.timeText.ifBlank { "时间待定" }, fontSize = 13.sp)
            if (exam.location.isNotBlank()) Text(exam.location, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (exam.status.isNotBlank()) Text(exam.status, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun AcademicAnalyticsPane(result: NwpuGradeQueryResult?, modifier: Modifier = Modifier) {
    if (result == null) {
        EmptyAcademicCard("暂无学业分析", "先查询成绩后即可生成 GPA 趋势、学分和未通过课程统计。", modifier)
        return
    }
    val overview = AcademicAnalytics.overview(result)
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnalyticsMetricCard("累计学分", formatAcademicNumber(overview.totalCredits), Modifier.weight(1f))
                AnalyticsMetricCard("通过学分", formatAcademicNumber(overview.passedCredits), Modifier.weight(1f))
                AnalyticsMetricCard("未通过", overview.failedCourseCount.toString(), Modifier.weight(1f))
            }
        }
        item { GpaTrendCard(overview.stats) }
        items(overview.stats.reversed(), key = { it.semesterId }) { stat -> SemesterStatCard(stat) }
    }
}

@Composable
private fun AnalyticsMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(15.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GpaTrendCard(stats: List<AcademicAnalytics.SemesterStat>) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(15.dp)) {
            Text("GPA 趋势", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(5.dp))
            val points = stats.mapNotNull { stat -> stat.gpa?.let { stat.semesterName to it } }
            if (points.size < 2) {
                Text("至少需要两个学期的绩点数据才能绘制趋势。", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val lineColor = MaterialTheme.colorScheme.primary
                Canvas(Modifier.fillMaxWidth().height(145.dp)) {
                    val left = 12.dp.toPx(); val right = size.width - 12.dp.toPx(); val top = 14.dp.toPx(); val bottom = size.height - 22.dp.toPx()
                    val values = points.map { it.second }
                    val minV = (values.minOrNull() ?: 0.0).coerceAtMost(2.0)
                    val maxV = (values.maxOrNull() ?: 4.0).coerceAtLeast(minV + 0.5)
                    val path = Path()
                    points.forEachIndexed { index, pair ->
                        val x = if (points.size == 1) left else left + (right - left) * index / (points.size - 1f)
                        val ratio = ((pair.second - minV) / (maxV - minV)).toFloat().coerceIn(0f, 1f)
                        val y = bottom - (bottom - top) * ratio
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        drawCircle(lineColor, radius = 4.dp.toPx(), center = Offset(x, y))
                    }
                    drawPath(path, lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
                }
                Text(
                    "${points.first().first} → ${points.last().first} · ${formatAcademicNumber(points.first().second)} → ${formatAcademicNumber(points.last().second)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SemesterStatCard(stat: AcademicAnalytics.SemesterStat) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(stat.semesterName, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "GPA ${stat.gpa?.let(::formatAcademicNumber) ?: "-"} · 学分 ${formatAcademicNumber(stat.credits)} · 均分 ${stat.weightedAverage?.let(::formatAcademicNumber) ?: "-"} · ${stat.courseCount} 门课",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AcademicOptionsDialog(
    examRemindersEnabled: Boolean,
    gradeNotificationsEnabled: Boolean,
    backgroundSyncEnabled: Boolean,
    maskExportEnabled: Boolean,
    onDismiss: () -> Unit,
    onExamReminders: (Boolean) -> Unit,
    onGradeNotifications: (Boolean) -> Unit,
    onBackgroundSync: (Boolean) -> Unit,
    onMaskExport: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("提醒与导出") },
        text = {
            Column {
                CompactSwitchRow("考试前 1 天 / 2 小时提醒", examRemindersEnabled, onExamReminders)
                CompactSwitchRow("新成绩通知", gradeNotificationsEnabled, onGradeNotifications)
                CompactSwitchRow("后台自动检查成绩", backgroundSyncEnabled, onBackgroundSync)
                CompactSwitchRow("导出时隐藏敏感信息", maskExportEnabled, onMaskExport)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        }
    )
}

@Composable
private fun CompactSwitchRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().height(38.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 12.sp)
        Switch(checked = checked, onCheckedChange = onChecked, modifier = Modifier.size(44.dp, 28.dp))
    }
}

@Composable
private fun AcademicWarningCard(warnings: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("部分数据读取警告", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            warnings.take(4).forEach { Text("• $it", fontSize = 11.sp) }
        }
    }
}

@Composable
private fun EmptyAcademicCard(title: String, detail: String, modifier: Modifier = Modifier) {
    Box(modifier) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(detail, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AcademicWebView(
    modifier: Modifier,
    bridge: NwpuAcademicJavascriptBridge,
    onWebViewCreated: (WebView) -> Unit,
    onPageStarted: (String) -> Unit,
    onPageFinished: (WebView, String) -> Unit,
    onPageError: (String) -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                onWebViewCreated(this)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.loadsImagesAutomatically = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                settings.setSupportMultipleWindows(false)
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.userAgentString = settings.userAgentString.replace("; wv", "").replace(" Version/4.0", "")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) settings.safeBrowsingEnabled = true

                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)
                addJavascriptInterface(bridge, NwpuAcademicExtractor.BRIDGE_NAME)
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon); onPageStarted(url)
                    }
                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url); onPageFinished(view, url)
                    }
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = shouldBlockAcademicUrl(request.url.toString())
                    @Suppress("DEPRECATION")
                    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean = shouldBlockAcademicUrl(url)
                    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                        super.onReceivedError(view, request, error)
                        if (request.isForMainFrame) onPageError(error.description.toString())
                    }
                    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
                        super.onReceivedHttpError(view, request, errorResponse)
                        if (request.isForMainFrame) onPageError("HTTP ${errorResponse.statusCode}")
                    }
                }
                loadUrl(ACADEMIC_PORTAL_URL)
            }
        }
    )
}

private fun openAcademicForQuery(webView: WebView?, onResult: (String) -> Unit) {
    if (webView == null) { onResult("NO_WEBVIEW"); return }
    val script = """
        (function() {
            try {
                var keyword = "翱翔教务";
                window.open = function(url) { if (url) window.location.href = url; return window; };
                var nodes = Array.prototype.slice.call(document.querySelectorAll("a,button,[role='button'],li,div,span"));
                var matches = nodes.filter(function(node) {
                    var text = (node.innerText || "").replace(/\s+/g, "").trim();
                    return text === keyword;
                });
                if (!matches.length) return "NOT_FOUND";
                var element = matches[0];
                var anchor = element.closest ? element.closest("a") : null;
                if (anchor) { anchor.target = "_self"; anchor.click(); } else { element.click(); }
                return "CLICKED";
            } catch (error) { return "ERROR"; }
        })();
    """.trimIndent()
    webView.evaluateJavascript(script) { result ->
        onResult(result?.trim()?.removeSurrounding("\"") ?: "ERROR")
    }
}

private fun isAcademicQueryPage(url: String): Boolean {
    val uri = academicSafeUri(url) ?: return false
    return uri.host?.lowercase() == "jwxt.nwpu.edu.cn"
}

private fun shouldBlockAcademicUrl(url: String): Boolean {
    val uri = academicSafeUri(url) ?: return true
    return uri.scheme?.lowercase() != "https"
}

private fun academicSanitizeUrl(url: String): String {
    val uri = academicSafeUri(url) ?: return url
    return buildString { append(uri.scheme ?: "https"); append("://"); append(uri.host ?: ""); append(uri.path ?: "") }
}

private fun academicSafeUri(url: String): Uri? = runCatching { Uri.parse(url) }.getOrNull()

private fun updatedAtText(value: Long): String =
    if (value <= 0L) "尚未更新" else "上次更新 ${SimpleDateFormat("M/d HH:mm", Locale.CHINA).format(Date(value))}"

private fun formatAcademicNumber(value: Double): String {
    val rounded = kotlin.math.round(value * 100.0) / 100.0
    return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString()
    else rounded.toString().trimEnd('0').trimEnd('.')
}

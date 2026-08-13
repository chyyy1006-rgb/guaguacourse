package com.example.npucourse

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.npucourse.data.AppDatabase
import com.example.npucourse.data.academic.AcademicCacheStore
import com.example.npucourse.launcher.LauncherIconManager
import com.example.npucourse.notification.AcademicNotificationHelper
import com.example.npucourse.notification.AcademicSyncScheduler
import com.example.npucourse.notification.CourseAlarmScheduler
import com.example.npucourse.notification.NotificationHelper
import com.example.npucourse.notification.TaskAlarmScheduler
import com.example.npucourse.notification.ReminderPermissionManager
import com.example.npucourse.ui.components.ReminderPermissionDialog
import com.example.npucourse.ui.screens.AcademicPage
import com.example.npucourse.ui.screens.MinePage
import com.example.npucourse.ui.screens.TimetablePage
import com.example.npucourse.ui.screens.TodayPage
import com.example.npucourse.ui.theme.NPUcourseTheme
import com.example.npucourse.update.AppUpdateInfo
import com.example.npucourse.update.AppUpdateManager
import com.example.npucourse.update.UpdateCheckResult
import com.example.npucourse.update.UpdatePromptDialog
import com.example.npucourse.widget.TodayScheduleWidgetUpdater
import com.example.npucourse.util.calculateCurrentWeek
import com.example.npucourse.viewmodel.CourseViewModel
import com.example.npucourse.viewmodel.SemesterViewModel
import com.example.npucourse.viewmodel.SettingsViewModel
import com.example.npucourse.viewmodel.TaskViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class MainActivity :
    ComponentActivity() {

    private var taskNavigationRequestToken by mutableIntStateOf(0)
    private var academicInfoNavigationRequestToken by mutableIntStateOf(0)

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        if (intent?.getBooleanExtra("open_academic_tasks", false) == true) {
            taskNavigationRequestToken++
            intent?.removeExtra("open_academic_tasks")
        }

        if (intent?.getBooleanExtra("open_academic_info", false) == true) {
            academicInfoNavigationRequestToken++
            intent?.removeExtra("open_academic_info")
        }

        enableEdgeToEdge()

        setContent {
            val settingsViewModel: SettingsViewModel =
                viewModel(
                    factory = SettingsViewModel.Factory(this@MainActivity)
                )

            val appearanceSettings by
                settingsViewModel.settings.collectAsState()

            LaunchedEffect(appearanceSettings.appIconStyle) {
                LauncherIconManager.applyIcon(
                    context = this@MainActivity,
                    requestedStyle = appearanceSettings.appIconStyle
                )
            }

            NPUcourseTheme(
                themeMode = appearanceSettings.themeMode,
                accentStyle = appearanceSettings.accentStyle,
                dynamicColor = appearanceSettings.dynamicColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NpuCourseApp(
                        settingsViewModel = settingsViewModel,
                        taskNavigationRequestToken = taskNavigationRequestToken,
                        academicInfoNavigationRequestToken = academicInfoNavigationRequestToken
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("open_academic_tasks", false)) {
            taskNavigationRequestToken++
            intent.removeExtra("open_academic_tasks")
        }
        if (intent.getBooleanExtra("open_academic_info", false)) {
            academicInfoNavigationRequestToken++
            intent.removeExtra("open_academic_info")
        }
    }
}


@Composable
fun NpuCourseApp(
    settingsViewModel: SettingsViewModel,
    taskNavigationRequestToken: Int = 0,
    academicInfoNavigationRequestToken: Int = 0
) {

    val context =
        LocalContext.current

    var startupUpdateInfo by remember {
        mutableStateOf<AppUpdateInfo?>(null)
    }

    val courseViewModel:
        CourseViewModel =
        viewModel(
            factory =
                CourseViewModel.Factory(context)
        )

    val semesterViewModel:
        SemesterViewModel =
        viewModel(
            factory =
                SemesterViewModel.Factory(context)
        )

    val courses by
        courseViewModel
            .courses
            .collectAsState()

    val taskViewModel:
        TaskViewModel =
        viewModel(
            factory =
                TaskViewModel.Factory(context)
        )

    val tasks by
        taskViewModel
            .tasks
            .collectAsState()

    val semesters by
        semesterViewModel
            .semesters
            .collectAsState()

    val settings by
        settingsViewModel
            .settings
            .collectAsState()

    var selectedTab by rememberSaveable {
        mutableStateOf(if (taskNavigationRequestToken > 0 || academicInfoNavigationRequestToken > 0) "学业" else "今天")
    }
    var internalAcademicInfoRequestToken by remember {
        mutableIntStateOf(academicInfoNavigationRequestToken)
    }

    LaunchedEffect(taskNavigationRequestToken, academicInfoNavigationRequestToken) {
        if (taskNavigationRequestToken > 0 || academicInfoNavigationRequestToken > 0) {
            selectedTab = "学业"
        }
        if (academicInfoNavigationRequestToken > internalAcademicInfoRequestToken) {
            internalAcademicInfoRequestToken = academicInfoNavigationRequestToken
        }
    }

    var cachedNextExam by remember { mutableStateOf(AcademicCacheStore.nextExam(context)) }
    LaunchedEffect(selectedTab) {
        if (selectedTab == "今天") cachedNextExam = AcademicCacheStore.nextExam(context)
    }

    var permissionRefreshToken by remember {
        mutableIntStateOf(0)
    }
    var showReminderPermissionDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var notificationPermissionRequestedThisSession by remember {
        mutableStateOf(false)
    }
    var continueToExactAlarmAfterNotification by remember {
        mutableStateOf(false)
    }

    /*
     * 当前选中的课表。
     *
     * DataStore 中的 activeSemesterId 是持久化选择。
     * 如果第一次升级到 V3 还没有保存选择，自动选择最新课表。
     */
    val selectedSemester =
        semesters.firstOrNull {
            it.id ==
                settings.activeSemesterId
        }
            ?: semesters.firstOrNull()

    LaunchedEffect(
        semesters,
        settings.activeSemesterId
    ) {
        val fallback =
            selectedSemester

        if (
            fallback != null &&
            settings.activeSemesterId !=
                fallback.id
        ) {
            settingsViewModel
                .setActiveSemesterId(
                    fallback.id
                )
        }
    }

    val selectedCourses =
        if (
            selectedSemester != null
        ) {
            courses.filter {
                it.semesterId ==
                    selectedSemester.id
            }
        } else {
            emptyList()
        }

    val selectedTasks =
        if (selectedSemester != null) {
            tasks.filter {
                it.semesterId == selectedSemester.id
            }
        } else {
            emptyList()
        }

    val currentWeek =
        selectedSemester
            ?.let {
                calculateCurrentWeek(
                    it.startMillis
                )
            }
            ?: 0


    /* =====================================================
       精确闹钟权限
       ===================================================== */

    val exactAlarmPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .StartActivityForResult()
        ) {
            permissionRefreshToken++
        }

    val requestExactAlarmAccess:
        () -> Unit = {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.S
        ) {
            permissionRefreshToken++
        } else if (
            CourseAlarmScheduler
                .canScheduleExactAlarms(
                    context
                )
        ) {
            permissionRefreshToken++
        } else {
            val intent =
                Intent(
                    Settings
                        .ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                ).apply {
                    data =
                        Uri.parse(
                            "package:" +
                                context.packageName
                        )
                }

            exactAlarmPermissionLauncher
                .launch(intent)
        }
    }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .RequestPermission()
        ) { granted ->
            permissionRefreshToken++
            if (granted && continueToExactAlarmAfterNotification) {
                requestExactAlarmAccess()
            }
            continueToExactAlarmAfterNotification = false
        }

    val reminderSystemSettingsLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) {
            permissionRefreshToken++
        }

    val reminderPermissionStatus = remember(permissionRefreshToken) {
        ReminderPermissionManager.status(context)
    }

    val continueReminderPermissionSetup: () -> Unit = {
        NotificationHelper.createTaskReminderChannel(context)
        val status = ReminderPermissionManager.status(context)

        when {
            status.fullyReady -> {
                showReminderPermissionDialog = false
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !status.runtimeNotificationPermissionGranted &&
                !notificationPermissionRequestedThisSession -> {
                notificationPermissionRequestedThisSession = true
                continueToExactAlarmAfterNotification = false
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            !status.notificationAccessReady -> {
                reminderSystemSettingsLauncher.launch(
                    ReminderPermissionManager.appNotificationSettingsIntent(context)
                )
            }
            !status.taskChannelHeadsUpAndSoundEnabled -> {
                reminderSystemSettingsLauncher.launch(
                    ReminderPermissionManager.taskChannelSettingsIntent(context)
                )
            }
            !status.exactAlarmAllowed -> {
                reminderSystemSettingsLauncher.launch(
                    ReminderPermissionManager.exactAlarmSettingsIntent(context)
                )
            }
            else -> showReminderPermissionDialog = false
        }
    }


    LaunchedEffect(Unit) {
        NotificationHelper
            .createCourseReminderChannel(
                context
            )
        NotificationHelper
            .createTaskReminderChannel(
                context
            )
        AcademicNotificationHelper.createChannel(context)
        AcademicSyncScheduler.schedule(
            context,
            com.example.npucourse.data.academic.AcademicPreferencesStore.get(context).backgroundSyncEnabled
        )

        withContext(Dispatchers.IO) {
            AppDatabase.getInstance(context)
                .taskDao()
                .disableLegacyOneMinuteTestReminders(System.currentTimeMillis())
        }

        if (
            ReminderPermissionManager.shouldShowFirstLaunchPrompt(context) &&
            !ReminderPermissionManager.status(context).fullyReady
        ) {
            showReminderPermissionDialog = true
        }

        if (AppUpdateManager.shouldAutoCheck(context)) {
            when (val result = AppUpdateManager.checkForUpdates(context)) {
                is UpdateCheckResult.UpdateAvailable -> {
                    if (AppUpdateManager.shouldPromptUpdate(context, result.info)) {
                        AppUpdateManager.markPrompted(context, result.info.versionCode)
                        startupUpdateInfo = result.info
                    }
                }
                else -> Unit
            }
        }
    }


    /*
     * 只为当前选中的课表建立提醒。
     * 切换课表时会自动取消旧提醒并重建。
     */
    LaunchedEffect(
        selectedCourses,
        selectedSemester?.id,
        selectedSemester?.startMillis,
        selectedSemester?.campus,
        settings.reminderMinutes,
        permissionRefreshToken
    ) {

        val semester =
            selectedSemester

        /*
         * AlarmManager 的批量取消/重建可能涉及几十到数百个提醒。
         * 这些操作不能放在 Compose 主线程上执行，否则课程较多时
         * App 启动会长时间像“卡在 Run app / 正在加载”。
         */
        withContext(
            Dispatchers.IO
        ) {
            if (
                semester == null
            ) {
                CourseAlarmScheduler
                    .cancelAll(context)
            } else {
                CourseAlarmScheduler
                    .rescheduleAll(
                        context = context,
                        courses = selectedCourses,
                        semesterStartMillis =
                            semester.startMillis,
                        campus = semester.campus,
                        reminderMinutes =
                            settings.reminderMinutes
                    )
            }
        }
    }


    /* DDL 提醒覆盖全部学期，不因切换当前课表而失效。 */
    LaunchedEffect(
        tasks,
        courses,
        permissionRefreshToken
    ) {
        withContext(Dispatchers.IO) {
            // StateFlow 在首次订阅时会短暂给出 emptyList；这里直接读取 Room，
            // 避免 App 启动瞬间错误取消已经存在的 DDL 闹钟。
            val database = AppDatabase.getInstance(context)
            TaskAlarmScheduler.rescheduleAll(
                context = context,
                tasks = database.taskDao().getAllTasksOnce(),
                courseNamesById = database.courseDao()
                    .getAllCoursesOnce()
                    .associate { it.id to it.name }
            )
        }
    }


    LaunchedEffect(
        selectedCourses,
        selectedTasks,
        selectedSemester?.id,
        selectedSemester?.startMillis,
        settings.activeSemesterId
    ) {
        TodayScheduleWidgetUpdater.updateAll(context)
    }


    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) {
        innerPadding ->

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
        ) {

            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
            ) {

                when (selectedTab) {

                    "今天" -> {
                        if (
                            selectedSemester != null
                        ) {
                            TodayPage(
                                courses = selectedCourses,
                                currentWeek = currentWeek,
                                campus =
                                    selectedSemester.campus,
                                tasks = selectedTasks,
                                onToggleTask = taskViewModel::setCompleted,
                                nextExam = cachedNextExam,
                                onOpenAcademicInfo = {
                                    internalAcademicInfoRequestToken++
                                    selectedTab = "学业"
                                }
                            )
                        } else {
                            EmptySemesterPage()
                        }
                    }


                    "课表" -> {
                        if (
                            selectedSemester != null
                        ) {
                            TimetablePage(
                                courses = selectedCourses,
                                currentWeek = currentWeek,
                                campus =
                                    selectedSemester.campus,
                                semesterStartMillis =
                                    selectedSemester.startMillis,
                                selectedSemesterId =
                                    selectedSemester.id,
                                selectedSemesterName =
                                    selectedSemester.name,
                                semesters = semesters,
                                uiDensity = settings.uiDensity,
                                courseCardStyle = settings.courseCardStyle,
                                showSectionTimes = settings.showSectionTimes,
                                onSemesterSelected = {
                                    semesterId ->

                                    settingsViewModel
                                        .setActiveSemesterId(
                                            semesterId
                                        )
                                },
                                onAddCourse = {
                                    newCourse ->

                                    courseViewModel
                                        .addCourse(
                                            newCourse.copy(
                                                semesterId =
                                                    selectedSemester.id
                                            )
                                        )
                                },
                                onUpdateCourse = {
                                    course ->

                                    courseViewModel
                                        .updateCourse(
                                            course.copy(
                                                semesterId =
                                                    selectedSemester.id
                                            )
                                        )
                                },
                                onDeleteCourse = {
                                    courseId ->

                                    courseViewModel
                                        .deleteCourse(
                                            courseId
                                        )
                                }
                            )
                        } else {
                            EmptySemesterPage()
                        }
                    }


                    "学业" -> {
                        AcademicPage(
                            openTasksRequestToken = taskNavigationRequestToken,
                            openAcademicInfoRequestToken = internalAcademicInfoRequestToken
                        )
                    }


                    "我的" -> {
                        if (
                            selectedSemester != null
                        ) {
                            MinePage(
                                semesterName =
                                    selectedSemester.name,
                                semesterStartMillis =
                                    selectedSemester.startMillis,
                                currentWeek = currentWeek,
                                campus =
                                    selectedSemester.campus,
                                reminderMinutes =
                                    settings.reminderMinutes,
                                semesters =
                                    semesters,
                                selectedSemesterId =
                                    selectedSemester.id,
                                courseCountBySemester =
                                    courses
                                        .groupingBy {
                                            it.semesterId
                                        }
                                        .eachCount(),
                                taskCount = tasks.size,
                                selectedCourses = selectedCourses,
                                themeMode = settings.themeMode,
                                accentStyle = settings.accentStyle,
                                dynamicColor = settings.dynamicColor,
                                uiDensity = settings.uiDensity,
                                appIconStyle = settings.appIconStyle,
                                courseCardStyle = settings.courseCardStyle,
                                showSectionTimes = settings.showSectionTimes,
                                onSemesterSelected = {
                                    semesterId ->

                                    settingsViewModel
                                        .setActiveSemesterId(
                                            semesterId
                                        )
                                },
                                onCreateSemester = {
                                    name,
                                    startMillis,
                                    newCampus ->

                                    semesterViewModel
                                        .createSemester(
                                            name = name,
                                            startMillis = startMillis,
                                            campus = newCampus
                                        ) {
                                            newSemester ->

                                            settingsViewModel
                                                .setActiveSemesterId(
                                                    newSemester.id
                                                )
                                        }
                                },
                                onRenameSemester = {
                                    semesterId,
                                    newName ->

                                    semesterViewModel
                                        .renameSemester(
                                            semesterId = semesterId,
                                            newName = newName
                                        )
                                },
                                onDuplicateSemester = {
                                    semesterId,
                                    newName ->

                                    semesterViewModel
                                        .duplicateSemester(
                                            sourceSemesterId = semesterId,
                                            requestedName = newName
                                        ) {
                                            newSemester ->

                                            if (
                                                newSemester != null
                                            ) {
                                                settingsViewModel
                                                    .setActiveSemesterId(
                                                        newSemester.id
                                                    )
                                            }
                                        }
                                },
                                onDeleteSemester = {
                                    semesterId ->

                                    val fallbackSemesterId =
                                        semesters
                                            .firstOrNull {
                                                it.id != semesterId
                                            }
                                            ?.id
                                            ?: 0L

                                    semesterViewModel
                                        .deleteSemester(
                                            semesterId = semesterId
                                        ) {
                                            deleted ->

                                            if (
                                                deleted &&
                                                settings.activeSemesterId == semesterId &&
                                                fallbackSemesterId > 0L
                                            ) {
                                                settingsViewModel
                                                    .setActiveSemesterId(
                                                        fallbackSemesterId
                                                    )
                                            }
                                        }
                                },
                                onSemesterStartChange = {
                                    newMillis ->

                                    semesterViewModel
                                        .updateStartMillis(
                                            semesterId =
                                                selectedSemester.id,
                                            startMillis =
                                                newMillis
                                        )

                                    /*
                                     * 保留旧 DataStore 字段，兼容已有设置。
                                     */
                                    settingsViewModel
                                        .setSemesterStartMillis(
                                            newMillis
                                        )
                                },
                                onCampusChange = {
                                    newCampus ->

                                    semesterViewModel
                                        .updateCampus(
                                            semesterId =
                                                selectedSemester.id,
                                            campus =
                                                newCampus
                                        )

                                    settingsViewModel
                                        .setCampus(
                                            newCampus
                                        )
                                },
                                onReminderMinutesChange = {
                                    minutes ->

                                    settingsViewModel
                                        .setReminderMinutes(
                                            minutes
                                        )

                                    if (
                                        minutes < 0
                                    ) {
                                        CourseAlarmScheduler
                                            .cancelAll(
                                                context
                                            )
                                    } else if (
                                        Build.VERSION.SDK_INT >=
                                        Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat
                                            .checkSelfPermission(
                                                context,
                                                Manifest.permission
                                                    .POST_NOTIFICATIONS
                                            ) !=
                                        PackageManager
                                            .PERMISSION_GRANTED
                                    ) {
                                        continueToExactAlarmAfterNotification = true
                                        notificationPermissionRequestedThisSession = true
                                        notificationPermissionLauncher
                                            .launch(
                                                Manifest.permission
                                                    .POST_NOTIFICATIONS
                                            )
                                    } else {
                                        requestExactAlarmAccess()
                                    }
                                },
                                onThemeModeChange = { settingsViewModel.setThemeMode(it) },
                                onAccentStyleChange = { settingsViewModel.setAccentStyle(it) },
                                onDynamicColorChange = { settingsViewModel.setDynamicColor(it) },
                                onUiDensityChange = { settingsViewModel.setUiDensity(it) },
                                onAppIconStyleChange = { style ->
                                    if (
                                        LauncherIconManager.applyIcon(
                                            context = context,
                                            requestedStyle = style
                                        )
                                    ) {
                                        settingsViewModel.setAppIconStyle(style)
                                    }
                                },
                                onCourseCardStyleChange = { settingsViewModel.setCourseCardStyle(it) },
                                onShowSectionTimesChange = { settingsViewModel.setShowSectionTimes(it) }
                            )
                        } else {
                            EmptySemesterPage()
                        }
                    }
                }
            }

            if (showReminderPermissionDialog) {
                ReminderPermissionDialog(
                    status = reminderPermissionStatus,
                    onContinueSetup = {
                        ReminderPermissionManager.markFirstLaunchPromptShown(context)
                        continueReminderPermissionSetup()
                    },
                    onDismiss = {
                        ReminderPermissionManager.markFirstLaunchPromptShown(context)
                        showReminderPermissionDialog = false
                    }
                )
            }

            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = {
                    selectedTab = it
                }
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )
        }
    }

    startupUpdateInfo?.let { info ->
        UpdatePromptDialog(
            info = info,
            onDismiss = {
                startupUpdateInfo = null
            }
        )
    }
}


@Composable
private fun EmptySemesterPage() {
    Box(
        modifier =
            Modifier.fillMaxSize(),
        contentAlignment =
            Alignment.Center
    ) {
        Text(
            text = "正在准备课表……",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
private fun BottomNavigationBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 18.dp
                ),
        shape =
            RoundedCornerShape(26.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 3.dp
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 7.dp,
                        vertical = 7.dp
                    ),
            horizontalArrangement =
                Arrangement.spacedBy(4.dp)
        ) {

            listOf(
                "今天",
                "课表",
                "学业",
                "我的"
            ).forEach {
                tab ->

                BottomNavigationItem(
                    modifier =
                        Modifier.weight(1f),
                    text =
                        tab,
                    selected =
                        selectedTab == tab,
                    onClick = {
                        onTabSelected(
                            tab
                        )
                    }
                )
            }
        }
    }
}


@Composable
private fun BottomNavigationItem(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Box(
        modifier =
            modifier
                .background(
                    color =
                        if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            Color.Transparent
                        },
                    shape =
                        RoundedCornerShape(18.dp)
                )
                .clickable(
                    onClick =
                        onClick
                )
                .padding(
                    vertical = 9.dp
                ),
        contentAlignment =
            Alignment.Center
    ) {

        Text(
            text =
                text,
            fontSize =
                14.sp,
            fontWeight =
                if (selected) {
                    FontWeight.Bold
                } else {
                    FontWeight.Medium
                },
            color =
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
        )
    }
}

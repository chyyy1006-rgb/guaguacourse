package com.example.npucourse.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.npucourse.importer.EduCourseRecord
import com.example.npucourse.importer.NwpuSemesterInfo
import com.example.npucourse.util.formatSemesterDate
import com.example.npucourse.util.calculateCurrentWeek
import com.example.npucourse.viewmodel.CourseViewModel
import com.example.npucourse.viewmodel.SemesterViewModel
import com.example.npucourse.viewmodel.SettingsViewModel
import com.example.npucourse.viewmodel.TaskViewModel


private enum class AcademicSubPage {
    HOME,
    COURSE_MANAGEMENT,
    COURSE_SYNC,
    PORTAL_LOGIN,
    ACADEMIC_INFO,
    TASKS,
    WEEK_INSIGHTS
}


@Composable
fun AcademicPage(
    openTasksRequestToken: Int = 0,
    openAcademicInfoRequestToken: Int = 0
) {

    val context =
        LocalContext.current

    val courseViewModel:
        CourseViewModel =
        viewModel(
            factory =
                CourseViewModel.Factory(
                    context
                )
        )

    val semesterViewModel:
        SemesterViewModel =
        viewModel(
            factory =
                SemesterViewModel.Factory(
                    context
                )
        )

    val settingsViewModel:
        SettingsViewModel =
        viewModel(
            factory =
                SettingsViewModel.Factory(
                    context
                )
        )

    val taskViewModel:
        TaskViewModel =
        viewModel(
            factory =
                TaskViewModel.Factory(
                    context
                )
        )

    val tasks by
        taskViewModel
            .tasks
            .collectAsState()

    val courses by
        courseViewModel
            .courses
            .collectAsState()

    val semesters by
        semesterViewModel
            .semesters
            .collectAsState()

    val settings by
        settingsViewModel
            .settings
            .collectAsState()

    var currentPage by rememberSaveable {
        mutableStateOf(
            when {
                openAcademicInfoRequestToken > 0 -> AcademicSubPage.ACADEMIC_INFO
                openTasksRequestToken > 0 -> AcademicSubPage.TASKS
                else -> AcademicSubPage.HOME
            }
        )
    }

    var handledTaskRequestToken by remember { mutableStateOf(openTasksRequestToken) }
    var handledAcademicInfoRequestToken by remember { mutableStateOf(openAcademicInfoRequestToken) }

    LaunchedEffect(openTasksRequestToken, openAcademicInfoRequestToken) {
        when {
            openAcademicInfoRequestToken > handledAcademicInfoRequestToken ->
                currentPage = AcademicSubPage.ACADEMIC_INFO
            openTasksRequestToken > handledTaskRequestToken ->
                currentPage = AcademicSubPage.TASKS
        }
        handledTaskRequestToken = openTasksRequestToken
        handledAcademicInfoRequestToken = openAcademicInfoRequestToken
    }

    var portalLoginCompleted by remember {
        mutableStateOf(false)
    }

    var realEduRecords by remember {
        mutableStateOf<List<EduCourseRecord>>(
            emptyList()
        )
    }

    var detectedSemester by remember {
        mutableStateOf<NwpuSemesterInfo?>(
            null
        )
    }

    /*
     * 当前这一批翱翔课表准备导入到哪个 Semester。
     */
    var targetSemesterId by remember {
        mutableLongStateOf(0L)
    }

    val activeSemester =
        semesters.firstOrNull {
            it.id == settings.activeSemesterId
        }
            ?: semesters.firstOrNull()

    val activeCourseCount =
        activeSemester
            ?.let { semester ->
                courses.count {
                    it.semesterId == semester.id
                }
            }
            ?: 0

    val activeCourses =
        activeSemester
            ?.let { semester ->
                courses.filter {
                    it.semesterId == semester.id
                }
            }
            ?: emptyList()

    val activeTasks =
        activeSemester
            ?.let { semester ->
                tasks.filter {
                    it.semesterId == semester.id
                }
            }
            ?: emptyList()

    val currentWeek =
        activeSemester
            ?.let {
                calculateCurrentWeek(
                    it.startMillis
                )
            }
            ?: 0

    when (
        currentPage
    ) {

        AcademicSubPage.PORTAL_LOGIN -> {

            EduLoginPage(
                onBack = {
                    currentPage =
                        AcademicSubPage.COURSE_SYNC
                },
                onLoginCompleted = {
                    portalLoginCompleted = true
                    currentPage =
                        AcademicSubPage.COURSE_SYNC
                },
                onCoursesExtracted = {
                    records,
                    semesterInfo ->

                    portalLoginCompleted = true

                    val semesterName =
                        semesterInfo
                            ?.label
                            ?.trim()
                            ?.ifBlank { null }
                            ?: "未识别学期课表"

                    /*
                     * 已有同名学期：复用。
                     * 新学期：自动创建一个新的课表。
                     */
                    semesterViewModel
                        .findOrCreateSemester(
                            name = semesterName,
                            startMillis =
                                semesterInfo
                                    ?.startMillis,
                            campus = activeSemester?.campus ?: settings.campus
                        ) {
                            semester ->

                            targetSemesterId =
                                semester.id

                            settingsViewModel
                                .setActiveSemesterId(
                                    semester.id
                                )

                            realEduRecords =
                                records

                            detectedSemester =
                                semesterInfo
                                    ?: NwpuSemesterInfo(
                                        label = semester.name,
                                        startMillis = null
                                    )

                            currentPage =
                                AcademicSubPage.COURSE_SYNC
                        }
                }
            )

            return
        }


        AcademicSubPage.COURSE_MANAGEMENT -> {

            val semester =
                activeSemester

            if (
                semester == null
            ) {
                currentPage =
                    AcademicSubPage.HOME
            } else {
                CourseManagementPage(
                    semesterId = semester.id,
                    semesterName = semester.name,
                    campus = semester.campus,
                    courses = courses.filter {
                        it.semesterId == semester.id
                    },
                    onBack = {
                        currentPage =
                            AcademicSubPage.HOME
                    },
                    onAddCourse = { course ->
                        courseViewModel
                            .addCourse(
                                course.copy(
                                    semesterId = semester.id
                                )
                            )
                    },
                    onUpdateCourse = { course ->
                        courseViewModel
                            .updateCourse(
                                course.copy(
                                    semesterId = semester.id
                                )
                            )
                    },
                    onDeleteCourse = { courseId ->
                        courseViewModel
                            .deleteCourse(
                                courseId
                            )
                    }
                )
            }

            return
        }


        AcademicSubPage.COURSE_SYNC -> {

            val effectiveSemesterId =
                when {
                    targetSemesterId > 0L ->
                        targetSemesterId

                    settings.activeSemesterId > 0L ->
                        settings.activeSemesterId

                    else ->
                        0L
                }

            val existingCoursesForSemester =
                if (
                    effectiveSemesterId > 0L
                ) {
                    courses.filter {
                        it.semesterId ==
                            effectiveSemesterId
                    }
                } else {
                    emptyList()
                }

            CourseSyncPage(
                existingCourses =
                    existingCoursesForSemester,
                portalLoginCompleted =
                    portalLoginCompleted,
                eduRecords =
                    realEduRecords,
                detectedSemester =
                    detectedSemester,
                onOpenPortalLogin = {
                    currentPage =
                        AcademicSubPage.PORTAL_LOGIN
                },
                onBack = {
                    currentPage =
                        AcademicSubPage.HOME
                },
                onApplySync = {
                    strategy,
                    coursesToApply ->

                    if (
                        effectiveSemesterId > 0L
                    ) {
                        when (strategy) {
                            CourseSyncStrategy.ADD_ONLY -> {
                                courseViewModel
                                    .importCourses(
                                        courses = coursesToApply,
                                        semesterId = effectiveSemesterId
                                    )
                            }

                            CourseSyncStrategy.SMART_MERGE,
                            CourseSyncStrategy.FULL_REPLACE -> {
                                courseViewModel
                                    .replaceSemesterCourses(
                                        courses = coursesToApply,
                                        semesterId = effectiveSemesterId
                                    )
                            }
                        }
                    }
                }
            )

            return
        }


        AcademicSubPage.ACADEMIC_INFO -> {
            AcademicInfoPage(
                onBack = {
                    currentPage = AcademicSubPage.HOME
                }
            )
            return
        }


        AcademicSubPage.TASKS -> {
            val semester = activeSemester
            if (semester == null) {
                currentPage = AcademicSubPage.HOME
            } else {
                TaskManagementPage(
                    semesterName = semester.name,
                    semesterId = semester.id,
                    courses = activeCourses,
                    tasks = activeTasks,
                    onAddTask = taskViewModel::addTask,
                    onUpdateTask = taskViewModel::updateTask,
                    onToggleTask = taskViewModel::setCompleted,
                    onDeleteTask = taskViewModel::deleteTask,
                    onBack = { currentPage = AcademicSubPage.HOME }
                )
                return
            }
        }

        AcademicSubPage.WEEK_INSIGHTS -> {
            val semester = activeSemester
            if (semester == null) {
                currentPage = AcademicSubPage.HOME
            } else {
                WeekInsightsPage(
                    courses = activeCourses,
                    currentWeek = currentWeek,
                    campus = semester.campus,
                    semesterName = semester.name,
                    onBack = { currentPage = AcademicSubPage.HOME }
                )
                return
            }
        }

        AcademicSubPage.HOME -> Unit
    }


    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    start = 24.dp,
                    top = 24.dp,
                    end = 24.dp,
                    bottom = 40.dp
                )
    ) {

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        Text(
            text = "学业",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Text(
            text = "西北工业大学",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(
            modifier =
                Modifier.height(30.dp)
        )

        if (
            activeSemester != null
        ) {
            AcademicSummaryCard(
                semesterName = activeSemester.name,
                startDate = formatSemesterDate(
                    activeSemester.startMillis
                ),
                courseCount = activeCourseCount
            )

            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )
        }

        AcademicFeatureCard(
            title = "课程管理",
            subtitle =
                if (
                    activeSemester != null
                ) {
                    activeCourseCount.toString() +
                        " 条安排 · 搜索、编辑、颜色与周次"
                } else {
                    "当前没有可管理的课表"
                },
            onClick =
                if (
                    activeSemester != null
                ) {
                    {
                        currentPage =
                            AcademicSubPage.COURSE_MANAGEMENT
                    }
                } else {
                    null
                }
        )

        Spacer(
            modifier =
                Modifier.height(14.dp)
        )

        AcademicFeatureCard(
            title = "课程待办 / DDL",
            subtitle =
                if (activeSemester != null) {
                    val open = activeTasks.count { !it.completed }
                    val overdue = activeTasks.count {
                        !it.completed && it.dueAt > 0L && it.dueAt < System.currentTimeMillis()
                    }
                    if (overdue > 0) {
                        "$open 项待完成 · $overdue 项已逾期"
                    } else {
                        "$open 项待完成 · 可绑定具体课程"
                    }
                } else {
                    "当前没有可用课表"
                },
            onClick = if (activeSemester != null) {
                { currentPage = AcademicSubPage.TASKS }
            } else null
        )

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        AcademicFeatureCard(
            title = "本周分析",
            subtitle = "空闲时间、课程冲突、课内时长与分享",
            onClick = if (activeSemester != null) {
                { currentPage = AcademicSubPage.WEEK_INSIGHTS }
            } else null
        )

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        AcademicFeatureCard(
            title = "考试与成绩",
            subtitle = "查询成绩、考试安排与学业趋势",
            onClick = {
                currentPage =
                    AcademicSubPage.ACADEMIC_INFO
            }
        )

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        AcademicFeatureCard(
            title = "课程表同步",
            subtitle =
                when {
                    realEduRecords.isNotEmpty() &&
                        detectedSemester != null ->
                        detectedSemester?.label
                            ?: "已读取翱翔课表"

                    semesters.size > 1 ->
                        "已保存 ${semesters.size} 个课表 · 可继续导入其他学期"

                    else ->
                        "登录翱翔教务并导入本人课表"
                },
            onClick = {
                currentPage =
                    AcademicSubPage.COURSE_SYNC
            }
        )    }
}


@Composable
private fun AcademicSummaryCard(
    semesterName: String,
    startDate: String,
    courseCount: Int
) {
    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
    ) {
        Column(
            modifier =
                Modifier.padding(20.dp)
        ) {
            Text(
                text = "当前课表",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )

            Text(
                text = semesterName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(
                modifier =
                    Modifier.height(7.dp)
            )

            Text(
                text =
                    "开学 $startDate · $courseCount 条课程安排",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun AcademicFeatureCard(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = onClick != null
                ) {
                    onClick?.invoke()
                },
        shape = RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(20.dp)
        ) {

            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

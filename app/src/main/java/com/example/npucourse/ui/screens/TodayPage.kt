package com.example.npucourse.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.npucourse.model.DemoCourse
import com.example.npucourse.data.TaskEntity
import com.example.npucourse.data.academic.AcademicTimeParser
import com.example.npucourse.importer.NwpuExamRecord
import com.example.npucourse.model.isActiveInWeek
import com.example.npucourse.model.weekDisplayText
import com.example.npucourse.util.campusDisplayName
import com.example.npucourse.util.getScheduleForCampus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay


@Composable
fun TodayPage(
    courses: List<DemoCourse>,
    currentWeek: Int,
    campus: String,
    tasks: List<TaskEntity> = emptyList(),
    onToggleTask: (Long, Boolean) -> Unit = { _, _ -> },
    nextExam: NwpuExamRecord? = null,
    onOpenAcademicInfo: () -> Unit = {}
) {

    var nowMillis by remember {
        mutableLongStateOf(
            System.currentTimeMillis()
        )
    }

    var selectedCourse by remember {
        mutableStateOf<DemoCourse?>(
            null
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            nowMillis =
                System.currentTimeMillis()
            delay(30_000L)
        }
    }

    val todayDay =
        getTodayDayOfWeek()

    val nowCalendar =
        Calendar
            .getInstance()
            .apply {
                timeInMillis =
                    nowMillis
            }

    val nowMinutes =
        nowCalendar.get(
            Calendar.HOUR_OF_DAY
        ) * 60 +
            nowCalendar.get(
                Calendar.MINUTE
            )

    val todayCourses =
        if (
            currentWeek in 1..20
        ) {
            courses
                .filter {
                    course ->

                    course.day ==
                        todayDay &&
                        course.isActiveInWeek(
                            currentWeek
                        )
                }
                .sortedBy {
                    it.startSection
                }
        } else {
            emptyList()
        }

    val courseMoment =
        resolveCourseMoment(
            courses =
                todayCourses,
            campus =
                campus,
            nowMinutes =
                nowMinutes
        )

    val nextTeachingDay =
        if (
            currentWeek in 1..20 &&
            courseMoment.ongoing == null &&
            courseMoment.next == null
        ) {
            findNextTeachingDay(
                courses =
                    courses,
                currentWeek =
                    currentWeek,
                todayDay =
                    todayDay
            )
        } else {
            null
        }


    val openTasks =
        tasks
            .filter { !it.completed }
            .sortedWith(
                compareBy<TaskEntity> { if (it.dueAt > 0L) 0 else 1 }
                    .thenBy { if (it.dueAt > 0L) it.dueAt else Long.MAX_VALUE }
                    .thenByDescending { it.priority }
            )

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 20.dp
                )
    ) {

        Spacer(
            modifier =
                Modifier.height(22.dp)
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.Bottom
        ) {

            Column {

                Text(
                    text =
                        getGreeting(),
                    fontSize =
                        14.sp,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(
                    modifier =
                        Modifier.height(3.dp)
                )

                Text(
                    text =
                        "今天",
                    fontSize =
                        32.sp,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        MaterialTheme.colorScheme.onBackground
                )
            }

            Text(
                text =
                    formatClockTime(
                        nowMillis
                    ),
                fontSize =
                    18.sp,
                fontWeight =
                    FontWeight.SemiBold,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Text(
            text =
                getTodayDateText(),
            fontSize =
                14.sp,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(
            modifier =
                Modifier.height(14.dp)
        )

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            InfoPill(
                text =
                    getSemesterStateText(
                        currentWeek
                    )
            )

            InfoPill(
                text =
                    campusDisplayName(
                        campus
                    )
            )

            if (
                currentWeek in 1..20
            ) {
                InfoPill(
                    text =
                        todayCourses.size
                            .toString() +
                            " 门课"
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        MainCourseCard(
            todayCourses =
                todayCourses,
            currentWeek =
                currentWeek,
            campus =
                campus,
            nowMillis =
                nowMillis,
            nowMinutes =
                nowMinutes,
            ongoingCourse =
                courseMoment.ongoing,
            nextCourse =
                courseMoment.next,
            nextTeachingDay =
                nextTeachingDay,
            onCourseClick = {
                course ->

                selectedCourse =
                    course
            }
        )

        if (nextExam != null) {
            Spacer(Modifier.height(16.dp))
            NextExamHomeCard(
                exam = nextExam,
                onClick = onOpenAcademicInfo
            )
        }

        if (
            nextTeachingDay != null
        ) {

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            NextTeachingDayCard(
                nextTeachingDay =
                    nextTeachingDay,
                campus =
                    campus,
                onCourseClick = {
                    course ->

                    selectedCourse =
                        course
                }
            )
        }

        if (openTasks.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            TodayTaskSection(
                tasks = openTasks.take(3),
                courses = courses,
                totalOpenCount = openTasks.size,
                onToggleTask = onToggleTask
            )
        }

        Spacer(
            modifier =
                Modifier.height(28.dp)
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text =
                    "今日课程",
                fontSize =
                    21.sp,
                fontWeight =
                    FontWeight.Bold,
                color =
                    MaterialTheme.colorScheme.onSurface
            )

            if (
                todayCourses.isNotEmpty()
            ) {
                Text(
                    text =
                        todayCourses.size
                            .toString() +
                            " 门",
                    fontSize =
                        13.sp,
                    color =
                        MaterialTheme.colorScheme.outline
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        if (
            todayCourses.isEmpty()
        ) {
            EmptyTodayCard(
                currentWeek =
                    currentWeek
            )
        } else {
            todayCourses.forEach {
                course ->

                TodayCourseCard(
                    course =
                        course,
                    campus =
                        campus,
                    nowMinutes =
                        nowMinutes,
                    onClick = {
                        selectedCourse =
                            course
                    }
                )

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(42.dp)
        )
    }


    selectedCourse
        ?.let {
            course ->

            TodayCourseDetailDialog(
                course =
                    course,
                campus =
                    campus,
                onDismiss = {
                    selectedCourse =
                        null
                }
            )
        }
}


@Composable
private fun NextExamHomeCard(
    exam: NwpuExamRecord,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(17.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "最近考试",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = exam.courseName.ifBlank { "未命名考试" },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = listOf(exam.timeText, exam.location).filter { it.isNotBlank() }.joinToString(" · "),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = AcademicTimeParser.countdownText(exam.timeText) ?: "查看",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}


@Composable
private fun InfoPill(
    text: String
) {

    Box(
        modifier =
            Modifier
                .background(
                    color =
                        MaterialTheme.colorScheme.surfaceVariant,
                    shape =
                        RoundedCornerShape(50)
                )
                .padding(
                    horizontal = 11.dp,
                    vertical = 6.dp
                )
    ) {
        Text(
            text =
                text,
            fontSize =
                12.sp,
            fontWeight =
                FontWeight.Medium,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
private fun TodayTaskSection(
    tasks: List<TaskEntity>,
    courses: List<DemoCourse>,
    totalOpenCount: Int,
    onToggleTask: (Long, Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "近期待办",
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$totalOpenCount 项未完成",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.outline
        )
    }

    Spacer(Modifier.height(10.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(vertical = 6.dp)) {
            tasks.forEachIndexed { index, task ->
                val courseName = courses.firstOrNull { it.id == task.courseId }?.name
                val overdue = task.dueAt > 0L && task.dueAt < System.currentTimeMillis()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = false,
                        onCheckedChange = { checked ->
                            onToggleTask(task.id, checked)
                        }
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val meta = buildString {
                            if (!courseName.isNullOrBlank()) append(courseName)
                            if (task.dueAt > 0L) {
                                if (isNotEmpty()) append(" · ")
                                append(if (overdue) "已逾期 " else "截止 ")
                                append(formatDueAt(task.dueAt))
                            }
                        }
                        if (meta.isNotBlank()) {
                            Text(
                                text = meta,
                                fontSize = 12.sp,
                                color = if (overdue) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (index < tasks.lastIndex) {
                    Spacer(Modifier.height(1.dp))
                }
            }
        }
    }
}


@Composable
private fun MainCourseCard(
    todayCourses: List<DemoCourse>,
    currentWeek: Int,
    campus: String,
    nowMillis: Long,
    nowMinutes: Int,
    ongoingCourse: DemoCourse?,
    nextCourse: DemoCourse?,
    nextTeachingDay: NextTeachingDay?,
    onCourseClick: (DemoCourse) -> Unit
) {

    val accentColor =
        when {
            ongoingCourse != null ->
                ongoingCourse.color

            nextCourse != null ->
                nextCourse.color

            else ->
                Color(0xFF6377F4)
        }

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(26.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(20.dp)
        ) {

            when {
                currentWeek == 0 -> {
                    StatusLabel(
                        text =
                            "学期尚未开始",
                        color =
                            Color(0xFF8A7A5B)
                    )

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )

                    Text(
                        text =
                            "还没有进入教学周",
                        fontSize =
                            23.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(
                        modifier =
                            Modifier.height(7.dp)
                    )

                    Text(
                        text =
                            "可在“我的”中检查当前课表的开学日期。",
                        fontSize =
                            13.sp,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                currentWeek > 20 -> {
                    StatusLabel(
                        text =
                            "本学期已结束",
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )

                    Text(
                        text =
                            "本学期课程已经结束",
                        fontSize =
                            23.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            MaterialTheme.colorScheme.onSurface
                    )
                }

                ongoingCourse != null -> {

                    StatusLabel(
                        text =
                            "正在上课",
                        color =
                            Color(0xFF43856F)
                    )

                    Spacer(
                        modifier =
                            Modifier.height(9.dp)
                    )

                    Text(
                        text =
                            ongoingCourse.name,
                        modifier =
                            Modifier.clickable {
                                onCourseClick(
                                    ongoingCourse
                                )
                            },
                        fontSize =
                            25.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(
                        modifier =
                            Modifier.height(7.dp)
                    )

                    Text(
                        text =
                            getCourseTimeText(
                                campus,
                                ongoingCourse
                            ) +
                                " · " +
                                getSectionRoomText(
                                    ongoingCourse
                                ),
                        fontSize =
                            14.sp,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val progress =
                        calculateCourseProgress(
                            campus =
                                campus,
                            course =
                                ongoingCourse,
                            nowMinutes =
                                nowMinutes
                        )

                    Spacer(
                        modifier =
                            Modifier.height(15.dp)
                    )

                    LinearProgressIndicator(
                        progress = {
                            progress
                        },
                        modifier =
                            Modifier.fillMaxWidth(),
                        color =
                            accentColor,
                        trackColor =
                            accentColor.copy(
                                alpha = 0.13f
                            )
                    )
                }

                nextCourse != null -> {

                    StatusLabel(
                        text =
                            "下一节课",
                        color =
                            Color(0xFF5268D5)
                    )

                    Spacer(
                        modifier =
                            Modifier.height(9.dp)
                    )

                    Text(
                        text =
                            nextCourse.name,
                        modifier =
                            Modifier.clickable {
                                onCourseClick(
                                    nextCourse
                                )
                            },
                        fontSize =
                            25.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(
                        modifier =
                            Modifier.height(7.dp)
                    )

                    Text(
                        text =
                            getCourseTimeText(
                                campus,
                                nextCourse
                            ) +
                                " · " +
                                nextCourse.room,
                        fontSize =
                            14.sp,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val nextStartMinutes =
                        getCourseStartMinutes(
                            campus,
                            nextCourse
                        )

                    if (
                        nextStartMinutes != null
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(13.dp)
                        )

                        Text(
                            text =
                                getRemainingText(
                                    nextStartMinutes -
                                        nowMinutes
                                ),
                            fontSize =
                                14.sp,
                            fontWeight =
                                FontWeight.SemiBold,
                            color =
                                accentColor
                        )
                    }
                }

                todayCourses.isNotEmpty() -> {

                    StatusLabel(
                        text =
                            "今日课程已结束",
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )

                    Text(
                        text =
                            "今天的课已经上完了",
                        fontSize =
                            23.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(
                        modifier =
                            Modifier.height(7.dp)
                    )

                    Text(
                        text =
                            if (
                                nextTeachingDay != null
                            ) {
                                "下一教学日：" +
                                    nextTeachingDay.relativeText +
                                    " · " +
                                    nextTeachingDay.courses.size +
                                    " 门课"
                            } else {
                                "现在 " +
                                    formatClockTime(
                                        nowMillis
                                    )
                            },
                        fontSize =
                            14.sp,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {

                    StatusLabel(
                        text =
                            "今天没有课程",
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )

                    Text(
                        text =
                            "今天可以自由安排",
                        fontSize =
                            24.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            MaterialTheme.colorScheme.onSurface
                    )

                    if (
                        nextTeachingDay != null
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(7.dp)
                        )

                        Text(
                            text =
                                "下一教学日：" +
                                    nextTeachingDay.relativeText +
                                    " · " +
                                    nextTeachingDay.courses.size +
                                    " 门课",
                            fontSize =
                                14.sp,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun StatusLabel(
    text: String,
    color: Color
) {

    Text(
        text =
            text,
        fontSize =
            13.sp,
        fontWeight =
            FontWeight.SemiBold,
        color =
            color
    )
}


@Composable
private fun NextTeachingDayCard(
    nextTeachingDay: NextTeachingDay,
    campus: String,
    onCourseClick: (DemoCourse) -> Unit
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surfaceVariant
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(17.dp)
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Text(
                    text =
                        "下一教学日",
                    fontSize =
                        15.sp,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text =
                        nextTeachingDay.relativeText,
                    fontSize =
                        13.sp,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(
                modifier =
                    Modifier.height(11.dp)
            )

            nextTeachingDay
                .courses
                .take(2)
                .forEach {
                    course ->

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onCourseClick(
                                        course
                                    )
                                }
                                .padding(
                                    vertical = 5.dp
                                ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Box(
                            modifier =
                                Modifier
                                    .size(8.dp)
                                    .background(
                                        color =
                                            course.color,
                                        shape =
                                            CircleShape
                                    )
                        )

                        Spacer(
                            modifier =
                                Modifier.size(10.dp)
                        )

                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {

                            Text(
                                text =
                                    course.name,
                                fontSize =
                                    14.sp,
                                fontWeight =
                                    FontWeight.SemiBold,
                                maxLines =
                                    1,
                                overflow =
                                    TextOverflow.Ellipsis,
                                color =
                                    MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text =
                                    getCourseStartTime(
                                        campus,
                                        course
                                    ) +
                                        " · " +
                                        course.room,
                                fontSize =
                                    12.sp,
                                color =
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

            if (
                nextTeachingDay.courses.size > 2
            ) {
                Text(
                    text =
                        "还有 " +
                            (nextTeachingDay.courses.size - 2) +
                            " 门课程",
                    modifier =
                        Modifier.padding(
                            top = 5.dp
                        ),
                    fontSize =
                        12.sp,
                    color =
                        MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}


@Composable
private fun TodayCourseCard(
    course: DemoCourse,
    campus: String,
    nowMinutes: Int,
    onClick: () -> Unit
) {

    val state =
        getCourseTimeState(
            campus =
                campus,
            course =
                course,
            nowMinutes =
                nowMinutes
        )

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick =
                        onClick
                ),
        shape =
            RoundedCornerShape(18.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (
                        state ==
                        CourseTimeState.IN_PROGRESS
                    ) {
                        course.color.copy(
                            alpha = 0.12f
                        )
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
    ) {

        Row(
            modifier =
                Modifier.padding(
                    15.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(
                    text =
                        getCourseStartTime(
                            campus,
                            course
                        ),
                    fontSize =
                        14.sp,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        MaterialTheme.colorScheme.onSurface
                )

                Spacer(
                    modifier =
                        Modifier.height(3.dp)
                )

                Box(
                    modifier =
                        Modifier
                            .size(8.dp)
                            .background(
                                color =
                                    courseStateColor(
                                        state
                                    ),
                                shape =
                                    CircleShape
                            )
                )
            }

            Spacer(
                modifier =
                    Modifier.size(15.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            course.name,
                        modifier =
                            Modifier.weight(1f),
                        fontSize =
                            17.sp,
                        fontWeight =
                            FontWeight.Bold,
                        maxLines =
                            1,
                        overflow =
                            TextOverflow.Ellipsis,
                        color =
                            MaterialTheme.colorScheme.onSurface
                    )

                    val stateText =
                        courseStateText(
                            state
                        )

                    if (
                        stateText.isNotBlank()
                    ) {
                        Text(
                            text =
                                stateText,
                            fontSize =
                                11.sp,
                            fontWeight =
                                FontWeight.SemiBold,
                            color =
                                courseStateColor(
                                    state
                                )
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(5.dp)
                )

                Text(
                    text =
                        getCourseEndTime(
                            campus,
                            course
                        ) +
                            " · 第" +
                            course.startSection +
                            "–" +
                            course.endSection +
                            "节 · " +
                            course.room,
                    fontSize =
                        13.sp,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines =
                        1,
                    overflow =
                        TextOverflow.Ellipsis
                )
            }
        }
    }
}


@Composable
private fun EmptyTodayCard(
    currentWeek: Int
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(18.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surfaceVariant
            )
    ) {

        Text(
            text =
                when {
                    currentWeek == 0 ->
                        "学期尚未开始，今日没有教学安排。"

                    currentWeek > 20 ->
                        "本学期已经结束。"

                    else ->
                        "今天没有课程安排。"
                },
            modifier =
                Modifier.padding(18.dp),
            fontSize =
                14.sp,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
private fun TodayCourseDetailDialog(
    course: DemoCourse,
    campus: String,
    onDismiss: () -> Unit
) {

    AlertDialog(
        onDismissRequest =
            onDismiss,
        title = {
            Text(
                text =
                    course.name,
                fontWeight =
                    FontWeight.Bold
            )
        },
        text = {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                DetailLine(
                    title =
                        "时间",
                    value =
                        getCourseTimeText(
                            campus,
                            course
                        ) +
                            " · 第" +
                            course.startSection +
                            "–" +
                            course.endSection +
                            "节"
                )

                DetailLine(
                    title =
                        "教室",
                    value =
                        course.room
                )

                DetailLine(
                    title =
                        "教师",
                    value =
                        course.teacher
                )

                DetailLine(
                    title =
                        "周次",
                    value =
                        course.weekDisplayText()
                )

                DetailLine(
                    title = "提醒",
                    value = when {
                        !course.reminderEnabled -> "本课程已关闭"
                        course.reminderMinutesOverride >= 0 -> "提前 ${course.reminderMinutesOverride} 分钟"
                        else -> "跟随全局设置"
                    }
                )

                if (course.notes.isNotBlank()) {
                    DetailLine(
                        title = "备注",
                        value = course.notes
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick =
                    onDismiss
            ) {
                Text(
                    "关闭"
                )
            }
        }
    )
}


@Composable
private fun DetailLine(
    title: String,
    value: String
) {

    Column {
        Text(
            text =
                title,
            fontSize =
                12.sp,
            color =
                MaterialTheme.colorScheme.outline
        )

        Spacer(
            modifier =
                Modifier.height(2.dp)
        )

        Text(
            text =
                value,
            fontSize =
                15.sp,
            color =
                MaterialTheme.colorScheme.onSurface
        )
    }
}


private data class CourseMoment(
    val ongoing: DemoCourse?,
    val next: DemoCourse?
)


private data class TimedCourse(
    val course: DemoCourse,
    val startMinutes: Int,
    val endMinutes: Int
)


private data class NextTeachingDay(
    val dayOffset: Int,
    val week: Int,
    val day: Int,
    val courses: List<DemoCourse>
) {
    val relativeText: String
        get() =
            when (
                dayOffset
            ) {
                1 ->
                    "明天 · " +
                        weekDayName(
                            day
                        )

                2 ->
                    "后天 · " +
                        weekDayName(
                            day
                        )

                else ->
                    dayOffset.toString() +
                        " 天后 · " +
                        weekDayName(
                            day
                        )
            }
}


private enum class CourseTimeState {
    FINISHED,
    IN_PROGRESS,
    UPCOMING,
    UNKNOWN
}


private fun resolveCourseMoment(
    courses: List<DemoCourse>,
    campus: String,
    nowMinutes: Int
): CourseMoment {

    val timedCourses =
        courses.mapNotNull {
            course ->

            val start =
                getCourseStartMinutes(
                    campus,
                    course
                )
                    ?: return@mapNotNull null

            val end =
                getCourseEndMinutes(
                    campus,
                    course
                )
                    ?: return@mapNotNull null

            TimedCourse(
                course =
                    course,
                startMinutes =
                    start,
                endMinutes =
                    end
            )
        }
            .sortedBy {
                it.startMinutes
            }

    val ongoing =
        timedCourses
            .firstOrNull {
                nowMinutes in
                    it.startMinutes..it.endMinutes
            }
            ?.course

    val next =
        timedCourses
            .firstOrNull {
                nowMinutes <
                    it.startMinutes
            }
            ?.course

    return CourseMoment(
        ongoing =
            ongoing,
        next =
            next
    )
}


private fun findNextTeachingDay(
    courses: List<DemoCourse>,
    currentWeek: Int,
    todayDay: Int
): NextTeachingDay? {

    for (
        offset in 1..14
    ) {

        val absoluteDay =
            todayDay - 1 +
                offset

        val weekOffset =
            absoluteDay / 7

        val day =
            absoluteDay % 7 +
                1

        val week =
            currentWeek +
                weekOffset

        if (
            week !in 1..20
        ) {
            continue
        }

        val dayCourses =
            courses
                .filter {
                    course ->

                    course.day ==
                        day &&
                        course.isActiveInWeek(
                            week
                        )
                }
                .sortedBy {
                    it.startSection
                }

        if (
            dayCourses.isNotEmpty()
        ) {
            return NextTeachingDay(
                dayOffset =
                    offset,
                week =
                    week,
                day =
                    day,
                courses =
                    dayCourses
            )
        }
    }

    return null
}


private fun getCourseTimeState(
    campus: String,
    course: DemoCourse,
    nowMinutes: Int
): CourseTimeState {

    val start =
        getCourseStartMinutes(
            campus,
            course
        )
            ?: return CourseTimeState.UNKNOWN

    val end =
        getCourseEndMinutes(
            campus,
            course
        )
            ?: return CourseTimeState.UNKNOWN

    return when {
        nowMinutes < start ->
            CourseTimeState.UPCOMING

        nowMinutes > end ->
            CourseTimeState.FINISHED

        else ->
            CourseTimeState.IN_PROGRESS
    }
}


private fun calculateCourseProgress(
    campus: String,
    course: DemoCourse,
    nowMinutes: Int
): Float {

    val start =
        getCourseStartMinutes(
            campus,
            course
        )
            ?: return 0f

    val end =
        getCourseEndMinutes(
            campus,
            course
        )
            ?: return 0f

    if (
        end <= start
    ) {
        return 0f
    }

    return (
        (
            nowMinutes -
                start
            ).toFloat() /
            (
                end -
                    start
                ).toFloat()
        )
        .coerceIn(
            0f,
            1f
        )
}


private fun courseStateText(
    state: CourseTimeState
): String {

    return when (
        state
    ) {
        CourseTimeState.IN_PROGRESS ->
            "进行中"

        CourseTimeState.UPCOMING ->
            "未开始"

        CourseTimeState.FINISHED ->
            "已结束"

        CourseTimeState.UNKNOWN ->
            ""
    }
}


private fun courseStateColor(
    state: CourseTimeState
): Color {

    return when (
        state
    ) {
        CourseTimeState.IN_PROGRESS ->
            Color(0xFF50A487)

        CourseTimeState.UPCOMING ->
            Color(0xFF6377F4)

        CourseTimeState.FINISHED ->
            Color(0xFFAAAAAF)

        CourseTimeState.UNKNOWN ->
            Color(0xFFAAAAAF)
    }
}


private fun getCourseStartTime(
    campus: String,
    course: DemoCourse
): String {

    return getScheduleForCampus(
        campus
    )
        .firstOrNull {
            it.section ==
                course.startSection
        }
        ?.startTime
        ?: "--:--"
}


private fun getCourseEndTime(
    campus: String,
    course: DemoCourse
): String {

    return getScheduleForCampus(
        campus
    )
        .firstOrNull {
            it.section ==
                course.endSection
        }
        ?.endTime
        ?: "--:--"
}


private fun getCourseTimeText(
    campus: String,
    course: DemoCourse
): String {

    return getCourseStartTime(
        campus,
        course
    ) +
        " – " +
        getCourseEndTime(
            campus,
            course
        )
}


private fun getSectionRoomText(
    course: DemoCourse
): String {

    return "第" +
        course.startSection +
        "–" +
        course.endSection +
        "节 · " +
        course.room
}


private fun getCourseStartMinutes(
    campus: String,
    course: DemoCourse
): Int? {

    val time =
        getScheduleForCampus(
            campus
        )
            .firstOrNull {
                it.section ==
                    course.startSection
            }
            ?.startTime
            ?: return null

    return timeToMinutes(
        time
    )
}


private fun getCourseEndMinutes(
    campus: String,
    course: DemoCourse
): Int? {

    val time =
        getScheduleForCampus(
            campus
        )
            .firstOrNull {
                it.section ==
                    course.endSection
            }
            ?.endTime
            ?: return null

    return timeToMinutes(
        time
    )
}


private fun timeToMinutes(
    time: String
): Int? {

    val parts =
        time.split(":")

    if (
        parts.size != 2
    ) {
        return null
    }

    val hour =
        parts[0]
            .toIntOrNull()
            ?: return null

    val minute =
        parts[1]
            .toIntOrNull()
            ?: return null

    return hour * 60 +
        minute
}


private fun getRemainingText(
    minutes: Int
): String {

    return when {
        minutes <= 1 ->
            "即将开始"

        minutes < 60 ->
            minutes.toString() +
                " 分钟后开始"

        else -> {
            val hours =
                minutes / 60
            val remainMinutes =
                minutes % 60

            if (
                remainMinutes == 0
            ) {
                hours.toString() +
                    " 小时后开始"
            } else {
                hours.toString() +
                    " 小时 " +
                    remainMinutes +
                    " 分钟后开始"
            }
        }
    }
}


private fun getSemesterStateText(
    currentWeek: Int
): String {

    return when {
        currentWeek == 0 ->
            "学期未开始"

        currentWeek > 20 ->
            "学期已结束"

        else ->
            "第" +
                currentWeek +
                "周"
    }
}


private fun getTodayDayOfWeek(): Int {

    return when (
        Calendar
            .getInstance()
            .get(
                Calendar.DAY_OF_WEEK
            )
    ) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        Calendar.SUNDAY -> 7
        else -> 1
    }
}


private fun getTodayDateText(): String {

    val calendar =
        Calendar.getInstance()

    val month =
        calendar.get(
            Calendar.MONTH
        ) + 1

    val day =
        calendar.get(
            Calendar.DAY_OF_MONTH
        )

    return month.toString() +
        "月" +
        day +
        "日 · " +
        weekDayName(
            getTodayDayOfWeek()
        )
}


private fun getGreeting(): String {

    val hour =
        Calendar
            .getInstance()
            .get(
                Calendar.HOUR_OF_DAY
            )

    return when (
        hour
    ) {
        in 5..10 -> "早上好"
        in 11..13 -> "中午好"
        in 14..17 -> "下午好"
        in 18..23 -> "晚上好"
        else -> "夜深了"
    }
}


private fun formatClockTime(
    millis: Long
): String {

    return SimpleDateFormat(
        "HH:mm",
        Locale.CHINA
    ).format(
        millis
    )
}


private fun weekDayName(
    day: Int
): String {

    return when (
        day
    ) {
        1 -> "星期一"
        2 -> "星期二"
        3 -> "星期三"
        4 -> "星期四"
        5 -> "星期五"
        6 -> "星期六"
        7 -> "星期日"
        else -> ""
    }
}

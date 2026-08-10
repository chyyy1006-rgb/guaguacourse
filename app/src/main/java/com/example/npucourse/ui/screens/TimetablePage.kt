package com.example.npucourse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.npucourse.model.DemoCourse
import com.example.npucourse.model.Semester
import com.example.npucourse.model.WeekMode
import com.example.npucourse.model.canonicalCustomWeeks
import com.example.npucourse.model.isActiveInWeek
import com.example.npucourse.model.parseCustomWeeks
import com.example.npucourse.model.weekDisplayText
import com.example.npucourse.data.settings.CourseCardStyle
import com.example.npucourse.data.settings.UiDensity
import com.example.npucourse.util.MAX_SEMESTER_WEEKS
import com.example.npucourse.util.campusDisplayName
import com.example.npucourse.util.getMaxSection
import com.example.npucourse.util.getSectionStartTime
import com.example.npucourse.util.semesterWeekDateMillis
import com.example.npucourse.util.todayStartMillis
import java.util.Calendar
import kotlinx.coroutines.launch


@Composable
fun TimetablePage(
    courses: List<DemoCourse>,
    currentWeek: Int,
    campus: String,
    semesterStartMillis: Long,
    selectedSemesterId: Long,
    selectedSemesterName: String,
    semesters: List<Semester>,
    uiDensity: String,
    courseCardStyle: String,
    showSectionTimes: Boolean,
    onSemesterSelected: (Long) -> Unit,
    onAddCourse: (DemoCourse) -> Unit,
    onUpdateCourse: (DemoCourse) -> Unit,
    onDeleteCourse: (Long) -> Unit
) {

    val lastCourseWeek =
        courses
            .maxOfOrNull {
                it.endWeek
            }
            ?.coerceIn(
                1,
                MAX_SEMESTER_WEEKS
            )
            ?: MAX_SEMESTER_WEEKS


    fun defaultDisplayedWeek(): Int {

        return when {

            currentWeek <= 0 ->
                1

            currentWeek > MAX_SEMESTER_WEEKS ->
                lastCourseWeek

            else ->
                currentWeek
        }
    }


    val pagerState =
        rememberPagerState(
            initialPage =
                defaultDisplayedWeek() - 1,
            pageCount = {
                MAX_SEMESTER_WEEKS
            }
        )


    val coroutineScope =
        rememberCoroutineScope()


    val displayedWeek =
        pagerState.currentPage + 1


    LaunchedEffect(
        currentWeek,
        selectedSemesterId,
        lastCourseWeek
    ) {

        pagerState.scrollToPage(
            defaultDisplayedWeek() - 1
        )
    }


    var selectedCourse by remember {
        mutableStateOf<DemoCourse?>(
            null
        )
    }


    var showAddDialog by remember {
        mutableStateOf(false)
    }


    var showWeekPicker by remember {
        mutableStateOf(false)
    }


    var editingCourse by remember {
        mutableStateOf<DemoCourse?>(
            null
        )
    }


    var deletingCourse by remember {
        mutableStateOf<DemoCourse?>(
            null
        )
    }


    Column(
        modifier =
            Modifier.fillMaxSize()
    ) {


        Spacer(
            modifier =
                Modifier.height(
                    14.dp
                )
        )


        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 20.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {


            Column(
                modifier =
                    Modifier.weight(1f)
            ) {


                Row(
                    modifier =
                        Modifier
                            .clickable {
                                showWeekPicker =
                                    true
                            }
                            .padding(
                                vertical = 2.dp
                            ),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            "第" +
                                displayedWeek +
                                "周",

                        fontSize =
                            28.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            MaterialTheme.colorScheme.onBackground
                    )


                    Text(
                        text =
                            "  ▾",

                        fontSize =
                            16.sp,

                        fontWeight =
                            FontWeight.SemiBold,

                        color =
                            MaterialTheme.colorScheme.primary
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            3.dp
                        )
                )


                Text(
                    text =
                        "西北工业大学 · " +
                            campusDisplayName(
                                campus
                            ),

                    fontSize =
                        14.sp,

                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }


            FilledTonalButton(
                onClick = {
                    showAddDialog =
                        true
                },

                contentPadding =
                    PaddingValues(
                        horizontal = 14.dp,
                        vertical = 8.dp
                    )
            ) {

                Text(
                    "＋ 添加"
                )
            }
        }


        SemesterSelector(
            selectedSemesterId =
                selectedSemesterId,

            selectedSemesterName =
                selectedSemesterName,

            semesters =
                semesters,

            onSemesterSelected =
                onSemesterSelected
        )


        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text =
                    when {

                        currentWeek <= 0 ->
                            "学期尚未开始"

                        currentWeek > MAX_SEMESTER_WEEKS ->
                            "本学期已结束"

                        displayedWeek == currentWeek ->
                            "正在查看当前周"

                        else ->
                            "当前第" +
                                currentWeek +
                                "周"
                    },

                modifier =
                    Modifier.weight(1f),

                fontSize =
                    12.sp,

                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )


            if (
                currentWeek in 1..MAX_SEMESTER_WEEKS &&
                displayedWeek != currentWeek
            ) {

                TextButton(
                    onClick = {

                        coroutineScope.launch {

                            pagerState
                                .animateScrollToPage(
                                    currentWeek - 1
                                )
                        }
                    }
                ) {

                    Text(
                        "回到本周"
                    )
                }
            }


            if (
                currentWeek > MAX_SEMESTER_WEEKS &&
                displayedWeek != lastCourseWeek
            ) {

                TextButton(
                    onClick = {

                        coroutineScope.launch {

                            pagerState
                                .animateScrollToPage(
                                    lastCourseWeek - 1
                                )
                        }
                    }
                ) {

                    Text(
                        "最后有课周"
                    )
                }
            }


            Text(
                text =
                    "左右滑动换周",

                fontSize =
                    10.sp,

                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }


        HorizontalPager(
            state =
                pagerState,

            modifier =
                Modifier.fillMaxSize()
        ) {
            page ->


            WeekSchedule(
                week =
                    page + 1,

                currentWeek =
                    currentWeek,

                semesterStartMillis =
                    semesterStartMillis,

                campus =
                    campus,

                courses =
                    courses,

                uiDensity = uiDensity,

                courseCardStyle = courseCardStyle,

                showSectionTimes = showSectionTimes,

                onCourseClick = {
                    course ->

                    selectedCourse =
                        course
                }
            )
        }
    }


    if (
        showWeekPicker
    ) {

        WeekPickerDialog(
            selectedWeek =
                displayedWeek,

            currentWeek =
                currentWeek,

            onDismiss = {
                showWeekPicker =
                    false
            },

            onWeekSelected = {
                week ->

                showWeekPicker =
                    false


                coroutineScope.launch {

                    pagerState
                        .animateScrollToPage(
                            week - 1
                        )
                }
            }
        )
    }


    selectedCourse
        ?.let {
            course ->


            CourseDetailDialog(
                course =
                    course,

                onDismiss = {
                    selectedCourse =
                        null
                },

                onEdit = {

                    selectedCourse =
                        null

                    editingCourse =
                        course
                },

                onDelete = {

                    selectedCourse =
                        null

                    deletingCourse =
                        course
                }
            )
        }


    if (
        showAddDialog
    ) {

        CourseFormDialog(
            title =
                "添加课程",

            initialCourse =
                null,

            maxSection =
                getMaxSection(
                    campus
                ),

            onDismiss = {

                showAddDialog =
                    false
            },

            onSave = {
                course ->

                onAddCourse(
                    course
                )

                showAddDialog =
                    false
            }
        )
    }


    editingCourse
        ?.let {
            course ->


            CourseFormDialog(
                title =
                    "编辑课程",

                initialCourse =
                    course,

                maxSection =
                    getMaxSection(
                        campus
                    ),

                onDismiss = {

                    editingCourse =
                        null
                },

                onSave = {
                    edited ->

                    onUpdateCourse(
                        edited
                    )

                    editingCourse =
                        null
                }
            )
        }


    deletingCourse
        ?.let {
            course ->


            AlertDialog(
                onDismissRequest = {
                    deletingCourse =
                        null
                },

                title = {
                    Text(
                        "删除课程？"
                    )
                },

                text = {
                    Text(
                        "确定删除“" +
                            course.name +
                            "”吗？"
                    )
                },

                dismissButton = {

                    TextButton(
                        onClick = {
                            deletingCourse =
                                null
                        }
                    ) {

                        Text(
                            "取消"
                        )
                    }
                },

                confirmButton = {

                    Button(
                        onClick = {

                            onDeleteCourse(
                                course.id
                            )

                            deletingCourse =
                                null
                        }
                    ) {

                        Text(
                            "删除"
                        )
                    }
                }
            )
        }
}


/* =========================================================
   多学期课表切换
   ========================================================= */

@Composable
private fun SemesterSelector(
    selectedSemesterId: Long,
    selectedSemesterName: String,
    semesters: List<Semester>,
    onSemesterSelected: (Long) -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 20.dp,
                    vertical = 4.dp
                )
    ) {

        OutlinedButton(
            modifier =
                Modifier.fillMaxWidth(),
            onClick = {
                expanded = true
            }
        ) {
            Text(
                text =
                    if (
                        selectedSemesterName.isBlank()
                    ) {
                        "选择课表"
                    } else {
                        selectedSemesterName + " ▼"
                    },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {

            semesters.forEach {
                semester ->

                DropdownMenuItem(
                    text = {
                        Text(
                            text =
                                if (
                                    semester.id ==
                                    selectedSemesterId
                                ) {
                                    "✓ " + semester.name
                                } else {
                                    semester.name
                                }
                        )
                    },
                    onClick = {
                        expanded = false
                        onSemesterSelected(
                            semester.id
                        )
                    }
                )
            }
        }
    }
}


/* =========================================================
   快速选择周次
   ========================================================= */

@Composable
private fun WeekPickerDialog(
    selectedWeek: Int,
    currentWeek: Int,
    onDismiss: () -> Unit,
    onWeekSelected: (Int) -> Unit
) {

    AlertDialog(
        onDismissRequest =
            onDismiss,

        title = {

            Text(
                text =
                    "快速选择周次",

                fontWeight =
                    FontWeight.Bold
            )
        },

        text = {

            Column {

                Text(
                    text =
                        if (
                            currentWeek in 1..MAX_SEMESTER_WEEKS
                        ) {

                            "当前第" +
                                currentWeek +
                                "周 · 也可以左右滑动切换"

                        } else {

                            "也可以在课表区域左右滑动切换"
                        },

                    fontSize =
                        12.sp,

                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )


                for (
                    rowIndex in 0 until 5
                ) {

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.spacedBy(
                                6.dp
                            )
                    ) {

                        for (
                            columnIndex in 0 until 4
                        ) {

                            val week =
                                rowIndex * 4 +
                                    columnIndex +
                                    1


                            FilterChip(
                                modifier =
                                    Modifier.weight(1f),

                                selected =
                                    week == selectedWeek,

                                onClick = {
                                    onWeekSelected(
                                        week
                                    )
                                },

                                label = {

                                    Text(
                                        text =
                                            if (
                                                week == currentWeek
                                            ) {

                                                week.toString() +
                                                    "·本"

                                            } else {

                                                week.toString()
                                            },

                                        fontSize =
                                            12.sp
                                    )
                                }
                            )
                        }
                    }
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


/* =========================================================
   一周课表
   ========================================================= */

@Composable
private fun WeekSchedule(
    week: Int,
    currentWeek: Int,
    semesterStartMillis: Long,
    campus: String,
    courses: List<DemoCourse>,
    uiDensity: String,
    courseCardStyle: String,
    showSectionTimes: Boolean,
    onCourseClick: (DemoCourse) -> Unit
) {

    val visibleCourses =
        courses.filter {
            course ->

            course.isActiveInWeek(
                week
            )
        }


    val verticalScroll =
        rememberScrollState()


    val maxSection =
        getMaxSection(
            campus
        )


    val sectionWidth =
        40.dp

    val sectionHeight =
        when (uiDensity) {
            UiDensity.COMPACT -> 58.dp
            UiDensity.COMFORTABLE -> 86.dp
            else -> 72.dp
        }


    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    verticalScroll
                )
    ) {


        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(
                        60.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {


            Box(
                modifier =
                    Modifier.width(
                        sectionWidth
                    ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =
                        formatWeekMonthLabel(
                            semesterStartMillis =
                                semesterStartMillis,

                            displayedWeek =
                                week
                        ),

                    fontSize =
                        10.sp,

                    fontWeight =
                        FontWeight.SemiBold,

                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }


            for (
                day in 1..7
            ) {

                val displayedDateMillis =
                    getDisplayedDateMillis(
                        semesterStartMillis =
                            semesterStartMillis,

                        displayedWeek =
                            week,

                        day =
                            day
                    )


                val isToday =
                    currentWeek in 1..MAX_SEMESTER_WEEKS &&
                        week == currentWeek &&
                        displayedDateMillis ==
                            todayStartMillis()


                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(
                                horizontal = 1.dp
                            )
                            .then(
                                if (
                                    isToday
                                ) {

                                    Modifier
                                        .background(
                                            color =
                                                MaterialTheme.colorScheme.primaryContainer,

                                            shape =
                                                RoundedCornerShape(
                                                    10.dp
                                                )
                                        )
                                        .padding(
                                            vertical = 5.dp
                                        )

                                } else {

                                    Modifier.padding(
                                        vertical = 5.dp
                                    )
                                }
                            ),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        text =
                            dayHeaderName(
                                day
                            ),

                        fontSize =
                            11.sp,

                        fontWeight =
                            FontWeight.SemiBold,

                        maxLines =
                            1,

                        color =
                            when {

                                isToday ->
                                    MaterialTheme.colorScheme.primary

                                day >= 6 ->
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)

                                else ->
                                    MaterialTheme.colorScheme.onSurface
                            }
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                3.dp
                            )
                    )


                    Text(
                        text =
                            formatDisplayedDay(
                                displayedDateMillis
                            ),

                        fontSize =
                            11.sp,

                        fontWeight =
                            if (
                                isToday
                            ) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            },

                        color =
                            if (
                                isToday
                            ) {

                                MaterialTheme.colorScheme.primary

                            } else {

                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                    )
                }
            }
        }


        Row(
            modifier =
                Modifier.fillMaxWidth()
        ) {


            SectionColumn(
                campus = campus,
                sectionHeight = sectionHeight,
                showSectionTimes = showSectionTimes
            )


            for (
                day in 1..7
            ) {


                DayColumn(
                    day =
                        day,

                    courses =
                        visibleCourses,

                    maxSection =
                        maxSection,

                    sectionHeight = sectionHeight,

                    courseCardStyle = courseCardStyle,

                    modifier =
                        Modifier.weight(1f),

                    onCourseClick =
                        onCourseClick
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(
                    30.dp
                )
        )
    }
}


/* =========================================================
   日期计算
   ========================================================= */

private fun getDisplayedDateMillis(
    semesterStartMillis: Long,
    displayedWeek: Int,
    day: Int
): Long {

    return semesterWeekDateMillis(
        semesterStartMillis =
            semesterStartMillis,

        week =
            displayedWeek,

        day =
            day
    )
}


private fun formatDisplayedDate(
    millis: Long
): String {

    val calendar =
        Calendar.getInstance().apply {
            timeInMillis =
                millis
        }


    return (
        calendar.get(
            Calendar.MONTH
        ) + 1
    ).toString() +
        "/" +
        calendar.get(
            Calendar.DAY_OF_MONTH
        )
}


private fun formatDisplayedDay(
    millis: Long
): String {

    val calendar =
        Calendar.getInstance().apply {
            timeInMillis =
                millis
        }


    return calendar
        .get(
            Calendar.DAY_OF_MONTH
        )
        .toString()
}


private fun formatWeekMonthLabel(
    semesterStartMillis: Long,
    displayedWeek: Int
): String {

    val monday =
        Calendar.getInstance().apply {

            timeInMillis =
                getDisplayedDateMillis(
                    semesterStartMillis =
                        semesterStartMillis,

                    displayedWeek =
                        displayedWeek,

                    day =
                        1
                )
        }


    val sunday =
        Calendar.getInstance().apply {

            timeInMillis =
                getDisplayedDateMillis(
                    semesterStartMillis =
                        semesterStartMillis,

                    displayedWeek =
                        displayedWeek,

                    day =
                        7
                )
        }


    val mondayMonth =
        monday.get(
            Calendar.MONTH
        ) + 1


    val sundayMonth =
        sunday.get(
            Calendar.MONTH
        ) + 1


    return if (
        mondayMonth == sundayMonth
    ) {

        mondayMonth.toString() +
            "月"

    } else {

        mondayMonth.toString() +
            "/" +
            sundayMonth +
            "月"
    }
}


private fun dayHeaderName(
    day: Int
): String {

    return when (day) {
        1 -> "周一"
        2 -> "周二"
        3 -> "周三"
        4 -> "周四"
        5 -> "周五"
        6 -> "周六"
        7 -> "周日"
        else -> ""
    }
}


/* =========================================================
   左侧节次
   ========================================================= */

@Composable
private fun SectionColumn(
    campus: String,
    sectionHeight: Dp,
    showSectionTimes: Boolean
) {

    val maxSection =
        getMaxSection(
            campus
        )


    Column(
        modifier =
            Modifier.width(
                40.dp
            )
    ) {


        for (
        section in 1..maxSection
        ) {


            Box(
                modifier =
                    Modifier
                        .height(
                            sectionHeight
                        )
                        .fillMaxWidth(),

                contentAlignment =
                    Alignment.TopCenter
            ) {


                Column(
                    modifier =
                        Modifier.padding(
                            top = 6.dp
                        ),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {


                    Text(
                        text =
                            section.toString(),

                        fontSize =
                            12.sp,

                        fontWeight =
                            FontWeight.SemiBold,

                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )


                    if (showSectionTimes) {
                        Spacer(
                            modifier =
                                Modifier.height(
                                    3.dp
                                )
                        )

                        Text(
                            text =
                                getSectionStartTime(
                                    campus,
                                    section
                                ),

                            fontSize =
                                9.sp,

                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}


/* =========================================================
   单日
   ========================================================= */

@Composable
private fun DayColumn(
    day: Int,
    courses: List<DemoCourse>,
    maxSection: Int,
    sectionHeight: Dp,
    courseCardStyle: String,
    modifier: Modifier = Modifier,
    onCourseClick: (DemoCourse) -> Unit
) {

    Column(
        modifier =
            modifier
    ) {


        var section =
            1


        while (
            section <=
            maxSection
        ) {


            val course =
                courses
                    .firstOrNull {

                        it.day ==
                                day &&
                                it.startSection ==
                                section
                    }


            if (
                course != null
            ) {


                val span =
                    (
                            course.endSection -
                                    course.startSection +
                                    1
                            )
                        .coerceAtLeast(
                            1
                        )
                        .coerceAtMost(
                            maxSection -
                                    section +
                                    1
                        )


                CourseBlock(
                    course =
                        course,

                    height =
                        sectionHeight * span,

                    courseCardStyle =
                        courseCardStyle,

                    onClick = {

                        onCourseClick(
                            course
                        )
                    }
                )


                section +=
                    span

            } else {


                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(
                                sectionHeight
                            )
                            .padding(
                                1.dp
                            )
                            .background(
                                color =
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),

                                shape =
                                    RoundedCornerShape(
                                        5.dp
                                    )
                            )
                )


                section++
            }
        }
    }
}


/* =========================================================
   课程块
   ========================================================= */

@Composable
private fun CourseBlock(
    course: DemoCourse,
    height: Dp,
    courseCardStyle: String,
    onClick: () -> Unit
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(
                    height
                )
                .padding(
                    1.dp
                )
                .clickable {
                    onClick()
                },

        shape =
            RoundedCornerShape(
                6.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    course.color.copy(
                        alpha = 0.18f
                    )
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
    ) {


        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        4.dp
                    )
        ) {


            Text(
                text =
                    course.name,

                fontSize =
                    9.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    MaterialTheme.colorScheme.onSurface,

                maxLines =
                    4,

                overflow =
                    TextOverflow.Ellipsis
            )


            if (courseCardStyle != CourseCardStyle.MINIMAL) {
                Spacer(
                    modifier =
                        Modifier.height(
                            3.dp
                        )
                )

                Text(
                    text =
                        course.room,

                    fontSize =
                        7.sp,

                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,

                    maxLines =
                        if (courseCardStyle == CourseCardStyle.DETAILED) 2 else 3,

                    overflow =
                        TextOverflow.Ellipsis
                )
            }

            if (
                courseCardStyle == CourseCardStyle.DETAILED &&
                course.teacher.isNotBlank()
            ) {
                Spacer(
                    modifier =
                        Modifier.height(2.dp)
                )

                Text(
                    text = course.teacher,
                    fontSize = 7.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


/* =========================================================
   课程详情
   ========================================================= */

@Composable
private fun CourseDetailDialog(
    course: DemoCourse,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
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
                    Arrangement.spacedBy(
                        12.dp
                    )
            ) {


                DetailRow(
                    title =
                        "时间",

                    value =
                        weekDayName(
                            course.day
                        ) +
                                " 第" +
                                course.startSection +
                                "–" +
                                course.endSection +
                                "节"
                )


                DetailRow(
                    title =
                        "教室",

                    value =
                        course.room
                )


                DetailRow(
                    title =
                        "教师",

                    value =
                        course.teacher
                )


                DetailRow(
                    title =
                        "周次",

                    value =
                        course.weekDisplayText()
                )
            }
        },

        dismissButton = {

            TextButton(
                onClick =
                    onDelete
            ) {

                Text(
                    "删除课程"
                )
            }
        },

        confirmButton = {

            Row {


                TextButton(
                    onClick =
                        onDismiss
                ) {

                    Text(
                        "关闭"
                    )
                }


                Button(
                    onClick =
                        onEdit
                ) {

                    Text(
                        "编辑"
                    )
                }
            }
        }
    )
}


@Composable
private fun DetailRow(
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
                MaterialTheme.colorScheme.onSurfaceVariant
        )


        Spacer(
            modifier =
                Modifier.height(
                    3.dp
                )
        )


        Text(
            text =
                value,

            fontSize =
                16.sp
        )
    }
}


/* =========================================================
   添加 / 编辑
   ========================================================= */

@Composable
private fun CourseFormDialog(
    title: String,
    initialCourse: DemoCourse?,
    maxSection: Int,
    onDismiss: () -> Unit,
    onSave: (DemoCourse) -> Unit
) {

    var name by remember(initialCourse) {
        mutableStateOf(
            initialCourse?.name ?: ""
        )
    }

    var teacher by remember(initialCourse) {
        mutableStateOf(
            initialCourse?.teacher ?: ""
        )
    }

    var room by remember(initialCourse) {
        mutableStateOf(
            initialCourse?.room ?: ""
        )
    }

    var day by remember(initialCourse) {
        mutableIntStateOf(
            initialCourse?.day ?: 1
        )
    }

    var startSection by remember(initialCourse) {
        mutableIntStateOf(
            initialCourse?.startSection ?: 1
        )
    }

    var endSection by remember(initialCourse) {
        mutableIntStateOf(
            initialCourse?.endSection ?: 2
        )
    }

    var startWeek by remember(initialCourse) {
        mutableIntStateOf(
            initialCourse?.startWeek ?: 1
        )
    }

    var endWeek by remember(initialCourse) {
        mutableIntStateOf(
            initialCourse?.endWeek ?: 16
        )
    }

    var weekMode by remember(initialCourse) {
        mutableStateOf(
            initialCourse?.weekMode
                ?: WeekMode.EVERY
        )
    }

    var customWeeks by remember(initialCourse) {
        mutableStateOf(
            initialCourse?.customWeeks
                ?: ""
        )
    }


    val parsedCustomWeeks =
        parseCustomWeeks(
            customWeeks
        )


    val validWeeks =
        when (
            weekMode
        ) {

            WeekMode.EVERY ->
                startWeek <= endWeek

            WeekMode.ODD ->
                (startWeek..endWeek)
                    .any {
                        it % 2 == 1
                    }

            WeekMode.EVEN ->
                (startWeek..endWeek)
                    .any {
                        it % 2 == 0
                    }

            WeekMode.CUSTOM ->
                parsedCustomWeeks
                    .isNotEmpty()
        }


    val canSave =
        name.isNotBlank() &&
                startSection <= endSection &&
                validWeeks


    Dialog(
        onDismissRequest =
            onDismiss
    ) {


        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(
                        max = 700.dp
                    ),

            shape =
                RoundedCornerShape(
                    28.dp
                )
        ) {


            Column(
                modifier =
                    Modifier
                        .verticalScroll(
                            rememberScrollState()
                        )
                        .padding(
                            22.dp
                        )
            ) {


                Text(
                    text =
                        title,

                    fontSize =
                        26.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            20.dp
                        )
                )


                OutlinedTextField(
                    value =
                        name,

                    onValueChange = {
                        name = it
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    label = {
                        Text(
                            "课程名称"
                        )
                    },

                    singleLine =
                        true
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            10.dp
                        )
                )


                OutlinedTextField(
                    value =
                        teacher,

                    onValueChange = {
                        teacher = it
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    label = {
                        Text(
                            "教师"
                        )
                    },

                    singleLine =
                        true
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            10.dp
                        )
                )


                OutlinedTextField(
                    value =
                        room,

                    onValueChange = {
                        room = it
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    label = {
                        Text(
                            "教室"
                        )
                    },

                    singleLine =
                        true
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            20.dp
                        )
                )


                Text(
                    text =
                        "星期",

                    fontWeight =
                        FontWeight.SemiBold
                )


                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            4.dp
                        )
                ) {


                    for (
                    item in 1..4
                    ) {

                        FilterChip(
                            modifier =
                                Modifier.weight(
                                    1f
                                ),

                            selected =
                                day == item,

                            onClick = {
                                day = item
                            },

                            label = {
                                Text(
                                    dayShortName(
                                        item
                                    )
                                )
                            }
                        )
                    }
                }


                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            4.dp
                        )
                ) {


                    for (
                    item in 5..7
                    ) {

                        FilterChip(
                            modifier =
                                Modifier.weight(
                                    1f
                                ),

                            selected =
                                day == item,

                            onClick = {
                                day = item
                            },

                            label = {
                                Text(
                                    dayShortName(
                                        item
                                    )
                                )
                            }
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            18.dp
                        )
                )


                StepperRow(
                    title =
                        "开始节次",

                    value =
                        startSection,

                    min =
                        1,

                    max =
                        maxSection,

                    onChange = {
                            value ->

                        startSection =
                            value

                        if (
                            endSection <
                            value
                        ) {

                            endSection =
                                value
                        }
                    }
                )


                StepperRow(
                    title =
                        "结束节次",

                    value =
                        endSection,

                    min =
                        startSection,

                    max =
                        maxSection,

                    onChange = {
                        endSection = it
                    }
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            18.dp
                        )
                )


                Text(
                    text =
                        "周次类型",

                    fontWeight =
                        FontWeight.SemiBold
                )


                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            4.dp
                        )
                ) {


                    WeekMode.values()
                        .forEach {
                                mode ->


                            FilterChip(
                                modifier =
                                    Modifier.weight(
                                        1f
                                    ),

                                selected =
                                    weekMode ==
                                            mode,

                                onClick = {
                                    weekMode =
                                        mode
                                },

                                label = {

                                    Text(
                                        weekModeName(
                                            mode
                                        )
                                    )
                                }
                            )
                        }
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )


                if (
                    weekMode ==
                    WeekMode.CUSTOM
                ) {

                    OutlinedTextField(
                        value =
                            customWeeks,

                        onValueChange = {
                            customWeeks = it
                        },

                        modifier =
                            Modifier.fillMaxWidth(),

                        label = {
                            Text(
                                "例如：1,3,5,8-10"
                            )
                        }
                    )

                } else {


                    StepperRow(
                        title =
                            "开始周",

                        value =
                            startWeek,

                        min =
                            1,

                        max =
                            20,

                        onChange = {
                                value ->

                            startWeek =
                                value

                            if (
                                endWeek <
                                value
                            ) {

                                endWeek =
                                    value
                            }
                        }
                    )


                    StepperRow(
                        title =
                            "结束周",

                        value =
                            endWeek,

                        min =
                            startWeek,

                        max =
                            20,

                        onChange = {
                            endWeek = it
                        }
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            22.dp
                        )
                )


                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.End
                ) {


                    TextButton(
                        onClick =
                            onDismiss
                    ) {

                        Text(
                            "取消"
                        )
                    }


                    Button(
                        enabled =
                            canSave,

                        onClick = {


                            val finalWeeks =
                                if (
                                    weekMode ==
                                    WeekMode.CUSTOM
                                ) {

                                    parsedCustomWeeks

                                } else {

                                    emptyList()
                                }


                            onSave(

                                DemoCourse(

                                    id =
                                        initialCourse?.id
                                            ?: System
                                                .currentTimeMillis(),

                                    name =
                                        name.trim(),

                                    room =
                                        room.trim()
                                            .ifBlank {
                                                "未填写"
                                            },

                                    teacher =
                                        teacher.trim()
                                            .ifBlank {
                                                "未填写"
                                            },

                                    day =
                                        day,

                                    startSection =
                                        startSection,

                                    endSection =
                                        endSection,

                                    startWeek =
                                        if (
                                            finalWeeks
                                                .isNotEmpty()
                                        ) {

                                            finalWeeks.first()

                                        } else {

                                            startWeek
                                        },

                                    endWeek =
                                        if (
                                            finalWeeks
                                                .isNotEmpty()
                                        ) {

                                            finalWeeks.last()

                                        } else {

                                            endWeek
                                        },

                                    color =
                                        initialCourse
                                            ?.color
                                            ?: pickCourseColor(
                                                name
                                            ),

                                    weekMode =
                                        weekMode,

                                    customWeeks =
                                        if (
                                            weekMode ==
                                            WeekMode.CUSTOM
                                        ) {

                                            canonicalCustomWeeks(
                                                customWeeks
                                            )

                                        } else {

                                            ""
                                        },

                                    notes = initialCourse?.notes ?: "",

                                    reminderEnabled =
                                        initialCourse?.reminderEnabled ?: true,

                                    reminderMinutesOverride =
                                        initialCourse?.reminderMinutesOverride ?: -1
                                )
                            )
                        }
                    ) {

                        Text(
                            "保存"
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun StepperRow(
    title: String,
    value: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 5.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {


        Text(
            text =
                "$title：$value",

            modifier =
                Modifier.weight(
                    1f
                )
        )


        OutlinedButton(
            enabled =
                value > min,

            onClick = {
                onChange(
                    value - 1
                )
            }
        ) {

            Text(
                "－"
            )
        }


        Spacer(
            modifier =
                Modifier.width(
                    6.dp
                )
        )


        OutlinedButton(
            enabled =
                value < max,

            onClick = {
                onChange(
                    value + 1
                )
            }
        ) {

            Text(
                "＋"
            )
        }
    }
}


/* =========================================================
   Helper
   ========================================================= */

private fun calendarDayToCourseDay(
    calendarDay: Int
): Int {

    return when (
        calendarDay
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


private fun dayShortName(
    day: Int
): String {

    return when (day) {
        1 -> "一"
        2 -> "二"
        3 -> "三"
        4 -> "四"
        5 -> "五"
        6 -> "六"
        7 -> "日"
        else -> ""
    }
}


private fun weekDayName(
    day: Int
): String {

    return when (day) {
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


private fun weekModeName(
    mode: WeekMode
): String {

    return when (mode) {
        WeekMode.EVERY -> "每周"
        WeekMode.ODD -> "单周"
        WeekMode.EVEN -> "双周"
        WeekMode.CUSTOM -> "自定义"
    }
}


private fun pickCourseColor(
    name: String
): Color {

    val colors =
        listOf(
            Color(0xFF6377F4),
            Color(0xFF50A487),
            Color(0xFF8A69D4),
            Color(0xFFE58A5D),
            Color(0xFF4F95CA),
            Color(0xFFD3698D),
            Color(0xFFE2A94B),
            Color(0xFF55A0A6)
        )


    return colors[
        name
            .hashCode()
            .ushr(1) %
                colors.size
    ]
}
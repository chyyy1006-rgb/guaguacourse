package com.example.npucourse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.window.Dialog
import com.example.npucourse.model.DemoCourse
import com.example.npucourse.model.WeekMode
import com.example.npucourse.model.canonicalCustomWeeks
import com.example.npucourse.model.parseCustomWeeks
import com.example.npucourse.model.weekDisplayText
import com.example.npucourse.util.campusDisplayName
import com.example.npucourse.util.getMaxSection
import com.example.npucourse.util.getScheduleForCampus


@Composable
fun CourseManagementPage(
    semesterId: Long,
    semesterName: String,
    campus: String,
    courses: List<DemoCourse>,
    onBack: () -> Unit,
    onAddCourse: (DemoCourse) -> Unit,
    onUpdateCourse: (DemoCourse) -> Unit,
    onDeleteCourse: (Long) -> Unit
) {

    var query by remember {
        mutableStateOf("")
    }

    var selectedDay by remember {
        mutableIntStateOf(0)
    }

    var selectedCourse by remember {
        mutableStateOf<DemoCourse?>(null)
    }

    var editingCourse by remember {
        mutableStateOf<DemoCourse?>(null)
    }

    var showAddDialog by remember {
        mutableStateOf(false)
    }

    var deletingCourse by remember {
        mutableStateOf<DemoCourse?>(null)
    }

    val normalizedQuery =
        query.trim()

    val filteredCourses =
        courses
            .filter { course ->
                selectedDay == 0 ||
                    course.day == selectedDay
            }
            .filter { course ->
                if (
                    normalizedQuery.isBlank()
                ) {
                    true
                } else {
                    course.name.contains(
                        normalizedQuery,
                        ignoreCase = true
                    ) ||
                        course.teacher.contains(
                            normalizedQuery,
                            ignoreCase = true
                        ) ||
                        course.room.contains(
                            normalizedQuery,
                            ignoreCase = true
                        ) ||
                        course.notes.contains(
                            normalizedQuery,
                            ignoreCase = true
                        )
                }
            }
            .sortedWith(
                compareBy<DemoCourse> {
                    it.day
                }
                    .thenBy {
                        it.startSection
                    }
                    .thenBy {
                        it.endSection
                    }
                    .thenBy {
                        it.name
                    }
            )

    val uniqueCourseCount =
        courses
            .map {
                it.name.trim()
            }
            .filter {
                it.isNotBlank()
            }
            .distinct()
            .size

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
                Modifier.height(18.dp)
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            OutlinedButton(
                onClick = onBack
            ) {
                Text("← 返回")
            }

            Button(
                onClick = {
                    showAddDialog = true
                }
            ) {
                Text("＋ 添加课程")
            }
        }

        Spacer(
            modifier =
                Modifier.height(18.dp)
        )

        Text(
            text = "课程管理",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(
            modifier =
                Modifier.height(5.dp)
        )

        Text(
            text = semesterName,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(
            modifier =
                Modifier.height(15.dp)
        )

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            CourseStatPill(
                text = courses.size.toString() + " 条安排"
            )

            CourseStatPill(
                text = uniqueCourseCount.toString() + " 门课程"
            )

            CourseStatPill(
                text = campusDisplayName(campus)
            )
        }

        Spacer(
            modifier =
                Modifier.height(18.dp)
        )

        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
            },
            modifier =
                Modifier.fillMaxWidth(),
            singleLine = true,
            label = {
                Text("搜索课程 / 教师 / 教室 / 备注")
            },
            trailingIcon = {
                if (
                    query.isNotBlank()
                ) {
                    Text(
                        text = "清除",
                        modifier =
                            Modifier
                                .clickable {
                                    query = ""
                                }
                                .padding(8.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(
                        rememberScrollState()
                    ),
            horizontalArrangement =
                Arrangement.spacedBy(7.dp)
        ) {

            FilterChip(
                selected = selectedDay == 0,
                onClick = {
                    selectedDay = 0
                },
                label = {
                    Text("全部")
                }
            )

            for (
                day in 1..7
            ) {
                FilterChip(
                    selected = selectedDay == day,
                    onClick = {
                        selectedDay = day
                    },
                    label = {
                        Text(
                            weekDayShortName(day)
                        )
                    }
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(18.dp)
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
                text = "课程安排",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = filteredCourses.size.toString() + " 条",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        if (
            filteredCourses.isEmpty()
        ) {
            EmptyCourseManagementCard(
                hasAnyCourse = courses.isNotEmpty(),
                query = normalizedQuery,
                selectedDay = selectedDay
            )
        } else {
            var lastDay = -1

            filteredCourses.forEach { course ->

                if (
                    selectedDay == 0 &&
                    lastDay != course.day
                ) {
                    if (
                        lastDay != -1
                    ) {
                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )
                    }

                    Text(
                        text = weekDayFullName(course.day),
                        modifier =
                            Modifier.padding(
                                horizontal = 2.dp,
                                vertical = 6.dp
                            ),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    lastDay = course.day
                }

                ManagedCourseCard(
                    course = course,
                    campus = campus,
                    onClick = {
                        selectedCourse = course
                    }
                )

                Spacer(
                    modifier =
                        Modifier.height(9.dp)
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(42.dp)
        )
    }


    selectedCourse?.let { course ->
        ManagedCourseDetailDialog(
            course = course,
            campus = campus,
            onDismiss = {
                selectedCourse = null
            },
            onEdit = {
                selectedCourse = null
                editingCourse = course
            },
            onDelete = {
                selectedCourse = null
                deletingCourse = course
            }
        )
    }


    if (
        showAddDialog
    ) {
        CourseManagementEditorDialog(
            title = "添加课程",
            semesterId = semesterId,
            campus = campus,
            initialCourse = null,
            onDismiss = {
                showAddDialog = false
            },
            onSave = { course ->
                onAddCourse(course)
                showAddDialog = false
            }
        )
    }


    editingCourse?.let { course ->
        CourseManagementEditorDialog(
            title = "编辑课程",
            semesterId = semesterId,
            campus = campus,
            initialCourse = course,
            onDismiss = {
                editingCourse = null
            },
            onSave = { edited ->
                onUpdateCourse(edited)
                editingCourse = null
            }
        )
    }


    deletingCourse?.let { course ->
        AlertDialog(
            onDismissRequest = {
                deletingCourse = null
            },
            title = {
                Text("删除课程？")
            },
            text = {
                Text(
                    "确定删除“" +
                        course.name +
                        "”这条课程安排吗？"
                )
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deletingCourse = null
                    }
                ) {
                    Text("取消")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCourse(course.id)
                        deletingCourse = null
                    }
                ) {
                    Text("删除")
                }
            }
        )
    }
}


@Composable
private fun CourseStatPill(
    text: String
) {
    Box(
        modifier =
            Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50)
                )
                .padding(
                    horizontal = 10.dp,
                    vertical = 6.dp
                )
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
private fun ManagedCourseCard(
    course: DemoCourse,
    campus: String,
    onClick: () -> Unit
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                },
        shape = RoundedCornerShape(18.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Box(
                modifier =
                    Modifier
                        .width(5.dp)
                        .height(66.dp)
                        .background(
                            color = course.color,
                            shape = RoundedCornerShape(50)
                        )
            )

            Spacer(
                modifier =
                    Modifier.width(12.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {
                Text(
                    text = course.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(
                    modifier =
                        Modifier.height(5.dp)
                )

                Text(
                    text =
                        weekDayFullName(course.day) +
                            " · 第" +
                            course.startSection +
                            "–" +
                            course.endSection +
                            "节 · " +
                            getCourseClockText(
                                campus,
                                course
                            ),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Text(
                    text =
                        course.weekDisplayText() +
                            " · " +
                            course.room.ifBlank {
                                "未填写教室"
                            } +
                            " · " +
                            course.teacher.ifBlank {
                                "未填写教师"
                            },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


@Composable
private fun EmptyCourseManagementCard(
    hasAnyCourse: Boolean,
    query: String,
    selectedDay: Int
) {
    Card(
        modifier =
            Modifier.fillMaxWidth(),
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
                Modifier.padding(22.dp)
        ) {
            Text(
                text =
                    if (
                        hasAnyCourse
                    ) {
                        "没有匹配的课程"
                    } else {
                        "当前课表还没有课程"
                    },
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(
                modifier =
                    Modifier.height(7.dp)
            )

            Text(
                text =
                    when {
                        query.isNotBlank() ->
                            "换一个关键词试试，支持课程名、教师和教室。"

                        selectedDay != 0 ->
                            weekDayFullName(selectedDay) +
                                "没有符合条件的课程安排。"

                        else ->
                            "可以从右上角手动添加，或在课程表同步中从翱翔教务导入。"
                    },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun ManagedCourseDetailDialog(
    course: DemoCourse,
    campus: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = course.name,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                ManagedDetailRow(
                    title = "时间",
                    value =
                        weekDayFullName(course.day) +
                            " 第" +
                            course.startSection +
                            "–" +
                            course.endSection +
                            "节 · " +
                            getCourseClockText(
                                campus,
                                course
                            )
                )

                ManagedDetailRow(
                    title = "周次",
                    value = course.weekDisplayText()
                )

                ManagedDetailRow(
                    title = "教室",
                    value = course.room.ifBlank {
                        "未填写"
                    }
                )

                ManagedDetailRow(
                    title = "教师",
                    value = course.teacher.ifBlank {
                        "未填写"
                    }
                )

                ManagedDetailRow(
                    title = "提醒",
                    value = when {
                        !course.reminderEnabled -> "本课程关闭提醒"
                        course.reminderMinutesOverride >= 0 -> "提前 ${course.reminderMinutesOverride} 分钟"
                        else -> "跟随全局设置"
                    }
                )

                if (course.notes.isNotBlank()) {
                    ManagedDetailRow(
                        title = "备注",
                        value = course.notes
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDelete
            ) {
                Text("删除")
            }
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text("关闭")
                }

                Button(
                    onClick = onEdit
                ) {
                    Text("编辑")
                }
            }
        }
    )
}


@Composable
private fun ManagedDetailRow(
    title: String,
    value: String
) {
    Column {
        Text(
            text = title,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(
            modifier =
                Modifier.height(2.dp)
        )

        Text(
            text = value,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}


@Composable
private fun CourseManagementEditorDialog(
    title: String,
    semesterId: Long,
    campus: String,
    initialCourse: DemoCourse?,
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

    var notes by remember(initialCourse) {
        mutableStateOf(
            initialCourse?.notes ?: ""
        )
    }

    var reminderEnabled by remember(initialCourse) {
        mutableStateOf(
            initialCourse?.reminderEnabled ?: true
        )
    }

    var reminderMinutesOverride by remember(initialCourse) {
        mutableIntStateOf(
            initialCourse?.reminderMinutesOverride ?: -1
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
            initialCourse?.weekMode ?: WeekMode.EVERY
        )
    }

    var customWeeks by remember(initialCourse) {
        mutableStateOf(
            initialCourse?.customWeeks ?: ""
        )
    }

    var selectedColor by remember(initialCourse) {
        mutableStateOf(
            initialCourse?.color ?: COURSE_MANAGER_COLORS.first()
        )
    }

    val maxSection =
        getMaxSection(campus)

    val parsedCustomWeeks =
        parseCustomWeeks(customWeeks)

    val validWeeks =
        when (weekMode) {
            WeekMode.CUSTOM ->
                parsedCustomWeeks.isNotEmpty()

            else ->
                startWeek <= endWeek
        }

    val canSave =
        name.isNotBlank() &&
            startSection <= endSection &&
            validWeeks

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(
                        max = 720.dp
                    ),
            shape = RoundedCornerShape(26.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
        ) {
            Column(
                modifier =
                    Modifier
                        .verticalScroll(
                            rememberScrollState()
                        )
                        .padding(20.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    label = {
                        Text("课程名称")
                    },
                    singleLine = true
                )

                Spacer(
                    modifier =
                        Modifier.height(9.dp)
                )

                OutlinedTextField(
                    value = teacher,
                    onValueChange = {
                        teacher = it
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    label = {
                        Text("教师")
                    },
                    singleLine = true
                )

                Spacer(
                    modifier =
                        Modifier.height(9.dp)
                )

                OutlinedTextField(
                    value = room,
                    onValueChange = {
                        room = it
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    label = {
                        Text("教室")
                    },
                    singleLine = true
                )

                Spacer(Modifier.height(9.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("课程备注（可选）") },
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(
                    modifier =
                        Modifier.height(17.dp)
                )

                EditorSectionTitle("星期")

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(
                                rememberScrollState()
                            ),
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    for (
                        item in 1..7
                    ) {
                        FilterChip(
                            selected = day == item,
                            onClick = {
                                day = item
                            },
                            label = {
                                Text(
                                    weekDayShortName(item)
                                )
                            }
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(13.dp)
                )

                EditorStepperRow(
                    title = "开始节次",
                    value = startSection,
                    min = 1,
                    max = maxSection,
                    onChange = { value ->
                        startSection = value
                        if (
                            endSection < value
                        ) {
                            endSection = value
                        }
                    }
                )

                EditorStepperRow(
                    title = "结束节次",
                    value = endSection,
                    min = startSection,
                    max = maxSection,
                    onChange = {
                        endSection = it
                    }
                )

                Spacer(
                    modifier =
                        Modifier.height(15.dp)
                )

                EditorSectionTitle("周次类型")

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(
                                rememberScrollState()
                            ),
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    WeekMode.entries.forEach { mode ->
                        FilterChip(
                            selected = weekMode == mode,
                            onClick = {
                                weekMode = mode
                            },
                            label = {
                                Text(
                                    weekModeDisplayName(mode)
                                )
                            }
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                if (
                    weekMode == WeekMode.CUSTOM
                ) {
                    OutlinedTextField(
                        value = customWeeks,
                        onValueChange = {
                            customWeeks = it
                        },
                        modifier =
                            Modifier.fillMaxWidth(),
                        label = {
                            Text("周次，例如 1,3,5,8-10")
                        },
                        supportingText = {
                            Text(
                                if (
                                    parsedCustomWeeks.isEmpty()
                                ) {
                                    "请输入 1～20 周中的有效周次"
                                } else {
                                    "已识别 " +
                                        parsedCustomWeeks.size +
                                        " 个教学周"
                                }
                            )
                        }
                    )
                } else {
                    EditorStepperRow(
                        title = "开始周",
                        value = startWeek,
                        min = 1,
                        max = 20,
                        onChange = { value ->
                            startWeek = value
                            if (
                                endWeek < value
                            ) {
                                endWeek = value
                            }
                        }
                    )

                    EditorStepperRow(
                        title = "结束周",
                        value = endWeek,
                        min = startWeek,
                        max = 20,
                        onChange = {
                            endWeek = it
                        }
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                EditorSectionTitle("课程提醒")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = reminderEnabled,
                        onClick = { reminderEnabled = true },
                        label = { Text("开启") }
                    )
                    FilterChip(
                        selected = !reminderEnabled,
                        onClick = { reminderEnabled = false },
                        label = { Text("关闭") }
                    )
                }

                if (reminderEnabled) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "提前时间",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(5.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(-1, 0, 5, 10, 15, 30).forEach { minutes ->
                            FilterChip(
                                selected = reminderMinutesOverride == minutes,
                                onClick = { reminderMinutesOverride = minutes },
                                label = {
                                    Text(if (minutes < 0) "跟随全局" else "${minutes}分钟")
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                EditorSectionTitle("课程颜色")

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(
                                rememberScrollState()
                            ),
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    COURSE_MANAGER_COLORS.forEach { color ->
                        val selected =
                            selectedColor == color

                        Box(
                            modifier =
                                Modifier
                                    .size(34.dp)
                                    .border(
                                        width =
                                            if (selected) {
                                                3.dp
                                            } else {
                                                1.dp
                                            },
                                        color =
                                            if (selected) {
                                                MaterialTheme.colorScheme.onSurface
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            },
                                        shape = CircleShape
                                    )
                                    .padding(4.dp)
                                    .background(
                                        color = color,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        selectedColor = color
                                    }
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(22.dp)
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("取消")
                    }

                    Button(
                        enabled = canSave,
                        onClick = {
                            val finalWeeks =
                                if (
                                    weekMode == WeekMode.CUSTOM
                                ) {
                                    parsedCustomWeeks
                                } else {
                                    emptyList()
                                }

                            onSave(
                                DemoCourse(
                                    id =
                                        initialCourse?.id ?: 0L,
                                    name = name.trim(),
                                    room = room.trim(),
                                    teacher = teacher.trim(),
                                    day = day,
                                    startSection = startSection,
                                    endSection = endSection,
                                    startWeek =
                                        if (
                                            finalWeeks.isNotEmpty()
                                        ) {
                                            finalWeeks.first()
                                        } else {
                                            startWeek
                                        },
                                    endWeek =
                                        if (
                                            finalWeeks.isNotEmpty()
                                        ) {
                                            finalWeeks.last()
                                        } else {
                                            endWeek
                                        },
                                    color = selectedColor,
                                    weekMode = weekMode,
                                    customWeeks =
                                        if (
                                            weekMode == WeekMode.CUSTOM
                                        ) {
                                            canonicalCustomWeeks(
                                                customWeeks
                                            )
                                        } else {
                                            ""
                                        },
                                    semesterId = semesterId,
                                    notes = notes.trim(),
                                    reminderEnabled = reminderEnabled,
                                    reminderMinutesOverride = reminderMinutesOverride
                                )
                            )
                        }
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}


@Composable
private fun EditorSectionTitle(
    text: String
) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(
        modifier =
            Modifier.height(7.dp)
    )
}


@Composable
private fun EditorStepperRow(
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
                    vertical = 4.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Text(
            text = title + "：" + value,
            modifier =
                Modifier.weight(1f),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedButton(
            enabled = value > min,
            onClick = {
                onChange(value - 1)
            }
        ) {
            Text("－")
        }

        Spacer(
            modifier =
                Modifier.width(6.dp)
        )

        OutlinedButton(
            enabled = value < max,
            onClick = {
                onChange(value + 1)
            }
        ) {
            Text("＋")
        }
    }
}


private fun getCourseClockText(
    campus: String,
    course: DemoCourse
): String {
    val schedule =
        getScheduleForCampus(
            campus
        )

    val start =
        schedule
            .firstOrNull {
                it.section == course.startSection
            }
            ?.startTime
            ?: "--:--"

    val end =
        schedule
            .firstOrNull {
                it.section == course.endSection
            }
            ?.endTime
            ?: "--:--"

    return start + "–" + end
}


private fun weekDayShortName(
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
        else -> "?"
    }
}


private fun weekDayFullName(
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
        else -> "未知星期"
    }
}


private fun weekModeDisplayName(
    mode: WeekMode
): String {
    return when (mode) {
        WeekMode.EVERY -> "每周"
        WeekMode.ODD -> "单周"
        WeekMode.EVEN -> "双周"
        WeekMode.CUSTOM -> "自定义"
    }
}


private val COURSE_MANAGER_COLORS =
    listOf(
        Color(0xFF6377F4),
        Color(0xFF50A487),
        Color(0xFF8A69D4),
        Color(0xFFE58A5D),
        Color(0xFF4F95CA),
        Color(0xFFD3698D),
        Color(0xFFE2A94B),
        Color(0xFF55A0A6),
        Color(0xFFB07B5B),
        Color(0xFF7C879C)
    )

package com.example.npucourse.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.os.Build
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.npucourse.data.TaskEntity
import com.example.npucourse.model.DemoCourse
import com.example.npucourse.notification.NotificationHelper
import com.example.npucourse.notification.ReminderPermissionManager
import com.example.npucourse.ui.components.ReminderPermissionDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class TaskFilter {
    OPEN,
    ALL,
    DONE
}

@Composable
fun TaskManagementPage(
    semesterName: String,
    semesterId: Long,
    courses: List<DemoCourse>,
    tasks: List<TaskEntity>,
    onAddTask: (TaskEntity) -> Unit,
    onUpdateTask: (TaskEntity) -> Unit,
    onToggleTask: (Long, Boolean) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onBack: () -> Unit
) {
    var filter by remember { mutableStateOf(TaskFilter.OPEN) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    val visibleTasks = tasks
        .filter { it.semesterId == semesterId }
        .filter {
            when (filter) {
                TaskFilter.OPEN -> !it.completed
                TaskFilter.ALL -> true
                TaskFilter.DONE -> it.completed
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("返回")
            }
            TextButton(
                onClick = {
                    editingTask = null
                    showEditor = true
                }
            ) {
                Text("新增待办")
            }
        }

        Text(
            text = "课程待办",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = semesterName,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(18.dp))

        val semesterTasks = tasks.filter { it.semesterId == semesterId }
        val openCount = semesterTasks.count { !it.completed }
        val overdueCount = semesterTasks.count {
            !it.completed && it.dueAt > 0L && it.dueAt < System.currentTimeMillis()
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    text = "待完成 $openCount 项",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (overdueCount > 0) {
                        "其中 $overdueCount 项已逾期"
                    } else {
                        "当前没有逾期待办"
                    },
                    color = if (overdueCount > 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChipLike("未完成", filter == TaskFilter.OPEN) { filter = TaskFilter.OPEN }
            FilterChipLike("全部", filter == TaskFilter.ALL) { filter = TaskFilter.ALL }
            FilterChipLike("已完成", filter == TaskFilter.DONE) { filter = TaskFilter.DONE }
        }

        Spacer(Modifier.height(14.dp))

        if (visibleTasks.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        text = when (filter) {
                            TaskFilter.OPEN -> "没有未完成待办"
                            TaskFilter.ALL -> "还没有创建待办"
                            TaskFilter.DONE -> "还没有已完成待办"
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = "可以把作业、实验报告、预习或其他课程事项绑定到具体课程。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            visibleTasks.forEach { task ->
                val courseName = courses
                    .firstOrNull { it.id == task.courseId }
                    ?.name
                    ?: "通用待办"

                TaskCard(
                    task = task,
                    courseName = courseName,
                    onToggle = { checked -> onToggleTask(task.id, checked) },
                    onEdit = {
                        editingTask = task
                        showEditor = true
                    },
                    onDelete = { onDeleteTask(task.id) }
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    if (showEditor) {
        TaskEditorDialog(
            semesterId = semesterId,
            courses = courses,
            initialTask = editingTask,
            onDismiss = { showEditor = false },
            onSave = { task ->
                if (task.id == 0L) onAddTask(task) else onUpdateTask(task)
                showEditor = false
            }
        )
    }
}

@Composable
private fun FilterChipLike(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun TaskCard(
    task: TaskEntity,
    courseName: String,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val overdue = !task.completed && task.dueAt > 0L && task.dueAt < System.currentTimeMillis()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked = task.completed,
                    onCheckedChange = onToggle
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 4.dp)
                ) {
                    Text(
                        text = task.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else null
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = courseName + " · " + priorityText(task.priority),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (task.dueAt > 0L) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = (if (overdue) "已逾期 · " else "截止 · ") + formatDueAt(task.dueAt),
                            fontSize = 13.sp,
                            color = if (overdue) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (task.reminderMinutesBefore >= 0) {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "提醒 · ${taskReminderText(task.reminderMinutesBefore)}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (task.note.isNotBlank()) {
                        Spacer(Modifier.height(7.dp))
                        Text(
                            text = task.note,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onEdit) { Text("编辑") }
                TextButton(onClick = onDelete) { Text("删除") }
            }
        }
    }
}

@Composable
private fun TaskEditorDialog(
    semesterId: Long,
    courses: List<DemoCourse>,
    initialTask: TaskEntity?,
    onDismiss: () -> Unit,
    onSave: (TaskEntity) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val hostView = LocalView.current
    var title by remember(initialTask?.id) { mutableStateOf(initialTask?.title ?: "") }
    var note by remember(initialTask?.id) { mutableStateOf(initialTask?.note ?: "") }
    var selectedCourseId by remember(initialTask?.id) {
        mutableStateOf(initialTask?.courseId)
    }
    var priority by remember(initialTask?.id) {
        mutableStateOf(initialTask?.priority ?: 1)
    }
    var reminderMinutesBefore by remember(initialTask?.id) {
        mutableStateOf(initialTask?.reminderMinutesBefore ?: -1)
    }
    var reminderMenuExpanded by remember { mutableStateOf(false) }

    var permissionRefreshToken by remember { mutableIntStateOf(0) }
    var showReminderPermissionDialog by remember { mutableStateOf(false) }
    var notificationPermissionRequestedThisSession by remember { mutableStateOf(false) }

    val reminderSystemSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        permissionRefreshToken++
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        permissionRefreshToken++
    }

    val reminderPermissionStatus = remember(permissionRefreshToken) {
        ReminderPermissionManager.status(context)
    }

    fun continueReminderPermissionSetup() {
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
            else -> {
                showReminderPermissionDialog = false
            }
        }
    }

    val defaultDueMillis = remember(initialTask?.id) {
        initialTask?.dueAt?.takeIf { it > 0L } ?: Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    var hasDueAt by remember(initialTask?.id) {
        mutableStateOf((initialTask?.dueAt ?: 0L) > 0L)
    }
    var dueAtMillis by remember(initialTask?.id) {
        mutableLongStateOf(defaultDueMillis)
    }

    var courseMenuExpanded by remember { mutableStateOf(false) }
    var validationText by remember { mutableStateOf("") }

    val dueCalendar = Calendar.getInstance().apply {
        timeInMillis = dueAtMillis
    }
    val selectedCourseName = courses.firstOrNull { it.id == selectedCourseId }?.name
        ?: "不关联课程"

    fun hideKeyboardAndClearFocus() {
        // clearFocus() alone does not reliably close every Android IME, especially
        // inside a Compose AlertDialog. Use both Compose and platform IME APIs.
        focusManager.clearFocus(force = true)
        hostView.post {
            val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE)
                as? InputMethodManager
            inputMethodManager?.hideSoftInputFromWindow(hostView.windowToken, 0)
        }
    }

    fun updateDueDate(year: Int, month: Int, dayOfMonth: Int) {
        dueAtMillis = Calendar.getInstance().apply {
            timeInMillis = dueAtMillis
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun updateDueTime(hour: Int? = null, minute: Int? = null) {
        dueAtMillis = Calendar.getInstance().apply {
            timeInMillis = dueAtMillis
            hour?.let { set(Calendar.HOUR_OF_DAY, it.coerceIn(0, 23)) }
            minute?.let { set(Calendar.MINUTE, it.coerceIn(0, 59)) }
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    AlertDialog(
        onDismissRequest = {
            hideKeyboardAndClearFocus()
            onDismiss()
        },
        title = {
            Text(if (initialTask == null) "新增待办" else "编辑待办")
        },
        text = {
            Column(
                modifier = Modifier
                    .imePadding()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        validationText = ""
                    },
                    label = { Text("事项") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { hideKeyboardAndClearFocus() }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))

                Box {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                hideKeyboardAndClearFocus()
                                courseMenuExpanded = true
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "关联课程",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(selectedCourseName, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    DropdownMenu(
                        expanded = courseMenuExpanded,
                        onDismissRequest = { courseMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("不关联课程") },
                            onClick = {
                                selectedCourseId = null
                                courseMenuExpanded = false
                            }
                        )
                        courses.distinctBy { it.name }.forEach { course ->
                            DropdownMenuItem(
                                text = { Text(course.name) },
                                onClick = {
                                    selectedCourseId = course.id
                                    courseMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    "优先级",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    PriorityChoice("低", 0, priority) { priority = 0 }
                    PriorityChoice("普通", 1, priority) { priority = 1 }
                    PriorityChoice("高", 2, priority) { priority = 2 }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "截止时间",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (hasDueAt) formatDueAtFull(dueAtMillis) else "未设置",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    TextButton(
                        onClick = {
                            hideKeyboardAndClearFocus()
                            hasDueAt = !hasDueAt
                        }
                    ) {
                        Text(if (hasDueAt) "清除" else "设置")
                    }
                }

                if (hasDueAt) {
                    Spacer(Modifier.height(6.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                hideKeyboardAndClearFocus()
                                val current = Calendar.getInstance().apply {
                                    timeInMillis = dueAtMillis
                                }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        updateDueDate(year, month, dayOfMonth)
                                    },
                                    current.get(Calendar.YEAR),
                                    current.get(Calendar.MONTH),
                                    current.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                            Text(
                                "日期",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                SimpleDateFormat("yyyy年M月d日 E", Locale.CHINA)
                                    .format(Date(dueAtMillis)),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        "小时 · ${dueCalendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = dueCalendar.get(Calendar.HOUR_OF_DAY).toFloat(),
                        onValueChange = {
                            hideKeyboardAndClearFocus()
                            updateDueTime(hour = it.toInt())
                        },
                        valueRange = 0f..23f,
                        steps = 22,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        "分钟 · ${dueCalendar.get(Calendar.MINUTE).toString().padStart(2, '0')}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = dueCalendar.get(Calendar.MINUTE).toFloat(),
                        onValueChange = {
                            hideKeyboardAndClearFocus()
                            updateDueTime(minute = it.toInt())
                        },
                        valueRange = 0f..59f,
                        steps = 58,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        TextButton(
                            onClick = {
                                hideKeyboardAndClearFocus()
                                hasDueAt = true
                                dueAtMillis = presetDueMillis(0)
                            }
                        ) { Text("今晚") }
                        TextButton(
                            onClick = {
                                hideKeyboardAndClearFocus()
                                hasDueAt = true
                                dueAtMillis = presetDueMillis(1)
                            }
                        ) { Text("明晚") }
                        TextButton(
                            onClick = {
                                hideKeyboardAndClearFocus()
                                hasDueAt = true
                                dueAtMillis = presetDueMillis(7)
                            }
                        ) { Text("一周后") }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    "DDL 提醒",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(5.dp))
                Box {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = hasDueAt) {
                                hideKeyboardAndClearFocus()
                                reminderMenuExpanded = true
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                            Text(
                                if (hasDueAt) taskReminderText(reminderMinutesBefore) else "请先设置截止时间",
                                fontWeight = FontWeight.SemiBold,
                                color = if (hasDueAt) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (hasDueAt && reminderMinutesBefore >= 0) {
                                Text(
                                    "仅提醒一次 · ${formatReminderAt(dueAtMillis, reminderMinutesBefore)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    DropdownMenu(
                        expanded = reminderMenuExpanded,
                        onDismissRequest = { reminderMenuExpanded = false }
                    ) {
                        taskReminderOptions.forEach { (minutes, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    reminderMinutesBefore = minutes
                                    reminderMenuExpanded = false
                                    if (minutes >= 0 &&
                                        !ReminderPermissionManager.status(context).fullyReady
                                    ) {
                                        showReminderPermissionDialog = true
                                    }
                                }
                            )
                        }
                    }
                }

                if (hasDueAt && reminderMinutesBefore >= 0 &&
                    dueAtMillis - reminderMinutesBefore * 60_000L <= System.currentTimeMillis()
                ) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "所选提醒时间已经过去，本次待办不会再触发该提醒。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (hasDueAt && reminderMinutesBefore >= 0 && !reminderPermissionStatus.fullyReady) {
                    Spacer(Modifier.height(10.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                hideKeyboardAndClearFocus()
                                showReminderPermissionDialog = true
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(
                                "提醒权限未完全开启",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "点击配置通知横幅、铃声与准时提醒权限",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { hideKeyboardAndClearFocus() }
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { hideKeyboardAndClearFocus() }) {
                        Text("收起键盘")
                    }
                }

                if (validationText.isNotBlank()) {
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = validationText,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    hideKeyboardAndClearFocus()
                    if (title.isBlank()) {
                        validationText = "请填写待办事项"
                        return@TextButton
                    }
                    onSave(
                        (initialTask ?: TaskEntity(
                            semesterId = semesterId,
                            title = title
                        )).copy(
                            semesterId = semesterId,
                            courseId = selectedCourseId,
                            title = title.trim(),
                            note = note.trim(),
                            dueAt = if (hasDueAt) dueAtMillis else 0L,
                            reminderMinutesBefore = if (hasDueAt) reminderMinutesBefore else -1,
                            priority = priority,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    hideKeyboardAndClearFocus()
                    onDismiss()
                }
            ) { Text("取消") }
        }
    )

    if (showReminderPermissionDialog) {
        ReminderPermissionDialog(
            status = reminderPermissionStatus,
            title = "开启 DDL 提醒",
            onContinueSetup = { continueReminderPermissionSetup() },
            onDismiss = { showReminderPermissionDialog = false }
        )
    }
}

@Composable
private fun PriorityChoice(
    label: String,
    value: Int,
    selected: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (value == selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp), fontSize = 13.sp)
    }
}

private fun priorityText(priority: Int): String = when (priority) {
    2 -> "高优先级"
    0 -> "低优先级"
    else -> "普通优先级"
}

private val taskReminderOptions = listOf(
    -1 to "不提醒",
    0 to "截止时提醒",
    10 to "提前 10 分钟",
    30 to "提前 30 分钟",
    60 to "提前 1 小时",
    180 to "提前 3 小时",
    1440 to "提前 1 天",
    2880 to "提前 2 天",
    10080 to "提前 1 周"
)

private fun taskReminderText(minutes: Int): String =
    taskReminderOptions.firstOrNull { it.first == minutes }?.second
        ?: when {
            minutes < 0 -> "不提醒"
            minutes < 60 -> "提前 $minutes 分钟"
            minutes < 1440 -> "提前 ${minutes / 60} 小时"
            else -> "提前 ${minutes / 1440} 天"
        }

private fun formatReminderAt(dueAt: Long, minutesBefore: Int): String {
    val triggerAt = dueAt - minutesBefore.coerceAtLeast(0) * 60_000L
    return SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(triggerAt))
}

fun formatDueAt(millis: Long): String =
    SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(millis))

private fun formatDueAtFull(millis: Long): String =
    SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA).format(Date(millis))

private fun presetDueMillis(daysFromToday: Int): Long =
    Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, daysFromToday)
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

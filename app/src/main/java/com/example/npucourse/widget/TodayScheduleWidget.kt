package com.example.npucourse.widget

import android.app.PendingIntent
import android.app.AlarmManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import com.example.npucourse.MainActivity
import com.example.npucourse.R
import com.example.npucourse.data.AppDatabase
import com.example.npucourse.data.CourseEntity
import com.example.npucourse.data.settings.SettingsRepository
import com.example.npucourse.model.WeekMode
import com.example.npucourse.util.calculateCurrentWeek
import com.example.npucourse.util.getScheduleForCampus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 4x2 左右的“今日课程”桌面小组件。
 */
class TodayScheduleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val snapshot = TodayScheduleWidgetData.load(context)
                WidgetRefreshAlarm.scheduleNext(context, snapshot.nextRefreshAtMillis)
                appWidgetIds.forEach { appWidgetId ->
                    appWidgetManager.updateAppWidget(
                        appWidgetId,
                        createTodayRemoteViews(context, snapshot)
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/**
 * 2x2 左右的“下一节课”桌面小组件。
 */
class NextCourseWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val snapshot = TodayScheduleWidgetData.load(context)
                WidgetRefreshAlarm.scheduleNext(context, snapshot.nextRefreshAtMillis)
                appWidgetIds.forEach { appWidgetId ->
                    appWidgetManager.updateAppWidget(
                        appWidgetId,
                        createNextRemoteViews(context, snapshot)
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/**
 * 2x1 左右的“DDL 倒计时”小组件，只显示最紧急的一项未完成待办。
 */
class TaskCompactWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val snapshot = TodayScheduleWidgetData.load(context)
                WidgetRefreshAlarm.scheduleNext(context, snapshot.nextRefreshAtMillis)
                appWidgetIds.forEach { appWidgetId ->
                    appWidgetManager.updateAppWidget(
                        appWidgetId,
                        createTaskCompactRemoteViews(context, snapshot)
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/**
 * 4x3 左右的“待办清单”小组件。支持纵向缩放，按高度动态显示 2~5 条待办。
 */
class TaskListWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val snapshot = TodayScheduleWidgetData.load(context)
                WidgetRefreshAlarm.scheduleNext(context, snapshot.nextRefreshAtMillis)
                appWidgetIds.forEach { appWidgetId ->
                    val maxRows = taskListRowsForOptions(
                        appWidgetManager.getAppWidgetOptions(appWidgetId)
                    )
                    appWidgetManager.updateAppWidget(
                        appWidgetId,
                        createTaskListRemoteViews(context, snapshot, maxRows)
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        TodayScheduleWidgetUpdater.updateAll(context.applicationContext)
    }
}

/**
 * App 内课程/学期发生变化后，立即刷新所有 瓜瓜课程表 小组件。
 */
object TodayScheduleWidgetUpdater {

    fun updateAll(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)

        val todayComponent = ComponentName(
            appContext,
            TodayScheduleWidgetProvider::class.java
        )
        val nextComponent = ComponentName(
            appContext,
            NextCourseWidgetProvider::class.java
        )
        val taskCompactComponent = ComponentName(
            appContext,
            TaskCompactWidgetProvider::class.java
        )
        val taskListComponent = ComponentName(
            appContext,
            TaskListWidgetProvider::class.java
        )

        val todayIds = manager.getAppWidgetIds(todayComponent)
        val nextIds = manager.getAppWidgetIds(nextComponent)
        val taskCompactIds = manager.getAppWidgetIds(taskCompactComponent)
        val taskListIds = manager.getAppWidgetIds(taskListComponent)

        if (todayIds.isEmpty() && nextIds.isEmpty() &&
            taskCompactIds.isEmpty() && taskListIds.isEmpty()
        ) return

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val snapshot = TodayScheduleWidgetData.load(appContext)
            WidgetRefreshAlarm.scheduleNext(appContext, snapshot.nextRefreshAtMillis)

            todayIds.forEach { id ->
                manager.updateAppWidget(
                    id,
                    createTodayRemoteViews(appContext, snapshot)
                )
            }

            nextIds.forEach { id ->
                manager.updateAppWidget(
                    id,
                    createNextRemoteViews(appContext, snapshot)
                )
            }

            taskCompactIds.forEach { id ->
                manager.updateAppWidget(
                    id,
                    createTaskCompactRemoteViews(appContext, snapshot)
                )
            }

            taskListIds.forEach { id ->
                val maxRows = taskListRowsForOptions(manager.getAppWidgetOptions(id))
                manager.updateAppWidget(
                    id,
                    createTaskListRemoteViews(appContext, snapshot, maxRows)
                )
            }
        }
    }
}


/**
 * 由 AlarmManager 在课程开始/结束边界附近触发，使桌面状态不必只依赖 30 分钟轮询。
 * 使用普通 alarm，不要求用户额外授予精确闹钟权限。
 */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TodayScheduleWidgetUpdater.updateAll(context.applicationContext)
    }
}

private object WidgetRefreshAlarm {
    private const val REQUEST_CODE = 4601

    fun scheduleNext(context: Context, triggerAtMillis: Long) {
        if (triggerAtMillis <= System.currentTimeMillis()) return

        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(appContext, WidgetRefreshReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }
}

/**
 * Android 8.0+ 支持从 App 内向桌面请求固定小组件。
 * 不支持时仍可从系统桌面的小组件列表手动添加。
 */
object TodayScheduleWidgetPinHelper {

    fun canPin(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        return AppWidgetManager
            .getInstance(context)
            .isRequestPinAppWidgetSupported
    }

    fun requestToday(context: Context): Boolean =
        requestPin(
            context = context,
            providerClass = TodayScheduleWidgetProvider::class.java
        )

    fun requestNext(context: Context): Boolean =
        requestPin(
            context = context,
            providerClass = NextCourseWidgetProvider::class.java
        )

    fun requestTaskCompact(context: Context): Boolean =
        requestPin(
            context = context,
            providerClass = TaskCompactWidgetProvider::class.java
        )

    fun requestTaskList(context: Context): Boolean =
        requestPin(
            context = context,
            providerClass = TaskListWidgetProvider::class.java
        )

    fun requestAcademicOverview(context: Context): Boolean =
        requestPin(
            context = context,
            providerClass = AcademicOverviewWidgetProvider::class.java
        )

    private fun requestPin(
        context: Context,
        providerClass: Class<*>
    ): Boolean {
        if (!canPin(context)) return false

        val provider = ComponentName(
            context,
            providerClass
        )

        return AppWidgetManager
            .getInstance(context)
            .requestPinAppWidget(
                provider,
                null,
                null
            )
    }
}

private data class WidgetCourseLine(
    val startTime: String,
    val endTime: String,
    val name: String,
    val room: String,
    val current: Boolean,
    val startMinutes: Int,
    val endMinutes: Int
)

private data class WidgetTaskLine(
    val title: String,
    val courseName: String,
    val dueAt: Long,
    val overdue: Boolean
)

private data class WidgetSnapshot(
    val header: String,
    val subtitle: String,
    val courses: List<WidgetCourseLine>,
    val allTodayCourses: List<WidgetCourseLine>,
    val tasks: List<WidgetTaskLine> = emptyList(),
    val openTaskCount: Int = 0,
    val nowMinutes: Int,
    val emptyText: String = "今天没有课程",
    val nextRefreshAtMillis: Long = 0L
)

private object TodayScheduleWidgetData {

    suspend fun load(context: Context): WidgetSnapshot {
        val database = AppDatabase.getInstance(context)
        val settings = SettingsRepository(context).settings.first()
        val semesters = database.semesterDao().getAllSemestersOnce()

        val semester =
            semesters.firstOrNull { it.id == settings.activeSemesterId }
                ?: semesters.firstOrNull()
                ?: return WidgetSnapshot(
                    header = "瓜瓜课程表",
                    subtitle = "还没有课表",
                    courses = emptyList(),
                    allTodayCourses = emptyList(),
                    nowMinutes = 0,
                    emptyText = "还没有课表"
                )

        val week = calculateCurrentWeek(semester.startMillis)
        val now = Calendar.getInstance()
        val day = calendarDayToCourseDay(now.get(Calendar.DAY_OF_WEEK))
        val nowMinutes =
            now.get(Calendar.HOUR_OF_DAY) * 60 +
                now.get(Calendar.MINUTE)
        val schedule = getScheduleForCampus(semester.campus)

        val semesterCourses = database
            .courseDao()
            .getCoursesForSemester(semester.id)

        val allTodayCourses = semesterCourses
            .filter { course ->
                course.day == day && isActiveInWeek(course, week)
            }
            .sortedBy { it.startSection }
            .mapNotNull { course ->
                val start = schedule.firstOrNull {
                    it.section == course.startSection
                } ?: return@mapNotNull null

                val end = schedule.firstOrNull {
                    it.section == course.endSection
                } ?: return@mapNotNull null

                val startMinutes = timeToMinutes(start.startTime)
                val endMinutes = timeToMinutes(end.endTime)

                WidgetCourseLine(
                    startTime = start.startTime,
                    endTime = end.endTime,
                    name = course.name,
                    room = course.room.ifBlank { "未填写教室" },
                    current = nowMinutes in startMinutes until endMinutes,
                    startMinutes = startMinutes,
                    endMinutes = endMinutes
                )
            }

        val relevant = allTodayCourses
            .filter { line ->
                line.current || line.startMinutes >= nowMinutes
            }
            .take(3)

        val dateText = SimpleDateFormat(
            "M月d日 E",
            Locale.CHINA
        ).format(now.time)

        val weekText = when {
            week <= 0 -> "学期尚未开始"
            week > 20 -> "学期已结束"
            else -> "第" + week + "周"
        }

        val courseNamesById = semesterCourses.associate { it.id to it.name }
        val nowMillis = now.timeInMillis
        val openTasks = database
            .taskDao()
            .getTasksForSemester(semester.id)
            .asSequence()
            .filter { !it.completed }
            .sortedWith(
                compareBy<com.example.npucourse.data.TaskEntity> {
                    if (it.dueAt <= 0L) Long.MAX_VALUE else it.dueAt
                }.thenByDescending { it.priority }
                    .thenBy { it.createdAt }
            )
            .toList()

        val taskLines = openTasks
            .take(6)
            .map { task ->
                WidgetTaskLine(
                    title = task.title,
                    courseName = task.courseId
                        ?.let(courseNamesById::get)
                        .orEmpty(),
                    dueAt = task.dueAt,
                    overdue = task.dueAt > 0L && task.dueAt < nowMillis
                )
            }

        val displayCourses = if (taskLines.isNotEmpty()) relevant.take(2) else relevant

        return WidgetSnapshot(
            header = "今天 · $weekText",
            subtitle = "$dateText · ${semester.name}",
            courses = displayCourses,
            allTodayCourses = allTodayCourses,
            tasks = taskLines,
            openTaskCount = openTasks.size,
            nowMinutes = nowMinutes,
            emptyText = when {
                allTodayCourses.isEmpty() -> "今天没有课程"
                relevant.isEmpty() -> "今日课程已结束"
                else -> ""
            },
            nextRefreshAtMillis = calculateNextRefreshAt(now, allTodayCourses, taskLines)
        )
    }

    private fun calculateNextRefreshAt(
        now: Calendar,
        courses: List<WidgetCourseLine>,
        tasks: List<WidgetTaskLine>
    ): Long {
        val nowMillis = now.timeInMillis
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        val courseCandidates = courses
            .flatMap { listOf(it.startMinutes, it.endMinutes) }
            .filter { it > nowMinutes }
            .map { minute ->
                (now.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, minute / 60)
                    set(Calendar.MINUTE, minute % 60)
                    set(Calendar.SECOND, 2)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }

        val taskCandidates = tasks
            .map { it.dueAt }
            .filter { it > nowMillis }
            .map { it + 2_000L }

        val midnightCandidate = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 1)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return (courseCandidates + taskCandidates + midnightCandidate)
            .filter { it > nowMillis }
            .minOrNull()
            ?: midnightCandidate
    }



    private fun isActiveInWeek(
        course: CourseEntity,
        week: Int
    ): Boolean {
        if (week !in 1..20) return false

        val start = course.startWeek.coerceIn(1, 20)
        val end = course.endWeek.coerceIn(start, 20)

        return when (WeekMode.fromStorage(course.weekMode)) {
            WeekMode.EVERY ->
                week in start..end

            WeekMode.ODD ->
                week in start..end && week % 2 == 1

            WeekMode.EVEN ->
                week in start..end && week % 2 == 0

            WeekMode.CUSTOM ->
                parseWeeks(course.customWeeks).contains(week)
        }
    }

    private fun parseWeeks(text: String): Set<Int> {
        val result = mutableSetOf<Int>()

        text
            .replace('，', ',')
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { token ->
                val parts = token
                    .replace('～', '-')
                    .replace('—', '-')
                    .split('-')

                if (parts.size == 2) {
                    val a = parts[0].trim().toIntOrNull()
                    val b = parts[1].trim().toIntOrNull()

                    if (a != null && b != null) {
                        for (week in minOf(a, b)..maxOf(a, b)) {
                            if (week in 1..20) {
                                result.add(week)
                            }
                        }
                    }
                } else {
                    token.toIntOrNull()?.let {
                        if (it in 1..20) {
                            result.add(it)
                        }
                    }
                }
            }

        return result
    }

    private fun timeToMinutes(text: String): Int {
        val parts = text.split(':')
        return (parts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 +
            (parts.getOrNull(1)?.toIntOrNull() ?: 0)
    }

    private fun calendarDayToCourseDay(calendarDay: Int): Int =
        when (calendarDay) {
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

private fun createTodayRemoteViews(
    context: Context,
    snapshot: WidgetSnapshot
): RemoteViews {
    val views = RemoteViews(
        context.packageName,
        R.layout.widget_today_schedule
    )

    views.setTextViewText(
        R.id.widgetTitle,
        snapshot.header
    )
    views.setTextViewText(
        R.id.widgetSubtitle,
        snapshot.subtitle
    )

    val rows = listOf(
        Triple(
            R.id.widgetRow1,
            R.id.widgetTime1,
            R.id.widgetCourse1
        ),
        Triple(
            R.id.widgetRow2,
            R.id.widgetTime2,
            R.id.widgetCourse2
        ),
        Triple(
            R.id.widgetRow3,
            R.id.widgetTime3,
            R.id.widgetCourse3
        )
    )

    val roomIds = listOf(
        R.id.widgetRoom1,
        R.id.widgetRoom2,
        R.id.widgetRoom3
    )

    rows.forEachIndexed { index, (rowId, timeId, courseId) ->
        val item = snapshot.courses.getOrNull(index)

        if (item == null) {
            views.setViewVisibility(
                rowId,
                View.GONE
            )
        } else {
            views.setViewVisibility(
                rowId,
                View.VISIBLE
            )
            views.setTextViewText(
                timeId,
                item.startTime
            )
            views.setTextViewText(
                courseId,
                if (item.current) {
                    "● ${item.name}"
                } else {
                    item.name
                }
            )
            views.setTextViewText(
                roomIds[index],
                item.room
            )
        }
    }

    val visibleTasks = snapshot.tasks.take(2)
    if (visibleTasks.isEmpty()) {
        views.setViewVisibility(R.id.widgetTaskContainer, View.GONE)
    } else {
        views.setViewVisibility(R.id.widgetTaskContainer, View.VISIBLE)
        val taskTextIds = listOf(R.id.widgetTask1, R.id.widgetTask2)
        taskTextIds.forEachIndexed { index, textId ->
            val task = visibleTasks.getOrNull(index)
            if (task == null) {
                views.setViewVisibility(textId, View.GONE)
            } else {
                views.setViewVisibility(textId, View.VISIBLE)
                views.setTextViewText(
                    textId,
                    buildWidgetTaskText(task)
                )
            }
        }
    }

    if (snapshot.courses.isEmpty() && visibleTasks.isEmpty()) {
        views.setViewVisibility(
            R.id.widgetEmpty,
            View.VISIBLE
        )
        views.setTextViewText(
            R.id.widgetEmpty,
            snapshot.emptyText
        )
    } else {
        views.setViewVisibility(
            R.id.widgetEmpty,
            View.GONE
        )
    }

    views.setOnClickPendingIntent(
        R.id.widgetRoot,
        mainActivityPendingIntent(context, 8001)
    )

    return views
}

private fun createNextRemoteViews(
    context: Context,
    snapshot: WidgetSnapshot
): RemoteViews {
    val views = RemoteViews(
        context.packageName,
        R.layout.widget_next_course
    )

    val item = snapshot.courses.firstOrNull()
    val nearestTask = snapshot.tasks.firstOrNull()

    views.setTextViewText(
        R.id.nextWidgetSubtitle,
        snapshot.subtitle
    )

    if (item == null) {
        if (nearestTask != null) {
            views.setTextViewText(R.id.nextWidgetLabel, "近期待办")
            views.setTextViewText(R.id.nextWidgetCourse, nearestTask.title)
            views.setTextViewText(
                R.id.nextWidgetTime,
                when {
                    nearestTask.dueAt <= 0L -> "未设置截止时间"
                    nearestTask.overdue -> "已逾期"
                    else -> formatWidgetTaskDue(nearestTask.dueAt)
                }
            )
            views.setTextViewText(
                R.id.nextWidgetRoom,
                nearestTask.courseName.ifBlank { "通用待办" }
            )
        } else {
            views.setTextViewText(R.id.nextWidgetLabel, "今天")
            views.setTextViewText(R.id.nextWidgetCourse, snapshot.emptyText)
            views.setTextViewText(R.id.nextWidgetTime, "打开 瓜瓜课程表 查看课表")
            views.setTextViewText(R.id.nextWidgetRoom, "")
        }
    } else {
        val label = if (item.current) {
            "正在上课"
        } else {
            "下一节课"
        }

        val timingText = if (item.current) {
            val remain = (item.endMinutes - snapshot.nowMinutes)
                .coerceAtLeast(0)
            if (remain > 0) {
                "${item.startTime}-${item.endTime} · 还有${formatDuration(remain)}"
            } else {
                "${item.startTime}-${item.endTime}"
            }
        } else {
            val wait = (item.startMinutes - snapshot.nowMinutes)
                .coerceAtLeast(0)
            if (wait > 0) {
                "${item.startTime}开始 · ${formatDuration(wait)}后"
            } else {
                "${item.startTime}-${item.endTime}"
            }
        }

        views.setTextViewText(
            R.id.nextWidgetLabel,
            label
        )
        views.setTextViewText(
            R.id.nextWidgetCourse,
            item.name
        )
        views.setTextViewText(
            R.id.nextWidgetTime,
            timingText
        )
        views.setTextViewText(
            R.id.nextWidgetRoom,
            item.room
        )
    }

    if (nearestTask == null || item == null) {
        views.setViewVisibility(R.id.nextWidgetTask, View.GONE)
    } else {
        views.setViewVisibility(R.id.nextWidgetTask, View.VISIBLE)
        views.setTextViewText(
            R.id.nextWidgetTask,
            "待办 · ${buildWidgetTaskText(nearestTask)}"
        )
    }

    views.setOnClickPendingIntent(
        R.id.nextWidgetRoot,
        mainActivityPendingIntent(context, 8002)
    )

    return views
}

private fun createTaskCompactRemoteViews(
    context: Context,
    snapshot: WidgetSnapshot
): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_task_compact)
    val task = snapshot.tasks.firstOrNull()

    if (task == null) {
        views.setTextViewText(R.id.taskCompactLabel, "待办")
        views.setTextViewText(R.id.taskCompactTitle, "暂无未完成待办")
        views.setTextViewText(R.id.taskCompactMeta, "点击新建事项")
    } else {
        views.setTextViewText(
            R.id.taskCompactLabel,
            when {
                task.overdue -> "DDL · 已逾期"
                task.dueAt > 0L -> "DDL · ${formatTaskCountdown(task.dueAt)}"
                else -> "最近待办"
            }
        )
        views.setTextViewText(R.id.taskCompactTitle, task.title)
        val due = when {
            task.dueAt <= 0L -> "未设置截止"
            task.overdue -> "已逾期 · ${formatWidgetTaskDue(task.dueAt)}"
            else -> formatWidgetTaskDue(task.dueAt)
        }
        val course = task.courseName.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
        views.setTextViewText(R.id.taskCompactMeta, "$due$course")
    }

    views.setOnClickPendingIntent(
        R.id.taskCompactRoot,
        mainActivityPendingIntent(context, 8003, openTasks = true)
    )
    return views
}

private fun createTaskListRemoteViews(
    context: Context,
    snapshot: WidgetSnapshot,
    maxRows: Int
): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_task_list)
    views.setTextViewText(
        R.id.taskListTitle,
        if (snapshot.openTaskCount > 0) "待办 · ${snapshot.openTaskCount} 项" else "待办"
    )
    views.setTextViewText(R.id.taskListSubtitle, snapshot.subtitle)

    val rowIds = listOf(
        R.id.taskListRow1,
        R.id.taskListRow2,
        R.id.taskListRow3,
        R.id.taskListRow4,
        R.id.taskListRow5
    )
    val textIds = listOf(
        R.id.taskListText1,
        R.id.taskListText2,
        R.id.taskListText3,
        R.id.taskListText4,
        R.id.taskListText5
    )

    val visible = snapshot.tasks.take(maxRows.coerceIn(2, 5))
    rowIds.forEachIndexed { index, rowId ->
        val task = visible.getOrNull(index)
        if (task == null) {
            views.setViewVisibility(rowId, View.GONE)
        } else {
            views.setViewVisibility(rowId, View.VISIBLE)
            views.setTextViewText(textIds[index], buildWidgetTaskText(task))
        }
    }

    if (visible.isEmpty()) {
        views.setViewVisibility(R.id.taskListEmpty, View.VISIBLE)
        views.setTextViewText(R.id.taskListEmpty, "暂无未完成待办")
    } else {
        views.setViewVisibility(R.id.taskListEmpty, View.GONE)
    }

    val hiddenCount = (snapshot.openTaskCount - visible.size).coerceAtLeast(0)
    if (hiddenCount > 0) {
        views.setViewVisibility(R.id.taskListMore, View.VISIBLE)
        views.setTextViewText(R.id.taskListMore, "还有 $hiddenCount 项 · 点击查看全部")
    } else {
        views.setViewVisibility(R.id.taskListMore, View.GONE)
    }

    views.setOnClickPendingIntent(
        R.id.taskListRoot,
        mainActivityPendingIntent(context, 8004, openTasks = true)
    )
    return views
}

private fun taskListRowsForOptions(options: android.os.Bundle): Int {
    val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180)
    return when {
        minHeight < 115 -> 2
        minHeight < 165 -> 3
        minHeight < 225 -> 4
        else -> 5
    }
}

private fun buildWidgetTaskText(task: WidgetTaskLine): String {
    val due = when {
        task.dueAt <= 0L -> "未设截止"
        task.overdue -> "已逾期"
        else -> formatWidgetTaskDue(task.dueAt)
    }
    val course = task.courseName
        .takeIf { it.isNotBlank() }
        ?.let { "$it · " }
        .orEmpty()
    return "$due · $course${task.title}"
}

private fun formatTaskCountdown(dueAt: Long): String {
    val remaining = (dueAt - System.currentTimeMillis()).coerceAtLeast(0L)
    val minutes = ((remaining + 59_999L) / 60_000L).coerceAtLeast(1L)
    return when {
        minutes < 60L -> "还有 ${minutes} 分钟"
        minutes < 24L * 60L -> "还有 ${minutes / 60L} 小时"
        else -> "还有 ${minutes / (24L * 60L)} 天"
    }
}

private fun formatWidgetTaskDue(millis: Long): String {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = millis }

    fun sameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    val tomorrow = (now.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, 1)
    }
    val time = SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(millis))

    return when {
        sameDay(target, now) -> "今天 $time"
        sameDay(target, tomorrow) -> "明天 $time"
        else -> SimpleDateFormat("M/d HH:mm", Locale.CHINA).format(Date(millis))
    }
}

private fun mainActivityPendingIntent(
    context: Context,
    requestCode: Int,
    openTasks: Boolean = false
): PendingIntent {
    val intent = Intent(
        context,
        MainActivity::class.java
    ).addFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
    ).apply {
        if (openTasks) {
            putExtra("open_academic_tasks", true)
        }
    }

    return PendingIntent.getActivity(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or
            PendingIntent.FLAG_IMMUTABLE
    )
}

private fun formatDuration(minutes: Int): String {
    if (minutes < 60) {
        return minutes.toString() + "分钟"
    }

    val hours = minutes / 60
    val rest = minutes % 60

    return if (rest == 0) {
        hours.toString() + "小时"
    } else {
        hours.toString() + "小时" + rest + "分钟"
    }
}

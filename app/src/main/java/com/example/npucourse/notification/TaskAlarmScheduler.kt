package com.example.npucourse.notification

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.example.npucourse.data.TaskEntity

/**
 * 为 DDL 待办注册一次性的截止前提醒。
 * reminderMinutesBefore = -1 表示关闭提醒。
 */
object TaskAlarmScheduler {
    private const val PREFS_NAME = "task_alarm_registry"
    private const val KEY_ALARM_URIS = "scheduled_task_alarm_uris"

    @SuppressLint("ScheduleExactAlarm")
    fun rescheduleAll(
        context: Context,
        tasks: List<TaskEntity>,
        courseNamesById: Map<Long, String> = emptyMap()
    ) {
        val appContext = context.applicationContext
        cancelAll(appContext)

        val now = System.currentTimeMillis()
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val newAlarmUris = mutableSetOf<String>()

        tasks.asSequence()
            .filter { !it.completed }
            .filter { it.dueAt > 0L }
            .filter { it.reminderMinutesBefore >= 0 && it.reminderMinutesBefore != 1 }
            .forEach { task ->
                val triggerAt = task.dueAt - task.reminderMinutesBefore * 60_000L
                if (triggerAt <= now) return@forEach

                val alarmUri = buildAlarmUri(task)
                val intent = Intent(appContext, TaskAlarmReceiver::class.java).apply {
                    data = Uri.parse(alarmUri)
                    putExtra(TaskAlarmReceiver.EXTRA_TASK_TITLE, task.title)
                    putExtra(TaskAlarmReceiver.EXTRA_TASK_NOTE, task.note)
                    putExtra(
                        TaskAlarmReceiver.EXTRA_COURSE_NAME,
                        task.courseId?.let(courseNamesById::get).orEmpty()
                    )
                    putExtra(TaskAlarmReceiver.EXTRA_DUE_AT, task.dueAt)
                    putExtra(TaskAlarmReceiver.EXTRA_REMINDER_MINUTES, task.reminderMinutesBefore)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    appContext,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    when {
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.M -> {
                            alarmManager.setExact(
                                AlarmManager.RTC_WAKEUP,
                                triggerAt,
                                pendingIntent
                            )
                        }
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                            alarmManager.canScheduleExactAlarms() -> {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                triggerAt,
                                pendingIntent
                            )
                        }
                        else -> {
                            // Android 12+ 没有精确闹钟能力时自动退化，DDL 提醒仍可工作。
                            alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                triggerAt,
                                pendingIntent
                            )
                        }
                    }
                    newAlarmUris.add(alarmUri)
                } catch (_: SecurityException) {
                    // 权限状态可能刚刚发生变化，不让提醒注册影响 App 主流程。
                }
            }

        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_ALARM_URIS, newAlarmUris)
            .apply()
    }

    fun cancelAll(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val alarmUris = preferences.getStringSet(KEY_ALARM_URIS, emptySet())?.toSet().orEmpty()

        alarmUris.forEach { alarmUri ->
            val intent = Intent(appContext, TaskAlarmReceiver::class.java).apply {
                data = Uri.parse(alarmUri)
            }
            PendingIntent.getBroadcast(
                appContext,
                0,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )?.let { pendingIntent ->
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }

        preferences.edit().remove(KEY_ALARM_URIS).apply()
    }

    private fun buildAlarmUri(task: TaskEntity): String =
        "npucourse://task-reminder/${task.id}/${task.dueAt}/${task.reminderMinutesBefore}"
}

package com.example.npucourse.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TaskAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "待办事项"
        val note = intent.getStringExtra(EXTRA_TASK_NOTE).orEmpty()
        val courseName = intent.getStringExtra(EXTRA_COURSE_NAME).orEmpty()
        val dueAt = intent.getLongExtra(EXTRA_DUE_AT, 0L)
        val reminderMinutes = intent.getIntExtra(EXTRA_REMINDER_MINUTES, -1)
        val notificationId = intent.dataString?.hashCode()
            ?: System.currentTimeMillis().toInt()

        NotificationHelper.showTaskReminder(
            context = context,
            notificationId = notificationId,
            taskTitle = title,
            note = note,
            courseName = courseName,
            dueAt = dueAt,
            reminderMinutes = reminderMinutes
        )
    }

    companion object {
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_NOTE = "extra_task_note"
        const val EXTRA_COURSE_NAME = "extra_task_course_name"
        const val EXTRA_DUE_AT = "extra_task_due_at"
        const val EXTRA_REMINDER_MINUTES = "extra_task_reminder_minutes"
    }
}

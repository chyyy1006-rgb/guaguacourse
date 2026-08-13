package com.example.npucourse.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.npucourse.importer.NwpuExamRecord

class ExamAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AcademicNotificationHelper.showExamReminder(
            context = context,
            exam = NwpuExamRecord(
                courseName = intent.getStringExtra(EXTRA_COURSE).orEmpty(),
                timeText = intent.getStringExtra(EXTRA_TIME).orEmpty(),
                location = intent.getStringExtra(EXTRA_LOCATION).orEmpty(),
                status = "",
                finished = false
            ),
            minutesBefore = intent.getIntExtra(EXTRA_MINUTES, 0)
        )
    }

    companion object {
        const val EXTRA_COURSE = "course"
        const val EXTRA_TIME = "time"
        const val EXTRA_LOCATION = "location"
        const val EXTRA_MINUTES = "minutes"
    }
}

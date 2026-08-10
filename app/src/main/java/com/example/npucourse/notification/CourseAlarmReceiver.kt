package com.example.npucourse.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class CourseAlarmReceiver :
    BroadcastReceiver() {


    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        /*
         * 获取安排闹钟时写进去的课程数据。
         */

        val courseName =
            intent.getStringExtra(
                EXTRA_COURSE_NAME
            )
                ?: "课程"


        val room =
            intent.getStringExtra(
                EXTRA_ROOM
            )
                ?: "未填写教室"


        val startTime =
            intent.getStringExtra(
                EXTRA_START_TIME
            )
                ?: "--:--"


        val reminderMinutes =
            intent.getIntExtra(
                EXTRA_REMINDER_MINUTES,
                10
            )


        /*
         * 每个闹钟 URI 都不同。
         *
         * 用 URI hashCode 作为通知 ID，
         * 这样不同课程不会互相覆盖。
         */
        val notificationId =
            intent.dataString
                ?.hashCode()
                ?: System
                    .currentTimeMillis()
                    .toInt()


        NotificationHelper
            .showCourseReminder(
                context =
                    context,

                notificationId =
                    notificationId,

                courseName =
                    courseName,

                room =
                    room,

                startTime =
                    startTime,

                reminderMinutes =
                    reminderMinutes
            )
    }


    companion object {

        const val EXTRA_COURSE_NAME =
            "extra_course_name"

        const val EXTRA_ROOM =
            "extra_room"

        const val EXTRA_START_TIME =
            "extra_start_time"

        const val EXTRA_REMINDER_MINUTES =
            "extra_reminder_minutes"
    }
}
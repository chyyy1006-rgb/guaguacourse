package com.example.npucourse.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.npucourse.MainActivity
import com.example.npucourse.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


object NotificationHelper {

    /*
     * 通知渠道 ID。
     *
     * 创建以后不要随意修改，
     * 否则系统会认为这是一个新的通知渠道。
     */
    const val COURSE_CHANNEL_ID =
        "course_reminders"

    const val TASK_CHANNEL_ID =
        "task_deadline_reminders"


    /*
     * =====================================================
     * 创建课程提醒通知渠道
     * =====================================================
     */

    fun createCourseReminderChannel(
        context: Context
    ) {

        /*
         * Android 8.0 以下没有 NotificationChannel。
         */
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    COURSE_CHANNEL_ID,
                    "课程提醒",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {

                    description =
                        "在课程开始前提醒你上课"

                    enableVibration(
                        true
                    )
                }


            val notificationManager =
                context.getSystemService(
                    NotificationManager::class.java
                )


            notificationManager
                .createNotificationChannel(
                    channel
                )
        }
    }


    fun createTaskReminderChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                TASK_CHANNEL_ID,
                "待办截止提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "在待办事项截止前通过横幅、铃声和振动提醒你"
                enableVibration(true)
                setSound(defaultSound, audioAttributes)
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }


    /*
     * =====================================================
     * 是否拥有通知权限
     * =====================================================
     */

    fun hasNotificationPermission(
        context: Context
    ): Boolean {

        /*
         * Android 13 以下没有 POST_NOTIFICATIONS
         * 运行时权限。
         */
        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.TIRAMISU
        ) {

            return true
        }


        return ContextCompat
            .checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) ==
                PackageManager.PERMISSION_GRANTED
    }


    /*
     * =====================================================
     * 显示课程通知
     * =====================================================
     */

    @SuppressLint(
        "MissingPermission"
    )
    fun showCourseReminder(
        context: Context,
        notificationId: Int,
        courseName: String,
        room: String,
        startTime: String,
        reminderMinutes: Int
    ) {

        /*
         * 没有权限时直接结束。
         */
        if (
            !hasNotificationPermission(
                context
            )
        ) {

            return
        }


        /*
         * 确保通知渠道已经存在。
         */
        createCourseReminderChannel(
            context
        )


        /*
         * 点击通知以后打开 App。
         */
        val openAppIntent =
            Intent(
                context,
                MainActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
            }


        val openAppPendingIntent =
            PendingIntent.getActivity(
                context,
                10001,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )


        /*
         * 通知正文。
         */
        val contentText =

            when {

                reminderMinutes > 0 -> {

                    "还有 " +
                            reminderMinutes +
                            " 分钟上课 · " +
                            startTime +
                            " · " +
                            room
                }


                else -> {

                    startTime +
                            " · " +
                            room
                }
            }


        /*
         * 构造通知。
         */
        val notification =
            NotificationCompat
                .Builder(
                    context,
                    COURSE_CHANNEL_ID
                )
                .setSmallIcon(
                    R.mipmap.ic_launcher
                )
                .setContentTitle(
                    courseName + " 即将开始"
                )
                .setContentText(
                    contentText
                )
                .setStyle(
                    NotificationCompat
                        .BigTextStyle()
                        .bigText(
                            contentText
                        )
                )
                .setPriority(
                    NotificationCompat.PRIORITY_HIGH
                )
                .setCategory(
                    NotificationCompat.CATEGORY_REMINDER
                )
                .setAutoCancel(
                    true
                )
                .setContentIntent(
                    openAppPendingIntent
                )
                .build()


        NotificationManagerCompat
            .from(
                context
            )
            .notify(
                notificationId,
                notification
            )
    }

    @SuppressLint("MissingPermission")
    fun showTaskReminder(
        context: Context,
        notificationId: Int,
        taskTitle: String,
        note: String,
        courseName: String,
        dueAt: Long,
        reminderMinutes: Int
    ) {
        if (!hasNotificationPermission(context)) return
        createTaskReminderChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_academic_tasks", true)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            10002,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dueText = if (dueAt > 0L) {
            SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(dueAt))
        } else {
            "未设置截止时间"
        }
        val prefix = courseName.takeIf { it.isNotBlank() }?.let { "$it · " }.orEmpty()
        val reminderText = when (reminderMinutes) {
            0 -> "现在截止"
            in 1..59 -> "$reminderMinutes 分钟后截止"
            60 -> "1 小时后截止"
            in 61..1439 -> "约 ${reminderMinutes / 60} 小时后截止"
            1440 -> "明天截止"
            in 1441..10079 -> "约 ${reminderMinutes / 1440} 天后截止"
            10080 -> "一周后截止"
            else -> "截止时间 · $dueText"
        }
        val contentText = "$prefix$reminderText · $dueText"
        val bigText = buildString {
            append(contentText)
            if (note.isNotBlank()) {
                append("\n")
                append(note)
            }
        }

        val notification = NotificationCompat.Builder(context, TASK_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("DDL 提醒 · $taskTitle")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

}
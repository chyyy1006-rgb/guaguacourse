package com.example.npucourse.notification

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * 集中判断课程 / DDL 提醒所需的系统能力。
 *
 * “悬浮提醒”这里指 Android 的 heads-up 横幅通知，不申请 SYSTEM_ALERT_WINDOW。
 * 横幅与铃声在 Android 8+ 由通知渠道控制；用户在系统设置中的选择始终优先。
 */
object ReminderPermissionManager {

    private const val PREFS_NAME = "reminder_permission_guide"
    private const val KEY_FIRST_LAUNCH_PROMPT_SHOWN = "first_launch_prompt_shown_v1"

    data class Status(
        val runtimeNotificationPermissionGranted: Boolean,
        val appNotificationsEnabled: Boolean,
        val taskChannelHeadsUpAndSoundEnabled: Boolean,
        val exactAlarmAllowed: Boolean
    ) {
        val notificationAccessReady: Boolean
            get() = runtimeNotificationPermissionGranted && appNotificationsEnabled

        val fullyReady: Boolean
            get() = notificationAccessReady &&
                taskChannelHeadsUpAndSoundEnabled &&
                exactAlarmAllowed
    }

    fun status(context: Context): Status {
        NotificationHelper.createTaskReminderChannel(context)

        val runtimeGranted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

        val appNotificationsEnabled =
            NotificationManagerCompat.from(context).areNotificationsEnabled()

        val channelReady = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            true
        } else {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = manager.getNotificationChannel(NotificationHelper.TASK_CHANNEL_ID)
            channel != null &&
                channel.importance >= NotificationManager.IMPORTANCE_HIGH &&
                channel.sound != null
        }

        return Status(
            runtimeNotificationPermissionGranted = runtimeGranted,
            appNotificationsEnabled = appNotificationsEnabled,
            taskChannelHeadsUpAndSoundEnabled = channelReady,
            exactAlarmAllowed = CourseAlarmScheduler.canScheduleExactAlarms(context)
        )
    }

    fun shouldShowFirstLaunchPrompt(context: Context): Boolean =
        !context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_FIRST_LAUNCH_PROMPT_SHOWN, false)

    fun markFirstLaunchPromptShown(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_FIRST_LAUNCH_PROMPT_SHOWN, true)
            .apply()
    }

    fun appNotificationSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }

    fun taskChannelSettingsIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, NotificationHelper.TASK_CHANNEL_ID)
            }
        } else {
            appNotificationSettingsIntent(context)
        }

    fun exactAlarmSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
        }
}

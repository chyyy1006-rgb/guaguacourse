package com.example.npucourse.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * Android 会根据 Doze 和厂商省电策略对非精确闹钟进行合并，因此 30 分钟是目标间隔，
 * 不是秒级保证。真正需要联网和 WebView 的工作由前台数据同步服务完成。
 */
object CampusAutoSyncScheduler {
    private const val REQUEST_CODE = 52_030
    private const val INTERVAL_MILLIS = 30L * 60L * 1000L
    private const val FIRST_RUN_DELAY_MILLIS = 2L * 60L * 1000L

    fun schedule(context: Context) {
        val app = context.applicationContext
        val manager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + FIRST_RUN_DELAY_MILLIS,
            INTERVAL_MILLIS,
            pendingIntent(app)
        )
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, CampusAutoSyncReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}

class CampusAutoSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        runCatching {
            ContextCompat.startForegroundService(
                context.applicationContext,
                Intent(context, CampusAutoSyncService::class.java)
            )
        }
    }
}

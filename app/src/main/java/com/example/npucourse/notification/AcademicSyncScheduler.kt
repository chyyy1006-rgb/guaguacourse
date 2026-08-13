package com.example.npucourse.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/** 低频后台检查。使用非精确 Alarm，不额外申请后台任务依赖。 */
object AcademicSyncScheduler {
    private const val REQUEST_CODE = 42_101
    private const val INTERVAL = 4 * 60 * 60 * 1000L

    fun schedule(context: Context, enabled: Boolean) {
        val app = context.applicationContext
        val manager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = pendingIntent(app)
        manager.cancel(pending)
        if (!enabled) return
        manager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 60 * 60 * 1000L,
            INTERVAL,
            pending
        )
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, AcademicSyncReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}

package com.example.npucourse.notification

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.example.npucourse.data.academic.AcademicTimeParser
import com.example.npucourse.importer.NwpuExamRecord

object ExamAlarmScheduler {
    private const val PREFS = "exam_alarm_registry"
    private const val KEY_URIS = "scheduled_exam_alarm_uris"
    val DEFAULT_OFFSETS_MINUTES = listOf(24 * 60, 2 * 60)

    @SuppressLint("ScheduleExactAlarm")
    fun rescheduleAll(
        context: Context,
        exams: List<NwpuExamRecord>,
        enabled: Boolean,
        offsetsMinutes: List<Int> = DEFAULT_OFFSETS_MINUTES
    ) {
        val app = context.applicationContext
        cancelAll(app)
        if (!enabled) return
        val manager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()
        val uris = mutableSetOf<String>()

        exams.asSequence().filter { !it.finished }.forEach { exam ->
            val start = AcademicTimeParser.parseStartMillis(exam.timeText) ?: return@forEach
            offsetsMinutes.forEach offsetLoop@ { offset ->
                val trigger = start - offset * 60_000L
                if (trigger <= now) return@offsetLoop
                val uri = "npucourse://exam-reminder/${kotlin.math.abs((exam.courseName + exam.timeText).hashCode())}/$offset"
                val intent = Intent(app, ExamAlarmReceiver::class.java).apply {
                    data = Uri.parse(uri)
                    putExtra(ExamAlarmReceiver.EXTRA_COURSE, exam.courseName)
                    putExtra(ExamAlarmReceiver.EXTRA_TIME, exam.timeText)
                    putExtra(ExamAlarmReceiver.EXTRA_LOCATION, exam.location)
                    putExtra(ExamAlarmReceiver.EXTRA_MINUTES, offset)
                }
                val pending = PendingIntent.getBroadcast(
                    app,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                try {
                    when {
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.M -> manager.setExact(AlarmManager.RTC_WAKEUP, trigger, pending)
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms() ->
                            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
                        else -> manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
                    }
                    uris += uri
                } catch (_: SecurityException) {
                    // 没有精确闹钟权限时不影响查询主流程。
                }
            }
        }
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putStringSet(KEY_URIS, uris).apply()
    }

    fun cancelAll(context: Context) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val manager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        prefs.getStringSet(KEY_URIS, emptySet())?.forEach { uri ->
            val intent = Intent(app, ExamAlarmReceiver::class.java).apply { data = Uri.parse(uri) }
            PendingIntent.getBroadcast(
                app, 0, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )?.let { manager.cancel(it); it.cancel() }
        }
        prefs.edit().remove(KEY_URIS).apply()
    }
}

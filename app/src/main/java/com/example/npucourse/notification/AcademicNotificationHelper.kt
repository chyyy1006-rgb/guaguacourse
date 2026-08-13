package com.example.npucourse.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.npucourse.MainActivity
import com.example.npucourse.R
import com.example.npucourse.data.academic.AcademicCacheStore
import com.example.npucourse.importer.NwpuExamRecord

object AcademicNotificationHelper {
    private const val CHANNEL_ID = "academic_updates"
    private const val EXAM_BASE_ID = 41_000
    private const val GRADE_ID = 41_900

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "考试与成绩",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "考试倒计时与新成绩提醒"
                enableVibration(true)
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    fun showExamReminder(
        context: Context,
        exam: NwpuExamRecord,
        minutesBefore: Int
    ) {
        if (!hasPermission(context)) return
        createChannel(context)
        val whenText = when {
            minutesBefore >= 1440 && minutesBefore % 1440 == 0 -> "还有 ${minutesBefore / 1440} 天"
            minutesBefore >= 60 && minutesBefore % 60 == 0 -> "还有 ${minutesBefore / 60} 小时"
            else -> "还有 $minutesBefore 分钟"
        }
        val content = buildString {
            append(whenText)
            if (exam.timeText.isNotBlank()) append(" · ${exam.timeText}")
            if (exam.location.isNotBlank()) append(" · ${exam.location}")
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("${exam.courseName.ifBlank { "考试" }} 即将开始")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAcademicPendingIntent(context))
            .build()
        val id = EXAM_BASE_ID + kotlin.math.abs((exam.courseName + exam.timeText).hashCode() % 800)
        context.getSystemService(NotificationManager::class.java).notify(id, notification)
    }

    @SuppressLint("MissingPermission")
    fun showGradeChanges(
        context: Context,
        changes: List<AcademicCacheStore.GradeChange>
    ) {
        if (changes.isEmpty() || !hasPermission(context)) return
        createChannel(context)
        val first = changes.first()
        val title = if (changes.size == 1) {
            "${first.courseName} 成绩已更新"
        } else {
            "检测到 ${changes.size} 门课程成绩更新"
        }
        val text = if (changes.size == 1) {
            buildString {
                append("成绩 ${first.grade}")
                first.gradePoint?.let { append(" · 绩点 ${formatNumber(it)}") }
                if (first.semesterName.isNotBlank()) append(" · ${first.semesterName}")
            }
        } else {
            changes.take(3).joinToString("；") { "${it.courseName} ${it.grade}" } +
                if (changes.size > 3) " 等" else ""
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAcademicPendingIntent(context))
            .build()
        context.getSystemService(NotificationManager::class.java).notify(GRADE_ID, notification)
    }

    private fun openAcademicPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_academic_info", true)
        }
        return PendingIntent.getActivity(
            context,
            41_001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun formatNumber(value: Double): String =
        "%.2f".format(java.util.Locale.US, value).trimEnd('0').trimEnd('.')
}

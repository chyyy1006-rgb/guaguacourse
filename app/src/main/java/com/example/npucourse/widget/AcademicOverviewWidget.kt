package com.example.npucourse.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.npucourse.MainActivity
import com.example.npucourse.R
import com.example.npucourse.data.academic.AcademicCacheStore
import com.example.npucourse.data.academic.AcademicTimeParser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AcademicOverviewWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { AcademicOverviewWidgetUpdater.update(context, manager, it) }
    }
}

object AcademicOverviewWidgetUpdater {
    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, AcademicOverviewWidgetProvider::class.java))
        ids.forEach { update(context, manager, it) }
    }

    fun update(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
        val grades = AcademicCacheStore.loadGrades(context)
        val exams = AcademicCacheStore.loadExams(context)
        val nextExam = AcademicCacheStore.nextExam(context)
        val views = RemoteViews(context.packageName, R.layout.widget_academic_overview)

        views.setTextViewText(R.id.widgetAcademicGpa, grades?.result?.gpa?.let { "GPA ${formatNumber(it)}" } ?: "GPA --")
        if (nextExam != null) {
            views.setTextViewText(R.id.widgetAcademicTitle, nextExam.courseName.ifBlank { "近期考试" })
            val countdown = AcademicTimeParser.countdownText(nextExam.timeText)
            views.setTextViewText(
                R.id.widgetAcademicMeta,
                listOfNotNull(countdown, nextExam.timeText.takeIf { it.isNotBlank() }).joinToString(" · ")
            )
            views.setTextViewText(R.id.widgetAcademicLocation, nextExam.location.ifBlank { "考试地点待定" })
        } else {
            views.setTextViewText(R.id.widgetAcademicTitle, "暂无近期考试")
            views.setTextViewText(R.id.widgetAcademicMeta, "打开瓜瓜课程表刷新考试与成绩")
            views.setTextViewText(R.id.widgetAcademicLocation, lastUpdatedText(grades?.updatedAt ?: exams?.updatedAt ?: 0L))
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_academic_info", true)
        }
        val pending = PendingIntent.getActivity(
            context,
            42_201,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetAcademicRoot, pending)
        manager.updateAppWidget(appWidgetId, views)
    }

    private fun lastUpdatedText(time: Long): String =
        if (time <= 0L) "尚未同步" else "上次同步 ${SimpleDateFormat("M/d HH:mm", Locale.CHINA).format(Date(time))}"

    private fun formatNumber(value: Double): String =
        "%.2f".format(Locale.US, value).trimEnd('0').trimEnd('.')
}

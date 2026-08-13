package com.example.npucourse.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.webkit.CookieManager
import com.example.npucourse.data.academic.AcademicCacheStore
import com.example.npucourse.data.academic.AcademicPreferencesStore
import com.example.npucourse.importer.NwpuAcademicDirectClient
import com.example.npucourse.widget.AcademicOverviewWidgetUpdater

class AcademicSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = AcademicPreferencesStore.get(context)
        if (!prefs.backgroundSyncEnabled) return
        // CookieManager 在主线程读取，网络请求再放到工作线程。
        val cookie = CookieManager.getInstance().getCookie("https://jwxt.nwpu.edu.cn").orEmpty()
        if (cookie.isBlank()) return
        val pending = goAsync()
        Thread {
            try {
                NwpuAcademicDirectClient.queryGrades(cookie).onSuccess { result ->
                    val changes = AcademicCacheStore.saveGrades(context, result)
                    if (prefs.gradeNotificationsEnabled) {
                        AcademicNotificationHelper.showGradeChanges(context, changes)
                    }
                }
                NwpuAcademicDirectClient.queryExams(cookie).onSuccess { result ->
                    AcademicCacheStore.saveExams(context, result)
                    ExamAlarmScheduler.rescheduleAll(
                        context,
                        result.exams,
                        prefs.examRemindersEnabled
                    )
                }
                AcademicOverviewWidgetUpdater.updateAll(context)
            } finally {
                pending.finish()
            }
        }.start()
    }
}

package com.example.npucourse.data.academic

import android.content.Context

object AcademicPreferencesStore {
    private const val PREFS = "academic_preferences_v413"
    private const val KEY_EXAM_REMINDERS = "exam_reminders"
    private const val KEY_GRADE_NOTIFICATIONS = "grade_notifications"
    private const val KEY_BACKGROUND_SYNC = "background_sync"
    private const val KEY_MASK_EXPORT = "mask_export"

    data class Preferences(
        val examRemindersEnabled: Boolean,
        val gradeNotificationsEnabled: Boolean,
        val backgroundSyncEnabled: Boolean,
        val maskExportEnabled: Boolean
    )

    fun get(context: Context): Preferences {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Preferences(
            examRemindersEnabled = prefs.getBoolean(KEY_EXAM_REMINDERS, true),
            gradeNotificationsEnabled = prefs.getBoolean(KEY_GRADE_NOTIFICATIONS, true),
            backgroundSyncEnabled = prefs.getBoolean(KEY_BACKGROUND_SYNC, true),
            maskExportEnabled = prefs.getBoolean(KEY_MASK_EXPORT, true)
        )
    }

    fun setExamReminders(context: Context, enabled: Boolean) = edit(context, KEY_EXAM_REMINDERS, enabled)
    fun setGradeNotifications(context: Context, enabled: Boolean) = edit(context, KEY_GRADE_NOTIFICATIONS, enabled)
    fun setBackgroundSync(context: Context, enabled: Boolean) = edit(context, KEY_BACKGROUND_SYNC, enabled)
    fun setMaskExport(context: Context, enabled: Boolean) = edit(context, KEY_MASK_EXPORT, enabled)

    private fun edit(context: Context, key: String, value: Boolean) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(key, value).apply()
    }
}

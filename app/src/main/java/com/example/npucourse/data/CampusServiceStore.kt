package com.example.npucourse.data

import android.content.Context

object CampusServiceStore {
    private const val PREFS = "campus_service_state"
    private const val ELECTRICITY_BALANCE = "electricity_balance"
    private const val ELECTRICITY_UPDATED_AT = "electricity_updated_at"
    private const val ELECTRICITY_ALERT_ACTIVE = "electricity_alert_active"
    private const val LAST_ELECTRICITY_SYNC = "last_electricity_sync"
    private const val LAST_SCHEDULE_SYNC = "last_schedule_sync"
    private const val TRACKED_COURSE_IDS = "tracked_course_ids"
    private const val LAST_AUTH_NOTICE = "last_auth_notice"

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun electricityBalance(context: Context): Double? =
        preferences(context).getString(ELECTRICITY_BALANCE, null)?.toDoubleOrNull()

    fun electricityUpdatedAt(context: Context): Long =
        preferences(context).getLong(ELECTRICITY_UPDATED_AT, 0L)

    fun saveElectricityBalance(context: Context, balance: Double, updatedAt: Long = System.currentTimeMillis()) {
        if (!balance.isFinite() || balance < 0.0) return
        preferences(context).edit()
            .putString(ELECTRICITY_BALANCE, balance.toString())
            .putLong(ELECTRICITY_UPDATED_AT, updatedAt)
            .putLong(LAST_ELECTRICITY_SYNC, updatedAt)
            .apply()
    }

    fun electricityAlertActive(context: Context): Boolean =
        preferences(context).getBoolean(ELECTRICITY_ALERT_ACTIVE, false)

    fun setElectricityAlertActive(context: Context, active: Boolean) {
        preferences(context).edit().putBoolean(ELECTRICITY_ALERT_ACTIVE, active).apply()
    }

    fun lastElectricitySync(context: Context): Long =
        preferences(context).getLong(LAST_ELECTRICITY_SYNC, 0L)

    fun lastScheduleSync(context: Context): Long =
        preferences(context).getLong(LAST_SCHEDULE_SYNC, 0L)

    fun markScheduleSynced(context: Context, timestamp: Long = System.currentTimeMillis()) {
        preferences(context).edit().putLong(LAST_SCHEDULE_SYNC, timestamp).apply()
    }

    fun trackedCourseIds(context: Context): Set<Long> =
        preferences(context).getStringSet(TRACKED_COURSE_IDS, emptySet()).orEmpty()
            .mapNotNull(String::toLongOrNull)
            .filterTo(mutableSetOf()) { it > 0L }

    fun setTrackedCourseIds(context: Context, ids: Set<Long>) {
        preferences(context).edit()
            .putStringSet(TRACKED_COURSE_IDS, ids.filter { it > 0L }.mapTo(mutableSetOf(), Long::toString))
            .apply()
    }

    fun lastAuthenticationNotice(context: Context): Long =
        preferences(context).getLong(LAST_AUTH_NOTICE, 0L)

    fun markAuthenticationNotice(context: Context, timestamp: Long = System.currentTimeMillis()) {
        preferences(context).edit().putLong(LAST_AUTH_NOTICE, timestamp).apply()
    }
}

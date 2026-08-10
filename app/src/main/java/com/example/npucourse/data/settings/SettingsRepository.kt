package com.example.npucourse.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.npucourse.util.todayStartMillis
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "npu_course_settings"
)

object ThemeMode {
    const val SYSTEM = "SYSTEM"
    const val LIGHT = "LIGHT"
    const val DARK = "DARK"
}

object AccentStyle {
    const val INDIGO = "INDIGO"
    const val BLUE = "BLUE"
    const val GREEN = "GREEN"
    const val ROSE = "ROSE"
    const val ORANGE = "ORANGE"
}

object UiDensity {
    const val COMPACT = "COMPACT"
    const val STANDARD = "STANDARD"
    const val COMFORTABLE = "COMFORTABLE"
}

object AppIconStyle {
    // v4.9.1 起使用 4 套“瓜瓜课程表”品牌图标。
    const val WATERMELON = "WATERMELON"
    const val COURSE = "COURSE"
    const val PLANNER = "PLANNER"
    const val MASCOT = "MASCOT"

    // 旧版序列化值保留用于升级/备份兼容，不再显示在设置页。
    const val CLASSIC = "CLASSIC"
    const val INDIGO = "INDIGO"
    const val BLUE = "BLUE"
    const val OBSIDIAN = "OBSIDIAN"
    const val ROSE = "ROSE"
    const val SUNSET = "SUNSET"

    val ALL = setOf(WATERMELON, COURSE, PLANNER, MASCOT)

    fun normalize(value: String): String =
        when (value) {
            WATERMELON, COURSE, PLANNER, MASCOT -> value
            INDIGO -> COURSE
            BLUE -> PLANNER
            OBSIDIAN -> MASCOT
            CLASSIC, ROSE, SUNSET -> WATERMELON
            else -> WATERMELON
        }
}

object CourseCardStyle {
    const val MINIMAL = "MINIMAL"
    const val STANDARD = "STANDARD"
    const val DETAILED = "DETAILED"
}

data class AppSettings(
    val semesterStartMillis: Long,
    val campus: String,
    val reminderMinutes: Int,
    val showWeekends: Boolean,
    val activeSemesterId: Long,
    val themeMode: String,
    val accentStyle: String,
    val dynamicColor: Boolean,
    val uiDensity: String,
    val appIconStyle: String,
    val courseCardStyle: String,
    val showSectionTimes: Boolean
)

class SettingsRepository(
    private val context: Context
) {

    private object Keys {
        val SEMESTER_START = longPreferencesKey("semester_start_millis")
        val CAMPUS = stringPreferencesKey("campus")
        val REMINDER_MINUTES = intPreferencesKey("reminder_minutes")
        val SHOW_WEEKENDS = booleanPreferencesKey("show_weekends")
        val ACTIVE_SEMESTER_ID = longPreferencesKey("active_semester_id")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_STYLE = stringPreferencesKey("accent_style")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val UI_DENSITY = stringPreferencesKey("ui_density")
        val APP_ICON_STYLE = stringPreferencesKey("app_icon_style")
        val COURSE_CARD_STYLE = stringPreferencesKey("course_card_style")
        val SHOW_SECTION_TIMES = booleanPreferencesKey("show_section_times")
    }

    val settings: Flow<AppSettings> =
        context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                AppSettings(
                    semesterStartMillis =
                        preferences[Keys.SEMESTER_START] ?: todayStartMillis(),
                    campus =
                        preferences[Keys.CAMPUS] ?: "CHANGAN",
                    reminderMinutes =
                        preferences[Keys.REMINDER_MINUTES] ?: 10,
                    showWeekends =
                        preferences[Keys.SHOW_WEEKENDS] ?: true,
                    activeSemesterId =
                        preferences[Keys.ACTIVE_SEMESTER_ID] ?: 0L,
                    themeMode =
                        preferences[Keys.THEME_MODE] ?: ThemeMode.SYSTEM,
                    accentStyle =
                        preferences[Keys.ACCENT_STYLE] ?: AccentStyle.INDIGO,
                    dynamicColor =
                        preferences[Keys.DYNAMIC_COLOR] ?: false,
                    uiDensity =
                        preferences[Keys.UI_DENSITY] ?: UiDensity.STANDARD,
                    appIconStyle =
                        AppIconStyle.normalize(
                            preferences[Keys.APP_ICON_STYLE] ?: AppIconStyle.WATERMELON
                        ),
                    courseCardStyle =
                        preferences[Keys.COURSE_CARD_STYLE] ?: CourseCardStyle.STANDARD,
                    showSectionTimes =
                        preferences[Keys.SHOW_SECTION_TIMES] ?: true
                )
            }

    suspend fun setSemesterStartMillis(millis: Long) {
        context.dataStore.edit { it[Keys.SEMESTER_START] = millis }
    }

    suspend fun setCampus(campus: String) {
        context.dataStore.edit { it[Keys.CAMPUS] = campus }
    }

    suspend fun setReminderMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.REMINDER_MINUTES] = minutes }
    }

    suspend fun setShowWeekends(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_WEEKENDS] = show }
    }

    suspend fun setActiveSemesterId(semesterId: Long) {
        context.dataStore.edit { it[Keys.ACTIVE_SEMESTER_ID] = semesterId }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setAccentStyle(style: String) {
        context.dataStore.edit { it[Keys.ACCENT_STYLE] = style }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setUiDensity(density: String) {
        context.dataStore.edit { it[Keys.UI_DENSITY] = density }
    }

    suspend fun setAppIconStyle(style: String) {
        context.dataStore.edit {
            it[Keys.APP_ICON_STYLE] = AppIconStyle.normalize(style)
        }
    }

    suspend fun setCourseCardStyle(style: String) {
        context.dataStore.edit { it[Keys.COURSE_CARD_STYLE] = style }
    }

    suspend fun setShowSectionTimes(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_SECTION_TIMES] = show }
    }

    suspend fun restoreSettings(settings: AppSettings) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SEMESTER_START] = settings.semesterStartMillis
            preferences[Keys.CAMPUS] = settings.campus
            preferences[Keys.REMINDER_MINUTES] = settings.reminderMinutes
            preferences[Keys.SHOW_WEEKENDS] = settings.showWeekends
            preferences[Keys.ACTIVE_SEMESTER_ID] = settings.activeSemesterId
            preferences[Keys.THEME_MODE] = settings.themeMode
            preferences[Keys.ACCENT_STYLE] = settings.accentStyle
            preferences[Keys.DYNAMIC_COLOR] = settings.dynamicColor
            preferences[Keys.UI_DENSITY] = settings.uiDensity
            preferences[Keys.APP_ICON_STYLE] = AppIconStyle.normalize(settings.appIconStyle)
            preferences[Keys.COURSE_CARD_STYLE] = settings.courseCardStyle
            preferences[Keys.SHOW_SECTION_TIMES] = settings.showSectionTimes
        }
    }
}

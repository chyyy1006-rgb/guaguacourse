package com.example.npucourse.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.npucourse.data.settings.AccentStyle
import com.example.npucourse.data.settings.AppIconStyle
import com.example.npucourse.data.settings.AppSettings
import com.example.npucourse.data.settings.CourseCardStyle
import com.example.npucourse.data.settings.SettingsRepository
import com.example.npucourse.data.settings.ThemeMode
import com.example.npucourse.data.settings.UiDensity
import com.example.npucourse.launcher.LauncherIconManager
import com.example.npucourse.util.todayStartMillis
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val initialIconStyle: String
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        repository.settings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSettings(
                semesterStartMillis = todayStartMillis(),
                campus = "CHANGAN",
                reminderMinutes = 10,
                showWeekends = true,
                activeSemesterId = 0L,
                themeMode = ThemeMode.SYSTEM,
                accentStyle = AccentStyle.INDIGO,
                dynamicColor = false,
                uiDensity = UiDensity.STANDARD,
                appIconStyle = AppIconStyle.normalize(initialIconStyle),
                courseCardStyle = CourseCardStyle.STANDARD,
                showSectionTimes = true
            )
        )

    fun setSemesterStartMillis(millis: Long) =
        viewModelScope.launch { repository.setSemesterStartMillis(millis) }

    fun setCampus(campus: String) =
        viewModelScope.launch { repository.setCampus(campus) }

    fun setReminderMinutes(minutes: Int) =
        viewModelScope.launch { repository.setReminderMinutes(minutes) }

    fun setShowWeekends(show: Boolean) =
        viewModelScope.launch { repository.setShowWeekends(show) }

    fun setActiveSemesterId(semesterId: Long) =
        viewModelScope.launch { repository.setActiveSemesterId(semesterId) }

    fun setThemeMode(mode: String) =
        viewModelScope.launch { repository.setThemeMode(mode) }

    fun setAccentStyle(style: String) =
        viewModelScope.launch { repository.setAccentStyle(style) }

    fun setDynamicColor(enabled: Boolean) =
        viewModelScope.launch { repository.setDynamicColor(enabled) }

    fun setUiDensity(density: String) =
        viewModelScope.launch { repository.setUiDensity(density) }

    fun setAppIconStyle(style: String) =
        viewModelScope.launch { repository.setAppIconStyle(style) }

    fun setCourseCardStyle(style: String) =
        viewModelScope.launch { repository.setCourseCardStyle(style) }

    fun setShowSectionTimes(show: Boolean) =
        viewModelScope.launch { repository.setShowSectionTimes(show) }

    class Factory(context: Context) : ViewModelProvider.Factory {
        private val applicationContext = context.applicationContext

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                repository = SettingsRepository(applicationContext),
                initialIconStyle = LauncherIconManager.currentStyle(applicationContext)
            ) as T
        }
    }
}

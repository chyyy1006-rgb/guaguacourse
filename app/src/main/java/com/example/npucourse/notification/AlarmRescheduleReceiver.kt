package com.example.npucourse.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.npucourse.data.AppDatabase
import com.example.npucourse.data.academic.AcademicCacheStore
import com.example.npucourse.data.academic.AcademicPreferencesStore
import com.example.npucourse.data.CourseRepository
import com.example.npucourse.data.SemesterRepository
import com.example.npucourse.data.settings.SettingsRepository
import com.example.npucourse.widget.AcademicOverviewWidgetUpdater
import com.example.npucourse.widget.TodayScheduleWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class AlarmRescheduleReceiver :
    BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val pendingResult =
            goAsync()

        CoroutineScope(
            Dispatchers.IO
        ).launch {

            try {

                val applicationContext =
                    context.applicationContext

                val database =
                    AppDatabase.getInstance(
                        applicationContext
                    )

                val courseRepository =
                    CourseRepository(
                        dao = database.courseDao(),
                        taskDao = database.taskDao(),
                        database = database
                    )

                val semesterRepository =
                    SemesterRepository(
                        semesterDao = database.semesterDao(),
                        courseDao = database.courseDao(),
                        taskDao = database.taskDao(),
                        database = database
                    )

                semesterRepository
                    .ensureDefaultSemester()

                val allCourses =
                    courseRepository
                        .courses
                        .first()

                val semesters =
                    semesterRepository
                        .semesters
                        .first()

                val settingsRepository =
                    SettingsRepository(
                        applicationContext
                    )

                val settings =
                    settingsRepository
                        .settings
                        .first()

                val selectedSemester =
                    semesters.firstOrNull {
                        it.id ==
                            settings.activeSemesterId
                    }
                        ?: semesters.firstOrNull()

                if (selectedSemester == null) {
                    CourseAlarmScheduler.cancelAll(applicationContext)
                } else {
                    val selectedCourses = allCourses.filter {
                        it.semesterId == selectedSemester.id
                    }

                    CourseAlarmScheduler.rescheduleAll(
                        context = applicationContext,
                        courses = selectedCourses,
                        semesterStartMillis = selectedSemester.startMillis,
                        campus = selectedSemester.campus,
                        reminderMinutes = settings.reminderMinutes
                    )
                }

                TaskAlarmScheduler.rescheduleAll(
                    context = applicationContext,
                    tasks = database.taskDao().getAllTasksOnce(),
                    courseNamesById = allCourses.associate { it.id to it.name }
                )

                TodayScheduleWidgetUpdater.updateAll(
                    applicationContext
                )

                val academicPreferences = AcademicPreferencesStore.get(applicationContext)
                ExamAlarmScheduler.rescheduleAll(
                    context = applicationContext,
                    exams = AcademicCacheStore.loadExams(applicationContext)?.result?.exams.orEmpty(),
                    enabled = academicPreferences.examRemindersEnabled
                )
                AcademicSyncScheduler.schedule(
                    applicationContext,
                    academicPreferences.backgroundSyncEnabled
                )
                AcademicOverviewWidgetUpdater.updateAll(applicationContext)

            } finally {
                pendingResult.finish()
            }
        }
    }
}

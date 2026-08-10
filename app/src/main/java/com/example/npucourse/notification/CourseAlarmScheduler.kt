package com.example.npucourse.notification

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.example.npucourse.model.DemoCourse
import com.example.npucourse.model.activeWeeks
import com.example.npucourse.util.getScheduleForCampus
import com.example.npucourse.util.semesterWeekDateMillis
import java.util.Calendar


object CourseAlarmScheduler {

    private const val PREFS_NAME =
        "course_alarm_registry"

    private const val KEY_ALARM_URIS =
        "scheduled_alarm_uris"


    /*
     * =====================================================
     * 是否拥有精确闹钟能力
     * =====================================================
     */

    fun canScheduleExactAlarms(
        context: Context
    ): Boolean {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.S
        ) {

            return true
        }

        val alarmManager =
            context.getSystemService(
                AlarmManager::class.java
            )

        return alarmManager
            .canScheduleExactAlarms()
    }


    /*
     * =====================================================
     * 重新安排全部课程
     * =====================================================
     */

    @SuppressLint(
        "ScheduleExactAlarm"
    )
    fun rescheduleAll(
        context: Context,
        courses: List<DemoCourse>,
        semesterStartMillis: Long,
        campus: String,
        reminderMinutes: Int
    ) {

        /*
         * 先删除旧提醒。
         */
        cancelAll(context)

        /*
         * -1 = 用户关闭提醒。
         */
        if (
            reminderMinutes < 0
        ) {
            return
        }

        if (
            !canScheduleExactAlarms(
                context
            )
        ) {
            return
        }

        val alarmManager =
            context.getSystemService(
                AlarmManager::class.java
            )

        val newAlarmUris =
            mutableSetOf<String>()

        val now =
            System.currentTimeMillis()

        courses.forEach { course ->

            if (!course.reminderEnabled) {
                return@forEach
            }

            val effectiveReminderMinutes =
                if (course.reminderMinutesOverride >= 0) {
                    course.reminderMinutesOverride
                } else {
                    reminderMinutes
                }

            /*
             * =================================================
             * 新逻辑：
             *
             * 不再使用 startWeek..endWeek
             *
             * 直接取得这门课真正需要上课的周。
             * =================================================
             */
            val weeks =
                course.activeWeeks()

            weeks.forEach { week ->

                val courseStartMillis =
                    calculateCourseStartMillis(
                        semesterStartMillis =
                            semesterStartMillis,
                        campus =
                            campus,
                        course =
                            course,
                        week =
                            week
                    )
                        ?: return@forEach

                val triggerAtMillis =
                    courseStartMillis -
                            effectiveReminderMinutes *
                            60_000L

                /*
                 * 已经过期的不注册。
                 */
                if (
                    triggerAtMillis <= now
                ) {
                    return@forEach
                }

                val startTime =
                    getCourseStartTime(
                        campus =
                            campus,
                        course =
                            course
                    )

                val alarmUri =
                    buildAlarmUri(
                        courseId =
                            course.id,
                        week =
                            week
                    )

                val pendingIntent =
                    createAlarmPendingIntent(
                        context =
                            context,
                        alarmUri =
                            alarmUri,
                        course =
                            course,
                        startTime =
                            startTime,
                        reminderMinutes =
                            effectiveReminderMinutes
                    )

                try {

                    alarmManager
                        .setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )

                    newAlarmUris.add(
                        alarmUri
                    )

                } catch (
                    exception: SecurityException
                ) {

                    /*
                     * 用户可能临时撤回权限。
                     * 不让 App 因此崩溃。
                     */
                }
            }
        }

        saveAlarmUris(
            context =
                context,
            alarmUris =
                newAlarmUris
        )
    }


    /*
     * =====================================================
     * 取消全部课程提醒
     * =====================================================
     */

    fun cancelAll(
        context: Context
    ) {

        val alarmManager =
            context.getSystemService(
                AlarmManager::class.java
            )

        val preferences =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val alarmUris =
            preferences
                .getStringSet(
                    KEY_ALARM_URIS,
                    emptySet()
                )
                ?.toSet()
                ?: emptySet()

        alarmUris.forEach { alarmUri ->

            val intent =
                Intent(
                    context,
                    CourseAlarmReceiver::class.java
                ).apply {

                    data =
                        Uri.parse(
                            alarmUri
                        )
                }

            val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or
                            PendingIntent.FLAG_IMMUTABLE
                )

            if (
                pendingIntent != null
            ) {

                alarmManager.cancel(
                    pendingIntent
                )

                pendingIntent.cancel()
            }
        }

        preferences
            .edit()
            .remove(
                KEY_ALARM_URIS
            )
            .apply()
    }


    /*
     * =====================================================
     * 计算某周课程实际开始时间
     * =====================================================
     */

    private fun calculateCourseStartMillis(
        semesterStartMillis: Long,
        campus: String,
        course: DemoCourse,
        week: Int
    ): Long? {

        val startTime =
            getScheduleForCampus(
                campus
            )
                .firstOrNull {

                    it.section ==
                            course.startSection
                }
                ?.startTime
                ?: return null

        val timeParts =
            startTime.split(":")

        if (
            timeParts.size != 2
        ) {
            return null
        }

        val hour =
            timeParts[0]
                .toIntOrNull()
                ?: return null

        val minute =
            timeParts[1]
                .toIntOrNull()
                ?: return null

        val courseDateMillis =
            semesterWeekDateMillis(
                semesterStartMillis =
                    semesterStartMillis,

                week =
                    week,

                day =
                    course.day
            )

        /*
         * 如果用户把“开学日期”设在某周中间，
         * 第1周周一～开学日前的日期不应创建提醒。
         */
        if (
            courseDateMillis <
            semesterStartMillis
        ) {
            return null
        }

        val calendar =
            Calendar
                .getInstance()
                .apply {

                    timeInMillis =
                        courseDateMillis

                    set(
                        Calendar.HOUR_OF_DAY,
                        hour
                    )

                    set(
                        Calendar.MINUTE,
                        minute
                    )

                    set(
                        Calendar.SECOND,
                        0
                    )

                    set(
                        Calendar.MILLISECOND,
                        0
                    )
                }

        return calendar.timeInMillis
    }


    /*
     * =====================================================
     * 获取课程开始时间
     * =====================================================
     */

    private fun getCourseStartTime(
        campus: String,
        course: DemoCourse
    ): String {

        return getScheduleForCampus(
            campus
        )
            .firstOrNull {

                it.section ==
                        course.startSection
            }
            ?.startTime
            ?: "--:--"
    }


    /*
     * =====================================================
     * 每门课程每周唯一 Alarm URI
     * =====================================================
     */

    private fun buildAlarmUri(
        courseId: Long,
        week: Int
    ): String {

        return "npucourse://" +
                "course-reminder/" +
                courseId +
                "/" +
                week
    }


    /*
     * =====================================================
     * PendingIntent
     * =====================================================
     */

    private fun createAlarmPendingIntent(
        context: Context,
        alarmUri: String,
        course: DemoCourse,
        startTime: String,
        reminderMinutes: Int
    ): PendingIntent {

        val intent =
            Intent(
                context,
                CourseAlarmReceiver::class.java
            ).apply {

                data =
                    Uri.parse(
                        alarmUri
                    )

                putExtra(
                    CourseAlarmReceiver
                        .EXTRA_COURSE_NAME,
                    course.name
                )

                putExtra(
                    CourseAlarmReceiver
                        .EXTRA_ROOM,
                    course.room
                )

                putExtra(
                    CourseAlarmReceiver
                        .EXTRA_START_TIME,
                    startTime
                )

                putExtra(
                    CourseAlarmReceiver
                        .EXTRA_REMINDER_MINUTES,
                    reminderMinutes
                )
            }

        return PendingIntent
            .getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )
    }


    /*
     * =====================================================
     * 保存闹钟注册表
     * =====================================================
     */

    private fun saveAlarmUris(
        context: Context,
        alarmUris: Set<String>
    ) {

        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putStringSet(
                KEY_ALARM_URIS,
                alarmUris
            )
            .apply()
    }
}
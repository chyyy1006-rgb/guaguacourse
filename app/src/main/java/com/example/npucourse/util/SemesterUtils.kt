package com.example.npucourse.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

const val MAX_SEMESTER_WEEKS = 20


fun todayStartMillis(): Long {

    return startOfDay(
        Calendar.getInstance()
            .timeInMillis
    )
}


/* =========================================================
   学期周锚点
   =========================================================
 *
 * 课表一周固定按：周一 ～ 周日 显示。
 *
 * 用户手动创建课表时，开学日期有可能不是周一。
 * 如果直接把“开学日期”当成周一，就会出现：
 *
 * 周一 8号
 * 周二 9号
 * ...
 *
 * 但真实日历中 8号可能其实是周六。
 *
 * 因此：
 * - semesterStartMillis 仍保存用户选择/学校识别出的真实开学日期；
 * - 周次和课表日期统一锚定到“开学日期所在周的周一”。
 */
fun semesterWeekOneMondayMillis(
    semesterStartMillis: Long
): Long {

    val calendar =
        Calendar.getInstance().apply {

            timeInMillis =
                startOfDay(
                    semesterStartMillis
                )

            val courseDay =
                calendarDayToCourseDay(
                    get(
                        Calendar.DAY_OF_WEEK
                    )
                )

            add(
                Calendar.DAY_OF_YEAR,
                -(courseDay - 1)
            )
        }

    return calendar.timeInMillis
}


/* =========================================================
   获取某教学周某一天的真实日期
   =========================================================
 *
 * week: 1..20
 * day : 1=周一 ... 7=周日
 */
fun semesterWeekDateMillis(
    semesterStartMillis: Long,
    week: Int,
    day: Int
): Long {

    return Calendar
        .getInstance()
        .apply {

            timeInMillis =
                semesterWeekOneMondayMillis(
                    semesterStartMillis
                )

            add(
                Calendar.DAY_OF_YEAR,
                (week - 1) * 7 +
                    (day - 1)
            )
        }
        .timeInMillis
}


fun calculateCurrentWeek(
    semesterStartMillis: Long
): Int {

    val actualStartMillis =
        startOfDay(
            semesterStartMillis
        )

    val todayMillis =
        todayStartMillis()

    /*
     * 真实开学日期还没到。
     *
     * 即使“开学日期所在周的周一”已经到了，
     * 也不能提前判定为第1周。
     */
    if (
        todayMillis <
        actualStartMillis
    ) {

        return 0
    }

    val weekOneMondayMillis =
        semesterWeekOneMondayMillis(
            semesterStartMillis
        )

    val differenceMillis =
        todayMillis -
            weekOneMondayMillis

    val oneDayMillis =
        24L *
            60L *
            60L *
            1000L

    val differenceDays =
        differenceMillis /
            oneDayMillis

    return (
        differenceDays /
            7L
        ).toInt() + 1
}


fun formatSemesterDate(
    millis: Long
): String {

    val formatter =
        SimpleDateFormat(
            "yyyy年M月d日",
            Locale.CHINA
        )

    return formatter.format(
        millis
    )
}


private fun startOfDay(
    millis: Long
): Long {

    return Calendar
        .getInstance()
        .apply {

            timeInMillis =
                millis

            set(
                Calendar.HOUR_OF_DAY,
                0
            )

            set(
                Calendar.MINUTE,
                0
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
        .timeInMillis
}


private fun calendarDayToCourseDay(
    calendarDay: Int
): Int {

    return when (
        calendarDay
    ) {

        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        Calendar.SUNDAY -> 7

        else -> 1
    }
}

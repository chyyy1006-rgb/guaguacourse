package com.example.npucourse.util

import com.example.npucourse.model.DemoCourse
import com.example.npucourse.model.isActiveInWeek

data class FreeWindow(
    val day: Int,
    val startMinutes: Int,
    val endMinutes: Int
) {
    val durationMinutes: Int get() = endMinutes - startMinutes
}

data class CourseConflict(
    val day: Int,
    val first: DemoCourse,
    val second: DemoCourse
)

data class WeeklyScheduleInsights(
    val activeCourseCount: Int,
    val scheduledMinutes: Int,
    val busiestDay: Int?,
    val freeWindows: List<FreeWindow>,
    val conflicts: List<CourseConflict>
)

fun buildWeeklyScheduleInsights(
    courses: List<DemoCourse>,
    week: Int,
    campus: String,
    dayStartMinutes: Int = 8 * 60,
    dayEndMinutes: Int = 22 * 60,
    minimumFreeMinutes: Int = 60
): WeeklyScheduleInsights {
    if (week !in 1..20) {
        return WeeklyScheduleInsights(0, 0, null, emptyList(), emptyList())
    }

    val schedule = getScheduleForCampus(campus)
    val active = courses.filter { it.isActiveInWeek(week) }

    fun sectionStart(section: Int): Int? = schedule
        .firstOrNull { it.section == section }
        ?.startTime
        ?.let(::scheduleTimeToMinutes)

    fun sectionEnd(section: Int): Int? = schedule
        .firstOrNull { it.section == section }
        ?.endTime
        ?.let(::scheduleTimeToMinutes)

    val intervalsByDay = (1..7).associateWith { day ->
        active
            .filter { it.day == day }
            .mapNotNull { course ->
                val start = sectionStart(course.startSection) ?: return@mapNotNull null
                val end = sectionEnd(course.endSection) ?: return@mapNotNull null
                Triple(start, end, course)
            }
            .sortedBy { it.first }
    }

    val conflicts = mutableListOf<CourseConflict>()
    intervalsByDay.forEach { (day, intervals) ->
        for (i in intervals.indices) {
            for (j in i + 1 until intervals.size) {
                val a = intervals[i]
                val b = intervals[j]
                if (b.first >= a.second) break
                if (maxOf(a.first, b.first) < minOf(a.second, b.second)) {
                    conflicts += CourseConflict(day, a.third, b.third)
                }
            }
        }
    }

    val freeWindows = mutableListOf<FreeWindow>()
    intervalsByDay.forEach { (day, intervals) ->
        val merged = mutableListOf<Pair<Int, Int>>()
        intervals.forEach intervalLoop@{ (rawStart, rawEnd, _) ->
            val start = rawStart.coerceIn(dayStartMinutes, dayEndMinutes)
            val end = rawEnd.coerceIn(dayStartMinutes, dayEndMinutes)
            if (end <= start) return@intervalLoop

            val last = merged.lastOrNull()
            if (last == null || start > last.second) {
                merged += start to end
            } else {
                merged[merged.lastIndex] = last.first to maxOf(last.second, end)
            }
        }

        var cursor = dayStartMinutes
        merged.forEach { interval ->
            if (interval.first - cursor >= minimumFreeMinutes) {
                freeWindows += FreeWindow(day, cursor, interval.first)
            }
            cursor = maxOf(cursor, interval.second)
        }
        if (dayEndMinutes - cursor >= minimumFreeMinutes) {
            freeWindows += FreeWindow(day, cursor, dayEndMinutes)
        }
    }

    val scheduledMinutes = intervalsByDay.values
        .flatten()
        .sumOf { (start, end, _) -> (end - start).coerceAtLeast(0) }

    val busiestDay = intervalsByDay
        .maxByOrNull { (_, intervals) ->
            intervals.sumOf { (start, end, _) -> (end - start).coerceAtLeast(0) }
        }
        ?.takeIf { it.value.isNotEmpty() }
        ?.key

    return WeeklyScheduleInsights(
        activeCourseCount = active.size,
        scheduledMinutes = scheduledMinutes,
        busiestDay = busiestDay,
        freeWindows = freeWindows,
        conflicts = conflicts
    )
}

fun scheduleTimeToMinutes(text: String): Int {
    val parts = text.split(':')
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return hour * 60 + minute
}

fun minutesToClockText(minutes: Int): String {
    val safe = minutes.coerceIn(0, 24 * 60)
    return "%02d:%02d".format(safe / 60, safe % 60)
}

fun weekdayText(day: Int): String = when (day) {
    1 -> "周一"
    2 -> "周二"
    3 -> "周三"
    4 -> "周四"
    5 -> "周五"
    6 -> "周六"
    7 -> "周日"
    else -> "未知"
}

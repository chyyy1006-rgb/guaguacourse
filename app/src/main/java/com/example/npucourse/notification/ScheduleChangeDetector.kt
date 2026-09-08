package com.example.npucourse.notification

import com.example.npucourse.data.CourseEntity
import com.example.npucourse.model.DemoCourse
import java.util.Locale

internal data class ScheduleCourseSnapshot(
    val name: String,
    val teacher: String,
    val room: String,
    val day: Int,
    val startSection: Int,
    val endSection: Int,
    val startWeek: Int,
    val endWeek: Int,
    val weekMode: String,
    val customWeeks: String
) {
    val identity: String
        get() = "${name.normalized()}\u0000${teacher.normalized()}"

    private fun String.normalized(): String = trim().lowercase(Locale.ROOT)
}

internal data class ScheduleChangeSummary(
    val added: Int,
    val removed: Int,
    val adjusted: Int
) {
    val hasChanges: Boolean get() = added > 0 || removed > 0 || adjusted > 0

    fun description(): String {
        val parts = buildList {
            if (added > 0) add("新增 $added 门")
            if (removed > 0) add("取消 $removed 门")
            if (adjusted > 0) add("调整 $adjusted 门")
        }
        return "检测到${parts.joinToString("、")}，已自动同步最新课程表"
    }
}

internal fun scheduleSnapshotOf(course: CourseEntity): ScheduleCourseSnapshot =
    ScheduleCourseSnapshot(
        name = course.name,
        teacher = course.teacher,
        room = course.room,
        day = course.day,
        startSection = course.startSection,
        endSection = course.endSection,
        startWeek = course.startWeek,
        endWeek = course.endWeek,
        weekMode = course.weekMode,
        customWeeks = course.customWeeks
    )

internal fun scheduleSnapshotOf(course: DemoCourse): ScheduleCourseSnapshot =
    ScheduleCourseSnapshot(
        name = course.name,
        teacher = course.teacher,
        room = course.room,
        day = course.day,
        startSection = course.startSection,
        endSection = course.endSection,
        startWeek = course.startWeek,
        endWeek = course.endWeek,
        weekMode = course.weekMode.name,
        customWeeks = course.customWeeks
    )

/**
 * 课程名和教师相同的条目视作同一门课；教室、时间、节次或周次变化计为“调整”。
 * 颜色、备注和提醒开关属于本地个性化设置，不应触发教务课表变化通知。
 */
internal fun detectScheduleChanges(
    previous: List<ScheduleCourseSnapshot>,
    refreshed: List<ScheduleCourseSnapshot>
): ScheduleChangeSummary {
    val previousGroups = previous.groupBy { it.identity }
    val refreshedGroups = refreshed.groupBy { it.identity }
    var added = 0
    var removed = 0
    var adjusted = 0

    (previousGroups.keys + refreshedGroups.keys).forEach { identity ->
        val before = previousGroups[identity].orEmpty()
        val after = refreshedGroups[identity].orEmpty()
        when {
            before.isEmpty() -> added++
            after.isEmpty() -> removed++
            before.toSet() != after.toSet() -> adjusted++
        }
    }

    return ScheduleChangeSummary(added = added, removed = removed, adjusted = adjusted)
}

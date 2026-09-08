package com.example.npucourse

import com.example.npucourse.notification.ScheduleCourseSnapshot
import com.example.npucourse.notification.detectScheduleChanges
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleChangeDetectorTest {
    @Test
    fun identicalScheduleHasNoChanges() {
        val schedule = listOf(course("高等数学"), course("大学英语", day = 2))

        val changes = detectScheduleChanges(schedule, schedule.reversed())

        assertFalse(changes.hasChanges)
    }

    @Test
    fun reportsAddedRemovedAndAdjustedCourses() {
        val previous = listOf(
            course("高等数学", room = "A101"),
            course("大学英语", day = 2),
            course("大学物理", day = 3)
        )
        val refreshed = listOf(
            course("高等数学", room = "A102"),
            course("大学物理", day = 3),
            course("程序设计", day = 4)
        )

        val changes = detectScheduleChanges(previous, refreshed)

        assertTrue(changes.hasChanges)
        assertEquals(1, changes.added)
        assertEquals(1, changes.removed)
        assertEquals(1, changes.adjusted)
        assertEquals("检测到新增 1 门、取消 1 门、调整 1 门，已自动同步最新课程表", changes.description())
    }

    private fun course(
        name: String,
        teacher: String = "张老师",
        room: String = "A101",
        day: Int = 1
    ) = ScheduleCourseSnapshot(
        name = name,
        teacher = teacher,
        room = room,
        day = day,
        startSection = 1,
        endSection = 2,
        startWeek = 1,
        endWeek = 16,
        weekMode = "EVERY",
        customWeeks = ""
    )
}

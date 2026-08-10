package com.example.npucourse

import androidx.compose.ui.graphics.Color
import com.example.npucourse.model.DemoCourse
import com.example.npucourse.util.CampusType
import com.example.npucourse.util.buildWeeklyScheduleInsights
import com.example.npucourse.util.minutesToClockText
import com.example.npucourse.util.scheduleTimeToMinutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleInsightsTest {

    private fun course(
        id: Long,
        name: String,
        day: Int,
        startSection: Int,
        endSection: Int
    ) = DemoCourse(
        id = id,
        name = name,
        room = "A101",
        teacher = "教师",
        day = day,
        startSection = startSection,
        endSection = endSection,
        startWeek = 1,
        endWeek = 16,
        color = Color.Black
    )

    @Test
    fun overlappingCoursesAreReportedAsConflict() {
        val insights = buildWeeklyScheduleInsights(
            courses = listOf(
                course(1, "课程A", 1, 1, 2),
                course(2, "课程B", 1, 2, 3)
            ),
            week = 5,
            campus = CampusType.CHANGAN
        )

        assertEquals(1, insights.conflicts.size)
        assertEquals(1, insights.conflicts.first().day)
    }

    @Test
    fun freeWindowsAreGeneratedForEachWeekday() {
        val insights = buildWeeklyScheduleInsights(
            courses = listOf(course(1, "课程A", 1, 1, 2)),
            week = 5,
            campus = CampusType.CHANGAN,
            dayStartMinutes = 8 * 60,
            dayEndMinutes = 12 * 60,
            minimumFreeMinutes = 30
        )

        assertTrue(insights.freeWindows.any { it.day == 1 })
        assertTrue(insights.freeWindows.any { it.day == 2 })
    }

    @Test
    fun clockConversionsAreStable() {
        assertEquals(8 * 60 + 30, scheduleTimeToMinutes("08:30"))
        assertEquals("08:30", minutesToClockText(8 * 60 + 30))
    }
}

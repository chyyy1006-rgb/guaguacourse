package com.example.npucourse

import androidx.compose.ui.graphics.Color
import com.example.npucourse.model.DemoCourse
import com.example.npucourse.model.WeekMode
import com.example.npucourse.model.isActiveInWeek
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseWeekLogicTest {

    private fun course(
        mode: WeekMode,
        start: Int = 1,
        end: Int = 16,
        custom: String = ""
    ) = DemoCourse(
        id = 1,
        name = "测试课程",
        room = "A101",
        teacher = "教师",
        day = 1,
        startSection = 1,
        endSection = 2,
        startWeek = start,
        endWeek = end,
        color = Color.Black,
        weekMode = mode,
        customWeeks = custom
    )

    @Test
    fun oddAndEvenWeeksAreSeparated() {
        val odd = course(WeekMode.ODD)
        val even = course(WeekMode.EVEN)

        assertTrue(odd.isActiveInWeek(3))
        assertFalse(odd.isActiveInWeek(4))
        assertTrue(even.isActiveInWeek(4))
        assertFalse(even.isActiveInWeek(3))
    }

    @Test
    fun customWeeksSupportNonContinuousSchedule() {
        val custom = course(
            mode = WeekMode.CUSTOM,
            custom = "1,3,5,8,10"
        )

        assertTrue(custom.isActiveInWeek(8))
        assertFalse(custom.isActiveInWeek(9))
    }

    @Test
    fun invalidTeachingWeeksAreAlwaysInactive() {
        val course = course(WeekMode.EVERY)
        assertFalse(course.isActiveInWeek(0))
        assertFalse(course.isActiveInWeek(21))
    }
}

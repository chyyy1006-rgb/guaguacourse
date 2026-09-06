package com.example.npucourse

import androidx.compose.ui.graphics.Color
import com.example.npucourse.model.DemoCourse
import com.example.npucourse.ui.timetable.resolveJellyCourseColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CourseColorResolverTest {
    @Test
    fun sameCourseAcrossDaysKeepsItsColor() {
        val first = course(1, "数字信号处理", 1, 1, 2)
        val second = course(2, "数字信号处理", 4, 5, 6)
        val result = resolveJellyCourseColors(listOf(first, second), darkTheme = false)
        assertEquals(result.getValue(first.id).background, result.getValue(second.id).background)
    }

    @Test
    fun visuallyAdjacentDifferentCoursesDoNotShareMainColor() {
        val upper = course(11, "空气动力学", 2, 1, 2)
        val lower = course(12, "操作系统", 2, 3, 4)
        val right = course(13, "大学物理", 3, 1, 2)
        val result = resolveJellyCourseColors(listOf(upper, lower, right), darkTheme = false)
        assertNotEquals(result.getValue(upper.id).background, result.getValue(lower.id).background)
        assertNotEquals(result.getValue(upper.id).background, result.getValue(right.id).background)
    }

    @Test
    fun inputOrderDoesNotChangeAssignments() {
        val courses = listOf(
            course(21, "高等数学", 1, 1, 2),
            course(22, "大学英语", 1, 3, 4),
            course(23, "材料力学", 2, 1, 2)
        )
        val forward = resolveJellyCourseColors(courses, darkTheme = true)
        val reversed = resolveJellyCourseColors(courses.reversed(), darkTheme = true)
        courses.forEach { assertEquals(forward.getValue(it.id), reversed.getValue(it.id)) }
    }

    private fun course(id: Long, name: String, day: Int, start: Int, end: Int) = DemoCourse(
        id = id,
        name = name,
        room = "A101",
        teacher = "教师",
        day = day,
        startSection = start,
        endSection = end,
        startWeek = 1,
        endWeek = 16,
        color = Color.Blue
    )
}

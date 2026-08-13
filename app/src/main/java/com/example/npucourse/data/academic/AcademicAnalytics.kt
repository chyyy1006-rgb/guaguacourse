package com.example.npucourse.data.academic

import com.example.npucourse.importer.NwpuGradeQueryResult
import com.example.npucourse.importer.NwpuGradeRecord

object AcademicAnalytics {
    data class SemesterStat(
        val semesterId: Long,
        val semesterName: String,
        val gpa: Double?,
        val credits: Double,
        val passedCredits: Double,
        val weightedAverage: Double?,
        val courseCount: Int
    )

    data class Overview(
        val totalCredits: Double,
        val passedCredits: Double,
        val failedCourseCount: Int,
        val stats: List<SemesterStat>
    )

    fun overview(result: NwpuGradeQueryResult): Overview {
        val stats = result.semesters.map { semester ->
            val rows = result.grades.filter { it.semesterId == semester.id }
            SemesterStat(
                semesterId = semester.id,
                semesterName = semester.name,
                gpa = weightedGpa(rows),
                credits = rows.mapNotNull { it.credits }.sum(),
                passedCredits = rows.filter(::isPassed).mapNotNull { it.credits }.sum(),
                weightedAverage = weightedAverage(rows),
                courseCount = rows.size
            )
        }.filter { it.courseCount > 0 }.sortedBy { it.semesterId }

        return Overview(
            totalCredits = result.grades.mapNotNull { it.credits }.sum(),
            passedCredits = result.grades.filter(::isPassed).mapNotNull { it.credits }.sum(),
            failedCourseCount = result.grades.count { !isPassed(it) && isPublished(it) },
            stats = stats
        )
    }

    private fun weightedGpa(rows: List<NwpuGradeRecord>): Double? {
        val valid = rows.filter { it.gradePoint != null && it.credits != null && it.credits > 0 }
        val credits = valid.sumOf { it.credits ?: 0.0 }
        return if (credits <= 0) null else valid.sumOf { (it.gradePoint ?: 0.0) * (it.credits ?: 0.0) } / credits
    }

    private fun weightedAverage(rows: List<NwpuGradeRecord>): Double? {
        val values = rows.mapNotNull { row ->
            val score = row.grade.toDoubleOrNull() ?: gradeNameScore(row.grade)
            val credit = row.credits
            if (score == null || credit == null || credit <= 0) null else score to credit
        }
        val credits = values.sumOf { it.second }
        return if (credits <= 0) null else values.sumOf { it.first * it.second } / credits
    }

    private fun isPublished(row: NwpuGradeRecord): Boolean = row.grade.isNotBlank() && row.grade != "-"

    private fun isPassed(row: NwpuGradeRecord): Boolean {
        if (!isPublished(row)) return false
        row.grade.toDoubleOrNull()?.let { return it >= 60.0 }
        return when (row.grade.trim()) {
            "不及格", "不通过", "未通过", "F" -> false
            else -> true
        }
    }

    private fun gradeNameScore(text: String): Double? = when (text.trim()) {
        "优秀" -> 93.0
        "良好" -> 80.0
        "中等" -> 70.0
        "及格" -> 60.0
        "不及格" -> 0.0
        else -> null
    }
}

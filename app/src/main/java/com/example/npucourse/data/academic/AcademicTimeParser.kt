package com.example.npucourse.data.academic

import java.util.Calendar
import java.util.TimeZone

object AcademicTimeParser {
    private val dateTimeRegex = Regex(
        """(20\d{2})\s*[-/.年]\s*(\d{1,2})\s*[-/.月]\s*(\d{1,2})\s*(?:日)?[^0-9]{0,18}(\d{1,2})\s*[:：]\s*(\d{2})"""
    )

    fun parseStartMillis(text: String): Long? {
        val match = dateTimeRegex.find(text) ?: return null
        val year = match.groupValues[1].toIntOrNull() ?: return null
        val month = match.groupValues[2].toIntOrNull() ?: return null
        val day = match.groupValues[3].toIntOrNull() ?: return null
        val hour = match.groupValues[4].toIntOrNull() ?: return null
        val minute = match.groupValues[5].toIntOrNull() ?: return null
        if (month !in 1..12 || day !in 1..31 || hour !in 0..23 || minute !in 0..59) return null

        return Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply {
            clear()
            set(year, month - 1, day, hour, minute, 0)
        }.timeInMillis
    }

    fun countdownText(text: String, now: Long = System.currentTimeMillis()): String? {
        val start = parseStartMillis(text) ?: return null
        val diff = start - now
        if (diff <= 0L) return "今天/进行中"
        val days = diff / 86_400_000L
        val hours = (diff % 86_400_000L) / 3_600_000L
        return when {
            days >= 1 -> "还有 ${days} 天"
            hours >= 1 -> "还有 ${hours} 小时"
            else -> "即将开始"
        }
    }
}

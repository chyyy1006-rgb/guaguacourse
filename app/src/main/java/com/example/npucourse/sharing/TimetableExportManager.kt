package com.example.npucourse.sharing

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import com.example.npucourse.model.DemoCourse
import com.example.npucourse.model.activeWeeks
import com.example.npucourse.model.isActiveInWeek
import com.example.npucourse.model.weekDisplayText
import com.example.npucourse.util.campusDisplayName
import com.example.npucourse.util.getMaxSection
import com.example.npucourse.util.getScheduleForCampus
import com.example.npucourse.util.semesterWeekDateMillis
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TimetableExportManager {

    fun renderWeekBitmap(
        semesterName: String,
        semesterStartMillis: Long,
        campus: String,
        week: Int,
        courses: List<DemoCourse>
    ): Bitmap {
        val width = 1400
        val height = 1900
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(AndroidColor.rgb(249, 249, 252))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(27, 27, 32)
            textSize = 58f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(108, 109, 119)
            textSize = 26f
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(57, 58, 67)
            textSize = 24f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(139, 140, 151)
            textSize = 20f
            textAlign = Paint.Align.CENTER
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(112, 113, 124)
            textSize = 20f
            textAlign = Paint.Align.CENTER
        }
        val coursePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(34, 35, 41)
            textSize = 21f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val roomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(80, 81, 90)
            textSize = 16f
        }

        canvas.drawText("第${week}周课表", 70f, 90f, titlePaint)
        canvas.drawText(
            "$semesterName · ${campusDisplayName(campus)}",
            70f,
            135f,
            subtitlePaint
        )

        val leftWidth = 105f
        val top = 210f
        val headerHeight = 90f
        val dayWidth = (width - leftWidth - 30f) / 7f
        val maxSection = getMaxSection(campus)
        val rowHeight = (height - top - headerHeight - 60f) / maxSection.toFloat()

        val weekdays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        val dateFormat = SimpleDateFormat("M/d", Locale.CHINA)

        weekdays.forEachIndexed { index, label ->
            val centerX = leftWidth + dayWidth * index + dayWidth / 2f
            canvas.drawText(label, centerX, top + 32f, headerPaint)
            val date = semesterWeekDateMillis(
                semesterStartMillis = semesterStartMillis,
                week = week,
                day = index + 1
            )
            canvas.drawText(
                dateFormat.format(Date(date)),
                centerX,
                top + 66f,
                datePaint
            )
        }

        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(230, 230, 236)
            strokeWidth = 1.5f
        }

        for (section in 1..maxSection) {
            val y = top + headerHeight + (section - 1) * rowHeight
            canvas.drawText(
                section.toString(),
                leftWidth / 2f,
                y + rowHeight / 2f + 7f,
                sectionPaint
            )
            canvas.drawLine(leftWidth, y, width - 30f, y, gridPaint)
        }
        canvas.drawLine(
            leftWidth,
            top + headerHeight + maxSection * rowHeight,
            width - 30f,
            top + headerHeight + maxSection * rowHeight,
            gridPaint
        )

        for (day in 0..7) {
            val x = leftWidth + dayWidth * day
            canvas.drawLine(
                x,
                top + headerHeight,
                x,
                top + headerHeight + maxSection * rowHeight,
                gridPaint
            )
        }

        courses
            .filter { it.isActiveInWeek(week) }
            .sortedWith(compareBy<DemoCourse> { it.day }.thenBy { it.startSection })
            .forEach { course ->
                val left = leftWidth + (course.day - 1) * dayWidth + 5f
                val right = leftWidth + course.day * dayWidth - 5f
                val courseTop = top + headerHeight + (course.startSection - 1) * rowHeight + 4f
                val courseBottom = top + headerHeight + course.endSection * rowHeight - 4f

                paint.color = blendWithWhite(course.color.toArgb(), 0.72f)
                canvas.drawRoundRect(
                    RectF(left, courseTop, right, courseBottom),
                    16f,
                    16f,
                    paint
                )

                val textLeft = left + 12f
                var textY = courseTop + 29f
                val maxTextWidth = right - left - 24f

                wrapText(course.name, coursePaint, maxTextWidth, maxLines = 4)
                    .forEach { line ->
                        if (textY < courseBottom - 34f) {
                            canvas.drawText(line, textLeft, textY, coursePaint)
                            textY += 27f
                        }
                    }

                if (textY < courseBottom - 12f && course.room.isNotBlank()) {
                    wrapText(course.room, roomPaint, maxTextWidth, maxLines = 2)
                        .forEach { line ->
                            if (textY < courseBottom - 8f) {
                                canvas.drawText(line, textLeft, textY, roomPaint)
                                textY += 21f
                            }
                        }
                }
            }

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(150, 151, 160)
            textSize = 18f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("由瓜瓜课程表生成", width - 42f, height - 24f, footerPaint)

        return bitmap
    }

    fun buildIcs(
        semesterName: String,
        semesterStartMillis: Long,
        campus: String,
        courses: List<DemoCourse>
    ): String {
        val schedule = getScheduleForCampus(campus)
        val timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        val dateTimeFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US).apply {
            this.timeZone = timeZone
        }
        val stampFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            this.timeZone = TimeZone.getTimeZone("UTC")
        }

        val builder = StringBuilder()
        builder.append("BEGIN:VCALENDAR\r\n")
        builder.append("VERSION:2.0\r\n")
        builder.append("PRODID:-//GuaguaCourse//Timetable//CN\r\n")
        builder.append("CALSCALE:GREGORIAN\r\n")
        builder.append("METHOD:PUBLISH\r\n")
        builder.append("X-WR-CALNAME:").append(icsEscape(semesterName)).append("\r\n")

        courses.forEach { course ->
            val startTime = schedule.firstOrNull { it.section == course.startSection }
                ?: return@forEach
            val endTime = schedule.firstOrNull { it.section == course.endSection }
                ?: return@forEach

            course.activeWeeks().forEach { week ->
                val dateMillis = semesterWeekDateMillis(
                    semesterStartMillis = semesterStartMillis,
                    week = week,
                    day = course.day
                )

                val startCalendar = calendarWithTime(dateMillis, startTime.startTime)
                val endCalendar = calendarWithTime(dateMillis, endTime.endTime)

                builder.append("BEGIN:VEVENT\r\n")
                builder.append("UID:")
                    .append("guaguacourse-")
                    .append(course.id)
                    .append('-')
                    .append(week)
                    .append("@local\r\n")
                builder.append("DTSTAMP:")
                    .append(stampFormat.format(Date()))
                    .append("\r\n")
                builder.append("DTSTART;TZID=Asia/Shanghai:")
                    .append(dateTimeFormat.format(startCalendar.time))
                    .append("\r\n")
                builder.append("DTEND;TZID=Asia/Shanghai:")
                    .append(dateTimeFormat.format(endCalendar.time))
                    .append("\r\n")
                builder.append("SUMMARY:").append(icsEscape(course.name)).append("\r\n")
                builder.append("LOCATION:").append(icsEscape(course.room)).append("\r\n")
                builder.append("DESCRIPTION:")
                    .append(icsEscape("教师：${course.teacher}；第${course.startSection}-${course.endSection}节；${course.weekDisplayText()}"))
                    .append("\r\n")
                builder.append("END:VEVENT\r\n")
            }
        }

        builder.append("END:VCALENDAR\r\n")
        return builder.toString()
    }

    fun shareBitmap(context: Context, bitmap: Bitmap, fileName: String) {
        val file = cacheFile(context, fileName)
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "分享课表图片")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun shareIcs(context: Context, content: String, fileName: String) {
        val file = cacheFile(context, fileName)
        file.writeText(content, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/calendar"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "分享课程日历")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun cacheFile(context: Context, fileName: String): File {
        val dir = File(context.cacheDir, "shared")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, fileName)
    }

    private fun calendarWithTime(dateMillis: Long, time: String): Calendar {
        val parts = time.split(':')
        return Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply {
            this.timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, parts.getOrNull(0)?.toIntOrNull() ?: 0)
            set(Calendar.MINUTE, parts.getOrNull(1)?.toIntOrNull() ?: 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun icsEscape(text: String): String =
        text.replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\n", "\\n")

    private fun blendWithWhite(color: Int, amount: Float): Int {
        val r = AndroidColor.red(color)
        val g = AndroidColor.green(color)
        val b = AndroidColor.blue(color)
        return AndroidColor.rgb(
            (r + (255 - r) * amount).toInt(),
            (g + (255 - g) * amount).toInt(),
            (b + (255 - b) * amount).toInt()
        )
    }

    private fun wrapText(
        text: String,
        paint: Paint,
        maxWidth: Float,
        maxLines: Int
    ): List<String> {
        if (text.isBlank()) return emptyList()
        val result = mutableListOf<String>()
        var current = ""
        text.forEach { char ->
            val candidate = current + char
            if (paint.measureText(candidate) <= maxWidth || current.isEmpty()) {
                current = candidate
            } else {
                result.add(current)
                current = char.toString()
                if (result.size >= maxLines) return@forEach
            }
        }
        if (current.isNotBlank() && result.size < maxLines) result.add(current)
        return result.take(maxLines)
    }
}

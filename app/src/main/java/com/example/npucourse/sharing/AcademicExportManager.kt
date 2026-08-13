package com.example.npucourse.sharing

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.npucourse.data.academic.AcademicAnalytics
import com.example.npucourse.data.academic.AcademicTimeParser
import com.example.npucourse.importer.NwpuExamRecord
import com.example.npucourse.importer.NwpuGradeQueryResult
import com.example.npucourse.importer.NwpuGradeRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AcademicExportManager {
    private const val WIDTH = 1080
    private const val SIDE = 64f

    fun renderGradesBitmap(
        result: NwpuGradeQueryResult,
        semesterId: Long,
        maskSensitive: Boolean
    ): Bitmap {
        val semester = result.semesters.firstOrNull { it.id == semesterId }
        val rows = result.grades.filter { semesterId == 0L || it.semesterId == semesterId }
            .sortedBy { it.courseName }
        val height = (430 + rows.size * 116 + 110).coerceAtLeast(760)
        val bitmap = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(248, 249, 253))

        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(28, 30, 38); textSize = 54f; typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(104, 108, 120); textSize = 25f }
        val metric = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(35, 39, 52); textSize = 35f; typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(118, 121, 132); textSize = 21f }
        val coursePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(31, 33, 40); textSize = 28f; typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val detail = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(100, 103, 113); textSize = 20f }
        val score = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(56, 87, 210); textSize = 36f; typeface = android.graphics.Typeface.DEFAULT_BOLD; textAlign = Paint.Align.RIGHT
        }
        val card = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

        canvas.drawText("成绩单", SIDE, 86f, title)
        canvas.drawText(semester?.name ?: "全部学期", SIDE, 128f, sub)
        val idText = if (maskSensitive) maskStudentId(result.studentId) else result.studentId
        if (idText.isNotBlank()) canvas.drawText("学号 $idText", WIDTH - SIDE, 128f, sub.apply { textAlign = Paint.Align.RIGHT })
        sub.textAlign = Paint.Align.LEFT

        val credits = rows.mapNotNull { it.credits }.sum()
        val semesterGpa = weightedGpa(rows)
        val average = weightedAverageScore(rows)
        drawMetric(canvas, "GPA", semesterGpa?.let(::formatNumber) ?: result.gpa?.let(::formatNumber) ?: "-", SIDE, 205f, metric, label)
        drawMetric(canvas, "学分", formatNumber(credits), 360f, 205f, metric, label)
        drawMetric(canvas, "加权均分", average?.let(::formatNumber) ?: "-", 640f, 205f, metric, label)
        if (!maskSensitive && result.rank != null) drawMetric(canvas, "GPA 排名", result.rank.toString(), 865f, 205f, metric, label)

        var y = 310f
        rows.forEach { grade ->
            canvas.drawRoundRect(RectF(SIDE, y, WIDTH - SIDE, y + 98f), 24f, 24f, card)
            canvas.drawText(ellipsize(grade.courseName, coursePaint, 620f), SIDE + 24f, y + 40f, coursePaint)
            val meta = buildString {
                grade.credits?.let { append("${formatNumber(it)} 学分") }
                grade.gradePoint?.let { if (isNotEmpty()) append(" · "); append("绩点 ${formatNumber(it)}") }
                if (!maskSensitive && !grade.classRank.isNullOrBlank()) {
                    if (isNotEmpty()) append(" · ")
                    append("教学班 ${grade.classRank}")
                }
            }
            canvas.drawText(meta, SIDE + 24f, y + 73f, detail)
            canvas.drawText(grade.grade.ifBlank { "-" }, WIDTH - SIDE - 24f, y + 59f, score)
            y += 116f
        }

        drawFooter(canvas, y + 35f)
        return bitmap
    }

    fun renderAnalyticsBitmap(
        result: NwpuGradeQueryResult,
        maskSensitive: Boolean
    ): Bitmap {
        val overview = AcademicAnalytics.overview(result)
        val stats = overview.stats
        val points = stats.mapNotNull { stat -> stat.gpa?.let { stat to it } }
        val height = (920 + stats.size * 118).coerceAtLeast(1040)
        val bitmap = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(248, 249, 253))

        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(28, 30, 38); textSize = 54f; typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(104, 108, 120); textSize = 24f }
        val section = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(31, 33, 40); textSize = 29f; typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val metric = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(35, 39, 52); textSize = 34f; typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(118, 121, 132); textSize = 20f }
        val meta = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(91, 95, 108); textSize = 20f }
        val card = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(224, 226, 235); strokeWidth = 2f }
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(76, 83, 216); strokeWidth = 7f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(76, 83, 216); style = Paint.Style.FILL }
        val axis = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(128, 132, 145); textSize = 18f }
        val pointValue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(76, 83, 216); textSize = 18f; textAlign = Paint.Align.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        canvas.drawText("学业分析", SIDE, 86f, title)
        canvas.drawText("${SimpleDateFormat("yyyy/M/d HH:mm", Locale.CHINA).format(Date())} 更新", SIDE, 130f, sub)
        val idText = if (maskSensitive) maskStudentId(result.studentId) else result.studentId
        if (idText.isNotBlank()) {
            val idPaint = Paint(sub).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText("学号 $idText", WIDTH - SIDE, 130f, idPaint)
        }

        val summaryTop = 178f
        val summaryBottom = 330f
        canvas.drawRoundRect(RectF(SIDE, summaryTop, WIDTH - SIDE, summaryBottom), 26f, 26f, card)
        drawMetric(canvas, "累计 GPA", result.gpa?.let(::formatNumber) ?: "-", SIDE + 28f, summaryTop + 62f, metric, label)
        drawMetric(canvas, "累计学分", formatNumber(overview.totalCredits), SIDE + 285f, summaryTop + 62f, metric, label)
        drawMetric(canvas, "通过学分", formatNumber(overview.passedCredits), SIDE + 545f, summaryTop + 62f, metric, label)
        drawMetric(canvas, "未通过课程", overview.failedCourseCount.toString(), SIDE + 790f, summaryTop + 62f, metric, label)

        val chartTop = 365f
        val chartBottom = 740f
        canvas.drawRoundRect(RectF(SIDE, chartTop, WIDTH - SIDE, chartBottom), 26f, 26f, card)
        canvas.drawText("GPA 趋势", SIDE + 28f, chartTop + 48f, section)

        if (points.size < 2) {
            canvas.drawText("至少需要两个学期的绩点数据才能绘制趋势。", SIDE + 28f, chartTop + 104f, meta)
        } else {
            val plotLeft = SIDE + 86f
            val plotRight = WIDTH - SIDE - 36f
            val plotTop = chartTop + 84f
            val plotBottom = chartBottom - 74f
            val values = points.map { it.second }
            val rawMin = values.minOrNull() ?: 0.0
            val rawMax = values.maxOrNull() ?: 4.0
            val minV = (kotlin.math.floor(rawMin * 2.0) / 2.0 - 0.25).coerceAtLeast(0.0)
            val maxV = (kotlin.math.ceil(rawMax * 2.0) / 2.0 + 0.25).coerceAtLeast(minV + 0.75)

            repeat(4) { index ->
                val ratio = index / 3f
                val y = plotBottom - (plotBottom - plotTop) * ratio
                canvas.drawLine(plotLeft, y, plotRight, y, grid)
                val value = minV + (maxV - minV) * ratio
                val axisPaint = Paint(axis).apply { textAlign = Paint.Align.RIGHT }
                canvas.drawText(formatNumber(value), plotLeft - 14f, y + 6f, axisPaint)
            }

            val path = android.graphics.Path()
            val coords = points.mapIndexed { index, pair ->
                val x = if (points.size == 1) plotLeft else plotLeft + (plotRight - plotLeft) * index / (points.size - 1f)
                val ratio = ((pair.second - minV) / (maxV - minV)).toFloat().coerceIn(0f, 1f)
                val y = plotBottom - (plotBottom - plotTop) * ratio
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                Triple(x, y, pair)
            }
            canvas.drawPath(path, line)

            val labelStep = kotlin.math.ceil(points.size / 5.0).toInt().coerceAtLeast(1)
            coords.forEachIndexed { index, (x, y, pair) ->
                canvas.drawCircle(x, y, 10f, dot)
                canvas.drawText(formatNumber(pair.second), x, y - 18f, pointValue)
                if (index == 0 || index == coords.lastIndex || index % labelStep == 0) {
                    val xLabel = Paint(axis).apply { textAlign = Paint.Align.CENTER }
                    canvas.drawText(shortSemesterName(pair.first.semesterName), x, plotBottom + 37f, xLabel)
                }
            }
        }

        var y = chartBottom + 48f
        canvas.drawText("学期概览", SIDE, y, section)
        y += 28f
        if (stats.isEmpty()) {
            canvas.drawText("暂无可分析的学期数据", SIDE, y + 54f, meta)
            y += 118f
        } else {
            stats.reversed().forEach { stat ->
                val top = y
                canvas.drawRoundRect(RectF(SIDE, top, WIDTH - SIDE, top + 96f), 22f, 22f, card)
                canvas.drawText(ellipsize(stat.semesterName, section, 380f), SIDE + 24f, top + 38f, section)
                canvas.drawText("${stat.courseCount} 门课 · ${formatNumber(stat.credits)} 学分", SIDE + 24f, top + 72f, meta)

                val right = Paint(metric).apply { textAlign = Paint.Align.RIGHT; textSize = 30f }
                canvas.drawText("GPA ${stat.gpa?.let(::formatNumber) ?: "-"}", WIDTH - SIDE - 24f, top + 38f, right)
                val rightMeta = Paint(meta).apply { textAlign = Paint.Align.RIGHT }
                canvas.drawText("均分 ${stat.weightedAverage?.let(::formatNumber) ?: "-"} · 通过 ${formatNumber(stat.passedCredits)} 学分", WIDTH - SIDE - 24f, top + 72f, rightMeta)
                y += 118f
            }
        }

        drawFooter(canvas, y + 36f)
        return bitmap
    }

    fun renderExamsBitmap(
        exams: List<NwpuExamRecord>,
        includeFinished: Boolean
    ): Bitmap {
        val rows = exams.filter { includeFinished || !it.finished }
            .sortedWith(compareBy<NwpuExamRecord> { AcademicTimeParser.parseStartMillis(it.timeText) == null }
                .thenBy { AcademicTimeParser.parseStartMillis(it.timeText) ?: Long.MAX_VALUE })
        val height = (300 + rows.size * 154 + 110).coerceAtLeast(720)
        val bitmap = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(248, 249, 253))

        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(28, 30, 38); textSize = 54f; typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(104, 108, 120); textSize = 24f }
        val course = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(31, 33, 40); textSize = 29f; typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val meta = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(91, 95, 108); textSize = 21f }
        val countdown = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(56, 87, 210); textSize = 21f; typeface = android.graphics.Typeface.DEFAULT_BOLD; textAlign = Paint.Align.RIGHT
        }
        val card = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

        canvas.drawText("考试安排", SIDE, 86f, title)
        canvas.drawText("${rows.size} 场 · ${SimpleDateFormat("yyyy/M/d HH:mm", Locale.CHINA).format(Date())} 更新", SIDE, 130f, sub)
        var y = 190f
        if (rows.isEmpty()) {
            canvas.drawText("暂无可导出的考试安排", SIDE, y + 60f, sub)
            y += 120f
        }
        rows.forEach { exam ->
            canvas.drawRoundRect(RectF(SIDE, y, WIDTH - SIDE, y + 136f), 24f, 24f, card)
            canvas.drawText(ellipsize(exam.courseName.ifBlank { "未命名考试" }, course, 700f), SIDE + 24f, y + 38f, course)
            canvas.drawText(exam.timeText.ifBlank { "时间待定" }, SIDE + 24f, y + 73f, meta)
            val location = exam.location.ifBlank { "地点待定" }
            canvas.drawText(ellipsize(location, meta, 750f), SIDE + 24f, y + 106f, meta)
            val rightText = if (exam.finished) "已结束" else AcademicTimeParser.countdownText(exam.timeText).orEmpty()
            if (rightText.isNotBlank()) canvas.drawText(rightText, WIDTH - SIDE - 24f, y + 39f, countdown)
            y += 154f
        }
        drawFooter(canvas, y + 30f)
        return bitmap
    }

    fun shareBitmap(context: Context, bitmap: Bitmap, fileName: String, chooserTitle: String) {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, sanitizeFileName(fileName))
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileName: String): Result<Uri> =
        runCatching {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, sanitizeFileName(fileName))
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GuaguaCourse")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("无法创建相册文件")
            try {
                resolver.openOutputStream(uri)?.use { output ->
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "图片写入失败" }
                } ?: error("无法写入相册")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
                uri
            } catch (error: Throwable) {
                resolver.delete(uri, null, null)
                throw error
            }
        }

    private fun drawMetric(
        canvas: Canvas,
        labelText: String,
        value: String,
        x: Float,
        y: Float,
        metric: Paint,
        label: Paint
    ) {
        canvas.drawText(value, x, y, metric)
        canvas.drawText(labelText, x, y + 31f, label)
    }

    private fun drawFooter(canvas: Canvas, y: Float) {
        val footer = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(145, 148, 159); textSize = 19f; textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("由瓜瓜课程表生成 · 数据来自翱翔教务", WIDTH - SIDE, y, footer)
    }

    private fun weightedGpa(rows: List<NwpuGradeRecord>): Double? {
        val valid = rows.filter { it.gradePoint != null && it.credits != null && it.credits > 0 }
        val credits = valid.sumOf { it.credits ?: 0.0 }
        if (credits <= 0) return null
        return valid.sumOf { (it.gradePoint ?: 0.0) * (it.credits ?: 0.0) } / credits
    }

    private fun weightedAverageScore(rows: List<NwpuGradeRecord>): Double? {
        val values = rows.mapNotNull { grade ->
            val score = grade.grade.toDoubleOrNull() ?: gradeNameScore(grade.grade)
            val credit = grade.credits
            if (score == null || credit == null || credit <= 0) null else score to credit
        }
        val credits = values.sumOf { it.second }
        if (credits <= 0) return null
        return values.sumOf { it.first * it.second } / credits
    }

    private fun gradeNameScore(text: String): Double? = when (text.trim()) {
        "优秀" -> 93.0
        "良好" -> 80.0
        "中等" -> 70.0
        "及格" -> 60.0
        "不及格" -> 0.0
        else -> null
    }

    private fun maskStudentId(value: String): String {
        if (value.length <= 5) return "***"
        return value.take(3) + "*".repeat((value.length - 6).coerceAtLeast(3)) + value.takeLast(3)
    }

    private fun formatNumber(value: Double): String {
        val rounded = kotlin.math.round(value * 100.0) / 100.0
        return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString()
        else "%.2f".format(Locale.US, rounded).trimEnd('0').trimEnd('.')
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var value = text
        while (value.length > 1 && paint.measureText("$value…") > maxWidth) value = value.dropLast(1)
        return "$value…"
    }

    private fun shortSemesterName(value: String): String {
        val clean = value.replace("学年", "").replace("学期", "").trim()
        return when {
            clean.length <= 9 -> clean
            clean.contains("春") || clean.contains("秋") -> clean.takeLast(8)
            else -> clean.take(8)
        }
    }

    private fun sanitizeFileName(value: String): String =
        value.replace(Regex("[^a-zA-Z0-9._\\-一-龥]"), "_").let { if (it.endsWith(".png")) it else "$it.png" }
}

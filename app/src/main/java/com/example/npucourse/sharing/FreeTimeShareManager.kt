package com.example.npucourse.sharing

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.FileProvider
import com.example.npucourse.util.FreeWindow
import com.example.npucourse.util.minutesToClockText
import com.example.npucourse.util.weekdayText
import java.io.File
import java.io.FileOutputStream

/**
 * 本周连续空闲时间的分享工具。
 *
 * 提供纯文字、简洁图片、卡片图片三种格式；图片只写入 cache/shared，
 * 通过现有 FileProvider 分享，不需要额外存储权限。
 */
object FreeTimeShareManager {

    enum class ImageStyle {
        SIMPLE,
        CARD
    }

    fun shareText(
        context: Context,
        semesterName: String,
        week: Int,
        freeWindows: List<FreeWindow>
    ) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                buildShareText(semesterName, week, freeWindows)
            )
        }
        context.startActivity(Intent.createChooser(intent, "分享本周空闲时间"))
    }

    fun shareImage(
        context: Context,
        semesterName: String,
        week: Int,
        freeWindows: List<FreeWindow>,
        style: ImageStyle
    ) {
        val bitmap = renderBitmap(
            semesterName = semesterName,
            week = week,
            freeWindows = freeWindows,
            style = style
        )

        val sharedDir = File(context.cacheDir, "shared").apply { mkdirs() }
        val styleName = if (style == ImageStyle.SIMPLE) "simple" else "card"
        val file = File(sharedDir, "free_time_week_${week}_${styleName}.png")
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        bitmap.recycle()

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(
                Intent.EXTRA_TEXT,
                "$semesterName · 第${week}周连续空闲时间"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享本周空闲时间图片"))
    }

    fun buildShareText(
        semesterName: String,
        week: Int,
        freeWindows: List<FreeWindow>
    ): String {
        val grouped = freeWindows.groupBy { it.day }
        return buildString {
            appendLine("$semesterName · 第${week}周空闲时间")
            appendLine("08:00–22:00 · 连续 1 小时以上")
            appendLine()
            (1..7).forEach { day ->
                val windows = grouped[day].orEmpty()
                append(weekdayText(day))
                append("：")
                if (windows.isEmpty()) {
                    append("无")
                } else {
                    append(
                        windows.joinToString("、") {
                            "${minutesToClockText(it.startMinutes)}–${minutesToClockText(it.endMinutes)}"
                        }
                    )
                }
                appendLine()
            }
            appendLine()
            append("来自瓜瓜课程表")
        }
    }

    private fun renderBitmap(
        semesterName: String,
        week: Int,
        freeWindows: List<FreeWindow>,
        style: ImageStyle
    ): Bitmap {
        val width = 1200
        val height = 1640
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val background = if (style == ImageStyle.CARD) {
            Color.rgb(243, 244, 252)
        } else {
            Color.WHITE
        }
        val primary = if (style == ImageStyle.CARD) {
            Color.rgb(79, 76, 180)
        } else {
            Color.rgb(35, 36, 43)
        }
        val textPrimary = Color.rgb(34, 35, 41)
        val textSecondary = Color.rgb(102, 104, 116)
        val cardColor = Color.WHITE

        canvas.drawColor(background)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primary
            textSize = 64f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSecondary
            textSize = 32f
        }
        val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrimary
            textSize = 36f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSecondary
            textSize = 31f
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSecondary
            textSize = 27f
        }
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cardColor
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primary
        }

        var y = 118f
        canvas.drawText("本周连续空闲时间", 78f, y, titlePaint)
        y += 62f
        canvas.drawText("$semesterName · 第${week}周", 80f, y, subtitlePaint)
        y += 47f
        canvas.drawText("08:00–22:00 · 连续 1 小时以上", 80f, y, subtitlePaint)
        y += 58f

        val grouped = freeWindows.groupBy { it.day }
        (1..7).forEach { day ->
            val windows = grouped[day].orEmpty()
            val timeText = if (windows.isEmpty()) {
                "无连续空档"
            } else {
                windows.joinToString("  ·  ") {
                    "${minutesToClockText(it.startMinutes)}–${minutesToClockText(it.endMinutes)}"
                }
            }

            if (style == ImageStyle.CARD) {
                val cardTop = y
                val cardBottom = y + 142f
                canvas.drawRoundRect(
                    RectF(68f, cardTop, width - 68f, cardBottom),
                    30f,
                    30f,
                    cardPaint
                )
                canvas.drawRoundRect(
                    RectF(68f, cardTop, 80f, cardBottom),
                    8f,
                    8f,
                    accentPaint
                )
                canvas.drawText(weekdayText(day), 112f, y + 52f, dayPaint)
                drawWrappedText(
                    canvas = canvas,
                    text = timeText,
                    x = 112f,
                    baselineY = y + 103f,
                    maxWidth = width - 180f,
                    paint = timePaint,
                    lineHeight = 38f,
                    maxLines = 2
                )
                y += 158f
            } else {
                canvas.drawText(weekdayText(day), 80f, y + 42f, dayPaint)
                drawWrappedText(
                    canvas = canvas,
                    text = timeText,
                    x = 276f,
                    baselineY = y + 42f,
                    maxWidth = width - 350f,
                    paint = timePaint,
                    lineHeight = 39f,
                    maxLines = 2
                )
                y += 116f
            }
        }

        val footer = "来自瓜瓜课程表"
        canvas.drawText(footer, 80f, height - 70f, footerPaint)
        return bitmap
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        baselineY: Float,
        maxWidth: Float,
        paint: Paint,
        lineHeight: Float,
        maxLines: Int
    ) {
        if (text.isBlank()) return

        val pieces = text.split("  ·  ")
        val lines = mutableListOf<String>()
        var current = ""

        pieces.forEach { piece ->
            val candidate = if (current.isBlank()) piece else "$current  ·  $piece"
            if (paint.measureText(candidate) <= maxWidth || current.isBlank()) {
                current = candidate
            } else {
                lines += current
                current = piece
            }
        }
        if (current.isNotBlank()) lines += current

        lines.take(maxLines).forEachIndexed { index, line ->
            val visible = if (index == maxLines - 1 && lines.size > maxLines) {
                var trimmed = line
                while (trimmed.isNotEmpty() && paint.measureText("$trimmed…") > maxWidth) {
                    trimmed = trimmed.dropLast(1)
                }
                "$trimmed…"
            } else {
                line
            }
            canvas.drawText(visible, x, baselineY + index * lineHeight, paint)
        }
    }
}

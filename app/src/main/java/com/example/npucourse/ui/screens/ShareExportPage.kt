package com.example.npucourse.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.npucourse.model.DemoCourse
import com.example.npucourse.model.Semester
import com.example.npucourse.sharing.TimetableExportManager
import com.example.npucourse.util.MAX_SEMESTER_WEEKS
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ShareExportPage(
    semester: Semester,
    courses: List<DemoCourse>,
    currentWeek: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedWeek by remember {
        mutableIntStateOf(
            currentWeek.coerceIn(1, MAX_SEMESTER_WEEKS)
        )
    }
    var pendingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingIcs by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }

    val imageSaver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        val bitmap = pendingBitmap
        if (uri != null && bitmap != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                } ?: error("无法打开保存位置")
            }.onSuccess {
                status = "课表图片已保存"
            }.onFailure {
                status = "保存失败：${it.message ?: "未知错误"}"
            }
        }
        pendingBitmap = null
    }

    val icsSaver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/calendar")
    ) { uri ->
        val ics = pendingIcs
        if (uri != null && ics != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
                    it.write(ics)
                } ?: error("无法打开保存位置")
            }.onSuccess {
                status = "ICS 日历已保存"
            }.onFailure {
                status = "保存失败：${it.message ?: "未知错误"}"
            }
        }
        pendingIcs = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onBack) { Text("← 返回") }
        Spacer(Modifier.height(8.dp))

        Text(
            text = "导出与分享",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = semester.name,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(22.dp))

        ExportCard(
            title = "课表图片",
            subtitle = "生成当前选中周的一屏七天课表图片，可保存或分享。"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    enabled = selectedWeek > 1,
                    onClick = { selectedWeek-- }
                ) { Text("上一周") }

                Text(
                    text = "第${selectedWeek}周",
                    modifier = Modifier.padding(top = 12.dp),
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(
                    enabled = selectedWeek < MAX_SEMESTER_WEEKS,
                    onClick = { selectedWeek++ }
                ) { Text("下一周") }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val bitmap = TimetableExportManager.renderWeekBitmap(
                        semesterName = semester.name,
                        semesterStartMillis = semester.startMillis,
                        campus = semester.campus,
                        week = selectedWeek,
                        courses = courses
                    )
                    TimetableExportManager.shareBitmap(
                        context = context,
                        bitmap = bitmap,
                        fileName = "GuaguaCourse_week_${selectedWeek}.png"
                    )
                }
            ) { Text("分享第${selectedWeek}周课表图片") }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    pendingBitmap = TimetableExportManager.renderWeekBitmap(
                        semesterName = semester.name,
                        semesterStartMillis = semester.startMillis,
                        campus = semester.campus,
                        week = selectedWeek,
                        courses = courses
                    )
                    imageSaver.launch(
                        "瓜瓜课程表_${safeFileName(semester.name)}_第${selectedWeek}周.png"
                    )
                }
            ) { Text("保存 PNG 图片") }
        }

        Spacer(Modifier.height(16.dp))

        ExportCard(
            title = "ICS 课程日历",
            subtitle = "把整个学期的课程按真实日期和上课时间导出，可导入系统日历、Google Calendar、Outlook 等。"
        ) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val ics = TimetableExportManager.buildIcs(
                        semesterName = semester.name,
                        semesterStartMillis = semester.startMillis,
                        campus = semester.campus,
                        courses = courses
                    )
                    TimetableExportManager.shareIcs(
                        context = context,
                        content = ics,
                        fileName = "GuaguaCourse_${safeFileName(semester.name)}.ics"
                    )
                }
            ) { Text("分享 ICS 日历") }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    pendingIcs = TimetableExportManager.buildIcs(
                        semesterName = semester.name,
                        semesterStartMillis = semester.startMillis,
                        campus = semester.campus,
                        courses = courses
                    )
                    icsSaver.launch(
                        "瓜瓜课程表_${safeFileName(semester.name)}.ics"
                    )
                }
            ) { Text("保存 ICS 文件") }
        }

        if (status != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = status ?: "",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun ExportCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

private fun safeFileName(text: String): String =
    text.replace(Regex("[\\\\/:*?\"<>|]"), "_")

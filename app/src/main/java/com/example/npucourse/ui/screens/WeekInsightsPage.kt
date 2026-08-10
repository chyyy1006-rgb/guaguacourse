package com.example.npucourse.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.npucourse.model.DemoCourse
import com.example.npucourse.sharing.FreeTimeShareManager
import com.example.npucourse.util.buildWeeklyScheduleInsights
import com.example.npucourse.util.minutesToClockText
import com.example.npucourse.util.weekdayText

@Composable
fun WeekInsightsPage(
    courses: List<DemoCourse>,
    currentWeek: Int,
    campus: String,
    semesterName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showShareOptions by remember { mutableStateOf(false) }
    val insights = buildWeeklyScheduleInsights(courses, currentWeek, campus)
    val groupedFree = insights.freeWindows.groupBy { it.day }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("返回") }
            TextButton(
                enabled = currentWeek in 1..20,
                onClick = {
                    showShareOptions = true
                }
            ) { Text("分享空闲") }
        }

        Text("本周分析", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text(
            text = if (currentWeek in 1..20) "$semesterName · 第${currentWeek}周" else "$semesterName · 当前不在教学周",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            InsightMetricCard(
                modifier = Modifier.weight(1f),
                title = "课程安排",
                value = "${insights.activeCourseCount} 条"
            )
            InsightMetricCard(
                modifier = Modifier.weight(1f),
                title = "课内时间",
                value = formatDuration(insights.scheduledMinutes)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            InsightMetricCard(
                modifier = Modifier.weight(1f),
                title = "最忙一天",
                value = insights.busiestDay?.let(::weekdayText) ?: "—"
            )
            InsightMetricCard(
                modifier = Modifier.weight(1f),
                title = "冲突",
                value = "${insights.conflicts.size} 处",
                emphasize = insights.conflicts.isNotEmpty()
            )
        }

        Spacer(Modifier.height(22.dp))
        Text("课程冲突检查", fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))

        if (currentWeek !in 1..20) {
            SimpleInfoCard("当前不在第 1–20 教学周，暂不计算本周分析。")
        } else if (insights.conflicts.isEmpty()) {
            SimpleInfoCard("本周没有检测到时间重叠的课程。")
        } else {
            insights.conflicts.forEach { conflict ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(Modifier.padding(15.dp)) {
                        Text(
                            text = weekdayText(conflict.day),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "${conflict.first.name}  ↔  ${conflict.second.name}",
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "请检查节次、周次或教务同步结果。",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("连续空闲时间", fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text(
            text = "统计 08:00–22:00 内至少连续 1 小时的空档。",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))

        if (currentWeek in 1..20) {
            (1..7).forEach { day ->
                val windows = groupedFree[day].orEmpty()
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(Modifier.padding(15.dp)) {
                        Text(weekdayText(day), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = if (windows.isEmpty()) {
                                "没有连续 1 小时以上的空档"
                            } else {
                                windows.joinToString("  ·  ") { window ->
                                    "${minutesToClockText(window.startMinutes)}–${minutesToClockText(window.endMinutes)}"
                                }
                            },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(30.dp))
    }

    if (showShareOptions) {
        AlertDialog(
            onDismissRequest = { showShareOptions = false },
            title = { Text("分享连续空闲时间") },
            text = {
                Column {
                    Text(
                        "选择分享格式。图片会自动生成后进入系统分享面板。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    ShareFormatCard(
                        title = "纯文字",
                        subtitle = "适合微信、QQ、群聊直接发送",
                        onClick = {
                            showShareOptions = false
                            FreeTimeShareManager.shareText(
                                context,
                                semesterName,
                                currentWeek,
                                insights.freeWindows
                            )
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    ShareFormatCard(
                        title = "简洁图片",
                        subtitle = "白底列表，适合快速查看与保存",
                        onClick = {
                            showShareOptions = false
                            FreeTimeShareManager.shareImage(
                                context,
                                semesterName,
                                currentWeek,
                                insights.freeWindows,
                                FreeTimeShareManager.ImageStyle.SIMPLE
                            )
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    ShareFormatCard(
                        title = "卡片图片",
                        subtitle = "卡片式排版，更适合朋友圈或群聊",
                        onClick = {
                            showShareOptions = false
                            FreeTimeShareManager.shareImage(
                                context,
                                semesterName,
                                currentWeek,
                                insights.freeWindows,
                                FreeTimeShareManager.ImageStyle.CARD
                            )
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showShareOptions = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ShareFormatCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(
                subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InsightMetricCard(
    modifier: Modifier,
    title: String,
    value: String,
    emphasize: Boolean = false
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (emphasize) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SimpleInfoCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDuration(minutes: Int): String {
    if (minutes <= 0) return "0 分钟"
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours <= 0 -> "${rest}分钟"
        rest == 0 -> "${hours}小时"
        else -> "${hours}小时${rest}分"
    }
}


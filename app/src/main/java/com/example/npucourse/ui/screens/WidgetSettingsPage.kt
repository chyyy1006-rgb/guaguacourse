package com.example.npucourse.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.npucourse.widget.AcademicOverviewWidgetUpdater
import com.example.npucourse.widget.TodayScheduleWidgetPinHelper
import com.example.npucourse.widget.TodayScheduleWidgetUpdater

@Composable
fun WidgetSettingsPage(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var message by remember {
        mutableStateOf<String?>(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        TextButton(onClick = onBack) {
            Text("← 返回")
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "桌面小组件",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "现在提供 5 种桌面组件：DDL 倒计时、下一节课、今日课程、待办清单和学业概览。",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(22.dp))

        WidgetCard(
            title = "DDL 倒计时 · 2×1",
            subtitle = "最紧凑的待办组件。只显示最近一项未完成待办、DDL 倒计时、截止时间和关联课程。",
            buttonText = "添加 DDL 倒计时",
            onAdd = {
                val sent = TodayScheduleWidgetPinHelper.requestTaskCompact(context)
                message = if (sent) {
                    "已向桌面启动器发送“DDL 倒计时”添加请求"
                } else {
                    manualAddMessage()
                }
            }
        )

        Spacer(Modifier.height(14.dp))

        WidgetCard(
            title = "下一节课 · 2×2",
            subtitle = "适合较小桌面空间。显示正在上课或下一节课程、倒计时、教室，并附最近一项待办。",
            buttonText = "添加下一节课小组件",
            onAdd = {
                val sent =
                    TodayScheduleWidgetPinHelper
                        .requestNext(context)

                message = if (sent) {
                    "已向桌面启动器发送“下一节课”添加请求"
                } else {
                    manualAddMessage()
                }
            }
        )

        Spacer(Modifier.height(14.dp))

        WidgetCard(
            title = "今日课程 · 4×2",
            subtitle = "适合较宽桌面空间。显示教学周、日期和课程；有待办时自动让出空间显示最近 2 项事项。",
            buttonText = "添加今日课程小组件",
            onAdd = {
                val sent =
                    TodayScheduleWidgetPinHelper
                        .requestToday(context)

                message = if (sent) {
                    "已向桌面启动器发送“今日课程”添加请求"
                } else {
                    manualAddMessage()
                }
            }
        )

        Spacer(Modifier.height(14.dp))

        WidgetCard(
            title = "待办清单 · 4×3 / 可缩放",
            subtitle = "仅显示未完成待办。拖动组件高度后会自动在 2～5 条之间调整显示数量。",
            buttonText = "添加待办清单",
            onAdd = {
                val sent = TodayScheduleWidgetPinHelper.requestTaskList(context)
                message = if (sent) {
                    "已向桌面启动器发送“待办清单”添加请求"
                } else {
                    manualAddMessage()
                }
            }
        )

        Spacer(Modifier.height(14.dp))

        WidgetCard(
            title = "学业概览 · 4×2",
            subtitle = "显示最近一场考试、倒计时、考场与当前 GPA。数据来自“考试与成绩”的最近一次查询结果。",
            buttonText = "添加学业概览",
            onAdd = {
                val sent = TodayScheduleWidgetPinHelper.requestAcademicOverview(context)
                message = if (sent) {
                    "已向桌面启动器发送“学业概览”添加请求"
                } else {
                    manualAddMessage()
                }
            }
        )

        Spacer(Modifier.height(14.dp))

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                TodayScheduleWidgetUpdater
                    .updateAll(context)
                AcademicOverviewWidgetUpdater.updateAll(context)

                message =
                    "已刷新全部 瓜瓜课程表 桌面小组件"
            }
        ) {
            Text("刷新全部小组件")
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {
                "如果系统没有弹出添加窗口，也可以长按手机桌面 → 小组件 → 瓜瓜课程表，手动选择 5 种组件。待办清单添加后可拖动边缘测试不同高度。"
            } else {
                "当前 Android 版本需要从桌面的小组件列表手动添加。"
            },
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (message != null) {
            Spacer(Modifier.height(14.dp))

            Text(
                text = message ?: "",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun WidgetCard(
    title: String,
    subtitle: String,
    buttonText: String,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(18.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onAdd
            ) {
                Text(buttonText)
            }
        }
    }
}

private fun manualAddMessage(): String =
    "当前桌面不支持应用内直接添加，请长按桌面 → 小组件 → 瓜瓜课程表"

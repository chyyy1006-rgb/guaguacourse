package com.example.npucourse.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.npucourse.notification.ReminderPermissionManager

@Composable
fun ReminderPermissionDialog(
    status: ReminderPermissionManager.Status,
    onContinueSetup: () -> Unit,
    onDismiss: () -> Unit,
    title: String = "开启提醒通知"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "为了让 DDL / 课程提醒能在屏幕顶部弹出横幅并播放提示音，需要开启通知权限；为了尽量准时，还建议允许“闹钟和提醒”。",
                    fontSize = 14.sp
                )

                PermissionStatusRow(
                    label = "通知权限",
                    ready = status.notificationAccessReady,
                    readyText = "已开启",
                    missingText = "未开启"
                )
                PermissionStatusRow(
                    label = "横幅与铃声",
                    ready = status.taskChannelHeadsUpAndSoundEnabled,
                    readyText = "已开启",
                    missingText = "需要系统确认"
                )
                PermissionStatusRow(
                    label = "准时提醒",
                    ready = status.exactAlarmAllowed,
                    readyText = "已允许",
                    missingText = "建议开启"
                )

                Text(
                    "这里使用的是 Android 通知横幅（Heads-up），不会申请可覆盖其他 App 的系统悬浮窗权限。系统或厂商省电策略仍可能影响最终显示方式。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onContinueSetup) {
                Text(if (status.fullyReady) "完成" else "继续设置")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("稍后") }
        }
    )
}

@Composable
private fun PermissionStatusRow(
    label: String,
    ready: Boolean,
    readyText: String,
    missingText: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = FontWeight.SemiBold)
        Text(
            if (ready) readyText else missingText,
            color = if (ready) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error,
            fontSize = 13.sp
        )
    }
}

package com.example.npucourse.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun UpdatePromptDialog(
    info: AppUpdateInfo,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!info.forceUpdate) onDismiss()
        },
        title = {
            Text("发现新版本 ${info.versionName}")
        },
        text = {
            Column {
                Text(
                    text = info.title,
                    fontWeight = FontWeight.SemiBold
                )
                if (info.publishedAt.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "发布时间：${info.publishedAt}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (info.changelog.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    info.changelog.forEach { item ->
                        Text("• $item")
                        Spacer(Modifier.height(4.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "点击“立即更新”将打开瓜瓜课程表官方 GitHub 发布页。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onUpdate) {
                Text("立即更新")
            }
        },
        dismissButton = if (!info.forceUpdate) {
            {
                TextButton(onClick = onDismiss) {
                    Text("稍后")
                }
            }
        } else {
            null
        }
    )
}

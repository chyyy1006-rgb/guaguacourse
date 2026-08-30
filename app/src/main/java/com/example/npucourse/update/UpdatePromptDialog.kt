package com.example.npucourse.update

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun UpdatePromptDialog(
    info: AppUpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var downloading by remember(info.versionCode) { mutableStateOf(false) }
    var progress by remember(info.versionCode) { mutableStateOf<Int?>(null) }
    var downloadedFile by remember(info.versionCode) { mutableStateOf<File?>(null) }
    var statusText by remember(info.versionCode) { mutableStateOf<String?>(null) }
    var errorText by remember(info.versionCode) { mutableStateOf<String?>(null) }


    val installPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        downloadedFile?.let { file ->
            when (val install = AppUpdateManager.installDownloadedApk(context, file)) {
                UpdateInstallResult.Started -> {
                    errorText = null
                    statusText = "已打开系统安装界面"
                }
                UpdateInstallResult.PermissionRequired -> {
                    statusText = null
                    errorText = "需要在系统设置中允许安装未知应用后才能继续"
                }
                is UpdateInstallResult.Failed -> {
                    statusText = null
                    errorText = install.message
                }
            }
        }
    }

    fun requestInstall(file: File) {
        when (val install = AppUpdateManager.installDownloadedApk(context, file)) {
            UpdateInstallResult.Started -> {
                errorText = null
                statusText = "已打开系统安装界面"
            }
            UpdateInstallResult.PermissionRequired -> {
                statusText = "请在系统设置中允许安装更新"
                installPermissionLauncher.launch(AppUpdateManager.unknownSourcesSettingsIntent(context))
            }
            is UpdateInstallResult.Failed -> {
                statusText = null
                errorText = install.message
            }
        }
    }

    fun downloadOrInstall() {
        downloadedFile?.takeIf { it.isFile }?.let {
            requestInstall(it)
            return
        }
        if (downloading) return

        downloading = true
        progress = null
        statusText = "正在下载更新包…"
        errorText = null
        scope.launch {
            when (
                val result = AppUpdateManager.downloadApk(
                    context = context,
                    info = info,
                    onProgress = { progress = it }
                )
            ) {
                is UpdateDownloadResult.Success -> {
                    downloading = false
                    downloadedFile = result.file
                    progress = 100
                    statusText = "下载完成，正在准备安装…"
                    requestInstall(result.file)
                }
                is UpdateDownloadResult.Failed -> {
                    downloading = false
                    statusText = null
                    errorText = result.message
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!info.forceUpdate && !downloading) onDismiss()
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

                if (downloading || statusText != null || errorText != null) {
                    Spacer(Modifier.height(12.dp))
                }

                if (downloading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = progress?.let { "正在下载 $it%" } ?: "正在下载…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    statusText?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                errorText?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(6.dp))
                    TextButton(
                        onClick = {
                            AppUpdateManager.openDownloadPage(context, AppUpdateManager.RELEASES_URL)
                        }
                    ) {
                        Text("前往发布页")
                    }
                }

                if (!downloading && statusText == null && errorText == null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "更新包会在应用内下载，完成后由 Android 系统确认安装。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { downloadOrInstall() },
                enabled = !downloading
            ) {
                Text(
                    when {
                        downloading -> "下载中"
                        downloadedFile?.isFile == true -> "继续安装"
                        errorText != null -> "重试"
                        else -> "下载并安装"
                    }
                )
            }
        },
        dismissButton = if (!info.forceUpdate && !downloading) {
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

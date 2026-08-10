package com.example.npucourse.ui.screens

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
import androidx.compose.ui.unit.sp
import com.example.npucourse.update.AppUpdateInfo
import com.example.npucourse.update.AppUpdateManager
import com.example.npucourse.update.UpdateCheckResult
import com.example.npucourse.update.UpdatePromptDialog
import kotlinx.coroutines.launch

@Composable
fun AboutUpdatePage(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var checking by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }

    val currentName = remember { AppUpdateManager.currentVersionName(context) }
    val currentCode = remember { AppUpdateManager.currentVersionCode(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("返回")
        }

        Text(
            text = "关于瓜瓜课程表",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = "版本与更新",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = "瓜瓜课程表",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "当前版本 $currentName · versionCode $currentCode",
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        if (!checking) {
                            checking = true
                            resultText = null
                            scope.launch {
                                when (val result = AppUpdateManager.checkForUpdates(context, manual = true)) {
                                    is UpdateCheckResult.UpdateAvailable -> {
                                        updateInfo = result.info
                                        resultText = "发现新版本 ${result.info.versionName}"
                                    }
                                    is UpdateCheckResult.UpToDate -> {
                                        resultText = "当前已经是最新版本"
                                    }
                                    is UpdateCheckResult.Failed -> {
                                        resultText = result.message
                                    }
                                }
                                checking = false
                            }
                        }
                    }
                ) {
                    if (checking) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("检查更新")
                    }
                }
                resultText?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        AboutActionCard(
            title = "GitHub 发布页",
            subtitle = "查看版本说明与下载正式 APK",
            onClick = {
                AppUpdateManager.openDownloadPage(context, AppUpdateManager.RELEASES_URL)
            }
        )


        Spacer(Modifier.height(22.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    text = "更新说明",
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    text = "瓜瓜课程表会在启动后定期检查官方 GitHub 版本清单。发现新版时会提示你前往官方发布页，安装仍由 Android 系统确认，不会静默替换应用。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(Modifier.height(34.dp))
    }

    updateInfo?.let { info ->
        UpdatePromptDialog(
            info = info,
            onUpdate = {
                AppUpdateManager.openDownloadPage(context, info.downloadUrl)
            },
            onDismiss = { updateInfo = null }
        )
    }
}

@Composable
private fun AboutActionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
            Text(
                text = "›",
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

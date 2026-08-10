package com.example.npucourse.ui.screens

import android.net.Uri
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.npucourse.data.backup.AppBackupManager
import com.example.npucourse.data.backup.BackupPreview
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch


@Composable
fun DataManagementPage(
    semesterCount: Int,
    courseCount: Int,
    taskCount: Int = 0,
    onBack: () -> Unit
) {

    val context =
        LocalContext.current

    val scope =
        rememberCoroutineScope()

    val backupManager =
        remember {
            AppBackupManager(
                context
            )
        }

    var busy by remember {
        mutableStateOf(false)
    }

    var message by remember {
        mutableStateOf<String?>(
            null
        )
    }

    var pendingRestoreUri by remember {
        mutableStateOf<Uri?>(
            null
        )
    }

    var pendingPreview by remember {
        mutableStateOf<BackupPreview?>(
            null
        )
    }


    val exportLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .CreateDocument(
                        "application/json"
                    )
        ) {
            uri ->

            if (
                uri == null
            ) {
                return@rememberLauncherForActivityResult
            }

            busy =
                true

            message =
                null

            scope.launch {

                runCatching {
                    backupManager
                        .exportTo(
                            uri
                        )
                }
                    .onSuccess {
                        preview ->

                        message =
                            "备份完成：" +
                                preview.semesterCount +
                                " 个课表，" +
                                preview.courseCount +
                                " 条课程，" +
                                preview.taskCount +
                                " 条待办"
                    }
                    .onFailure {
                        throwable ->

                        message =
                            "备份失败：" +
                                (
                                    throwable.message
                                        ?: "未知错误"
                                    )
                    }

                busy =
                    false
            }
        }


    val importLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .OpenDocument()
        ) {
            uri ->

            if (
                uri == null
            ) {
                return@rememberLauncherForActivityResult
            }

            busy =
                true

            message =
                null

            scope.launch {

                runCatching {
                    backupManager
                        .preview(
                            uri
                        )
                }
                    .onSuccess {
                        preview ->

                        pendingRestoreUri =
                            uri

                        pendingPreview =
                            preview
                    }
                    .onFailure {
                        throwable ->

                        message =
                            "无法读取备份：" +
                                (
                                    throwable.message
                                        ?: "文件格式错误"
                                    )
                    }

                busy =
                    false
            }
        }


    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 20.dp
                )
    ) {

        Spacer(
            modifier =
                Modifier.height(
                    18.dp
                )
        )

        TextButton(
            onClick =
                onBack
        ) {
            Text(
                "← 返回"
            )
        }

        Spacer(
            modifier =
                Modifier.height(
                    10.dp
                )
        )

        Text(
            text =
                "数据管理",
            fontSize =
                30.sp,
            fontWeight =
                FontWeight.Bold,
            color =
                MaterialTheme.colorScheme.onBackground
        )

        Spacer(
            modifier =
                Modifier.height(
                    6.dp
                )
        )

        Text(
            text =
                "备份多个学期、课程和应用设置",
            fontSize =
                14.sp,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(
            modifier =
                Modifier.height(
                    22.dp
                )
        )

        Card(
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(
                    22.dp
                ),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.primaryContainer
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(
                        20.dp
                    )
            ) {

                Text(
                    text =
                        "当前数据",
                    fontSize =
                        15.sp,
                    fontWeight =
                        FontWeight.SemiBold,
                    color =
                        MaterialTheme.colorScheme.primary
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            10.dp
                        )
                )

                Text(
                    text =
                        semesterCount.toString() +
                            " 个课表 · " +
                            courseCount +
                            " 条课程 · " +
                            taskCount +
                            " 条待办",
                    fontSize =
                        24.sp,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        MaterialTheme.colorScheme.onSurface
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )

                Text(
                    text =
                        "备份不会包含翱翔门户密码、Cookie 或登录凭据。",
                    fontSize =
                        13.sp,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(
                    18.dp
                )
        )

        ActionCard(
            title =
                "导出完整备份",
            subtitle =
                "保存为 JSON 文件，可用于换机、重装或版本升级前留档",
            buttonText =
                if (
                    busy
                ) {
                    "处理中…"
                } else {
                    "选择保存位置"
                },
            enabled =
                !busy,
            onClick = {

                exportLauncher.launch(
                    buildBackupFileName()
                )
            }
        )

        Spacer(
            modifier =
                Modifier.height(
                    14.dp
                )
        )

        ActionCard(
            title =
                "从备份恢复",
            subtitle =
                "恢复会替换当前所有课表和课程，执行前会再次确认",
            buttonText =
                if (
                    busy
                ) {
                    "处理中…"
                } else {
                    "选择备份文件"
                },
            enabled =
                !busy,
            onClick = {

                importLauncher.launch(
                    arrayOf(
                        "application/json",
                        "text/plain"
                    )
                )
            }
        )

        if (
            message != null
        ) {

            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )

            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(
                        16.dp
                    ),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.surfaceVariant
                    )
            ) {

                Text(
                    text =
                        message ?: "",
                    modifier =
                        Modifier.padding(
                            16.dp
                        ),
                    fontSize =
                        13.sp,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(
                    18.dp
                )
        )

        Text(
            text =
                "备份内容",
            fontSize =
                16.sp,
            fontWeight =
                FontWeight.SemiBold,
            color =
                MaterialTheme.colorScheme.onSurface
        )

        Spacer(
            modifier =
                Modifier.height(
                    10.dp
                )
        )

        BackupItem(
            "全部课表与开学日期"
        )

        BackupItem(
            "全部课程、周次、教室与教师"
        )

        BackupItem(
            "当前选中的课表"
        )

        BackupItem(
            "课程提醒、校区等基础设置"
        )

        Spacer(
            modifier =
                Modifier.height(
                    40.dp
                )
        )
    }


    val preview =
        pendingPreview

    if (
        preview != null &&
        pendingRestoreUri != null
    ) {

        AlertDialog(
            onDismissRequest = {
                pendingPreview =
                    null
                pendingRestoreUri =
                    null
            },
            title = {
                Text(
                    "恢复完整备份？"
                )
            },
            text = {

                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {

                    Text(
                        text =
                            "备份中包含 " +
                                preview.semesterCount +
                                " 个课表、" +
                                preview.courseCount +
                                " 条课程、" +
                                preview.taskCount +
                                " 条待办。"
                    )

                    Text(
                        text =
                            "恢复后，当前本地数据会被完整替换。建议先导出一次当前备份。",
                        color =
                            MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingPreview =
                            null
                        pendingRestoreUri =
                            null
                    }
                ) {
                    Text(
                        "取消"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {

                        val restoreUri =
                            pendingRestoreUri

                        if (
                            restoreUri == null
                        ) {
                            return@Button
                        }

                        pendingPreview =
                            null

                        pendingRestoreUri =
                            null

                        busy =
                            true

                        scope.launch {

                            runCatching {
                                backupManager
                                    .restoreFrom(
                                        restoreUri
                                    )
                            }
                                .onSuccess {
                                    restored ->

                                    message =
                                        "恢复完成：" +
                                            restored.semesterCount +
                                            " 个课表，" +
                                            restored.courseCount +
                                            " 条课程，" +
                                            restored.taskCount +
                                            " 条待办"
                                }
                                .onFailure {
                                    throwable ->

                                    message =
                                        "恢复失败：" +
                                            (
                                                throwable.message
                                                    ?: "未知错误"
                                                )
                                }

                            busy =
                                false
                        }
                    }
                ) {
                    Text(
                        "确认恢复"
                    )
                }
            }
        )
    }
}


@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    buttonText: String,
    enabled: Boolean,
    onClick: () -> Unit
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                20.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    1.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    18.dp
                )
        ) {

            Text(
                text =
                    title,
                fontSize =
                    17.sp,
                fontWeight =
                    FontWeight.Bold,
                color =
                    MaterialTheme.colorScheme.onSurface
            )

            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )

            Text(
                text =
                    subtitle,
                fontSize =
                    13.sp,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )

            OutlinedButton(
                modifier =
                    Modifier.fillMaxWidth(),
                enabled =
                    enabled,
                onClick =
                    onClick
            ) {
                Text(
                    buttonText
                )
            }
        }
    }
}


@Composable
private fun BackupItem(
    text: String
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 5.dp
                )
    ) {

        Text(
            text =
                "•",
            color =
                MaterialTheme.colorScheme.primary,
            fontWeight =
                FontWeight.Bold
        )

        Text(
            text =
                "  " + text,
            fontSize =
                14.sp,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


private fun buildBackupFileName():
    String {

    val suffix =
        SimpleDateFormat(
            "yyyyMMdd_HHmm",
            Locale.CHINA
        ).format(
            Date()
        )

    return "GuaguaCourse_backup_" +
        suffix +
        ".json"
}

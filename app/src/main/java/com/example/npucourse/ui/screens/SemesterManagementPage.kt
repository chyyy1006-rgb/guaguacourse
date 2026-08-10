package com.example.npucourse.ui.screens

import android.app.DatePickerDialog
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.npucourse.model.Semester
import com.example.npucourse.util.campusDisplayName
import com.example.npucourse.util.formatSemesterDate
import java.util.Calendar


@Composable
fun SemesterManagementPage(
    semesters: List<Semester>,
    selectedSemesterId: Long,
    courseCountBySemester: Map<Long, Int>,
    onBack: () -> Unit,
    onSemesterSelected: (Long) -> Unit,
    onCreateSemester: (
        name: String,
        startMillis: Long,
        campus: String
    ) -> Unit,
    onRenameSemester: (
        semesterId: Long,
        newName: String
    ) -> Unit,
    onDuplicateSemester: (
        sourceSemesterId: Long,
        newName: String
    ) -> Unit,
    onDeleteSemester: (Long) -> Unit
) {

    val selectedSemester =
        semesters.firstOrNull {
            it.id == selectedSemesterId
        } ?: semesters.firstOrNull()

    var showCreateDialog by remember {
        mutableStateOf(false)
    }

    var showRenameDialog by remember {
        mutableStateOf(false)
    }

    var showDuplicateDialog by remember {
        mutableStateOf(false)
    }

    var showDeleteDialog by remember {
        mutableStateOf(false)
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
                Modifier.height(18.dp)
        )

        OutlinedButton(
            onClick = onBack
        ) {
            Text("← 返回我的")
        }

        Spacer(
            modifier =
                Modifier.height(18.dp)
        )

        Text(
            text = "课表管理",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(
            modifier =
                Modifier.height(6.dp)
        )

        Text(
            text =
                "每个课表都有独立的开学日期、校区和课程。",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        Button(
            modifier =
                Modifier.fillMaxWidth(),
            onClick = {
                showCreateDialog = true
            }
        ) {
            Text("＋ 新建空白课表")
        }

        Spacer(
            modifier =
                Modifier.height(18.dp)
        )

        semesters.forEach { semester ->

            SemesterManagementCard(
                semester = semester,
                selected =
                    semester.id == selectedSemesterId,
                courseCount =
                    courseCountBySemester[
                        semester.id
                    ] ?: 0,
                onClick = {
                    onSemesterSelected(
                        semester.id
                    )
                }
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )
        }

        if (
            selectedSemester != null
        ) {

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            Text(
                text = "当前课表操作",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                OutlinedButton(
                    modifier =
                        Modifier.weight(1f),
                    onClick = {
                        showRenameDialog = true
                    }
                ) {
                    Text("重命名")
                }

                OutlinedButton(
                    modifier =
                        Modifier.weight(1f),
                    onClick = {
                        showDuplicateDialog = true
                    }
                ) {
                    Text("复制")
                }
            }

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            OutlinedButton(
                modifier =
                    Modifier.fillMaxWidth(),
                enabled =
                    semesters.size > 1,
                onClick = {
                    showDeleteDialog = true
                }
            ) {
                Text(
                    if (
                        semesters.size > 1
                    ) {
                        "删除当前课表"
                    } else {
                        "至少保留一个课表"
                    }
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(40.dp)
        )
    }


    if (
        showCreateDialog
    ) {

        CreateSemesterDialog(
            initialStartMillis =
                selectedSemester
                    ?.startMillis
                    ?: System.currentTimeMillis(),
            inheritedCampus =
                selectedSemester
                    ?.campus
                    ?: "CHANGAN",
            onDismiss = {
                showCreateDialog = false
            },
            onCreate = {
                name,
                startMillis,
                campus ->

                onCreateSemester(
                    name,
                    startMillis,
                    campus
                )

                showCreateDialog = false
            }
        )
    }


    if (
        showRenameDialog &&
        selectedSemester != null
    ) {

        NameDialog(
            title = "重命名课表",
            initialName =
                selectedSemester.name,
            confirmText = "保存",
            onDismiss = {
                showRenameDialog = false
            },
            onConfirm = { newName ->

                onRenameSemester(
                    selectedSemester.id,
                    newName
                )

                showRenameDialog = false
            }
        )
    }


    if (
        showDuplicateDialog &&
        selectedSemester != null
    ) {

        NameDialog(
            title = "复制课表",
            initialName =
                selectedSemester.name +
                    " 副本",
            confirmText = "复制",
            onDismiss = {
                showDuplicateDialog = false
            },
            onConfirm = { newName ->

                onDuplicateSemester(
                    selectedSemester.id,
                    newName
                )

                showDuplicateDialog = false
            }
        )
    }


    if (
        showDeleteDialog &&
        selectedSemester != null
    ) {

        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            title = {
                Text("删除课表？")
            },
            text = {
                Text(
                    "将删除“" +
                        selectedSemester.name +
                        "”及其中的 " +
                        (
                            courseCountBySemester[
                                selectedSemester.id
                            ] ?: 0
                            ) +
                        " 条课程。此操作不可撤销。"
                )
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {
                    Text("取消")
                }
            },
            confirmButton = {
                Button(
                    onClick = {

                        onDeleteSemester(
                            selectedSemester.id
                        )

                        showDeleteDialog = false
                    }
                ) {
                    Text("删除")
                }
            }
        )
    }
}


@Composable
private fun SemesterManagementCard(
    semester: Semester,
    selected: Boolean,
    courseCount: Int,
    onClick: () -> Unit
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                },
        shape =
            RoundedCornerShape(18.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(18.dp)
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = semester.name,
                    modifier =
                        Modifier.weight(1f),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (selected) {
                    Text(
                        text = "当前",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(7.dp)
            )

            Text(
                text =
                    "开学：" +
                        formatSemesterDate(
                            semester.startMillis
                        ),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier =
                    Modifier.height(3.dp)
            )

            Text(
                text =
                    campusDisplayName(
                        semester.campus
                    ) +
                        " · " +
                        courseCount +
                        " 条课程",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun CreateSemesterDialog(
    initialStartMillis: Long,
    inheritedCampus: String,
    onDismiss: () -> Unit,
    onCreate: (
        name: String,
        startMillis: Long,
        campus: String
    ) -> Unit
) {

    val context =
        LocalContext.current

    var name by remember {
        mutableStateOf("新课表")
    }

    var startMillis by remember {
        mutableLongStateOf(
            initialStartMillis
        )
    }

    val dateCalendar =
        Calendar
            .getInstance()
            .apply {
                timeInMillis =
                    startMillis
            }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("新建空白课表")
        },
        text = {

            Column {

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    label = {
                        Text("课表名称")
                    },
                    singleLine = true
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                OutlinedButton(
                    modifier =
                        Modifier.fillMaxWidth(),
                    onClick = {

                        DatePickerDialog(
                            context,
                            {
                                _,
                                year,
                                month,
                                dayOfMonth ->

                                val calendar =
                                    Calendar
                                        .getInstance()
                                        .apply {
                                            set(
                                                year,
                                                month,
                                                dayOfMonth,
                                                0,
                                                0,
                                                0
                                            )
                                            set(
                                                Calendar.MILLISECOND,
                                                0
                                            )
                                        }

                                startMillis =
                                    calendar.timeInMillis
                            },
                            dateCalendar.get(
                                Calendar.YEAR
                            ),
                            dateCalendar.get(
                                Calendar.MONTH
                            ),
                            dateCalendar.get(
                                Calendar.DAY_OF_MONTH
                            )
                        ).show()
                    }
                ) {
                    Text(
                        "开学日期：" +
                            formatSemesterDate(
                                startMillis
                            )
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                Text(
                    text =
                        "校区沿用当前课表：" +
                            campusDisplayName(
                                inheritedCampus
                            ),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("取消")
            }
        },
        confirmButton = {
            Button(
                enabled =
                    name.isNotBlank(),
                onClick = {
                    onCreate(
                        name.trim(),
                        startMillis,
                        inheritedCampus
                    )
                }
            ) {
                Text("创建")
            }
        }
    )
}


@Composable
private fun NameDialog(
    title: String,
    initialName: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {

    var name by remember(
        initialName
    ) {
        mutableStateOf(
            initialName
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title)
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                },
                modifier =
                    Modifier.fillMaxWidth(),
                label = {
                    Text("课表名称")
                },
                singleLine = true
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("取消")
            }
        },
        confirmButton = {
            Button(
                enabled =
                    name.isNotBlank(),
                onClick = {
                    onConfirm(
                        name.trim()
                    )
                }
            ) {
                Text(confirmText)
            }
        }
    )
}

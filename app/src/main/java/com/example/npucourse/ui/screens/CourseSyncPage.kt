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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.npucourse.importer.CourseImportMapper
import com.example.npucourse.importer.EduCourseRecord
import com.example.npucourse.importer.NwpuSemesterInfo
import com.example.npucourse.model.DemoCourse
import com.example.npucourse.model.WeekMode
import com.example.npucourse.model.canonicalCustomWeeks
import com.example.npucourse.model.weekDisplayText
import com.example.npucourse.util.formatSemesterDate


enum class CourseSyncStrategy {
    SMART_MERGE,
    ADD_ONLY,
    FULL_REPLACE
}


private data class CourseSyncPreview(
    val addedCount: Int,
    val updatedCount: Int,
    val unchangedCount: Int,
    val localOnlyCount: Int,
    val appendOnlyCourses: List<DemoCourse>,
    val smartMergedCourses: List<DemoCourse>
)


@Composable
fun CourseSyncPage(
    existingCourses: List<DemoCourse>,
    portalLoginCompleted: Boolean,
    eduRecords: List<EduCourseRecord>,
    detectedSemester: NwpuSemesterInfo?,
    onOpenPortalLogin: () -> Unit,
    onBack: () -> Unit,
    onApplySync: (CourseSyncStrategy, List<DemoCourse>) -> Unit
) {

    val importResult =
        CourseImportMapper.map(
            eduRecords
        )

    val preview =
        remember(
            existingCourses,
            importResult.courses
        ) {
            buildSyncPreview(
                existingCourses = existingCourses,
                incomingCourses = importResult.courses
            )
        }

    var strategy by remember {
        mutableStateOf(
            CourseSyncStrategy.SMART_MERGE
        )
    }

    var showReplaceConfirm by remember {
        mutableStateOf(false)
    }

    val actionCourses =
        when (strategy) {
            CourseSyncStrategy.SMART_MERGE ->
                preview.smartMergedCourses

            CourseSyncStrategy.ADD_ONLY ->
                preview.appendOnlyCourses

            CourseSyncStrategy.FULL_REPLACE ->
                importResult.courses
        }

    val actionEnabled =
        when (strategy) {
            CourseSyncStrategy.SMART_MERGE,
            CourseSyncStrategy.FULL_REPLACE ->
                importResult.courses.isNotEmpty()

            CourseSyncStrategy.ADD_ONLY ->
                preview.appendOnlyCourses.isNotEmpty()
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
            modifier = Modifier.height(18.dp)
        )

        OutlinedButton(
            onClick = onBack
        ) {
            Text("← 返回学业")
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "课程表同步",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = "翱翔教务",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(
            modifier = Modifier.height(22.dp)
        )

        SyncSourceCard(
            recordCount = eduRecords.size,
            portalLoginCompleted = portalLoginCompleted,
            onOpenPortalLogin = onOpenPortalLogin
        )

        if (
            detectedSemester != null
        ) {
            Spacer(
                modifier = Modifier.height(14.dp)
            )

            SemesterDetectedCard(
                semester = detectedSemester
            )
        }

        if (
            eduRecords.isEmpty()
        ) {
            Spacer(
                modifier = Modifier.height(18.dp)
            )

            InfoCard(
                title = "等待读取课表",
                text = "进入翱翔教务“我的课表”，读取当前学期后再进行同步。"
            )

            Spacer(
                modifier = Modifier.height(40.dp)
            )

            return
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        SyncSummaryCard(
            sourceCount = eduRecords.size,
            parsedCount = importResult.courses.size,
            addedCount = preview.addedCount,
            updatedCount = preview.updatedCount,
            unchangedCount = preview.unchangedCount,
            localOnlyCount = preview.localOnlyCount,
            errorCount = importResult.errors.size
        )

        if (
            importResult.errors.isNotEmpty()
        ) {
            Spacer(
                modifier = Modifier.height(14.dp)
            )

            MessageCard(
                title = "未能解析的课程",
                messages = importResult.errors
            )
        }

        if (
            importResult.warnings.isNotEmpty()
        ) {
            Spacer(
                modifier = Modifier.height(14.dp)
            )

            MessageCard(
                title = "同步提示",
                messages = importResult.warnings
            )
        }

        Spacer(
            modifier = Modifier.height(22.dp)
        )

        Text(
            text = "同步方式",
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        StrategySelector(
            selected = strategy,
            onSelected = {
                strategy = it
            }
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        StrategyExplanation(
            strategy = strategy,
            localOnlyCount = preview.localOnlyCount,
            updatedCount = preview.updatedCount,
            addedCount = preview.addedCount
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Text(
            text = "课程预览",
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        importResult.courses.forEach {
                course ->

            val status =
                resolveIncomingStatus(
                    incoming = course,
                    existingCourses = existingCourses
                )

            CoursePreviewCard(
                course = course,
                status = status
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )
        }

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = actionEnabled,
            onClick = {
                if (
                    strategy == CourseSyncStrategy.FULL_REPLACE
                ) {
                    showReplaceConfirm = true
                } else {
                    onApplySync(
                        strategy,
                        actionCourses
                    )
                }
            }
        ) {
            Text(
                syncButtonText(
                    strategy = strategy,
                    preview = preview,
                    incomingCount = importResult.courses.size
                )
            )
        }

        Spacer(
            modifier = Modifier.height(40.dp)
        )
    }


    if (
        showReplaceConfirm
    ) {
        AlertDialog(
            onDismissRequest = {
                showReplaceConfirm = false
            },
            title = {
                Text("完全覆盖当前课表？")
            },
            text = {
                Text(
                    "当前学期的本地课程会全部删除，然后以本次从翱翔教务读取的 " +
                        importResult.courses.size +
                        " 条课程为准。手动添加但教务中不存在的课程也会被删除。"
                )
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showReplaceConfirm = false
                    }
                ) {
                    Text("取消")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReplaceConfirm = false
                        onApplySync(
                            CourseSyncStrategy.FULL_REPLACE,
                            importResult.courses
                        )
                    }
                ) {
                    Text("确认覆盖")
                }
            }
        )
    }
}


@Composable
private fun SyncSourceCard(
    recordCount: Int,
    portalLoginCompleted: Boolean,
    onOpenPortalLogin: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "翱翔教务同步",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text =
                    when {
                        recordCount > 0 ->
                            "已读取 $recordCount 条课程安排"

                        portalLoginCompleted ->
                            "登录状态已保存，可以重新读取课表"

                        else ->
                            "登录学校统一身份认证后读取本人课表"
                    },
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenPortalLogin
            ) {
                Text(
                    if (
                        recordCount > 0
                    ) {
                        "重新读取翱翔课表"
                    } else {
                        "登录并读取翱翔课表"
                    }
                )
            }
        }
    }
}


@Composable
private fun SemesterDetectedCard(
    semester: NwpuSemesterInfo
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (
                        semester.startMillis != null
                    ) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
            )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "已识别学期",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            Text(
                text = semester.label,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text =
                    if (
                        semester.startMillis != null
                    ) {
                        "开学日期：" +
                            formatSemesterDate(
                                semester.startMillis
                            )
                    } else {
                        "未读取到明确的开学日期，可在“我的”中手动确认"
                    },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun SyncSummaryCard(
    sourceCount: Int,
    parsedCount: Int,
    addedCount: Int,
    updatedCount: Int,
    unchangedCount: Int,
    localOnlyCount: Int,
    errorCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "同步分析",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            SummaryRow(
                name = "网页读取",
                value = "$sourceCount 条"
            )
            SummaryRow(
                name = "成功解析",
                value = "$parsedCount 条"
            )
            SummaryRow(
                name = "新增",
                value = "$addedCount 条"
            )
            SummaryRow(
                name = "信息有变化",
                value = "$updatedCount 条"
            )
            SummaryRow(
                name = "无变化",
                value = "$unchangedCount 条"
            )
            SummaryRow(
                name = "仅本地存在",
                value = "$localOnlyCount 条"
            )
            SummaryRow(
                name = "解析失败",
                value = "$errorCount 条"
            )
        }
    }
}


@Composable
private fun SummaryRow(
    name: String,
    value: String
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 4.dp
                ),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}


@Composable
private fun StrategySelector(
    selected: CourseSyncStrategy,
    onSelected: (CourseSyncStrategy) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected =
                selected == CourseSyncStrategy.SMART_MERGE,
            onClick = {
                onSelected(
                    CourseSyncStrategy.SMART_MERGE
                )
            },
            label = {
                Text("智能同步（推荐）")
            }
        )

        FilterChip(
            selected =
                selected == CourseSyncStrategy.ADD_ONLY,
            onClick = {
                onSelected(
                    CourseSyncStrategy.ADD_ONLY
                )
            },
            label = {
                Text("仅追加新课程")
            }
        )

        FilterChip(
            selected =
                selected == CourseSyncStrategy.FULL_REPLACE,
            onClick = {
                onSelected(
                    CourseSyncStrategy.FULL_REPLACE
                )
            },
            label = {
                Text("完全覆盖")
            }
        )
    }
}


@Composable
private fun StrategyExplanation(
    strategy: CourseSyncStrategy,
    localOnlyCount: Int,
    updatedCount: Int,
    addedCount: Int
) {
    val text =
        when (strategy) {
            CourseSyncStrategy.SMART_MERGE ->
                "更新同一时间段课程的教师、教室等变化，新增 $addedCount 条；" +
                    "保留 $localOnlyCount 条仅本地存在的课程。"

            CourseSyncStrategy.ADD_ONLY ->
                "只添加完全不存在的课程，不修改现有课程。适合你只想补课，不想动本地编辑内容时使用。"

            CourseSyncStrategy.FULL_REPLACE ->
                "以翱翔教务为唯一准则。会删除当前学期所有本地课程后重建，" +
                    "包括 $localOnlyCount 条仅本地存在的课程。"
        }

    val extra =
        if (
            strategy == CourseSyncStrategy.SMART_MERGE &&
            updatedCount > 0
        ) {
            " 本次检测到 $updatedCount 条课程信息有变化。"
        } else {
            ""
        }

    InfoCard(
        title =
            when (strategy) {
                CourseSyncStrategy.SMART_MERGE ->
                    "推荐：日常重新同步"

                CourseSyncStrategy.ADD_ONLY ->
                    "保守模式"

                CourseSyncStrategy.FULL_REPLACE ->
                    "严格跟随教务"
            },
        text = text + extra
    )
}


private enum class IncomingCourseStatus {
    NEW,
    UPDATED,
    UNCHANGED
}


@Composable
private fun CoursePreviewCard(
    course: DemoCourse,
    status: IncomingCourseStatus
) {
    val statusText =
        when (status) {
            IncomingCourseStatus.NEW ->
                "新增"

            IncomingCourseStatus.UPDATED ->
                "有变化"

            IncomingCourseStatus.UNCHANGED ->
                "无变化"
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    when (status) {
                        IncomingCourseStatus.NEW ->
                            course.color.copy(
                                alpha = 0.10f
                            )

                        IncomingCourseStatus.UPDATED ->
                            MaterialTheme.colorScheme.surfaceVariant

                        IncomingCourseStatus.UNCHANGED ->
                            MaterialTheme.colorScheme.surfaceVariant
                    }
            )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = course.name,
                    modifier = Modifier.weight(1f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            Text(
                text =
                    weekDayName(
                        course.day
                    ) +
                        " · 第" +
                        course.startSection +
                        "–" +
                        course.endSection +
                        "节",
                fontSize = 14.sp
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = course.weekDisplayText(),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text =
                    course.room +
                        " · " +
                        course.teacher,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun InfoCard(
    title: String,
    text: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = text,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun MessageCard(
    title: String,
    messages: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            messages.forEach {
                    message ->

                Text(
                    text = "• $message",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


private fun buildSyncPreview(
    existingCourses: List<DemoCourse>,
    incomingCourses: List<DemoCourse>
): CourseSyncPreview {

    val existingBySmartKey =
        existingCourses
            .groupBy {
                buildSmartCourseKey(it)
            }

    var addedCount = 0
    var updatedCount = 0
    var unchangedCount = 0

    incomingCourses.forEach {
            incoming ->

        val candidates =
            existingBySmartKey[
                buildSmartCourseKey(
                    incoming
                )
            ]
                .orEmpty()

        val exactMatch =
            candidates.any {
                buildExactCourseKey(it) ==
                    buildExactCourseKey(incoming)
            }

        when {
            exactMatch ->
                unchangedCount++

            candidates.isNotEmpty() ->
                updatedCount++

            else ->
                addedCount++
        }
    }

    val incomingSmartKeys =
        incomingCourses
            .map {
                buildSmartCourseKey(it)
            }
            .toSet()

    val localOnlyCourses =
        existingCourses.filter {
            buildSmartCourseKey(it) !in
                incomingSmartKeys
        }

    val existingSmartKeys =
        existingCourses
            .map {
                buildSmartCourseKey(it)
            }
            .toSet()

    val appendOnlyCourses =
        incomingCourses.filter {
            buildSmartCourseKey(it) !in
                existingSmartKeys
        }

    val smartMergedCourses =
        buildList {
            addAll(
                localOnlyCourses
            )

            addAll(
                incomingCourses
            )
        }

    return CourseSyncPreview(
        addedCount = addedCount,
        updatedCount = updatedCount,
        unchangedCount = unchangedCount,
        localOnlyCount = localOnlyCourses.size,
        appendOnlyCourses = appendOnlyCourses,
        smartMergedCourses = smartMergedCourses
    )
}


private fun resolveIncomingStatus(
    incoming: DemoCourse,
    existingCourses: List<DemoCourse>
): IncomingCourseStatus {

    val smartKey =
        buildSmartCourseKey(
            incoming
        )

    val candidates =
        existingCourses.filter {
            buildSmartCourseKey(it) ==
                smartKey
        }

    if (
        candidates.isEmpty()
    ) {
        return IncomingCourseStatus.NEW
    }

    val exact =
        candidates.any {
            buildExactCourseKey(it) ==
                buildExactCourseKey(incoming)
        }

    return if (exact) {
        IncomingCourseStatus.UNCHANGED
    } else {
        IncomingCourseStatus.UPDATED
    }
}


private fun buildSmartCourseKey(
    course: DemoCourse
): String {
    return listOf(
        course.name.trim().lowercase(),
        course.day.toString(),
        course.startSection.toString(),
        course.endSection.toString(),
        course.weekMode.name,
        course.startWeek.toString(),
        course.endWeek.toString(),
        canonicalCustomWeeks(
            course.customWeeks
        )
    ).joinToString(
        separator = "|"
    )
}


private fun buildExactCourseKey(
    course: DemoCourse
): String {
    return listOf(
        buildSmartCourseKey(course),
        course.teacher.trim().lowercase(),
        course.room.trim().lowercase()
    ).joinToString(
        separator = "|"
    )
}


private fun syncButtonText(
    strategy: CourseSyncStrategy,
    preview: CourseSyncPreview,
    incomingCount: Int
): String {
    return when (strategy) {
        CourseSyncStrategy.SMART_MERGE ->
            "执行智能同步"

        CourseSyncStrategy.ADD_ONLY ->
            if (
                preview.appendOnlyCourses.isEmpty()
            ) {
                "没有可追加的课程"
            } else {
                "追加 ${preview.appendOnlyCourses.size} 条课程"
            }

        CourseSyncStrategy.FULL_REPLACE ->
            "用 $incomingCount 条教务课程完全覆盖"
    }
}


private fun weekDayName(
    day: Int
): String {
    return when (day) {
        1 -> "星期一"
        2 -> "星期二"
        3 -> "星期三"
        4 -> "星期四"
        5 -> "星期五"
        6 -> "星期六"
        7 -> "星期日"
        else -> "未知"
    }
}

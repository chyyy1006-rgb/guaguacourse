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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.npucourse.model.DemoCourse
import com.example.npucourse.model.Semester
import com.example.npucourse.util.CampusType
import com.example.npucourse.util.campusDisplayName
import com.example.npucourse.util.formatSemesterDate
import com.example.npucourse.util.isYouYiSummerTime
import java.util.Calendar


@Composable
fun MinePage(
    semesterName: String,
    semesterStartMillis: Long,
    currentWeek: Int,
    campus: String,
    reminderMinutes: Int,
    semesters: List<Semester>,
    selectedSemesterId: Long,
    courseCountBySemester: Map<Long, Int>,
    taskCount: Int,
    selectedCourses: List<DemoCourse>,
    themeMode: String,
    accentStyle: String,
    dynamicColor: Boolean,
    uiDensity: String,
    appIconStyle: String,
    courseCardStyle: String,
    showSectionTimes: Boolean,
    onSemesterSelected: (Long) -> Unit,
    onCreateSemester: (String, Long, String) -> Unit,
    onRenameSemester: (Long, String) -> Unit,
    onDuplicateSemester: (Long, String) -> Unit,
    onDeleteSemester: (Long) -> Unit,
    onSemesterStartChange: (Long) -> Unit,
    onCampusChange: (String) -> Unit,
    onReminderMinutesChange: (Int) -> Unit,
    onThemeModeChange: (String) -> Unit,
    onAccentStyleChange: (String) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onUiDensityChange: (String) -> Unit,
    onAppIconStyleChange: (String) -> Unit,
    onCourseCardStyleChange: (String) -> Unit,
    onShowSectionTimesChange: (Boolean) -> Unit
) {

    val context =
        LocalContext.current

    var showCampusDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showReminderDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showSemesterManagement by rememberSaveable {
        mutableStateOf(false)
    }

    var showDataManagement by rememberSaveable {
        mutableStateOf(false)
    }

    var showAppearanceSettings by rememberSaveable {
        mutableStateOf(false)
    }

    var showShareExport by rememberSaveable {
        mutableStateOf(false)
    }

    var showWidgetSettings by rememberSaveable {
        mutableStateOf(false)
    }

    var showAboutUpdate by rememberSaveable {
        mutableStateOf(false)
    }

    var showFeedback by rememberSaveable {
        mutableStateOf(false)
    }


    if (
        showSemesterManagement
    ) {

        SemesterManagementPage(
            semesters =
                semesters,
            selectedSemesterId =
                selectedSemesterId,
            courseCountBySemester =
                courseCountBySemester,
            onBack = {
                showSemesterManagement =
                    false
            },
            onSemesterSelected =
                onSemesterSelected,
            onCreateSemester =
                onCreateSemester,
            onRenameSemester =
                onRenameSemester,
            onDuplicateSemester =
                onDuplicateSemester,
            onDeleteSemester =
                onDeleteSemester
        )

        return
    }


    if (
        showDataManagement
    ) {

        DataManagementPage(
            semesterCount =
                semesters.size,
            courseCount =
                courseCountBySemester
                    .values
                    .sum(),
            taskCount = taskCount,
            onBack = {
                showDataManagement =
                    false
            }
        )

        return
    }


    if (showAppearanceSettings) {
        AppearanceSettingsPage(
            themeMode = themeMode,
            accentStyle = accentStyle,
            dynamicColor = dynamicColor,
            uiDensity = uiDensity,
            appIconStyle = appIconStyle,
            courseCardStyle = courseCardStyle,
            showSectionTimes = showSectionTimes,
            onBack = { showAppearanceSettings = false },
            onThemeModeChange = onThemeModeChange,
            onAccentStyleChange = onAccentStyleChange,
            onDynamicColorChange = onDynamicColorChange,
            onUiDensityChange = onUiDensityChange,
            onAppIconStyleChange = onAppIconStyleChange,
            onCourseCardStyleChange = onCourseCardStyleChange,
            onShowSectionTimesChange = onShowSectionTimesChange
        )
        return
    }

    if (showShareExport) {
        ShareExportPage(
            semester = Semester(
                id = selectedSemesterId,
                name = semesterName,
                startMillis = semesterStartMillis,
                campus = campus
            ),
            courses = selectedCourses,
            currentWeek = currentWeek,
            onBack = { showShareExport = false }
        )
        return
    }

    if (showFeedback) {
        FeedbackPage(
            onBack = { showFeedback = false }
        )
        return
    }

    if (showAboutUpdate) {
        AboutUpdatePage(
            onBack = { showAboutUpdate = false }
        )
        return
    }

    if (showWidgetSettings) {
        WidgetSettingsPage(
            onBack = { showWidgetSettings = false }
        )
        return
    }

    val selectedDateCalendar =
        Calendar
            .getInstance()
            .apply {
                timeInMillis =
                    semesterStartMillis
            }

    val currentSemesterCourseCount =
        courseCountBySemester[
            selectedSemesterId
        ] ?: 0

    val campusSubtitle =
        if (
            campus ==
            CampusType.YOUYI
        ) {
            campusDisplayName(
                campus
            ) +
                if (
                    isYouYiSummerTime()
                ) {
                    " · 夏季作息"
                } else {
                    " · 冬季作息"
                }
        } else {
            campusDisplayName(
                campus
            )
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
                Modifier.height(22.dp)
        )

        Text(
            text =
                "我的",
            fontSize =
                30.sp,
            fontWeight =
                FontWeight.Bold,
            color =
                MaterialTheme.colorScheme.onBackground
        )

        Spacer(
            modifier =
                Modifier.height(6.dp)
        )

        Text(
            text =
                "课表、提醒与本地数据",
            fontSize =
                14.sp,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(
            modifier =
                Modifier.height(22.dp)
        )

        CurrentSemesterCard(
            semesterName =
                semesterName,
            semesterStartMillis =
                semesterStartMillis,
            currentWeek =
                currentWeek,
            courseCount =
                currentSemesterCourseCount,
            campus =
                campusSubtitle
        )

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        SectionTitle(
            "课表"
        )

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        SettingGroup {

            SettingRow(
                title =
                    "课表管理",
                subtitle =
                    "共 " +
                        semesters.size +
                        " 个课表",
                onClick = {
                    showSemesterManagement =
                        true
                }
            )

            GroupDivider()

            SettingRow(
                title =
                    "开学日期",
                subtitle =
                    formatSemesterDate(
                        semesterStartMillis
                    ),
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

                            onSemesterStartChange(
                                calendar.timeInMillis
                            )
                        },
                        selectedDateCalendar.get(
                            Calendar.YEAR
                        ),
                        selectedDateCalendar.get(
                            Calendar.MONTH
                        ),
                        selectedDateCalendar.get(
                            Calendar.DAY_OF_MONTH
                        )
                    ).show()
                }
            )

            GroupDivider()

            SettingRow(
                title =
                    "校区与作息",
                subtitle =
                    campusSubtitle,
                onClick = {
                    showCampusDialog =
                        true
                }
            )
        }

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        SectionTitle(
            "课程"
        )

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        SettingGroup {

            SettingRow(
                title =
                    "当前教学状态",
                subtitle =
                    currentWeekDisplayText(
                        currentWeek
                    )
            )

            GroupDivider()

            SettingRow(
                title =
                    "课程提醒",
                subtitle =
                    reminderDisplayText(
                        reminderMinutes
                    ),
                onClick = {
                    showReminderDialog =
                        true
                }
            )
        }

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        SectionTitle(
            "应用"
        )

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        SettingGroup {

            SettingRow(
                title = "导出与分享",
                subtitle = "课表图片 · ICS 日历",
                onClick = { showShareExport = true }
            )

            GroupDivider()

            SettingRow(
                title = "桌面小组件",
                subtitle = "今日课程 · 下一节课",
                onClick = { showWidgetSettings = true }
            )

            GroupDivider()

            SettingRow(
                title = "外观设置",
                subtitle = appearanceSummary(themeMode, uiDensity),
                onClick = { showAppearanceSettings = true }
            )

            GroupDivider()

            SettingRow(
                title =
                    "数据管理",
                subtitle =
                    "完整备份 · 恢复",
                onClick = {
                    showDataManagement =
                        true
                }
            )
        }

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        SectionTitle(
            "支持与关于"
        )

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        SettingGroup {
            SettingRow(
                title = "意见反馈",
                subtitle = "功能建议 · Bug · 导入与提醒问题",
                onClick = { showFeedback = true }
            )

            GroupDivider()

            SettingRow(
                title = "关于与更新",
                subtitle = "版本信息 · 检查更新 · GitHub 发布页",
                onClick = { showAboutUpdate = true }
            )
        }

        Spacer(
            modifier =
                Modifier.height(44.dp)
        )
    }


    if (
        showCampusDialog
    ) {

        CampusDialog(
            campus =
                campus,
            onDismiss = {
                showCampusDialog =
                    false
            },
            onSelect = {
                selectedCampus ->

                onCampusChange(
                    selectedCampus
                )

                showCampusDialog =
                    false
            }
        )
    }


    if (
        showReminderDialog
    ) {

        ReminderDialog(
            currentValue =
                reminderMinutes,
            onDismiss = {
                showReminderDialog =
                    false
            },
            onSelect = {
                minutes ->

                onReminderMinutesChange(
                    minutes
                )

                showReminderDialog =
                    false
            }
        )
    }
}


@Composable
private fun CurrentSemesterCard(
    semesterName: String,
    semesterStartMillis: Long,
    currentWeek: Int,
    courseCount: Int,
    campus: String
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(24.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.primaryContainer
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(20.dp)
        ) {

            Text(
                text =
                    "当前课表",
                fontSize =
                    13.sp,
                fontWeight =
                    FontWeight.SemiBold,
                color =
                    MaterialTheme.colorScheme.primary
            )

            Spacer(
                modifier =
                    Modifier.height(7.dp)
            )

            Text(
                text =
                    semesterName,
                fontSize =
                    21.sp,
                fontWeight =
                    FontWeight.Bold,
                color =
                    MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            Text(
                text =
                    currentWeekDisplayText(
                        currentWeek
                    ) +
                        " · " +
                        courseCount +
                        " 条课程",
                fontSize =
                    14.sp,
                color =
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
            )

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Text(
                text =
                    campus +
                        " · 开学 " +
                        formatSemesterDate(
                            semesterStartMillis
                        ),
                fontSize =
                    13.sp,
                color =
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.64f)
            )
        }
    }
}


@Composable
private fun SectionTitle(
    text: String
) {

    Text(
        text =
            text,
        modifier =
            Modifier.padding(
                horizontal = 4.dp
            ),
        fontSize =
            14.sp,
        fontWeight =
            FontWeight.SemiBold,
        color =
            MaterialTheme.colorScheme.onSurfaceVariant
    )
}


@Composable
private fun SettingGroup(
    content: @Composable () -> Unit
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
    ) {
        content()
    }
}


@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    enabled =
                        onClick != null
                ) {
                    onClick?.invoke()
                }
                .padding(
                    horizontal = 18.dp,
                    vertical = 15.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Column(
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                text =
                    title,
                fontSize =
                    16.sp,
                fontWeight =
                    FontWeight.SemiBold,
                color =
                    MaterialTheme.colorScheme.onSurface
            )

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Text(
                text =
                    subtitle,
                fontSize =
                    13.sp,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (
            onClick != null
        ) {
            Text(
                text =
                    "›",
                fontSize =
                    24.sp,
                color =
                    MaterialTheme.colorScheme.outline
            )
        }
    }
}


@Composable
private fun GroupDivider() {

    HorizontalDivider(
        modifier =
            Modifier.padding(
                start = 18.dp
            ),
        thickness =
            0.5.dp,
        color =
            MaterialTheme.colorScheme.surfaceVariant
    )
}


@Composable
private fun CampusDialog(
    campus: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {

    AlertDialog(
        onDismissRequest =
            onDismiss,
        title = {
            Text(
                "选择校区"
            )
        },
        text = {

            Column {

                CampusOption(
                    title =
                        "长安校区",
                    selected =
                        campus ==
                            CampusType.CHANGAN,
                    onClick = {
                        onSelect(
                            CampusType.CHANGAN
                        )
                    }
                )

                CampusOption(
                    title =
                        "友谊校区",
                    selected =
                        campus ==
                            CampusType.YOUYI,
                    onClick = {
                        onSelect(
                            CampusType.YOUYI
                        )
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick =
                    onDismiss
            ) {
                Text(
                    "取消"
                )
            }
        }
    )
}


@Composable
private fun CampusOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                }
                .padding(
                    vertical = 8.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        RadioButton(
            selected =
                selected,
            onClick =
                onClick
        )

        Text(
            text =
                title,
            fontSize =
                16.sp
        )
    }
}


@Composable
private fun ReminderDialog(
    currentValue: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {

    val options =
        listOf(
            -1,
            0,
            5,
            10,
            15,
            30
        )

    AlertDialog(
        onDismissRequest =
            onDismiss,
        title = {
            Text(
                "课程提醒"
            )
        },
        text = {

            Column {
                options.forEach {
                    minutes ->

                    ReminderOption(
                        minutes =
                            minutes,
                        selected =
                            currentValue ==
                                minutes,
                        onClick = {
                            onSelect(
                                minutes
                            )
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick =
                    onDismiss
            ) {
                Text(
                    "取消"
                )
            }
        }
    )
}


@Composable
private fun ReminderOption(
    minutes: Int,
    selected: Boolean,
    onClick: () -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                }
                .padding(
                    vertical = 6.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        RadioButton(
            selected =
                selected,
            onClick =
                onClick
        )

        Text(
            text =
                reminderOptionText(
                    minutes
                ),
            fontSize =
                16.sp
        )
    }
}


private fun appearanceSummary(
    themeMode: String,
    uiDensity: String
): String {
    val theme = when (themeMode) {
        "LIGHT" -> "浅色"
        "DARK" -> "深色"
        else -> "跟随系统"
    }
    val density = when (uiDensity) {
        "COMPACT" -> "紧凑"
        "COMFORTABLE" -> "舒适"
        else -> "标准"
    }
    return "$theme · $density"
}


private fun currentWeekDisplayText(
    currentWeek: Int
): String {

    return when {
        currentWeek == 0 ->
            "学期尚未开始"

        currentWeek > 20 ->
            "本学期已结束"

        else ->
            "当前第" +
                currentWeek +
                "周"
    }
}


private fun reminderDisplayText(
    minutes: Int
): String {

    return when {
        minutes < 0 ->
            "已关闭"

        minutes == 0 ->
            "上课时提醒"

        else ->
            "提前 " +
                minutes +
                " 分钟提醒"
    }
}


private fun reminderOptionText(
    minutes: Int
): String {

    return when {
        minutes < 0 ->
            "关闭提醒"

        minutes == 0 ->
            "上课时提醒"

        else ->
            "提前 " +
                minutes +
                " 分钟"
    }
}

package com.example.npucourse.ui.screens

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.npucourse.R
import com.example.npucourse.data.settings.AccentStyle
import com.example.npucourse.data.settings.AppIconStyle
import com.example.npucourse.data.settings.CourseCardStyle
import com.example.npucourse.data.settings.ThemeMode
import com.example.npucourse.data.settings.UiDensity

@Composable
fun AppearanceSettingsPage(
    themeMode: String,
    accentStyle: String,
    dynamicColor: Boolean,
    uiDensity: String,
    appIconStyle: String,
    courseCardStyle: String,
    showSectionTimes: Boolean,
    onBack: () -> Unit,
    onThemeModeChange: (String) -> Unit,
    onAccentStyleChange: (String) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onUiDensityChange: (String) -> Unit,
    onAppIconStyleChange: (String) -> Unit,
    onCourseCardStyleChange: (String) -> Unit,
    onShowSectionTimesChange: (Boolean) -> Unit
) {
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
            text = "外观设置",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "主题、桌面图标、强调色与课表显示密度",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        AppearanceCard(title = "主题") {
            ThemeOption(
                title = "跟随系统",
                subtitle = "自动跟随手机的浅色 / 深色模式",
                selected = themeMode == ThemeMode.SYSTEM,
                onClick = { onThemeModeChange(ThemeMode.SYSTEM) }
            )
            ThemeOption(
                title = "浅色",
                subtitle = "始终使用浅色界面",
                selected = themeMode == ThemeMode.LIGHT,
                onClick = { onThemeModeChange(ThemeMode.LIGHT) }
            )
            ThemeOption(
                title = "深色",
                subtitle = "始终使用深色界面",
                selected = themeMode == ThemeMode.DARK,
                onClick = { onThemeModeChange(ThemeMode.DARK) }
            )
        }

        Spacer(Modifier.height(16.dp))

        AppearanceCard(title = "桌面图标") {
            Text(
                text = "选择瓜瓜课程表在桌面显示的品牌图标。设置页展示的就是实际 Launcher 图标预览。",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(14.dp))

            AppIconRow(
                first = AppIconOptionData(
                    label = "西瓜日历",
                    style = AppIconStyle.WATERMELON,
                    iconRes = R.drawable.launcher_preview_watermelon
                ),
                second = AppIconOptionData(
                    label = "瓜瓜·课",
                    style = AppIconStyle.COURSE,
                    iconRes = R.drawable.launcher_preview_course
                ),
                selectedStyle = appIconStyle,
                onChange = onAppIconStyleChange
            )

            Spacer(Modifier.height(10.dp))

            AppIconRow(
                first = AppIconOptionData(
                    label = "清新日程",
                    style = AppIconStyle.PLANNER,
                    iconRes = R.drawable.launcher_preview_planner
                ),
                second = AppIconOptionData(
                    label = "小瓜助手",
                    style = AppIconStyle.MASCOT,
                    iconRes = R.drawable.launcher_preview_mascot
                ),
                selectedStyle = appIconStyle,
                onChange = onAppIconStyleChange
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "说明：Android 主应用图标需要预置在安装包中。旧版的彩色图标会在升级后自动迁移，不影响桌面入口。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(16.dp))

        AppearanceCard(title = "强调色") {
            Text(
                text = "用于底部导航、按钮、高亮日期等界面元素。课程自身颜色不会被覆盖。",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AccentOption("靛紫", AccentStyle.INDIGO, Color(0xFF5B5BD6), accentStyle, onAccentStyleChange)
                AccentOption("蓝", AccentStyle.BLUE, Color(0xFF3768D8), accentStyle, onAccentStyleChange)
                AccentOption("绿", AccentStyle.GREEN, Color(0xFF26765E), accentStyle, onAccentStyleChange)
                AccentOption("玫瑰", AccentStyle.ROSE, Color(0xFF9A405F), accentStyle, onAccentStyleChange)
                AccentOption("橙", AccentStyle.ORANGE, Color(0xFF9A4F16), accentStyle, onAccentStyleChange)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "系统动态配色",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "开启后优先使用 Android 系统壁纸配色",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = dynamicColor,
                        onCheckedChange = onDynamicColorChange
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        AppearanceCard(title = "课表密度") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DensityChip(
                    label = "紧凑",
                    selected = uiDensity == UiDensity.COMPACT,
                    onClick = { onUiDensityChange(UiDensity.COMPACT) }
                )
                DensityChip(
                    label = "标准",
                    selected = uiDensity == UiDensity.STANDARD,
                    onClick = { onUiDensityChange(UiDensity.STANDARD) }
                )
                DensityChip(
                    label = "舒适",
                    selected = uiDensity == UiDensity.COMFORTABLE,
                    onClick = { onUiDensityChange(UiDensity.COMFORTABLE) }
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = when (uiDensity) {
                    UiDensity.COMPACT -> "减少每节课的高度，一屏可以看到更多节次。"
                    UiDensity.COMFORTABLE -> "增加每节课的高度，课程卡内容更容易阅读。"
                    else -> "平衡信息密度与可读性。"
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(16.dp))

        AppearanceCard(title = "课表内容") {
            Text(
                text = "课程卡信息",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DensityChip(
                    label = "简洁",
                    selected = courseCardStyle == CourseCardStyle.MINIMAL,
                    onClick = {
                        onCourseCardStyleChange(CourseCardStyle.MINIMAL)
                    }
                )
                DensityChip(
                    label = "标准",
                    selected = courseCardStyle == CourseCardStyle.STANDARD,
                    onClick = {
                        onCourseCardStyleChange(CourseCardStyle.STANDARD)
                    }
                )
                DensityChip(
                    label = "详细",
                    selected = courseCardStyle == CourseCardStyle.DETAILED,
                    onClick = {
                        onCourseCardStyleChange(CourseCardStyle.DETAILED)
                    }
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = when (courseCardStyle) {
                    CourseCardStyle.MINIMAL -> "只显示课程名称，适合课程较多或小屏手机。"
                    CourseCardStyle.DETAILED -> "显示课程名称、教室和教师。"
                    else -> "显示课程名称和教室。"
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "显示节次时间",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "在左侧节次编号下显示 08:30、09:25 等时间。",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = showSectionTimes,
                    onCheckedChange = onShowSectionTimesChange
                )
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

private data class AppIconOptionData(
    val label: String,
    val style: String,
    val iconRes: Int
)

@Composable
private fun AppIconRow(
    first: AppIconOptionData,
    second: AppIconOptionData,
    selectedStyle: String,
    onChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppIconOption(
            option = first,
            selected = selectedStyle == first.style,
            onClick = { onChange(first.style) }
        )
        AppIconOption(
            option = second,
            selected = selectedStyle == second.style,
            onClick = { onChange(second.style) }
        )
    }
}

@Composable
private fun RowScope.AppIconOption(
    option: AppIconOptionData,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor =
        if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LauncherIconPreview(
                iconRes = option.iconRes,
                description = option.label
            )

            Column(Modifier.weight(1f)) {
                Text(
                    text = option.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (selected) "正在使用" else "点击切换",
                    fontSize = 12.sp,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun LauncherIconPreview(
    iconRes: Int,
    description: String
) {
    Image(
        painter = painterResource(iconRes),
        contentDescription = description,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .height(52.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
    )
}

@Composable
private fun AppearanceCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ThemeOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column {
            Text(
                text = title,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RowScope.AccentOption(
    label: String,
    value: String,
    color: Color,
    selectedValue: String,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect(value) }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .height(34.dp)
                .fillMaxWidth(),
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = color),
            border = if (selectedValue == value) {
                androidx.compose.foundation.BorderStroke(
                    3.dp,
                    MaterialTheme.colorScheme.onSurface
                )
            } else {
                null
            }
        ) {}
        Spacer(Modifier.height(5.dp))
        Text(
            label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RowScope.DensityChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        modifier = Modifier.weight(1f),
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

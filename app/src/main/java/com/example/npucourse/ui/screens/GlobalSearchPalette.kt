package com.example.npucourse.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.npucourse.data.TaskEntity
import com.example.npucourse.importer.NwpuExamRecord
import com.example.npucourse.model.DemoCourse

object GlobalSearchDestination {
    const val TIMETABLE = "timetable"
    const val TASKS = "tasks"
    const val ACADEMIC = "academic"
    const val OVERLAY = "overlay"
    const val WIDGET = "widget"
    const val BACKUP = "backup"
    const val EXPORT = "export"
    const val APPEARANCE = "appearance"
    const val MINE = "mine"
}

private data class PaletteResult(
    val key: String,
    val type: String,
    val title: String,
    val subtitle: String,
    val searchable: String,
    val destination: String
)

@Composable
fun GlobalSearchPalette(
    courses: List<DemoCourse>,
    tasks: List<TaskEntity>,
    exams: List<NwpuExamRecord>,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val courseNames = remember(courses) { courses.associate { it.id to it.name } }
    val staticResults = remember {
        listOf(
            PaletteResult("add-course", "命令", "新增课程", "打开课表并新增课程", "新增课程 添加课程 课表", GlobalSearchDestination.TIMETABLE),
            PaletteResult("add-task", "命令", "新增待办", "打开 DDL / 待办", "新增待办 添加ddl 作业", GlobalSearchDestination.TASKS),
            PaletteResult("exam", "功能", "查看考试", "考试安排与成绩", "考试 成绩 学业", GlobalSearchDestination.ACADEMIC),
            PaletteResult("export", "功能", "导出 ICS / 分享课表", "导出与分享", "导出 ics 日历 分享", GlobalSearchDestination.EXPORT),
            PaletteResult("backup", "功能", "完整备份与恢复", "数据管理", "备份 恢复 数据 导入 导出", GlobalSearchDestination.BACKUP),
            PaletteResult("overlay", "设置", "快捷悬浮窗", "五种手势动作与防误触", "悬浮窗 快捷 手势 双击", GlobalSearchDestination.OVERLAY),
            PaletteResult("widget", "设置", "桌面小组件", "今日课程与待办组件", "小组件 桌面 widget", GlobalSearchDestination.WIDGET),
            PaletteResult("appearance", "设置", "外观", "主题、动态配色与课程卡", "外观 主题 深色 动态配色", GlobalSearchDestination.APPEARANCE)
        )
    }
    val results = remember(query, courses, tasks, exams) {
        if (query.isBlank()) staticResults.take(6) else buildList {
            courses.distinctBy { it.id }.forEach { course ->
                val text = "${course.name} ${course.teacher} ${course.room} ${course.notes}"
                if (text.fuzzyContains(query)) add(PaletteResult("course-${course.id}", "课程", course.name,
                    listOf(course.teacher, course.room).filter { it.isNotBlank() }.joinToString(" · "), text, GlobalSearchDestination.TIMETABLE))
            }
            tasks.forEach { task ->
                val courseName = task.courseId?.let(courseNames::get).orEmpty()
                val text = "${task.title} ${task.note} $courseName"
                if (text.fuzzyContains(query)) add(PaletteResult("task-${task.id}", "待办", task.title, courseName, text, GlobalSearchDestination.TASKS))
            }
            exams.forEachIndexed { index, exam ->
                val text = "${exam.courseName} ${exam.timeText} ${exam.location} ${exam.status}"
                if (text.fuzzyContains(query)) add(PaletteResult("exam-$index", "考试", exam.courseName,
                    "${exam.timeText} · ${exam.location}", text, GlobalSearchDestination.ACADEMIC))
            }
            addAll(staticResults.filter { "${it.title} ${it.subtitle} ${it.searchable}".fuzzyContains(query) })
        }.take(60)
    }

    BackHandler(onBack = onDismiss)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // 搜索结果属于高密度阅读界面，必须使用实色承载；如果沿用通透玻璃，
        // Today 页面和 BottomBar 会穿透进文字区域，严重降低对比度。
        Surface(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 12.dp
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("全局搜索", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("课程、教师、教室、DDL、考试或功能") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                if (results.isEmpty()) {
                    Text("没有找到匹配结果", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(18.dp))
                } else LazyColumn(Modifier.fillMaxSize()) {
                    items(results, key = { it.key }) { result ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                onDismiss()
                                onNavigate(result.destination)
                            }.padding(vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(result.type, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(result.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (result.subtitle.isNotBlank()) Text(result.subtitle, fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text("›", fontSize = 22.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .12f))
                    }
                }
            }
        }
    }
}

private fun String.fuzzyContains(rawQuery: String): Boolean {
    val source = lowercase().filterNot(Char::isWhitespace)
    val query = rawQuery.lowercase().filterNot(Char::isWhitespace)
    if (query.isBlank() || source.contains(query)) return true
    var index = 0
    source.forEach { if (index < query.length && it == query[index]) index++ }
    return index == query.length
}

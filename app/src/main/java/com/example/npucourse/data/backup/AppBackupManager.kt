package com.example.npucourse.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.npucourse.data.AppDatabase
import com.example.npucourse.data.CourseEntity
import com.example.npucourse.data.SemesterEntity
import com.example.npucourse.data.TaskEntity
import com.example.npucourse.data.settings.AppSettings
import com.example.npucourse.data.settings.SettingsRepository
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject


data class BackupPreview(
    val semesterCount: Int,
    val courseCount: Int,
    val exportedAt: Long,
    val activeSemesterId: Long,
    val taskCount: Int = 0
)


class AppBackupManager(
    context: Context
) {

    private val appContext =
        context.applicationContext

    private val database =
        AppDatabase.getInstance(
            appContext
        )

    private val settingsRepository =
        SettingsRepository(
            appContext
        )


    suspend fun exportTo(
        uri: Uri
    ): BackupPreview =
        withContext(Dispatchers.IO) {

            val semesters =
                database
                    .semesterDao()
                    .getAllSemestersOnce()

            val courses =
                database
                    .courseDao()
                    .getAllCoursesOnce()

            val tasks =
                database
                    .taskDao()
                    .getAllTasksOnce()

            val settings =
                settingsRepository
                    .settings
                    .first()

            val exportedAt =
                System.currentTimeMillis()

            val root =
                JSONObject()
                    .put(
                        "schemaVersion",
                        4
                    )
                    .put(
                        "app",
                        "瓜瓜课程表"
                    )
                    .put(
                        "exportedAt",
                        exportedAt
                    )
                    .put(
                        "settings",
                        settingsToJson(
                            settings
                        )
                    )
                    .put(
                        "semesters",
                        semestersToJson(
                            semesters
                        )
                    )
                    .put(
                        "courses",
                        coursesToJson(
                            courses
                        )
                    )
                    .put(
                        "tasks",
                        tasksToJson(
                            tasks
                        )
                    )

            val outputStream =
                appContext
                    .contentResolver
                    .openOutputStream(
                        uri,
                        "w"
                    )
                    ?: error(
                        "无法打开备份文件"
                    )

            outputStream
                .bufferedWriter()
                .use { writer ->
                    writer.write(
                        root.toString(2)
                    )
                }

            BackupPreview(
                semesterCount =
                    semesters.size,
                courseCount =
                    courses.size,
                exportedAt =
                    exportedAt,
                activeSemesterId =
                    settings.activeSemesterId,
                taskCount =
                    tasks.size
            )
        }


    suspend fun preview(
        uri: Uri
    ): BackupPreview =
        withContext(Dispatchers.IO) {

            val payload =
                readPayload(
                    uri
                )

            payload.preview
        }


    suspend fun restoreFrom(
        uri: Uri
    ): BackupPreview =
        withContext(Dispatchers.IO) {

            val payload =
                readPayload(
                    uri
                )

            database.withTransaction {

                database
                    .taskDao()
                    .deleteAllTasks()

                database
                    .courseDao()
                    .deleteAllCourses()

                database
                    .semesterDao()
                    .deleteAllSemesters()

                database
                    .semesterDao()
                    .insertSemestersForRestore(
                        payload.semesters
                    )

                if (
                    payload.courses
                        .isNotEmpty()
                ) {
                    database
                        .courseDao()
                        .insertCourses(
                            payload.courses
                        )
                }

                if (payload.tasks.isNotEmpty()) {
                    database
                        .taskDao()
                        .insertTasks(payload.tasks)
                }
            }

            settingsRepository
                .restoreSettings(
                    payload.settings
                )

            payload.preview
        }


    private fun readPayload(
        uri: Uri
    ): BackupPayload {

        val inputStream =
            appContext
                .contentResolver
                .openInputStream(
                    uri
                )
                ?: error(
                    "无法读取备份文件"
                )

        val text =
            BufferedReader(
                InputStreamReader(
                    inputStream,
                    Charsets.UTF_8
                )
            ).use {
                reader ->

                reader.readText()
            }

        val root =
            JSONObject(
                text
            )

        val schemaVersion =
            root.optInt(
                "schemaVersion",
                -1
            )

        if (
            schemaVersion !in 1..4
        ) {
            error(
                "不支持的备份版本：$schemaVersion"
            )
        }

        val semesters =
            parseSemesters(
                root.getJSONArray(
                    "semesters"
                )
            )

        if (
            semesters.isEmpty()
        ) {
            error(
                "备份中没有课表，无法恢复"
            )
        }

        val semesterIds =
            semesters
                .map {
                    it.id
                }
                .toSet()

        val courses =
            parseCourses(
                root.optJSONArray(
                    "courses"
                ) ?: JSONArray()
            )

        val invalidCourse =
            courses.firstOrNull {
                it.semesterId !in
                    semesterIds
            }

        if (
            invalidCourse != null
        ) {
            error(
                "备份中的课程与课表关系不完整"
            )
        }

        val tasks =
            if (schemaVersion >= 2) {
                parseTasks(
                    root.optJSONArray("tasks") ?: JSONArray()
                )
            } else {
                emptyList()
            }

        val courseIds = courses.map { it.id }.toSet()
        val invalidTask = tasks.firstOrNull { task ->
            task.semesterId !in semesterIds ||
                (task.courseId != null && task.courseId !in courseIds)
        }
        if (invalidTask != null) {
            error("备份中的待办与课表/课程关系不完整")
        }

        val settings =
            parseSettings(
                root.optJSONObject(
                    "settings"
                ) ?: JSONObject()
            )

        val normalizedActiveSemesterId =
            if (
                settings.activeSemesterId in
                semesterIds
            ) {
                settings.activeSemesterId
            } else {
                semesters.first().id
            }

        val normalizedSettings =
            settings.copy(
                activeSemesterId =
                    normalizedActiveSemesterId
            )

        val exportedAt =
            root.optLong(
                "exportedAt",
                0L
            )

        return BackupPayload(
            semesters =
                semesters,
            courses =
                courses,
            tasks =
                tasks,
            settings =
                normalizedSettings,
            preview =
                BackupPreview(
                    semesterCount =
                        semesters.size,
                    courseCount =
                        courses.size,
                    exportedAt =
                        exportedAt,
                    activeSemesterId =
                        normalizedActiveSemesterId,
                    taskCount =
                        tasks.size
                )
        )
    }


    private fun semestersToJson(
        semesters: List<SemesterEntity>
    ): JSONArray {

        val array =
            JSONArray()

        semesters.forEach {
            semester ->

            array.put(
                JSONObject()
                    .put(
                        "id",
                        semester.id
                    )
                    .put(
                        "name",
                        semester.name
                    )
                    .put(
                        "startMillis",
                        semester.startMillis
                    )
                    .put(
                        "campus",
                        semester.campus
                    )
                    .put(
                        "createdAt",
                        semester.createdAt
                    )
            )
        }

        return array
    }


    private fun coursesToJson(
        courses: List<CourseEntity>
    ): JSONArray {

        val array =
            JSONArray()

        courses.forEach {
            course ->

            array.put(
                JSONObject()
                    .put(
                        "id",
                        course.id
                    )
                    .put(
                        "name",
                        course.name
                    )
                    .put(
                        "room",
                        course.room
                    )
                    .put(
                        "teacher",
                        course.teacher
                    )
                    .put(
                        "day",
                        course.day
                    )
                    .put(
                        "startSection",
                        course.startSection
                    )
                    .put(
                        "endSection",
                        course.endSection
                    )
                    .put(
                        "startWeek",
                        course.startWeek
                    )
                    .put(
                        "endWeek",
                        course.endWeek
                    )
                    .put(
                        "colorArgb",
                        course.colorArgb
                    )
                    .put(
                        "weekMode",
                        course.weekMode
                    )
                    .put(
                        "customWeeks",
                        course.customWeeks
                    )
                    .put(
                        "semesterId",
                        course.semesterId
                    )
                    .put("notes", course.notes)
                    .put("reminderEnabled", course.reminderEnabled)
                    .put("reminderMinutesOverride", course.reminderMinutesOverride)
            )
        }

        return array
    }


    private fun tasksToJson(
        tasks: List<TaskEntity>
    ): JSONArray {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(
                JSONObject()
                    .put("id", task.id)
                    .put("semesterId", task.semesterId)
                    .put("courseId", task.courseId ?: JSONObject.NULL)
                    .put("title", task.title)
                    .put("note", task.note)
                    .put("dueAt", task.dueAt)
                    .put("reminderMinutesBefore", task.reminderMinutesBefore)
                    .put("priority", task.priority)
                    .put("completed", task.completed)
                    .put("createdAt", task.createdAt)
                    .put("updatedAt", task.updatedAt)
            )
        }
        return array
    }


    private fun settingsToJson(
        settings: AppSettings
    ): JSONObject {

        return JSONObject()
            .put(
                "semesterStartMillis",
                settings.semesterStartMillis
            )
            .put(
                "campus",
                settings.campus
            )
            .put(
                "reminderMinutes",
                settings.reminderMinutes
            )
            .put(
                "showWeekends",
                settings.showWeekends
            )
            .put(
                "activeSemesterId",
                settings.activeSemesterId
            )
            .put(
                "themeMode",
                settings.themeMode
            )
            .put(
                "accentStyle",
                settings.accentStyle
            )
            .put(
                "dynamicColor",
                settings.dynamicColor
            )
            .put(
                "uiDensity",
                settings.uiDensity
            )
            .put(
                "appIconStyle",
                settings.appIconStyle
            )
            .put(
                "courseCardStyle",
                settings.courseCardStyle
            )
            .put(
                "showSectionTimes",
                settings.showSectionTimes
            )
    }


    private fun parseSemesters(
        array: JSONArray
    ): List<SemesterEntity> {

        val result =
            mutableListOf<SemesterEntity>()

        for (
            index in 0 until
                array.length()
        ) {

            val item =
                array.getJSONObject(
                    index
                )

            val id =
                item.getLong(
                    "id"
                )

            if (
                id <= 0L
            ) {
                error(
                    "备份中的课表 ID 无效"
                )
            }

            result.add(
                SemesterEntity(
                    id = id,
                    name =
                        item.getString(
                            "name"
                        ),
                    startMillis =
                        item.getLong(
                            "startMillis"
                        ),
                    campus =
                        item.getString(
                            "campus"
                        ),
                    createdAt =
                        item.optLong(
                            "createdAt",
                            System.currentTimeMillis()
                        )
                )
            )
        }

        return result
    }


    private fun parseCourses(
        array: JSONArray
    ): List<CourseEntity> {

        val result =
            mutableListOf<CourseEntity>()

        for (
            index in 0 until
                array.length()
        ) {

            val item =
                array.getJSONObject(
                    index
                )

            result.add(
                CourseEntity(
                    id =
                        item.getLong(
                            "id"
                        ),
                    name =
                        item.getString(
                            "name"
                        ),
                    room =
                        item.optString(
                            "room",
                            ""
                        ),
                    teacher =
                        item.optString(
                            "teacher",
                            ""
                        ),
                    day =
                        item.getInt(
                            "day"
                        ),
                    startSection =
                        item.getInt(
                            "startSection"
                        ),
                    endSection =
                        item.getInt(
                            "endSection"
                        ),
                    startWeek =
                        item.getInt(
                            "startWeek"
                        ),
                    endWeek =
                        item.getInt(
                            "endWeek"
                        ),
                    colorArgb =
                        item.getInt(
                            "colorArgb"
                        ),
                    weekMode =
                        item.optString(
                            "weekMode",
                            "EVERY"
                        ),
                    customWeeks =
                        item.optString(
                            "customWeeks",
                            ""
                        ),
                    semesterId =
                        item.getLong(
                            "semesterId"
                        ),
                    notes = item.optString("notes", ""),
                    reminderEnabled = item.optBoolean("reminderEnabled", true),
                    reminderMinutesOverride = item.optInt("reminderMinutesOverride", -1)
                )
            )
        }

        return result
    }


    private fun parseTasks(
        array: JSONArray
    ): List<TaskEntity> {
        val result = mutableListOf<TaskEntity>()
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val courseId = if (item.isNull("courseId")) {
                null
            } else {
                item.optLong("courseId", 0L).takeIf { it > 0L }
            }
            result.add(
                TaskEntity(
                    id = item.getLong("id"),
                    semesterId = item.getLong("semesterId"),
                    courseId = courseId,
                    title = item.getString("title"),
                    note = item.optString("note", ""),
                    dueAt = item.optLong("dueAt", 0L),
                    reminderMinutesBefore = item.optInt("reminderMinutesBefore", -1).coerceAtLeast(-1).let { if (it == 1) -1 else it },
                    priority = item.optInt("priority", 1).coerceIn(0, 2),
                    completed = item.optBoolean("completed", false),
                    createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = item.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }
        return result
    }


    private fun parseSettings(
        item: JSONObject
    ): AppSettings {

        return AppSettings(
            semesterStartMillis =
                item.optLong(
                    "semesterStartMillis",
                    System.currentTimeMillis()
                ),
            campus =
                item.optString(
                    "campus",
                    "CHANGAN"
                ),
            reminderMinutes =
                item.optInt(
                    "reminderMinutes",
                    10
                ),
            showWeekends =
                item.optBoolean(
                    "showWeekends",
                    true
                ),
            activeSemesterId =
                item.optLong(
                    "activeSemesterId",
                    0L
                ),
            themeMode =
                item.optString(
                    "themeMode",
                    "SYSTEM"
                ),
            accentStyle =
                item.optString(
                    "accentStyle",
                    "INDIGO"
                ),
            dynamicColor =
                item.optBoolean(
                    "dynamicColor",
                    false
                ),
            uiDensity =
                item.optString(
                    "uiDensity",
                    "STANDARD"
                ),
            appIconStyle =
                com.example.npucourse.data.settings.AppIconStyle.normalize(
                    item.optString(
                        "appIconStyle",
                        com.example.npucourse.data.settings.AppIconStyle.WATERMELON
                    )
                ),
            courseCardStyle =
                item.optString(
                    "courseCardStyle",
                    "STANDARD"
                ),
            showSectionTimes =
                item.optBoolean(
                    "showSectionTimes",
                    true
                )
        )
    }


    private data class BackupPayload(
        val semesters: List<SemesterEntity>,
        val courses: List<CourseEntity>,
        val tasks: List<TaskEntity>,
        val settings: AppSettings,
        val preview: BackupPreview
    )
}

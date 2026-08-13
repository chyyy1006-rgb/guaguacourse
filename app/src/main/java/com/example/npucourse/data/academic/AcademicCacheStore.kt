package com.example.npucourse.data.academic

import android.content.Context
import com.example.npucourse.importer.NwpuAcademicExtractor
import com.example.npucourse.importer.NwpuExamQueryResult
import com.example.npucourse.importer.NwpuExamRecord
import com.example.npucourse.importer.NwpuGradeQueryResult
import org.json.JSONArray
import org.json.JSONObject

/**
 * 考试/成绩的轻量本地缓存。
 *
 * 这里故意不放进 Room：学业数据是教务系统的只读镜像，整包 JSON 缓存更便于
 * 在接口字段变化时保持向后兼容，也不会影响现有课表数据库迁移。
 */
object AcademicCacheStore {
    private const val PREFS = "academic_cache_v413"
    private const val KEY_GRADES = "grades_payload"
    private const val KEY_EXAMS = "exams_payload"
    private const val KEY_GRADES_UPDATED_AT = "grades_updated_at"
    private const val KEY_EXAMS_UPDATED_AT = "exams_updated_at"
    private const val KEY_QUERY_COUNT = "query_count"

    data class CachedGrades(
        val result: NwpuGradeQueryResult,
        val updatedAt: Long
    )

    data class CachedExams(
        val result: NwpuExamQueryResult,
        val updatedAt: Long
    )

    data class GradeChange(
        val courseName: String,
        val semesterName: String,
        val grade: String,
        val gradePoint: Double?,
        val isNew: Boolean
    )

    fun loadGrades(context: Context): CachedGrades? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val payload = prefs.getString(KEY_GRADES, null) ?: return null
        val result = NwpuAcademicExtractor.parseGradePayload(payload).getOrNull() ?: return null
        return CachedGrades(result, prefs.getLong(KEY_GRADES_UPDATED_AT, 0L))
    }

    fun loadExams(context: Context): CachedExams? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val payload = prefs.getString(KEY_EXAMS, null) ?: return null
        val result = NwpuAcademicExtractor.parseExamPayload(payload).getOrNull() ?: return null
        return CachedExams(result, prefs.getLong(KEY_EXAMS_UPDATED_AT, 0L))
    }

    /** 保存成绩并返回与上次缓存相比新增/变化的课程。首次建立缓存不产生提醒。 */
    fun saveGrades(
        context: Context,
        result: NwpuGradeQueryResult,
        now: Long = System.currentTimeMillis()
    ): List<GradeChange> {
        val old = loadGrades(context)?.result
        val changes = if (old == null) {
            emptyList()
        } else {
            val oldMap = old.grades.associateBy { gradeKey(it.courseCode, it.courseName, it.semesterId) }
            result.grades.mapNotNull { current ->
                if (current.grade.isBlank() || current.grade == "-") return@mapNotNull null
                val previous = oldMap[gradeKey(current.courseCode, current.courseName, current.semesterId)]
                val changed = previous == null ||
                    previous.grade != current.grade ||
                    previous.gradePoint != current.gradePoint
                if (!changed) return@mapNotNull null
                GradeChange(
                    courseName = current.courseName,
                    semesterName = current.semesterName,
                    grade = current.grade,
                    gradePoint = current.gradePoint,
                    isNew = previous == null
                )
            }
        }

        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_GRADES, encodeGrades(result))
            .putLong(KEY_GRADES_UPDATED_AT, now)
            .putInt(KEY_QUERY_COUNT, queryCount(context) + 1)
            .apply()
        return changes
    }

    fun saveExams(
        context: Context,
        result: NwpuExamQueryResult,
        now: Long = System.currentTimeMillis()
    ) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_EXAMS, encodeExams(result))
            .putLong(KEY_EXAMS_UPDATED_AT, now)
            .putInt(KEY_QUERY_COUNT, queryCount(context) + 1)
            .apply()
    }

    fun queryCount(context: Context): Int =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_QUERY_COUNT, 0)

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    fun nextExam(context: Context, now: Long = System.currentTimeMillis()): NwpuExamRecord? =
        loadExams(context)?.result?.exams
            ?.asSequence()
            ?.filter { !it.finished }
            ?.map { it to AcademicTimeParser.parseStartMillis(it.timeText) }
            ?.filter { (_, start) -> start == null || start >= now - 30 * 60_000L }
            ?.sortedWith(compareBy<Pair<NwpuExamRecord, Long?>> { it.second == null }
                .thenBy { it.second ?: Long.MAX_VALUE })
            ?.map { it.first }
            ?.firstOrNull()

    private fun gradeKey(code: String, name: String, semesterId: Long): String =
        "${code.ifBlank { name }}|$semesterId"

    private fun encodeGrades(result: NwpuGradeQueryResult): String {
        val root = JSONObject()
            .put("ok", true)
            .put("studentId", result.studentId)
            .putNullable("gpa", result.gpa)
            .putNullable("rank", result.rank)
            .putNullable("beforeRankGpa", result.beforeRankGpa)
            .putNullable("afterRankGpa", result.afterRankGpa)

        val semesters = JSONArray()
        result.semesters.forEach { semester ->
            semesters.put(JSONObject().put("id", semester.id).put("name", semester.name))
        }
        root.put("semesters", semesters)

        val grades = JSONArray()
        result.grades.forEach { grade ->
            grades.put(
                JSONObject()
                    .put("courseId", grade.courseId)
                    .put("courseCode", grade.courseCode)
                    .put("courseName", grade.courseName)
                    .putNullable("credits", grade.credits)
                    .put("grade", grade.grade)
                    .putNullable("gradePoint", grade.gradePoint)
                    .put("semesterId", grade.semesterId)
                    .put("semesterName", grade.semesterName)
                    .putNullable("obligatory", grade.obligatory)
                    .putNullable("classRank", grade.classRank)
            )
        }
        root.put("grades", grades)
        root.put("warnings", JSONArray(result.warnings))
        return root.toString()
    }

    private fun encodeExams(result: NwpuExamQueryResult): String {
        val exams = JSONArray()
        result.exams.forEach { exam ->
            exams.put(
                JSONObject()
                    .put("courseName", exam.courseName)
                    .put("timeText", exam.timeText)
                    .put("location", exam.location)
                    .put("status", exam.status)
                    .put("finished", exam.finished)
            )
        }
        return JSONObject()
            .put("ok", true)
            .put("exams", exams)
            .put("source", result.source)
            .put("warnings", JSONArray(result.warnings))
            .toString()
    }

    private fun JSONObject.putNullable(key: String, value: Any?): JSONObject =
        put(key, value ?: JSONObject.NULL)
}

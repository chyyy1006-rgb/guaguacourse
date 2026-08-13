package com.example.npucourse.importer

import android.text.Html
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import org.json.JSONObject

/**
 * 使用 WebView 已建立的 Cookie 直接请求翱翔教务。
 *
 * 登录仍由 WebView/CAS 完成；登录后查询无需等待页面渲染和 JS bridge，显著降低
 * 成绩页重复查询的延迟。接口异常时 UI 仍可回退到 NwpuAcademicExtractor 的 JS 方案。
 */
object NwpuAcademicDirectClient {
    private const val BASE = "https://jwxt.nwpu.edu.cn"
    private const val DEFAULT_UA =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36 GuaguaCourse/4.13"

    fun queryGrades(cookie: String, userAgent: String = DEFAULT_UA): Result<NwpuGradeQueryResult> =
        runCatching {
            require(cookie.isNotBlank()) { "没有可用的翱翔教务登录 Cookie，请先登录" }

            val info = fetchJson(
                "/student/for-std/student-portrait/getStdInfo?bizTypeAssoc=2&cultivateTypeAssoc=1",
                cookie,
                userAgent
            )
            val studentId = info.optJSONObject("student")?.opt("id")?.toString()?.takeIf { it.isNotBlank() }
                ?: error("未能从教务系统获取学生身份 ID")

            val pool = Executors.newFixedThreadPool(3)
            val gpaFuture = pool.submit<JSONObject> {
                fetchJson("/student/for-std/student-portrait/getMyGpa?studentAssoc=${urlEncode(studentId)}", cookie, userAgent)
            }
            val semesterFuture = pool.submit<JSONObject> {
                fetchJson(
                    "/student/for-std/student-portrait/getMyGrades?studentAssoc=${urlEncode(studentId)}&semesterAssoc=",
                    cookie,
                    userAgent
                )
            }
            val rankFuture = pool.submit<JSONObject?> {
                runCatching {
                    fetchJson(
                        "/student/for-std/student-portrait/getMyGradesByProgram?studentAssoc=${urlEncode(studentId)}",
                        cookie,
                        userAgent
                    )
                }.getOrNull()
            }

            val gpaData = gpaFuture.get()
            val semesterData = semesterFuture.get()
            val rankData = rankFuture.get()
            pool.shutdown()

            val semesters = buildList {
                val array = semesterData.optJSONArray("semesters")
                if (array != null) {
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        val id = item.optLong("id", 0L)
                        val name = item.optString("nameZh", item.optString("name", "")).trim()
                        if (id > 0 && name.isNotBlank()) add(NwpuAcademicSemester(id, name))
                    }
                }
            }.sortedByDescending { it.id }

            val classRanks = mutableMapOf<String, String>()
            val courseItemMap = rankData?.optJSONObject("courseItemMap")
            if (courseItemMap != null) {
                val keys = courseItemMap.keys()
                while (keys.hasNext()) {
                    val courseId = keys.next()
                    val item = courseItemMap.optJSONObject(courseId) ?: continue
                    if (!item.isNull("stdLessonRank")) {
                        val rank = item.optInt("stdLessonRank")
                        val count = if (!item.isNull("stdCount")) item.optInt("stdCount") else 0
                        classRanks[courseId] = if (count > 0) "$rank/$count" else rank.toString()
                    }
                }
            }

            // 学期详情并发拉取。限制 4 个线程，避免对教务系统造成不必要的瞬时压力。
            val gradePool = Executors.newFixedThreadPool(4)
            val futures = semesters.map { semester ->
                semester to gradePool.submit<Pair<List<NwpuGradeRecord>, String?>> {
                    runCatching {
                        val detail = fetchJson(
                            "/student/for-std/grade/sheet/info/${urlEncode(studentId)}?semester=${semester.id}",
                            cookie,
                            userAgent
                        )
                        val gradeMap = detail.optJSONObject("semesterId2studentGrades")
                        val list = gradeMap?.optJSONArray(semester.id.toString())
                        val records = buildList {
                            if (list != null) {
                                for (index in 0 until list.length()) {
                                    val item = list.optJSONObject(index) ?: continue
                                    val course = item.optJSONObject("course") ?: continue
                                    val courseName = course.optString("nameZh", course.optString("name", "")).trim()
                                    if (courseName.isBlank()) continue
                                    val courseId = course.opt("id")?.toString().orEmpty()
                                    add(
                                        NwpuGradeRecord(
                                            courseId = courseId,
                                            courseCode = course.optString("code", ""),
                                            courseName = courseName,
                                            credits = course.optNullableDouble("credits"),
                                            grade = firstText(item, "gaGrade", "grade", "score") ?: "-",
                                            gradePoint = firstDouble(item, "gp", "gradePoint"),
                                            semesterId = semester.id,
                                            semesterName = semester.name,
                                            obligatory = course.optNullableBoolean("obligatory"),
                                            classRank = classRanks[courseId]
                                        )
                                    )
                                }
                            }
                        }
                        records to null
                    }.getOrElse { emptyList<NwpuGradeRecord>() to (it.message ?: "读取失败") }
                }
            }

            val warnings = mutableListOf<String>()
            val grades = mutableListOf<NwpuGradeRecord>()
            futures.forEach { (semester, future) ->
                val (records, warning) = future.get()
                grades += records
                if (warning != null) warnings += "${semester.name}：$warning"
            }
            gradePool.shutdown()

            val gpaRank = gpaData.optJSONObject("stdGpaRankDto") ?: JSONObject()
            val gpa = firstDouble(gpaRank, "gpa", "stdGpa", "gradePointAverage")
                ?: semesterData.optNullableDouble("gpa")

            NwpuGradeQueryResult(
                studentId = studentId,
                gpa = gpa,
                rank = firstDouble(gpaRank, "rank", "gpaRank")?.toInt(),
                beforeRankGpa = gpaRank.optNullableDouble("beforeRankGpa"),
                afterRankGpa = gpaRank.optNullableDouble("afterRankGpa"),
                semesters = semesters,
                grades = grades,
                warnings = warnings
            )
        }

    fun queryExams(cookie: String, userAgent: String = DEFAULT_UA): Result<NwpuExamQueryResult> =
        runCatching {
            require(cookie.isNotBlank()) { "没有可用的翱翔教务登录 Cookie，请先登录" }
            val html = fetchText("/student/for-std/exam-arrange", cookie, userAgent, acceptJson = false)
            val known = parseKnownExamRows(html)
            val exams = if (known.isNotEmpty()) known else parseGenericExamRows(html)
            NwpuExamQueryResult(
                exams = exams.distinctBy { "${it.courseName}|${it.timeText}|${it.location}" },
                source = if (known.isNotEmpty()) "direct-html" else "direct-html-fallback",
                warnings = if (exams.isEmpty()) {
                    listOf("当前考试信息页没有可识别的记录；若学校尚未发布排考，这属于正常情况。")
                } else emptyList()
            )
        }

    private fun fetchJson(path: String, cookie: String, userAgent: String): JSONObject {
        val text = fetchText(path, cookie, userAgent, acceptJson = true)
        return runCatching { JSONObject(text) }
            .getOrElse { throw IllegalStateException("教务返回了无法解析的数据") }
    }

    private fun fetchText(
        path: String,
        cookie: String,
        userAgent: String,
        acceptJson: Boolean
    ): String {
        val connection = (URL(BASE + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 12_000
            instanceFollowRedirects = true
            setRequestProperty("Cookie", cookie)
            setRequestProperty("User-Agent", userAgent.ifBlank { DEFAULT_UA })
            setRequestProperty("Referer", "$BASE/student/home")
            setRequestProperty(
                "Accept",
                if (acceptJson) "application/json, text/plain, */*" else "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
            )
        }
        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) error("HTTP $code")
            if (looksLikeLoginPage(text, connection.url.toString())) {
                error("登录状态已失效，请重新进入翱翔教务")
            }
            text
        } finally {
            connection.disconnect()
        }
    }

    private fun looksLikeLoginPage(text: String, finalUrl: String): Boolean =
        finalUrl.contains("uis.nwpu.edu.cn", ignoreCase = true) ||
            text.contains("统一身份认证") ||
            text.contains("uis.nwpu.edu.cn/cas/login", ignoreCase = true)

    private fun parseKnownExamRows(html: String): List<NwpuExamRecord> {
        val rowRegex = Regex("""(?is)<tr\b([^>]*)data-finished\s*=\s*[\"']?([^\"' >]+)[\"']?([^>]*)>(.*?)</tr>""")
        val cellRegex = Regex("""(?is)<td\b[^>]*>(.*?)</td>""")
        val timeRegex = Regex("""(?is)<(?:div|span)\b[^>]*class\s*=\s*[\"'][^\"']*\btime\b[^\"']*[\"'][^>]*>(.*?)</(?:div|span)>""")
        return rowRegex.findAll(html).mapNotNull { match ->
            val finished = match.groupValues[2].equals("true", ignoreCase = true)
            val body = match.groupValues[4]
            val cells = cellRegex.findAll(body).map { stripHtml(it.groupValues[1]) }.toList()
            val course = cells.getOrNull(1).orEmpty()
            val location = cells.getOrNull(0).orEmpty()
            val status = cells.getOrNull(2).orEmpty()
            val time = timeRegex.find(body)?.let { stripHtml(it.groupValues[1]) }
                ?: extractDateTime(stripHtml(body)).orEmpty()
            if (course.isBlank() && time.isBlank()) null
            else NwpuExamRecord(course, time, location, status, finished)
        }.toList()
    }

    private fun parseGenericExamRows(html: String): List<NwpuExamRecord> {
        val rowRegex = Regex("""(?is)<tr\b[^>]*>(.*?)</tr>""")
        val cellRegex = Regex("""(?is)<td\b[^>]*>(.*?)</td>""")
        return rowRegex.findAll(html).mapNotNull { row ->
            val cells = cellRegex.findAll(row.groupValues[1]).map { stripHtml(it.groupValues[1]) }.toList()
            if (cells.size < 2) return@mapNotNull null
            val joined = cells.joinToString(" | ")
            val time = extractDateTime(joined) ?: return@mapNotNull null
            val course = cells.firstOrNull { cell ->
                cell.isNotBlank() && !cell.contains(Regex("""20\d{2}[-/.年]""")) && cell.length in 2..80
            }.orEmpty()
            NwpuExamRecord(
                courseName = course,
                timeText = time,
                location = cells.firstOrNull().orEmpty(),
                status = cells.lastOrNull().orEmpty(),
                finished = row.value.contains("data-finished=\"true\"", true) ||
                    row.value.contains("data-finished='true'", true)
            )
        }.toList()
    }

    private fun extractDateTime(text: String): String? {
        val regex = Regex(
            """20\d{2}\s*[-/.年]\s*\d{1,2}\s*[-/.月]\s*\d{1,2}\s*(?:日)?[^|]{0,36}?\d{1,2}\s*[:：]\s*\d{2}(?:[^|]{0,18}?\d{1,2}\s*[:：]\s*\d{2})?"""
        )
        return regex.find(text)?.value?.replace(Regex("\\s+"), " ")?.trim()
    }

    @Suppress("DEPRECATION")
    private fun stripHtml(value: String): String =
        Html.fromHtml(value).toString()
            .replace('\u00A0', ' ')
            .replace(Regex("[\\t\\r ]+"), " ")
            .replace(Regex("\\n+"), " ")
            .trim()

    private fun urlEncode(value: String): String = java.net.URLEncoder.encode(value, "UTF-8")

    private fun firstText(obj: JSONObject, vararg keys: String): String? {
        keys.forEach { key ->
            if (!obj.isNull(key)) {
                val value = obj.opt(key)?.toString()?.trim()
                if (!value.isNullOrBlank()) return value
            }
        }
        return null
    }

    private fun firstDouble(obj: JSONObject, vararg keys: String): Double? {
        keys.forEach { key -> obj.optNullableDouble(key)?.let { return it } }
        return null
    }

    private fun JSONObject.optNullableDouble(key: String): Double? {
        if (isNull(key)) return null
        val value = opt(key) ?: return null
        return when (value) {
            is Number -> value.toDouble()
            else -> value.toString().toDoubleOrNull()
        }
    }

    private fun JSONObject.optNullableBoolean(key: String): Boolean? {
        if (isNull(key)) return null
        val value = opt(key) ?: return null
        return when (value) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            else -> when (value.toString().trim().lowercase()) {
                "true", "1", "yes" -> true
                "false", "0", "no" -> false
                else -> null
            }
        }
    }
}

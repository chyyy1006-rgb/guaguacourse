package com.example.npucourse.importer

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import org.json.JSONObject


data class NwpuAcademicSemester(
    val id: Long,
    val name: String
)


data class NwpuGradeRecord(
    val courseId: String,
    val courseCode: String,
    val courseName: String,
    val credits: Double?,
    val grade: String,
    val gradePoint: Double?,
    val semesterId: Long,
    val semesterName: String,
    val obligatory: Boolean?,
    val classRank: String?
)


data class NwpuGradeQueryResult(
    val studentId: String,
    val gpa: Double?,
    val rank: Int?,
    val beforeRankGpa: Double?,
    val afterRankGpa: Double?,
    val semesters: List<NwpuAcademicSemester>,
    val grades: List<NwpuGradeRecord>,
    val warnings: List<String>
)


data class NwpuExamRecord(
    val courseName: String,
    val timeText: String,
    val location: String,
    val status: String,
    val finished: Boolean
)


data class NwpuExamQueryResult(
    val exams: List<NwpuExamRecord>,
    val source: String,
    val warnings: List<String>
)


class NwpuAcademicJavascriptBridge(
    private val onGradesPayload: (String) -> Unit,
    private val onExamsPayload: (String) -> Unit
) {

    private val mainHandler =
        Handler(
            Looper.getMainLooper()
        )


    @JavascriptInterface
    fun onGrades(
        json: String
    ) {
        mainHandler.post {
            onGradesPayload(
                json
            )
        }
    }


    @JavascriptInterface
    fun onExams(
        json: String
    ) {
        mainHandler.post {
            onExamsPayload(
                json
            )
        }
    }
}


object NwpuAcademicExtractor {

    const val BRIDGE_NAME =
        "GuaguaAcademic"


    fun queryGrades(
        webView: WebView
    ) {
        webView.evaluateJavascript(
            GRADE_QUERY_SCRIPT,
            null
        )
    }


    fun queryExams(
        webView: WebView
    ) {
        webView.evaluateJavascript(
            EXAM_QUERY_SCRIPT,
            null
        )
    }


    fun parseGradePayload(
        json: String
    ): Result<NwpuGradeQueryResult> =
        runCatching {
            val root =
                JSONObject(
                    json
                )

            require(
                root.optBoolean(
                    "ok",
                    false
                )
            ) {
                root.optString(
                    "error",
                    "成绩查询失败"
                )
            }

            val semesters =
                buildList {
                    val array =
                        root.optJSONArray(
                            "semesters"
                        )

                    if (
                        array != null
                    ) {
                        for (
                            index in 0 until array.length()
                        ) {
                            val item =
                                array.optJSONObject(
                                    index
                                )
                                    ?: continue

                            val id =
                                item.optLong(
                                    "id",
                                    0L
                                )

                            val name =
                                item.optString(
                                    "name",
                                    ""
                                )
                                    .trim()

                            if (
                                id > 0L &&
                                name.isNotBlank()
                            ) {
                                add(
                                    NwpuAcademicSemester(
                                        id = id,
                                        name = name
                                    )
                                )
                            }
                        }
                    }
                }

            val grades =
                buildList {
                    val array =
                        root.optJSONArray(
                            "grades"
                        )

                    if (
                        array != null
                    ) {
                        for (
                            index in 0 until array.length()
                        ) {
                            val item =
                                array.optJSONObject(
                                    index
                                )
                                    ?: continue

                            val courseName =
                                item.optString(
                                    "courseName",
                                    ""
                                )
                                    .trim()

                            if (
                                courseName.isBlank()
                            ) {
                                continue
                            }

                            add(
                                NwpuGradeRecord(
                                    courseId =
                                        item.optString(
                                            "courseId",
                                            ""
                                        ),
                                    courseCode =
                                        item.optString(
                                            "courseCode",
                                            ""
                                        ),
                                    courseName =
                                        courseName,
                                    credits =
                                        item.optNullableDouble(
                                            "credits"
                                        ),
                                    grade =
                                        item.optString(
                                            "grade",
                                            "-"
                                        ),
                                    gradePoint =
                                        item.optNullableDouble(
                                            "gradePoint"
                                        ),
                                    semesterId =
                                        item.optLong(
                                            "semesterId",
                                            0L
                                        ),
                                    semesterName =
                                        item.optString(
                                            "semesterName",
                                            ""
                                        ),
                                    obligatory =
                                        item.optNullableBoolean(
                                            "obligatory"
                                        ),
                                    classRank =
                                        item.optNullableString(
                                            "classRank"
                                        )
                                )
                            )
                        }
                    }
                }

            NwpuGradeQueryResult(
                studentId =
                    root.optString(
                        "studentId",
                        ""
                    ),
                gpa =
                    root.optNullableDouble(
                        "gpa"
                    ),
                rank =
                    root.optNullableInt(
                        "rank"
                    ),
                beforeRankGpa =
                    root.optNullableDouble(
                        "beforeRankGpa"
                    ),
                afterRankGpa =
                    root.optNullableDouble(
                        "afterRankGpa"
                    ),
                semesters =
                    semesters,
                grades =
                    grades,
                warnings =
                    root.stringList(
                        "warnings"
                    )
            )
        }


    fun parseExamPayload(
        json: String
    ): Result<NwpuExamQueryResult> =
        runCatching {
            val root =
                JSONObject(
                    json
                )

            require(
                root.optBoolean(
                    "ok",
                    false
                )
            ) {
                root.optString(
                    "error",
                    "考试查询失败"
                )
            }

            val exams =
                buildList {
                    val array =
                        root.optJSONArray(
                            "exams"
                        )

                    if (
                        array != null
                    ) {
                        for (
                            index in 0 until array.length()
                        ) {
                            val item =
                                array.optJSONObject(
                                    index
                                )
                                    ?: continue

                            val courseName =
                                item.optString(
                                    "courseName",
                                    ""
                                )
                                    .trim()

                            val timeText =
                                item.optString(
                                    "timeText",
                                    ""
                                )
                                    .trim()

                            if (
                                courseName.isBlank() &&
                                timeText.isBlank()
                            ) {
                                continue
                            }

                            add(
                                NwpuExamRecord(
                                    courseName =
                                        courseName.ifBlank {
                                            "未识别课程"
                                        },
                                    timeText =
                                        timeText,
                                    location =
                                        item.optString(
                                            "location",
                                            ""
                                        )
                                            .trim(),
                                    status =
                                        item.optString(
                                            "status",
                                            ""
                                        )
                                            .trim(),
                                    finished =
                                        item.optBoolean(
                                            "finished",
                                            false
                                        )
                                )
                            )
                        }
                    }
                }

            NwpuExamQueryResult(
                exams =
                    exams,
                source =
                    root.optString(
                        "source",
                        "exam-arrange"
                    ),
                warnings =
                    root.stringList(
                        "warnings"
                    )
            )
        }


    private fun JSONObject.optNullableString(
        key: String
    ): String? {
        val value =
            opt(
                key
            )

        if (
            value == null ||
            value == JSONObject.NULL
        ) {
            return null
        }

        return value
            .toString()
            .trim()
            .ifBlank {
                null
            }
    }


    private fun JSONObject.optNullableDouble(
        key: String
    ): Double? {
        val value =
            opt(
                key
            )

        if (
            value == null ||
            value == JSONObject.NULL
        ) {
            return null
        }

        return when (
            value
        ) {
            is Number ->
                value.toDouble()

            else ->
                value
                    .toString()
                    .trim()
                    .toDoubleOrNull()
        }
    }


    private fun JSONObject.optNullableInt(
        key: String
    ): Int? {
        val value =
            opt(
                key
            )

        if (
            value == null ||
            value == JSONObject.NULL
        ) {
            return null
        }

        return when (
            value
        ) {
            is Number ->
                value.toInt()

            else ->
                value
                    .toString()
                    .trim()
                    .toIntOrNull()
        }
    }


    private fun JSONObject.optNullableBoolean(
        key: String
    ): Boolean? {
        val value =
            opt(
                key
            )

        if (
            value == null ||
            value == JSONObject.NULL
        ) {
            return null
        }

        return when (
            value
        ) {
            is Boolean ->
                value

            is Number ->
                value.toInt() != 0

            else ->
                when (
                    value
                        .toString()
                        .trim()
                        .lowercase()
                ) {
                    "true",
                    "1",
                    "yes" ->
                        true

                    "false",
                    "0",
                    "no" ->
                        false

                    else ->
                        null
                }
        }
    }


    private fun JSONObject.stringList(
        key: String
    ): List<String> =
        buildList {
            val array =
                optJSONArray(
                    key
                )
                    ?: return@buildList

            for (
                index in 0 until array.length()
            ) {
                val text =
                    array.optString(
                        index,
                        ""
                    )
                        .trim()

                if (
                    text.isNotBlank()
                ) {
                    add(
                        text
                    )
                }
            }
        }


    private val GRADE_QUERY_SCRIPT =
        """
        (function() {
            var bridge = window.GuaguaAcademic;

            function send(payload) {
                try {
                    bridge.onGrades(JSON.stringify(payload));
                } catch (bridgeError) {
                    console.error("[GuaguaCourse] grade bridge failed", bridgeError);
                }
            }

            function asNumber(value) {
                if (value === null || value === undefined || value === "") {
                    return null;
                }
                var number = Number(value);
                return Number.isFinite(number) ? number : null;
            }

            function firstValue(object, keys) {
                if (!object) return null;
                for (var i = 0; i < keys.length; i++) {
                    var value = object[keys[i]];
                    if (value !== undefined && value !== null && value !== "") {
                        return value;
                    }
                }
                return null;
            }

            async function fetchJson(url) {
                var response = await fetch(url, {
                    credentials: "include",
                    headers: {
                        "Accept": "application/json, text/plain, */*"
                    }
                });

                var text = await response.text();

                if (!response.ok) {
                    throw new Error("HTTP " + response.status + " · " + url);
                }

                if (/<!doctype\s+html|<html/i.test(text)) {
                    throw new Error("登录状态已失效，请重新进入翱翔教务");
                }

                try {
                    return JSON.parse(text);
                } catch (error) {
                    throw new Error("教务返回了无法解析的数据");
                }
            }

            (async function() {
                if (!location.host || location.host.toLowerCase() !== "jwxt.nwpu.edu.cn") {
                    throw new Error("请先进入翱翔教务后再查询成绩");
                }

                var info = await fetchJson(
                    "/student/for-std/student-portrait/getStdInfo?bizTypeAssoc=2&cultivateTypeAssoc=1"
                );

                var studentId = info && info.student && info.student.id;

                if (!studentId) {
                    throw new Error("未能从教务系统获取学生身份 ID");
                }

                var gpaPromise = fetchJson(
                    "/student/for-std/student-portrait/getMyGpa?studentAssoc=" +
                    encodeURIComponent(studentId)
                );

                var semesterPromise = fetchJson(
                    "/student/for-std/student-portrait/getMyGrades?studentAssoc=" +
                    encodeURIComponent(studentId) +
                    "&semesterAssoc="
                );

                var rankPromise = fetchJson(
                    "/student/for-std/student-portrait/getMyGradesByProgram?studentAssoc=" +
                    encodeURIComponent(studentId)
                ).catch(function() {
                    return null;
                });

                var common = await Promise.all([
                    gpaPromise,
                    semesterPromise,
                    rankPromise
                ]);

                var gpaData = common[0] || {};
                var semesterData = common[1] || {};
                var rankData = common[2] || {};

                var semesters = Array.isArray(semesterData.semesters)
                    ? semesterData.semesters.slice()
                    : [];

                semesters.sort(function(a, b) {
                    return Number(b && b.id || 0) - Number(a && a.id || 0);
                });

                var classRankMap = {};
                var courseItemMap = rankData && rankData.courseItemMap;

                if (courseItemMap && typeof courseItemMap === "object") {
                    Object.keys(courseItemMap).forEach(function(courseId) {
                        var item = courseItemMap[courseId];
                        if (item && item.stdLessonRank !== null && item.stdLessonRank !== undefined) {
                            var total = item.stdCount !== null && item.stdCount !== undefined
                                ? "/" + item.stdCount
                                : "";
                            classRankMap[String(courseId)] = String(item.stdLessonRank) + total;
                        }
                    });
                }

                var warnings = [];
                var grades = [];

                var gradeGroups = await Promise.all(
                    semesters.map(async function(semester) {
                        var semesterId = semester && semester.id;
                        if (!semesterId) return [];

                        try {
                            var detail = await fetchJson(
                                "/student/for-std/grade/sheet/info/" +
                                encodeURIComponent(studentId) +
                                "?semester=" +
                                encodeURIComponent(semesterId)
                            );

                            var map = detail && detail.semesterId2studentGrades;
                            var list = map && (
                                map[semesterId] ||
                                map[String(semesterId)]
                            );

                            return Array.isArray(list) ? list : [];
                        } catch (error) {
                            warnings.push(
                                (semester && semester.nameZh ? semester.nameZh : String(semesterId)) +
                                "：" +
                                (error && error.message ? error.message : "读取失败")
                            );
                            return [];
                        }
                    })
                );

                gradeGroups.forEach(function(group, semesterIndex) {
                    var semester = semesters[semesterIndex] || {};
                    var semesterId = Number(semester.id || 0);
                    var semesterName = String(semester.nameZh || semester.name || "");

                    group.forEach(function(item) {
                        var course = item && item.course || {};
                        var courseId = String(course.id || "");
                        var courseName = String(course.nameZh || course.name || "").trim();

                        if (!courseName) return;

                        grades.push({
                            courseId: courseId,
                            courseCode: String(course.code || ""),
                            courseName: courseName,
                            credits: asNumber(course.credits),
                            grade: String(
                                firstValue(item, ["gaGrade", "grade", "score"]) || "-"
                            ),
                            gradePoint: asNumber(
                                firstValue(item, ["gp", "gradePoint"])
                            ),
                            semesterId: semesterId,
                            semesterName: semesterName,
                            obligatory: course.obligatory === undefined || course.obligatory === null
                                ? null
                                : Boolean(course.obligatory),
                            classRank: classRankMap[courseId] || null
                        });
                    });
                });

                var gpaRank = gpaData && gpaData.stdGpaRankDto || {};
                var gpa = asNumber(
                    firstValue(gpaRank, ["gpa", "stdGpa", "gradePointAverage"])
                );

                if (gpa === null) {
                    gpa = asNumber(
                        firstValue(semesterData, ["gpa"])
                    );
                }

                send({
                    ok: true,
                    studentId: String(studentId),
                    gpa: gpa,
                    rank: asNumber(firstValue(gpaRank, ["rank", "gpaRank"])),
                    beforeRankGpa: asNumber(gpaRank.beforeRankGpa),
                    afterRankGpa: asNumber(gpaRank.afterRankGpa),
                    semesters: semesters.map(function(semester) {
                        return {
                            id: Number(semester.id || 0),
                            name: String(semester.nameZh || semester.name || "")
                        };
                    }).filter(function(semester) {
                        return semester.id > 0 && semester.name;
                    }),
                    grades: grades,
                    warnings: warnings
                });
            })().catch(function(error) {
                send({
                    ok: false,
                    error: error && error.message
                        ? error.message
                        : "成绩查询失败"
                });
            });

            return "STARTED";
        })();
        """.trimIndent()


    private val EXAM_QUERY_SCRIPT =
        """
        (function() {
            var bridge = window.GuaguaAcademic;

            function send(payload) {
                try {
                    bridge.onExams(JSON.stringify(payload));
                } catch (bridgeError) {
                    console.error("[GuaguaCourse] exam bridge failed", bridgeError);
                }
            }

            function clean(value) {
                return String(value || "")
                    .replace(/\u00a0/g, " ")
                    .replace(/[\t\r ]+/g, " ")
                    .replace(/\n\s*\n+/g, "\n")
                    .trim();
            }

            function findHeaderIndex(headers, keywords) {
                for (var i = 0; i < headers.length; i++) {
                    var text = clean(headers[i]);
                    for (var j = 0; j < keywords.length; j++) {
                        if (text.indexOf(keywords[j]) >= 0) return i;
                    }
                }
                return -1;
            }

            function parseKnownRows(root) {
                var rows = Array.prototype.slice.call(
                    root.querySelectorAll("tr[data-finished]")
                );

                return rows.map(function(row) {
                    var cells = Array.prototype.slice.call(
                        row.querySelectorAll("td")
                    );

                    var locationSpans = cells[0]
                        ? Array.prototype.slice.call(cells[0].querySelectorAll("span"))
                        : [];

                    var location = clean(
                        locationSpans.map(function(span) {
                            return clean(span.innerText || span.textContent);
                        }).filter(Boolean).join(", ")
                    );

                    if (!location && cells[0]) {
                        location = clean(cells[0].innerText || cells[0].textContent);
                    }

                    var course = "";
                    if (cells[1]) {
                        var courseSpan = cells[1].querySelector("span");
                        course = clean(
                            courseSpan
                                ? courseSpan.innerText || courseSpan.textContent
                                : cells[1].innerText || cells[1].textContent
                        );
                    }

                    var timeNode = row.querySelector("div.time, .time");
                    var timeText = clean(
                        timeNode
                            ? timeNode.innerText || timeNode.textContent
                            : ""
                    );

                    var status = cells[2]
                        ? clean(cells[2].innerText || cells[2].textContent)
                        : "";

                    return {
                        courseName: course,
                        timeText: timeText,
                        location: location,
                        status: status,
                        finished: String(row.getAttribute("data-finished")).toLowerCase() === "true"
                    };
                }).filter(function(item) {
                    return item.courseName || item.timeText;
                });
            }

            function parseGenericTables(root) {
                var results = [];
                var tables = Array.prototype.slice.call(
                    root.querySelectorAll("table")
                );

                tables.forEach(function(table) {
                    var headerNodes = Array.prototype.slice.call(
                        table.querySelectorAll("thead th")
                    );

                    if (!headerNodes.length) {
                        var firstRow = table.querySelector("tr");
                        if (firstRow) {
                            headerNodes = Array.prototype.slice.call(
                                firstRow.querySelectorAll("th")
                            );
                        }
                    }

                    var headers = headerNodes.map(function(node) {
                        return clean(node.innerText || node.textContent);
                    });

                    var courseIndex = findHeaderIndex(headers, ["课程", "科目"]);
                    var timeIndex = findHeaderIndex(headers, ["考试时间", "时间"]);
                    var locationIndex = findHeaderIndex(headers, ["考试地点", "地点", "考场"]);
                    var statusIndex = findHeaderIndex(headers, ["状态"]);

                    var rows = Array.prototype.slice.call(
                        table.querySelectorAll("tbody tr")
                    );

                    rows.forEach(function(row) {
                        var cells = Array.prototype.slice.call(
                            row.querySelectorAll("td")
                        ).map(function(cell) {
                            return clean(cell.innerText || cell.textContent);
                        });

                        if (!cells.length) return;

                        var rowText = clean(cells.join(" | "));
                        var likelyExam =
                            /\d{4}[-/.年]\d{1,2}[-/.月]\d{1,2}/.test(rowText) ||
                            /\d{1,2}:\d{2}/.test(rowText) ||
                            row.hasAttribute("data-finished");

                        if (!likelyExam) return;

                        var timeText = timeIndex >= 0 ? cells[timeIndex] || "" : "";
                        if (!timeText) {
                            var timeMatch = rowText.match(
                                /\d{4}[-/.年]\d{1,2}[-/.月]\d{1,2}[^|]{0,40}(?:\d{1,2}:\d{2}[^|]{0,20})?/
                            );
                            timeText = timeMatch ? clean(timeMatch[0]) : "";
                        }

                        var courseName = courseIndex >= 0 ? cells[courseIndex] || "" : "";
                        if (!courseName && cells.length > 1) {
                            courseName = cells[1];
                        }

                        results.push({
                            courseName: courseName,
                            timeText: timeText,
                            location: locationIndex >= 0 ? cells[locationIndex] || "" : (cells[0] || ""),
                            status: statusIndex >= 0 ? cells[statusIndex] || "" : "",
                            finished: String(row.getAttribute("data-finished") || "false").toLowerCase() === "true"
                        });
                    });
                });

                return results;
            }

            function uniqueExams(list) {
                var seen = {};
                return list.filter(function(item) {
                    var key = [
                        item.courseName,
                        item.timeText,
                        item.location
                    ].join("|");

                    if (seen[key]) return false;
                    seen[key] = true;
                    return true;
                });
            }

            (async function() {
                if (!location.host || location.host.toLowerCase() !== "jwxt.nwpu.edu.cn") {
                    throw new Error("请先进入翱翔教务后再查询考试");
                }

                var response = await fetch(
                    "/student/for-std/exam-arrange",
                    {
                        credentials: "include",
                        headers: {
                            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
                        }
                    }
                );

                var html = await response.text();

                if (!response.ok) {
                    throw new Error("考试信息页面 HTTP " + response.status);
                }

                if (/统一身份认证|uis\.nwpu\.edu\.cn\/cas\/login/i.test(html)) {
                    throw new Error("登录状态已失效，请重新进入翱翔教务");
                }

                var documentFromServer = new DOMParser().parseFromString(
                    html,
                    "text/html"
                );

                var exams = parseKnownRows(documentFromServer);
                var source = "exam-arrange-html";
                var warnings = [];

                if (!exams.length) {
                    exams = parseGenericTables(documentFromServer);
                    source = "exam-arrange-table-fallback";
                }

                if (!exams.length) {
                    var currentRows = parseKnownRows(document);
                    if (!currentRows.length) {
                        currentRows = parseGenericTables(document);
                    }

                    if (currentRows.length) {
                        exams = currentRows;
                        source = "current-page-fallback";
                    }
                }

                exams = uniqueExams(exams);

                if (!exams.length) {
                    warnings.push(
                        "当前考试信息页没有可识别的考试记录；如果学校尚未发布排考，这属于正常情况。"
                    );
                }

                send({
                    ok: true,
                    exams: exams,
                    source: source,
                    warnings: warnings
                });
            })().catch(function(error) {
                send({
                    ok: false,
                    error: error && error.message
                        ? error.message
                        : "考试查询失败"
                });
            });

            return "STARTED";
        })();
        """.trimIndent()
}

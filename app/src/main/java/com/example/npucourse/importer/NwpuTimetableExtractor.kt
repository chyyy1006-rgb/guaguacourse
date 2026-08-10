package com.example.npucourse.importer

import android.webkit.WebView
import org.json.JSONObject
import org.json.JSONTokener


/*
 * =========================================================
 * 西北工业大学翱翔教务课表提取器
 * =========================================================
 *
 * 作用：
 *
 * 当前已经登录的 WebView
 * ↓
 * 读取当前课表 DOM
 * ↓
 * 转换成 EduCourseRecord
 *
 * 不读取：
 *
 * 学号
 * 密码
 * Cookie
 * token
 *
 * 只读取当前网页中已经显示的课表内容。
 */

object NwpuTimetableExtractor {


    /*
     * =====================================================
     * 提取当前课表
     * =====================================================
     */

    fun extract(
        webView: WebView,
        onResult: (Result<List<EduCourseRecord>>) -> Unit
    ) {

        webView.evaluateJavascript(
            EXTRACT_SCRIPT
        ) { rawResult ->

            val result =
                runCatching {

                    parseJavascriptResult(
                        rawResult
                    )
                }

            onResult(
                result
            )
        }
    }


    /*
     * =====================================================
     * JavaScript 返回值 → Kotlin
     * =====================================================
     */

    private fun parseJavascriptResult(
        rawResult: String?
    ): List<EduCourseRecord> {

        if (
            rawResult.isNullOrBlank() ||
            rawResult == "null"
        ) {

            error(
                "网页没有返回课表数据"
            )
        }


        /*
         * evaluateJavascript 会额外进行一层 JSON 编码。
         *
         * 先解除第一层字符串。
         */
        val firstLayer =
            JSONTokener(
                rawResult
            ).nextValue()


        val jsonText =
            when (
                firstLayer
            ) {

                is String ->
                    firstLayer

                else ->
                    firstLayer.toString()
            }


        val root =
            JSONObject(
                jsonText
            )


        if (
            !root.optBoolean(
                "ok",
                false
            )
        ) {

            error(
                root.optString(
                    "error",
                    "无法读取课表"
                )
            )
        }


        val array =
            root.getJSONArray(
                "records"
            )


        val result =
            mutableListOf<EduCourseRecord>()


        for (
        index in 0 until
                array.length()
        ) {

            val item =
                array.getJSONObject(
                    index
                )


            val courseName =
                item.optString(
                    "courseName"
                ).trim()


            val weekday =
                item.optInt(
                    "weekday",
                    0
                )


            val startSection =
                item.optInt(
                    "startSection",
                    0
                )


            val endSection =
                item.optInt(
                    "endSection",
                    0
                )


            val weekText =
                item.optString(
                    "weekText"
                ).trim()


            /*
             * 基础数据不完整的记录直接跳过。
             */
            if (
                courseName.isBlank() ||
                weekday !in 1..7 ||
                startSection <= 0 ||
                endSection <= 0 ||
                weekText.isBlank()
            ) {

                continue
            }


            result.add(

                EduCourseRecord(

                    externalId =
                        "jwxt-" +
                                index,

                    courseName =
                        courseName,

                    teacher =
                        item.optString(
                            "teacher"
                        ).trim(),

                    room =
                        item.optString(
                            "room"
                        ).trim(),

                    weekday =
                        weekday,

                    startSection =
                        startSection,

                    endSection =
                        endSection,

                    weekText =
                        weekText,

                    campus =
                        null
                )
            )
        }


        if (
            result.isEmpty()
        ) {

            error(
                "找到了课表页面，但没有识别到课程。请确认当前课表中确实有课程。"
            )
        }


        return result
    }


    /*
     * =====================================================
     * 网页端课表解析
     * =====================================================
     *
     * 当前翱翔教务课表结构：
     *
     * table
     * └── td
     *     └── div.tdHtml
     *         └── div.course-name
     *
     * 同时考虑 rowspan，
     * 避免课程跨多节时星期列错位。
     */

    private val EXTRACT_SCRIPT =
        """
        (function() {

            try {

                function cleanText(value) {

                    return (value || "")
                        .replace(/\u00a0/g, " ")
                        .replace(/\s+/g, " ")
                        .trim();
                }


                /*
                 * =========================================
                 * 找课程表
                 * =========================================
                 */

                var table =
                    document.querySelector(
                        "table.courseTable"
                    ) ||
                    document.querySelector(
                        ".course-table table"
                    ) ||
                    document.querySelector(
                        "#courseTable"
                    ) ||
                    document.querySelector(
                        "table"
                    );


                if (!table) {

                    return JSON.stringify({
                        ok: false,
                        error: "当前页面中没有找到课程表"
                    });
                }


                /*
                 * =========================================
                 * 星期表头
                 * =========================================
                 */

                var weekdayNames = [
                    "",
                    "星期一",
                    "星期二",
                    "星期三",
                    "星期四",
                    "星期五",
                    "星期六",
                    "星期日"
                ];


                var weekdayByColumn = {};


                var headerRow =
                    table.querySelector(
                        "thead tr:last-child"
                    ) ||
                    table.querySelector(
                        "tr"
                    );


                if (headerRow) {

                    var headerCells =
                        Array.prototype.slice.call(
                            headerRow.children
                        );


                    var headerColumn = 0;


                    headerCells.forEach(
                        function(cell) {

                            var label =
                                cleanText(
                                    cell.textContent
                                );


                            var colSpan =
                                parseInt(
                                    cell.getAttribute(
                                        "colspan"
                                    ) || "1",
                                    10
                                );


                            for (
                                var day = 1;
                                day <= 7;
                                day++
                            ) {

                                if (
                                    label.indexOf(
                                        weekdayNames[day]
                                    ) >= 0
                                ) {

                                    weekdayByColumn[
                                        headerColumn
                                    ] = day;

                                    break;
                                }
                            }


                            headerColumn +=
                                colSpan;
                        }
                    );
                }


                /*
                 * =========================================
                 * 处理 rowspan
                 * =========================================
                 *
                 * 不能简单使用 td.cellIndex。
                 *
                 * 因为课程跨节时会产生 rowspan，
                 * 后续行的 DOM 单元格数量会减少。
                 */

                var logicalColumns =
                    new Map();


                var bodyRows =
                    Array.prototype.slice.call(
                        table.querySelectorAll(
                            "tbody tr"
                        )
                    );


                if (
                    bodyRows.length === 0
                ) {

                    bodyRows =
                        Array.prototype.slice.call(
                            table.querySelectorAll(
                                "tr"
                            )
                        ).slice(1);
                }


                var occupiedColumns = [];


                bodyRows.forEach(
                    function(row) {

                        /*
                         * 上一行留下的 rowspan
                         * 进入下一行后减少一次。
                         */
                        for (
                            var i = 0;
                            i < occupiedColumns.length;
                            i++
                        ) {

                            if (
                                occupiedColumns[i] > 0
                            ) {

                                occupiedColumns[i]--;
                            }
                        }


                        var logicalColumn = 0;


                        var cells =
                            Array.prototype.slice.call(
                                row.children
                            );


                        cells.forEach(
                            function(cell) {

                                while (
                                    (
                                        occupiedColumns[
                                            logicalColumn
                                        ] || 0
                                    ) > 0
                                ) {

                                    logicalColumn++;
                                }


                                logicalColumns.set(
                                    cell,
                                    logicalColumn
                                );


                                var rowSpan =
                                    parseInt(
                                        cell.getAttribute(
                                            "rowspan"
                                        ) || "1",
                                        10
                                    );


                                var colSpan =
                                    parseInt(
                                        cell.getAttribute(
                                            "colspan"
                                        ) || "1",
                                        10
                                    );


                                for (
                                    var offset = 0;
                                    offset < colSpan;
                                    offset++
                                ) {

                                    occupiedColumns[
                                        logicalColumn +
                                        offset
                                    ] =
                                        Math.max(
                                            occupiedColumns[
                                                logicalColumn +
                                                offset
                                            ] || 0,
                                            rowSpan
                                        );
                                }


                                logicalColumn +=
                                    colSpan;
                            }
                        );
                    }
                );


                /*
                 * =========================================
                 * 元信息解析
                 * =========================================
                 */

                function parseMeta(
                    raw
                ) {

                    var value =
                        cleanText(
                            raw
                        );


                    var campus = "";
                    var room = "";
                    var teacher = "";


                    /*
                     * 明确标签优先。
                     */
                    var teacherMatch =
                        value.match(
                            /(?:教师|老师)[:：]?\s*([^\s]+)/
                        );


                    if (
                        teacherMatch
                    ) {

                        teacher =
                            cleanText(
                                teacherMatch[1]
                            );
                    }


                    var roomMatch =
                        value.match(
                            /(?:地点|教室)[:：]?\s*([^\s]+)/
                        );


                    if (
                        roomMatch
                    ) {

                        room =
                            cleanText(
                                roomMatch[1]
                            );
                    }


                    var tokens =
                        value
                            .replace(
                                /[，,；;]/g,
                                " "
                            )
                            .split(
                                /\s+/
                            )
                            .filter(
                                function(item) {
                                    return item.trim().length > 0;
                                }
                            );


                    tokens.forEach(
                        function(token) {

                            if (
                                token.indexOf(
                                    "校区"
                                ) >= 0 &&
                                campus.length === 0
                            ) {

                                campus =
                                    token;

                                return;
                            }


                            if (
                                room.length === 0 &&
                                (
                                    token.indexOf("楼") >= 0 ||
                                    token.indexOf("教") >= 0 ||
                                    token.indexOf("实验") >= 0 ||
                                    token.indexOf("中心") >= 0 ||
                                    token.indexOf("室") >= 0 ||
                                    token.indexOf("馆") >= 0 ||
                                    token.indexOf("座") >= 0
                                ) &&
                                token.indexOf("教师") < 0
                            ) {

                                room =
                                    token;

                                return;
                            }


                            if (
                                teacher.length === 0 &&
                                token.indexOf("校区") < 0 &&
                                token.indexOf("周") < 0 &&
                                token.indexOf("节") < 0 &&
                                token.indexOf("楼") < 0 &&
                                token.indexOf("实验") < 0 &&
                                token.indexOf("中心") < 0 &&
                                token.indexOf("教室") < 0
                            ) {

                                var chineseMatch =
                                    token.match(
                                        /^[\u4e00-\u9fff]{2,6}/
                                    );


                                if (
                                    chineseMatch &&
                                    chineseMatch[0].length ===
                                        token.length
                                ) {

                                    teacher =
                                        token;
                                }
                            }
                        }
                    );


                    var finalRoom =
                        cleanText(
                            (
                                campus +
                                " " +
                                room
                            )
                        );


                    return {
                        teacher:
                            teacher,

                        room:
                            finalRoom
                    };
                }


                /*
                 * =========================================
                 * 课程单元格
                 * =========================================
                 */

                var courseContainers =
                    Array.prototype.slice.call(
                        table.querySelectorAll(
                            "div.tdHtml"
                        )
                    );


                /*
                 * 极端情况下没有 tdHtml，
                 * 尝试根据 course-name 反查父节点。
                 */
                if (
                    courseContainers.length === 0
                ) {

                    var names =
                        table.querySelectorAll(
                            "div.course-name"
                        );


                    var containerSet =
                        [];


                    Array.prototype
                        .slice
                        .call(names)
                        .forEach(
                            function(nameNode) {

                                var parent =
                                    nameNode.parentElement;

                                if (
                                    parent &&
                                    containerSet.indexOf(
                                        parent
                                    ) < 0
                                ) {

                                    containerSet.push(
                                        parent
                                    );
                                }
                            }
                        );


                    courseContainers =
                        containerSet;
                }


                var records = [];

                var seen = {};


                courseContainers.forEach(
                    function(container) {

                        var parentTd =
                            container.closest(
                                "td"
                            );


                        if (
                            !parentTd
                        ) {

                            return;
                        }


                        var column =
                            logicalColumns.get(
                                parentTd
                            );


                        var weekday =
                            weekdayByColumn[
                                column
                            ] || 0;


                        /*
                         * 常见结构中：
                         *
                         * 第0列 = 节次
                         * 第1~7列 = 周一~周日
                         *
                         * 表头映射失败时使用这个兜底。
                         */
                        if (
                            weekday === 0 &&
                            column >= 1 &&
                            column <= 7
                        ) {

                            weekday =
                                column;
                        }


                        if (
                            weekday < 1 ||
                            weekday > 7
                        ) {

                            return;
                        }


                        var nameNodes =
                            Array.prototype.slice.call(
                                container.querySelectorAll(
                                    "div.course-name"
                                )
                            );


                        nameNodes.forEach(
                            function(nameNode) {

                                var courseName =
                                    cleanText(
                                        nameNode.textContent
                                    );


                                /*
                                 * 某些课程名前面可能出现“本”。
                                 */
                                if (
                                    courseName.indexOf(
                                        "本"
                                    ) === 0 &&
                                    courseName.length > 1
                                ) {

                                    courseName =
                                        courseName.substring(
                                            1
                                        );
                                }


                                if (
                                    courseName.length === 0
                                ) {

                                    return;
                                }


                                /*
                                 * 收集本课程名称之后、
                                 * 下一课程名称之前的所有信息。
                                 */
                                var node =
                                    nameNode.nextSibling;


                                var infoParts = [];


                                while (node) {

                                    if (
                                        node.nodeType === 1 &&
                                        node.classList &&
                                        node.classList.contains(
                                            "course-name"
                                        )
                                    ) {

                                        break;
                                    }


                                    var nodeText =
                                        cleanText(
                                            node.textContent
                                        );


                                    if (
                                        nodeText.length > 0
                                    ) {

                                        /*
                                         * 排除典型课程代码。
                                         */
                                        var codeMatch =
                                            nodeText.match(
                                                /^[A-Z][A-Z0-9]*\.\d+/
                                            );


                                        if (
                                            !codeMatch ||
                                            codeMatch[0].length !==
                                                nodeText.length
                                        ) {

                                            infoParts.push(
                                                nodeText
                                            );
                                        }
                                    }


                                    node =
                                        node.nextSibling;
                                }


                                var allInfo =
                                    cleanText(
                                        infoParts.join(
                                            " "
                                        )
                                    );


                                /*
                                 * =====================================
                                 * 时间模式
                                 * =====================================
                                 *
                                 * 示例：
                                 *
                                 * (1~16周) ... (1-2节)
                                 *
                                 * (1~15周)(单) ... (3-4节)
                                 *
                                 * (1,3,5周) ... (7-8节)
                                 */

                                var scheduleRegex =
                                    /\(([^()]*周[^()]*)\)([\s\S]*?)\((\d+)(?:\s*[-~～]\s*(\d+))?\s*节\)/g;


                                var matches = [];

                                var match;


                                while (
                                    (
                                        match =
                                            scheduleRegex.exec(
                                                allInfo
                                            )
                                    ) !== null
                                ) {

                                    matches.push({

                                        index:
                                            match.index,

                                        endIndex:
                                            scheduleRegex.lastIndex,

                                        week:
                                            cleanText(
                                                match[1]
                                            ),

                                        middle:
                                            cleanText(
                                                match[2]
                                            ),

                                        startSection:
                                            parseInt(
                                                match[3],
                                                10
                                            ),

                                        endSection:
                                            parseInt(
                                                match[4] ||
                                                match[3],
                                                10
                                            )
                                    });
                                }


                                matches.forEach(
                                    function(schedule, scheduleIndex) {

                                        var nextIndex =
                                            scheduleIndex + 1 <
                                                matches.length
                                                ? matches[
                                                    scheduleIndex + 1
                                                ].index
                                                : allInfo.length;


                                        var metadataText =
                                            allInfo.substring(
                                                schedule.endIndex,
                                                nextIndex
                                            );


                                        var weekText =
                                            schedule.week;


                                        /*
                                         * 单双周标记有时位于
                                         * 周次括号和节次括号之间。
                                         */
                                        if (
                                            schedule.middle.indexOf(
                                                "单"
                                            ) >= 0 &&
                                            weekText.indexOf(
                                                "单"
                                            ) < 0
                                        ) {

                                            weekText +=
                                                "单";
                                        }


                                        if (
                                            schedule.middle.indexOf(
                                                "双"
                                            ) >= 0 &&
                                            weekText.indexOf(
                                                "双"
                                            ) < 0
                                        ) {

                                            weekText +=
                                                "双";
                                        }


                                        var meta =
                                            parseMeta(
                                                metadataText
                                            );


                                        var key =
                                            [
                                                courseName,
                                                weekday,
                                                schedule.startSection,
                                                schedule.endSection,
                                                weekText,
                                                meta.teacher,
                                                meta.room
                                            ].join(
                                                "|"
                                            );


                                        if (
                                            seen[key]
                                        ) {

                                            return;
                                        }


                                        seen[key] =
                                            true;


                                        records.push({

                                            courseName:
                                                courseName,

                                            weekday:
                                                weekday,

                                            startSection:
                                                schedule.startSection,

                                            endSection:
                                                schedule.endSection,

                                            teacher:
                                                meta.teacher,

                                            room:
                                                meta.room,

                                            weekText:
                                                weekText
                                        });
                                    }
                                );
                            }
                        );
                    }
                );


                return JSON.stringify({
                    ok: true,
                    count: records.length,
                    records: records
                });


            } catch (error) {

                return JSON.stringify({
                    ok: false,
                    error:
                        error &&
                        error.message
                            ? error.message
                            : "未知课表解析错误"
                });
            }

        })();
        """.trimIndent()
}
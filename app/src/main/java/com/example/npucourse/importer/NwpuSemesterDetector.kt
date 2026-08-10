package com.example.npucourse.importer

import android.webkit.WebView
import com.example.npucourse.util.resolveSemesterStartMillis
import org.json.JSONObject
import org.json.JSONTokener
import java.util.Calendar


/*
 * =========================================================
 * 翱翔教务当前学期
 * =========================================================
 */

data class NwpuSemesterInfo(
    val label: String,
    val startMillis: Long?
)


/*
 * =========================================================
 * 翱翔教务学期识别器
 * =========================================================
 *
 * 本版做两件事：
 *
 * 1. 保留原来的“学期名称识别”逻辑；
 * 2. 优先直接从“我的课表”网页 DOM 中读取学期起始日期。
 *
 * 如果网页没有找到起始日期，才回退到 SemesterResolver。
 */

object NwpuSemesterDetector {

    fun detect(
        webView: WebView,
        onResult: (NwpuSemesterInfo?) -> Unit
    ) {
        webView.evaluateJavascript(
            DETECT_SCRIPT
        ) { rawResult ->
            val detection =
                parseDetectionResult(rawResult)

            if (
                detection == null ||
                detection.label.isBlank()
            ) {
                onResult(null)
                return@evaluateJavascript
            }

            val pageStartMillis =
                parseDateMillis(
                    detection.startDate
                )

            val finalStartMillis =
                pageStartMillis
                    ?: resolveSemesterStartMillis(
                        detection.label
                    )

            onResult(
                NwpuSemesterInfo(
                    label = detection.label,
                    startMillis = finalStartMillis
                )
            )
        }
    }


    private data class DetectionResult(
        val label: String,
        val startDate: String
    )


    /*
     * =====================================================
     * evaluateJavascript 返回值解析
     * =====================================================
     */

    private fun parseDetectionResult(
        rawValue: String?
    ): DetectionResult? {
        if (
            rawValue.isNullOrBlank() ||
            rawValue == "null"
        ) {
            return null
        }

        return runCatching {
            val firstLayer =
                JSONTokener(rawValue)
                    .nextValue()

            val jsonText =
                if (firstLayer is String) {
                    firstLayer
                } else {
                    firstLayer.toString()
                }

            val root =
                JSONObject(jsonText)

            DetectionResult(
                label =
                    root.optString(
                        "label"
                    ).trim(),
                startDate =
                    root.optString(
                        "startDate"
                    ).trim()
            )
        }.getOrNull()
    }


    /*
     * =====================================================
     * 日期文本 -> 当天 00:00
     * =====================================================
     *
     * 支持：
     *
     * 2026-03-02
     * 2026/03/02
     * 2026.03.02
     * 2026年3月2日
     */

    private fun parseDateMillis(
        value: String
    ): Long? {
        if (value.isBlank()) {
            return null
        }

        val match =
            Regex(
                "(20\\d{2})\\D{1,4}(\\d{1,2})\\D{1,4}(\\d{1,2})"
            ).find(value)
                ?: return null

        val year =
            match.groupValues[1]
                .toIntOrNull()
                ?: return null

        val month =
            match.groupValues[2]
                .toIntOrNull()
                ?: return null

        val day =
            match.groupValues[3]
                .toIntOrNull()
                ?: return null

        if (
            month !in 1..12 ||
            day !in 1..31
        ) {
            return null
        }

        return runCatching {
            Calendar
                .getInstance()
                .apply {
                    isLenient = false
                    clear()

                    set(
                        year,
                        month - 1,
                        day,
                        0,
                        0,
                        0
                    )

                    set(
                        Calendar.MILLISECOND,
                        0
                    )
                }
                .timeInMillis
        }.getOrNull()
    }


    /*
     * =====================================================
     * 网页学期 + 起始日期检测
     * =====================================================
     *
     * 学期名称：保持之前已经可用的识别策略。
     *
     * 起始日期：只优先选择与以下关键词相邻的日期：
     *
     * 开学日期 / 开学时间 / 开学
     * 学期开始日期 / 学期开始时间 / 学期开始
     * 学期起始日期 / 学期起始时间 / 学期起始
     * 起始日期 / 开始日期
     *
     * 这样不会把“今天”“查询日期”“考试日期”等其他日期
     * 错当成学期开学日期。
     */

    private val DETECT_SCRIPT =
        """
        (function() {

            try {

                function clean(value) {
                    return (value || "")
                        .replace(/\u00a0/g, " ")
                        .replace(/\s+/g, " ")
                        .trim();
                }


                function compact(value) {
                    return clean(value)
                        .replace(/\s+/g, "");
                }


                function normalizeDate(value) {
                    var text = clean(value);

                    var match = text.match(
                        /(20\d{2})\s*(?:年|[-\/.－—–])\s*(\d{1,2})\s*(?:月|[-\/.－—–])\s*(\d{1,2})\s*日?/
                    );

                    if (!match) {
                        return "";
                    }

                    var year = match[1];
                    var month = String(match[2]).padStart(2, "0");
                    var day = String(match[3]).padStart(2, "0");

                    return year + "-" + month + "-" + day;
                }


                function hasStartKeyword(value) {
                    var text = compact(value);

                    var keys = [
                        "开学日期",
                        "开学时间",
                        "开学",
                        "学期开始日期",
                        "学期开始时间",
                        "学期开始",
                        "学期起始日期",
                        "学期起始时间",
                        "学期起始",
                        "起始日期",
                        "开始日期"
                    ];

                    for (
                        var i = 0;
                        i < keys.length;
                        i++
                    ) {
                        if (
                            text.indexOf(keys[i]) >= 0
                        ) {
                            return true;
                        }
                    }

                    return false;
                }


                function dateNearElement(element) {
                    if (!element) {
                        return "";
                    }

                    var candidates = [];

                    function pushValue(value) {
                        if (value) {
                            candidates.push(value);
                        }
                    }

                    pushValue(
                        element.textContent
                    );

                    if (
                        element.value
                    ) {
                        pushValue(
                            element.value
                        );
                    }

                    if (
                        element.getAttribute
                    ) {
                        pushValue(
                            element.getAttribute("value")
                        );

                        pushValue(
                            element.getAttribute("data-value")
                        );

                        pushValue(
                            element.getAttribute("data-date")
                        );
                    }

                    if (
                        element.nextElementSibling
                    ) {
                        pushValue(
                            element.nextElementSibling.textContent
                        );

                        if (
                            element.nextElementSibling.value
                        ) {
                            pushValue(
                                element.nextElementSibling.value
                            );
                        }
                    }

                    if (
                        element.parentElement
                    ) {
                        pushValue(
                            element.parentElement.textContent
                        );

                        var parentInput =
                            element.parentElement.querySelector(
                                "input"
                            );

                        if (
                            parentInput &&
                            parentInput.value
                        ) {
                            pushValue(
                                parentInput.value
                            );
                        }
                    }

                    if (
                        element.tagName === "LABEL"
                    ) {
                        var forId =
                            element.getAttribute("for");

                        if (forId) {
                            var linked =
                                document.getElementById(
                                    forId
                                );

                            if (linked) {
                                pushValue(
                                    linked.value
                                );

                                pushValue(
                                    linked.textContent
                                );
                            }
                        }
                    }

                    for (
                        var index = 0;
                        index < candidates.length;
                        index++
                    ) {
                        var normalized =
                            normalizeDate(
                                candidates[index]
                            );

                        if (normalized) {
                            return normalized;
                        }
                    }

                    return "";
                }


                /*
                 * =========================================
                 * 学期名称
                 * =========================================
                 */

                var semesterLabel = "";

                var selects =
                    Array.prototype.slice.call(
                        document.querySelectorAll(
                            "select"
                        )
                    );


                for (
                    var selectIndex = 0;
                    selectIndex < selects.length;
                    selectIndex++
                ) {
                    var select =
                        selects[selectIndex];

                    var option =
                        select.options &&
                        select.selectedIndex >= 0
                            ? select.options[
                                select.selectedIndex
                            ]
                            : null;

                    if (!option) {
                        continue;
                    }

                    var optionText =
                        compact(
                            option.textContent
                        );

                    if (
                        /20\d{2}.*20\d{2}/.test(optionText) &&
                        (
                            optionText.indexOf("学期") >= 0 ||
                            optionText.indexOf("春") >= 0 ||
                            optionText.indexOf("秋") >= 0
                        )
                    ) {
                        semesterLabel =
                            optionText;

                        break;
                    }
                }


                if (!semesterLabel) {
                    var bodyText =
                        document.body
                            ? document.body.innerText
                            : "";

                    var semesterMatch =
                        bodyText.match(
                            /20\d{2}\s*[-—–－~～]\s*20\d{2}\s*学年[^\n]{0,20}?(?:第一学期|第二学期|第1学期|第2学期|春季学期|秋季学期)/
                        );

                    if (
                        semesterMatch &&
                        semesterMatch.length > 0
                    ) {
                        semesterLabel =
                            compact(
                                semesterMatch[0]
                            );
                    }
                }


                /*
                 * =========================================
                 * 学期起始日期
                 * =========================================
                 */

                var startDate = "";


                /*
                 * 1. 优先检查字段名明显指向“学期开始”的 input。
                 */
                var inputs =
                    Array.prototype.slice.call(
                        document.querySelectorAll(
                            "input"
                        )
                    );

                for (
                    var inputIndex = 0;
                    inputIndex < inputs.length;
                    inputIndex++
                ) {
                    var input =
                        inputs[inputIndex];

                    var inputDescriptor =
                        [
                            input.id || "",
                            input.name || "",
                            input.placeholder || "",
                            input.getAttribute("aria-label") || "",
                            input.getAttribute("title") || ""
                        ].join(" ");

                    if (
                        hasStartKeyword(
                            inputDescriptor
                        )
                    ) {
                        startDate =
                            normalizeDate(
                                input.value ||
                                input.getAttribute("value") ||
                                ""
                            );

                        if (startDate) {
                            break;
                        }
                    }
                }


                /*
                 * 2. 查找页面中“开学日期 / 学期开始日期”等文字节点，
                 *    再读取它自己、兄弟节点、父节点附近的日期。
                 */
                if (!startDate) {
                    var textNodes =
                        Array.prototype.slice.call(
                            document.querySelectorAll(
                                "label,th,td,dt,dd,span,div,p,strong,b"
                            )
                        );

                    for (
                        var nodeIndex = 0;
                        nodeIndex < textNodes.length;
                        nodeIndex++
                    ) {
                        var element =
                            textNodes[nodeIndex];

                        var text =
                            clean(
                                element.textContent
                            );

                        if (
                            !text ||
                            text.length > 160 ||
                            !hasStartKeyword(text)
                        ) {
                            continue;
                        }

                        startDate =
                            dateNearElement(
                                element
                            );

                        if (startDate) {
                            break;
                        }
                    }
                }


                /*
                 * 3. 表格行 / 表单块通常会把标签和值放在同一个容器里。
                 */
                if (!startDate) {
                    var containers =
                        Array.prototype.slice.call(
                            document.querySelectorAll(
                                "tr,.form-group,.row,.input-group,.control-group"
                            )
                        );

                    for (
                        var containerIndex = 0;
                        containerIndex < containers.length;
                        containerIndex++
                    ) {
                        var container =
                            containers[containerIndex];

                        var containerText =
                            clean(
                                container.textContent
                            );

                        if (
                            !hasStartKeyword(
                                containerText
                            )
                        ) {
                            continue;
                        }

                        startDate =
                            normalizeDate(
                                containerText
                            );

                        if (!startDate) {
                            var innerInput =
                                container.querySelector(
                                    "input"
                                );

                            if (
                                innerInput &&
                                innerInput.value
                            ) {
                                startDate =
                                    normalizeDate(
                                        innerInput.value
                                    );
                            }
                        }

                        if (startDate) {
                            break;
                        }
                    }
                }


                /*
                 * 4. 最后只在“起始日期关键词”附近的正文片段中匹配。
                 *    不直接取页面里的第一个日期。
                 */
                if (!startDate) {
                    var allText =
                        document.body
                            ? document.body.innerText
                            : "";

                    var startPatterns = [
                        /(?:开学日期|开学时间|开学)[^\n]{0,60}?((?:20\d{2})\s*(?:年|[-\/.－—–])\s*\d{1,2}\s*(?:月|[-\/.－—–])\s*\d{1,2}\s*日?)/,
                        /(?:学期开始日期|学期开始时间|学期开始)[^\n]{0,60}?((?:20\d{2})\s*(?:年|[-\/.－—–])\s*\d{1,2}\s*(?:月|[-\/.－—–])\s*\d{1,2}\s*日?)/,
                        /(?:学期起始日期|学期起始时间|学期起始)[^\n]{0,60}?((?:20\d{2})\s*(?:年|[-\/.－—–])\s*\d{1,2}\s*(?:月|[-\/.－—–])\s*\d{1,2}\s*日?)/,
                        /(?:起始日期|开始日期)[^\n]{0,60}?((?:20\d{2})\s*(?:年|[-\/.－—–])\s*\d{1,2}\s*(?:月|[-\/.－—–])\s*\d{1,2}\s*日?)/
                    ];

                    for (
                        var patternIndex = 0;
                        patternIndex < startPatterns.length;
                        patternIndex++
                    ) {
                        var dateMatch =
                            allText.match(
                                startPatterns[
                                    patternIndex
                                ]
                            );

                        if (
                            dateMatch &&
                            dateMatch[1]
                        ) {
                            startDate =
                                normalizeDate(
                                    dateMatch[1]
                                );

                            if (startDate) {
                                break;
                            }
                        }
                    }
                }


                return JSON.stringify({
                    label:
                        semesterLabel,
                    startDate:
                        startDate
                });

            } catch (error) {

                return JSON.stringify({
                    label: "",
                    startDate: ""
                });
            }

        })();
        """.trimIndent()
}

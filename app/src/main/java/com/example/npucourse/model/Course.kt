package com.example.npucourse.model

import androidx.compose.ui.graphics.Color


/*
 * =========================================================
 * 周次类型
 * =========================================================
 *
 * EVERY
 * 普通连续周
 * 例如：1-16周
 *
 * ODD
 * 单周
 * 例如：1-15周单周
 *
 * EVEN
 * 双周
 * 例如：2-16周双周
 *
 * CUSTOM
 * 自定义非连续周
 * 例如：1,3,5,8,10周
 */

enum class WeekMode {

    EVERY,

    ODD,

    EVEN,

    CUSTOM;


    companion object {

        fun fromStorage(
            value: String
        ): WeekMode {

            return entries
                .firstOrNull {

                    it.name ==
                            value
                }
                ?: EVERY
        }
    }
}


/*
 * =========================================================
 * UI 使用的课程模型
 * =========================================================
 */

data class DemoCourse(

    val id: Long,

    val name: String,

    val room: String,

    val teacher: String,


    /*
     * 星期
     *
     * 1 = 周一
     * ...
     * 7 = 周日
     */
    val day: Int,


    /*
     * 节次
     */
    val startSection: Int,

    val endSection: Int,


    /*
     * 周次范围。
     *
     * 对普通周：
     * 直接表示 1-16 周。
     *
     * 对单双周：
     * 表示单双周的范围。
     *
     * 对 CUSTOM：
     * 表示自定义周次的最小/最大范围。
     */
    val startWeek: Int,

    val endWeek: Int,


    /*
     * 课程颜色
     */
    val color: Color,


    /*
     * 新增：
     * 周次类型。
     *
     * 给默认值的原因：
     * 以前所有 DemoCourse(...) 的代码
     * 即使不传这个参数仍然可以正常工作。
     */
    val weekMode: WeekMode =
        WeekMode.EVERY,


    /*
     * 新增：
     * 自定义周次。
     *
     * 数据格式使用：
     *
     * 1,3,5,8,10
     *
     * 只有 WeekMode.CUSTOM 时才使用。
     */
    val customWeeks: String =
        "",


    /*
     * 所属课表 / 学期。
     */
    val semesterId: Long =
        1L,

    val notes: String = "",

    val reminderEnabled: Boolean = true,

    /** -1 = 跟随全局提醒提前时间。 */
    val reminderMinutesOverride: Int = -1
)


/*
 * =========================================================
 * 演示课程
 * =========================================================
 */

val demoCourses =
    listOf(

        DemoCourse(
            id = 1L,
            name = "高等数学",
            room = "A100",
            teacher = "张老师",
            day = 1,
            startSection = 1,
            endSection = 2,
            startWeek = 1,
            endWeek = 16,
            color = Color(0xFF6377F4)
        ),

        DemoCourse(
            id = 2L,
            name = "大学物理",
            room = "B201",
            teacher = "李老师",
            day = 1,
            startSection = 7,
            endSection = 8,
            startWeek = 1,
            endWeek = 16,
            color = Color(0xFF50A487)
        ),

        DemoCourse(
            id = 3L,
            name = "程序设计基础",
            room = "301",
            teacher = "王老师",
            day = 2,
            startSection = 3,
            endSection = 4,
            startWeek = 1,
            endWeek = 16,
            color = Color(0xFF8A69D4)
        ),

        DemoCourse(
            id = 4L,
            name = "大学英语",
            room = "B205",
            teacher = "陈老师",
            day = 3,
            startSection = 1,
            endSection = 2,
            startWeek = 1,
            endWeek = 16,
            color = Color(0xFFE58A5D)
        ),

        DemoCourse(
            id = 5L,
            name = "工程图学",
            room = "C302",
            teacher = "赵老师",
            day = 4,
            startSection = 5,
            endSection = 6,
            startWeek = 1,
            endWeek = 8,
            color = Color(0xFF4F95CA)
        ),

        DemoCourse(
            id = 6L,
            name = "线性代数",
            room = "A203",
            teacher = "刘老师",
            day = 5,
            startSection = 3,
            endSection = 4,
            startWeek = 1,
            endWeek = 16,
            color = Color(0xFFD3698D)
        )
    )


/*
 * =========================================================
 * 判断某门课在指定周是否上课
 * =========================================================
 *
 * 后面：
 *
 * TimetablePage
 * TodayPage
 * CourseAlarmScheduler
 *
 * 都统一调用这个函数。
 *
 * 不再自己写：
 *
 * week in startWeek..endWeek
 */

fun DemoCourse.isActiveInWeek(
    week: Int
): Boolean {


    if (
        week !in 1..20
    ) {

        return false
    }


    val safeStartWeek =
        startWeek.coerceIn(
            1,
            20
        )


    val safeEndWeek =
        endWeek.coerceIn(
            safeStartWeek,
            20
        )


    return when (
        weekMode
    ) {


        /*
         * 普通连续周
         */
        WeekMode.EVERY -> {

            week in
                    safeStartWeek..
                    safeEndWeek
        }


        /*
         * 单周
         */
        WeekMode.ODD -> {

            week in
                    safeStartWeek..
                    safeEndWeek &&

                    week % 2 == 1
        }


        /*
         * 双周
         */
        WeekMode.EVEN -> {

            week in
                    safeStartWeek..
                    safeEndWeek &&

                    week % 2 == 0
        }


        /*
         * 自定义周
         */
        WeekMode.CUSTOM -> {

            week in
                    parseCustomWeeks(
                        customWeeks
                    )
        }
    }
}


/*
 * =========================================================
 * 获得课程真正包含的所有周次
 * =========================================================
 */

fun DemoCourse.activeWeeks():
        List<Int> {


    return when (
        weekMode
    ) {


        WeekMode.EVERY -> {

            safeWeekRange()
                .toList()
        }


        WeekMode.ODD -> {

            safeWeekRange()
                .filter {

                    it % 2 == 1
                }
        }


        WeekMode.EVEN -> {

            safeWeekRange()
                .filter {

                    it % 2 == 0
                }
        }


        WeekMode.CUSTOM -> {

            parseCustomWeeks(
                customWeeks
            )
        }
    }
}


/*
 * =========================================================
 * 课程周次显示文字
 * =========================================================
 *
 * 示例：
 *
 * 1–16周
 *
 * 1–15周 · 单周
 *
 * 2–16周 · 双周
 *
 * 1,3,5,8–10周
 */

fun DemoCourse.weekDisplayText():
        String {


    val safeStartWeek =
        startWeek.coerceIn(
            1,
            20
        )


    val safeEndWeek =
        endWeek.coerceIn(
            safeStartWeek,
            20
        )


    return when (
        weekMode
    ) {


        WeekMode.EVERY -> {

            safeStartWeek
                .toString() +
                    "–" +
                    safeEndWeek +
                    "周"
        }


        WeekMode.ODD -> {

            safeStartWeek
                .toString() +
                    "–" +
                    safeEndWeek +
                    "周 · 单周"
        }


        WeekMode.EVEN -> {

            safeStartWeek
                .toString() +
                    "–" +
                    safeEndWeek +
                    "周 · 双周"
        }


        WeekMode.CUSTOM -> {


            val weeks =
                parseCustomWeeks(
                    customWeeks
                )


            if (
                weeks.isEmpty()
            ) {

                "未设置周次"

            } else {

                formatWeekList(
                    weeks
                ) +
                        "周"
            }
        }
    }
}


/*
 * =========================================================
 * 自定义周次解析
 * =========================================================
 *
 * 支持输入：
 *
 * 1,3,5
 *
 * 1，3，5
 *
 * 1、3、5
 *
 * 1-4,7,9
 *
 * 1～4，7，9
 *
 * 1–4
 *
 * 最终统一转换成：
 *
 * [1, 2, 3, 4, 7, 9]
 */

fun parseCustomWeeks(
    input: String
): List<Int> {


    if (
        input.isBlank()
    ) {

        return emptyList()
    }


    val normalized =
        input
            .replace(
                "周",
                ""
            )
            .replace(
                " ",
                ""
            )
            .replace(
                "，",
                ","
            )
            .replace(
                "、",
                ","
            )
            .replace(
                "；",
                ","
            )
            .replace(
                ";",
                ","
            )
            .replace(
                "～",
                "-"
            )
            .replace(
                "~",
                "-"
            )
            .replace(
                "－",
                "-"
            )
            .replace(
                "—",
                "-"
            )
            .replace(
                "–",
                "-"
            )


    val result =
        mutableSetOf<Int>()


    normalized
        .split(
            ","
        )
        .forEach {
                token ->


            if (
                token.isBlank()
            ) {

                return@forEach
            }


            /*
             * 范围：
             *
             * 3-6
             */
            if (
                "-" in token
            ) {


                val parts =
                    token.split(
                        "-",
                        limit = 2
                    )


                if (
                    parts.size == 2
                ) {


                    val first =
                        parts[0]
                            .toIntOrNull()


                    val last =
                        parts[1]
                            .toIntOrNull()


                    if (
                        first != null &&
                        last != null
                    ) {


                        val rangeStart =
                            minOf(
                                first,
                                last
                            )


                        val rangeEnd =
                            maxOf(
                                first,
                                last
                            )


                        for (
                        week in
                        rangeStart..
                                rangeEnd
                        ) {


                            if (
                                week in 1..20
                            ) {

                                result.add(
                                    week
                                )
                            }
                        }
                    }
                }

            } else {


                /*
                 * 单个周次：
                 *
                 * 5
                 */

                val week =
                    token
                        .toIntOrNull()


                if (
                    week != null &&
                    week in 1..20
                ) {

                    result.add(
                        week
                    )
                }
            }
        }


    return result
        .sorted()
}


/*
 * =========================================================
 * 数据库保存前规范化自定义周次
 * =========================================================
 *
 * 输入：
 *
 * 1-3，5，7
 *
 * 数据库统一保存为：
 *
 * 1,2,3,5,7
 */

fun canonicalCustomWeeks(
    input: String
): String {


    return parseCustomWeeks(
        input
    )
        .joinToString(
            separator = ","
        )
}


/*
 * =========================================================
 * 安全周次范围
 * =========================================================
 */

private fun DemoCourse.safeWeekRange():
        IntRange {


    val start =
        startWeek.coerceIn(
            1,
            20
        )


    val end =
        endWeek.coerceIn(
            start,
            20
        )


    return start..end
}


/*
 * =========================================================
 * 周次列表压缩为易读文字
 * =========================================================
 *
 * 输入：
 *
 * [1, 2, 3, 5, 8, 9, 10]
 *
 * 输出：
 *
 * 1–3,5,8–10
 */

private fun formatWeekList(
    weeks: List<Int>
): String {


    if (
        weeks.isEmpty()
    ) {

        return ""
    }


    val sorted =
        weeks
            .distinct()
            .sorted()


    val parts =
        mutableListOf<String>()


    var rangeStart =
        sorted.first()


    var previous =
        sorted.first()


    for (
    index in 1 until
            sorted.size
    ) {


        val current =
            sorted[index]


        /*
         * 仍然连续。
         */
        if (
            current ==
            previous + 1
        ) {

            previous =
                current

            continue
        }


        parts.add(
            formatWeekRange(
                rangeStart,
                previous
            )
        )


        rangeStart =
            current


        previous =
            current
    }


    /*
     * 最后一段。
     */
    parts.add(
        formatWeekRange(
            rangeStart,
            previous
        )
    )


    return parts.joinToString(
        separator = ","
    )
}


/*
 * =========================================================
 * 格式化一个连续范围
 * =========================================================
 */

private fun formatWeekRange(
    start: Int,
    end: Int
): String {


    return if (
        start == end
    ) {

        start.toString()

    } else {

        start
            .toString() +
                "–" +
                end
    }
}
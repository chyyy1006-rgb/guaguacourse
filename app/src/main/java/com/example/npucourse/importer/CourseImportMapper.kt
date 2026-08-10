package com.example.npucourse.importer

import androidx.compose.ui.graphics.Color
import com.example.npucourse.model.DemoCourse
import com.example.npucourse.model.WeekMode
import com.example.npucourse.model.canonicalCustomWeeks
import com.example.npucourse.model.parseCustomWeeks


/*
 * =========================================================
 * 一次导入的结果
 * =========================================================
 *
 * courses：
 * 成功解析出来的课程。
 *
 * errors：
 * 无法解析的记录。
 *
 * warnings：
 * 可以导入，但存在需要关注的信息。
 */

data class CourseImportResult(

    val courses: List<DemoCourse>,

    val errors: List<String>,

    val warnings: List<String>
) {

    /*
     * 是否完全成功。
     */
    val isSuccess: Boolean
        get() =
            errors.isEmpty()


    /*
     * 成功导入数量。
     */
    val successCount: Int
        get() =
            courses.size


    /*
     * 错误数量。
     */
    val errorCount: Int
        get() =
            errors.size
}


/*
 * =========================================================
 * 教务课程 → 本地课程
 * =========================================================
 */

object CourseImportMapper {


    /*
     * =====================================================
     * 批量转换
     * =====================================================
     */

    fun map(
        records: List<EduCourseRecord>,
        defaultCampus: String? = null
    ): CourseImportResult {


        val courses =
            mutableListOf<DemoCourse>()


        val errors =
            mutableListOf<String>()


        val warnings =
            mutableListOf<String>()


        records.forEachIndexed {
                index,
                record ->


            val result =
                mapSingle(
                    record =
                        record,

                    defaultCampus =
                        defaultCampus
                )


            if (
                result.course != null
            ) {

                courses.add(
                    result.course
                )
            }


            result.error?.let {
                    error ->


                errors.add(
                    "第" +
                            (index + 1) +
                            "条：" +
                            error
                )
            }


            result.warning?.let {
                    warning ->


                warnings.add(
                    "第" +
                            (index + 1) +
                            "条：" +
                            warning
                )
            }
        }


        return CourseImportResult(
            courses =
                courses,

            errors =
                errors,

            warnings =
                warnings
        )
    }


    /*
     * =====================================================
     * 转换单门课程
     * =====================================================
     */

    private fun mapSingle(
        record: EduCourseRecord,
        defaultCampus: String?
    ): SingleMapResult {


        /*
         * =============================
         * 课程名称
         * =============================
         */

        val name =
            record.courseName
                .trim()


        if (
            name.isBlank()
        ) {

            return SingleMapResult(
                error =
                    "课程名称为空"
            )
        }


        /*
         * =============================
         * 星期检查
         * =============================
         */

        if (
            record.weekday !in 1..7
        ) {

            return SingleMapResult(
                error =
                    "星期值无效：" +
                            record.weekday
            )
        }


        /*
         * =============================
         * 节次检查
         * =============================
         */

        if (
            record.startSection !in 1..13
        ) {

            return SingleMapResult(
                error =
                    "开始节次无效：" +
                            record.startSection
            )
        }


        if (
            record.endSection !in 1..13
        ) {

            return SingleMapResult(
                error =
                    "结束节次无效：" +
                            record.endSection
            )
        }


        if (
            record.endSection <
            record.startSection
        ) {

            return SingleMapResult(
                error =
                    "结束节次不能早于开始节次"
            )
        }


        /*
         * =============================
         * 周次
         * =============================
         */

        val weekResult =
            parseWeekRule(
                record.weekText
            )


        if (
            weekResult == null
        ) {

            return SingleMapResult(
                error =
                    "无法识别周次：" +
                            record.weekText
            )
        }


        /*
         * =============================
         * 校区检查
         * =============================
         *
         * 当前 DemoCourse 仍然使用 App 全局校区。
         *
         * 所以如果教务记录明确给出了一个
         * 与当前默认校区不同的校区，
         * 暂时产生 warning。
         *
         * 后续我们会把课程模型升级成：
         *
         * 每门课程可以拥有自己的校区。
         */

        var warning:
                String? =
            null


        val sourceCampus =
            record.campus
                ?.trim()
                ?.takeIf {

                    it.isNotBlank()
                }


        if (
            sourceCampus != null &&
            defaultCampus != null &&
            sourceCampus !=
            defaultCampus
        ) {

            warning =
                "课程校区为 " +
                        sourceCampus +
                        "，当前 App 默认校区为 " +
                        defaultCampus
        }


        /*
         * =============================
         * 创建本地课程
         * =============================
         */

        val course =
            DemoCourse(

                /*
                 * 真正写入 Room 时，
                 * Repository 会重新使用自动生成 ID。
                 */
                id =
                    0L,

                name =
                    name,

                room =
                    record.room
                        .trim()
                        .ifBlank {

                            "未填写"
                        },

                teacher =
                    record.teacher
                        .trim()
                        .ifBlank {

                            "未填写"
                        },

                day =
                    record.weekday,

                startSection =
                    record.startSection,

                endSection =
                    record.endSection,

                startWeek =
                    weekResult.startWeek,

                endWeek =
                    weekResult.endWeek,

                color =
                    pickCourseColor(
                        name
                    ),

                weekMode =
                    weekResult.weekMode,

                customWeeks =
                    weekResult.customWeeks
            )


        return SingleMapResult(
            course =
                course,

            warning =
                warning
        )
    }


    /*
     * =====================================================
     * 解析教务周次文字
     * =====================================================
     *
     * 支持：
     *
     * 1-16周
     *
     * 1～16周
     *
     * 第1-16周
     *
     * 1-15周(单)
     *
     * 1-15周（单周）
     *
     * 2-16周(双)
     *
     * 1,3,5,8-10周
     */

    private fun parseWeekRule(
        source: String
    ): WeekParseResult? {


        if (
            source.isBlank()
        ) {

            return null
        }


        val original =
            source.trim()


        /*
         * 是否单周。
         */
        val isOdd =
            original.contains(
                "单"
            )


        /*
         * 是否双周。
         */
        val isEven =
            original.contains(
                "双"
            )


        /*
         * 单、双同时出现，
         * 说明原始数据异常。
         */
        if (
            isOdd &&
            isEven
        ) {

            return null
        }


        /*
         * 去除教务系统可能加入的描述文字。
         *
         * 最终留下：
         *
         * 1-16
         *
         * 1,3,5,8-10
         */

        val cleanText =
            original

                .replace(
                    "单周",
                    ""
                )

                .replace(
                    "双周",
                    ""
                )

                .replace(
                    "单双周",
                    ""
                )

                .replace(
                    "单",
                    ""
                )

                .replace(
                    "双",
                    ""
                )

                .replace(
                    "第",
                    ""
                )

                .replace(
                    "周次",
                    ""
                )

                .replace(
                    "周",
                    ""
                )

                .replace(
                    "(",
                    ""
                )

                .replace(
                    ")",
                    ""
                )

                .replace(
                    "（",
                    ""
                )

                .replace(
                    "）",
                    ""
                )

                .trim()


        /*
         * 使用我们 Course.kt 已经实现的
         * 通用周次解析器。
         */

        val weeks =
            parseCustomWeeks(
                cleanText
            )


        if (
            weeks.isEmpty()
        ) {

            return null
        }


        val firstWeek =
            weeks.first()


        val lastWeek =
            weeks.last()


        /*
         * =============================
         * 单周
         * =============================
         */

        if (
            isOdd
        ) {

            return WeekParseResult(
                startWeek =
                    firstWeek,

                endWeek =
                    lastWeek,

                weekMode =
                    WeekMode.ODD,

                customWeeks =
                    ""
            )
        }


        /*
         * =============================
         * 双周
         * =============================
         */

        if (
            isEven
        ) {

            return WeekParseResult(
                startWeek =
                    firstWeek,

                endWeek =
                    lastWeek,

                weekMode =
                    WeekMode.EVEN,

                customWeeks =
                    ""
            )
        }


        /*
         * =============================
         * 判断是不是完整连续范围
         * =============================
         */

        val expectedContinuousWeeks =
            (
                    firstWeek..
                            lastWeek
                    )
                .toList()


        val isContinuous =
            weeks ==
                    expectedContinuousWeeks


        if (
            isContinuous
        ) {

            /*
             * 普通每周课程。
             */
            return WeekParseResult(

                startWeek =
                    firstWeek,

                endWeek =
                    lastWeek,

                weekMode =
                    WeekMode.EVERY,

                customWeeks =
                    ""
            )
        }


        /*
         * =============================
         * 非连续周次
         * =============================
         */

        return WeekParseResult(

            startWeek =
                firstWeek,

            endWeek =
                lastWeek,

            weekMode =
                WeekMode.CUSTOM,

            customWeeks =
                canonicalCustomWeeks(
                    cleanText
                )
        )
    }


    /*
     * =====================================================
     * 自动课程颜色
     * =====================================================
     */

    private fun pickCourseColor(
        name: String
    ): Color {


        val colors =
            listOf(

                Color(
                    0xFF6377F4
                ),

                Color(
                    0xFF50A487
                ),

                Color(
                    0xFF8A69D4
                ),

                Color(
                    0xFFE58A5D
                ),

                Color(
                    0xFF4F95CA
                ),

                Color(
                    0xFFD3698D
                ),

                Color(
                    0xFFE2A94B
                ),

                Color(
                    0xFF55A0A6
                )
            )


        val index =
            name
                .hashCode()
                .ushr(
                    1
                ) %
                    colors.size


        return colors[
            index
        ]
    }
}


/* =========================================================
   单条转换内部结果
   ========================================================= */

private data class SingleMapResult(

    val course: DemoCourse? =
        null,

    val error: String? =
        null,

    val warning: String? =
        null
)


/* =========================================================
   周次解析内部结果
   ========================================================= */

private data class WeekParseResult(

    val startWeek: Int,

    val endWeek: Int,

    val weekMode: WeekMode,

    val customWeeks: String
)
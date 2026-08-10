package com.example.npucourse.util

import java.util.Calendar


/*
 * =========================================================
 * 已知学期起始日期解析
 * =========================================================
 *
 * 当前先维护已经确认准确的学期。
 *
 * 这样比根据“春季学期”盲猜一个日期更可靠。
 *
 * 后续做多学期功能后，这里可以逐渐扩充成
 * 西工大校历数据源。
 */

fun resolveSemesterStartMillis(
    semesterLabel: String
): Long? {

    if (
        semesterLabel.isBlank()
    ) {
        return null
    }


    val normalized =
        semesterLabel
            .replace(" ", "")
            .replace("—", "-")
            .replace("–", "-")
            .replace("－", "-")
            .replace("~", "-")
            .replace("～", "-")


    /*
     * =====================================================
     * 2025-2026 学年第二学期
     *
     * 用户确认：
     *
     * 2026-03-02 开始
     * =====================================================
     */

    val is2025To2026 =
        normalized.contains(
            "2025-2026"
        )


    val isSecondSemester =
        normalized.contains(
            "第二学期"
        ) ||
                normalized.contains(
                    "第2学期"
                ) ||
                normalized.contains(
                    "春季学期"
                ) ||
                normalized.contains(
                    "春季"
                )


    if (
        is2025To2026 &&
        isSecondSemester
    ) {

        return createDateMillis(
            year = 2026,
            month = Calendar.MARCH,
            day = 2
        )
    }


    /*
     * 暂时不对未知学期进行猜测。
     *
     * 避免错误修改用户的整个课表日期。
     */
    return null
}


/*
 * =========================================================
 * 创建当天 00:00
 * =========================================================
 */

private fun createDateMillis(
    year: Int,
    month: Int,
    day: Int
): Long {

    return Calendar
        .getInstance()
        .apply {

            set(
                year,
                month,
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
}
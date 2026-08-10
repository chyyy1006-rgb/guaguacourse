package com.example.npucourse.util

import java.util.Calendar


data class SectionTime(
    val section: Int,
    val startTime: String,
    val endTime: String
)


object CampusType {

    const val CHANGAN =
        "CHANGAN"

    const val YOUYI =
        "YOUYI"
}


/*
 * =========================
 * 长安校区
 * =========================
 */

private val changAnSchedule =
    listOf(

        SectionTime(
            1,
            "08:30",
            "09:15"
        ),

        SectionTime(
            2,
            "09:25",
            "10:10"
        ),

        SectionTime(
            3,
            "10:30",
            "11:15"
        ),

        SectionTime(
            4,
            "11:25",
            "12:10"
        ),

        SectionTime(
            5,
            "12:20",
            "13:05"
        ),

        SectionTime(
            6,
            "13:05",
            "13:50"
        ),

        SectionTime(
            7,
            "14:00",
            "14:45"
        ),

        SectionTime(
            8,
            "14:55",
            "15:40"
        ),

        SectionTime(
            9,
            "16:00",
            "16:45"
        ),

        SectionTime(
            10,
            "16:55",
            "17:40"
        ),

        SectionTime(
            11,
            "19:00",
            "19:45"
        ),

        SectionTime(
            12,
            "19:55",
            "20:40"
        ),

        SectionTime(
            13,
            "20:40",
            "21:25"
        )
    )


/*
 * =========================
 * 友谊校区 · 夏季
 * =========================
 */

private val youYiSummerSchedule =
    listOf(

        SectionTime(
            1,
            "08:00",
            "08:50"
        ),

        SectionTime(
            2,
            "09:00",
            "09:50"
        ),

        SectionTime(
            3,
            "10:10",
            "11:00"
        ),

        SectionTime(
            4,
            "11:10",
            "12:00"
        ),

        SectionTime(
            5,
            "12:20",
            "13:05"
        ),

        SectionTime(
            6,
            "13:05",
            "13:50"
        ),

        SectionTime(
            7,
            "14:30",
            "15:20"
        ),

        SectionTime(
            8,
            "15:30",
            "16:20"
        ),

        SectionTime(
            9,
            "16:40",
            "17:30"
        ),

        SectionTime(
            10,
            "17:40",
            "18:30"
        ),

        SectionTime(
            11,
            "19:30",
            "20:20"
        ),

        SectionTime(
            12,
            "20:30",
            "21:20"
        )
    )


/*
 * =========================
 * 友谊校区 · 冬季
 * =========================
 */

private val youYiWinterSchedule =
    listOf(

        SectionTime(
            1,
            "08:00",
            "08:50"
        ),

        SectionTime(
            2,
            "09:00",
            "09:50"
        ),

        SectionTime(
            3,
            "10:10",
            "11:00"
        ),

        SectionTime(
            4,
            "11:10",
            "12:00"
        ),

        SectionTime(
            5,
            "12:20",
            "13:05"
        ),

        SectionTime(
            6,
            "13:05",
            "13:50"
        ),

        SectionTime(
            7,
            "14:00",
            "14:50"
        ),

        SectionTime(
            8,
            "15:00",
            "15:50"
        ),

        SectionTime(
            9,
            "16:10",
            "17:00"
        ),

        SectionTime(
            10,
            "17:10",
            "18:00"
        ),

        SectionTime(
            11,
            "19:00",
            "19:50"
        ),

        SectionTime(
            12,
            "20:00",
            "20:50"
        )
    )


/*
 * 判断友谊校区当前使用夏季还是冬季作息。
 *
 * 夏季：
 * 5月1日 ～ 9月30日
 */

fun isYouYiSummerTime(): Boolean {

    val calendar =
        Calendar.getInstance()

    val month =
        calendar.get(
            Calendar.MONTH
        ) + 1


    return month in 5..9
}


/*
 * 根据校区返回节次表。
 */

fun getScheduleForCampus(
    campus: String
): List<SectionTime> {

    return when (campus) {

        CampusType.YOUYI -> {

            if (
                isYouYiSummerTime()
            ) {

                youYiSummerSchedule

            } else {

                youYiWinterSchedule
            }
        }

        else -> {

            changAnSchedule
        }
    }
}


/*
 * 获取某一节开始时间。
 */

fun getSectionStartTime(
    campus: String,
    section: Int
): String {

    return getScheduleForCampus(
        campus
    )
        .firstOrNull {

            it.section ==
                    section
        }
        ?.startTime
        ?: "--:--"
}


/*
 * 获取某节完整时间。
 */

fun getSectionTimeText(
    campus: String,
    section: Int
): String {

    val item =
        getScheduleForCampus(
            campus
        )
            .firstOrNull {

                it.section ==
                        section
            }


    return if (
        item == null
    ) {

        ""

    } else {

        "${item.startTime}-${item.endTime}"
    }
}


/*
 * 一个校区最多有多少节。
 */

fun getMaxSection(
    campus: String
): Int {

    return getScheduleForCampus(
        campus
    ).size
}


/*
 * UI显示名称。
 */

fun campusDisplayName(
    campus: String
): String {

    return when (campus) {

        CampusType.YOUYI ->
            "友谊校区"

        else ->
            "长安校区"
    }
}
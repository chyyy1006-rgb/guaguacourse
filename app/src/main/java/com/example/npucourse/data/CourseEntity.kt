package com.example.npucourse.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(
    tableName = "courses"
)
data class CourseEntity(

    /*
     * Room 自动生成主键。
     */
    @PrimaryKey(
        autoGenerate = true
    )
    val id: Long =
        0L,


    /*
     * 课程名称
     */
    val name: String,


    /*
     * 教室
     */
    val room: String,


    /*
     * 教师
     */
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
     * 开始节次
     */
    val startSection: Int,


    /*
     * 结束节次
     */
    val endSection: Int,


    /*
     * 开始周
     */
    val startWeek: Int,


    /*
     * 结束周
     */
    val endWeek: Int,


    /*
     * Compose Color 转成 ARGB Int 保存。
     */
    val colorArgb: Int,


    /*
     * =====================================================
     * 数据库 V2 新增字段
     * =====================================================
     *
     * EVERY
     * ODD
     * EVEN
     * CUSTOM
     *
     * 以前数据库中的所有课程
     * 迁移以后默认都是 EVERY。
     */

    @ColumnInfo(
        defaultValue = "EVERY"
    )
    val weekMode: String =
        "EVERY",


    /*
     * CUSTOM 模式下使用。
     *
     * 例如：
     *
     * 1,3,5,8,10
     */

    @ColumnInfo(
        defaultValue = ""
    )
    val customWeeks: String =
        "",


    /*
     * 所属课表 / 学期。
     *
     * V2 → V3 迁移时，旧课程全部归入 semesterId = 1。
     */
    @ColumnInfo(
        defaultValue = "1"
    )
    val semesterId: Long =
        1L,

    /* V4：课程备注，仅本地保存。 */
    @ColumnInfo(defaultValue = "")
    val notes: String = "",

    /* V4：是否允许该课程发送提醒。 */
    @ColumnInfo(defaultValue = "1")
    val reminderEnabled: Boolean = true,

    /* V4：-1 表示跟随全局提前时间，>=0 表示课程专属提前分钟数。 */
    @ColumnInfo(defaultValue = "-1")
    val reminderMinutesOverride: Int = -1
)
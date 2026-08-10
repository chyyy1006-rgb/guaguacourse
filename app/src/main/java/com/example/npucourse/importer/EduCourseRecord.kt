package com.example.npucourse.importer


/*
 * =========================================================
 * 教务系统课程原始记录
 * =========================================================
 *
 * 这个类不是 Room Entity，
 * 也不是 Compose UI Model。
 *
 * 它的作用是作为：
 *
 * 翱翔教务
 *      ↓
 * WebView / 网络解析层
 *      ↓
 * EduCourseRecord
 *      ↓
 * CourseImportMapper
 *      ↓
 * DemoCourse
 *      ↓
 * Room
 *
 * 中间的标准数据格式。
 */

data class EduCourseRecord(

    /*
     * 教务系统内部课程 ID。
     *
     * 如果暂时拿不到，可以为 null。
     */
    val externalId: String? = null,


    /*
     * 课程名称。
     */
    val courseName: String,


    /*
     * 教师。
     */
    val teacher: String = "",


    /*
     * 教室。
     */
    val room: String = "",


    /*
     * 星期。
     *
     * 1 = 周一
     * ...
     * 7 = 周日
     */
    val weekday: Int,


    /*
     * 开始节次。
     */
    val startSection: Int,


    /*
     * 结束节次。
     */
    val endSection: Int,


    /*
     * 教务系统返回的原始周次文字。
     *
     * 例如：
     *
     * 1-16周
     *
     * 1-15周(单)
     *
     * 2-16周(双)
     *
     * 1,3,5,8-10周
     */
    val weekText: String,


    /*
     * 校区。
     *
     * 暂时作为导入元数据保留。
     *
     * 例如：
     *
     * CHANGAN
     * YOUYI
     *
     * null 表示原始数据没有明确给出。
     */
    val campus: String? = null
)
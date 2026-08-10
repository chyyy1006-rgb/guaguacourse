package com.example.npucourse.data

import androidx.compose.ui.graphics.Color
import androidx.room.withTransaction
import androidx.compose.ui.graphics.toArgb
import com.example.npucourse.model.DemoCourse
import com.example.npucourse.model.WeekMode
import com.example.npucourse.model.canonicalCustomWeeks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class CourseRepository(
    private val dao: CourseDao,
    private val taskDao: TaskDao,
    private val database: AppDatabase
) {


    /*
     * =====================================================
     * 数据库课程 → UI课程
     * =====================================================
     */

    val courses:
            Flow<List<DemoCourse>> =

        dao
            .observeAllCourses()
            .map {
                    entityList ->


                entityList.map {
                        entity ->


                    entity
                        .toDemoCourse()
                }
            }


    /*
     * =====================================================
     * 添加课程
     * =====================================================
     */

    suspend fun addCourse(
        course: DemoCourse
    ) {


        dao.insertCourse(
            course
                .toEntityForInsert()
        )
    }


    /*
     * =====================================================
     * 批量添加
     * =====================================================
     */

    suspend fun addCourses(
        courses: List<DemoCourse>
    ) {


        dao.insertCourses(

            courses.map {
                    course ->


                course
                    .toEntityForInsert()
            }
        )
    }


    /*
     * =====================================================
     * 替换某个学期的全部课程
     * =====================================================
     *
     * 用于：
     *
     * 智能同步
     * 完全覆盖
     *
     * 当前数据库结构不变，不需要 Migration。
     * 同步完成后 MainActivity 会根据 Flow 自动刷新课程和提醒。
     */
    suspend fun replaceSemesterCourses(
        semesterId: Long,
        courses: List<DemoCourse>
    ) {

        if (
            semesterId <= 0L
        ) {
            return
        }

        val entities =
            courses.map {
                    course ->

                course
                    .copy(
                        semesterId = semesterId
                    )
                    .toEntityForInsert()
            }

        database.withTransaction {
            // 覆盖同步会重新生成课程主键。待办本身保留，课程关联安全降级为“通用待办”，
            // 避免留下指向已删除课程的悬空 courseId。
            taskDao.clearCourseLinksForSemester(semesterId, System.currentTimeMillis())
            dao.deleteCoursesBySemesterId(semesterId)
            if (entities.isNotEmpty()) {
                dao.insertCourses(entities)
            }
        }
    }


    /*
     * =====================================================
     * 编辑课程
     * =====================================================
     */

    suspend fun updateCourse(
        course: DemoCourse
    ) {


        dao.updateCourse(
            course
                .toEntity()
        )
    }


    /*
     * =====================================================
     * 删除课程
     * =====================================================
     */

    suspend fun deleteCourse(
        courseId: Long
    ) {


        database.withTransaction {
            taskDao.clearCourseLink(courseId, System.currentTimeMillis())
            dao.deleteCourseById(courseId)
        }
    }


    /*
     * =====================================================
     * 课程数量
     * =====================================================
     */

    suspend fun countCourses():
            Int {


        return dao
            .countCourses()
    }
}


/* =========================================================
   Database → UI
   ========================================================= */

private fun CourseEntity.toDemoCourse():
        DemoCourse {


    return DemoCourse(

        id =
            id,

        name =
            name,

        room =
            room,

        teacher =
            teacher,

        day =
            day,

        startSection =
            startSection,

        endSection =
            endSection,

        startWeek =
            startWeek,

        endWeek =
            endWeek,

        color =
            Color(
                colorArgb
            ),

        /*
         * 数据库 String
         *
         * →
         *
         * Kotlin enum
         */
        weekMode =
            WeekMode
                .fromStorage(
                    weekMode
                ),

        customWeeks =
            customWeeks,

        semesterId =
            semesterId,

        notes = notes,

        reminderEnabled = reminderEnabled,

        reminderMinutesOverride = reminderMinutesOverride
    )
}


/* =========================================================
   新增课程
   UI → Database
   ========================================================= */

private fun DemoCourse.toEntityForInsert():
        CourseEntity {


    return CourseEntity(

        /*
         * 新增交给 Room 自动产生 ID。
         */
        id =
            0L,

        name =
            name,

        room =
            room,

        teacher =
            teacher,

        day =
            day,

        startSection =
            startSection,

        endSection =
            endSection,

        startWeek =
            startWeek,

        endWeek =
            endWeek,

        colorArgb =
            color.toArgb(),

        weekMode =
            weekMode.name,

        customWeeks =

            if (
                weekMode ==
                WeekMode.CUSTOM
            ) {

                canonicalCustomWeeks(
                    customWeeks
                )

            } else {

                ""
            },

        semesterId =
            semesterId,

        notes = notes.trim(),

        reminderEnabled = reminderEnabled,

        reminderMinutesOverride = reminderMinutesOverride
    )
}


/* =========================================================
   编辑课程
   UI → Database
   ========================================================= */

private fun DemoCourse.toEntity():
        CourseEntity {


    return CourseEntity(

        /*
         * 编辑必须保留原 ID。
         */
        id =
            id,

        name =
            name,

        room =
            room,

        teacher =
            teacher,

        day =
            day,

        startSection =
            startSection,

        endSection =
            endSection,

        startWeek =
            startWeek,

        endWeek =
            endWeek,

        colorArgb =
            color.toArgb(),

        weekMode =
            weekMode.name,

        customWeeks =

            if (
                weekMode ==
                WeekMode.CUSTOM
            ) {

                canonicalCustomWeeks(
                    customWeeks
                )

            } else {

                ""
            },

        semesterId =
            semesterId,

        notes = notes.trim(),

        reminderEnabled = reminderEnabled,

        reminderMinutesOverride = reminderMinutesOverride
    )
}
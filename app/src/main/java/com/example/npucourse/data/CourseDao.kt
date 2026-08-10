package com.example.npucourse.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow


@Dao
interface CourseDao {

    @Query(
        """
        SELECT *
        FROM courses
        ORDER BY day ASC, startSection ASC
        """
    )
    fun observeAllCourses():
        Flow<List<CourseEntity>>


    @Query(
        """
        SELECT *
        FROM courses
        WHERE semesterId = :semesterId
        ORDER BY day ASC, startSection ASC
        """
    )
    suspend fun getCoursesForSemester(
        semesterId: Long
    ): List<CourseEntity>


    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insertCourse(
        course: CourseEntity
    ): Long


    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insertCourses(
        courses: List<CourseEntity>
    )


    @Update
    suspend fun updateCourse(
        course: CourseEntity
    )


    @Query(
        "DELETE FROM courses WHERE id = :courseId"
    )
    suspend fun deleteCourseById(
        courseId: Long
    )


    @Query(
        "DELETE FROM courses WHERE semesterId = :semesterId"
    )
    suspend fun deleteCoursesBySemesterId(
        semesterId: Long
    )


    @Transaction
    suspend fun replaceCoursesForSemester(
        semesterId: Long,
        courses: List<CourseEntity>
    ) {
        deleteCoursesBySemesterId(semesterId)
        if (courses.isNotEmpty()) {
            insertCourses(courses)
        }
    }


    @Query(
        "SELECT COUNT(*) FROM courses"
    )
    suspend fun countCourses(): Int


    @Query(
        "SELECT * FROM courses ORDER BY semesterId ASC, day ASC, startSection ASC"
    )
    suspend fun getAllCoursesOnce():
        List<CourseEntity>


    @Query(
        "DELETE FROM courses"
    )
    suspend fun deleteAllCourses()
}

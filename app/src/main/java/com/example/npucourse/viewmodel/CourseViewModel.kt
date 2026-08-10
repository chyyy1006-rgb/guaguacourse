package com.example.npucourse.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.npucourse.data.AppDatabase
import com.example.npucourse.data.CourseRepository
import com.example.npucourse.model.DemoCourse
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class CourseViewModel(
    private val repository: CourseRepository
) : ViewModel() {

    /*
     * 所有学期的课程都保存在这里。
     *
     * MainActivity / AcademicPage 根据 semesterId 过滤。
     */
    val courses:
        StateFlow<List<DemoCourse>> =
        repository
            .courses
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )


    fun addCourse(
        course: DemoCourse
    ) {
        viewModelScope.launch {
            repository.addCourse(
                course
            )
        }
    }


    fun importCourses(
        courses: List<DemoCourse>,
        semesterId: Long
    ) {

        if (
            courses.isEmpty() ||
            semesterId <= 0L
        ) {
            return
        }

        viewModelScope.launch {
            repository.addCourses(
                courses.map {
                    it.copy(
                        semesterId = semesterId
                    )
                }
            )
        }
    }


    fun replaceSemesterCourses(
        courses: List<DemoCourse>,
        semesterId: Long
    ) {

        if (
            semesterId <= 0L
        ) {
            return
        }

        viewModelScope.launch {
            repository.replaceSemesterCourses(
                semesterId = semesterId,
                courses = courses
            )
        }
    }


    fun updateCourse(
        course: DemoCourse
    ) {
        viewModelScope.launch {
            repository.updateCourse(
                course
            )
        }
    }


    fun deleteCourse(
        courseId: Long
    ) {
        viewModelScope.launch {
            repository.deleteCourse(
                courseId
            )
        }
    }


    class Factory(
        context: Context
    ) : ViewModelProvider.Factory {

        private val applicationContext =
            context.applicationContext

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>
        ): T {

            val database =
                AppDatabase.getInstance(
                    applicationContext
                )

            val repository =
                CourseRepository(
                    dao = database.courseDao(),
                    taskDao = database.taskDao(),
                    database = database
                )

            return CourseViewModel(
                repository
            ) as T
        }
    }
}

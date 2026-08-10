package com.example.npucourse.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.npucourse.data.AppDatabase
import com.example.npucourse.data.SemesterRepository
import com.example.npucourse.model.Semester
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class SemesterViewModel(
    private val repository: SemesterRepository
) : ViewModel() {

    val semesters:
        StateFlow<List<Semester>> =
        repository
            .semesters
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )


    init {
        viewModelScope.launch {
            repository.ensureDefaultSemester()
        }
    }


    fun findOrCreateSemester(
        name: String,
        startMillis: Long?,
        campus: String,
        onReady: (Semester) -> Unit
    ) {
        viewModelScope.launch {
            val semester =
                repository.findOrCreateSemester(
                    name = name,
                    startMillis = startMillis,
                    campus = campus
                )
            onReady(semester)
        }
    }


    fun createSemester(
        name: String,
        startMillis: Long,
        campus: String,
        onReady: (Semester) -> Unit
    ) {
        viewModelScope.launch {
            val semester =
                repository.createSemester(
                    name = name,
                    startMillis = startMillis,
                    campus = campus
                )

            onReady(semester)
        }
    }


    fun renameSemester(
        semesterId: Long,
        newName: String,
        onReady: (Semester?) -> Unit = {}
    ) {
        viewModelScope.launch {
            onReady(
                repository.renameSemester(
                    semesterId = semesterId,
                    newName = newName
                )
            )
        }
    }


    fun duplicateSemester(
        sourceSemesterId: Long,
        requestedName: String,
        onReady: (Semester?) -> Unit
    ) {
        viewModelScope.launch {
            onReady(
                repository.duplicateSemester(
                    sourceSemesterId = sourceSemesterId,
                    requestedName = requestedName
                )
            )
        }
    }


    fun deleteSemester(
        semesterId: Long,
        onDone: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            onDone(
                repository.deleteSemester(
                    semesterId
                )
            )
        }
    }


    fun updateStartMillis(
        semesterId: Long,
        startMillis: Long
    ) {
        viewModelScope.launch {
            repository.updateStartMillis(
                semesterId,
                startMillis
            )
        }
    }


    fun updateCampus(
        semesterId: Long,
        campus: String
    ) {
        viewModelScope.launch {
            repository.updateCampus(
                semesterId,
                campus
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
                SemesterRepository(
                    semesterDao = database.semesterDao(),
                    courseDao = database.courseDao(),
                    taskDao = database.taskDao(),
                    database = database
                )

            return SemesterViewModel(
                repository
            ) as T
        }
    }
}

package com.example.npucourse.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.npucourse.data.AppDatabase
import com.example.npucourse.data.TaskEntity
import com.example.npucourse.data.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    val tasks: StateFlow<List<TaskEntity>> =
        repository.tasks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addTask(task: TaskEntity) {
        if (task.semesterId <= 0L || task.title.isBlank()) return
        viewModelScope.launch { repository.addTask(task) }
    }

    fun updateTask(task: TaskEntity) {
        if (task.id <= 0L || task.title.isBlank()) return
        viewModelScope.launch { repository.updateTask(task) }
    }

    fun setCompleted(taskId: Long, completed: Boolean) {
        if (taskId <= 0L) return
        viewModelScope.launch { repository.setCompleted(taskId, completed) }
    }

    fun deleteTask(taskId: Long) {
        if (taskId <= 0L) return
        viewModelScope.launch { repository.deleteTask(taskId) }
    }

    class Factory(context: Context) : ViewModelProvider.Factory {
        private val appContext = context.applicationContext

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val database = AppDatabase.getInstance(appContext)
            return TaskViewModel(
                TaskRepository(database.taskDao())
            ) as T
        }
    }
}

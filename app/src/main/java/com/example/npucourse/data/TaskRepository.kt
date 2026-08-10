package com.example.npucourse.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val dao: TaskDao
) {
    val tasks: Flow<List<TaskEntity>> = dao.observeAllTasks()

    suspend fun addTask(task: TaskEntity) {
        val now = System.currentTimeMillis()
        dao.insertTask(
            task.copy(
                id = 0L,
                title = task.title.trim(),
                note = task.note.trim(),
                priority = task.priority.coerceIn(0, 2),
                reminderMinutesBefore = normalizeReminder(task),
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updateTask(task: TaskEntity) {
        dao.updateTask(
            task.copy(
                title = task.title.trim(),
                note = task.note.trim(),
                priority = task.priority.coerceIn(0, 2),
                reminderMinutesBefore = normalizeReminder(task),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun setCompleted(taskId: Long, completed: Boolean) {
        dao.setCompleted(taskId, completed, System.currentTimeMillis())
    }

    suspend fun deleteTask(taskId: Long) {
        dao.deleteTask(taskId)
    }

    private fun normalizeReminder(task: TaskEntity): Int {
        if (task.dueAt <= 0L) return -1
        val minutes = task.reminderMinutesBefore.coerceAtLeast(-1)
        // v4.7 中的 1 分钟仅用于内部快速测试，正式版本不再保留。
        return if (minutes == 1) -1 else minutes
    }
}

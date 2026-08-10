package com.example.npucourse.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query(
        """
        SELECT * FROM tasks
        ORDER BY completed ASC,
                 CASE WHEN dueAt = 0 THEN 1 ELSE 0 END ASC,
                 dueAt ASC,
                 priority DESC,
                 createdAt DESC
        """
    )
    fun observeAllTasks(): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE semesterId = :semesterId
        ORDER BY completed ASC,
                 CASE WHEN dueAt = 0 THEN 1 ELSE 0 END ASC,
                 dueAt ASC,
                 priority DESC,
                 createdAt DESC
        """
    )
    suspend fun getTasksForSemester(semesterId: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks ORDER BY semesterId ASC, createdAt ASC")
    suspend fun getAllTasksOnce(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET completed = :completed, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun setCompleted(
        taskId: Long,
        completed: Boolean,
        updatedAt: Long
    )

    @Query("UPDATE tasks SET courseId = NULL, updatedAt = :updatedAt WHERE courseId = :courseId")
    suspend fun clearCourseLink(
        courseId: Long,
        updatedAt: Long
    )

    @Query("UPDATE tasks SET courseId = NULL, updatedAt = :updatedAt WHERE semesterId = :semesterId")
    suspend fun clearCourseLinksForSemester(
        semesterId: Long,
        updatedAt: Long
    )


    @Query(
        "UPDATE tasks SET reminderMinutesBefore = -1, updatedAt = :updatedAt " +
            "WHERE reminderMinutesBefore = 1"
    )
    suspend fun disableLegacyOneMinuteTestReminders(updatedAt: Long): Int

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: Long)

    @Query("DELETE FROM tasks WHERE semesterId = :semesterId")
    suspend fun deleteTasksBySemesterId(semesterId: Long)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}

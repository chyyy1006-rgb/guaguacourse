package com.example.npucourse.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 与学期/课程关联的待办事项。
 *
 * dueAt = 0 表示未设置截止时间；courseId = null 表示学期级通用待办。
 */
@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["semesterId"]),
        Index(value = ["dueAt"])
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val semesterId: Long,
    val courseId: Long? = null,
    val title: String,
    val note: String = "",
    val dueAt: Long = 0L,
    /** -1 表示不提醒；其余值表示在 DDL 前多少分钟提醒一次。 */
    @ColumnInfo(defaultValue = "-1")
    val reminderMinutesBefore: Int = -1,
    val priority: Int = 1,
    @ColumnInfo(defaultValue = "0")
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

package com.example.npucourse.data

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(
    tableName = "semesters"
)
data class SemesterEntity(

    @PrimaryKey(
        autoGenerate = true
    )
    val id: Long = 0L,

    val name: String,

    val startMillis: Long,

    val campus: String,

    val createdAt: Long = System.currentTimeMillis()
)

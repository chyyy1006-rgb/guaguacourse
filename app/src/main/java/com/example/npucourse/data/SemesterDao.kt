package com.example.npucourse.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow


@Dao
interface SemesterDao {

    @Query(
        """
        SELECT *
        FROM semesters
        ORDER BY startMillis DESC, id DESC
        """
    )
    fun observeAllSemesters():
        Flow<List<SemesterEntity>>


    @Query(
        """
        SELECT *
        FROM semesters
        WHERE id = :semesterId
        LIMIT 1
        """
    )
    suspend fun getSemesterById(
        semesterId: Long
    ): SemesterEntity?


    @Query(
        """
        SELECT *
        FROM semesters
        WHERE name = :name
        LIMIT 1
        """
    )
    suspend fun getSemesterByName(
        name: String
    ): SemesterEntity?


    @Insert(
        onConflict = OnConflictStrategy.IGNORE
    )
    suspend fun insertSemester(
        semester: SemesterEntity
    ): Long


    @Query(
        """
        UPDATE semesters
        SET name = :name
        WHERE id = :semesterId
        """
    )
    suspend fun updateName(
        semesterId: Long,
        name: String
    )


    @Query(
        """
        UPDATE semesters
        SET startMillis = :startMillis
        WHERE id = :semesterId
        """
    )
    suspend fun updateStartMillis(
        semesterId: Long,
        startMillis: Long
    )


    @Query(
        """
        UPDATE semesters
        SET campus = :campus
        WHERE id = :semesterId
        """
    )
    suspend fun updateCampus(
        semesterId: Long,
        campus: String
    )


    @Query(
        "DELETE FROM semesters WHERE id = :semesterId"
    )
    suspend fun deleteSemester(
        semesterId: Long
    )


    @Query(
        "SELECT COUNT(*) FROM semesters"
    )
    suspend fun countSemesters(): Int


    @Query(
        "SELECT * FROM semesters ORDER BY startMillis DESC, id DESC"
    )
    suspend fun getAllSemestersOnce():
        List<SemesterEntity>


    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insertSemestersForRestore(
        semesters: List<SemesterEntity>
    )


    @Query(
        "DELETE FROM semesters"
    )
    suspend fun deleteAllSemesters()
}

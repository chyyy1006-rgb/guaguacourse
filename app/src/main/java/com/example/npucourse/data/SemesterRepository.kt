package com.example.npucourse.data

import androidx.room.withTransaction
import com.example.npucourse.model.Semester
import com.example.npucourse.util.resolveSemesterStartMillis
import com.example.npucourse.util.todayStartMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class SemesterRepository(
    private val semesterDao: SemesterDao,
    private val courseDao: CourseDao,
    private val taskDao: TaskDao,
    private val database: AppDatabase
) {

    val semesters:
        Flow<List<Semester>> =
        semesterDao
            .observeAllSemesters()
            .map { entities ->
                entities.map { it.toModel() }
            }


    suspend fun ensureDefaultSemester() {

        if (
            semesterDao.countSemesters() > 0
        ) {
            return
        }

        val defaultName =
            "2025-2026学年第二学期"

        semesterDao.insertSemester(
            SemesterEntity(
                name = defaultName,
                startMillis =
                    resolveSemesterStartMillis(
                        defaultName
                    ) ?: todayStartMillis(),
                campus = "CHANGAN"
            )
        )
    }


    suspend fun findOrCreateSemester(
        name: String,
        startMillis: Long?,
        campus: String
    ): Semester {

        val cleanName =
            name.trim()
                .ifBlank {
                    "未命名课表"
                }

        val existing =
            semesterDao.getSemesterByName(
                cleanName
            )

        if (
            existing != null
        ) {

            if (
                startMillis != null &&
                existing.startMillis != startMillis
            ) {
                semesterDao.updateStartMillis(
                    existing.id,
                    startMillis
                )
            }

            return Semester(
                id = existing.id,
                name = existing.name,
                startMillis = startMillis
                    ?: existing.startMillis,
                campus = existing.campus
            )
        }

        val actualStartMillis =
            startMillis
                ?: todayStartMillis()

        val newId =
            semesterDao.insertSemester(
                SemesterEntity(
                    name = cleanName,
                    startMillis = actualStartMillis,
                    campus = campus
                )
            )

        val actualId =
            if (
                newId > 0L
            ) {
                newId
            } else {
                semesterDao.getSemesterByName(
                    cleanName
                )?.id
                    ?: error(
                        "无法创建课表：$cleanName"
                    )
            }

        return Semester(
            id = actualId,
            name = cleanName,
            startMillis = actualStartMillis,
            campus = campus
        )
    }


    suspend fun createSemester(
        name: String,
        startMillis: Long,
        campus: String
    ): Semester {

        val uniqueName =
            makeUniqueName(
                baseName = name,
                exceptSemesterId = null
            )

        val newId =
            semesterDao.insertSemester(
                SemesterEntity(
                    name = uniqueName,
                    startMillis = startMillis,
                    campus = campus
                )
            )

        if (
            newId <= 0L
        ) {
            error(
                "无法创建课表：$uniqueName"
            )
        }

        return Semester(
            id = newId,
            name = uniqueName,
            startMillis = startMillis,
            campus = campus
        )
    }


    suspend fun renameSemester(
        semesterId: Long,
        newName: String
    ): Semester? {

        val existing =
            semesterDao.getSemesterById(
                semesterId
            ) ?: return null

        val uniqueName =
            makeUniqueName(
                baseName = newName,
                exceptSemesterId = semesterId
            )

        semesterDao.updateName(
            semesterId = semesterId,
            name = uniqueName
        )

        return existing
            .copy(
                name = uniqueName
            )
            .toModel()
    }


    suspend fun duplicateSemester(
        sourceSemesterId: Long,
        requestedName: String
    ): Semester? = database.withTransaction {

        val source =
            semesterDao.getSemesterById(
                sourceSemesterId
            ) ?: return@withTransaction null

        val uniqueName =
            makeUniqueName(
                baseName = requestedName,
                exceptSemesterId = null
            )

        val newId =
            semesterDao.insertSemester(
                SemesterEntity(
                    name = uniqueName,
                    startMillis = source.startMillis,
                    campus = source.campus
                )
            )

        if (newId <= 0L) {
            error("无法复制课表：$uniqueName")
        }

        val sourceCourses =
            courseDao.getCoursesForSemester(sourceSemesterId)

        val courseIdMap = mutableMapOf<Long, Long>()
        sourceCourses.forEach { course ->
            val insertedId = courseDao.insertCourse(
                course.copy(
                    id = 0L,
                    semesterId = newId
                )
            )
            courseIdMap[course.id] = insertedId
        }

        val sourceTasks =
            taskDao.getTasksForSemester(sourceSemesterId)

        if (sourceTasks.isNotEmpty()) {
            taskDao.insertTasks(
                sourceTasks.map { task ->
                    task.copy(
                        id = 0L,
                        semesterId = newId,
                        courseId = task.courseId?.let { courseIdMap[it] },
                        completed = false,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                }
            )
        }

        Semester(
            id = newId,
            name = uniqueName,
            startMillis = source.startMillis,
            campus = source.campus
        )
    }

    suspend fun deleteSemester(
        semesterId: Long
    ): Boolean = database.withTransaction {

        if (semesterDao.countSemesters() <= 1) {
            return@withTransaction false
        }

        val existing =
            semesterDao.getSemesterById(semesterId)
                ?: return@withTransaction false

        taskDao.deleteTasksBySemesterId(existing.id)
        courseDao.deleteCoursesBySemesterId(existing.id)
        semesterDao.deleteSemester(existing.id)

        true
    }

    suspend fun updateStartMillis(
        semesterId: Long,
        startMillis: Long
    ) {
        semesterDao.updateStartMillis(
            semesterId,
            startMillis
        )
    }


    suspend fun updateCampus(
        semesterId: Long,
        campus: String
    ) {
        semesterDao.updateCampus(
            semesterId,
            campus
        )
    }


    private suspend fun makeUniqueName(
        baseName: String,
        exceptSemesterId: Long?
    ): String {

        val cleanBase =
            baseName
                .trim()
                .ifBlank {
                    "新课表"
                }

        var candidate =
            cleanBase

        var suffix =
            2

        while (true) {

            val conflict =
                semesterDao.getSemesterByName(
                    candidate
                )

            if (
                conflict == null ||
                conflict.id == exceptSemesterId
            ) {
                return candidate
            }

            candidate =
                cleanBase +
                    " (" +
                    suffix +
                    ")"

            suffix++
        }
    }
}


private fun SemesterEntity.toModel(): Semester {
    return Semester(
        id = id,
        name = name,
        startMillis = startMillis,
        campus = campus
    )
}

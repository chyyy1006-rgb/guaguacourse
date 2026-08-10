package com.example.npucourse.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.Calendar


@Database(
    entities = [
        CourseEntity::class,
        SemesterEntity::class,
        TaskEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase :
    RoomDatabase() {

    abstract fun courseDao():
        CourseDao

    abstract fun semesterDao():
        SemesterDao

    abstract fun taskDao():
        TaskDao


    companion object {

        /*
         * =================================================
         * V1 → V2
         * =================================================
         */
        private val MIGRATION_1_2 =
            object : Migration(1, 2) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    database.execSQL(
                        """
                        ALTER TABLE courses
                        ADD COLUMN weekMode
                        TEXT NOT NULL
                        DEFAULT 'EVERY'
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        ALTER TABLE courses
                        ADD COLUMN customWeeks
                        TEXT NOT NULL
                        DEFAULT ''
                        """.trimIndent()
                    )
                }
            }


        /*
         * =================================================
         * V2 → V3：多课表 / 多学期
         * =================================================
         *
         * 旧数据库中的课程全部归入：
         *
         * 2025-2026学年第二学期
         * 开学日期：2026-03-02
         * semesterId = 1
         *
         * 因此现有真实课表不会丢失。
         */
        private val MIGRATION_2_3 =
            object : Migration(2, 3) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    val spring2026Start =
                        Calendar
                            .getInstance()
                            .apply {
                                set(
                                    2026,
                                    Calendar.MARCH,
                                    2,
                                    0,
                                    0,
                                    0
                                )
                                set(
                                    Calendar.MILLISECOND,
                                    0
                                )
                            }
                            .timeInMillis

                    val now =
                        System.currentTimeMillis()

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS semesters (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            name TEXT NOT NULL,
                            startMillis INTEGER NOT NULL,
                            campus TEXT NOT NULL,
                            createdAt INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        INSERT OR IGNORE INTO semesters
                        (id, name, startMillis, campus, createdAt)
                        VALUES
                        (1, '2025-2026学年第二学期', $spring2026Start, 'CHANGAN', $now)
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        ALTER TABLE courses
                        ADD COLUMN semesterId
                        INTEGER NOT NULL
                        DEFAULT 1
                        """.trimIndent()
                    )
                }
            }


        /*
         * =================================================
         * V3 → V4：课程待办 / DDL
         * =================================================
         */
        private val MIGRATION_3_4 =
            object : Migration(3, 4) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {
                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS tasks (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            semesterId INTEGER NOT NULL,
                            courseId INTEGER,
                            title TEXT NOT NULL,
                            note TEXT NOT NULL,
                            dueAt INTEGER NOT NULL,
                            priority INTEGER NOT NULL,
                            completed INTEGER NOT NULL DEFAULT 0,
                            createdAt INTEGER NOT NULL,
                            updatedAt INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    database.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_tasks_semesterId ON tasks(semesterId)"
                    )
                    database.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_tasks_dueAt ON tasks(dueAt)"
                    )

                    database.execSQL(
                        "ALTER TABLE courses ADD COLUMN notes TEXT NOT NULL DEFAULT ''"
                    )
                    database.execSQL(
                        "ALTER TABLE courses ADD COLUMN reminderEnabled INTEGER NOT NULL DEFAULT 1"
                    )
                    database.execSQL(
                        "ALTER TABLE courses ADD COLUMN reminderMinutesOverride INTEGER NOT NULL DEFAULT -1"
                    )
                }
            }


        /*
         * =================================================
         * V4 → V5：DDL 单次提前提醒
         * =================================================
         */
        private val MIGRATION_4_5 =
            object : Migration(4, 5) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL(
                        "ALTER TABLE tasks ADD COLUMN reminderMinutesBefore INTEGER NOT NULL DEFAULT -1"
                    )
                }
            }


        @Volatile
        private var INSTANCE:
            AppDatabase? = null


        fun getInstance(
            context: Context
        ): AppDatabase {

            return INSTANCE
                ?: synchronized(this) {

                    val instance =
                        Room
                            .databaseBuilder(
                                context.applicationContext,
                                AppDatabase::class.java,
                                "npu_course_database"
                            )
                            .addMigrations(
                                MIGRATION_1_2,
                                MIGRATION_2_3,
                                MIGRATION_3_4,
                                MIGRATION_4_5
                            )
                            .build()

                    INSTANCE = instance

                    instance
                }
        }
    }
}

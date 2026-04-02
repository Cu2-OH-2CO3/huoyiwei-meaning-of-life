package com.memoria.meaningoflife.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.memoria.meaningoflife.data.database.checkin.CheckinDao
import com.memoria.meaningoflife.data.database.checkin.CheckinEntity
import com.memoria.meaningoflife.data.database.diary.DiaryDao
import com.memoria.meaningoflife.data.database.diary.DiaryEntity
import com.memoria.meaningoflife.data.database.diary.DiaryTagEntity
import com.memoria.meaningoflife.data.database.lunch.DishDao
import com.memoria.meaningoflife.data.database.lunch.DishEntity
import com.memoria.meaningoflife.data.database.lunch.LotteryHistoryDao
import com.memoria.meaningoflife.data.database.lunch.LotteryHistoryEntity
import com.memoria.meaningoflife.data.database.painting.*
import com.memoria.meaningoflife.data.database.task.TaskDao
import com.memoria.meaningoflife.data.database.task.TaskEntity
import com.memoria.meaningoflife.data.database.task.TaskNodeDao
import com.memoria.meaningoflife.data.database.task.TaskNodeEntity

@Database(
    entities = [
        WorkEntity::class,
        NodeEntity::class,
        GoalEntity::class,
        DiaryEntity::class,
        DiaryTagEntity::class,
        DishEntity::class,
        LotteryHistoryEntity::class,
        CheckinEntity::class,
        TaskEntity::class,
        TaskNodeEntity::class
    ],
    version = 4,  // 版本号从 3 改为 4
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workDao(): WorkDao
    abstract fun nodeDao(): NodeDao
    abstract fun goalDao(): GoalDao
    abstract fun diaryDao(): DiaryDao
    abstract fun dishDao(): DishDao
    abstract fun lotteryHistoryDao(): LotteryHistoryDao
    abstract fun checkinDao(): CheckinDao
    abstract fun taskDao(): TaskDao
    abstract fun taskNodeDao(): TaskNodeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "huoyiwei.db"
                )
                    .addMigrations(MIGRATION_3_4)  // 添加迁移
                    .build().also { INSTANCE = it }
            }
        }

        // 数据库迁移：从版本3到版本4
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 由于 mood 和 weather 字段可能已经是可空的，这里不需要额外操作
                // 但如果需要确保字段存在，可以执行以下语句
                // database.execSQL("ALTER TABLE diaries ADD COLUMN mood INTEGER DEFAULT 2")
                // database.execSQL("ALTER TABLE diaries ADD COLUMN weather INTEGER DEFAULT 0")

                // 确保 task 和 task_nodes 表存在
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT,
                        isUrgent INTEGER NOT NULL DEFAULT 0,
                        isImportant INTEGER NOT NULL DEFAULT 0,
                        deadline INTEGER,
                        createdAt INTEGER NOT NULL,
                        completedAt INTEGER,
                        isDeleted INTEGER NOT NULL DEFAULT 0
                    )
                """)

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS task_nodes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        taskId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT,
                        deadline INTEGER,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        completedAt INTEGER,
                        sortOrder INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                """)
            }
        }
        

        // 如果不想保留数据，可以使用破坏性迁移
        // 在开发阶段可以使用 fallbackToDestructiveMigration()
        // 但正式版本应该使用 addMigrations()
    }
}
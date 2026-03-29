package com.memoria.meaningoflife.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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

@Database(
    entities = [
        WorkEntity::class,
        NodeEntity::class,
        GoalEntity::class,
        DiaryEntity::class,
        DiaryTagEntity::class,
        DishEntity::class,
        LotteryHistoryEntity::class,
        CheckinEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workDao(): WorkDao
    abstract fun nodeDao(): NodeDao
    abstract fun goalDao(): GoalDao
    abstract fun diaryDao(): DiaryDao
    abstract fun dishDao(): DishDao
    abstract fun lotteryHistoryDao(): LotteryHistoryDao
    abstract fun checkinDao(): CheckinDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "huoyiwei.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
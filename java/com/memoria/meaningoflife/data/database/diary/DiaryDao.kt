package com.memoria.meaningoflife.data.database.diary

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {

    @Query("SELECT * FROM diaries WHERE is_deleted = 0 ORDER BY created_date DESC")
    fun getAllDiaries(): Flow<List<DiaryEntity>>

    @Query("DELETE FROM diaries")
    fun deleteAll()



    @Query("SELECT * FROM diaries WHERE is_deleted = 0 ORDER BY created_date DESC")
    fun getAllDiariesSync(): List<DiaryEntity>

    @Query("SELECT * FROM diaries WHERE id = :diaryId AND is_deleted = 0")
    fun getDiaryById(diaryId: Long): DiaryEntity?

    @Query("SELECT * FROM diaries WHERE created_date = :date AND is_deleted = 0")
    fun getDiaryByDate(date: String): DiaryEntity?

    @Query("DELETE FROM diaries")
    fun deleteAllSync()

    @Query("SELECT * FROM diaries WHERE created_date BETWEEN :startDate AND :endDate AND is_deleted = 0 ORDER BY created_date DESC")
    fun getDiariesByDateRange(startDate: String, endDate: String): List<DiaryEntity>

    @Query("SELECT COUNT(*) FROM diaries WHERE created_date BETWEEN :startDate AND :endDate AND is_deleted = 0")
    fun getCountByDateRange(startDate: String, endDate: String): Int

    // 修改这里：返回 MoodStats 数据类列表，而不是 Map
    @Query("SELECT mood, COUNT(*) as count FROM diaries WHERE created_date BETWEEN :startDate AND :endDate AND is_deleted = 0 GROUP BY mood")
    fun getMoodStats(startDate: String, endDate: String): List<MoodStats>

    @Insert
    fun insertDiary(diary: DiaryEntity): Long

    @Update
    fun updateDiary(diary: DiaryEntity)

    @Query("UPDATE diaries SET is_deleted = 1, updated_time = :updatedTime WHERE id = :diaryId")
    fun softDeleteDiary(diaryId: Long, updatedTime: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM diaries WHERE created_date >= :date AND is_deleted = 0")
    fun getCountSinceDate(date: String): Int
}

// 添加数据类用于接收统计结果
data class MoodStats(
    val mood: Int,
    val count: Int
)
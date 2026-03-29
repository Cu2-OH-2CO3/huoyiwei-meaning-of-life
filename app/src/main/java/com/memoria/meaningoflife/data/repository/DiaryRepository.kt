package com.memoria.meaningoflife.data.repository

import com.memoria.meaningoflife.data.database.AppDatabase
import com.memoria.meaningoflife.data.database.diary.DiaryEntity
import com.memoria.meaningoflife.data.database.diary.MoodStats
import com.memoria.meaningoflife.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DiaryRepository(private val database: AppDatabase) {

    fun getAllDiaries(): Flow<List<DiaryEntity>> = database.diaryDao().getAllDiaries()

    suspend fun getDiaryById(diaryId: Long): DiaryEntity? = withContext(Dispatchers.IO) {
        database.diaryDao().getDiaryById(diaryId)
    }

    suspend fun getDiaryByDate(date: String): DiaryEntity? = withContext(Dispatchers.IO) {
        database.diaryDao().getDiaryByDate(date)
    }

    suspend fun getDiariesByDateRange(startDate: String, endDate: String): List<DiaryEntity> = withContext(Dispatchers.IO) {
        database.diaryDao().getDiariesByDateRange(startDate, endDate)
    }

    suspend fun getDiaryCountByDateRange(startDate: String, endDate: String): Int = withContext(Dispatchers.IO) {
        database.diaryDao().getCountByDateRange(startDate, endDate)
    }

    suspend fun getMoodStats(startDate: String, endDate: String): Map<Int, Int> = withContext(Dispatchers.IO) {
        val stats = database.diaryDao().getMoodStats(startDate, endDate)
        stats.associate { it.mood to it.count }
    }

    suspend fun insertDiary(diary: DiaryEntity): Long = withContext(Dispatchers.IO) {
        database.diaryDao().insertDiary(diary)
    }

    suspend fun updateDiary(diary: DiaryEntity) = withContext(Dispatchers.IO) {
        database.diaryDao().updateDiary(diary)
    }

    suspend fun deleteDiary(diaryId: Long) = withContext(Dispatchers.IO) {
        database.diaryDao().softDeleteDiary(diaryId)
    }

    suspend fun getConsecutiveDiaryDays(): Int = withContext(Dispatchers.IO) {
        val allDiaries = database.diaryDao().getAllDiariesSync()
        val dates = allDiaries.map { it.createdDate }
        DateUtils.getConsecutiveDays(dates)
    }
}
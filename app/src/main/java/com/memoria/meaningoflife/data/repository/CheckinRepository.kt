package com.memoria.meaningoflife.data.repository

import com.memoria.meaningoflife.data.database.AppDatabase
import com.memoria.meaningoflife.data.database.checkin.CheckinEntity
import com.memoria.meaningoflife.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CheckinRepository(private val database: AppDatabase) {

    suspend fun getTodayCheckin(): CheckinEntity? = withContext(Dispatchers.IO) {
        database.checkinDao().getCheckinByDate(DateUtils.getCurrentDate())
    }
    // 在 CheckinRepository 中添加
    suspend fun getAllCheckinsSync(): List<CheckinEntity> = withContext(Dispatchers.IO) {
        database.checkinDao().getAllCheckinsSync()
    }


    suspend fun checkin(note: String? = null): Boolean = withContext(Dispatchers.IO) {
        val today = DateUtils.getCurrentDate()
        val existing = database.checkinDao().getCheckinByDate(today)
        if (existing != null) return@withContext false

        val checkin = CheckinEntity(
            date = today,
            createdTime = System.currentTimeMillis(),
            note = note
        )
        database.checkinDao().insertCheckin(checkin)
        return@withContext true
    }

    suspend fun getCheckinsByDateRange(startDate: String, endDate: String): List<CheckinEntity> = withContext(Dispatchers.IO) {
        database.checkinDao().getCheckinsByDateRange(startDate, endDate)
    }

    suspend fun getCheckinCountByDateRange(startDate: String, endDate: String): Int = withContext(Dispatchers.IO) {
        database.checkinDao().getCountByDateRange(startDate, endDate)
    }

    suspend fun getConsecutiveCheckinDays(): Int = withContext(Dispatchers.IO) {
        val allCheckins = database.checkinDao().getAllCheckins()
        val dates = allCheckins.map { it.date }
        DateUtils.getConsecutiveDays(dates)
    }
}
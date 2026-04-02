package com.memoria.meaningoflife.data.database.checkin

import androidx.room.*

@Dao
interface CheckinDao {

    @Query("SELECT * FROM checkins ORDER BY date DESC")
    fun getAllCheckins(): List<CheckinEntity>

    @Query("DELETE FROM checkins")
    fun deleteAll()

    @Query("DELETE FROM checkins")
    fun deleteAllSync()

    @Query("SELECT * FROM checkins ORDER BY date DESC")
    fun getAllCheckinsSync(): List<CheckinEntity>


    @Query("SELECT * FROM checkins WHERE date = :date")
    fun getCheckinByDate(date: String): CheckinEntity?

    @Query("SELECT * FROM checkins WHERE date BETWEEN :startDate AND :endDate")
    fun getCheckinsByDateRange(startDate: String, endDate: String): List<CheckinEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCheckin(checkin: CheckinEntity): Long

    @Delete
    fun deleteCheckin(checkin: CheckinEntity)

    @Query("SELECT COUNT(*) FROM checkins WHERE date BETWEEN :startDate AND :endDate")
    fun getCountByDateRange(startDate: String, endDate: String): Int

    @Query("SELECT COUNT(*) FROM checkins WHERE date >= :startDate")
    fun getCountSinceDate(startDate: String): Int
}
package com.memoria.meaningoflife.data.database.lunch

import androidx.room.*

@Dao
interface LotteryHistoryDao {

    @Query("SELECT * FROM lottery_history ORDER BY selected_time DESC LIMIT 20")
    fun getRecentHistory(): List<LotteryHistoryEntity>

    @Query("SELECT * FROM lottery_history WHERE selected_date = :date")
    fun getHistoryByDate(date: String): LotteryHistoryEntity?

    @Query("DELETE FROM lottery_history")
    fun deleteAll()

    @Query("DELETE FROM lottery_history")
    fun deleteAllSync()

    @Query("SELECT * FROM lottery_history")
    fun getAllHistorySync(): List<LotteryHistoryEntity>

    @Insert
    fun insertHistory(history: LotteryHistoryEntity): Long

    @Delete
    fun deleteHistory(history: LotteryHistoryEntity)

    @Query("DELETE FROM lottery_history WHERE selected_date < :date")
    fun deleteOldHistory(date: String)
}
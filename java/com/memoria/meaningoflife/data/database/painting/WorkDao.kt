package com.memoria.meaningoflife.data.database.painting

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkDao {

    // 使用数据库列名 is_deleted（下划线）
    @Query("SELECT * FROM works WHERE is_deleted = 0 ORDER BY created_date DESC")
    fun getAllWorks(): Flow<List<WorkEntity>>

    @Query("SELECT * FROM works WHERE is_deleted = 0 ORDER BY created_date DESC")
    fun getAllWorksSync(): List<WorkEntity>

    @Query("SELECT * FROM works WHERE id = :workId AND is_deleted = 0")
    fun getWorkById(workId: Long): WorkEntity?

    @Query("SELECT * FROM works WHERE created_date = :date AND is_deleted = 0")
    fun getWorksByDate(date: String): List<WorkEntity>

    @Query("SELECT * FROM works WHERE created_date BETWEEN :startDate AND :endDate AND is_deleted = 0")
    fun getWorksByDateRange(startDate: String, endDate: String): List<WorkEntity>

    @Query("SELECT COUNT(*) FROM works WHERE created_date BETWEEN :startDate AND :endDate AND is_deleted = 0")
    fun getCountByDateRange(startDate: String, endDate: String): Int

    @Query("SELECT SUM(total_duration) FROM works WHERE created_date BETWEEN :startDate AND :endDate AND is_deleted = 0")
    fun getTotalDurationByDateRange(startDate: String, endDate: String): Int?

    @Insert
    fun insertWork(work: WorkEntity): Long

    @Update
    fun updateWork(work: WorkEntity)

    @Query("DELETE FROM works")
    fun deleteAll()

    @Query("DELETE FROM works")
    fun deleteAllSync()




    @Query("UPDATE works SET is_deleted = 1, updated_time = :updatedTime WHERE id = :workId")
    fun softDeleteWork(workId: Long, updatedTime: Long = System.currentTimeMillis())

    @Query("DELETE FROM works WHERE is_deleted = 1")
    fun deletePermanently()


}
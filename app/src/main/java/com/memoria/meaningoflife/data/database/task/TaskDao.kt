package com.memoria.meaningoflife.data.database.task

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 ORDER BY " +
            "CASE WHEN completedAt IS NULL THEN 0 ELSE 1 END, " +
            "deadline ASC, createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    // 改为同步方法，不使用 suspend
    @Query("SELECT * FROM tasks WHERE isDeleted = 0")
    fun getAllTasksSync(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 AND completedAt IS NULL ORDER BY deadline ASC")
    fun getActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 AND completedAt IS NULL AND isUrgent = 1 AND isImportant = 1")
    fun getUrgentImportantTasks(): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE isDeleted = 0 AND completedAt IS NULL AND isUrgent = 1 AND isImportant = 1")
    fun getUrgentImportantCount(): Flow<Int>

    @Query("SELECT * FROM tasks WHERE id = :taskId AND isDeleted = 0")
    fun getTaskById(taskId: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 AND completedAt IS NULL AND deadline IS NOT NULL " +
            "AND deadline BETWEEN :currentTime AND :threeDaysLater AND isUrgent = 0")
    fun getTasksNeedingUrgentFlag(currentTime: Long, threeDaysLater: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 AND completedAt IS NULL " +
            "AND deadline IS NOT NULL AND deadline <= :endOfDay " +
            "ORDER BY deadline ASC")
    fun getTasksByDeadline(endOfDay: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 AND deadline IS NOT NULL " +
            "AND deadline BETWEEN :startDate AND :endDate " +
            "ORDER BY deadline ASC")
    fun getTasksByDateRange(startDate: Long, endDate: Long): List<TaskEntity>

    @Insert
    fun insertTask(task: TaskEntity): Long

    @Update
    fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET completedAt = :completedAt WHERE id = :taskId")
    fun completeTask(taskId: Long, completedAt: Long)

    @Query("UPDATE tasks SET isUrgent = 1 WHERE id = :taskId")
    fun markAsUrgent(taskId: Long)

    @Query("UPDATE tasks SET isDeleted = 1 WHERE id = :taskId")
    fun softDeleteTask(taskId: Long)

    @Query("DELETE FROM tasks")
    fun deleteAll()

    @Query("DELETE FROM tasks")
    fun deleteAllSync()

    // 在 TaskDao.kt 中添加
    @Query("SELECT COUNT(*) FROM tasks WHERE isDeleted = 0 AND completedAt IS NULL AND isUrgent = 1 AND isImportant = 1")
    fun getUrgentImportantCountSync(): Int


    @Query("SELECT * FROM tasks WHERE completedAt IS NOT NULL ORDER BY completedAt DESC LIMIT 20")
    fun getRecentCompletedTasks(): List<TaskEntity>
}
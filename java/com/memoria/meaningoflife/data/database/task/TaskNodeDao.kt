package com.memoria.meaningoflife.data.database.task

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskNodeDao {

    @Query("SELECT * FROM task_nodes WHERE taskId = :taskId ORDER BY sortOrder ASC")
    fun getNodesByTaskId(taskId: Long): Flow<List<TaskNodeEntity>>

    @Query("SELECT * FROM task_nodes WHERE taskId = :taskId AND isCompleted = 0 ORDER BY deadline ASC")
    fun getActiveNodesByTaskId(taskId: Long): Flow<List<TaskNodeEntity>>

    @Insert
    fun insertNode(node: TaskNodeEntity): Long

    @Update
    fun updateNode(node: TaskNodeEntity)

    @Query("UPDATE task_nodes SET isCompleted = 1, completedAt = :completedAt WHERE id = :nodeId")
    fun completeNode(nodeId: Long, completedAt: Long)

    @Query("DELETE FROM task_nodes WHERE taskId = :taskId")
    fun deleteNodesByTaskId(taskId: Long)

    @Query("SELECT MAX(sortOrder) FROM task_nodes WHERE taskId = :taskId")
    fun getMaxSortOrder(taskId: Long): Int?

    @Query("DELETE FROM task_nodes")
    fun deleteAll()

    @Query("SELECT * FROM task_nodes")
    fun getAllTaskNodesSync(): List<TaskNodeEntity>

    @Query("DELETE FROM task_nodes")
    fun deleteAllSync()


}
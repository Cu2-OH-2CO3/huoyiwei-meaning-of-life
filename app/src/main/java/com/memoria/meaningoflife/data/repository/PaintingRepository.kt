package com.memoria.meaningoflife.data.repository

import com.memoria.meaningoflife.data.database.AppDatabase
import com.memoria.meaningoflife.data.database.painting.GoalEntity
import com.memoria.meaningoflife.data.database.painting.NodeEntity
import com.memoria.meaningoflife.data.database.painting.WorkEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PaintingRepository(private val database: AppDatabase) {

    // Work
    fun getAllWorks(): Flow<List<WorkEntity>> = database.workDao().getAllWorks()

    suspend fun getWorkById(workId: Long): WorkEntity? = withContext(Dispatchers.IO) {
        database.workDao().getWorkById(workId)
    }
    // 在 PaintingRepository 中添加

    suspend fun getWorksByDateRange(startDate: String, endDate: String): List<WorkEntity> = withContext(Dispatchers.IO) {
        database.workDao().getWorksByDateRange(startDate, endDate)
    }

    suspend fun getWorkCountByDateRange(startDate: String, endDate: String): Int = withContext(Dispatchers.IO) {
        database.workDao().getCountByDateRange(startDate, endDate)
    }

    suspend fun getTotalDurationByDateRange(startDate: String, endDate: String): Int = withContext(Dispatchers.IO) {
        database.workDao().getTotalDurationByDateRange(startDate, endDate) ?: 0
    }

    suspend fun insertWork(work: WorkEntity): Long = withContext(Dispatchers.IO) {
        database.workDao().insertWork(work)
    }

    suspend fun updateWork(work: WorkEntity) = withContext(Dispatchers.IO) {
        database.workDao().updateWork(work)
    }

    suspend fun deleteWork(workId: Long) = withContext(Dispatchers.IO) {
        database.workDao().softDeleteWork(workId)
    }

    // Node
    suspend fun getNodesByWorkId(workId: Long): List<NodeEntity> = withContext(Dispatchers.IO) {
        database.nodeDao().getNodesByWorkId(workId)
    }

    // 添加这个方法
    suspend fun getNodeById(nodeId: Long): NodeEntity? = withContext(Dispatchers.IO) {
        database.nodeDao().getNodeById(nodeId)
    }

    suspend fun insertNode(node: NodeEntity): Long = withContext(Dispatchers.IO) {
        database.nodeDao().insertNode(node)
    }

    suspend fun updateNode(node: NodeEntity) = withContext(Dispatchers.IO) {
        database.nodeDao().updateNode(node)
    }

    suspend fun deleteNode(node: NodeEntity) = withContext(Dispatchers.IO) {
        database.nodeDao().deleteNode(node)
    }

    suspend fun getMaxNodeOrder(workId: Long): Int = withContext(Dispatchers.IO) {
        database.nodeDao().getMaxNodeOrder(workId) ?: 0
    }

    // Goal
    suspend fun getCurrentGoal(): GoalEntity? = withContext(Dispatchers.IO) {
        database.goalDao().getCurrentGoal()
    }

    suspend fun getAllGoals(): List<GoalEntity> = withContext(Dispatchers.IO) {
        database.goalDao().getAllGoals()
    }

    suspend fun insertGoal(goal: GoalEntity): Long = withContext(Dispatchers.IO) {
        database.goalDao().insertGoal(goal)
    }

    suspend fun updateGoal(goal: GoalEntity) = withContext(Dispatchers.IO) {
        database.goalDao().updateGoal(goal)
    }

    suspend fun updateGoalProgress(goalId: Long, currentValue: Int) = withContext(Dispatchers.IO) {
        database.goalDao().updateProgress(goalId, currentValue)
    }

    suspend fun completeGoal(goalId: Long) = withContext(Dispatchers.IO) {
        database.goalDao().completeGoal(goalId)
    }

    suspend fun deleteGoal(goal: GoalEntity) = withContext(Dispatchers.IO) {
        database.goalDao().deleteGoal(goal)
    }

    suspend fun getAllWorksSync(): List<WorkEntity> = withContext(Dispatchers.IO) {
        database.workDao().getAllWorksSync()
    }

    suspend fun getAllNodesSync(): List<NodeEntity> = withContext(Dispatchers.IO) {
        database.nodeDao().getAllNodesSync()
    }

    suspend fun getAllGoalsSync(): List<GoalEntity> = withContext(Dispatchers.IO) {
        database.goalDao().getAllGoalsSync()
    }
}
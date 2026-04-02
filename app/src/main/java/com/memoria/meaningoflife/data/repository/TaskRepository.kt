package com.memoria.meaningoflife.data.repository

import com.memoria.meaningoflife.data.database.AppDatabase
import com.memoria.meaningoflife.data.database.task.TaskEntity
import com.memoria.meaningoflife.data.database.task.TaskNodeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TaskRepository(private val database: AppDatabase) {

    // Task operations
    fun getAllTasks(): Flow<List<TaskEntity>> = database.taskDao().getAllTasks()
    fun getActiveTasks(): Flow<List<TaskEntity>> = database.taskDao().getActiveTasks()
    fun getUrgentImportantTasks(): Flow<List<TaskEntity>> = database.taskDao().getUrgentImportantTasks()
    fun getUrgentImportantCount(): Flow<Int> = database.taskDao().getUrgentImportantCount()

    // 同步方法，在协程中使用 withContext 调用
    suspend fun getAllTasksSync(): List<TaskEntity> = withContext(Dispatchers.IO) {
        database.taskDao().getAllTasksSync()
    }

    suspend fun getAllTaskNodesSync(): List<TaskNodeEntity> = withContext(Dispatchers.IO) {
        database.taskNodeDao().getAllTaskNodesSync()
    }

    suspend fun getTaskById(taskId: Long): TaskEntity? = withContext(Dispatchers.IO) {
        database.taskDao().getTaskById(taskId)
    }

    suspend fun getTasksByDeadline(endOfDay: Long): List<TaskEntity> = withContext(Dispatchers.IO) {
        database.taskDao().getTasksByDeadline(endOfDay)
    }

    suspend fun getTasksByDateRange(startDate: Long, endDate: Long): List<TaskEntity> = withContext(Dispatchers.IO) {
        database.taskDao().getTasksByDateRange(startDate, endDate)
    }

    suspend fun insertTask(task: TaskEntity): Long = withContext(Dispatchers.IO) {
        database.taskDao().insertTask(task)
    }

    suspend fun updateTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        database.taskDao().updateTask(task)
    }

    suspend fun completeTask(taskId: Long) = withContext(Dispatchers.IO) {
        database.taskDao().completeTask(taskId, System.currentTimeMillis())
    }

    suspend fun softDeleteTask(taskId: Long) = withContext(Dispatchers.IO) {
        database.taskDao().softDeleteTask(taskId)
    }

    suspend fun markTasksAsUrgent() = withContext(Dispatchers.IO) {
        val threeDaysLater = System.currentTimeMillis() + 3 * 24 * 60 * 60 * 1000L
        val tasks = database.taskDao().getTasksNeedingUrgentFlag(threeDaysLater)
        tasks.forEach { task ->
            if (!task.isUrgent) {
                database.taskDao().markAsUrgent(task.id)
            }
        }
    }

    suspend fun getRecentCompletedTasks(): List<TaskEntity> = withContext(Dispatchers.IO) {
        database.taskDao().getRecentCompletedTasks()
    }

    // TaskNode operations
    fun getNodesByTaskId(taskId: Long): Flow<List<TaskNodeEntity>> = database.taskNodeDao().getNodesByTaskId(taskId)

    suspend fun insertNode(node: TaskNodeEntity): Long = withContext(Dispatchers.IO) {
        database.taskNodeDao().insertNode(node)
    }

    suspend fun updateNode(node: TaskNodeEntity) = withContext(Dispatchers.IO) {
        database.taskNodeDao().updateNode(node)
    }

    suspend fun completeNode(nodeId: Long) = withContext(Dispatchers.IO) {
        database.taskNodeDao().completeNode(nodeId, System.currentTimeMillis())
    }

    // 在 TaskRepository.kt 中添加
    fun getUrgentImportantCountSync(): Int {
        return database.taskDao().getUrgentImportantCountSync()
    }


    suspend fun deleteNodesByTaskId(taskId: Long) = withContext(Dispatchers.IO) {
        database.taskNodeDao().deleteNodesByTaskId(taskId)
    }

    suspend fun getMaxSortOrder(taskId: Long): Int = withContext(Dispatchers.IO) {
        database.taskNodeDao().getMaxSortOrder(taskId) ?: 0
    }
}
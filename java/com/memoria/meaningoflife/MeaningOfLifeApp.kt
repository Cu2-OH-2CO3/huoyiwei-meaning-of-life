package com.memoria.meaningoflife

import android.app.Application
import com.memoria.meaningoflife.data.database.AppDatabase
import com.memoria.meaningoflife.data.repository.TaskRepository
import com.memoria.meaningoflife.utils.LogManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MeaningOfLifeApp : Application() {

    lateinit var database: AppDatabase
        private set

    companion object {
        lateinit var instance: MeaningOfLifeApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)

        // 检查并标记即将到期的任务
        markUrgentTasks()
    }

    private fun markUrgentTasks() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val taskRepository = TaskRepository(database)
                taskRepository.markTasksAsUrgent()
            } catch (e: Exception) {
                LogManager.e("MeaningOfLifeApp", "Failed to mark urgent tasks: ${e.message}")
            }
        }
    }
}
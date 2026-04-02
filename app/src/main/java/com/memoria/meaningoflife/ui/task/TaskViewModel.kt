package com.memoria.meaningoflife.ui.task

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memoria.meaningoflife.MeaningOfLifeApp
import com.memoria.meaningoflife.data.database.diary.DiaryEntity
import com.memoria.meaningoflife.data.database.task.TaskEntity
import com.memoria.meaningoflife.data.repository.DiaryRepository
import com.memoria.meaningoflife.data.repository.TaskRepository
import com.memoria.meaningoflife.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TaskViewModel : ViewModel() {

    private val repository = TaskRepository(MeaningOfLifeApp.instance.database)
    private val diaryRepository = DiaryRepository(MeaningOfLifeApp.instance.database)

    private val _allTasks = MutableLiveData<List<TaskEntity>>()
    val allTasks: LiveData<List<TaskEntity>> = _allTasks

    init {
        loadTasks()
    }

    private fun loadTasks() {
        viewModelScope.launch {
            repository.getAllTasks().collect { tasks ->
                _allTasks.postValue(tasks)
            }
        }
    }

    fun getTaskById(taskId: Long, callback: (TaskEntity?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val task = repository.getTaskById(taskId)
            withContext(Dispatchers.Main) {
                callback(task)
            }
        }
    }

    fun insertTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertTask(task)
            loadTasks()
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTask(task)
            loadTasks()
        }
    }

    fun completeTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.completeTask(task.id)

            // 创建完成任务的日记记录
            val currentDate = DateUtils.getCurrentDate()

            // 检查当天是否已有日记
            val existingDiary = diaryRepository.getDiaryByDate(currentDate)

            val completionContent = "✅ 完成任务：${task.title}"

            if (existingDiary != null) {
                // 更新现有日记，添加任务完成记录
                val updatedContent = existingDiary.content + "\n" + completionContent
                val updatedDiary = existingDiary.copy(
                    content = updatedContent,
                    updatedTime = System.currentTimeMillis()
                )
                diaryRepository.updateDiary(updatedDiary)
            } else {
                // 创建新的日记记录
                val diary = DiaryEntity(
                    title = "任务完成记录",
                    content = completionContent,
                    mood = 2,
                    weather = null,
                    tags = null,
                    images = null,
                    createdDate = currentDate,
                    createdTime = System.currentTimeMillis(),
                    updatedTime = System.currentTimeMillis(),
                    isDeleted = false
                )
                diaryRepository.insertDiary(diary)
            }

            loadTasks()
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.softDeleteTask(task.id)
            loadTasks()
        }
    }
}
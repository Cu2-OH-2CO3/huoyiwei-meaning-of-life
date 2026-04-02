package com.memoria.meaningoflife.ui.painting

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asLiveData
import com.memoria.meaningoflife.MeaningOfLifeApp
import com.memoria.meaningoflife.data.database.painting.GoalEntity
import com.memoria.meaningoflife.data.database.painting.WorkEntity
import com.memoria.meaningoflife.data.repository.PaintingRepository
import kotlinx.coroutines.launch

class PaintingViewModel : ViewModel() {

    private val repository = PaintingRepository(MeaningOfLifeApp.instance.database)

    val allWorks: LiveData<List<WorkEntity>> = repository.getAllWorks().asLiveData()

    private val _totalDuration = MutableLiveData<Int>()
    val totalDuration: LiveData<Int> = _totalDuration

    private val _currentGoal = MutableLiveData<GoalEntity?>()
    val currentGoal: LiveData<GoalEntity?> = _currentGoal

    init {
        loadTotalDuration()
        loadCurrentGoal()
        // 监听作品变化，自动更新目标进度
        observeWorksForGoalUpdate()
    }

    private fun observeWorksForGoalUpdate() {
        viewModelScope.launch {
            allWorks.observeForever { works ->
                updateGoalProgress()
            }
        }
    }

    private fun loadTotalDuration() {
        viewModelScope.launch {
            val startDate = "2020-01-01"
            val endDate = "2099-12-31"
            val duration = repository.getTotalDurationByDateRange(startDate, endDate)
            _totalDuration.postValue(duration)
        }
    }

    private fun loadCurrentGoal() {
        viewModelScope.launch {
            val goal = repository.getCurrentGoal()
            _currentGoal.postValue(goal)
        }
    }

    private fun updateGoalProgress() {
        viewModelScope.launch {
            val currentGoal = repository.getCurrentGoal()
            if (currentGoal != null && currentGoal.status == 0) {
                // 获取当前进度 - 统计从开始日期到目标日期之间的作品
                val currentValue = when (currentGoal.targetType) {
                    0 -> {
                        // 作品数量目标
                        repository.getWorkCountByDateRange(currentGoal.startDate, currentGoal.targetDate)
                    }
                    else -> {
                        // 时长目标
                        repository.getTotalDurationByDateRange(currentGoal.startDate, currentGoal.targetDate)
                    }
                }

                // 如果进度有变化，更新数据库
                if (currentValue != currentGoal.currentValue) {
                    repository.updateGoalProgress(currentGoal.id, currentValue)
                    // 重新加载目标以更新UI
                    loadCurrentGoal()
                }
                // 注意：不再自动完成，由用户手动点击"完成"按钮
            }
        }
    }

    fun deleteWork(work: WorkEntity) {
        viewModelScope.launch {
            repository.deleteWork(work.id)
            loadTotalDuration()
            updateGoalProgress()
        }
    }

    fun refreshData() {
        loadTotalDuration()
        loadCurrentGoal()
        updateGoalProgress()
    }
}
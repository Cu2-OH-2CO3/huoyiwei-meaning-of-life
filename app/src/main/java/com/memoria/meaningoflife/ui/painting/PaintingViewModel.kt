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

    // 将 Flow 转换为 LiveData
    val allWorks: LiveData<List<WorkEntity>> = repository.getAllWorks().asLiveData()

    private val _totalDuration = MutableLiveData<Int>()
    val totalDuration: LiveData<Int> = _totalDuration

    private val _currentGoal = MutableLiveData<GoalEntity?>()
    val currentGoal: LiveData<GoalEntity?> = _currentGoal

    init {
        loadTotalDuration()
        loadCurrentGoal()
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

    fun deleteWork(work: WorkEntity) {
        viewModelScope.launch {
            repository.deleteWork(work.id)
            loadTotalDuration()
        }
    }
}
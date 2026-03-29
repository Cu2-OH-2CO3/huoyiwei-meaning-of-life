package com.memoria.meaningoflife.ui.diary

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asLiveData
import com.memoria.meaningoflife.MeaningOfLifeApp
import com.memoria.meaningoflife.data.database.diary.DiaryEntity
import com.memoria.meaningoflife.data.repository.DiaryRepository
import com.memoria.meaningoflife.utils.DateUtils
import kotlinx.coroutines.launch

class DiaryViewModel : ViewModel() {

    private val repository = DiaryRepository(MeaningOfLifeApp.instance.database)

    // 将 Flow 转换为 LiveData
    val allDiaries: LiveData<List<DiaryEntity>> = repository.getAllDiaries().asLiveData()

    private val _consecutiveDays = MutableLiveData<Int>()
    val consecutiveDays: LiveData<Int> = _consecutiveDays

    private val _monthCount = MutableLiveData<Int>()
    val monthCount: LiveData<Int> = _monthCount

    init {
        loadStats()
    }

    fun refreshData() {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            val consecutive = repository.getConsecutiveDiaryDays()
            _consecutiveDays.postValue(consecutive)

            val currentDate = DateUtils.getCurrentDate()
            val startOfMonth = DateUtils.getStartDateOfMonth(
                currentDate.substring(0, 4).toInt(),
                currentDate.substring(5, 7).toInt()
            )
            val count = repository.getDiaryCountByDateRange(startOfMonth, currentDate)
            _monthCount.postValue(count)
        }
    }

    fun deleteDiary(diaryId: Long) {
        viewModelScope.launch {
            repository.deleteDiary(diaryId)
            loadStats()
        }
    }
}
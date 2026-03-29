package com.memoria.meaningoflife.ui.diary

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memoria.meaningoflife.MeaningOfLifeApp
import com.memoria.meaningoflife.data.database.diary.DiaryEntity
import com.memoria.meaningoflife.data.repository.DiaryRepository
import kotlinx.coroutines.launch

class DiaryDetailViewModel : ViewModel() {

    private val repository = DiaryRepository(MeaningOfLifeApp.instance.database)

    private val _diary = MutableLiveData<DiaryEntity?>()
    val diary: LiveData<DiaryEntity?> = _diary

    fun loadDiary(diaryId: Long) {
        viewModelScope.launch {
            val diary = repository.getDiaryById(diaryId)
            _diary.postValue(diary)
        }
    }

    fun deleteDiary() {
        viewModelScope.launch {
            _diary.value?.let {
                repository.deleteDiary(it.id)
            }
        }
    }
}
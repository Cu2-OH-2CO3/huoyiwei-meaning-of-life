// 路径：com/memoria/meaningoflife/ui/timeline/free/FreeTimelineViewModel.kt
package com.memoria.meaningoflife.ui.timeline.free

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.memoria.meaningoflife.data.database.timeline.SourceMode
import com.memoria.meaningoflife.data.database.timeline.TimelineEventEntity
import com.memoria.meaningoflife.data.repository.TimelineRepository
import kotlinx.coroutines.launch
import java.util.*

class FreeTimelineViewModel(private val repository: TimelineRepository) : ViewModel() {

    var currentDateMillis = System.currentTimeMillis()
        private set

    private val _currentEvents = MutableLiveData<List<TimelineEventEntity>>()
    val currentEvents: LiveData<List<TimelineEventEntity>> = _currentEvents

    init {
        loadEventsForDate(currentDateMillis)
    }

    fun loadEventsForDate(dateMillis: Long) {
        val startOfDay = getStartOfDay(dateMillis)
        val endOfDay = getEndOfDay(dateMillis)

        viewModelScope.launch {
            val allEvents = repository.getEventsInRange(startOfDay, endOfDay)
            val freeEvents = allEvents.filter { it.sourceMode == SourceMode.FREE }
            _currentEvents.postValue(freeEvents)
        }
    }

    fun toggleEventCompletion(event: TimelineEventEntity) {
        viewModelScope.launch {
            if (!event.isCompleted) {
                repository.completeEvent(event.id)
                loadEventsForDate(currentDateMillis)
            }
        }
    }

    fun deleteEvent(event: TimelineEventEntity) {
        viewModelScope.launch {
            repository.deleteEvent(event.id)
            loadEventsForDate(currentDateMillis)
        }
    }

    fun previousDay() {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentDateMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        currentDateMillis = calendar.timeInMillis
        loadEventsForDate(currentDateMillis)
    }

    fun nextDay() {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentDateMillis
            add(Calendar.DAY_OF_YEAR, 1)
        }
        currentDateMillis = calendar.timeInMillis
        loadEventsForDate(currentDateMillis)
    }

    fun goToToday() {
        currentDateMillis = System.currentTimeMillis()
        loadEventsForDate(currentDateMillis)
    }

    private fun getStartOfDay(dateMillis: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun getEndOfDay(dateMillis: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }
}

class FreeTimelineViewModelFactory(private val repository: TimelineRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FreeTimelineViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FreeTimelineViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
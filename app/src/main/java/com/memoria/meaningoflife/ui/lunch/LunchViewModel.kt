package com.memoria.meaningoflife.ui.lunch

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memoria.meaningoflife.MeaningOfLifeApp
import com.memoria.meaningoflife.data.database.lunch.DishEntity
import com.memoria.meaningoflife.data.database.lunch.LotteryHistoryEntity
import com.memoria.meaningoflife.data.repository.LunchRepository
import kotlinx.coroutines.launch

class LunchViewModel : ViewModel() {

    private val repository = LunchRepository(MeaningOfLifeApp.instance.database)

    private val _activeDishes = MutableLiveData<List<DishEntity>>()
    val activeDishes: LiveData<List<DishEntity>> = _activeDishes

    init {
        loadActiveDishes()
    }

    private fun loadActiveDishes() {
        viewModelScope.launch {
            val dishes = repository.getActiveDishes()
            _activeDishes.postValue(dishes)
        }
    }

    fun getTodayLottery(callback: (DishEntity?) -> Unit) {
        viewModelScope.launch {
            val history = repository.getTodayLottery()
            if (history != null) {
                val dish = repository.getDishById(history.dishId)
                callback(dish)
            } else {
                callback(null)
            }
        }
    }

    fun saveLotteryResult(dish: DishEntity) {
        viewModelScope.launch {
            repository.recordLottery(dish)
        }
    }

    fun getRecentHistory(callback: (List<LotteryHistoryEntity>) -> Unit) {
        viewModelScope.launch {
            val history = repository.getRecentHistory()
            callback(history)
        }
    }

    fun addDish(dish: DishEntity, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val id = repository.insertDish(dish)
                callback(id > 0)
                loadActiveDishes()
            } catch (e: Exception) {
                callback(false)
            }
        }
    }

    fun refreshData() {
        loadActiveDishes()
    }
}
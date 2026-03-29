package com.memoria.meaningoflife.ui.lunch

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memoria.meaningoflife.MeaningOfLifeApp
import com.memoria.meaningoflife.data.database.lunch.DishEntity
import com.memoria.meaningoflife.data.repository.LunchRepository
import kotlinx.coroutines.launch

class DishManageViewModel : ViewModel() {

    private val repository = LunchRepository(MeaningOfLifeApp.instance.database)

    private val _allDishes = MutableLiveData<List<DishEntity>>()
    val allDishes: LiveData<List<DishEntity>> = _allDishes

    init {
        loadAllDishes()
    }

    fun refreshData() {
        loadAllDishes()
    }

    private fun loadAllDishes() {
        viewModelScope.launch {
            val dishes = repository.getAllDishesSync()
            _allDishes.postValue(dishes)
        }
    }

    fun toggleActive(dish: DishEntity) {
        viewModelScope.launch {
            repository.updateActiveStatus(dish.id, !dish.isActive)
            loadAllDishes()
        }
    }

    fun deleteDish(dish: DishEntity) {
        viewModelScope.launch {
            repository.deleteDish(dish)
            loadAllDishes()
        }
    }
}
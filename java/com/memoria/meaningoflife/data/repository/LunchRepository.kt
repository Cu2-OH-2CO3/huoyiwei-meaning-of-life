package com.memoria.meaningoflife.data.repository

import com.memoria.meaningoflife.data.database.AppDatabase
import com.memoria.meaningoflife.data.database.lunch.DishEntity
import com.memoria.meaningoflife.data.database.lunch.LotteryHistoryEntity
import com.memoria.meaningoflife.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.random.Random

class LunchRepository(private val database: AppDatabase) {

    // Dish
    fun getAllDishes(): Flow<List<DishEntity>> = database.dishDao().getAllDishes()

    suspend fun getAllDishesSync(): List<DishEntity> = withContext(Dispatchers.IO) {
        database.dishDao().getAllDishesSync()
    }

    suspend fun getAllLotteryHistorySync(): List<LotteryHistoryEntity> = withContext(Dispatchers.IO) {
        database.lotteryHistoryDao().getAllHistorySync()
    }

    suspend fun getActiveDishes(): List<DishEntity> = withContext(Dispatchers.IO) {
        database.dishDao().getActiveDishes()
    }

    suspend fun getDishById(dishId: Long): DishEntity? = withContext(Dispatchers.IO) {
        database.dishDao().getDishById(dishId)
    }

    suspend fun getActiveCount(): Int = withContext(Dispatchers.IO) {
        database.dishDao().getActiveCount()
    }

    suspend fun insertDish(dish: DishEntity): Long = withContext(Dispatchers.IO) {
        database.dishDao().insertDish(dish)
    }

    suspend fun updateDish(dish: DishEntity) = withContext(Dispatchers.IO) {
        database.dishDao().updateDish(dish)
    }

    suspend fun deleteDish(dish: DishEntity) = withContext(Dispatchers.IO) {
        database.dishDao().deleteDish(dish)
    }

    suspend fun updateActiveStatus(dishId: Long, isActive: Boolean) = withContext(Dispatchers.IO) {
        database.dishDao().updateActiveStatus(dishId, isActive)
    }

    // Lottery
    suspend fun randomPick(): DishEntity? = withContext(Dispatchers.IO) {
        val activeDishes = getActiveDishes()
        if (activeDishes.isEmpty()) null else activeDishes[Random.nextInt(activeDishes.size)]
    }

    suspend fun recordLottery(dish: DishEntity) = withContext(Dispatchers.IO) {
        val history = LotteryHistoryEntity(
            dishId = dish.id,
            dishName = dish.name,
            selectedDate = DateUtils.getCurrentDate()
        )
        database.lotteryHistoryDao().insertHistory(history)
    }

    suspend fun getTodayLottery(): LotteryHistoryEntity? = withContext(Dispatchers.IO) {
        database.lotteryHistoryDao().getHistoryByDate(DateUtils.getCurrentDate())
    }

    suspend fun getRecentHistory(): List<LotteryHistoryEntity> = withContext(Dispatchers.IO) {
        database.lotteryHistoryDao().getRecentHistory()
    }
}
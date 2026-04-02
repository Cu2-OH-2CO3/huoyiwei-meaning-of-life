package com.memoria.meaningoflife.data.database.lunch

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DishDao {

    @Query("SELECT * FROM dishes ORDER BY sort_order ASC, id ASC")
    fun getAllDishes(): Flow<List<DishEntity>>

    @Query("DELETE FROM dishes")
    fun deleteAll()

    @Query("DELETE FROM dishes")
    fun deleteAllSync()


    @Query("SELECT * FROM dishes ORDER BY sort_order ASC, id ASC")
    fun getAllDishesSync(): List<DishEntity>

    @Query("SELECT * FROM dishes WHERE is_active = 1 ORDER BY sort_order ASC")
    fun getActiveDishes(): List<DishEntity>

    @Query("SELECT * FROM dishes WHERE id = :dishId")
    fun getDishById(dishId: Long): DishEntity?

    @Query("SELECT COUNT(*) FROM dishes WHERE is_active = 1")
    fun getActiveCount(): Int

    @Insert
    fun insertDish(dish: DishEntity): Long

    @Update
    fun updateDish(dish: DishEntity)

    @Delete
    fun deleteDish(dish: DishEntity)

    @Query("UPDATE dishes SET is_active = :isActive WHERE id = :dishId")
    fun updateActiveStatus(dishId: Long, isActive: Boolean)

    @Query("UPDATE dishes SET sort_order = :sortOrder WHERE id = :dishId")
    fun updateSortOrder(dishId: Long, sortOrder: Int)
}
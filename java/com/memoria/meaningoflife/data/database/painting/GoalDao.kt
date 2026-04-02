package com.memoria.meaningoflife.data.database.painting

import androidx.room.*

@Dao
interface GoalDao {

    @Query("SELECT * FROM goals WHERE status = 0 ORDER BY target_date ASC")
    fun getActiveGoals(): List<GoalEntity>

    @Query("SELECT * FROM goals")
    fun getAllGoalsSync(): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE status = 0 LIMIT 1")
    fun getCurrentGoal(): GoalEntity?

    @Query("SELECT * FROM goals ORDER BY created_time DESC")
    fun getAllGoals(): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE id = :goalId")
    fun getGoalById(goalId: Long): GoalEntity?

    @Insert
    fun insertGoal(goal: GoalEntity): Long

    @Query("DELETE FROM goals")
    fun deleteAll()

    @Query("DELETE FROM goals")
    fun deleteAllSync()

    @Update
    fun updateGoal(goal: GoalEntity)

    @Delete
    fun deleteGoal(goal: GoalEntity)

    @Query("UPDATE goals SET current_value = :currentValue WHERE id = :goalId")
    fun updateProgress(goalId: Long, currentValue: Int)

    @Query("UPDATE goals SET status = 1, completed_time = :completedTime WHERE id = :goalId")
    fun completeGoal(goalId: Long, completedTime: Long = System.currentTimeMillis())
}
package com.memoria.meaningoflife.data.database.lunch

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lottery_history")
data class LotteryHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "dish_id")
    val dishId: Long,

    @ColumnInfo(name = "dish_name")
    val dishName: String,

    @ColumnInfo(name = "selected_date")
    val selectedDate: String,

    @ColumnInfo(name = "selected_time")
    val selectedTime: Long = System.currentTimeMillis()
)
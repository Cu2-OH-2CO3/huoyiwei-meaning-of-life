package com.memoria.meaningoflife.data.database.painting

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "target_date")
    val targetDate: String,

    @ColumnInfo(name = "start_date")
    val startDate: String,

    @ColumnInfo(name = "target_type")
    val targetType: Int = 0,

    @ColumnInfo(name = "target_value")
    val targetValue: Int,

    @ColumnInfo(name = "current_value")
    val currentValue: Int = 0,

    @ColumnInfo(name = "status")
    val status: Int = 0,

    @ColumnInfo(name = "created_time")
    val createdTime: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "completed_time")
    val completedTime: Long? = null
)
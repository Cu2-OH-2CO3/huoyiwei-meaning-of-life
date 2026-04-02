package com.memoria.meaningoflife.data.database.diary

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_tags")
data class DiaryTagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: Int = 0xFF6B6B6B.toInt(),  // 标签颜色
    val sortOrder: Int = 0
)
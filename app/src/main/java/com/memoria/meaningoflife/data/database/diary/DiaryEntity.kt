package com.memoria.meaningoflife.data.database.diary

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diaries")
data class DiaryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String? = null,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "mood")
    val mood: Int = 2,

    @ColumnInfo(name = "weather")
    val weather: Int = 1,

    @ColumnInfo(name = "tags")
    val tags: String? = null,

    @ColumnInfo(name = "images")
    val images: String? = null,

    @ColumnInfo(name = "created_date")
    val createdDate: String,

    @ColumnInfo(name = "created_time")
    val createdTime: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_time")
    val updatedTime: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false
)
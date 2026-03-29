package com.memoria.meaningoflife.data.database.painting

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "works")
data class WorkEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "final_image_path")
    val finalImagePath: String? = null,

    @ColumnInfo(name = "total_duration")
    val totalDuration: Int = 0,

    @ColumnInfo(name = "tags")
    val tags: String? = null,

    @ColumnInfo(name = "created_date")
    val createdDate: String,

    @ColumnInfo(name = "created_time")
    val createdTime: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_time")
    val updatedTime: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false
)
// 路径：data/database/timeline/SemesterEntity.kt
package com.memoria.meaningoflife.data.database.timeline

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "semesters")
data class SemesterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val startDate: Long,
    val endDate: Long,
    val totalWeeks: Int = 16,
    val isCurrent: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
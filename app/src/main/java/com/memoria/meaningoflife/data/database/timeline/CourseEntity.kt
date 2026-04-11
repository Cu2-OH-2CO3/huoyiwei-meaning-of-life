// 路径：data/database/timeline/CourseEntity.kt
package com.memoria.meaningoflife.data.database.timeline

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val teacher: String? = null,
    val location: String? = null,
    val color: Int? = null,
    val weekDay: Int,           // 1-7 (周一=1, 周日=7)
    val startTime: Long,        // 从0点开始的毫秒数
    val endTime: Long,
    val weekStart: Int = 1,
    val weekEnd: Int = 16,
    val isOddWeek: Boolean = false,
    val isEvenWeek: Boolean = false,
    val remark: String? = null,
    val icsUid: String? = null,
    val semesterId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
package com.memoria.meaningoflife.data.database.task

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TaskPriority {
    URGENT_IMPORTANT,
    URGENT_NOT_IMPORTANT,
    NOT_URGENT_IMPORTANT,
    NOT_URGENT_NOT_IMPORTANT
}

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val isUrgent: Boolean = false,
    val isImportant: Boolean = false,
    val deadline: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val isDeleted: Boolean = false
) {
    fun getPriority(): TaskPriority = when {
        isUrgent && isImportant -> TaskPriority.URGENT_IMPORTANT
        isUrgent && !isImportant -> TaskPriority.URGENT_NOT_IMPORTANT
        !isUrgent && isImportant -> TaskPriority.NOT_URGENT_IMPORTANT
        else -> TaskPriority.NOT_URGENT_NOT_IMPORTANT
    }
}
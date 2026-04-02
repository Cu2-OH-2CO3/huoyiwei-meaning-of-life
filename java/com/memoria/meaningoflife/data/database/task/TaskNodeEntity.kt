package com.memoria.meaningoflife.data.database.task

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_nodes")
data class TaskNodeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,
    val title: String,
    val description: String? = null,
    val deadline: Long? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
// 路径：data/database/timeline/TimelineEventEntity.kt
package com.memoria.meaningoflife.data.database.timeline

import androidx.room.Entity
import androidx.room.PrimaryKey

// 在 TimelineEventEntity.kt 中修改 EventType
enum class EventType {
    COURSE, TASK, DIARY, PAINTING, LUNCH, CUSTOM, DEADLINE, APPOINTMENT
}

enum class SourceMode {
    SCHEDULE, FREE
}

@Entity(tableName = "timeline_events")
data class TimelineEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val eventType: EventType,
    val sourceMode: SourceMode,
    val startTime: Long,
    val endTime: Long? = null,
    val isAllDay: Boolean = false,
    val isRecurring: Boolean = false,
    val courseId: Long? = null,
    val weekDay: Int? = null,
    val weekStart: Int? = null,
    val weekEnd: Int? = null,
    val location: String? = null,
    val teacher: String? = null,
    val color: Int? = null,
    val isCompleted: Boolean = false,
    val reminderMinutes: Int? = null,
    val icsUid: String? = null,
    val icsSequence: Int? = null,
    val sourceId: Long? = null,
    val sourceTable: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false

)
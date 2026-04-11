// 路径：com/memoria/meaningoflife/data/database/timeline/TimelineEventDao.kt
package com.memoria.meaningoflife.data.database.timeline

import androidx.room.*

@Dao
interface TimelineEventDao {

    @Query("SELECT * FROM timeline_events WHERE isDeleted = 0 ORDER BY startTime ASC")
    fun getAllEvents(): List<TimelineEventEntity>

    @Query("SELECT * FROM timeline_events WHERE isDeleted = 0 AND startTime >= :start AND startTime <= :end ORDER BY startTime ASC")
    fun getEventsInRange(start: Long, end: Long): List<TimelineEventEntity>

    @Query("SELECT * FROM timeline_events WHERE isDeleted = 0 AND sourceMode = :mode ORDER BY startTime ASC")
    fun getEventsByMode(mode: SourceMode): List<TimelineEventEntity>

    @Query("SELECT * FROM timeline_events WHERE id = :id AND isDeleted = 0")
    fun getEventById(id: Long): TimelineEventEntity?

    @Query("SELECT * FROM timeline_events WHERE sourceId = :sourceId AND sourceTable = :sourceTable AND isDeleted = 0")
    fun getBySourceId(sourceId: Long, sourceTable: String): TimelineEventEntity?

    @Insert
    fun insertEvent(event: TimelineEventEntity): Long

    @Update
    fun updateEvent(event: TimelineEventEntity)

    @Query("UPDATE timeline_events SET isCompleted = 1, updatedAt = :updatedAt WHERE id = :id")
    fun completeEvent(id: Long, updatedAt: Long = System.currentTimeMillis()): Int

    @Query("UPDATE timeline_events SET isDeleted = 1 WHERE id = :id")
    fun softDeleteEvent(id: Long): Int

    @Query("DELETE FROM timeline_events WHERE isDeleted = 1")
    fun cleanDeletedEvents(): Int
}
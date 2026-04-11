// 路径：com/memoria/meaningoflife/data/repository/TimelineRepository.kt
package com.memoria.meaningoflife.data.repository

import com.memoria.meaningoflife.data.database.AppDatabase
import com.memoria.meaningoflife.data.database.timeline.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TimelineRepository(private val database: AppDatabase) {

    // ========== Timeline Event ==========
    suspend fun getAllEvents(): List<TimelineEventEntity> = withContext(Dispatchers.IO) {
        database.timelineEventDao().getAllEvents()
    }

    suspend fun getEventsInRange(start: Long, end: Long): List<TimelineEventEntity> = withContext(Dispatchers.IO) {
        database.timelineEventDao().getEventsInRange(start, end)
    }

    suspend fun getEventsByMode(mode: SourceMode): List<TimelineEventEntity> = withContext(Dispatchers.IO) {
        database.timelineEventDao().getEventsByMode(mode)
    }

    suspend fun insertEvent(event: TimelineEventEntity): Long = withContext(Dispatchers.IO) {
        database.timelineEventDao().insertEvent(event)
    }

    suspend fun updateEvent(event: TimelineEventEntity) = withContext(Dispatchers.IO) {
        database.timelineEventDao().updateEvent(event)
    }

    suspend fun completeEvent(id: Long) = withContext(Dispatchers.IO) {
        database.timelineEventDao().completeEvent(id)
    }

    suspend fun deleteEvent(id: Long) = withContext(Dispatchers.IO) {
        database.timelineEventDao().softDeleteEvent(id)
    }

    suspend fun getEventById(id: Long): TimelineEventEntity? = withContext(Dispatchers.IO) {
        database.timelineEventDao().getEventById(id)
    }

    // ========== Course ==========
    suspend fun getAllCourses(): List<CourseEntity> = withContext(Dispatchers.IO) {
        database.courseDao().getAllCourses()
    }

    suspend fun getCoursesBySemester(semesterId: Long): List<CourseEntity> = withContext(Dispatchers.IO) {
        database.courseDao().getCoursesBySemester(semesterId)
    }

    suspend fun getCoursesBySemesterList(semesterId: Long): List<CourseEntity> = withContext(Dispatchers.IO) {
        database.courseDao().getCoursesBySemester(semesterId)
    }

    suspend fun insertCourse(course: CourseEntity): Long = withContext(Dispatchers.IO) {
        database.courseDao().insertCourse(course)
    }

    suspend fun updateCourse(course: CourseEntity) = withContext(Dispatchers.IO) {
        database.courseDao().updateCourse(course)
    }

    suspend fun deleteCourse(id: Long) = withContext(Dispatchers.IO) {
        database.courseDao().softDeleteCourse(id)
    }

    suspend fun getCourseById(id: Long): CourseEntity? = withContext(Dispatchers.IO) {
        database.courseDao().getCourseById(id)
    }

    // ========== Semester ==========
    suspend fun getAllSemesters(): List<SemesterEntity> = withContext(Dispatchers.IO) {
        database.semesterDao().getAllSemesters()
    }

    suspend fun getCurrentSemester(): SemesterEntity? = withContext(Dispatchers.IO) {
        database.semesterDao().getCurrentSemester()
    }

    suspend fun setCurrentSemester(semester: SemesterEntity) = withContext(Dispatchers.IO) {
        database.semesterDao().clearCurrentSemester()
        database.semesterDao().updateSemester(semester.copy(isCurrent = true))
    }

    suspend fun insertSemester(semester: SemesterEntity): Long = withContext(Dispatchers.IO) {
        if (semester.isCurrent) {
            database.semesterDao().clearCurrentSemester()
        }
        database.semesterDao().insertSemester(semester)
    }
}
// 路径：com/memoria/meaningoflife/data/database/timeline/CourseDao.kt
package com.memoria.meaningoflife.data.database.timeline

import androidx.room.*

@Dao
interface CourseDao {

    @Query("SELECT * FROM courses WHERE isDeleted = 0 ORDER BY weekDay ASC, startTime ASC")
    fun getAllCourses(): List<CourseEntity>

    @Query("SELECT * FROM courses WHERE isDeleted = 0 AND semesterId = :semesterId")
    fun getCoursesBySemester(semesterId: Long): List<CourseEntity>

    @Query("SELECT * FROM courses WHERE id = :id AND isDeleted = 0")
    fun getCourseById(id: Long): CourseEntity?

    @Query("SELECT * FROM courses WHERE isDeleted = 0 AND weekDay = :weekDay ORDER BY startTime ASC")
    fun getCoursesByWeekDay(weekDay: Int): List<CourseEntity>

    @Insert
    fun insertCourse(course: CourseEntity): Long

    @Update
    fun updateCourse(course: CourseEntity)

    @Query("UPDATE courses SET isDeleted = 1 WHERE id = :id")
    fun softDeleteCourse(id: Long): Int

    @Query("DELETE FROM courses WHERE isDeleted = 1")
    fun cleanDeletedCourses(): Int
}
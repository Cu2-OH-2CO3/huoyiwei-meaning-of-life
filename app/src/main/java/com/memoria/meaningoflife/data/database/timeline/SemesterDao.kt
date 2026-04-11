// 路径：com/memoria/meaningoflife/data/database/timeline/SemesterDao.kt
package com.memoria.meaningoflife.data.database.timeline

import androidx.room.*

@Dao
interface SemesterDao {

    @Query("SELECT * FROM semesters ORDER BY isCurrent DESC, startDate DESC")
    fun getAllSemesters(): List<SemesterEntity>

    @Query("SELECT * FROM semesters WHERE isCurrent = 1")
    fun getCurrentSemester(): SemesterEntity?

    @Insert
    fun insertSemester(semester: SemesterEntity): Long

    @Update
    fun updateSemester(semester: SemesterEntity)

    @Query("UPDATE semesters SET isCurrent = 0")
    fun clearCurrentSemester(): Int

    @Query("DELETE FROM semesters WHERE id = :id")
    fun deleteSemester(id: Long): Int
}
// 路径：com/memoria/meaningoflife/data/database/timeline/IcsImportDao.kt
package com.memoria.meaningoflife.data.database.timeline

import androidx.room.*

@Dao
interface IcsImportDao {

    @Query("SELECT * FROM ics_imports ORDER BY importTime DESC")
    suspend fun getAllImports(): List<IcsImportEntity>

    @Insert
    suspend fun insertImport(import: IcsImportEntity): Long

    @Query("DELETE FROM ics_imports WHERE id = :id")
    suspend fun deleteImport(id: Long)
}
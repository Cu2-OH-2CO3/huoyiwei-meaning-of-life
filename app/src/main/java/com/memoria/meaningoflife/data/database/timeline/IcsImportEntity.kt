// 路径：data/database/timeline/IcsImportEntity.kt
package com.memoria.meaningoflife.data.database.timeline

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ics_imports")
data class IcsImportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val filePath: String? = null,
    val eventCount: Int,
    val courseCount: Int,
    val semesterId: Long? = null,
    val importTime: Long = System.currentTimeMillis(),
    val lastSyncTime: Long? = null
)
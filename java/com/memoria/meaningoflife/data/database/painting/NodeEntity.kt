package com.memoria.meaningoflife.data.database.painting

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "nodes",
    foreignKeys = [
        ForeignKey(
            entity = WorkEntity::class,
            parentColumns = ["id"],
            childColumns = ["work_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("work_id")]
)
data class NodeEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "work_id")
    val workId: Long,

    @ColumnInfo(name = "node_order")
    val nodeOrder: Int,

    @ColumnInfo(name = "image_path")
    val imagePath: String? = null,

    @ColumnInfo(name = "duration")
    val duration: Int = 0,

    @ColumnInfo(name = "cumulative_duration")
    val cumulativeDuration: Int = 0,

    @ColumnInfo(name = "note")
    val note: String? = null,

    @ColumnInfo(name = "reference_image_path")
    val referenceImagePath: String? = null,

    @ColumnInfo(name = "inspiration")
    val inspiration: String? = null,

    @ColumnInfo(name = "created_time")
    val createdTime: Long = System.currentTimeMillis()
)
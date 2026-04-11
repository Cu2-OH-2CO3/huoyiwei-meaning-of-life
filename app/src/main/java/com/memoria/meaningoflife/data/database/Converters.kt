package com.memoria.meaningoflife.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.memoria.meaningoflife.data.database.timeline.EventType
import com.memoria.meaningoflife.data.database.timeline.SourceMode

class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(it, type)
        }
    }

    @TypeConverter
    fun fromLongList(value: List<Long>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toLongList(value: String?): List<Long>? {
        return value?.let {
            val type = object : TypeToken<List<Long>>() {}.type
            gson.fromJson(it, type)
        }
    }
    // ==================== 时间轴模块转换器 ====================

    @TypeConverter
    fun fromEventType(value: EventType): String = value.name

    @TypeConverter
    fun toEventType(value: String): EventType = EventType.valueOf(value)

    @TypeConverter
    fun fromSourceMode(value: SourceMode): String = value.name

    @TypeConverter
    fun toSourceMode(value: String): SourceMode = SourceMode.valueOf(value)
}
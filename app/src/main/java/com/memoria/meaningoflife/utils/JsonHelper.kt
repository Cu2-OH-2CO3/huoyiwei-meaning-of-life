package com.memoria.meaningoflife.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object JsonHelper {

    private val gson = Gson()

    fun imagesToJson(paths: List<String>): String {
        return gson.toJson(paths)
    }

    fun jsonToImages(json: String): List<String> {
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun tagsToJson(tags: List<String>): String {
        return gson.toJson(tags)
    }

    fun jsonToTags(json: String): List<String> {
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
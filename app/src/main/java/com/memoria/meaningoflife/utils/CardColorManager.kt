package com.memoria.meaningoflife.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object CardColorManager {

    private const val PREFS_NAME = "card_colors"
    private const val KEY_PAINTING = "painting_color"
    private const val KEY_DIARY = "diary_color"
    private const val KEY_LUNCH = "lunch_color"
    private const val KEY_BACKUP = "backup_color"
    private const val KEY_CUSTOM_PRESETS = "custom_presets"

    private val DEFAULT_PRESETS: Map<String, List<String>> = mapOf(
        "本色" to listOf("#5D7A5C", "#9B8E7C", "#FF8C42", "#FF8C42"),
        "藍二乗" to listOf("#78C7D2", "#78D1E5", "#88C6ED", "#89ABE3"),
        "花綠青" to listOf("#A5D1B5", "#6BB392", "#53976F", "#1C8D6C"),
        "烏の歌に茜" to listOf("#FEA837", "#B85B56", "#84495F", "#593E67")
    )

    private val gson = Gson()

    fun getPaintingCardColor(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val colorStr = prefs.getString(KEY_PAINTING, "#5D7A5C") ?: "#5D7A5C"
        return android.graphics.Color.parseColor(colorStr)
    }

    fun getDiaryCardColor(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val colorStr = prefs.getString(KEY_DIARY, "#9B8E7C") ?: "#9B8E7C"
        return android.graphics.Color.parseColor(colorStr)
    }

    fun getLunchCardColor(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val colorStr = prefs.getString(KEY_LUNCH, "#FF8C42") ?: "#FF8C42"
        return android.graphics.Color.parseColor(colorStr)
    }

    fun getBackupCardColor(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val colorStr = prefs.getString(KEY_BACKUP, "#FF8C42") ?: "#FF8C42"
        return android.graphics.Color.parseColor(colorStr)
    }

    fun setPaintingCardColor(context: Context, colorStr: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PAINTING, colorStr).apply()
    }

    fun setDiaryCardColor(context: Context, colorStr: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DIARY, colorStr).apply()
    }

    fun setLunchCardColor(context: Context, colorStr: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LUNCH, colorStr).apply()
    }

    fun setBackupCardColor(context: Context, colorStr: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BACKUP, colorStr).apply()
    }

    fun applyPreset(context: Context, presetName: String) {
        val colors = getPresetColors(context, presetName)
        if (colors.isNotEmpty()) {
            setPaintingCardColor(context, colors[0])
            setDiaryCardColor(context, colors[1])
            setLunchCardColor(context, colors[2])
            setBackupCardColor(context, colors[3])
        }
    }

    fun getPresetNames(context: Context): List<String> {
        val allPresets = getAllPresets(context)
        return allPresets.keys.toList()
    }

    fun getPresetColors(context: Context, presetName: String): List<String> {
        val allPresets = getAllPresets(context)
        return allPresets[presetName] ?: emptyList()
    }

    fun saveCustomPreset(context: Context, name: String, colors: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val presets = getAllPresets(context).toMutableMap()
        presets[name] = colors
        val json = gson.toJson(presets)
        prefs.edit().putString(KEY_CUSTOM_PRESETS, json).apply()
    }

    private fun getAllPresets(context: Context): Map<String, List<String>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_CUSTOM_PRESETS, null)
        val customPresets: Map<String, List<String>> = if (json != null) {
            try {
                val type = object : TypeToken<Map<String, List<String>>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                emptyMap()
            }
        } else {
            emptyMap()
        }
        // 合并默认预设和自定义预设
        val result = mutableMapOf<String, List<String>>()
        result.putAll(DEFAULT_PRESETS)
        result.putAll(customPresets)
        return result
    }
}
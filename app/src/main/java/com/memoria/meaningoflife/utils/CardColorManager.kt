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
    private const val KEY_TASK = "task_color"
    private const val KEY_BACKUP = "backup_color"
    private const val KEY_CUSTOM_PRESETS = "custom_presets"
    private const val KEY_CURRENT_PRESET = "current_preset"

    private const val KEY_TIMELINE_COLOR = "timeline_card_color"

    // 默认预设（5个颜色：绘画、日记、午餐、待办、备份）
    private val DEFAULT_PRESETS: Map<String, List<String>> = mapOf(
        "本色" to listOf("#5D7A5C", "#9B8E7C", "#FF8C42", "#9C27B0", "#FF8C42", "#FF8C42"),
        "藍二乗" to listOf("#78C7D2", "#78D1E5", "#88C6ED", "#89ABE3", "#446CCF", "#446CCF"),
        "花綠青" to listOf("#A5D1B5", "#6BB392", "#53976F", "#1C8D6C", "#007D62", "#007D62"),
        "烏の歌に茜" to listOf("#FEA837", "#DE741C", "#B85B56", "#84495F", "#593E67", "#593E67"),
        "無我夢中" to listOf("#C0C4C3", "#810100", "#1B1717", "#630000", "#BE0208", "#BE0208")

    )

    private val gson = Gson()

    // ==================== 设置卡片颜色 ====================

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

    fun setTaskCardColor(context: Context, colorStr: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TASK, colorStr).apply()
    }

    fun setBackupCardColor(context: Context, colorStr: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BACKUP, colorStr).apply()
    }

    fun setTimelineCardColor(context: Context, colorStr: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TIMELINE_COLOR, colorStr).apply()
    }



    // ==================== 获取卡片颜色（返回十六进制字符串） ====================

    // 这些方法应该返回 Int（颜色值），而不是 String
    fun getPaintingCardColorHex(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val colorStr = prefs.getString(KEY_PAINTING, "#5D7A5C") ?: "#5D7A5C"
        return android.graphics.Color.parseColor(colorStr)  // 返回 Int
    }

    fun getDiaryCardColorHex(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val colorStr = prefs.getString(KEY_DIARY, "#9B8E7C") ?: "#9B8E7C"
        return android.graphics.Color.parseColor(colorStr)
    }

    fun getLunchCardColorHex(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val colorStr = prefs.getString(KEY_LUNCH, "#FF8C42") ?: "#FF8C42"
        return android.graphics.Color.parseColor(colorStr)
    }

    fun getTaskCardColorHex(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val colorStr = prefs.getString(KEY_TASK, "#9C27B0") ?: "#9C27B0"
        return android.graphics.Color.parseColor(colorStr)
    }

    fun getBackupCardColorHex(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val colorStr = prefs.getString(KEY_BACKUP, "#FF8C42") ?: "#FF8C42"
        return android.graphics.Color.parseColor(colorStr)
    }

    fun getTimelineCardColorHex(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val colorStr = prefs.getString(KEY_BACKUP, "#7C3AED") ?: "#7C3AED"
        return android.graphics.Color.parseColor(colorStr)
    }


    // ==================== 预设管理 ====================

    fun getCurrentPreset(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CURRENT_PRESET, "本色") ?: "本色"
    }

    fun applyPreset(context: Context, presetName: String) {
        val colors = getPresetColors(context, presetName)
        if (colors.size >= 5) {
            setPaintingCardColor(context, colors[0])
            setDiaryCardColor(context, colors[1])
            setLunchCardColor(context, colors[2])
            setTaskCardColor(context, colors[3])
            setBackupCardColor(context, colors[4])

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_CURRENT_PRESET, presetName).apply()
        }
    }

    fun getPresetNames(context: Context): List<String> {
        val allPresets = getAllPresets(context)
        return allPresets.keys.toList()
    }

    fun getPresetColors(context: Context, presetName: String): List<String> {
        val allPresets = getAllPresets(context)
        return allPresets[presetName] ?: DEFAULT_PRESETS["本色"] ?: emptyList()
    }

    fun saveCustomPreset(context: Context, name: String, colors: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val presets = getAllPresets(context).toMutableMap()
        presets[name] = colors
        val json = gson.toJson(presets)
        prefs.edit().putString(KEY_CUSTOM_PRESETS, json).apply()
    }

    fun deleteCustomPreset(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val presets = getAllPresets(context).toMutableMap()
        presets.remove(name)
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
        val result = mutableMapOf<String, List<String>>()
        result.putAll(DEFAULT_PRESETS)
        result.putAll(customPresets)
        return result
    }
}
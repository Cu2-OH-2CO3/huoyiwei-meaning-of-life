// 路径：com/memoria/meaningoflife/utils/PreferenceManager.kt
package com.memoria.meaningoflife.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * 应用偏好设置管理类
 * 统一管理所有 SharedPreferences 键值
 */
object PreferenceManager {

    private const val PREFS_NAME = "app_preferences"

    // ==================== 键名常量 ====================
    private const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_THEME_COLOR = "theme_color"
    private const val KEY_FIRST_LAUNCH = "first_launch"
    private const val KEY_HIDDEN_MODULES = "hidden_modules"
    private const val KEY_DAILY_QUOTE_INDEX = "daily_quote_index"
    private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
    private const val KEY_SCHEDULE_SHOW_NON_COURSE_EVENTS = "schedule_show_non_course_events"

    // ==================== 私有方法 ====================
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ==================== 自动备份 ====================
    fun isAutoBackupEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUTO_BACKUP_ENABLED, true)
    }

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUTO_BACKUP_ENABLED, enabled).apply()
    }

    fun getLastBackupTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_BACKUP_TIME, 0)
    }

    fun setLastBackupTime(context: Context, time: Long) {
        getPrefs(context).edit().putLong(KEY_LAST_BACKUP_TIME, time).apply()
    }

    // ==================== 深色模式 ====================
    fun isDarkModeEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DARK_MODE, false)
    }

    fun setDarkModeEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    // ==================== 主题颜色 ====================
    fun getThemeColor(context: Context): String {
        return getPrefs(context).getString(KEY_THEME_COLOR, "#FF8C42") ?: "#FF8C42"
    }

    fun setThemeColor(context: Context, color: String) {
        getPrefs(context).edit().putString(KEY_THEME_COLOR, color).apply()
    }

    // ==================== 首次启动 ====================
    fun isFirstLaunch(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun setFirstLaunchCompleted(context: Context) {
        getPrefs(context).edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    // ==================== 隐藏模块 ====================
    fun getHiddenModules(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_HIDDEN_MODULES, emptySet()) ?: emptySet()
    }

    fun setHiddenModules(context: Context, modules: Set<String>) {
        getPrefs(context).edit().putStringSet(KEY_HIDDEN_MODULES, modules).apply()
    }

    fun addHiddenModule(context: Context, moduleId: String) {
        val current = getHiddenModules(context).toMutableSet()
        current.add(moduleId)
        setHiddenModules(context, current)
    }

    fun removeHiddenModule(context: Context, moduleId: String) {
        val current = getHiddenModules(context).toMutableSet()
        current.remove(moduleId)
        setHiddenModules(context, current)
    }

    // ==================== 每日一言 ====================
    fun getDailyQuoteIndex(context: Context): Int {
        return getPrefs(context).getInt(KEY_DAILY_QUOTE_INDEX, 0)
    }

    fun setDailyQuoteIndex(context: Context, index: Int) {
        getPrefs(context).edit().putInt(KEY_DAILY_QUOTE_INDEX, index).apply()
    }

    // ==================== 课表模式显示设置 ====================
    fun isScheduleNonCourseVisible(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SCHEDULE_SHOW_NON_COURSE_EVENTS, false)
    }

    fun setScheduleNonCourseVisible(context: Context, visible: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SCHEDULE_SHOW_NON_COURSE_EVENTS, visible).apply()
    }
}
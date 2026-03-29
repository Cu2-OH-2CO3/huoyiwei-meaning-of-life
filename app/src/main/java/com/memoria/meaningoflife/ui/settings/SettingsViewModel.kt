package com.memoria.meaningoflife.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import com.memoria.meaningoflife.MeaningOfLifeApp

class SettingsViewModel : ViewModel() {

    private val prefs: SharedPreferences = MeaningOfLifeApp.instance.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun getVersionName(): String {
        return try {
            val packageInfo = MeaningOfLifeApp.instance.packageManager.getPackageInfo(MeaningOfLifeApp.instance.packageName, 0)
            packageInfo.versionName
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    fun isDarkModeEnabled(): Boolean {
        return prefs.getBoolean("dark_mode", false)
    }

    fun saveDarkModePreference(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode", enabled).apply()
        android.util.Log.d("SettingsViewModel", "Dark mode saved: $enabled")
    }

    fun getThemePreference(): Int {
        return prefs.getInt("theme_preference", 0)
    }

    fun saveThemePreference(position: Int) {
        prefs.edit().putInt("theme_preference", position).apply()
        android.util.Log.d("SettingsViewModel", "Theme saved: $position")
    }

    fun getDailyReminderEnabled(): Boolean {
        return prefs.getBoolean("daily_reminder", true)
    }

    fun saveDailyReminder(enabled: Boolean) {
        prefs.edit().putBoolean("daily_reminder", enabled).apply()
    }

    fun getLunchReminderTime(): String {
        return prefs.getString("lunch_reminder_time", "12:00") ?: "12:00"
    }

    fun saveLunchReminderTime(time: String) {
        prefs.edit().putString("lunch_reminder_time", time).apply()
    }
}
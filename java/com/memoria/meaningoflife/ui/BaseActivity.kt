package com.memoria.meaningoflife.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.memoria.meaningoflife.R

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // 先获取保存的设置
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)

        // 在 setTheme 之前应用深色模式
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        // 应用主题
        val themePosition = prefs.getInt("theme_preference", 0)
        when (themePosition) {
            0 -> setTheme(R.style.Theme_MeaningOfLife_Orange)
            1 -> setTheme(R.style.Theme_MeaningOfLife_Green)
            2 -> setTheme(R.style.Theme_MeaningOfLife_Blue)
            3 -> setTheme(R.style.Theme_MeaningOfLife_Purple)
            4 -> setTheme(R.style.Theme_MeaningOfLife_Pink)
            5 -> setTheme(R.style.Theme_MeaningOfLife_Red)
            else -> setTheme(R.style.Theme_MeaningOfLife_Orange)
        }

        super.onCreate(savedInstanceState)
    }
}
package com.memoria.meaningoflife.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.ColorUtils
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.button.MaterialButton
import com.memoria.meaningoflife.R

object ThemeManager {

    fun applyTheme(activity: AppCompatActivity) {
        val primaryColor = resolvePrimaryColor(activity)

        // 设置状态栏和导航栏
        activity.window.statusBarColor = primaryColor
        activity.window.navigationBarColor = primaryColor

        // 设置 ActionBar 颜色
        activity.supportActionBar?.setBackgroundDrawable(ColorDrawable(primaryColor))

        // 设置按钮和开关的主题色
        applyThemeToViews(activity.window.decorView, primaryColor)
    }

    fun resolvePrimaryColor(context: Context): Int {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val themePosition = prefs.getInt("theme_preference", 0)
        val themeRes = when (themePosition) {
            0 -> R.style.Theme_MeaningOfLife_Orange
            1 -> R.style.Theme_MeaningOfLife_Green
            2 -> R.style.Theme_MeaningOfLife_Blue
            3 -> R.style.Theme_MeaningOfLife_Purple
            4 -> R.style.Theme_MeaningOfLife_Pink
            5 -> R.style.Theme_MeaningOfLife_Red
            else -> R.style.Theme_MeaningOfLife_Orange
        }

        val themedContext = ContextThemeWrapper(context, themeRes)
        val typedValue = TypedValue()
        // 基于已保存主题解析 colorPrimary，避免回退到默认色。
        val resolved = themedContext.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
                || themedContext.theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)
                || themedContext.theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)

        if (!resolved) return Color.BLACK

        return when {
            typedValue.resourceId != 0 -> ContextCompat.getColor(themedContext, typedValue.resourceId)
            else -> typedValue.data
        }
    }

    fun tintSwitch(switchCompat: SwitchCompat, primaryColor: Int = resolvePrimaryColor(switchCompat.context)) {
        val checkedState = intArrayOf(android.R.attr.state_checked)
        val defaultState = intArrayOf()
        switchCompat.thumbTintList = ColorStateList(
            arrayOf(checkedState, defaultState),
            intArrayOf(primaryColor, ColorUtils.setAlphaComponent(primaryColor, 140))
        )
        switchCompat.trackTintList = ColorStateList(
            arrayOf(checkedState, defaultState),
            intArrayOf(ColorUtils.setAlphaComponent(primaryColor, 160), ColorUtils.setAlphaComponent(primaryColor, 80))
        )
    }

    fun tintFab(fab: FloatingActionButton, primaryColor: Int = resolvePrimaryColor(fab.context)) {
        fab.backgroundTintList = ColorStateList.valueOf(primaryColor)
    }

    private fun applyThemeToViews(view: View, primaryColor: Int) {
        if (view is MaterialButton) {
            try {
                view.backgroundTintList = ColorStateList.valueOf(primaryColor)
            } catch (e: Exception) { }
        }

        if (view is SwitchCompat) {
            try {
                tintSwitch(view, primaryColor)
            } catch (e: Exception) { }
        }

        if (view is FloatingActionButton) {
            try {
                tintFab(view, primaryColor)
            } catch (e: Exception) { }
        }

        if (view is Toolbar) {
            try {
                view.setBackgroundColor(primaryColor)
            } catch (e: Exception) { }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyThemeToViews(view.getChildAt(i), primaryColor)
            }
        }
    }
}
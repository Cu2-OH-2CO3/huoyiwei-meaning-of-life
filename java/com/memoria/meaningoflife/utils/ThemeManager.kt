package com.memoria.meaningoflife.utils

import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.button.MaterialButton
import com.memoria.meaningoflife.R

object ThemeManager {

    fun applyTheme(activity: AppCompatActivity) {
        // 从主题中获取颜色
        val typedValue = TypedValue()
        activity.theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
        val primaryColor = typedValue.data

        // 设置状态栏和导航栏
        activity.window.statusBarColor = primaryColor
        activity.window.navigationBarColor = primaryColor

        // 设置 ActionBar 颜色
        activity.supportActionBar?.setBackgroundDrawable(ColorDrawable(primaryColor))

        // 设置按钮和开关的主题色
        applyThemeToViews(activity.window.decorView, primaryColor)
    }

    private fun applyThemeToViews(view: View, primaryColor: Int) {
        if (view is MaterialButton) {
            try {
                view.backgroundTintList = android.content.res.ColorStateList.valueOf(primaryColor)
            } catch (e: Exception) { }
        }

        if (view is SwitchCompat) {
            try {
                view.trackTintList = android.content.res.ColorStateList.valueOf(primaryColor)
                view.thumbTintList = android.content.res.ColorStateList.valueOf(primaryColor)
            } catch (e: Exception) { }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyThemeToViews(view.getChildAt(i), primaryColor)
            }
        }
    }
}
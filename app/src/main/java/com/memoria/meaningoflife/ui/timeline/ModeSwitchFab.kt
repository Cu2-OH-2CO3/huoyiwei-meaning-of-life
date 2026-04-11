// 路径：com/memoria/meaningoflife/ui/timeline/ModeSwitchFab.kt
package com.memoria.meaningoflife.ui.timeline

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.memoria.meaningoflife.R

class ModeSwitchFab @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FloatingActionButton(context, attrs, defStyleAttr) {

    enum class Mode {
        SCHEDULE, FREE
    }

    var currentMode: Mode = Mode.SCHEDULE
        private set

    private var onModeChangedListener: ((Mode) -> Unit)? = null

    init {
        // 获取主题色
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
        val primaryColor = typedValue.data

        backgroundTintList = ColorStateList.valueOf(primaryColor)
        imageTintList = ColorStateList.valueOf(Color.WHITE)
        updateModeIcon()

        setOnClickListener {
            toggleMode()
        }
    }

    private fun updateModeIcon() {
        // 约定：in=FREE, out=SCHEDULE。使用强引用避免 release 资源收缩误删。
        val iconRes = when (currentMode) {
            Mode.FREE -> R.drawable.ic_mode_in
            Mode.SCHEDULE -> R.drawable.ic_mode_out
        }
        setImageResource(iconRes)
    }

    fun toggleMode() {
        val targetMode = if (currentMode == Mode.SCHEDULE) Mode.FREE else Mode.SCHEDULE
        switchToMode(targetMode)
    }

    @Suppress("UNUSED_PARAMETER")
    fun switchToMode(mode: Mode, animate: Boolean = true) {
        if (currentMode == mode) return

        currentMode = mode
        updateModeIcon()
        onModeChangedListener?.invoke(mode)
    }

    fun setOnModeChangedListener(listener: (Mode) -> Unit) {
        onModeChangedListener = listener
    }

    override fun onDetachedFromWindow() = super.onDetachedFromWindow()
}
package com.memoria.meaningoflife.ui.timeline

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import com.memoria.meaningoflife.ui.BaseActivity

class TimelineActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = FrameLayout(this).apply {
            id = View.generateViewId()
            setBackgroundResource(com.memoria.meaningoflife.R.color.background)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        setContentView(container)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(container.id, TimelineMainFragment())
                .commit()
        }
    }
}


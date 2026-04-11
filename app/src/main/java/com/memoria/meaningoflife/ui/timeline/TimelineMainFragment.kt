// 路径：com/memoria/meaningoflife/ui/timeline/TimelineMainFragment.kt
package com.memoria.meaningoflife.ui.timeline

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.ui.timeline.free.FreeTimelineFragment
import com.memoria.meaningoflife.ui.timeline.schedule.ScheduleFragment

class TimelineMainFragment : Fragment() {

    companion object {
        private const val TAG_SCHEDULE = "schedule_fragment"
        private const val TAG_FREE = "free_fragment"
        private const val KEY_MODE = "timeline_mode"
    }

    private lateinit var modeSwitchFab: ModeSwitchFab
    private lateinit var scheduleFragment: ScheduleFragment
    private lateinit var freeTimelineFragment: FreeTimelineFragment

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_timeline_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupModeSwitchFab(view)

        // 无论冷启动还是恢复，均确保子 Fragment 已添加，避免 show/hide 时崩溃
        ensureFragments()

        val restoredMode = savedInstanceState?.getString(KEY_MODE)
        val initialMode = if (restoredMode == ModeSwitchFab.Mode.FREE.name) {
            ModeSwitchFab.Mode.FREE
        } else {
            ModeSwitchFab.Mode.SCHEDULE
        }

        modeSwitchFab.switchToMode(initialMode, animate = false)
        if (initialMode == ModeSwitchFab.Mode.FREE) {
            showFreeMode()
        } else {
            showScheduleMode()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_MODE, modeSwitchFab.currentMode.name)
    }

    private fun setupModeSwitchFab(view: View) {
        modeSwitchFab = ModeSwitchFab(requireContext())

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
            setMargins(0, 0, 32, 32)
        }

        (view as? ViewGroup)?.addView(modeSwitchFab, params)

        modeSwitchFab.setOnModeChangedListener { mode ->
            when (mode) {
                ModeSwitchFab.Mode.SCHEDULE -> showScheduleMode()
                ModeSwitchFab.Mode.FREE -> showFreeMode()
            }
        }
    }

    private fun ensureFragments() {
        scheduleFragment = childFragmentManager.findFragmentByTag(TAG_SCHEDULE) as? ScheduleFragment
            ?: ScheduleFragment()
        freeTimelineFragment = childFragmentManager.findFragmentByTag(TAG_FREE) as? FreeTimelineFragment
            ?: FreeTimelineFragment()

        val transaction = childFragmentManager.beginTransaction()

        if (!scheduleFragment.isAdded) {
            transaction.add(R.id.fragment_container, scheduleFragment, TAG_SCHEDULE)
        }
        if (!freeTimelineFragment.isAdded) {
            transaction.add(R.id.fragment_container, freeTimelineFragment, TAG_FREE)
        }

        // 默认仅显示课表模式
        transaction.hide(freeTimelineFragment).show(scheduleFragment)
        transaction.commitNowAllowingStateLoss()
    }

    private fun showScheduleMode() {
        if (!isAdded || childFragmentManager.isStateSaved) return
        if (!scheduleFragment.isAdded || !freeTimelineFragment.isAdded) return

        childFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .hide(freeTimelineFragment)
            .show(scheduleFragment)
            .commit()
    }

    private fun showFreeMode() {
        if (!isAdded || childFragmentManager.isStateSaved) return
        if (!scheduleFragment.isAdded || !freeTimelineFragment.isAdded) return

        childFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .hide(scheduleFragment)
            .show(freeTimelineFragment)
            .commit()
    }
}
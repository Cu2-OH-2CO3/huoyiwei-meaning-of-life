package com.memoria.meaningoflife.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.memoria.meaningoflife.ui.calendar.CalendarFragment
import com.memoria.meaningoflife.ui.home.HomeFragment
import com.memoria.meaningoflife.ui.settings.SettingsFragment

class MainPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1 -> CalendarFragment()
            2 -> SettingsFragment()
            else -> HomeFragment()
        }
    }
}
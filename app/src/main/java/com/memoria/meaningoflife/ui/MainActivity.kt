package com.memoria.meaningoflife.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.util.TypedValue
import androidx.appcompat.app.AppCompatDelegate
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var pagerAdapter: MainPagerAdapter
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("settings", MODE_PRIVATE)

        // 应用深色模式
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupBottomNavigation()
        applyThemeColors()
    }

    private fun applyThemeColors() {
        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
        val primaryColor = typedValue.data

        window.statusBarColor = primaryColor
        window.navigationBarColor = primaryColor
        binding.bottomNav.itemIconTintList = android.content.res.ColorStateList.valueOf(primaryColor)
        binding.bottomNav.itemTextColor = android.content.res.ColorStateList.valueOf(primaryColor)
    }

    private fun setupViewPager() {
        pagerAdapter = MainPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.isUserInputEnabled = false

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> binding.bottomNav.menu.findItem(R.id.nav_home).isChecked = true
                    1 -> binding.bottomNav.menu.findItem(R.id.nav_calendar).isChecked = true
                    2 -> binding.bottomNav.menu.findItem(R.id.nav_settings).isChecked = true
                }
            }
        })
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    binding.viewPager.currentItem = 0
                    true
                }
                R.id.nav_calendar -> {
                    binding.viewPager.currentItem = 1
                    true
                }
                R.id.nav_settings -> {
                    binding.viewPager.currentItem = 2
                    true
                }
                else -> false
            }
        }
    }
}
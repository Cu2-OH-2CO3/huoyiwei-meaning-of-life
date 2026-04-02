package com.memoria.meaningoflife.ui.settings

import android.os.Bundle
import android.util.TypedValue
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.databinding.ActivityIntroBinding
import com.memoria.meaningoflife.ui.BaseActivity

class IntroActivity : BaseActivity() {

    private lateinit var binding: ActivityIntroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "功能介绍"

        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
        val primaryColor = typedValue.data
        supportActionBar?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(primaryColor))

        setupViewPager()
    }

    private fun setupViewPager() {
        val fragments = listOf(
            IntroFragment.newInstance("绘画记录", "记录绘画作品，添加节点，追踪绘画时长，设定目标", R.drawable.ic_painting),
            IntroFragment.newInstance("日记本", "记录每日心情，添加图片，生成高频词统计", R.drawable.ic_diary),
            IntroFragment.newInstance("午餐抽选", "随机抽选午餐，管理菜单，记录抽选历史", R.drawable.ic_lunch),
            IntroFragment.newInstance("日历视图", "查看绘画和日记完成情况，主题色标记", R.drawable.ic_calendar),
            IntroFragment.newInstance("每日一言", "自定义语句库，每日刷新展示", R.drawable.ic_quote)
        )

        val adapter = IntroPagerAdapter(this, fragments)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "绘画记录"
                1 -> "日记本"
                2 -> "午餐抽选"
                3 -> "日历视图"
                else -> "每日一言"
            }
        }.attach()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
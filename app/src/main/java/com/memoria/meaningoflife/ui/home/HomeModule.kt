package com.memoria.meaningoflife.ui.home

import com.memoria.meaningoflife.R

data class HomeModule(
    val id: String,
    val title: String,
    val iconRes: Int,
    val statsText: String,
    val color: Int,
    val badgeCount: Int = 0  // 新增：角标数量（用于显示未完成任务数）
) {
    companion object {
        fun getDefaultModules(): List<HomeModule> {
            return listOf(
                HomeModule(
                    id = "painting",
                    title = "绘画记录",
                    iconRes = R.drawable.ic_painting,
                    statsText = "0 幅作品",
                    color = android.graphics.Color.parseColor("#5D7A5C")
                ),
                HomeModule(
                    id = "diary",
                    title = "日记本",
                    iconRes = R.drawable.ic_diary,
                    statsText = "0 篇日记",
                    color = android.graphics.Color.parseColor("#9B8E7C")
                ),
                HomeModule(
                    id = "lunch",
                    title = "午餐抽选",
                    iconRes = R.drawable.ic_lunch,
                    statsText = "今日已选",
                    color = android.graphics.Color.parseColor("#FF8C42")
                ),
                HomeModule(
                    id = "task",
                    title = "待办任务",
                    iconRes = R.drawable.ic_task,
                    statsText = "0 项待办",
                    color = android.graphics.Color.parseColor("#D97706"),
                    badgeCount = 0
                ),
                // 新增时间轴模块
                HomeModule(
                    id = "timeline",
                    title = "时间轴",
                    iconRes = R.drawable.ic_timeline_module,
                    statsText = "表里模式",
                    color = android.graphics.Color.parseColor("#7C3AED"),
                    badgeCount = 0
                )
            )
        }
    }
}
package com.memoria.meaningoflife.ui.home

data class HomeModule(
    val id: String,
    val title: String,
    val iconRes: Int,
    val statsText: String,
    val color: Int,
    val badgeCount: Int = 0  // 新增：角标数量（用于显示未完成任务数）
)
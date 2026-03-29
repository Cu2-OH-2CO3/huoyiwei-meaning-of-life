package com.memoria.meaningoflife.ui.calendar

import com.memoria.meaningoflife.model.Mood

/**
 * 日历单日数据
 */
data class CalendarDayData(
    val date: String,                    // yyyy-MM-dd
    val hasPainting: Boolean = false,    // 是否有绘画记录
    val hasDiary: Boolean = false,       // 是否有日记
    val hasCheckin: Boolean = false,     // 是否签到
    val paintingCount: Int = 0,          // 作品数量
    val diaryMood: Mood? = null,         // 日记心情
    val diaryId: Long = 0,               // 日记ID（用于跳转）
    val paintingIds: List<Long> = emptyList()  // 作品ID列表
)

/**
 * 日期详情数据（点击后显示）
 */
data class DayDetailData(
    val date: String,                    // yyyy-MM-dd
    val hasPainting: Boolean = false,
    val hasDiary: Boolean = false,
    val hasCheckin: Boolean = false,
    val paintingCount: Int = 0,
    val paintingTitles: List<String> = emptyList(),
    val diaryMood: Mood? = null,
    val diaryTitle: String? = null,
    val diaryContent: String? = null,
    val diaryId: Long = 0,
    val checkinNote: String? = null,
    val dishes: List<String>? = null     // 午餐抽选历史
)

/**
 * 月度统计数据（用于统计图表）
 */
data class MonthlyStats(
    val year: Int,
    val month: Int,
    val totalPaintings: Int,              // 作品总数
    val totalDiaries: Int,                // 日记总数
    val totalCheckins: Int,               // 签到总数
    val totalDuration: Int,               // 绘画总时长（分钟）
    val moodDistribution: Map<Int, Int>,  // 心情分布
    val consecutiveDiaryDays: Int,        // 最长连续日记天数
    val consecutiveCheckinDays: Int       // 最长连续签到天数
)

/**
 * 年度统计数据
 */
data class YearlyStats(
    val year: Int,
    val monthlyData: List<MonthlyStats>,
    val totalPaintings: Int,
    val totalDiaries: Int,
    val totalCheckins: Int,
    val totalDuration: Int,
    val bestMonth: String?                // 最活跃的月份
)
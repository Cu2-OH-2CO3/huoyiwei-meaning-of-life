package com.memoria.meaningoflife.ui.home

import com.memoria.meaningoflife.model.Mood
import com.memoria.meaningoflife.model.Weather

/**
 * 绘画模块首页统计数据
 */
data class PaintingStats(
    val todayDuration: Int = 0,          // 今日绘画时长（分钟）
    val weekDuration: Int = 0,           // 本周绘画时长（分钟）
    val monthWorkCount: Int = 0,         // 本月作品数量
    val totalWorkCount: Int = 0,         // 作品总数
    val currentGoal: String? = null,     // 当前进行中的目标
    val goalProgress: Float = 0f,        // 目标进度 0-1
    val recentWorks: List<RecentWork> = emptyList()  // 最近作品
)

/**
 * 日记模块首页统计数据
 */
data class DiaryStats(
    val consecutiveDays: Int = 0,        // 连续写日记天数
    val monthCount: Int = 0,             // 本月日记数量
    val totalCount: Int = 0,             // 日记总数
    val todayMoodText: String = "未记录", // 今日心情文字
    val todayMood: Mood? = null,         // 今日心情
    val todayWeather: Weather? = null,   // 今日天气
    val hasTodayDiary: Boolean = false,  // 今日是否有日记
    val recentDiaries: List<RecentDiary> = emptyList()  // 最近日记
)

/**
 * 午餐模块首页统计数据
 */
data class LunchStats(
    val todayRecommend: String? = null,  // 今日推荐菜品
    val todayRecommendSpicy: String? = null,  // 辣度文字
    val totalDishes: Int = 0,            // 菜品总数
    val activeDishes: Int = 0,           // 启用菜品数
    val lastLotteryDate: String? = null, // 上次抽选日期
    val popularDishes: List<String> = emptyList()  // 热门菜品
)

/**
 * 首页聚合数据（用于一次性加载）
 */
data class HomeAggregatedData(
    val paintingStats: PaintingStats,
    val diaryStats: DiaryStats,
    val lunchStats: LunchStats,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * 最近作品预览
 */
data class RecentWork(
    val id: Long,
    val title: String,
    val thumbnailPath: String?,
    val createdDate: String,
    val totalDuration: Int
)

/**
 * 最近日记预览
 */
data class RecentDiary(
    val id: Long,
    val title: String?,
    val content: String,
    val mood: Mood,
    val createdDate: String
)

/**
 * 目标进度数据
 */
data class GoalProgressData(
    val goalId: Long,
    val title: String,
    val targetValue: Int,
    val currentValue: Int,
    val targetDate: String,
    val remainingDays: Int,
    val progress: Float
)
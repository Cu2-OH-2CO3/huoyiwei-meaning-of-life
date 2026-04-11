// 路径：utils/WeekCalculator.kt
package com.memoria.meaningoflife.utils

import java.util.*

object WeekCalculator {

    fun getWeekStartDate(semesterStartDate: Long, weekNumber: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = semesterStartDate
            // 获取周一日期
            val dayOfWeek = get(Calendar.DAY_OF_WEEK)
            val daysToMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
            add(Calendar.DAY_OF_YEAR, -daysToMonday)
        }
        // 加上周数偏移
        calendar.add(Calendar.WEEK_OF_YEAR, weekNumber - 1)
        return calendar.timeInMillis
    }

    fun getWeekNumber(date: Long, semesterStartDate: Long): Long {
        val diff = date - semesterStartDate
        val days = diff / (24 * 60 * 60 * 1000)
        return (days / 7) + 1
    }

    fun formatDateToChinese(date: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = date }
        return "${calendar.get(Calendar.MONTH) + 1}月${calendar.get(Calendar.DAY_OF_MONTH)}日"
    }
}
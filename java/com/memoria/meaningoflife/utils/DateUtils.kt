package com.memoria.meaningoflife.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINA)
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("MM月dd日", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())  // 新增

    fun getCurrentDate(): String = dateFormat.format(Date())

    fun getCurrentDateTime(): Long = System.currentTimeMillis()

    fun formatDate(date: String): String {
        return try {
            val d = dateFormat.parse(date)
            displayFormat.format(d!!)
        } catch (e: Exception) {
            date
        }
    }

    /**
     * 格式化时间戳为日期字符串 yyyy-MM-dd
     */
    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    /**
     * 格式化时间戳为日期时间字符串 yyyy-MM-dd HH:mm
     */
    fun formatDateTime(timestamp: Long): String {
        return dateTimeFormat.format(Date(timestamp))
    }

    fun formatShortDate(date: String): String {
        return try {
            val d = dateFormat.parse(date)
            shortDateFormat.format(d!!)
        } catch (e: Exception) {
            date
        }
    }

    fun formatTime(timestamp: Long): String = timeFormat.format(Date(timestamp))

    fun getYearMonth(date: String): String {
        return try {
            val d = dateFormat.parse(date)
            monthFormat.format(d!!)
        } catch (e: Exception) {
            date.substring(0, 7)
        }
    }

    fun getYear(date: String): String {
        return try {
            val d = dateFormat.parse(date)
            yearFormat.format(d!!)
        } catch (e: Exception) {
            date.substring(0, 4)
        }
    }

    /**
     * 获取指定月份的天数
     * @param year 年份
     * @param month 月份 (1-12)
     * @return 该月的天数
     */
    fun getDaysInMonth(year: Int, month: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1)
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    /**
     * 获取指定月份第一天是星期几
     * @param year 年份
     * @param month 月份 (1-12)
     * @return 星期几 (1=周一, 7=周日)
     */
    fun getFirstDayOfMonth(year: Int, month: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1)
        // 获取星期几，Calendar.SUNDAY = 1, Calendar.MONDAY = 2, ...
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        // 转换为周一=1, 周日=7
        return if (dayOfWeek == 1) 7 else dayOfWeek - 1
    }

    /**
     * 获取指定月份最后一天
     * @param year 年份
     * @param month 月份 (1-12)
     * @return 日期字符串 yyyy-MM-dd
     */
    fun getLastDayOfMonth(year: Int, month: Int): String {
        val days = getDaysInMonth(year, month)
        return "$year-${month.toString().padStart(2, '0')}-$days"
    }

    /**
     * 获取指定月份的第一天
     * @param year 年份
     * @param month 月份 (1-12)
     * @return 日期字符串 yyyy-MM-dd
     */
    fun getStartDateOfMonth(year: Int, month: Int): String {
        return "$year-${month.toString().padStart(2, '0')}-01"
    }

    fun parseDate(dateStr: String): Date? {
        return try {
            dateFormat.parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 格式化时长（分钟）
     * @param minutes 分钟数
     * @return 格式化的字符串，如 "2小时30分钟" 或 "45分钟"
     */
    fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}小时${mins}分钟"
            hours > 0 -> "${hours}小时"
            mins > 0 -> "${mins}分钟"
            else -> "0分钟"
        }
    }

    /**
     * 计算两个日期之间的天数差
     * @param startDate 开始日期 yyyy-MM-dd
     * @param endDate 结束日期 yyyy-MM-dd
     * @return 天数差
     */
    fun getDaysBetween(startDate: String, endDate: String): Int {
        return try {
            val start = dateFormat.parse(startDate)
            val end = dateFormat.parse(endDate)
            val diff = end!!.time - start!!.time
            (diff / (24 * 60 * 60 * 1000)).toInt()
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 计算连续天数
     * @param dates 日期列表
     * @return 最大连续天数
     */
    fun getConsecutiveDays(dates: List<String>): Int {
        if (dates.isEmpty()) return 0

        val sortedDates = dates.mapNotNull { parseDate(it) }.sorted()
        var consecutive = 1
        var maxConsecutive = 1

        for (i in 1 until sortedDates.size) {
            val diff = (sortedDates[i].time - sortedDates[i - 1].time) / (24 * 60 * 60 * 1000)
            if (diff == 1L) {
                consecutive++
                maxConsecutive = maxOf(maxConsecutive, consecutive)
            } else if (diff > 1) {
                consecutive = 1
            }
        }

        return maxConsecutive
    }

    /**
     * 获取本周的日期范围
     * @return Pair(开始日期, 结束日期)
     */
    fun getThisWeekRange(): Pair<String, String> {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val startDate = dateFormat.format(calendar.time)

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val endDate = dateFormat.format(calendar.time)

        return Pair(startDate, endDate)
    }

    /**
     * 获取本月的日期范围
     * @return Pair(开始日期, 结束日期)
     */
    fun getThisMonthRange(): Pair<String, String> {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val startDate = getStartDateOfMonth(year, month)
        val endDate = getLastDayOfMonth(year, month)
        return Pair(startDate, endDate)
    }

    /**
     * 检查日期是否在指定范围内
     * @param date 检查的日期
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 是否在范围内
     */
    fun isDateInRange(date: String, startDate: String, endDate: String): Boolean {
        return try {
            val d = parseDate(date) ?: return false
            val start = parseDate(startDate) ?: return false
            val end = parseDate(endDate) ?: return false
            d >= start && d <= end
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取当前年份
     */
    fun getCurrentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)

    /**
     * 获取当前月份 (1-12)
     */
    fun getCurrentMonth(): Int = Calendar.getInstance().get(Calendar.MONTH) + 1

    /**
     * 获取当前日期
     */
    fun getCurrentDay(): Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
}
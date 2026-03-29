package com.memoria.meaningoflife.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memoria.meaningoflife.MeaningOfLifeApp
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.data.repository.DiaryRepository
import com.memoria.meaningoflife.data.repository.LunchRepository
import com.memoria.meaningoflife.data.repository.PaintingRepository
import com.memoria.meaningoflife.model.Mood
import com.memoria.meaningoflife.utils.DateUtils
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val paintingRepository = PaintingRepository(MeaningOfLifeApp.instance.database)
    private val diaryRepository = DiaryRepository(MeaningOfLifeApp.instance.database)
    private val lunchRepository = LunchRepository(MeaningOfLifeApp.instance.database)

    private val prefs = MeaningOfLifeApp.instance.getSharedPreferences("module_prefs", android.content.Context.MODE_PRIVATE)

    // 所有可用模块
    private val allModules = listOf(
        HomeModule(
            id = "painting",
            title = "绘画记录",
            iconRes = R.drawable.ic_painting,
            statsText = "加载中...",
            color = android.graphics.Color.parseColor("#5D7A5C")
        ),
        HomeModule(
            id = "diary",
            title = "日记本",
            iconRes = R.drawable.ic_diary,
            statsText = "加载中...",
            color = android.graphics.Color.parseColor("#9B8E7C")
        ),
        HomeModule(
            id = "lunch",
            title = "午餐抽选",
            iconRes = R.drawable.ic_lunch,
            statsText = "加载中...",
            color = android.graphics.Color.parseColor("#FF8C42")
        )
    )

    private val _visibleModules = MutableLiveData<List<HomeModule>>()
    val visibleModules: LiveData<List<HomeModule>> = _visibleModules

    init {
        loadStats()
        loadVisibleModules()
    }

    private fun loadVisibleModules() {
        val hiddenIds = prefs.getStringSet("hidden_modules", emptySet()) ?: emptySet()
        val visible = allModules.filter { it.id !in hiddenIds }
        _visibleModules.postValue(visible)
    }

    fun hideModule(moduleId: String) {
        val hiddenIds = prefs.getStringSet("hidden_modules", emptySet())?.toMutableSet() ?: mutableSetOf()
        hiddenIds.add(moduleId)
        prefs.edit().putStringSet("hidden_modules", hiddenIds).apply()
        loadVisibleModules()
    }

    fun showModule(moduleId: String) {
        val hiddenIds = prefs.getStringSet("hidden_modules", emptySet())?.toMutableSet() ?: mutableSetOf()
        hiddenIds.remove(moduleId)
        prefs.edit().putStringSet("hidden_modules", hiddenIds).apply()
        loadVisibleModules()
    }

    fun getHiddenModules(): List<HomeModule> {
        val hiddenIds = prefs.getStringSet("hidden_modules", emptySet()) ?: emptySet()
        return allModules.filter { it.id in hiddenIds }
    }

    private fun loadStats() {
        viewModelScope.launch {
            // 加载绘画统计
            val currentDate = DateUtils.getCurrentDate()
            val todayDuration = paintingRepository.getTotalDurationByDateRange(currentDate, currentDate)
            val monthWorkCount = paintingRepository.getWorkCountByDateRange(
                DateUtils.getStartDateOfMonth(currentDate.substring(0, 4).toInt(), currentDate.substring(5, 7).toInt()),
                currentDate
            )

            // 加载日记统计
            val consecutiveDays = diaryRepository.getConsecutiveDiaryDays()
            val monthCount = diaryRepository.getDiaryCountByDateRange(
                DateUtils.getStartDateOfMonth(currentDate.substring(0, 4).toInt(), currentDate.substring(5, 7).toInt()),
                currentDate
            )
            val todayDiary = diaryRepository.getDiaryByDate(currentDate)
            val todayMoodText = todayDiary?.let { Mood.fromValue(it.mood).icon + " " + Mood.fromValue(it.mood).text } ?: "未记录"

            // 加载午餐统计
            val todayLottery = lunchRepository.getTodayLottery()
            val totalDishes = lunchRepository.getAllDishesSync().size

            // 更新模块统计文本
            val updatedModules = allModules.map { module ->
                when (module.id) {
                    "painting" -> module.copy(statsText = "今日练习: ${DateUtils.formatDuration(todayDuration)}\n本月作品: ${monthWorkCount}幅")
                    "diary" -> module.copy(statsText = "连续写日记: ${consecutiveDays}天\n本月日记: ${monthCount}篇\n今日心情: $todayMoodText")
                    "lunch" -> module.copy(statsText = "今日推荐: ${todayLottery?.dishName ?: "待抽选"}\n菜单数量: ${totalDishes}道")
                    else -> module
                }
            }

            // 更新可见模块的统计文本
            val hiddenIds = prefs.getStringSet("hidden_modules", emptySet()) ?: emptySet()
            val visible = updatedModules.filter { it.id !in hiddenIds }
            _visibleModules.postValue(visible)
        }
    }
}
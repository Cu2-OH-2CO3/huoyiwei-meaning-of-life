package com.memoria.meaningoflife.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memoria.meaningoflife.MeaningOfLifeApp
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.data.repository.*
import com.memoria.meaningoflife.model.Mood
import com.memoria.meaningoflife.utils.DateUtils
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val paintingRepository = PaintingRepository(MeaningOfLifeApp.instance.database)
    private val diaryRepository = DiaryRepository(MeaningOfLifeApp.instance.database)
    private val lunchRepository = LunchRepository(MeaningOfLifeApp.instance.database)
    private val taskRepository = TaskRepository(MeaningOfLifeApp.instance.database)

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
        ),
        HomeModule(
            id = "task",
            title = "待办任务",
            iconRes = R.drawable.ic_task,
            statsText = "加载中...",
            color = android.graphics.Color.parseColor("#9C27B0"),
            badgeCount = 0
        ),
        // 新增时间轴模块
        HomeModule(
            id = "timeline",
            title = "时间轴",
            iconRes = R.drawable.ic_timeline_module,
            statsText = "加载中...",
            color = android.graphics.Color.parseColor("#7C3AED"),
            badgeCount = 0
        )
    )


    private val _visibleModules = MutableLiveData<List<HomeModule>>()
    val visibleModules: LiveData<List<HomeModule>> = _visibleModules
    private var latestUrgentTaskCount: Int = 0

    init {
        loadStats()
        loadVisibleModules()
        observeTaskCount()
    }

    private fun observeTaskCount() {
        viewModelScope.launch {
            // 先补齐临近截止任务的紧急标记，避免首页第一次渲染读到旧计数。
            taskRepository.markTasksAsUrgent()
            taskRepository.getUrgentImportantCount().collect { count ->
                updateTaskModuleStats(count)
            }
        }
    }

    private fun updateTaskModuleStats(urgentCount: Int) {
        latestUrgentTaskCount = urgentCount
        val currentModules = _visibleModules.value ?: filterVisibleModules(allModules)
        val statsText = if (urgentCount > 0) {
            "你有 $urgentCount 项未完成的紧急任务"
        } else {
            "暂无紧急任务"
        }

        val updatedModules = currentModules.map { module ->
            if (module.id == "task") {
                module.copy(
                    badgeCount = urgentCount,
                    statsText = statsText
                )
            } else {
                module
            }
        }
        _visibleModules.postValue(updatedModules)
    }

    // 改为 public，供外部调用
    fun loadVisibleModules() {
        val baseModules = _visibleModules.value ?: allModules
        val visible = filterVisibleModules(baseModules).map { module ->
            if (module.id == "task") {
                module.copy(
                    badgeCount = latestUrgentTaskCount,
                    statsText = if (latestUrgentTaskCount > 0) {
                        "你有 $latestUrgentTaskCount 项未完成的紧急任务"
                    } else {
                        "暂无紧急任务"
                    }
                )
            } else {
                module
            }
        }
        _visibleModules.value = visible
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

    // 改为 public，供外部调用
    fun loadStats() {
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

            // 获取待办统计
            val urgentCount = try {
                taskRepository.markTasksAsUrgent()
                taskRepository.getUrgentImportantCountSync()
            } catch (e: Exception) {
                0
            }

            // 获取时间轴统计（今日事件数）
            val todayEventCount = try {
                // 这里可以调用时间轴仓库获取今日事件数
                // 暂时先设为0，等时间轴仓库实现后可以获取真实数据
                0
            } catch (e: Exception) {
                0
            }

            // 更新所有模块的统计文本
            latestUrgentTaskCount = urgentCount
            val updatedModules = allModules.map { module ->
                when (module.id) {
                    "painting" -> module.copy(statsText = "今日练习: ${DateUtils.formatDuration(todayDuration)}\n本月作品: ${monthWorkCount}幅")
                    "diary" -> module.copy(statsText = "连续写日记: ${consecutiveDays}天\n本月日记: ${monthCount}篇\n今日心情: $todayMoodText")
                    "lunch" -> module.copy(statsText = "今日推荐: ${todayLottery?.dishName ?: "待抽选"}\n菜单数量: ${totalDishes}道")
                    "task" -> module.copy(
                        statsText = if (urgentCount > 0) "你有 $urgentCount 项未完成的紧急任务" else "暂无紧急任务",
                        badgeCount = urgentCount
                    )
                    "timeline" -> module.copy(
                        statsText = if (todayEventCount > 0) "今日 ${todayEventCount} 个事件" else "暂无事件",
                        badgeCount = 0
                    )
                    else -> module
                }
            }

            // 更新可见模块
            val visible = filterVisibleModules(updatedModules)
            _visibleModules.postValue(visible)
        }
    }

    private fun filterVisibleModules(modules: List<HomeModule>): List<HomeModule> {
        val hiddenIds = prefs.getStringSet("hidden_modules", emptySet()) ?: emptySet()
        return modules.filter { it.id !in hiddenIds }
    }

    // 刷新模块数据
    fun refreshModules() {
        loadStats()
    }
}
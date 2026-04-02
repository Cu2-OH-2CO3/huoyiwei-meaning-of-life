package com.memoria.meaningoflife.ui.calendar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memoria.meaningoflife.MeaningOfLifeApp
import com.memoria.meaningoflife.data.database.diary.DiaryEntity
import com.memoria.meaningoflife.data.database.painting.WorkEntity
import com.memoria.meaningoflife.data.database.task.TaskEntity
import com.memoria.meaningoflife.data.repository.DiaryRepository
import com.memoria.meaningoflife.data.repository.PaintingRepository
import com.memoria.meaningoflife.data.repository.TaskRepository
import com.memoria.meaningoflife.model.Mood
import com.memoria.meaningoflife.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CalendarViewModel : ViewModel() {

    private val paintingRepository = PaintingRepository(MeaningOfLifeApp.instance.database)
    private val diaryRepository = DiaryRepository(MeaningOfLifeApp.instance.database)
    private val taskRepository = TaskRepository(MeaningOfLifeApp.instance.database)

    private val _selectedDayData = MutableLiveData<DayDetailData?>()
    val selectedDayData: LiveData<DayDetailData?> = _selectedDayData

    private val _markedDatesData = MutableLiveData<MutableMap<String, CustomCalendarView.MarkData>>()
    val markedDatesData: LiveData<MutableMap<String, CustomCalendarView.MarkData>> = _markedDatesData

    private val _diaryMoodsData = MutableLiveData<MutableMap<String, Mood>>()
    val diaryMoodsData: LiveData<MutableMap<String, Mood>> = _diaryMoodsData

    // 抽屉内容
    private val _drawerTasks = MutableLiveData<List<TaskEntity>>()
    val drawerTasks: LiveData<List<TaskEntity>> = _drawerTasks

    private val _drawerDiaries = MutableLiveData<List<DiaryEntity>>()
    val drawerDiaries: LiveData<List<DiaryEntity>> = _drawerDiaries

    private val _drawerPaintings = MutableLiveData<List<WorkEntity>>()
    val drawerPaintings: LiveData<List<WorkEntity>> = _drawerPaintings

    fun loadDayDetail(date: String, mode: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val paintings = paintingRepository.getWorksByDateRange(date, date)
                val diary = diaryRepository.getDiaryByDate(date)

                withContext(Dispatchers.Main) {
                    _selectedDayData.postValue(
                        DayDetailData(
                            date = date,
                            hasPainting = paintings.isNotEmpty(),
                            hasDiary = diary != null,
                            paintingCount = paintings.size,
                            diaryMood = diary?.mood?.let { Mood.fromValue(it) },
                            diaryContent = diary?.content?.take(100)
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _selectedDayData.postValue(null)
                }
            }
        }
    }

    fun loadDrawerContent(date: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val parsedDate = DateUtils.parseDate(date)
                if (parsedDate != null) {
                    val calendar = java.util.Calendar.getInstance()
                    calendar.time = parsedDate
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    calendar.set(java.util.Calendar.MINUTE, 0)
                    calendar.set(java.util.Calendar.SECOND, 0)
                    calendar.set(java.util.Calendar.MILLISECOND, 0)
                    val startOfDay = calendar.timeInMillis
                    val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1

                    // 获取已完成的待办任务（completedAt 不为 null 且在当天完成）
                    val allTasks = taskRepository.getAllTasksSync()
                    val completedTasks = allTasks.filter {
                        it.completedAt != null && it.completedAt!! in startOfDay..endOfDay
                    }

                    // 获取日记
                    val diary = diaryRepository.getDiaryByDate(date)

                    // 获取绘画记录
                    val paintings = paintingRepository.getWorksByDateRange(date, date)

                    withContext(Dispatchers.Main) {
                        _drawerTasks.postValue(completedTasks)
                        _drawerDiaries.postValue(diary?.let { listOf(it) } ?: emptyList())
                        _drawerPaintings.postValue(paintings)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _drawerTasks.postValue(emptyList())
                        _drawerDiaries.postValue(emptyList())
                        _drawerPaintings.postValue(emptyList())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _drawerTasks.postValue(emptyList())
                    _drawerDiaries.postValue(emptyList())
                    _drawerPaintings.postValue(emptyList())
                }
            }
        }
    }

    fun loadMarkedDates(startDate: String, endDate: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val paintings = paintingRepository.getWorksByDateRange(startDate, endDate)
                val diaries = diaryRepository.getDiariesByDateRange(startDate, endDate)

                val paintingMap = paintings.groupBy { it.createdDate }
                val diaryMap = diaries.groupBy { it.createdDate }

                val result = mutableMapOf<String, CustomCalendarView.MarkData>()

                val start = DateUtils.parseDate(startDate) ?: return@launch
                val end = DateUtils.parseDate(endDate) ?: return@launch
                val calendar = java.util.Calendar.getInstance()
                calendar.time = start

                while (calendar.time <= end) {
                    val year = calendar.get(java.util.Calendar.YEAR)
                    val month = calendar.get(java.util.Calendar.MONTH) + 1
                    val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
                    val dateStr = "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

                    result[dateStr] = CustomCalendarView.MarkData(
                        hasPainting = paintingMap.containsKey(dateStr),
                        hasDiary = diaryMap.containsKey(dateStr)
                    )

                    calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
                }

                withContext(Dispatchers.Main) {
                    _markedDatesData.postValue(result)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadDiaryMoods(startDate: String, endDate: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val diaries = diaryRepository.getDiariesByDateRange(startDate, endDate)
                val result = mutableMapOf<String, Mood>()

                diaries.forEach { diary ->
                    result[diary.createdDate] = Mood.fromValue(diary.mood)
                }

                withContext(Dispatchers.Main) {
                    _diaryMoodsData.postValue(result)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
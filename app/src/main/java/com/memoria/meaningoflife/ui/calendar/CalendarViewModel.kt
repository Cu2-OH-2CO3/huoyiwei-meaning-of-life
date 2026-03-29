package com.memoria.meaningoflife.ui.calendar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memoria.meaningoflife.MeaningOfLifeApp
import com.memoria.meaningoflife.data.repository.DiaryRepository
import com.memoria.meaningoflife.data.repository.PaintingRepository
import com.memoria.meaningoflife.model.Mood
import com.memoria.meaningoflife.utils.DateUtils
import kotlinx.coroutines.launch

class CalendarViewModel : ViewModel() {

    private val paintingRepository = PaintingRepository(MeaningOfLifeApp.instance.database)
    private val diaryRepository = DiaryRepository(MeaningOfLifeApp.instance.database)

    private val _selectedDayData = MutableLiveData<DayDetailData?>()
    val selectedDayData: LiveData<DayDetailData?> = _selectedDayData

    private val _markedDatesData = MutableLiveData<MutableMap<String, CustomCalendarView.MarkData>>()
    val markedDatesData: LiveData<MutableMap<String, CustomCalendarView.MarkData>> = _markedDatesData

    private val _diaryMoodsData = MutableLiveData<MutableMap<String, Mood>>()
    val diaryMoodsData: LiveData<MutableMap<String, Mood>> = _diaryMoodsData

    fun loadDayDetail(date: String, mode: Int) {
        viewModelScope.launch {
            try {
                val paintings = paintingRepository.getWorksByDateRange(date, date)
                val diary = diaryRepository.getDiaryByDate(date)

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
            } catch (e: Exception) {
                e.printStackTrace()
                _selectedDayData.postValue(null)
            }
        }
    }

    fun loadMarkedDates(startDate: String, endDate: String) {
        viewModelScope.launch {
            try {
                val paintings = paintingRepository.getWorksByDateRange(startDate, endDate)
                val diaries = diaryRepository.getDiariesByDateRange(startDate, endDate)

                val paintingMap = paintings.groupBy { it.createdDate }
                val diaryMap = diaries.groupBy { it.createdDate }

                val result = mutableMapOf<String, CustomCalendarView.MarkData>()

                // 遍历日期范围
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

                _markedDatesData.postValue(result)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadDiaryMoods(startDate: String, endDate: String) {
        viewModelScope.launch {
            try {
                val diaries = diaryRepository.getDiariesByDateRange(startDate, endDate)
                val result = mutableMapOf<String, Mood>()

                diaries.forEach { diary ->
                    result[diary.createdDate] = Mood.fromValue(diary.mood)
                }

                _diaryMoodsData.postValue(result)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
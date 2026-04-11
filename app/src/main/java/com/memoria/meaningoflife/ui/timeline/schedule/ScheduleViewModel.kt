// 路径：com/memoria/meaningoflife/ui/timeline/schedule/ScheduleViewModel.kt
package com.memoria.meaningoflife.ui.timeline.schedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.memoria.meaningoflife.data.database.timeline.CourseEntity
import com.memoria.meaningoflife.data.database.timeline.SemesterEntity
import com.memoria.meaningoflife.data.repository.TimelineRepository
import kotlinx.coroutines.launch

class ScheduleViewModel(private val repository: TimelineRepository) : ViewModel() {

    // 使用 MutableLiveData 而不是直接从 repository 获取
    private val _allCourses = MutableLiveData<List<CourseEntity>>()
    val allCourses: LiveData<List<CourseEntity>> = _allCourses

    private val _currentSemester = MutableLiveData<SemesterEntity?>()
    val currentSemester: LiveData<SemesterEntity?> = _currentSemester

    init {
        loadCurrentSemester()
        loadAllCourses()
    }

    fun loadCurrentSemester() {
        viewModelScope.launch {
            val semester = repository.getCurrentSemester()
            _currentSemester.postValue(semester)
        }
    }

    fun loadAllCourses() {
        viewModelScope.launch {
            val courses = repository.getAllCourses()
            _allCourses.postValue(courses)
        }
    }

    fun getCoursesForWeek(weekNumber: Int): LiveData<List<CourseEntity>> {
        val result = MutableLiveData<List<CourseEntity>>()
        viewModelScope.launch {
            val semester = repository.getCurrentSemester()
            if (semester != null) {
                val courses = repository.getCoursesBySemester(semester.id)
                // 根据周次过滤课程
                val filteredCourses = courses.filter { course ->
                    shouldShowCourse(course, weekNumber)
                }
                result.postValue(filteredCourses)
            } else {
                result.postValue(emptyList())
            }
        }
        return result
    }

    private fun shouldShowCourse(course: CourseEntity, week: Int): Boolean {
        return CourseWeekPattern.shouldShowInWeek(course, week)
    }

    fun deleteCourse(course: CourseEntity) {
        viewModelScope.launch {
            repository.deleteCourse(course.id)
            loadAllCourses() // 重新加载
        }
    }

    suspend fun getCourseById(courseId: Long): CourseEntity? {
        return repository.getCourseById(courseId)
    }
}

class ScheduleViewModelFactory(private val repository: TimelineRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScheduleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScheduleViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
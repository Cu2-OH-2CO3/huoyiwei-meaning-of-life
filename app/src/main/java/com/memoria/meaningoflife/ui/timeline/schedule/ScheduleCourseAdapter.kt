// 路径：ui/timeline/schedule/ScheduleCourseAdapter.kt
package com.memoria.meaningoflife.ui.timeline.schedule

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.data.database.timeline.CourseEntity
import com.memoria.meaningoflife.databinding.ItemCourseBinding

class ScheduleCourseAdapter(
    private val onItemClick: (CourseEntity) -> Unit
) : RecyclerView.Adapter<ScheduleCourseAdapter.CourseViewHolder>() {

    private var courses: List<CourseEntity> = emptyList()

    fun submitList(newList: List<CourseEntity>) {
        courses = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val binding = ItemCourseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CourseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        holder.bind(courses[position])
    }

    override fun getItemCount(): Int = courses.size

    inner class CourseViewHolder(
        private val binding: ItemCourseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(course: CourseEntity) {
            binding.tvCourseName.text = course.name
            binding.tvTeacher.text = course.teacher ?: ""
            binding.tvLocation.text = course.location ?: ""

            val startHour = course.startTime / 3600000
            val startMinute = (course.startTime % 3600000) / 60000
            val endHour = course.endTime / 3600000
            val endMinute = (course.endTime % 3600000) / 60000
            binding.tvTime.text = String.format("%02d:%02d - %02d:%02d", startHour, startMinute, endHour, endMinute)

            binding.root.setOnClickListener { onItemClick(course) }
        }
    }
}
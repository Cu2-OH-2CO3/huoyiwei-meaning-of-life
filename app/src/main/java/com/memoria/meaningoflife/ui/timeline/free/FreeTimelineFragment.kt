// 路径：ui/timeline/free/FreeTimelineFragment.kt
package com.memoria.meaningoflife.ui.timeline.free

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.data.database.timeline.CourseEntity
import com.memoria.meaningoflife.data.database.timeline.SourceMode
import com.memoria.meaningoflife.data.database.timeline.TimelineEventEntity
import com.memoria.meaningoflife.data.repository.TimelineRepository
import com.memoria.meaningoflife.databinding.FragmentFreeTimelineBinding
import com.memoria.meaningoflife.MeaningOfLifeApp
import com.memoria.meaningoflife.ui.timeline.schedule.CourseEditActivity
import com.memoria.meaningoflife.ui.timeline.schedule.CourseWeekPattern
import com.memoria.meaningoflife.utils.ThemeManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FreeTimelineFragment : Fragment() {

    private var _binding: FragmentFreeTimelineBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: FreeTimelineViewModel
    private lateinit var repository: TimelineRepository
    private var cachedEvents: List<TimelineEventEntity> = emptyList()
    private var cachedCourses: List<CourseEntity> = emptyList()
    private var lastDragUndoSnackbar: Snackbar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFreeTimelineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = TimelineRepository(MeaningOfLifeApp.instance.database)
        viewModel = ViewModelProvider(this, FreeTimelineViewModelFactory(repository))[FreeTimelineViewModel::class.java]

        setupDayTimelineView()
        setupFab()
        observeData()
        setupDateNavigator()
        loadCoursesForCurrentDate()
    }

    private fun setupDayTimelineView() {
        binding.dayTimelineView.setOnCourseClickListener { course ->
            if (!isResumed) return@setOnCourseClickListener
            startActivity(Intent(requireContext(), CourseEditActivity::class.java).apply {
                putExtra("course_id", course.id)
            })
        }
        binding.dayTimelineView.setOnEventClickListener { event ->
            if (!isResumed) return@setOnEventClickListener
            startActivity(Intent(requireContext(), EventCreateActivity::class.java).apply {
                putExtra("event_id", event.id)
            })
        }
        binding.dayTimelineView.setOnEventDragChangedListener { event, newStart, newEnd ->
            val oldStart = event.startTime
            val oldEnd = event.endTime
            if (oldStart == newStart && oldEnd == newEnd) return@setOnEventDragChangedListener

            viewLifecycleOwner.lifecycleScope.launch {
                repository.updateEvent(
                    event.copy(
                        startTime = newStart,
                        endTime = newEnd,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                viewModel.loadEventsForDate(viewModel.currentDateMillis)
                showDragUndoSnackbar(event, oldStart, oldEnd)
            }
        }
    }

    private fun showDragUndoSnackbar(event: TimelineEventEntity, oldStart: Long, oldEnd: Long?) {
        lastDragUndoSnackbar?.dismiss()
        lastDragUndoSnackbar = Snackbar.make(binding.root, "已调整时间块", Snackbar.LENGTH_LONG)
            .setAction("撤销") {
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.updateEvent(
                        event.copy(
                            startTime = oldStart,
                            endTime = oldEnd,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    viewModel.loadEventsForDate(viewModel.currentDateMillis)
                }
            }
        lastDragUndoSnackbar?.show()
    }

    private fun setupFab() {
        ThemeManager.tintFab(binding.fabAddEvent)
        binding.fabAddEvent.setOnClickListener {
            startActivity(Intent(requireContext(), EventCreateActivity::class.java))
        }
    }

    private fun setupDateNavigator() {
        binding.btnPrevDay.setOnClickListener {
            viewModel.previousDay()
            updateDateDisplay()
            loadEvents()
            loadCoursesForCurrentDate()
        }

        binding.btnNextDay.setOnClickListener {
            viewModel.nextDay()
            updateDateDisplay()
            loadEvents()
            loadCoursesForCurrentDate()
        }

        binding.btnToday.setOnClickListener {
            viewModel.goToToday()
            updateDateDisplay()
            loadEvents()
            loadCoursesForCurrentDate()
        }

        updateDateDisplay()
    }

    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINA)
        binding.tvCurrentDate.text = dateFormat.format(Date(viewModel.currentDateMillis))
    }

    private fun loadEvents() {
        viewModel.loadEventsForDate(viewModel.currentDateMillis)
    }

    private fun observeData() {
        viewModel.currentEvents.observe(viewLifecycleOwner) { events ->
            cachedEvents = events.filter { it.sourceMode == SourceMode.FREE }
            renderTimeline()
        }
    }

    override fun onResume() {
        super.onResume()
        // 从事件编辑页返回后立即刷新，保证时间块变更实时反映到时间轴。
        loadEvents()
        loadCoursesForCurrentDate()
    }

    private fun loadCoursesForCurrentDate() {
        viewLifecycleOwner.lifecycleScope.launch {
            val semester = repository.getCurrentSemester()
            cachedCourses = if (semester == null) {
                emptyList()
            } else {
                val dayCalendar = Calendar.getInstance().apply { timeInMillis = viewModel.currentDateMillis }
                val weekDay = when (dayCalendar.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> 1
                    Calendar.TUESDAY -> 2
                    Calendar.WEDNESDAY -> 3
                    Calendar.THURSDAY -> 4
                    Calendar.FRIDAY -> 5
                    Calendar.SATURDAY -> 6
                    else -> 7
                }
                val week = (((viewModel.currentDateMillis - semester.startDate) / (7L * 24 * 3600_000L)) + 1).toInt()
                repository.getCoursesBySemester(semester.id)
                    .filter { it.weekDay == weekDay && CourseWeekPattern.shouldShowInWeek(it, week) }
                    .sortedBy { it.startTime }
            }
            renderTimeline()
        }
    }

    private fun renderTimeline() {
        if (!isAdded) return
        binding.dayTimelineView.setData(cachedCourses, cachedEvents.sortedBy { it.startTime })
        val isEmpty = cachedCourses.isEmpty() && cachedEvents.isEmpty()
        binding.tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.dayTimelineScroll.visibility = View.VISIBLE
    }

    private fun showEventDetailDialog(event: TimelineEventEntity) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_event_detail, null)

        val tvTitle = dialogView.findViewById<android.widget.TextView>(R.id.tv_event_title)
        val tvType = dialogView.findViewById<android.widget.TextView>(R.id.tv_event_type)
        val tvTime = dialogView.findViewById<android.widget.TextView>(R.id.tv_event_time)
        val tvLocation = dialogView.findViewById<android.widget.TextView>(R.id.tv_event_location)
        val tvDescription = dialogView.findViewById<android.widget.TextView>(R.id.tv_event_description)
        val btnEdit = dialogView.findViewById<android.widget.Button>(R.id.btn_edit)
        val btnDelete = dialogView.findViewById<android.widget.Button>(R.id.btn_delete)

        tvTitle.text = event.title
        tvType.text = event.eventType.name
        tvTime.text = formatEventTime(event)
        tvLocation.text = event.location ?: "无"
        tvDescription.text = event.description ?: "无"

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("关闭", null)
            .create()

        btnEdit.setOnClickListener {
            val intent = Intent(requireContext(), EventCreateActivity::class.java)
            intent.putExtra("event_id", event.id)
            startActivity(intent)
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {
            val deleteDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("删除事件")
                .setMessage("确定要删除「${event.title}」吗？")
                .setPositiveButton("删除") { _, _ ->
                    viewModel.deleteEvent(event)
                    Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .setNegativeButton("取消", null)
                .show()
            tintDialogButtons(deleteDialog)
        }

        dialog.show()
        tintDialogButtons(dialog)
    }

    private fun showEventOptionsDialog(event: TimelineEventEntity) {
        val options = listOf("编辑", "删除")

        val optionsDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(event.title)
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(requireContext(), EventCreateActivity::class.java)
                        intent.putExtra("event_id", event.id)
                        startActivity(intent)
                    }
                    1 -> {
                        val deleteDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("删除事件")
                            .setMessage("确定要删除「${event.title}」吗？")
                            .setPositiveButton("删除") { _, _ ->
                                viewModel.deleteEvent(event)
                                Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("取消", null)
                            .show()
                        tintDialogButtons(deleteDialog)
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
        tintDialogButtons(optionsDialog)
    }

    private fun tintDialogButtons(dialog: androidx.appcompat.app.AlertDialog) {
        val primary = ThemeManager.resolvePrimaryColor(requireContext())
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(primary)
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(primary)
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL)?.setTextColor(primary)
    }

    private fun formatEventTime(event: TimelineEventEntity): String {
        val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        val startStr = dateFormat.format(Date(event.startTime))

        return if (event.endTime != null) {
            val endFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val endStr = endFormat.format(Date(event.endTime))
            "$startStr - $endStr"
        } else {
            startStr
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
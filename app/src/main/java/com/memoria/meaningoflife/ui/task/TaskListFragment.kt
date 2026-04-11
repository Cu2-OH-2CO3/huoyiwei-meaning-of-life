package com.memoria.meaningoflife.ui.task

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.data.database.task.TaskEntity
import com.memoria.meaningoflife.data.database.task.TaskPriority
import com.memoria.meaningoflife.databinding.FragmentTaskListBinding
import com.memoria.meaningoflife.ui.timeline.free.EventCreateActivity
import com.memoria.meaningoflife.utils.DateUtils
import com.memoria.meaningoflife.utils.ThemeManager
import java.util.Calendar

class TaskListFragment : Fragment() {

    private var _binding: FragmentTaskListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: TaskViewModel
    private lateinit var taskAdapter: TaskAdapter
    private var pageFab: FloatingActionButton? = null
    private var customTypeface: Typeface? = null
    private var currentFilter = FilterType.ALL
    private var latestTasks: List<TaskEntity> = emptyList()

    enum class FilterType {
        ALL, ACTIVE, COMPLETED, URGENT_IMPORTANT
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTaskListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 加载字体
        try {
            customTypeface = ResourcesCompat.getFont(requireContext(), R.font.lxgw)
        } catch (_: Exception) {
            customTypeface = null
        }

        // 设置标题字体
        binding.tvTitle.typeface = customTypeface
        binding.toolbarTask.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.nd_surface))
        binding.tvTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.nd_text_display))

        viewModel = ViewModelProvider(this)[TaskViewModel::class.java]

        setupRecyclerView()
        setupFilterChips()
        setupFab()
        observeData()
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            onTaskClick = { task ->
                showTaskDetailDialog(task)
            },
            onTaskLongClick = { task ->
                showTaskOptionsDialog(task)
            },
            onCheckboxClick = { task ->
                if (task.completedAt == null) {
                    viewModel.completeTask(task)
                }
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskAdapter
        }
    }

    private fun setupFilterChips() {
        val borderVisible = ContextCompat.getColor(requireContext(), R.color.nd_border_visible)
        val textSecondary = ContextCompat.getColor(requireContext(), R.color.nd_text_secondary)
        val textDisplay = ContextCompat.getColor(requireContext(), R.color.nd_text_display)
        val bgSurface = ContextCompat.getColor(requireContext(), R.color.nd_surface)

        val strokeStates = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(textDisplay, borderVisible)
        )
        val textStates = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(bgSurface, textSecondary)
        )
        val bgStates = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(textDisplay, Color.TRANSPARENT)
        )

        // 创建筛选 Chip
        val chipAll = Chip(requireContext()).apply {
            text = "全部"
            isCheckable = true
            tag = FilterType.ALL
            typeface = Typeface.MONOSPACE
        }
        val chipActive = Chip(requireContext()).apply {
            text = "未完成"
            isCheckable = true
            tag = FilterType.ACTIVE
            typeface = Typeface.MONOSPACE
        }
        val chipCompleted = Chip(requireContext()).apply {
            text = "已完成"
            isCheckable = true
            tag = FilterType.COMPLETED
            typeface = Typeface.MONOSPACE
        }
        val chipUrgent = Chip(requireContext()).apply {
            text = "紧急重要"
            isCheckable = true
            tag = FilterType.URGENT_IMPORTANT
            typeface = Typeface.MONOSPACE
        }

        val chips = listOf(chipAll, chipActive, chipCompleted, chipUrgent)
        chips.forEach { chip ->
            chip.textSize = 11f
            chip.isAllCaps = true
            chip.chipStrokeWidth = 1f
            chip.chipStrokeColor = strokeStates
            chip.chipBackgroundColor = bgStates
            chip.setTextColor(textStates)
            chip.rippleColor = ColorStateList.valueOf(Color.TRANSPARENT)
            chip.setEnsureMinTouchTargetSize(false)
        }

        binding.chipGroup.addView(chipAll)
        binding.chipGroup.addView(chipActive)
        binding.chipGroup.addView(chipCompleted)
        binding.chipGroup.addView(chipUrgent)
        chipAll.isChecked = true

        @Suppress("DEPRECATION")
        binding.chipGroup.setOnCheckedChangeListener { _, checkedId ->
            val checkedChip = binding.chipGroup.findViewById<Chip>(checkedId)
            currentFilter = checkedChip?.tag as? FilterType ?: FilterType.ALL
            applyFilterAndRender()
        }
    }

    private fun setupFab() {
        pageFab?.let { (it.parent as? ViewGroup)?.removeView(it) }

        val fab = FloatingActionButton(requireContext()).apply {
            setImageResource(android.R.drawable.ic_input_add)
            ThemeManager.tintFab(this)
            setOnClickListener {
                val intent = android.content.Intent(requireContext(), TaskEditActivity::class.java)
                startActivity(intent)
            }
        }

        // 使用 FrameLayout.LayoutParams
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.BOTTOM or Gravity.END
        params.setMargins(0, 0, 32, 32)
        fab.layoutParams = params

        // 将 FAB 添加到根视图
        val rootView = requireView() as? ViewGroup
        rootView?.addView(fab)
        pageFab = fab
    }

    private fun showTaskDetailDialog(task: TaskEntity) {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_task_detail, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_title)
        val tvPriority = dialogView.findViewById<TextView>(R.id.tv_priority)
        val tvDeadline = dialogView.findViewById<TextView>(R.id.tv_deadline)
        val tvDescription = dialogView.findViewById<TextView>(R.id.tv_description)
        val btnEdit = dialogView.findViewById<View>(R.id.btn_edit)
        val btnAddToTimeline = dialogView.findViewById<View>(R.id.btn_add_to_timeline)
        val btnDelete = dialogView.findViewById<View>(R.id.btn_delete)

        tvTitle.text = task.title
        tvTitle.typeface = customTypeface

        val priority = task.getPriority()
        val priorityText = when (priority) {
            TaskPriority.URGENT_IMPORTANT -> "紧急重要"
            TaskPriority.URGENT_NOT_IMPORTANT -> "紧急不重要"
            TaskPriority.NOT_URGENT_IMPORTANT -> "不紧急重要"
            TaskPriority.NOT_URGENT_NOT_IMPORTANT -> "不紧急不重要"
        }
        tvPriority.text = "优先级：$priorityText"
        tvPriority.setTextColor(ContextCompat.getColor(requireContext(), getPriorityColor(priority)))

        tvDeadline.text = task.deadline?.let { "截止日期：${DateUtils.formatDateTime(it)}" } ?: "无截止日期"

        tvDescription.text = task.description ?: "无描述"

        val dialog = builder.setView(dialogView)
            .setNegativeButton("关闭", null)
            .create()

        btnEdit.setOnClickListener {
            val intent = android.content.Intent(requireContext(), TaskEditActivity::class.java)
            intent.putExtra("task_id", task.id)
            startActivity(intent)
            dialog.dismiss()
        }

        btnAddToTimeline.setOnClickListener {
            openCreateTimelineBlock(task)
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {
            val deleteDialog = AlertDialog.Builder(requireContext())
                .setTitle("删除任务")
                .setMessage("确定要删除这个任务吗？")
                .setPositiveButton("删除") { _, _ ->
                    viewModel.deleteTask(task)
                    dialog.dismiss()
                }
                .setNegativeButton("取消", null)
                .show()
            tintDialogButtons(deleteDialog)
        }

        dialog.show()
        tintDialogButtons(dialog)
    }

    private fun openCreateTimelineBlock(task: TaskEntity) {
        val now = System.currentTimeMillis()
        val startAt = task.deadline?.let { it - 60 * 60 * 1000L }?.coerceAtLeast(now) ?: now
        val endAt = startAt + 60 * 60 * 1000L

        val roundedStart = roundToFiveMinutes(startAt)
        val roundedEnd = roundToFiveMinutes(endAt)

        val intent = android.content.Intent(requireContext(), EventCreateActivity::class.java).apply {
            putExtra("prefill_title", task.title)
            putExtra("prefill_description", task.description)
            putExtra("prefill_event_type", "TASK")
            putExtra("prefill_start_at", roundedStart)
            putExtra("prefill_end_at", roundedEnd)
            putExtra("prefill_source_id", task.id)
            putExtra("prefill_source_table", "tasks")
        }
        startActivity(intent)
    }

    private fun roundToFiveMinutes(timeMillis: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val minute = calendar.get(Calendar.MINUTE)
        val roundedMinute = (minute / 5) * 5
        calendar.set(Calendar.MINUTE, roundedMinute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun showTaskOptionsDialog(task: TaskEntity) {
        val options = mutableListOf<String>()
        if (task.completedAt == null) {
            options.add("完成")
        }
        options.add("编辑")
        options.add("删除")

        val optionsDialog = AlertDialog.Builder(requireContext())
            .setTitle(task.title)
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "完成" -> viewModel.completeTask(task)
                    "编辑" -> {
                        val intent = android.content.Intent(requireContext(), TaskEditActivity::class.java)
                        intent.putExtra("task_id", task.id)
                        startActivity(intent)
                    }
                    "删除" -> {
                        val deleteDialog = AlertDialog.Builder(requireContext())
                            .setTitle("删除任务")
                            .setMessage("确定要删除这个任务吗？")
                            .setPositiveButton("删除") { _, _ ->
                                viewModel.deleteTask(task)
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

    private fun getPriorityColor(priority: TaskPriority): Int {
        return when (priority) {
            TaskPriority.URGENT_IMPORTANT -> R.color.nd_accent
            TaskPriority.URGENT_NOT_IMPORTANT -> R.color.nd_text_primary
            TaskPriority.NOT_URGENT_IMPORTANT -> R.color.nd_text_display
            TaskPriority.NOT_URGENT_NOT_IMPORTANT -> R.color.nd_text_secondary
        }
    }

    private fun tintDialogButtons(dialog: AlertDialog) {
        val primary = ThemeManager.resolvePrimaryColor(requireContext())
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(primary)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(primary)
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(primary)
    }

    private fun observeData() {
        viewModel.allTasks.observe(viewLifecycleOwner) { tasks ->
            latestTasks = tasks
            applyFilterAndRender()
        }
    }

    private fun applyFilterAndRender() {
        val filteredTasks = when (currentFilter) {
            FilterType.ACTIVE -> latestTasks.filter { it.completedAt == null }
            FilterType.COMPLETED -> latestTasks.filter { it.completedAt != null }
            FilterType.URGENT_IMPORTANT -> latestTasks.filter { it.isUrgent && it.isImportant && it.completedAt == null }
            else -> latestTasks
        }
        taskAdapter.submitList(filteredTasks)
    }

    override fun onDestroyView() {
        pageFab = null
        super.onDestroyView()
        _binding = null
    }
}
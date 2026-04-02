package com.memoria.meaningoflife.ui.task

import android.content.res.ColorStateList
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
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.data.database.task.TaskEntity
import com.memoria.meaningoflife.data.database.task.TaskPriority
import com.memoria.meaningoflife.databinding.FragmentTaskListBinding
import com.memoria.meaningoflife.utils.DateUtils

class TaskListFragment : Fragment() {

    private var _binding: FragmentTaskListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: TaskViewModel
    private lateinit var taskAdapter: TaskAdapter
    private var customTypeface: Typeface? = null
    private var currentFilter = FilterType.ALL

    enum class FilterType {
        ALL, ACTIVE, COMPLETED, URGENT_IMPORTANT
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentTaskListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 加载字体
        try {
            customTypeface = ResourcesCompat.getFont(requireContext(), R.font.lxgw)
        } catch (e: Exception) {
            customTypeface = null
        }

        // 设置标题字体
        binding.tvTitle.typeface = customTypeface

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
        // 创建筛选 Chip
        val chipAll = Chip(requireContext()).apply {
            text = "全部"
            isCheckable = true
            tag = FilterType.ALL
            customTypeface?.let { typeface = it }
        }
        val chipActive = Chip(requireContext()).apply {
            text = "未完成"
            isCheckable = true
            tag = FilterType.ACTIVE
            customTypeface?.let { typeface = it }
        }
        val chipCompleted = Chip(requireContext()).apply {
            text = "已完成"
            isCheckable = true
            tag = FilterType.COMPLETED
            customTypeface?.let { typeface = it }
        }
        val chipUrgent = Chip(requireContext()).apply {
            text = "紧急重要"
            isCheckable = true
            tag = FilterType.URGENT_IMPORTANT
            customTypeface?.let { typeface = it }
            setChipBackgroundColor(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.task_urgent_important)
            ))
        }

        binding.chipGroup.addView(chipAll)
        binding.chipGroup.addView(chipActive)
        binding.chipGroup.addView(chipCompleted)
        binding.chipGroup.addView(chipUrgent)
        chipAll.isChecked = true

        binding.chipGroup.setOnCheckedChangeListener { _, checkedId ->
            val checkedChip = binding.chipGroup.findViewById<Chip>(checkedId)
            currentFilter = checkedChip?.tag as? FilterType ?: FilterType.ALL
            taskAdapter.setFilter(currentFilter)
        }
    }

    private fun setupFab() {
        val fab = FloatingActionButton(requireContext()).apply {
            setImageResource(android.R.drawable.ic_input_add)
            setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.primary)
            ))
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

        btnEdit.setOnClickListener {
            val intent = android.content.Intent(requireContext(), TaskEditActivity::class.java)
            intent.putExtra("task_id", task.id)
            startActivity(intent)
            (dialogView.parent as? android.app.Dialog)?.dismiss()
        }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("删除任务")
                .setMessage("确定要删除这个任务吗？")
                .setPositiveButton("删除") { _, _ ->
                    viewModel.deleteTask(task)
                    (dialogView.parent as? android.app.Dialog)?.dismiss()
                }
                .setNegativeButton("取消", null)
                .show()
        }

        builder.setView(dialogView)
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun showTaskOptionsDialog(task: TaskEntity) {
        val options = mutableListOf<String>()
        if (task.completedAt == null) {
            options.add("完成")
        }
        options.add("编辑")
        options.add("删除")

        AlertDialog.Builder(requireContext())
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
                        AlertDialog.Builder(requireContext())
                            .setTitle("删除任务")
                            .setMessage("确定要删除这个任务吗？")
                            .setPositiveButton("删除") { _, _ ->
                                viewModel.deleteTask(task)
                            }
                            .setNegativeButton("取消", null)
                            .show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun getPriorityColor(priority: TaskPriority): Int {
        return when (priority) {
            TaskPriority.URGENT_IMPORTANT -> R.color.task_urgent_important
            TaskPriority.URGENT_NOT_IMPORTANT -> R.color.task_urgent_not_important
            TaskPriority.NOT_URGENT_IMPORTANT -> R.color.task_not_urgent_important
            TaskPriority.NOT_URGENT_NOT_IMPORTANT -> R.color.task_not_urgent_not_important
        }
    }

    private fun observeData() {
        viewModel.allTasks.observe(viewLifecycleOwner) { tasks ->
            val filteredTasks = when (currentFilter) {
                FilterType.ACTIVE -> tasks.filter { it.completedAt == null }
                FilterType.COMPLETED -> tasks.filter { it.completedAt != null }
                FilterType.URGENT_IMPORTANT -> tasks.filter { it.isUrgent && it.isImportant && it.completedAt == null }
                else -> tasks
            }
            taskAdapter.submitList(filteredTasks)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
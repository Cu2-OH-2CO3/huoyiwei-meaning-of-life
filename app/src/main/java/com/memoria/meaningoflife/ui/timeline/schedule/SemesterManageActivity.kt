// 路径：com/memoria/meaningoflife/ui/timeline/schedule/SemesterManageActivity.kt
package com.memoria.meaningoflife.ui.timeline.schedule

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.data.database.timeline.SemesterEntity
import com.memoria.meaningoflife.data.repository.TimelineRepository
import com.memoria.meaningoflife.MeaningOfLifeApp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SemesterManageActivity : AppCompatActivity() {

    private lateinit var repository: TimelineRepository
    private lateinit var rvSemesters: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var semesterAdapter: SemesterAdapter
    private var semesters = mutableListOf<SemesterEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_semester_manage)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "学期管理"

        repository = TimelineRepository(MeaningOfLifeApp.instance.database)

        setupViews()
        loadSemesters()
    }

    private fun setupViews() {
        rvSemesters = findViewById(R.id.rv_semesters)
        tvEmpty = findViewById(R.id.tv_empty)

        semesterAdapter = SemesterAdapter(
            onItemClick = { semester ->
                showSemesterDetailDialog(semester)
            },
            onSetCurrent = { semester ->
                setAsCurrentSemester(semester)
            },
            onDelete = { semester ->
                deleteSemester(semester)
            }
        )

        rvSemesters.layoutManager = LinearLayoutManager(this)
        rvSemesters.adapter = semesterAdapter

        findViewById<Button>(R.id.btn_add_semester).setOnClickListener {
            showAddSemesterDialog()
        }
    }

    private fun loadSemesters() {
        lifecycleScope.launch {
            val semesterList = repository.getAllSemesters()
            semesters = semesterList.toMutableList()
            semesterAdapter.submitList(semesters)
            tvEmpty.visibility = if (semesters.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showAddSemesterDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_semester_edit, null)

        val etName = dialogView.findViewById<EditText>(R.id.et_semester_name)
        val tvStartDate = dialogView.findViewById<TextView>(R.id.tv_start_date)
        val tvEndDate = dialogView.findViewById<TextView>(R.id.tv_end_date)
        val etTotalWeeks = dialogView.findViewById<EditText>(R.id.et_total_weeks)

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        tvStartDate.text = dateFormat.format(calendar.time)
        calendar.add(Calendar.MONTH, 4)
        tvEndDate.text = dateFormat.format(calendar.time)
        etTotalWeeks.setText("16")

        tvStartDate.setOnClickListener {
            showDatePicker { date ->
                tvStartDate.text = date
            }
        }

        tvEndDate.setOnClickListener {
            showDatePicker { date ->
                tvEndDate.text = date
            }
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.add_semester))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, getString(R.string.enter_semester_name), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val startDateStr = tvStartDate.text.toString()
                val endDateStr = tvEndDate.text.toString()
                val totalWeeks = etTotalWeeks.text.toString().toIntOrNull() ?: 16

                val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(startDateStr)?.time ?: return@setPositiveButton
                val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(endDateStr)?.time ?: return@setPositiveButton

                lifecycleScope.launch {
                    val semester = SemesterEntity(
                        name = name,
                        startDate = startDate,
                        endDate = endDate,
                        totalWeeks = totalWeeks
                    )
                    repository.insertSemester(semester)
                    Toast.makeText(this@SemesterManageActivity, getString(R.string.add_success), Toast.LENGTH_SHORT).show()
                    loadSemesters()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showSemesterDetailDialog(semester: SemesterEntity) {
        val startDateStr = formatDate(semester.startDate)
        val endDateStr = formatDate(semester.endDate)
        val message = buildString {
            append("${getString(R.string.start_date)}: $startDateStr\n")
            append("${getString(R.string.end_date)}: $endDateStr\n")
            append("${getString(R.string.total_weeks)}: ${semester.totalWeeks} ${getString(R.string.weeks)}\n")
            if (semester.isCurrent) append(getString(R.string.current_semester))
        }

        AlertDialog.Builder(this)
            .setTitle(semester.name)
            .setMessage(message)
            .setPositiveButton(getString(R.string.close), null)
            .setNeutralButton(getString(R.string.set_as_current)) { _, _ ->
                setAsCurrentSemester(semester)
            }
            .setNegativeButton(getString(R.string.delete)) { _, _ ->
                deleteSemester(semester)
            }
            .show()
    }

    private fun setAsCurrentSemester(semester: SemesterEntity) {
        lifecycleScope.launch {
            repository.setCurrentSemester(semester)
            Toast.makeText(this@SemesterManageActivity, getString(R.string.set_as_current_success), Toast.LENGTH_SHORT).show()
            loadSemesters()
        }
    }

    private fun deleteSemester(semester: SemesterEntity) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_semester))
            .setMessage(getString(R.string.confirm_delete_semester, semester.name))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                lifecycleScope.launch {
                    // TODO: 添加删除学期的方法
                    Toast.makeText(this@SemesterManageActivity, getString(R.string.deleted), Toast.LENGTH_SHORT).show()
                    loadSemesters()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day)
                onDateSelected(date)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    // SemesterAdapter 内部类
    inner class SemesterAdapter(
        private val onItemClick: (SemesterEntity) -> Unit,
        private val onSetCurrent: (SemesterEntity) -> Unit,
        private val onDelete: (SemesterEntity) -> Unit
    ) : RecyclerView.Adapter<SemesterAdapter.ViewHolder>() {

        private var items: List<SemesterEntity> = emptyList()

        fun submitList(newList: List<SemesterEntity>) {
            items = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(semester: SemesterEntity) {
                val text1 = itemView.findViewById<TextView>(android.R.id.text1)
                val text2 = itemView.findViewById<TextView>(android.R.id.text2)

                text1.text = semester.name
                val dateRange = "${formatDate(semester.startDate)} ~ ${formatDate(semester.endDate)}"
                text2.text = if (semester.isCurrent) {
                    "$dateRange (${getString(R.string.current)})"
                } else {
                    dateRange
                }

                itemView.setOnClickListener { onItemClick(semester) }
                itemView.setOnLongClickListener {
                    val options = mutableListOf<String>()
                    if (!semester.isCurrent) options.add(getString(R.string.set_as_current))
                    options.add(getString(R.string.delete))

                    AlertDialog.Builder(itemView.context)
                        .setTitle(semester.name)
                        .setItems(options.toTypedArray()) { _, which ->
                            when (options[which]) {
                                getString(R.string.set_as_current) -> onSetCurrent(semester)
                                getString(R.string.delete) -> onDelete(semester)
                            }
                        }
                        .show()
                    true
                }
            }
        }
    }
}
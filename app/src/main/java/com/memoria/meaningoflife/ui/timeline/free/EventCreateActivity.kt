// 路径：ui/timeline/free/EventCreateActivity.kt
package com.memoria.meaningoflife.ui.timeline.free

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.data.database.timeline.EventType
import com.memoria.meaningoflife.data.database.timeline.SourceMode
import com.memoria.meaningoflife.data.database.timeline.TimelineEventEntity
import com.memoria.meaningoflife.data.repository.TimelineRepository
import com.memoria.meaningoflife.MeaningOfLifeApp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EventCreateActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PREFILL_TITLE = "prefill_title"
        const val EXTRA_PREFILL_DESCRIPTION = "prefill_description"
        const val EXTRA_PREFILL_EVENT_TYPE = "prefill_event_type"
        const val EXTRA_PREFILL_START_AT = "prefill_start_at"
        const val EXTRA_PREFILL_END_AT = "prefill_end_at"
        const val EXTRA_PREFILL_SOURCE_ID = "prefill_source_id"
        const val EXTRA_PREFILL_SOURCE_TABLE = "prefill_source_table"
    }

    private lateinit var repository: TimelineRepository

    private lateinit var etTitle: EditText
    private lateinit var spinnerType: Spinner
    private lateinit var chkAllDay: CheckBox
    private lateinit var tvDate: TextView
    private lateinit var tvStartTime: TextView
    private lateinit var tvEndTime: TextView
    private lateinit var etLocation: EditText
    private lateinit var etDescription: EditText
    private lateinit var btnSave: Button

    private var eventId: Long = 0
    private var existingEvent: TimelineEventEntity? = null
    private var selectedDate: Long = 0L
    private var startTimeMillis: Long = 9 * 3600000L  // 默认 09:00
    private var endTimeMillis: Long = 10 * 3600000L   // 默认 10:00
    private var isAllDay = false
    private var prefillSourceId: Long? = null
    private var prefillSourceTable: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_create)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        repository = TimelineRepository(MeaningOfLifeApp.instance.database)
        eventId = intent.getLongExtra("event_id", 0)
        selectedDate = getStartOfDay(System.currentTimeMillis())

        setupViews()
        setupPickers()

        if (eventId != 0L) {
            supportActionBar?.title = "编辑事件"
            loadEvent()
        } else {
            supportActionBar?.title = "新建事件"
            applyPrefillFromIntent()
        }

        btnSave.setOnClickListener { saveEvent() }
    }

    private fun setupViews() {
        etTitle = findViewById(R.id.et_event_title)
        spinnerType = findViewById(R.id.spinner_event_type)
        chkAllDay = findViewById(R.id.chk_all_day)
        tvDate = findViewById(R.id.tv_date)
        tvStartTime = findViewById(R.id.tv_start_time)
        tvEndTime = findViewById(R.id.tv_end_time)
        etLocation = findViewById(R.id.et_event_location)
        etDescription = findViewById(R.id.et_event_description)
        btnSave = findViewById(R.id.btn_save)

        val eventTypes = arrayOf("自定义", "任务", "约会", "提醒", "其他")
        spinnerType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, eventTypes)

        chkAllDay.setOnCheckedChangeListener { _, isChecked ->
            isAllDay = isChecked
            tvStartTime.isEnabled = !isChecked
            tvEndTime.isEnabled = !isChecked
        }

        updateDateTimeDisplay()
    }

    private fun setupPickers() {
        tvDate.setOnClickListener { showDatePicker() }
        tvStartTime.setOnClickListener { showTimePicker(true) }
        tvEndTime.setOnClickListener { showTimePicker(false) }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                selectedDate = getStartOfDay(calendar.timeInMillis)
                updateDateTimeDisplay()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker(isStart: Boolean) {
        val currentTime = if (isStart) startTimeMillis else endTimeMillis
        val hour = (currentTime / 3600000).toInt()
        val minute = ((currentTime % 3600000) / 60000).toInt()

        TimePickerDialog(this, { _, hourOfDay, minuteOfHour ->
            if (isStart) {
                startTimeMillis = hourOfDay * 3600000L + minuteOfHour * 60000L
            } else {
                endTimeMillis = hourOfDay * 3600000L + minuteOfHour * 60000L
            }
            updateDateTimeDisplay()
        }, hour, minute, true).show()
    }

    // 修改 updateDateTimeDisplay 方法
    private fun updateDateTimeDisplay() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        tvDate.text = dateFormat.format(Date(selectedDate))

        tvStartTime.text = formatTime(startTimeMillis)
        tvEndTime.text = formatTime(endTimeMillis)
    }

    private fun formatTime(timeInMillis: Long): String {
        val hour = timeInMillis / 3600000
        val minute = (timeInMillis % 3600000) / 60000
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }

    // 修改 loadEvent 方法
    private fun loadEvent() {
        lifecycleScope.launch {
            existingEvent = repository.getEventById(eventId)  // 使用 repository
            existingEvent?.let { event ->
                etTitle.setText(event.title)
                val dayStart = getStartOfDay(event.startTime)
                selectedDate = dayStart
                startTimeMillis = (event.startTime - dayStart).coerceIn(0L, 24 * 3600000L - 1)
                val endAt = (event.endTime ?: event.startTime)
                endTimeMillis = (endAt - dayStart).coerceIn(startTimeMillis, 24 * 3600000L)
                isAllDay = event.isAllDay
                chkAllDay.isChecked = isAllDay
                etLocation.setText(event.location ?: "")
                etDescription.setText(event.description ?: "")
                updateDateTimeDisplay()
            }
        }
    }

    private fun saveEvent() {
        val title = etTitle.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(this, "请输入事件标题", Toast.LENGTH_SHORT).show()
            return
        }

        // 验证时间
        if (!isAllDay && startTimeMillis >= endTimeMillis) {
            Toast.makeText(this, "结束时间必须晚于开始时间", Toast.LENGTH_SHORT).show()
            return
        }

        val eventTypeValue = when (spinnerType.selectedItemPosition) {
            0 -> EventType.CUSTOM
            1 -> EventType.TASK
            2 -> EventType.APPOINTMENT
            else -> EventType.CUSTOM
        }

        val dayStart = getStartOfDay(selectedDate)

        val startTime = if (isAllDay) {
            dayStart
        } else {
            dayStart + startTimeMillis
        }

        val endTime = if (isAllDay) {
            getEndOfDay(dayStart)
        } else {
            dayStart + endTimeMillis
        }

        val event = if (existingEvent != null) {
            existingEvent!!.copy(
                title = title,
                description = etDescription.text.toString().takeIf { it.isNotEmpty() },
                eventType = eventTypeValue,
                startTime = startTime,
                endTime = endTime,
                isAllDay = isAllDay,
                location = etLocation.text.toString().takeIf { it.isNotEmpty() },
                sourceId = prefillSourceId ?: existingEvent!!.sourceId,
                sourceTable = prefillSourceTable ?: existingEvent!!.sourceTable,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            TimelineEventEntity(
                title = title,
                description = etDescription.text.toString().takeIf { it.isNotEmpty() },
                eventType = eventTypeValue,
                sourceMode = SourceMode.FREE,
                startTime = startTime,
                endTime = endTime,
                isAllDay = isAllDay,
                isCompleted = false,
                location = etLocation.text.toString().takeIf { it.isNotEmpty() },
                sourceId = prefillSourceId,
                sourceTable = prefillSourceTable,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }

        lifecycleScope.launch {
            try {
                if (existingEvent != null) {
                    repository.updateEvent(event)
                    Toast.makeText(this@EventCreateActivity, "更新成功", Toast.LENGTH_SHORT).show()
                } else {
                    repository.insertEvent(event)
                    Toast.makeText(this@EventCreateActivity, "添加成功", Toast.LENGTH_SHORT).show()
                }
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@EventCreateActivity, "保存失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getStartOfDay(dateMillis: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun getEndOfDay(dateMillis: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }

    private fun applyPrefillFromIntent() {
        val prefillTitle = intent.getStringExtra(EXTRA_PREFILL_TITLE)
        val prefillDescription = intent.getStringExtra(EXTRA_PREFILL_DESCRIPTION)
        val prefillType = intent.getStringExtra(EXTRA_PREFILL_EVENT_TYPE)
        val prefillStartAt = intent.getLongExtra(EXTRA_PREFILL_START_AT, -1L)
        val prefillEndAt = intent.getLongExtra(EXTRA_PREFILL_END_AT, -1L)
        val sourceId = intent.getLongExtra(EXTRA_PREFILL_SOURCE_ID, -1L)
        val sourceTable = intent.getStringExtra(EXTRA_PREFILL_SOURCE_TABLE)

        if (!prefillTitle.isNullOrBlank()) etTitle.setText(prefillTitle)
        if (!prefillDescription.isNullOrBlank()) etDescription.setText(prefillDescription)

        if (!sourceTable.isNullOrBlank()) {
            prefillSourceTable = sourceTable
        }
        if (sourceId > 0L) {
            prefillSourceId = sourceId
        }

        if (!prefillType.isNullOrBlank()) {
            val typeIndex = when (prefillType.uppercase(Locale.getDefault())) {
                "TASK" -> 1
                "APPOINTMENT" -> 2
                else -> 0
            }
            spinnerType.setSelection(typeIndex)
        }

        if (prefillStartAt > 0L) {
            selectedDate = getStartOfDay(prefillStartAt)
            startTimeMillis = prefillStartAt - selectedDate
            endTimeMillis = if (prefillEndAt > prefillStartAt) {
                (prefillEndAt - selectedDate).coerceAtMost(24 * 3600000L - 1)
            } else {
                (startTimeMillis + 60 * 60 * 1000L).coerceAtMost(24 * 3600000L - 1)
            }
            updateDateTimeDisplay()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
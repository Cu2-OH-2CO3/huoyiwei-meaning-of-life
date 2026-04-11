// 路径：com/memoria/meaningoflife/ui/timeline/schedule/IcsImportActivity.kt
package com.memoria.meaningoflife.ui.timeline.schedule

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.data.database.timeline.CourseEntity
import com.memoria.meaningoflife.data.database.timeline.SemesterEntity
import com.memoria.meaningoflife.data.database.timeline.SourceMode
import com.memoria.meaningoflife.data.database.timeline.TimelineEventEntity
import com.memoria.meaningoflife.data.repository.TimelineRepository
import com.memoria.meaningoflife.MeaningOfLifeApp
import com.memoria.meaningoflife.utils.IcsParser
import com.memoria.meaningoflife.utils.LogManager
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class IcsImportActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "IcsImportActivity"
    }

    private lateinit var repository: TimelineRepository
    private lateinit var icsParser: IcsParser

    private lateinit var tvFileName: TextView
    private lateinit var rvPreview: RecyclerView
    private lateinit var etSemesterName: EditText
    private lateinit var etStartDate: EditText
    private lateinit var etTotalWeeks: EditText
    private lateinit var btnImport: Button

    private var selectedUri: Uri? = null
    private var previewCourses: List<CourseEntity> = emptyList()
    private var previewEvents: List<TimelineEventEntity> = emptyList()

    private data class DedupResult<T>(
        val kept: List<T>,
        val skippedCount: Int
    )

    private val selectFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedUri = uri
                tvFileName.text = getFileName(uri)
                LogManager.i(TAG, "选择ICS文件: uri=$uri, name=${tvFileName.text}")
                parseIcsFile(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ics_import)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "导入课程表"

        repository = TimelineRepository(MeaningOfLifeApp.instance.database)
        // 修复：传入 contentResolver
        icsParser = IcsParser(contentResolver)

        setupViews()
        setupListeners()
    }

    private fun setupViews() {
        tvFileName = findViewById(R.id.tv_file_name)
        rvPreview = findViewById(R.id.rv_preview)
        etSemesterName = findViewById(R.id.et_semester_name)
        etStartDate = findViewById(R.id.et_start_date)
        etTotalWeeks = findViewById(R.id.et_total_weeks)
        btnImport = findViewById(R.id.btn_import)

        rvPreview.layoutManager = LinearLayoutManager(this)

        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        etSemesterName.setText("${year}年${if (calendar.get(Calendar.MONTH) < 7) "春季" else "秋季"}学期")

        val startDate = getCurrentWeekStart()
        etStartDate.setText(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(startDate)))

        etTotalWeeks.setText("16")
    }

    private fun setupListeners() {
        findViewById<Button>(R.id.btn_select_file).setOnClickListener {
            showSelectFileOptions()
        }

        etStartDate.setOnClickListener {
            showDatePicker()
        }

        btnImport.setOnClickListener {
            importCourses()
        }
    }

    private fun showSelectFileOptions() {
        val options = arrayOf("系统文件选择", "扫描 Download 目录")
        AlertDialog.Builder(this)
            .setTitle("选择导入方式")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> selectFile()
                    1 -> pickFromLocalIcsFiles()
                }
            }
            .show()
    }

    private fun selectFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/calendar", "text/plain", "application/octet-stream"))
        }
        selectFileLauncher.launch(intent)
    }

    private fun pickFromLocalIcsFiles() {
        lifecycleScope.launch {
            val files = scanDownloadIcsFiles()
            if (files.isEmpty()) {
                Toast.makeText(this@IcsImportActivity, "未在 Download 目录找到 .ics 文件", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val names = files.map { it.name }.toTypedArray()
            AlertDialog.Builder(this@IcsImportActivity)
                .setTitle("选择本地 ICS 文件")
                .setItems(names) { _, which ->
                    val uri = Uri.fromFile(files[which])
                    selectedUri = uri
                    tvFileName.text = files[which].absolutePath
                    LogManager.i(TAG, "本地扫描选择ICS文件: ${files[which].absolutePath}")
                    parseIcsFile(uri)
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    private fun scanDownloadIcsFiles(): List<File> {
        return try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadDir.exists()) return emptyList()

            downloadDir.walkTopDown()
                .maxDepth(3)
                .filter { it.isFile && it.extension.equals("ics", ignoreCase = true) }
                .sortedByDescending { it.lastModified() }
                .toList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseIcsFile(uri: Uri) {
        lifecycleScope.launch {
            try {
                val result = icsParser.parseIcsFile(uri)
                previewCourses = result.courses
                previewEvents = result.events
                LogManager.i(
                    TAG,
                    "解析完成: uri=$uri, courses=${result.courses.size}, events=${result.events.size}, warnings=${result.warnings.size}"
                )

                btnImport.isEnabled = previewCourses.isNotEmpty() || previewEvents.isNotEmpty()

                showPreview(result)

                Toast.makeText(
                    this@IcsImportActivity,
                    "解析成功：发现 ${result.courses.size} 门课程，${result.events.size} 个事件",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                btnImport.isEnabled = false
                LogManager.e(TAG, "解析异常: uri=$uri, error=${e.message}")
                Toast.makeText(this@IcsImportActivity, "解析失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPreview(result: com.memoria.meaningoflife.utils.IcsParser.IcsParseResult) {
        val coursePreview = result.courses.map { course ->
            "${course.name} - ${getWeekDayName(course.weekDay)} ${formatTime(course.startTime)}-${formatTime(course.endTime)}"
        }
        val eventPreview = result.events.map { event ->
            "${event.title} - ${SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(event.startTime))}"
        }
        val previewList = coursePreview + eventPreview

        // 简化预览适配器
        val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val tv = TextView(parent.context).apply {
                    setPadding(32, 16, 32, 16)
                    textSize = 14f
                }
                return object : RecyclerView.ViewHolder(tv) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                (holder.itemView as TextView).text = previewList[position]
            }

            override fun getItemCount(): Int = previewList.size
        }

        rvPreview.adapter = adapter

        if (previewList.isEmpty()) {
            Toast.makeText(this, "该文件未解析出可导入数据", Toast.LENGTH_SHORT).show()
        }

        if (result.warnings.isNotEmpty()) {
            showWarnings(result.warnings)
        }
    }

    private fun importCourses() {
        val semesterName = etSemesterName.text.toString().trim()
        if (semesterName.isEmpty()) {
            Toast.makeText(this, "请输入学期名称", Toast.LENGTH_SHORT).show()
            return
        }

        val startDateStr = etStartDate.text.toString()
        val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(startDateStr)?.time ?: run {
            Toast.makeText(this, "请输入有效的开始日期", Toast.LENGTH_SHORT).show()
            return
        }

        val totalWeeks = etTotalWeeks.text.toString().toIntOrNull() ?: 16

        lifecycleScope.launch {
            try {
                val baselineCourses = loadBaselineCoursesForDedupe(semesterName, startDate, totalWeeks)
                val dedupedCourses = dedupeCourses(previewCourses, baselineCourses, startDate, totalWeeks)
                val dedupedEvents = dedupeEvents(previewEvents)

                val semester = SemesterEntity(
                    name = semesterName,
                    startDate = startDate,
                    endDate = startDate + (totalWeeks * 7 - 1) * 24 * 3600000L,
                    totalWeeks = totalWeeks,
                    isCurrent = true
                )
                val semesterId = repository.insertSemester(semester)

                dedupedCourses.kept.forEach { course ->
                    repository.insertCourse(course.copy(semesterId = semesterId))
                }
                dedupedEvents.kept.forEach { event ->
                    repository.insertEvent(event.copy(sourceMode = SourceMode.SCHEDULE))
                }

                val skippedTotal = dedupedCourses.skippedCount + dedupedEvents.skippedCount
                val skipHint = if (skippedTotal > 0) {
                    "（已去重跳过 $skippedTotal 条）"
                } else {
                    ""
                }

                Toast.makeText(
                    this@IcsImportActivity,
                    "导入成功：${dedupedCourses.kept.size} 门课程，${dedupedEvents.kept.size} 个事件$skipHint",
                    Toast.LENGTH_LONG
                ).show()
                LogManager.i(
                    TAG,
                    "导入成功: semester=$semesterName, courses=${dedupedCourses.kept.size}, events=${dedupedEvents.kept.size}, skipped=$skippedTotal"
                )

                setResult(Activity.RESULT_OK)
                finish()
            } catch (e: Exception) {
                LogManager.e(TAG, "导入失败: error=${e.message}")
                Toast.makeText(this@IcsImportActivity, "导入失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun dedupeCourses(
        input: List<CourseEntity>,
        baselineCourses: List<CourseEntity>,
        semesterStartDate: Long,
        totalWeeks: Int
    ): DedupResult<CourseEntity> {
        val existingWeeksByCore = baselineCourses
            .groupBy { courseDedupeBucketKey(it) }
            .mapValues { (_, list) -> list.flatMap { resolveCourseWeeks(it, semesterStartDate, totalWeeks) }.toSet() }

        val result = mutableListOf<CourseEntity>()
        var skipped = 0

        input.forEach { course ->
            val coreKey = courseDedupeBucketKey(course)
            val existingWeeks = existingWeeksByCore[coreKey].orEmpty()
            val incomingWeeks = resolveCourseWeeks(course, semesterStartDate, totalWeeks)
            val missingWeeks = incomingWeeks.filter { it !in existingWeeks }.toSortedSet()

            if (missingWeeks.isEmpty()) {
                skipped++
            } else {
                result.add(
                    toExplicitWeekCourse(
                        seed = course,
                        explicitWeeks = missingWeeks,
                        plainRemark = stripWeekMarker(course.remark),
                        uid = normalizedUid(course.icsUid)
                    )
                )
            }
        }

        return DedupResult(result, skipped)
    }

    private suspend fun loadBaselineCoursesForDedupe(
        semesterName: String,
        startDate: Long,
        totalWeeks: Int
    ): List<CourseEntity> {
        val matchedSemester = repository.getAllSemesters()
            .filter {
                it.name == semesterName &&
                        isSameLocalDate(it.startDate, startDate) &&
                        it.totalWeeks == totalWeeks
            }
            .maxByOrNull { it.createdAt }
            ?: return emptyList()

        return repository.getCoursesBySemesterList(matchedSemester.id)
    }

    private fun isSameLocalDate(timeA: Long, timeB: Long): Boolean {
        val a = Calendar.getInstance().apply { timeInMillis = timeA }
        val b = Calendar.getInstance().apply { timeInMillis = timeB }
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }


    private suspend fun dedupeEvents(input: List<TimelineEventEntity>): DedupResult<TimelineEventEntity> {
        val existing = repository.getAllEvents()
        val existingKeySet = existing.map { eventFallbackKey(it) }.toMutableSet()
        val existingUidKeySet = existing.mapNotNull { eventUidKey(it) }.toMutableSet()
        val seenKeySet = mutableSetOf<String>()
        val seenUidKeySet = mutableSetOf<String>()

        val result = mutableListOf<TimelineEventEntity>()
        var skipped = 0

        input.forEach { event ->
            val fallback = eventFallbackKey(event)
            val uidKey = eventUidKey(event)

            val duplicated = when {
                uidKey != null && uidKey in existingUidKeySet -> true
                uidKey != null && uidKey in seenUidKeySet -> true
                fallback in existingKeySet -> true
                fallback in seenKeySet -> true
                else -> false
            }

            if (duplicated) {
                skipped++
            } else {
                result.add(event)
                uidKey?.let {
                    existingUidKeySet.add(it)
                    seenUidKeySet.add(it)
                }
                existingKeySet.add(fallback)
                seenKeySet.add(fallback)
            }
        }

        return DedupResult(result, skipped)
    }

    private fun normalizedUid(uid: String?): String? {
        return uid?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun courseFallbackKey(course: CourseEntity): String {
        return listOf(
            course.name.trim(),
            course.teacher?.trim().orEmpty(),
            course.weekDay.toString(),
            course.startTime.toString(),
            course.endTime.toString(),
            course.location?.trim().orEmpty(),
            courseWeekSignature(course)
        ).joinToString("|")
    }

    private fun courseCoreKey(course: CourseEntity): String {
        return listOf(
            course.name.trim(),
            course.teacher?.trim().orEmpty(),
            course.weekDay.toString(),
            course.startTime.toString(),
            course.endTime.toString(),
            course.location?.trim().orEmpty()
        ).joinToString("|")
    }

    private fun courseDedupeBucketKey(course: CourseEntity): String {
        val uid = normalizedUid(course.icsUid)
        return if (uid != null) {
            "UID:$uid"
        } else {
            "CORE:${courseCoreKey(course)}"
        }
    }

    private fun courseUidKey(course: CourseEntity): String? {
        val uid = normalizedUid(course.icsUid) ?: return null
        return "$uid|${courseFallbackKey(course)}"
    }

    private fun eventFallbackKey(event: TimelineEventEntity): String {
        return listOf(
            event.title.trim(),
            event.startTime.toString(),
            (event.endTime ?: -1L).toString(),
            event.location?.trim().orEmpty()
        ).joinToString("|")
    }

    private fun eventUidKey(event: TimelineEventEntity): String? {
        val uid = normalizedUid(event.icsUid) ?: return null
        return "$uid|${eventFallbackKey(event)}"
    }

    private fun resolveCourseWeeks(course: CourseEntity, semesterStartDate: Long, totalWeeks: Int): Set<Int> {
        val explicit = extractExplicitWeeks(course.remark)
        if (explicit.isNotEmpty()) {
            val baseStart = extractIcsBaseStart(course.remark)
            if (baseStart != null) {
                val baseWeek = calcWeekIndex(semesterStartDate, baseStart)
                return explicit.map { baseWeek + it - 1 }
                    .filter { it in 1..totalWeeks }
                    .toSet()
            }
            return explicit.filter { it in 1..totalWeeks }.toSet()
        }

        val start = course.weekStart.coerceAtLeast(1)
        val end = course.weekEnd.coerceAtLeast(start)
        return (start..end).filter { week ->
            when {
                course.isOddWeek -> week % 2 == 1
                course.isEvenWeek -> week % 2 == 0
                else -> true
            }
        }.filter { it in 1..totalWeeks }.toSet()
    }

    private fun extractIcsBaseStart(remark: String?): Long? {
        val raw = Regex("\\[\\[ICS_START:(\\d+)]]")
            .find(remark ?: "")
            ?.groupValues
            ?.getOrNull(1)
            ?: return null
        return raw.toLongOrNull()
    }

    private fun calcWeekIndex(semesterStart: Long, targetTime: Long): Int {
        val startDay = startOfLocalDay(semesterStart)
        val targetDay = startOfLocalDay(targetTime)
        val diffDays = ((targetDay - startDay) / 86_400_000L).toInt()
        return diffDays / 7 + 1
    }

    private fun startOfLocalDay(time: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = time
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun toExplicitWeekCourse(
        seed: CourseEntity,
        explicitWeeks: Set<Int>,
        plainRemark: String?,
        uid: String?
    ): CourseEntity {
        val sortedWeeks = explicitWeeks.filter { it > 0 }.sorted()
        if (sortedWeeks.isEmpty()) return seed

        val marker = "[[WEEKS:${sortedWeeks.joinToString(",")}]]"
        val mergedRemark = if (plainRemark.isNullOrBlank()) marker else "$marker\n${plainRemark.trim()}"

        return seed.copy(
            weekStart = sortedWeeks.first(),
            weekEnd = sortedWeeks.last(),
            isOddWeek = false,
            isEvenWeek = false,
            remark = mergedRemark,
            icsUid = uid
        )
    }

    private fun stripWeekMarker(remark: String?): String? {
        if (remark.isNullOrBlank()) return null
        val plain = remark
            .let { Regex("\\[\\[ICS_START:\\d+]]").replace(it, "") }
            .let { Regex("\\[\\[WEEKS:[0-9,\\s]+]]").replace(it, "") }
            .trim()
        return plain.takeIf { it.isNotEmpty() }
    }

    private fun courseWeekSignature(course: CourseEntity): String {
        val explicit = extractExplicitWeeks(course.remark)
        return if (explicit.isNotEmpty()) {
            "E:${explicit.joinToString(",")}"
        } else {
            "R:${course.weekStart}-${course.weekEnd}:${course.isOddWeek}:${course.isEvenWeek}"
        }
    }

    private fun extractExplicitWeeks(remark: String?): List<Int> {
        val marker = Regex("\\[\\[WEEKS:([0-9,\\s]+)]]")
            .find(remark ?: "")
            ?.groupValues
            ?.getOrNull(1)
            ?: return emptyList()

        return marker.split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it > 0 }
            .distinct()
            .sorted()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        android.app.DatePickerDialog(
            this,
            { _, year, month, day ->
                val date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day)
                etStartDate.setText(date)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showWarnings(warnings: List<String>) {
        AlertDialog.Builder(this)
            .setTitle("解析警告")
            .setMessage(warnings.joinToString("\n"))
            .setPositiveButton("确定", null)
            .show()
    }

    private fun getFileName(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex("_display_name")
            if (it.moveToFirst()) it.getString(nameIndex) else "未知文件"
        } ?: "未知文件"
    }

    private fun getWeekDayName(weekDay: Int): String = when (weekDay) {
        1 -> "周一"
        2 -> "周二"
        3 -> "周三"
        4 -> "周四"
        5 -> "周五"
        6 -> "周六"
        else -> "周日"
    }

    private fun formatTime(timeInMillis: Long): String {
        val hour = timeInMillis / 3600000
        val minute = (timeInMillis % 3600000) / 60000
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }

    private fun getCurrentWeekStart(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        return calendar.timeInMillis
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
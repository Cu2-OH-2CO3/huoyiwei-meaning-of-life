// 路径：utils/IcsParser.kt
package com.memoria.meaningoflife.utils

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.memoria.meaningoflife.data.database.timeline.CourseEntity
import com.memoria.meaningoflife.data.database.timeline.EventType
import com.memoria.meaningoflife.data.database.timeline.SourceMode
import com.memoria.meaningoflife.data.database.timeline.TimelineEventEntity
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.floor
import kotlin.math.max

class IcsParser(private val contentResolver: ContentResolver? = null) {

    companion object {
        private const val TAG = "IcsParser"
        private const val DEFAULT_TOTAL_WEEKS = 16
    }

    data class IcsParseResult(
        val courses: List<CourseEntity>,
        val events: List<TimelineEventEntity>,
        val warnings: List<String>
    )

    fun parseIcsFile(uri: Uri): IcsParseResult {
        val resolver = contentResolver
            ?: return IcsParseResult(emptyList(), emptyList(), listOf("未提供 ContentResolver"))

        val rawText = resolver.openInputStream(uri)?.use { input ->
            input.bufferedReader(Charsets.UTF_8).readText()
        } ?: return IcsParseResult(emptyList(), emptyList(), listOf("无法打开文件"))

        return parseIcsText(rawText)
    }

    fun parseIcsText(rawText: String): IcsParseResult {
        if (rawText.isBlank()) {
            return IcsParseResult(emptyList(), emptyList(), listOf("文件内容为空"))
        }

        val unfoldedText = unfoldIcsLines(rawText)
        val blocks = extractEventBlocks(unfoldedText)

        val courses = mutableListOf<CourseEntity>()
        val events = mutableListOf<TimelineEventEntity>()
        val warnings = mutableListOf<String>()

        blocks.forEachIndexed { index, block ->
            try {
                val summary = block["SUMMARY"]?.firstOrNull()?.value?.let(::unescapeIcsText)
                if (summary.isNullOrBlank()) return@forEachIndexed

                val dtStart = parseIcsDateValue(block["DTSTART"]?.firstOrNull())
                    ?: return@forEachIndexed
                val dtEnd = parseIcsDateValue(block["DTEND"]?.firstOrNull())
                    ?: parseDurationEnd(dtStart, block["DURATION"]?.firstOrNull()?.value)
                    ?: dtStart

                val recurrenceRuleRaw = block["RRULE"]?.firstOrNull()?.value
                val hasWeeklyRule = recurrenceRuleRaw?.contains("FREQ=WEEKLY", ignoreCase = true) == true

                val location = block["LOCATION"]?.firstOrNull()?.let { unescapeIcsText(it.value) }
                val description = block["DESCRIPTION"]?.firstOrNull()?.let { unescapeIcsText(it.value) }
                val uid = block["UID"]?.firstOrNull()?.value?.trim()
                val exDates = parseExDates(block)

                if (hasWeeklyRule) {
                    // 周期性事件 → 生成课程
                    courses.add(parseAsCourse(summary, dtStart, dtEnd, recurrenceRuleRaw, location, description, uid, exDates))
                } else {
                    // 单次事件 → 普通事件
                    events.add(
                        TimelineEventEntity(
                            title = summary,
                            description = description,
                            eventType = EventType.CUSTOM,
                            sourceMode = SourceMode.SCHEDULE,
                            startTime = dtStart.time,
                            endTime = dtEnd.time,
                            location = location,
                            icsUid = uid
                        )
                    )
                }
            } catch (e: Exception) {
                warnings.add("解析第 ${index + 1} 个事件失败: ${e.message}")
                Log.w(TAG, "解析事件失败", e)
                LogManager.w(TAG, "事件#${index + 1} 解析失败: ${e.message}")
            }
        }

        LogManager.i(TAG, "ICS解析完成: courses=${courses.size}, events=${events.size}, warnings=${warnings.size}")

        return IcsParseResult(courses, events, warnings)
    }

    private fun parseAsCourse(
        summary: String,
        start: Date,
        end: Date,
        rrule: String?,
        location: String?,
        description: String?,
        uid: String?,
        exDates: List<Date>
    ): CourseEntity {
        val weekDay = parseWeekDayFromRRule(rrule) ?: run {
            // 某些导出的 RRULE 不带 BYDAY，回退到 DTSTART 对应周几。
            val calendar = Calendar.getInstance().apply { time = start }
            when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                else -> 7
            }
        }

        // 提取开始时间（时分）
        val calendar = Calendar.getInstance().apply { time = start }
        val startTimeOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 3600000L +
                calendar.get(Calendar.MINUTE) * 60000L

        val endCalendar = Calendar.getInstance().apply { time = end }
        val endTimeOfDay = endCalendar.get(Calendar.HOUR_OF_DAY) * 3600000L +
                endCalendar.get(Calendar.MINUTE) * 60000L

        val normalizedEnd = max(endTimeOfDay, startTimeOfDay + 30 * 60 * 1000L)

        val derivedWeeks = deriveWeeks(start, rrule, exDates)
        val weekStart = derivedWeeks?.firstOrNull() ?: 1
        val weekEnd = derivedWeeks?.lastOrNull() ?: DEFAULT_TOTAL_WEEKS
        val mergedRemark = mergeRemarkWithWeeks(description, derivedWeeks, start)

        return CourseEntity(
            name = normalizeCourseName(summary, location),
            teacher = extractTeacher(description),
            location = location,
            weekDay = weekDay,
            startTime = startTimeOfDay,
            endTime = normalizedEnd,
            weekStart = weekStart,
            weekEnd = weekEnd,
            remark = mergedRemark,
            icsUid = normalizedUid(uid) ?: buildSyntheticCourseUid(summary, start, end, rrule, location)
        )
    }

    private fun normalizedUid(uid: String?): String? {
        return uid?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun buildSyntheticCourseUid(
        summary: String,
        start: Date,
        end: Date,
        rrule: String?,
        location: String?
    ): String {
        val seed = listOf(
            summary.trim(),
            start.time.toString(),
            end.time.toString(),
            rrule?.trim().orEmpty(),
            location?.trim().orEmpty()
        ).joinToString("|")
        return "ics-course-${seed.hashCode().toUInt().toString(16)}"
    }

    private fun extractTeacher(description: String?): String? {
        if (description == null) return null
        val teacherPattern = Regex("(?:教师|老师)[：:]\\s*([^\\n\\r]+)")
        return teacherPattern.find(description)?.groupValues?.get(1)?.trim()?.ifEmpty { null }
    }

    private fun normalizeCourseName(summary: String, location: String?): String {
        val trimmed = summary.trim()
        val atIndex = trimmed.indexOf('@')
        if (atIndex <= 0) return trimmed
        val beforeAt = trimmed.substring(0, atIndex).trim()
        val afterAt = trimmed.substring(atIndex + 1).trim()
        return if (location != null && afterAt.equals(location.trim(), ignoreCase = true)) {
            beforeAt
        } else {
            trimmed
        }
    }

    private fun parseWeekDayFromRRule(rrule: String?): Int? {
        if (rrule.isNullOrBlank()) return null
        val bydayPattern = Regex("BYDAY=([^;]+)", RegexOption.IGNORE_CASE)
        val dayToken = bydayPattern.find(rrule)
            ?.groupValues
            ?.getOrNull(1)
            ?.split(',')
            ?.firstOrNull()
            ?.trim()
            ?.uppercase(Locale.getDefault())
            ?: return null

        return when {
            dayToken.endsWith("MO") -> 1
            dayToken.endsWith("TU") -> 2
            dayToken.endsWith("WE") -> 3
            dayToken.endsWith("TH") -> 4
            dayToken.endsWith("FR") -> 5
            dayToken.endsWith("SA") -> 6
            dayToken.endsWith("SU") -> 7
            else -> null
        }
    }

    private fun parseExDates(block: Map<String, List<IcsField>>): List<Date> {
        return block["EXDATE"].orEmpty().flatMap { parseIcsDateValues(it) }
    }

    private fun parseIcsDateValues(field: IcsField): List<Date> {
        val tz = field.params["TZID"]
        return field.value.split(',')
            .mapNotNull { parseIcsDateRaw(it.trim(), tz) }
    }

    private fun parseDurationEnd(start: Date, rawDuration: String?): Date? {
        val millis = parseDurationMillis(rawDuration) ?: return null
        return Date(start.time + millis)
    }

    private fun parseDurationMillis(rawDuration: String?): Long? {
        if (rawDuration.isNullOrBlank()) return null
        val pattern = Regex("^P(?:(\\d+)W)?(?:(\\d+)D)?(?:T(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?)?$", RegexOption.IGNORE_CASE)
        val match = pattern.matchEntire(rawDuration.trim()) ?: return null

        val weeks = match.groupValues[1].toLongOrNull() ?: 0L
        val days = match.groupValues[2].toLongOrNull() ?: 0L
        val hours = match.groupValues[3].toLongOrNull() ?: 0L
        val minutes = match.groupValues[4].toLongOrNull() ?: 0L
        val seconds = match.groupValues[5].toLongOrNull() ?: 0L

        return (((weeks * 7 + days) * 24 + hours) * 60 + minutes) * 60_000L + seconds * 1000L
    }

    private fun deriveWeeks(start: Date, rrule: String?, exDates: List<Date>): List<Int>? {
        if (rrule.isNullOrBlank()) return null

        val interval = parseIntRRule(rrule, "INTERVAL")?.coerceAtLeast(1) ?: 1
        val count = parseIntRRule(rrule, "COUNT")
        val until = parseStringRRule(rrule, "UNTIL")?.let { parseIcsDateRaw(it, null) }

        val exWeekSet = exDates.mapNotNull { diffWeekIndex(start, it) }.toSet()

        val weeks = mutableListOf<Int>()
        if (count != null && count > 0) {
            var week = 1
            repeat(count) {
                if (week !in exWeekSet) weeks.add(week)
                week += interval
            }
        } else {
            val limitWeek = until?.let { diffWeekIndex(start, it)?.coerceAtLeast(1) } ?: DEFAULT_TOTAL_WEEKS
            var week = 1
            while (week <= limitWeek) {
                if (week !in exWeekSet) weeks.add(week)
                week += interval
            }
        }

        if (weeks.isEmpty()) return null
        return weeks.distinct().sorted()
    }

    private fun parseIntRRule(rrule: String, key: String): Int? {
        val raw = parseStringRRule(rrule, key) ?: return null
        return raw.toIntOrNull()
    }

    private fun parseStringRRule(rrule: String, key: String): String? {
        val regex = Regex("(?:^|;)${key}=([^;]+)", RegexOption.IGNORE_CASE)
        return regex.find(rrule)?.groupValues?.getOrNull(1)?.trim()
    }

    private fun diffWeekIndex(start: Date, target: Date): Int? {
        val startMs = startOfLocalDay(start.time)
        val targetMs = startOfLocalDay(target.time)
        if (targetMs < startMs) return null
        val days = floor((targetMs - startMs).toDouble() / 86_400_000.0).toInt()
        return days / 7 + 1
    }

    private fun startOfLocalDay(epochMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = epochMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun mergeRemarkWithWeeks(remark: String?, explicitWeeks: List<Int>?, start: Date): String? {
        if (explicitWeeks.isNullOrEmpty()) return remark
        val marker = "[[WEEKS:${explicitWeeks.joinToString(",") }]]"
        val baseMarker = "[[ICS_START:${start.time}]]"
        val plain = remark?.trim().orEmpty()
        val prefix = "$baseMarker\n$marker"
        return if (plain.isEmpty()) prefix else "$prefix\n$plain"
    }

    private fun unfoldIcsLines(text: String): String {
        val lines = text.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val unfolded = mutableListOf<String>()
        lines.forEach { line ->
            if ((line.startsWith(" ") || line.startsWith("\t")) && unfolded.isNotEmpty()) {
                unfolded[unfolded.lastIndex] = unfolded.last() + line.trimStart()
            } else {
                unfolded.add(line)
            }
        }
        return unfolded.joinToString("\n")
    }

    private data class IcsField(
        val params: Map<String, String>,
        val value: String
    )

    private fun extractEventBlocks(unfoldedText: String): List<Map<String, List<IcsField>>> {
        val eventBlockRegex = Regex("BEGIN:VEVENT([\\s\\S]*?)END:VEVENT", RegexOption.IGNORE_CASE)
        return eventBlockRegex.findAll(unfoldedText).map { match ->
            parseEventBlock(match.value)
        }.toList()
    }

    private fun parseEventBlock(block: String): Map<String, List<IcsField>> {
        val parsed = linkedMapOf<String, MutableList<IcsField>>()
        block.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEach
            if (trimmed.equals("BEGIN:VEVENT", ignoreCase = true) || trimmed.equals("END:VEVENT", ignoreCase = true)) {
                return@forEach
            }

            val colonIndex = trimmed.indexOf(':')
            if (colonIndex <= 0) return@forEach

            val header = trimmed.substring(0, colonIndex)
            val value = trimmed.substring(colonIndex + 1)

            val headerParts = header.split(';')
            val key = headerParts.first().uppercase(Locale.getDefault())
            val params = linkedMapOf<String, String>()
            headerParts.drop(1).forEach { part ->
                val eqIndex = part.indexOf('=')
                if (eqIndex > 0 && eqIndex < part.length - 1) {
                    val paramName = part.substring(0, eqIndex).uppercase(Locale.getDefault())
                    val paramValue = part.substring(eqIndex + 1)
                    params[paramName] = paramValue
                }
            }

            parsed.getOrPut(key) { mutableListOf() }.add(IcsField(params, value))
        }
        return parsed
    }

    private fun parseIcsDateValue(field: IcsField?): Date? {
        if (field == null) return null
        return parseIcsDateRaw(field.value.trim(), field.params["TZID"])
    }

    private fun parseIcsDateRaw(raw: String, tzId: String?): Date? {
        if (raw.isEmpty()) return null

        val tz = tzId?.let { TimeZone.getTimeZone(it) }
        val formatCandidates = when {
            raw.endsWith("Z", ignoreCase = true) -> listOf("yyyyMMdd'T'HHmmss'Z'", "yyyyMMdd'T'HHmm'Z'")
            raw.contains('T') -> listOf("yyyyMMdd'T'HHmmss", "yyyyMMdd'T'HHmm")
            else -> listOf("yyyyMMdd")
        }

        for (pattern in formatCandidates) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                    timeZone = when {
                        pattern.endsWith("'Z'") -> TimeZone.getTimeZone("UTC")
                        tz != null -> tz
                        else -> TimeZone.getDefault()
                    }
                }
                val parsed = sdf.parse(raw) ?: continue
                return parsed
            } catch (_: ParseException) {
                // Try next pattern.
            }
        }

        return null
    }

    private fun unescapeIcsText(value: String): String {
        return value
            .replace("\\\\n", "\n", ignoreCase = true)
            .replace("\\N", "\n", ignoreCase = true)
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")
    }
}
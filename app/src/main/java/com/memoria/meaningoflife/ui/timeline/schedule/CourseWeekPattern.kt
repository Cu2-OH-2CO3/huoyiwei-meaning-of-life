package com.memoria.meaningoflife.ui.timeline.schedule

import com.memoria.meaningoflife.data.database.timeline.CourseEntity
import kotlin.math.max
import kotlin.math.min

object CourseWeekPattern {
    private val markerRegex = Regex("\\[\\[WEEKS:([0-9,\\s]+)]]")

    fun parseExplicitWeeks(remark: String?): Set<Int>? {
        val raw = markerRegex.find(remark ?: "")?.groupValues?.getOrNull(1) ?: return null
        val weeks = raw.split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it > 0 }
            .toSortedSet()
        return weeks.takeIf { it.isNotEmpty() }
    }

    fun stripWeekMarker(remark: String?): String? {
        if (remark.isNullOrBlank()) return null
        val plain = markerRegex.replace(remark, "").trim()
        return plain.takeIf { it.isNotBlank() }
    }

    fun mergeRemarkWithWeeks(plainRemark: String?, explicitWeeks: Set<Int>?): String? {
        val cleanedRemark = plainRemark?.trim().orEmpty()
        val normalizedWeeks = explicitWeeks?.filter { it > 0 }?.toSortedSet().orEmpty()

        if (normalizedWeeks.isEmpty()) {
            return cleanedRemark.ifBlank { null }
        }

        val marker = "[[WEEKS:${normalizedWeeks.joinToString(",")}]]"
        return if (cleanedRemark.isBlank()) marker else "$marker\n$cleanedRemark"
    }

    fun resolveWeeks(course: CourseEntity, totalWeeks: Int = 30): Set<Int> {
        val explicit = parseExplicitWeeks(course.remark)
        if (!explicit.isNullOrEmpty()) return explicit

        val safeStart = max(1, min(course.weekStart, totalWeeks))
        val safeEnd = max(safeStart, min(course.weekEnd, totalWeeks))

        return (safeStart..safeEnd).filter { week ->
            if (course.isOddWeek) {
                week % 2 == 1
            } else if (course.isEvenWeek) {
                week % 2 == 0
            } else {
                true
            }
        }.toSet()
    }

    fun shouldShowInWeek(course: CourseEntity, week: Int): Boolean {
        if (week <= 0) return false
        val explicit = parseExplicitWeeks(course.remark)
        if (!explicit.isNullOrEmpty()) {
            return week in explicit
        }

        if (week < course.weekStart || week > course.weekEnd) return false
        if (course.isOddWeek && week % 2 == 0) return false
        if (course.isEvenWeek && week % 2 == 1) return false
        return true
    }

    fun buildWeekInfoText(course: CourseEntity): String {
        val explicit = parseExplicitWeeks(course.remark)
        if (!explicit.isNullOrEmpty()) {
            return "第${explicit.joinToString(",")}周"
        }

        return buildString {
            append("第${course.weekStart}-${course.weekEnd}周")
            if (course.isOddWeek) append("，单周")
            if (course.isEvenWeek) append("，双周")
        }
    }
}


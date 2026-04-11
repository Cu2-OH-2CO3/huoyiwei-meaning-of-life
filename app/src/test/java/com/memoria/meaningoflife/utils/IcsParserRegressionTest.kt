package com.memoria.meaningoflife.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class IcsParserRegressionTest {

    @Test
    fun parseRootIcsFile_shouldExtractCoursesAndEvents() {
        val testFile = findRootIcsFile()
        val text = testFile.readText(Charsets.UTF_8)

        val result = IcsParser().parseIcsText(text)

        assertTrue("应至少解析出1门课程", result.courses.isNotEmpty())
        assertTrue("应至少解析出1个事件", result.events.isNotEmpty())
    }

    @Test
    fun parseDuration_whenDtEndMissing_shouldUseDurationAsEnd() {
        val ics = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            UID:event-duration-1
            SUMMARY:持续事件
            DTSTART:20260410T083000
            DURATION:PT90M
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val result = IcsParser().parseIcsText(ics)
        val event = result.events.first()

        assertEquals("应解析出1个事件", 1, result.events.size)
        assertEquals("结束时间应为开始后90分钟", 90 * 60_000L, (event.endTime ?: event.startTime) - event.startTime)
    }

    @Test
    fun parseWeeklyCourse_withUntilAndExDate_shouldBuildExplicitWeeksMarker() {
        val ics = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            UID:course-week-1
            SUMMARY:高等数学
            DTSTART;TZID=Asia/Shanghai:20260302T083000
            DTEND;TZID=Asia/Shanghai:20260302T101000
            RRULE:FREQ=WEEKLY;BYDAY=MO;UNTIL=20260413T235959Z
            EXDATE;TZID=Asia/Shanghai:20260316T083000
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val result = IcsParser().parseIcsText(ics)
        val course = result.courses.first()
        val weeks = parseWeeks(course.remark)

        assertEquals("应解析出1门课程", 1, result.courses.size)
        assertTrue("应写入显式周次标记", (course.remark ?: "").contains("[[WEEKS:"))
        assertTrue("应包含第1周", 1 in weeks)
        assertTrue("应排除第3周", 3 !in weeks)
    }

    private fun parseWeeks(remark: String?): Set<Int> {
        val marker = Regex("\\[\\[WEEKS:([0-9,\\s]+)]]")
            .find(remark ?: "")
            ?.groupValues
            ?.getOrNull(1)
            ?: return emptySet()
        return marker.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    private fun findRootIcsFile(): File {
        val fileName = "classtable-20260409T130140-2025-2026-2.ics"
        val baseDir = File(System.getProperty("user.dir") ?: ".")
        val candidates = listOf(
            File(baseDir, fileName),
            File(baseDir, "../$fileName"),
            File(baseDir, "../../$fileName")
        )

        return candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("未找到测试ICS文件: $fileName")
    }
}


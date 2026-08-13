package com.example.timetable

import com.example.timetable.model.Course
import com.example.timetable.model.WeekType
import com.example.timetable.model.createActiveWeeks
import com.example.timetable.model.formatActiveWeeks
import com.example.timetable.model.parseActiveWeeks
import com.example.timetable.model.weekSchedulesOverlap
import com.example.timetable.importer.BuctPdfTimetableParser
import com.example.timetable.importer.NenuPdfTimetableParser
import com.example.timetable.data.toEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun parsesMultipleWeekRanges() {
        assertEquals(
            ((1..9) + (11..18)).toSet(),
            parseActiveWeeks("1-9周,11-18周", 20)
        )
    }

    @Test
    fun acceptsChineseSeparatorsAndFormatsRanges() {
        val weeks = parseActiveWeeks("1至3、5，7-8", 20)

        assertEquals(setOf(1, 2, 3, 5, 7, 8), weeks)
        assertEquals("1-3,5,7-8", formatActiveWeeks(requireNotNull(weeks)))
    }

    @Test
    fun rejectsInvalidOrOutOfSemesterWeeks() {
        assertNull(parseActiveWeeks("", 20))
        assertNull(parseActiveWeeks("5-3", 20))
        assertNull(parseActiveWeeks("1-21", 20))
    }

    @Test
    fun convertsLegacyOddWeekSchedule() {
        assertEquals(
            setOf(1, 3, 5, 7),
            createActiveWeeks(1, 8, WeekType.ODD_WEEK)
        )
    }

    @Test
    fun detectsOverlapUsingExactWeeks() {
        val existing = Course(
            name = "实验课",
            teacher = "教师",
            classroom = "实验室",
            weekDay = "周一",
            startSection = 1,
            endSection = 2,
            activeWeeks = setOf(1, 2, 4)
        )

        assertTrue(weekSchedulesOverlap(setOf(2, 3), existing))
        assertFalse(weekSchedulesOverlap(setOf(3, 5), existing))
    }

    @Test
    fun associatesStoredCourseWithSelectedTimetable() {
        val course = Course(
            name = "测试课程",
            teacher = "教师",
            classroom = "教室",
            weekDay = "周一",
            startSection = 1,
            endSection = 2,
            activeWeeks = setOf(1, 2)
        )

        assertEquals(42L, course.toEntity(timetableId = 42L).timetableId)
    }

    @Test
    fun preservesCustomCourseTimesInStorage() {
        val course = Course(
            name = "自定义时间课程",
            teacher = "教师",
            classroom = "教室",
            weekDay = "周二",
            startSection = 2,
            endSection = 4,
            activeWeeks = setOf(1, 2),
            customStartMinutes = 9 * 60 + 20,
            customEndMinutes = 10 * 60 + 50
        )

        val entity = course.toEntity(timetableId = 1L)
        assertEquals(560, entity.customStartMinutes)
        assertEquals(650, entity.customEndMinutes)
    }

    @Test
    fun parsesBuctCourseCellText() {
        val courses = BuctPdfTimetableParser.parseCourseColumn(
            text = """
                概率论与数理统计A★
                (1-2节)1-9周,11-16周/校区
                :北区/场地:一教B阶-303/教
                师:李志强/教学班:概率论与数理统计A-0002
            """.trimIndent(),
            weekDay = "周五",
            totalWeeks = 20
        )

        assertEquals(1, courses.size)
        assertEquals("概率论与数理统计A", courses.single().name)
        assertEquals("周五", courses.single().weekDay)
        assertEquals(1, courses.single().startSection)
        assertEquals(2, courses.single().endSection)
        assertEquals("一教B阶-303", courses.single().classroom)
        assertEquals("李志强", courses.single().teacher)
        assertEquals(((1..9) + (11..16)).toSet(), courses.single().activeWeeks)
    }

    @Test
    fun usesDisplayedWidthForRotatedBuctPdfPages() {
        assertEquals(
            842f,
            BuctPdfTimetableParser.displayedPageWidth(595f, 842f, 90),
            0f
        )
        assertEquals(
            842f,
            BuctPdfTimetableParser.displayedPageWidth(842f, 595f, 0),
            0f
        )
    }

    @Test
    fun parsesNenuOddEvenAndExplicitWeekCourses() {
        val courses = NenuPdfTimetableParser.parseCell(
            nameLines = listOf(
                NenuPdfTimetableParser.TextLine(100f, "*微观经济学概论"),
                NenuPdfTimetableParser.TextLine(110f, "**微观经济学概论"),
                NenuPdfTimetableParser.TextLine(120f, "劳动教育（1-2周）")
            ),
            teacherLines = listOf(
                NenuPdfTimetableParser.TextLine(100f, "Mario Gonzales"),
                NenuPdfTimetableParser.TextLine(110f, "朱鸿")
            ),
            roomLines = listOf(NenuPdfTimetableParser.TextLine(105f, "传媒东130")),
            weekDay = "周一",
            startSection = 1,
            endSection = 2
        )

        assertEquals(3, courses.size)
        assertEquals((1..16 step 2).toSet(), courses[0].activeWeeks)
        assertEquals((2..16 step 2).toSet(), courses[1].activeWeeks)
        assertEquals(setOf(1, 2), courses[2].activeWeeks)
        assertEquals("劳动教育", courses[2].name)
        assertEquals("传媒东130", courses[0].classroom)
    }
}

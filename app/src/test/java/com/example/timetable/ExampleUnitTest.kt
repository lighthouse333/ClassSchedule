package com.example.timetable

import com.example.timetable.model.Course
import com.example.timetable.model.WeekType
import com.example.timetable.model.createActiveWeeks
import com.example.timetable.model.findWeekContainingDate
import com.example.timetable.model.formatActiveWeeks
import com.example.timetable.model.parseActiveWeeks
import com.example.timetable.model.weekSchedulesOverlap
import com.example.timetable.importer.BuctPdfTimetableParser
import com.example.timetable.importer.NenuPdfTimetableParser
import com.example.timetable.data.toEntity
import com.example.timetable.data.toDomain
import com.example.timetable.update.parseReleaseVersionCode
import com.example.timetable.update.shouldShowUpdatePrompt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExampleUnitTest {
    @Test
    fun parsesReleaseVersionCodeUsingSupportedSeparators() {
        assertEquals(5L, parseReleaseVersionCode("版本：1.2.0（versionCode 5）"))
        assertEquals(6L, parseReleaseVersionCode("versionCode：6"))
        assertEquals(7L, parseReleaseVersionCode("versionCode: 7"))
        assertEquals(8L, parseReleaseVersionCode("versionCode = 8"))
        assertNull(parseReleaseVersionCode("版本：1.2.0"))
    }

    @Test
    fun controlsAutomaticUpdatePromptReminders() {
        val day = 24 * 60 * 60 * 1000L
        val now = 10 * day

        assertFalse(shouldShowUpdatePrompt(false, 7, -1, 0, now, day))
        assertFalse(shouldShowUpdatePrompt(true, 7, 7, now - day / 2, now, day))
        assertTrue(shouldShowUpdatePrompt(true, 7, 7, now - day, now, day))
        assertTrue(shouldShowUpdatePrompt(true, 8, 7, now, now, day))
    }

    @Test
    fun findsWeekUsingDisplayedDateRangesAndClampsOutsideSemester() {
        val semesterStart = LocalDate.of(2026, 9, 7)

        assertEquals(
            1,
            findWeekContainingDate(LocalDate.of(2026, 8, 13), semesterStart, 20)
        )
        assertEquals(
            3,
            findWeekContainingDate(LocalDate.of(2026, 9, 21), semesterStart, 20)
        )
        assertEquals(
            20,
            findWeekContainingDate(LocalDate.of(2027, 2, 1), semesterStart, 20)
        )
    }

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
    fun preservesCourseNoteInStorage() {
        val course = Course(
            name = "测试课程",
            teacher = "教师",
            classroom = "教室",
            weekDay = "周三",
            startSection = 1,
            endSection = 2,
            activeWeeks = setOf(1, 2),
            note = "下周记得带实验报告"
        )

        val restored = course.toEntity(timetableId = 1L).toDomain()

        assertEquals("下周记得带实验报告", restored.note)
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
    fun parsesBuctStudentCourseWithoutTeacherField() {
        val courses = BuctPdfTimetableParser.parseCourseColumn(
            text = """
                电子技术实验（Ⅰ）○
                (1-4节)11-18周/校区:北区
                /场地:未排地点/教学班:电子技术实验（Ⅰ）-0008
                /教学班组成:电科2501;电科2502/课程学时组成:实验:32
            """.trimIndent(),
            weekDay = "周四",
            totalWeeks = 20
        )

        assertEquals(1, courses.size)
        assertEquals("电子技术实验(Ⅰ)", courses.single().name)
        assertEquals("未排地点", courses.single().classroom)
        assertEquals("无", courses.single().teacher)
        assertEquals((11..18).toSet(), courses.single().activeWeeks)
    }

    @Test
    fun parsesBuctFieldsIndependentlyOfTheirOrder() {
        val courses = BuctPdfTimetableParser.parseCourseColumn(
            text = """
                字段乱序课程★
                (3-5节)1-4周/教学班:字段乱序课程-0001
                /教师:张老师/校区:北区/场地:一教101
            """.trimIndent(),
            weekDay = "周二",
            totalWeeks = 20
        )

        assertEquals(1, courses.size)
        assertEquals("张老师", courses.single().teacher)
        assertEquals("一教101", courses.single().classroom)
        assertEquals(3, courses.single().startSection)
        assertEquals(5, courses.single().endSection)
    }

    @Test
    fun fillsMissingBuctOptionalFieldsWithNone() {
        val courses = BuctPdfTimetableParser.parseCourseColumn(
            text = "缺少可选字段课程★\n(6-7节)2-3周/教学班:课程-0001",
            weekDay = "周三",
            totalWeeks = 20
        )

        assertEquals(1, courses.size)
        assertEquals("无", courses.single().teacher)
        assertEquals("无", courses.single().classroom)
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

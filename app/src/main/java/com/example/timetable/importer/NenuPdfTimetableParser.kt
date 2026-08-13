package com.example.timetable.importer

import android.content.Context
import android.net.Uri
import com.example.timetable.model.Course
import com.example.timetable.model.parseActiveWeeks
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.InputStream
import kotlin.math.abs

class NenuPdfTimetableParser(private val context: Context) : TimetableFileParser {
    override fun parse(uri: Uri, totalWeeks: Int): ParsedTimetable {
        PDFBoxResourceLoader.init(context.applicationContext)
        val input = requireNotNull(context.contentResolver.openInputStream(uri)) {
            "无法打开所选文件"
        }
        return input.use(::parseDocument)
    }

    private fun parseDocument(input: InputStream): ParsedTimetable {
        PDDocument.load(input).use { document ->
            val collector = PositionedTextCollector().apply { sortByPosition = true }
            collector.getText(document)
            val completeText = collector.completeText()
            require("东北师范大学" in completeText && "课程表" in completeText) {
                "未识别到东北师范大学课表表头"
            }

            val groups = collector.pages.flatMap(::parsePage)
            require(groups.isNotEmpty() && groups.any { it.courses.isNotEmpty() }) {
                "没有识别到课程；该文件可能是扫描件或课表版式暂不受支持"
            }
            return ParsedTimetable(
                title = "东北师范大学课程表",
                semester = Regex("(\\d{4}\\s*年(?:春季|秋季)学期)")
                    .find(completeText)?.value?.replace(" ", ""),
                courses = groups.first().courses,
                warnings = emptyList(),
                groups = groups
            )
        }
    }

    private fun parsePage(page: PageText): List<ParsedTimetableGroup> {
        val headerItems = page.items.filter { it.y in 135f..170f }
        val classNames = headerItems
            .groupByLine()
            .flatMap { line ->
                Regex("2025\\s*级[^班]{1,20}班").findAll(line.text).map { match ->
                    Header(match.value.replace(Regex("\\s+"), ""), line.centerX)
                }
            }
            .ifEmpty {
                headerItems.filter { "班" in it.text }.map { Header(it.text, it.x) }
            }
            .sortedBy(Header::x)
        if (classNames.isEmpty()) return emptyList()

        val tableLeft = page.width * 0.113f
        val tableRight = page.width * 0.969f
        val bandWidth = (tableRight - tableLeft) / classNames.size
        val sectionMarkers = page.items
            .filter { it.x in page.width * 0.075f..page.width * 0.115f }
            .groupByLine()
            .mapNotNull { line ->
                SECTION_PATTERN.matchEntire(line.text.trim())?.let { line.y to it.groupValues }
            }
            .sortedBy { it.first }
        if (sectionMarkers.size < 20) return emptyList()

        return classNames.mapIndexed { classIndex, header ->
            val warnings = mutableListOf<String>()
            val courses = mutableListOf<Course>()
            val bandStart = tableLeft + classIndex * bandWidth
            val bandEnd = bandStart + bandWidth
            sectionMarkers.forEachIndexed { rowIndex, marker ->
                val centerY = marker.first
                val previousY = sectionMarkers.getOrNull(rowIndex - 1)?.first
                val nextY = sectionMarkers.getOrNull(rowIndex + 1)?.first
                val top = previousY?.let { (it + centerY) / 2f } ?: centerY - 14f
                val bottom = nextY?.let { (it + centerY) / 2f } ?: centerY + 14f
                val rowItems = page.items.filter {
                    it.x >= bandStart && it.x < bandEnd && it.y in top..bottom
                }
                val relative = { item: PositionedText -> (item.x - bandStart) / bandWidth }
                val nameLines = rowItems.filter { relative(it) < 0.32f }.groupByLine()
                val teacherLines = rowItems.filter { relative(it) in 0.43f..0.70f }.groupByLine()
                val roomLines = rowItems.filter { relative(it) >= 0.70f }.groupByLine()
                val dayIndex = rowIndex / 5
                val startSection = marker.second[1].toInt()
                val endSection = marker.second[2].toInt()
                courses += parseCell(
                    nameLines = nameLines,
                    teacherLines = teacherLines,
                    roomLines = roomLines,
                    weekDay = WEEK_DAYS.getOrElse(dayIndex) { "周一" },
                    startSection = startSection,
                    endSection = endSection,
                    warnings = warnings
                )
            }
            ParsedTimetableGroup(
                name = header.name,
                courses = mergeAdjacentCourses(courses).sortedWith(
                    compareBy<Course> { WEEK_DAYS.indexOf(it.weekDay) }
                        .thenBy(Course::startSection)
                ),
                warnings = warnings.distinct()
            )
        }
    }

    companion object {
        private const val TEACHING_WEEKS = 16
        private val WEEK_DAYS = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        private val SECTION_PATTERN = Regex("(\\d+)\\.(\\d+)")

        internal fun parseCell(
            nameLines: List<TextLine>,
            teacherLines: List<TextLine>,
            roomLines: List<TextLine>,
            weekDay: String,
            startSection: Int,
            endSection: Int,
            warnings: MutableList<String> = mutableListOf()
        ): List<Course> {
            val entries = mutableListOf<TextLine>()
            nameLines.sortedBy(TextLine::y).forEach { line ->
                val text = line.text.trim()
                if (text.isBlank() || text == "课程名称") return@forEach
                if ((text.startsWith("(") || text.startsWith("（")) && entries.isNotEmpty()) {
                    val previous = entries.removeLast()
                    entries += previous.copy(text = previous.text + text)
                } else {
                    entries += line.copy(text = text)
                }
            }

            return entries.mapNotNull { entry ->
                val prefixLength = entry.text.takeWhile { it == '*' || it == '＊' }.length
                val rawName = entry.text.drop(prefixLength).trim()
                val explicitWeeks = Regex("[（(]([0-9/、,，\\-—~至周]+)[）)]")
                    .find(rawName)?.groupValues?.get(1)
                val activeWeeks = when {
                    explicitWeeks != null -> parseActiveWeeks(
                        explicitWeeks.replace('/', ','), TEACHING_WEEKS
                    )
                    prefixLength >= 2 -> (2..TEACHING_WEEKS step 2).toSet()
                    prefixLength == 1 -> (1..TEACHING_WEEKS step 2).toSet()
                    else -> (1..TEACHING_WEEKS).toSet()
                }
                if (activeWeeks == null) {
                    warnings += "$weekDay $rawName：无法识别周次"
                    return@mapNotNull null
                }
                val name = rawName.replace(Regex("[（(][0-9/、,，\\-—~至周]+[）)]"), "").trim()
                if (name.isBlank()) return@mapNotNull null
                Course(
                    name = name,
                    teacher = teacherLines.closestText(entry.y),
                    classroom = roomLines.closestText(entry.y),
                    weekDay = weekDay,
                    startSection = startSection,
                    endSection = endSection,
                    activeWeeks = activeWeeks
                )
            }
        }

        private fun mergeAdjacentCourses(courses: List<Course>): List<Course> = courses
            .sortedWith(compareBy<Course> { it.weekDay }.thenBy { it.startSection })
            .fold(mutableListOf()) { merged, course ->
                val previous = merged.lastOrNull()
                if (previous != null && previous.name == course.name &&
                    previous.teacher == course.teacher && previous.classroom == course.classroom &&
                    previous.weekDay == course.weekDay && previous.activeWeeks == course.activeWeeks &&
                    previous.endSection + 1 == course.startSection
                ) {
                    merged[merged.lastIndex] = previous.copy(endSection = course.endSection)
                } else {
                    merged += course
                }
                merged
            }

        private fun List<TextLine>.closestText(y: Float): String = minByOrNull {
            abs(it.y - y)
        }?.text?.trim().orEmpty()
    }

    internal data class TextLine(val y: Float, val text: String, val centerX: Float = 0f)
    private data class Header(val name: String, val x: Float)
    private data class PositionedText(val page: Int, val x: Float, val y: Float, val text: String)
    private data class PageText(val width: Float, val items: List<PositionedText>)

    private class PositionedTextCollector : PDFTextStripper() {
        private val pageItems = mutableListOf<MutableList<PositionedText>>()
        private val widths = mutableListOf<Float>()
        private var currentPage = -1

        override fun startPage(page: com.tom_roush.pdfbox.pdmodel.PDPage) {
            super.startPage(page)
            currentPage += 1
            pageItems.add(mutableListOf())
            widths.add(page.cropBox.width)
        }

        override fun processTextPosition(text: TextPosition) {
            super.processTextPosition(text)
            pageItems[currentPage] += PositionedText(
                page = currentPage,
                x = text.xDirAdj,
                y = text.yDirAdj,
                text = text.unicode
            )
        }

        val pages: List<PageText>
            get() = pageItems.mapIndexed { index, items -> PageText(widths[index], items) }

        fun completeText(): String = pageItems.flatten()
            .groupBy { it.page to (it.y * 2).toInt() }
            .toSortedMap(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second })
            .values
            .joinToString("\n") { line ->
                line.sortedBy(PositionedText::x).joinToString("") { it.text }
            }
    }

    private fun List<PositionedText>.groupByLine(): List<TextLine> = groupBy {
        it.page to (it.y * 2).toInt()
    }.values.map { characters ->
        val sorted = characters.sortedBy(PositionedText::x)
        TextLine(
            y = sorted.map(PositionedText::y).average().toFloat(),
            text = sorted.joinToString("") { it.text },
            centerX = (sorted.minOf(PositionedText::x) + sorted.maxOf(PositionedText::x)) / 2f
        )
    }.sortedBy(TextLine::y)
}

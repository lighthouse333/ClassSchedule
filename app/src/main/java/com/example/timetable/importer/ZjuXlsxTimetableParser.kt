package com.example.timetable.importer

import android.content.Context
import android.net.Uri
import com.example.timetable.model.Course
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.SortedMap
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

class ZjuXlsxTimetableParser(private val context: Context) : TimetableFileParser {
    override fun parse(uri: Uri, totalWeeks: Int): ParsedTimetable {
        val input = requireNotNull(context.contentResolver.openInputStream(uri)) {
            "无法打开所选文件"
        }
        return input.use { parseWorkbook(it, totalWeeks) }
    }

    companion object {
        private const val SHARED_STRINGS_PATH = "xl/sharedStrings.xml"
        private val WORKSHEET_PATH = Regex("xl/worksheets/sheet(\\d+)\\.xml")
        private val TIME_PATTERN = Regex("周([一二三四五六日天])第([0-9,，、]+)节")
        private val REQUIRED_HEADERS = setOf("课程名称", "教师姓名", "学期", "上课时间", "上课地点")
        private const val MAX_ENTRY_BYTES = 8 * 1024 * 1024
        private const val MAX_WORKBOOK_BYTES = 24 * 1024 * 1024

        internal fun parseWorkbook(input: InputStream, totalWeeks: Int): ParsedTimetable {
            val entries = readXlsxEntries(input)
            val sharedStrings = entries[SHARED_STRINGS_PATH]
                ?.let(::parseSharedStrings)
                .orEmpty()
            val sheets = entries.entries
                .filter { (path, _) -> WORKSHEET_PATH.matches(path) }
                .sortedBy { (path, _) -> worksheetNumber(path) }

            require(sheets.isNotEmpty()) { "文件中没有工作表" }
            val rows = sheets.asSequence()
                .map { (_, bytes) -> parseRows(bytes, sharedStrings) }
                .firstOrNull { sheetRows -> findHeaderRow(sheetRows) != null }
                ?: error("未识别到浙江大学课表表头")
            return parseRows(rows, totalWeeks)
        }

        internal fun parseRows(rows: List<List<String>>, totalWeeks: Int): ParsedTimetable {
            val headerIndex = requireNotNull(findHeaderRow(rows)) {
                "未识别到浙江大学课表表头"
            }
            val headers = rows[headerIndex].mapIndexed { index, value -> value.trim() to index }.toMap()
            val title = rows.take(headerIndex)
                .flatten()
                .firstOrNull { "课表" in it }
                ?.trim()
            val warnings = mutableListOf<String>()
            val courses = rows.drop(headerIndex + 1).flatMap { row ->
                parseCourseRow(row, headers, totalWeeks, warnings)
            }.distinctBy { course ->
                listOf(
                    course.name,
                    course.teacher,
                    course.classroom,
                    course.weekDay,
                    course.startSection,
                    course.endSection,
                    course.activeWeeks
                )
            }.sortedWith(
                compareBy<Course> { WEEK_DAYS.indexOf(it.weekDay) }
                    .thenBy(Course::startSection)
                    .thenBy(Course::name)
            )

            require(courses.isNotEmpty()) { "没有识别到课程；请确认文件来自浙江大学选课系统" }
            return ParsedTimetable(
                title = title,
                semester = title?.let { Regex("\\d{4}-\\d{4}学年[^课表]*学期").find(it)?.value },
                courses = courses,
                warnings = warnings.distinct()
            )
        }

        private fun parseCourseRow(
            row: List<String>,
            headers: Map<String, Int>,
            totalWeeks: Int,
            warnings: MutableList<String>
        ): List<Course> {
            fun field(name: String): String = row.getOrNull(headers.getValue(name)).orEmpty().trim()

            val name = field("课程名称")
            if (name.isEmpty()) return emptyList()
            val timeParts = field("上课时间").split(Regex("[;；]")).map(String::trim).filter(String::isNotEmpty)
            val placeParts = field("上课地点").split(Regex("[;；]")).map(String::trim)
            val activeWeeks = activeWeeksForTerm(field("学期"), totalWeeks)
            if (activeWeeks == null) {
                warnings += "$name：无法识别学期“${field("学期")}”"
                return emptyList()
            }

            return timeParts.mapIndexedNotNull { index, time ->
                val match = TIME_PATTERN.matchEntire(time.replace(" ", ""))
                if (match == null) {
                    warnings += "$name：无法识别上课时间“$time”"
                    return@mapIndexedNotNull null
                }
                val sections = match.groupValues[2]
                    .split(Regex("[,，、]"))
                    .mapNotNull(String::toIntOrNull)
                if (sections.isEmpty() || sections.any { it !in 1..30 }) {
                    warnings += "$name：节次超出支持范围“$time”"
                    return@mapIndexedNotNull null
                }
                Course(
                    name = name,
                    teacher = field("教师姓名").ifEmpty { "无" },
                    classroom = (placeParts.getOrNull(index) ?: placeParts.singleOrNull())
                        .orEmpty()
                        .ifEmpty { "无" },
                    weekDay = "周${match.groupValues[1].replace('天', '日')}",
                    startSection = sections.min(),
                    endSection = sections.max(),
                    activeWeeks = activeWeeks
                )
            }
        }

        internal fun activeWeeksForTerm(term: String, totalWeeks: Int): Set<Int>? {
            val weeks = when (term.trim()) {
                "秋冬", "春夏" -> 1..16
                "秋", "春" -> 1..8
                "冬", "夏" -> 9..16
                else -> return null
            }
            return weeks.filterTo(linkedSetOf()) { it <= totalWeeks }
                .takeIf(Set<Int>::isNotEmpty)
        }

        private fun findHeaderRow(rows: List<List<String>>): Int? = rows.indexOfFirst { row ->
            row.map(String::trim).toSet().containsAll(REQUIRED_HEADERS)
        }.takeIf { it >= 0 }

        private fun readXlsxEntries(input: InputStream): Map<String, ByteArray> {
            val entries = linkedMapOf<String, ByteArray>()
            var totalBytes = 0
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!entry.isDirectory &&
                        (entry.name == SHARED_STRINGS_PATH || WORKSHEET_PATH.matches(entry.name))
                    ) {
                        val bytes = readLimited(zip, MAX_ENTRY_BYTES)
                        totalBytes += bytes.size
                        require(totalBytes <= MAX_WORKBOOK_BYTES) { "课表文件过大" }
                        entries[entry.name] = bytes
                    }
                    zip.closeEntry()
                }
            }
            return entries
        }

        private fun readLimited(input: InputStream, limit: Int): ByteArray {
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= limit) { "课表文件内容过大" }
                output.write(buffer, 0, count)
            }
            return output.toByteArray()
        }

        private fun parseSharedStrings(bytes: ByteArray): List<String> {
            val document = parseXml(bytes)
            return document.getElementsByTagNameNS("*", "si").asElements().map { item ->
                item.getElementsByTagNameNS("*", "t").asElements().joinToString("") { it.textContent }
            }
        }

        private fun parseRows(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
            val document = parseXml(bytes)
            return document.getElementsByTagNameNS("*", "row").asElements().map { row ->
                val values = sortedMapOf<Int, String>()
                row.getElementsByTagNameNS("*", "c").asElements().forEach { cell ->
                    val column = columnIndex(cell.getAttribute("r"))
                    val raw = cell.getElementsByTagNameNS("*", "v").item(0)?.textContent.orEmpty()
                    values[column] = when (cell.getAttribute("t")) {
                        "s" -> sharedStrings.getOrNull(raw.toIntOrNull() ?: -1).orEmpty()
                        "inlineStr" -> cell.getElementsByTagNameNS("*", "t").asElements()
                            .joinToString("") { it.textContent }
                        else -> raw
                    }
                }
                List((values.lastKeyOrNull() ?: -1) + 1) { values[it].orEmpty() }
            }
        }

        private fun parseXml(bytes: ByteArray) = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        }.newDocumentBuilder().parse(ByteArrayInputStream(bytes))

        private fun columnIndex(reference: String): Int = reference
            .takeWhile(Char::isLetter)
            .uppercase()
            .fold(0) { value, character -> value * 26 + (character - 'A' + 1) } - 1

        private fun worksheetNumber(path: String): Int = WORKSHEET_PATH.matchEntire(path)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
            ?: Int.MAX_VALUE

        private fun org.w3c.dom.NodeList.asElements(): List<Element> =
            (0 until length).mapNotNull { item(it) as? Element }

        private fun <K, V> SortedMap<K, V>.lastKeyOrNull(): K? = if (isEmpty()) null else lastKey()

        private val WEEK_DAYS = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    }
}

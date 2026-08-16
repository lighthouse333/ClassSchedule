package com.example.timetable.importer

import android.net.Uri

enum class TimetableImportSchool(
    val displayName: String,
    val importRequirement: String,
    val acceptedMimeTypes: Array<String>
) {
    BEIJING_UNIVERSITY_OF_CHEMICAL_TECHNOLOGY(
        displayName = "北京化工大学",
        importRequirement = "PDF｜教务系统导出的文字版课表，不支持扫描件",
        acceptedMimeTypes = arrayOf("application/pdf")
    ),
    NORTHEAST_NORMAL_UNIVERSITY(
        displayName = "东北师范大学",
        importRequirement = "PDF｜教务系统导出的文字版学生课表，不支持扫描件",
        acceptedMimeTypes = arrayOf("application/pdf")
    ),
    ZHEJIANG_UNIVERSITY(
        displayName = "浙江大学",
        importRequirement = "XLSX｜选课系统导出的课表 Excel 文件",
        acceptedMimeTypes = arrayOf(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    )
}

interface TimetableFileParser {
    fun parse(uri: Uri, totalWeeks: Int): ParsedTimetable
}

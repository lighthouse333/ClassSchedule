package com.example.timetable.importer

import android.net.Uri

enum class TimetableImportSchool(
    val displayName: String,
    val acceptedMimeTypes: Array<String>
) {
    BEIJING_UNIVERSITY_OF_CHEMICAL_TECHNOLOGY(
        displayName = "北京化工大学",
        acceptedMimeTypes = arrayOf("application/pdf")
    ),
    NORTHEAST_NORMAL_UNIVERSITY(
        displayName = "东北师范大学",
        acceptedMimeTypes = arrayOf("application/pdf")
    )
}

interface TimetableFileParser {
    fun parse(uri: Uri, totalWeeks: Int): ParsedTimetable
}

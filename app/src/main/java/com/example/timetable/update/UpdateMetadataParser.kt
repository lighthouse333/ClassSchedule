package com.example.timetable.update

private val VERSION_CODE_REGEX = Regex(
    pattern = "versionCode\\s*(?:[：:=]\\s*)?(\\d+)",
    option = RegexOption.IGNORE_CASE
)

internal fun parseReleaseVersionCode(releaseNotes: String): Long? =
    VERSION_CODE_REGEX.find(releaseNotes)?.groupValues?.get(1)?.toLongOrNull()

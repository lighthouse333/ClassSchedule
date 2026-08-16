package com.example.timetable.update

private val VERSION_CODE_REGEX = Regex(
    pattern = "versionCode\\s*(?:[：:=]\\s*)?(\\d+)",
    option = RegexOption.IGNORE_CASE
)

internal fun parseReleaseVersionCode(releaseNotes: String): Long? =
    VERSION_CODE_REGEX.find(releaseNotes)?.groupValues?.get(1)?.toLongOrNull()

internal fun shouldShowUpdatePrompt(
    remindersEnabled: Boolean,
    availableVersionCode: Long,
    dismissedVersionCode: Long,
    dismissedAt: Long,
    now: Long,
    snoozeMillis: Long
): Boolean = remindersEnabled && (
    dismissedVersionCode != availableVersionCode || now - dismissedAt >= snoozeMillis
    )

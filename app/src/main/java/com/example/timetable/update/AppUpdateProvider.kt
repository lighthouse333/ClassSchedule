package com.example.timetable.update

import java.io.File

data class AppUpdateInfo(
    val versionCode: Long,
    val versionName: String,
    val releaseNotes: String,
    val releaseUrl: String,
    val apkUrl: String,
    val apkName: String,
    val apkSize: Long,
    val sha256: String
)

sealed interface UpdateCheckResult {
    data class Available(val info: AppUpdateInfo) : UpdateCheckResult
    data class UpToDate(val checkedAt: Long) : UpdateCheckResult
}

interface AppUpdateProvider {
    suspend fun checkForUpdate(): UpdateCheckResult
    suspend fun downloadUpdate(info: AppUpdateInfo, onProgress: (Int) -> Unit): File
    fun requestInstall(apk: File): Boolean
}

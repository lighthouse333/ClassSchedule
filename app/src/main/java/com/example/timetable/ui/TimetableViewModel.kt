package com.example.timetable.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timetable.data.AppDatabase
import com.example.timetable.data.CourseRepository
import com.example.timetable.data.ScheduleSettingsRepository
import com.example.timetable.importer.BuctPdfTimetableParser
import com.example.timetable.importer.ParsedTimetable
import com.example.timetable.importer.TimetableFileParser
import com.example.timetable.importer.TimetableImportSchool
import com.example.timetable.model.Course
import com.example.timetable.model.ScheduleSettings
import com.example.timetable.model.ClassPeriod
import com.example.timetable.model.createDefaultScheduleSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface PdfImportState {
    data object Idle : PdfImportState
    data object Loading : PdfImportState
    data object Saving : PdfImportState
    data class Success(val timetable: ParsedTimetable) : PdfImportState
    data class Completed(val importedCount: Int) : PdfImportState
    data class Error(val message: String) : PdfImportState
}

class TimetableViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CourseRepository(
        AppDatabase.getInstance(application).courseDao()
    )
    private val settingsRepository = ScheduleSettingsRepository(application)
    private val timetableParsers: Map<TimetableImportSchool, TimetableFileParser> = mapOf(
        TimetableImportSchool.BEIJING_UNIVERSITY_OF_CHEMICAL_TECHNOLOGY to
            BuctPdfTimetableParser(application)
    )
    private val _pdfImportState = MutableStateFlow<PdfImportState>(PdfImportState.Idle)
    val pdfImportState: StateFlow<PdfImportState> = _pdfImportState.asStateFlow()

    val courses = repository.courses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val settings = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = createDefaultScheduleSettings()
    )

    fun addCourse(course: Course) {
        viewModelScope.launch {
            repository.add(course)
        }
    }

    fun updateCourse(course: Course) {
        viewModelScope.launch {
            repository.update(course)
        }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch {
            repository.delete(course)
        }
    }

    fun saveSettings(settings: ScheduleSettings) {
        viewModelScope.launch {
            settingsRepository.save(settings)
        }
    }

    fun parseTimetableFile(uri: Uri, school: TimetableImportSchool) {
        viewModelScope.launch {
            _pdfImportState.value = PdfImportState.Loading
            _pdfImportState.value = try {
                val parsed = withContext(Dispatchers.IO) {
                    requireNotNull(timetableParsers[school]) {
                        "暂不支持${school.displayName}的课表格式"
                    }.parse(uri, MAX_IMPORT_WEEKS)
                }
                PdfImportState.Success(parsed)
            } catch (error: Exception) {
                PdfImportState.Error(error.message ?: "PDF 解析失败")
            }
        }
    }

    fun dismissPdfImportResult() {
        _pdfImportState.value = PdfImportState.Idle
    }

    fun importCourses(importedCourses: List<Course>) {
        if (importedCourses.isEmpty()) return
        viewModelScope.launch {
            _pdfImportState.value = PdfImportState.Saving
            _pdfImportState.value = try {
                val current = settings.value
                val requiredSectionCount = maxOf(
                    current.sectionCount,
                    importedCourses.maxOf(Course::endSection)
                )
                val requiredTotalWeeks = maxOf(
                    current.totalWeeks,
                    importedCourses.maxOf(Course::endWeek)
                )
                val expandedPeriods = current.classPeriods.toMutableList()
                while (expandedPeriods.size < requiredSectionCount) {
                    val previousEnd = expandedPeriods.lastOrNull()?.endMinutes ?: (8 * 60 - 10)
                    val number = expandedPeriods.size + 1
                    expandedPeriods += ClassPeriod(
                        number = number,
                        startMinutes = previousEnd + 10,
                        endMinutes = previousEnd + 55
                    )
                }
                if (
                    requiredSectionCount != current.sectionCount ||
                    requiredTotalWeeks != current.totalWeeks
                ) {
                    settingsRepository.save(
                        current.copy(
                            sectionCount = requiredSectionCount,
                            totalWeeks = requiredTotalWeeks,
                            classPeriods = expandedPeriods
                        )
                    )
                }
                repository.addAll(importedCourses)
                PdfImportState.Completed(importedCourses.size)
            } catch (error: Exception) {
                PdfImportState.Error(error.message ?: "课程导入失败")
            }
        }
    }

    companion object {
        private const val MAX_IMPORT_WEEKS = 30
    }
}

package com.example.timetable.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.timetable.model.ClassPeriod
import com.example.timetable.model.ScheduleSettings
import com.example.timetable.model.createDefaultScheduleSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalDate

private val Context.scheduleSettingsDataStore by preferencesDataStore(
    name = "schedule_settings"
)

class ScheduleSettingsRepository(
    private val context: Context
) {
    private object Keys {
        val semesterStart = stringPreferencesKey("semester_start")
        val totalWeeks = intPreferencesKey("total_weeks")
        val sectionCount = intPreferencesKey("section_count")
        val classPeriods = stringPreferencesKey("class_periods")
    }

    val settings: Flow<ScheduleSettings> = context.scheduleSettingsDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            val defaults = createDefaultScheduleSettings()
            val sectionCount = preferences[Keys.sectionCount]
                ?.coerceIn(1, 12)
                ?: defaults.sectionCount
            val periods = decodePeriods(preferences[Keys.classPeriods])
                .takeIf { it.size == sectionCount }
                ?: defaults.classPeriods.takeIf { it.size == sectionCount }
                ?: com.example.timetable.model.createDefaultPeriods(sectionCount)

            ScheduleSettings(
                semesterStart = preferences[Keys.semesterStart]
                    ?.let { savedDate ->
                        runCatching { LocalDate.parse(savedDate) }.getOrNull()
                    }
                    ?: defaults.semesterStart,
                totalWeeks = preferences[Keys.totalWeeks]
                    ?.coerceIn(1, 30)
                    ?: defaults.totalWeeks,
                sectionCount = sectionCount,
                classPeriods = periods
            )
        }

    suspend fun save(settings: ScheduleSettings) {
        context.scheduleSettingsDataStore.edit { preferences ->
            preferences[Keys.semesterStart] = settings.semesterStart.toString()
            preferences[Keys.totalWeeks] = settings.totalWeeks
            preferences[Keys.sectionCount] = settings.sectionCount
            preferences[Keys.classPeriods] = encodePeriods(settings.classPeriods)
        }
    }

    private fun encodePeriods(periods: List<ClassPeriod>): String =
        periods.joinToString(";") { period ->
            "${period.number},${period.startMinutes},${period.endMinutes}"
        }

    private fun decodePeriods(value: String?): List<ClassPeriod> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(";").mapNotNull { encodedPeriod ->
            val parts = encodedPeriod.split(",")
            if (parts.size != 3) return@mapNotNull null
            val number = parts[0].toIntOrNull() ?: return@mapNotNull null
            val start = parts[1].toIntOrNull() ?: return@mapNotNull null
            val end = parts[2].toIntOrNull() ?: return@mapNotNull null
            ClassPeriod(number, start, end)
        }.takeIf { periods ->
            periods.indices.all { index ->
                periods[index].number == index + 1 &&
                    periods[index].startMinutes < periods[index].endMinutes &&
                    (index == 0 || periods[index].startMinutes >= periods[index - 1].endMinutes)
            }
        }.orEmpty()
    }
}

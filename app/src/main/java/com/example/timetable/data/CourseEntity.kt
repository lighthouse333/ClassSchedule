package com.example.timetable.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.timetable.model.Course
import com.example.timetable.model.WeekType

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val teacher: String,
    val classroom: String,
    val weekDay: String,
    val startSection: Int,
    val endSection: Int,
    val startWeek: Int,
    val endWeek: Int,
    val weekType: String,
    val activeWeeks: String
)

fun CourseEntity.toDomain(): Course {
    val parsedActiveWeeks = activeWeeks
        .split(',')
        .mapNotNull(String::toIntOrNull)
        .filterTo(sortedSetOf()) { it >= 1 }
        .ifEmpty {
            com.example.timetable.model.createActiveWeeks(
                startWeek = startWeek,
                endWeek = endWeek,
                weekType = WeekType.valueOf(weekType)
            )
        }

    return Course(
    id = id,
    name = name,
    teacher = teacher,
    classroom = classroom,
    weekDay = weekDay,
    startSection = startSection,
    endSection = endSection,
    activeWeeks = parsedActiveWeeks
    )
}

fun Course.toEntity(): CourseEntity = CourseEntity(
    id = id,
    name = name,
    teacher = teacher,
    classroom = classroom,
    weekDay = weekDay,
    startSection = startSection,
    endSection = endSection,
    startWeek = startWeek,
    endWeek = endWeek,
    weekType = WeekType.EVERY_WEEK.name,
    activeWeeks = activeWeeks.sorted().joinToString(",")
)

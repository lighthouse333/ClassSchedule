package com.example.timetable.data

import com.example.timetable.model.Course
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CourseRepository(
    private val courseDao: CourseDao
) {
    fun courses(timetableId: Long): Flow<List<Course>> =
        courseDao.observeForTimetable(timetableId).map { entities ->
        entities.map(CourseEntity::toDomain)
    }

    suspend fun isEmpty(): Boolean = courseDao.count() == 0

    suspend fun add(timetableId: Long, course: Course): Long =
        courseDao.insert(course.copy(id = 0).toEntity(timetableId))

    suspend fun addAll(timetableId: Long, courses: List<Course>): List<Long> =
        courseDao.insertAll(courses.map { it.copy(id = 0).toEntity(timetableId) })

    suspend fun update(timetableId: Long, course: Course) {
        require(course.id != 0L) { "更新课程前必须拥有数据库 ID" }
        courseDao.update(course.toEntity(timetableId))
    }

    suspend fun delete(timetableId: Long, course: Course) {
        require(course.id != 0L) { "删除课程前必须拥有数据库 ID" }
        courseDao.delete(course.toEntity(timetableId))
    }
}

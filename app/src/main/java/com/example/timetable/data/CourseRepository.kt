package com.example.timetable.data

import com.example.timetable.model.Course
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CourseRepository(
    private val courseDao: CourseDao
) {
    val courses: Flow<List<Course>> = courseDao.observeAll().map { entities ->
        entities.map(CourseEntity::toDomain)
    }

    suspend fun isEmpty(): Boolean = courseDao.count() == 0

    suspend fun add(course: Course): Long =
        courseDao.insert(course.copy(id = 0).toEntity())

    suspend fun addAll(courses: List<Course>): List<Long> =
        courseDao.insertAll(courses.map { it.copy(id = 0).toEntity() })

    suspend fun update(course: Course) {
        require(course.id != 0L) { "更新课程前必须拥有数据库 ID" }
        courseDao.update(course.toEntity())
    }

    suspend fun delete(course: Course) {
        require(course.id != 0L) { "删除课程前必须拥有数据库 ID" }
        courseDao.delete(course.toEntity())
    }
}

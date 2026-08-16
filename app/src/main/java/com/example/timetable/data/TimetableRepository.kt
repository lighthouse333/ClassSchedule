package com.example.timetable.data

class TimetableRepository(private val timetableDao: TimetableDao) {
    val timetables = timetableDao.observeAll()

    suspend fun ensureDefaultTimetable() {
        if (timetableDao.count() == 0) {
            timetableDao.insert(TimetableEntity(name = "我的课表"))
        }
    }

    suspend fun create(name: String): Long = timetableDao.insert(
        TimetableEntity(name = name.trim())
    )

    suspend fun rename(timetableId: Long, name: String) {
        timetableDao.rename(timetableId, name.trim())
    }

    suspend fun delete(timetableId: Long) {
        timetableDao.deleteWithCourses(timetableId)
    }
}

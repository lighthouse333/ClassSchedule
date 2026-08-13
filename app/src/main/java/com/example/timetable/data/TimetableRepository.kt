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
}

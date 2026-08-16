package com.example.timetable.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetables ORDER BY id")
    fun observeAll(): Flow<List<TimetableEntity>>

    @Query("SELECT COUNT(*) FROM timetables")
    suspend fun count(): Int

    @Insert
    suspend fun insert(timetable: TimetableEntity): Long

    @Query("UPDATE timetables SET name = :name WHERE id = :timetableId")
    suspend fun rename(timetableId: Long, name: String)

    @Query("DELETE FROM courses WHERE timetableId = :timetableId")
    suspend fun deleteCourses(timetableId: Long)

    @Query("DELETE FROM timetables WHERE id = :timetableId")
    suspend fun deleteById(timetableId: Long)

    @Transaction
    suspend fun deleteWithCourses(timetableId: Long) {
        deleteCourses(timetableId)
        deleteById(timetableId)
    }
}

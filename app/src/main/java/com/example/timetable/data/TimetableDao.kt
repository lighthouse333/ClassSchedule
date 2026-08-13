package com.example.timetable.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetables ORDER BY id")
    fun observeAll(): Flow<List<TimetableEntity>>

    @Query("SELECT COUNT(*) FROM timetables")
    suspend fun count(): Int

    @Insert
    suspend fun insert(timetable: TimetableEntity): Long
}

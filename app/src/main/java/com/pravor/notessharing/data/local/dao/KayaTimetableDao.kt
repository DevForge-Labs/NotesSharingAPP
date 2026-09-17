package com.pravor.notessharing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.pravor.notessharing.data.local.entity.KayaTimetableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KayaTimetableDao {

    @Query("SELECT * FROM kaya_timetable_entries WHERE (:userId = 'anonymous' OR userId = :userId) ORDER BY slotOrder ASC, time ASC")
    fun observeTimetable(userId: String): Flow<List<KayaTimetableEntity>>

    @Query("SELECT * FROM kaya_timetable_entries WHERE (:userId = 'anonymous' OR userId = :userId) AND LOWER(day) = LOWER(:day) ORDER BY slotOrder ASC, time ASC")
    fun observeTimetableByDay(userId: String, day: String): Flow<List<KayaTimetableEntity>>

    @Query("SELECT * FROM kaya_timetable_entries WHERE (:userId = 'anonymous' OR userId = :userId) ORDER BY slotOrder ASC, time ASC")
    suspend fun getTimetableForUser(userId: String): List<KayaTimetableEntity>

    @Query("SELECT * FROM kaya_timetable_entries ORDER BY slotOrder ASC, time ASC")
    suspend fun getAllCachedTimetable(): List<KayaTimetableEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<KayaTimetableEntity>): List<Long>

    @Query("DELETE FROM kaya_timetable_entries WHERE userId = :userId")
    suspend fun deleteForUser(userId: String): Int

    @Query("DELETE FROM kaya_timetable_entries")
    suspend fun clearAll(): Int
}

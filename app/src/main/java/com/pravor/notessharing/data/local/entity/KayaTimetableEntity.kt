package com.pravor.notessharing.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "kaya_timetable_entries",
    primaryKeys = ["userId", "entryId"]
)
data class KayaTimetableEntity(
    val userId: String,
    val entryId: String,
    val courseCode: String,
    val courseName: String,
    val section: String,
    val sectionShort: String,
    val faculty: String,
    val facultyCode: String,
    val day: String,
    val period: String,
    val time: String,
    val room: String,
    val fullRoom: String,
    val type: String,
    val slotOrder: Int = 0,
    val lastSyncedAt: Long = System.currentTimeMillis()
)

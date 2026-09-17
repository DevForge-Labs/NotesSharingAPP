package com.pravor.notessharing.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class KayaDatabaseMigration12To13 : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `kaya_timetable_entries` (
                `userId` TEXT NOT NULL,
                `entryId` TEXT NOT NULL,
                `courseCode` TEXT NOT NULL,
                `courseName` TEXT NOT NULL,
                `section` TEXT NOT NULL,
                `sectionShort` TEXT NOT NULL,
                `faculty` TEXT NOT NULL,
                `facultyCode` TEXT NOT NULL,
                `day` TEXT NOT NULL,
                `period` TEXT NOT NULL,
                `time` TEXT NOT NULL,
                `room` TEXT NOT NULL,
                `fullRoom` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `slotOrder` INTEGER NOT NULL DEFAULT 0,
                `lastSyncedAt` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`userId`, `entryId`)
            )
            """.trimIndent()
        )
    }
}

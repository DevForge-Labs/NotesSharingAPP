package com.pravor.notessharing.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class ClassroomDatabaseMigration : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `classroom_courses` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `courseId` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `classroomAccount` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `section` TEXT,
                `descriptionHeading` TEXT,
                `description` TEXT,
                `room` TEXT,
                `enrollmentCode` TEXT,
                `alternateLink` TEXT,
                `state` TEXT NOT NULL,
                `lastSyncedAt` INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `classroom_materials` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `materialId` TEXT NOT NULL,
                `courseId` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT,
                `creationTime` TEXT,
                `updateTime` TEXT,
                `alternateLink` TEXT,
                `lastSyncedAt` INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `classroom_announcements` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `announcementId` TEXT NOT NULL,
                `courseId` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `text` TEXT NOT NULL,
                `creationTime` TEXT,
                `updateTime` TEXT,
                `alternateLink` TEXT,
                `lastSyncedAt` INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `classroom_coursework` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `courseWorkId` TEXT NOT NULL,
                `courseId` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT,
                `dueFormatted` TEXT,
                `creationTime` TEXT,
                `alternateLink` TEXT,
                `lastSyncedAt` INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `classroom_attachments` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `parentId` TEXT NOT NULL,
                `parentType` TEXT NOT NULL,
                `courseId` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `linkUrl` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `driveFileId` TEXT,
                `thumbnailUrl` TEXT,
                `localCachedPath` TEXT
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `classroom_cached_files` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `driveFileId` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `classroomAccount` TEXT NOT NULL,
                `fileName` TEXT NOT NULL,
                `mimeType` TEXT NOT NULL,
                `localPath` TEXT NOT NULL,
                `fileSize` INTEGER NOT NULL,
                `lastDownloadedAt` INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

class ClassroomDatabaseMigration4To5 : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `classroom_hidden_courses` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `userId` TEXT NOT NULL,
                `classroomAccount` TEXT NOT NULL,
                `courseId` TEXT NOT NULL,
                `hiddenAt` INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

class ClassroomDatabaseMigration5To6 : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val existingColumns = mutableSetOf<String>()
        db.query("PRAGMA table_info(`classroom_courses`)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (nameIndex != -1) {
                    existingColumns.add(cursor.getString(nameIndex))
                }
            }
        }

        if (!existingColumns.contains("teacherId")) {
            db.execSQL("ALTER TABLE `classroom_courses` ADD COLUMN `teacherId` TEXT")
        }
        if (!existingColumns.contains("teacherName")) {
            db.execSQL("ALTER TABLE `classroom_courses` ADD COLUMN `teacherName` TEXT")
        }
        if (!existingColumns.contains("teacherPhotoUrl")) {
            db.execSQL("ALTER TABLE `classroom_courses` ADD COLUMN `teacherPhotoUrl` TEXT")
        }
    }
}

class ClassroomDatabaseMigration6To7 : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `classroom_submissions` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `submissionId` TEXT NOT NULL,
                `courseId` TEXT NOT NULL,
                `courseWorkId` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `state` TEXT NOT NULL,
                `late` INTEGER NOT NULL DEFAULT 0,
                `assignedGrade` REAL,
                `alternateLink` TEXT,
                `lastSyncedAt` INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

class ClassroomDatabaseMigration7To8 : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val existingColumns = mutableSetOf<String>()
        db.query("PRAGMA table_info(`classroom_coursework`)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (nameIndex != -1) {
                    existingColumns.add(cursor.getString(nameIndex))
                }
            }
        }

        if (!existingColumns.contains("associatedWithDeveloper")) {
            db.execSQL("ALTER TABLE `classroom_coursework` ADD COLUMN `associatedWithDeveloper` INTEGER NOT NULL DEFAULT 0")
        }
    }
}

class ClassroomDatabaseMigration8To9 : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `classroom_manual_completions` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `userId` TEXT NOT NULL,
                `courseId` TEXT NOT NULL,
                `courseWorkId` TEXT NOT NULL,
                `completed` INTEGER NOT NULL DEFAULT 1,
                `completedAt` INTEGER NOT NULL,
                `completionSource` TEXT NOT NULL
            )
        """.trimIndent())
    }
}

class ExploreDatabaseMigration9To10 : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val existingColumns = mutableSetOf<String>()
        db.query("PRAGMA table_info(`explore_items`)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (nameIndex != -1) {
                    existingColumns.add(cursor.getString(nameIndex))
                }
            }
        }

        if (!existingColumns.contains("semester")) {
            db.execSQL("ALTER TABLE `explore_items` ADD COLUMN `semester` TEXT")
        }
    }
}


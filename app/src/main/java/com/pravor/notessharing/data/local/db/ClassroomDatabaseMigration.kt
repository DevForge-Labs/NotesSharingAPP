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
                `teacherId` TEXT,
                `teacherName` TEXT,
                `teacherPhotoUrl` TEXT,
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
        db.execSQL("ALTER TABLE `classroom_courses` ADD COLUMN `teacherId` TEXT")
        db.execSQL("ALTER TABLE `classroom_courses` ADD COLUMN `teacherName` TEXT")
        db.execSQL("ALTER TABLE `classroom_courses` ADD COLUMN `teacherPhotoUrl` TEXT")
    }
}

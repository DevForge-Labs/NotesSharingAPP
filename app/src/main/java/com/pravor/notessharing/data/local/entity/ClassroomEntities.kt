package com.pravor.notessharing.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "classroom_courses")
data class ClassroomCourseEntity(
    @PrimaryKey val id: String,
    val courseId: String,
    val userId: String,
    val classroomAccount: String,
    val name: String,
    val section: String?,
    val descriptionHeading: String?,
    val description: String?,
    val room: String?,
    val enrollmentCode: String?,
    val alternateLink: String?,
    val state: String,
    val teacherId: String? = null,
    val teacherName: String? = null,
    val teacherPhotoUrl: String? = null,
    val lastSyncedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "classroom_materials")
data class ClassroomMaterialEntity(
    @PrimaryKey val id: String,
    val materialId: String,
    val courseId: String,
    val userId: String,
    val title: String,
    val description: String?,
    val creationTime: String?,
    val updateTime: String?,
    val alternateLink: String?,
    val lastSyncedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "classroom_announcements")
data class ClassroomAnnouncementEntity(
    @PrimaryKey val id: String,
    val announcementId: String,
    val courseId: String,
    val userId: String,
    val text: String,
    val creationTime: String?,
    val updateTime: String?,
    val alternateLink: String?,
    val lastSyncedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "classroom_coursework")
data class ClassroomCourseWorkEntity(
    @PrimaryKey val id: String,
    val courseWorkId: String,
    val courseId: String,
    val userId: String,
    val title: String,
    val description: String?,
    val dueFormatted: String?,
    val creationTime: String?,
    val alternateLink: String?,
    val lastSyncedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "classroom_attachments")
data class ClassroomAttachmentEntity(
    @PrimaryKey val id: String,
    val parentId: String,
    val parentType: String,
    val courseId: String,
    val userId: String,
    val title: String,
    val linkUrl: String,
    val type: String,
    val driveFileId: String?,
    val thumbnailUrl: String?,
    val localCachedPath: String? = null
)

@Entity(tableName = "classroom_cached_files")
data class ClassroomFileEntity(
    @PrimaryKey val id: String,
    val driveFileId: String,
    val userId: String,
    val classroomAccount: String,
    val fileName: String,
    val mimeType: String,
    val localPath: String,
    val fileSize: Long,
    val lastDownloadedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "classroom_hidden_courses")
data class ClassroomHiddenCourseEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val classroomAccount: String,
    val courseId: String,
    val hiddenAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "classroom_submissions")
data class ClassroomSubmissionEntity(
    @PrimaryKey val id: String,
    val submissionId: String,
    val courseId: String,
    val courseWorkId: String,
    val userId: String,
    val state: String,
    val late: Boolean = false,
    val assignedGrade: Double? = null,
    val alternateLink: String? = null,
    val lastSyncedAt: Long = System.currentTimeMillis()
)

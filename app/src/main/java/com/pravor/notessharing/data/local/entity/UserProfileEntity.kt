package com.pravor.notessharing.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val uid: String,
    val name: String,
    val email: String,
    val semester: String,
    val college: String,
    val section: String,
    val profileImageUrl: String,
    val role: String,
    val accountStatus: String,
    val totalUploads: Int,
    val bookmarks: Int,
    val upvotes: Int,
    val contributorLevel: Int,
    val branch: String,
    val createdAt: Long,
    val pyqUploads: Int,
    val notesUploads: Int,
    val assignmentUploads: Int,
    val cheatSheetUploads: Int,
    val youtubeResourceUploads: Int,
    val lastUpdatedMs: Long = System.currentTimeMillis()
)

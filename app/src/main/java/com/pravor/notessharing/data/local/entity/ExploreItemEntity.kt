package com.pravor.notessharing.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "explore_items",
    indices = [
        Index(value = ["collegeId"]),
        Index(value = ["collegeId", "sectionCategory"]),
        Index(value = ["collegeId", "subjectId"])
    ]
)
data class ExploreItemEntity(
    @PrimaryKey val compositeId: String, // e.g. "$sectionCategory-$id"
    val id: String,
    val collegeId: String,
    val sectionCategory: String, // POPULAR, NOTES, EXAM_PREP, ASSIGNMENTS, VIDEOS, DISCOVER
    val title: String,
    val description: String,
    val uploaderName: String,
    val uploaderInitials: String,
    val uploadDate: String,
    val subject: String,
    val subjectId: String,
    val fileTypeName: String,
    val documentType: String?,
    val type: String?,
    val thumbnailUrl: String?,
    val youtubeVideoId: String?,
    val youtubeUrl: String?,
    val youtubeThumbnailUrl: String?,
    val thumbnailGenerated: Boolean?,
    val thumbnailType: String?,
    val tagsJson: String,
    val thumbnailUrlsJson: String,
    val upvotes: Int,
    val commentsCount: Int,
    val downloadsCount: Int,
    val bookmarksCount: Int,
    val section: String?,
    val sectionDisplay: String?,
    val examYear: String?,
    val examType: String?,
    val semester: String? = null,
    val cachedAtMs: Long = System.currentTimeMillis(),
    val uploadedAtMs: Long = 0L
)

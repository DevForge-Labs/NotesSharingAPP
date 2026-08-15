package com.pravor.notessharing.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "home_feed_items")
data class HomeFeedItemEntity(
    @PrimaryKey val id: String,
    val uploaderName: String,
    val uploaderInitials: String,
    val uploadDate: String,
    val title: String,
    val description: String,
    val tagsJson: String,
    val fileTypeName: String,
    val upvotes: Int,
    val comments: Int,
    val downloadsCount: Int,
    val isSaved: Boolean,
    val bookmarksCount: Int,
    val youtubeVideoId: String?,
    val youtubeUrl: String?,
    val thumbnailUrl: String?,
    val youtubeThumbnailUrl: String?,
    val thumbnailGenerated: Boolean?,
    val thumbnailType: String?,
    val thumbnailUrlsJson: String,
    val documentType: String?,
    val type: String?,
    val subject: String?,
    val examYear: String?,
    val examType: String?,
    val section: String?,
    val sectionDisplay: String?,
    val collegeId: String,
    val cachedAtMs: Long = System.currentTimeMillis(),
    val uploadedAtMs: Long = 0L
)

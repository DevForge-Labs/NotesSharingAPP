package com.pravor.notessharing.model

import androidx.compose.runtime.Immutable

@Immutable
data class VideoDetail(
    val id: String,
    val title: String,
    val description: String,
    val branch: String,
    val semester: String,
    val subject: String,
    val uploaderId: String,
    val uploaderName: String,
    val uploaderPhotoUrl: String,
    val uploadedAt: Long,
    val youtubeUrl: String,
    val youtubeVideoId: String,
    val upvotes: Int,
    val downloads: Int,
    val bookmarks: Int,
    val thumbnailUrl: String? = null,
    val youtubeThumbnailUrl: String? = null,
    val youtubeResourceType: String = "video",
    val youtubePlaylistId: String = ""
)

fun Map<String, Any>.toVideoDetail(id: String): VideoDetail {
    val youtubeUrlStr = this["youtubeUrl"] as? String ?: ""
    val extractedId = extractYoutubeVideoId(youtubeUrlStr) ?: (this["youtubeVideoId"] as? String ?: "")
    val youtubeResourceType = this["youtubeResourceType"] as? String ?: "video"
    val youtubePlaylistId = this["youtubePlaylistId"] as? String ?: this["youtubeId"] as? String ?: ""
    
    return VideoDetail(
        id = id,
        title = this["title"] as? String ?: this["videoTitle"] as? String ?: "Untitled Video",
        description = this["description"] as? String ?: "",
        branch = this["branch"] as? String ?: "",
        semester = this["semester"] as? String ?: "",
        subject = this["subject"] as? String ?: "",
        uploaderId = this["uploaderId"] as? String ?: "",
        uploaderName = this["uploaderName"] as? String ?: "Anonymous",
        uploaderPhotoUrl = this["uploaderPhotoUrl"] as? String ?: "",
        uploadedAt = (this["uploadedAt"] as? Long) ?: (this["uploadTimestamp"] as? Long) ?: 0L,
        youtubeUrl = youtubeUrlStr,
        youtubeVideoId = extractedId,
        upvotes = (this["upvotes"] as? Long)?.toInt() ?: (this["likesCount"] as? Long)?.toInt() ?: 0,
        downloads = (this["downloads"] as? Long)?.toInt() ?: (this["downloadsCount"] as? Long)?.toInt() ?: 0,
        bookmarks = (this["bookmarks"] as? Long)?.toInt() ?: 0,
        thumbnailUrl = this["thumbnailUrl"] as? String,
        youtubeThumbnailUrl = this["youtubeThumbnailUrl"] as? String,
        youtubeResourceType = youtubeResourceType,
        youtubePlaylistId = youtubePlaylistId
    )
}

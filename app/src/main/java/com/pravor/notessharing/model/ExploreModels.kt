package com.pravor.notessharing.model

import androidx.compose.runtime.Immutable

enum class ResourceType {
    NOTE,
    PYQ,
    CHEAT_SHEET,
    ASSIGNMENT,
    VIDEO,
    PLAYLIST
}

@Immutable
data class TrendingNote(
    val id: String,
    val title: String,
    val subject: String,
    val downloadsCount: Int,
    val rating: Double,
    val upvotes: Int,
    val isBookmarked: Boolean,
    val thumbnailUrl: String? = null,
    val thumbnailGenerated: Boolean? = null,
    val thumbnailType: String? = null,
    val description: String = "",
    val uploaderName: String = "",
    val uploaderPhotoUrl: String = "",
    val contributorLevel: String = "",
    val documentType: String = "",
    val type: String? = null,
    val bookmarks: Int = 0,
    val examYear: String? = null,
    val examType: String? = null,
    val semester: String = "",
    val isUpvoted: Boolean = false,
    val branch: String = "",
    val trendingScore: Double = 0.0,
    val displaySubject: String? = null,
    val sectionDisplay: String? = null,
    val uploadedAt: Long = 0L,
    val resourceType: ResourceType = ResourceType.NOTE,
    val channelName: String = "",
    val duration: String = "",
    val youtubeVideoId: String = "",
    val youtubeThumbnailUrl: String? = null,
    val youtubeUrl: String = ""
) {
    fun isTrendingNote(): Boolean {
        return resourceType == ResourceType.NOTE
    }

    fun toVideoRecommendation(): VideoRecommendation {
        return VideoRecommendation(
            id = id,
            title = title,
            channelName = channelName.ifBlank { uploaderName },
            duration = duration,
            subject = subject,
            youtubeVideoId = youtubeVideoId,
            upvotes = upvotes,
            bookmarks = bookmarks,
            thumbnailUrl = thumbnailUrl,
            youtubeThumbnailUrl = youtubeThumbnailUrl,
            documentType = documentType,
            semester = semester.ifBlank { "Semester 4" },
            youtubeUrl = youtubeUrl,
            isUpvoted = isUpvoted,
            isBookmarked = isBookmarked
        )
    }
}

@Immutable
data class VideoRecommendation(
    val id: String,
    val title: String,
    val channelName: String,
    val duration: String,
    val subject: String,
    val youtubeVideoId: String = "",
    val upvotes: Int = 0,
    val bookmarks: Int = 0,
    val thumbnailUrl: String? = null,
    val youtubeThumbnailUrl: String? = null,
    val documentType: String = "",
    val semester: String = "Semester 4",
    val youtubeUrl: String = "",
    val isUpvoted: Boolean = false,
    val isBookmarked: Boolean = false
) {
    fun toTrendingNote(): TrendingNote {
        return TrendingNote(
            id = id,
            title = title,
            subject = subject,
            downloadsCount = 0,
            rating = 4.5,
            upvotes = upvotes,
            isBookmarked = isBookmarked,
            thumbnailUrl = thumbnailUrl,
            thumbnailGenerated = null,
            thumbnailType = null,
            description = "",
            uploaderName = channelName,
            uploaderPhotoUrl = "",
            contributorLevel = "Bronze Contributor",
            documentType = documentType,
            type = documentType,
            bookmarks = bookmarks,
            examYear = null,
            examType = null,
            semester = semester,
            isUpvoted = isUpvoted,
            branch = "",
            trendingScore = 0.0,
            displaySubject = null,
            sectionDisplay = null,
            uploadedAt = 0L,
            resourceType = ResourceType.VIDEO,
            channelName = channelName,
            duration = duration,
            youtubeVideoId = youtubeVideoId,
            youtubeThumbnailUrl = youtubeThumbnailUrl,
            youtubeUrl = youtubeUrl
        )
    }
}

@Immutable
data class StudyCollection(
    val id: String,
    val title: String,
    val notes: Int,
    val pyqs: Int,
    val playlists: Int,
    val cheatSheets: Int
)

@Immutable
data class Contributor(
    val id: String,
    val name: String,
    val initials: String,
    val totalUploads: Int,
    val rating: Double
)

@Immutable
data class RevisionCard(
    val id: String,
    val title: String,
    val points: List<String>
)

@Immutable
sealed interface DiscoverFeedItem {
    val id: String

    @Immutable
    data class Note(
        override val id: String,
        val title: String,
        val subject: String,
        val downloadsCount: Int
    ) : DiscoverFeedItem

    @Immutable
    data class Video(
        override val id: String,
        val title: String,
        val channelName: String,
        val duration: String
    ) : DiscoverFeedItem

    @Immutable
    data class Collection(
        override val id: String,
        val title: String,
        val resourceCount: Int
    ) : DiscoverFeedItem

    @Immutable
    data class ContributorPost(
        override val id: String,
        val name: String,
        val initials: String,
        val message: String
    ) : DiscoverFeedItem
}

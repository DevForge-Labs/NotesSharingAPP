package com.pravor.notessharing.model

import androidx.compose.runtime.Immutable

@Immutable
data class TrendingNote(
    val id: String,
    val title: String,
    val subject: String,
    val downloads: Int,
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
    val documentType: String = "Notes",
    val bookmarks: Int = 0
)

@Immutable
data class VideoRecommendation(
    val id: String,
    val title: String,
    val channelName: String,
    val duration: String,
    val subject: String,
    val youtubeVideoId: String = "",
    val upvotes: Int = 0,
    val bookmarks: Int = 0
)

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
    val uploads: Int,
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
        val downloads: Int
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

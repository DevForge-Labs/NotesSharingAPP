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
    val isBookmarked: Boolean
)

@Immutable
data class VideoRecommendation(
    val id: String,
    val title: String,
    val channelName: String,
    val duration: String,
    val subject: String
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

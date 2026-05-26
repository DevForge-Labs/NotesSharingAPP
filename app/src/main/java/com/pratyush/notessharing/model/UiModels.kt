package com.pratyush.notessharing.model

import androidx.compose.runtime.Immutable

@Immutable
data class FeedItem(
    val id: String,
    val uploaderName: String,
    val uploaderInitials: String,
    val uploadDate: String,
    val title: String,
    val description: String,
    val tags: List<String>,
    val fileType: FileType,
    val upvotes: Int,
    val comments: Int,
    val downloads: Int,
    val isUpvoted: Boolean,
    val isSaved: Boolean
)

@Immutable
data class StudyFile(
    val id: String,
    val title: String,
    val uploadDate: String,
    val fileType: FileType,
    val downloads: Int,
    val upvotes: Int
)

@Immutable
data class TrendingTopic(
    val id: String,
    val title: String,
    val subtitle: String
)

@Immutable
data class Profile(
    val name: String,
    val initials: String,
    val branch: String,
    val semester: String,
    val uploads: Int,
    val saved: Int,
    val upvotes: Int
)

enum class FileType(val label: String) {
    Pdf("PDF"),
    Notes("Notes"),
    Pyq("PYQ"),
    Book("Book"),
    Video("Video"),
    LabManual("Lab Manual"),
    CheatSheet("Cheat Sheet"),
    StudyGuide("Study Guide")
}

enum class Category(val label: String) {
    Notes("Notes"),
    Pyqs("PYQs"),
    Books("Books"),
    Videos("Videos"),
    Trending("Trending")
}

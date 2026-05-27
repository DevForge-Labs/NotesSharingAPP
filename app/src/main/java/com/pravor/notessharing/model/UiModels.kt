package com.pravor.notessharing.model

import androidx.compose.runtime.Immutable
import com.google.firebase.firestore.Exclude

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
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val semester: String = "Not Set",
    val profileImageUrl: String = "",
    val role: String = "user",
    val uploads: Int = 0,
    val bookmarks: Int = 0,
    val upvotes: Int = 0,
    val notesUploaded: Int = 0,
    val contributorLevel: Int = 1,
    val branch: String = "Computer Science",
    val createdAt: Long = System.currentTimeMillis()
) {
    @get:Exclude
    val initials: String
        get() = if (name.isNotBlank()) {
            name.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .map { it.first().uppercase() }
                .joinToString("")
        } else {
            "PN"
        }
}

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

fun semesterToYear(semester: Int): Int {
    return when (semester) {
        1, 2 -> 1
        3, 4 -> 2
        5, 6 -> 3
        7, 8 -> 4
        else -> 1
    }
}

fun getSemesterInt(semesterStr: String): Int {
    return semesterStr.filter { it.isDigit() }.toIntOrNull() ?: 1
}


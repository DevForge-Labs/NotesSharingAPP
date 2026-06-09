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
    val isSaved: Boolean,
    val bookmarksCount: Int = 0,
    val youtubeVideoId: String? = null,
    val youtubeUrl: String? = null,
    val thumbnailUrl: String? = null,
    val thumbnailGenerated: Boolean? = null,
    val thumbnailType: String? = null,
    val thumbnailUrls: List<String> = emptyList(),
    val documentType: String? = null,
    val type: String? = null,
    val subject: String? = null,
    val examYear: String? = null,
    val examType: String? = null,
    val section: String? = null,
    val sectionDisplay: String? = null,
    val youtubeThumbnailUrl: String? = null
)

enum class ResourceAvailability {
    ACTIVE,
    ARCHIVED_DOWNLOAD
}

@Immutable
data class StudyFile(
    val id: String,
    val title: String,
    val uploadDate: String,
    val fileType: FileType,
    val downloads: Int,
    val upvotes: Int,
    val thumbnailUrl: String? = null,
    val subject: String? = null,
    val documentType: String? = null,
    val examYear: String? = null,
    val examType: String? = null,
    val sectionDisplay: String? = null,
    val availability: ResourceAvailability = ResourceAvailability.ACTIVE,
    val localThumbnailPath: String? = null
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
    val college: String = "kiit",
    val section: String = "",
    val profileImageUrl: String = "",
    val role: String = "user",
    val uploads: Int = 0,
    val bookmarks: Int = 0,
    val upvotes: Int = 0,
    val notesUploaded: Int = 0,
    val contributorLevel: Int = 1,
    val branch: String = "Computer Science",
    val createdAt: Long = System.currentTimeMillis(),
    
    // Type-specific upload stats
    val pyqUploads: Int = 0,
    val notesUploads: Int = 0,
    val assignmentUploads: Int = 0,
    val cheatSheetUploads: Int = 0,
    val youtubeUploads: Int = 0,
    
    // Auth-only property (ignored in Firestore using @Exclude)
    @get:com.google.firebase.firestore.Exclude
    val isOnboardingRequired: Boolean = false
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

data class LevelProgress(
    val currentLevel: Int,
    val nextLevel: Int,
    val currentUploads: Int,
    val targetUploads: Int,
    val progress: Float,
    val nextLevelText: String
)

fun calculateLevelProgress(uploads: Int): LevelProgress {
    return when {
        uploads < 5 -> {
            val progress = uploads.toFloat() / 5f
            LevelProgress(
                currentLevel = 1,
                nextLevel = 2,
                currentUploads = uploads,
                targetUploads = 5,
                progress = progress,
                nextLevelText = "$uploads / 5 uploads completed"
            )
        }
        uploads < 15 -> {
            val completedInLevel = uploads - 5
            val neededInLevel = 15 - 5 // 10
            val progress = completedInLevel.toFloat() / neededInLevel.toFloat()
            LevelProgress(
                currentLevel = 2,
                nextLevel = 3,
                currentUploads = uploads,
                targetUploads = 15,
                progress = progress,
                nextLevelText = "$uploads / 15 uploads completed"
            )
        }
        uploads < 30 -> {
            val completedInLevel = uploads - 15
            val neededInLevel = 30 - 15 // 15
            val progress = completedInLevel.toFloat() / neededInLevel.toFloat()
            LevelProgress(
                currentLevel = 3,
                nextLevel = 4,
                currentUploads = uploads,
                targetUploads = 30,
                progress = progress,
                nextLevelText = "$uploads / 30 uploads completed"
            )
        }
        uploads < 50 -> {
            val completedInLevel = uploads - 30
            val neededInLevel = 50 - 30 // 20
            val progress = completedInLevel.toFloat() / neededInLevel.toFloat()
            LevelProgress(
                currentLevel = 4,
                nextLevel = 5,
                currentUploads = uploads,
                targetUploads = 50,
                progress = progress,
                nextLevelText = "$uploads / 50 uploads completed"
            )
        }
        else -> {
            LevelProgress(
                currentLevel = 5,
                nextLevel = 5,
                currentUploads = uploads,
                targetUploads = uploads,
                progress = 1.0f,
                nextLevelText = "Max Level 5 Contributor ($uploads uploads)"
            )
        }
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


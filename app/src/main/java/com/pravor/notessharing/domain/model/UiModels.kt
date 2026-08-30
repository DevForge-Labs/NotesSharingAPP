package com.pravor.notessharing.domain.model

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
    val downloadsCount: Int,
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
    val subjectId: String? = null,
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
    val downloadsCount: Int,
    val upvotes: Int,
    val thumbnailUrl: String? = null,
    val subject: String? = null,
    val documentType: String? = null,
    val examYear: String? = null,
    val examType: String? = null,
    val sectionDisplay: String? = null,
    val availability: ResourceAvailability = ResourceAvailability.ACTIVE,
    val localThumbnailPath: String? = null,
    val college: String? = null,
    val branch: String? = null,
    val semester: String? = null,
    val subjectId: String? = null
) {
    fun matchesSearchQuery(query: String): Boolean {
        if (query.isBlank()) return true
        val normalizedQuery = query.trim().lowercase(java.util.Locale.ROOT)
        
        return title.lowercase(java.util.Locale.ROOT).contains(normalizedQuery) ||
                subject?.lowercase(java.util.Locale.ROOT)?.contains(normalizedQuery) == true ||
                documentType?.lowercase(java.util.Locale.ROOT)?.contains(normalizedQuery) == true ||
                examYear?.lowercase(java.util.Locale.ROOT)?.contains(normalizedQuery) == true ||
                examType?.lowercase(java.util.Locale.ROOT)?.contains(normalizedQuery) == true ||
                sectionDisplay?.lowercase(java.util.Locale.ROOT)?.contains(normalizedQuery) == true ||
                fileType.label.lowercase(java.util.Locale.ROOT).contains(normalizedQuery)
    }
}

@Immutable
data class TrendingTopic(
    val id: String,
    val title: String,
    val subtitle: String
)



object ContributorLevels {
    val THRESHOLDS = listOf(
        0,   // Level 1
        5,   // Level 2
        15,  // Level 3
        30,  // Level 4
        50   // Level 5
    )
}

fun calculateLevel(totalUploads: Int): Int {
    for (i in ContributorLevels.THRESHOLDS.indices.reversed()) {
        if (totalUploads >= ContributorLevels.THRESHOLDS[i]) {
            return i + 1
        }
    }
    return 1
}

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
    val accountStatus: String = "ACTIVE",
    val totalUploads: Int = 0,
    val bookmarks: Int = 0,
    val upvotes: Int = 0,
    val contributorLevel: Int = 1,
    val branch: String = "Computer Science",
    val createdAt: Long = System.currentTimeMillis(),
    
    // Type-specific upload stats
    val pyqUploads: Int = 0,
    val notesUploads: Int = 0,
    val assignmentUploads: Int = 0,
    val cheatSheetUploads: Int = 0,
    val youtubeResourceUploads: Int = 0,
    
    // Auth-only property (ignored in Firestore using @Exclude)
    @get:com.google.firebase.firestore.Exclude
    val isOnboardingRequired: Boolean = false
) {
    @get:Exclude
    val isDisabled: Boolean
        get() = accountStatus.equals("DISABLED", ignoreCase = true) || accountStatus.equals("AUTH_DELETED", ignoreCase = true)

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

fun calculateLevelProgress(totalUploads: Int): LevelProgress {
    val currentLevel = calculateLevel(totalUploads)
    if (currentLevel >= 5) {
        return LevelProgress(
            currentLevel = 5,
            nextLevel = 5,
            currentUploads = totalUploads,
            targetUploads = totalUploads,
            progress = 1.0f,
            nextLevelText = "Max Level 5 Contributor ($totalUploads uploads)"
        )
    }
    
    val currentThreshold = ContributorLevels.THRESHOLDS[currentLevel - 1]
    val nextThreshold = ContributorLevels.THRESHOLDS[currentLevel]
    
    val completedInLevel = totalUploads - currentThreshold
    val neededInLevel = nextThreshold - currentThreshold
    val progress = completedInLevel.toFloat() / neededInLevel.toFloat()
    
    return LevelProgress(
        currentLevel = currentLevel,
        nextLevel = currentLevel + 1,
        currentUploads = totalUploads,
        targetUploads = nextThreshold,
        progress = progress.coerceIn(0f, 1f),
        nextLevelText = "$totalUploads / $nextThreshold uploads completed"
    )
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


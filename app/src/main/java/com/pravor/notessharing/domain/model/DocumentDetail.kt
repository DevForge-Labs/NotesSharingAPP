package com.pravor.notessharing.domain.model

import androidx.compose.runtime.Immutable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Immutable
data class DocumentDetail(
    val id: String,
    val title: String,
    val description: String,
    val branch: String,
    val semester: String,
    val subject: String,
    val documentType: String,
    val uploaderId: String,
    val uploaderName: String,
    val uploaderPhotoUrl: String,
    val uploadedAt: Long,
    val downloadsCount: Int,
    val upvotes: Int,
    val bookmarks: Int,
    val fileUrls: List<String>,
    val fileSize: Long,
    val fileExtension: String,
    val fileType: String, // "pdf" or "image"
    val attachmentCount: Int,
    val thumbnailUrl: String? = null,
    val thumbnailGenerated: Boolean? = null,
    val thumbnailType: String? = null,
    val thumbnailUrls: List<String> = emptyList(),
    val examYear: String? = null,
    val examType: String? = null,
    val section: String? = null,
    val sectionDisplay: String? = null,
    val youtubeThumbnailUrl: String? = null,
    val collection: String = "notes",
    val isUpvoted: Boolean = false,
    val isBookmarked: Boolean = false,
    val displaySubject: String? = null,
    val college: String = "",
    val processingStatus: String? = null,
    val processingError: String? = null
) {
    fun toFeedItem(): FeedItem {
        val initials = if (uploaderName.isNotBlank()) {
            uploaderName.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .map { it.first().uppercase() }
                .joinToString("")
                .ifBlank { "PN" }
        } else {
            "PN"
        }

        val fileTypeEnum = when (documentType.lowercase(Locale.ROOT).replace(" ", "")) {
            "pyq" -> FileType.Pyq
            "cheatsheet" -> FileType.CheatSheet
            "assignment" -> FileType.Pdf
            "notes" -> FileType.Notes
            else -> FileType.Pdf
        }

        val dateFormatted = try {
            val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
            sdf.format(Date(uploadedAt))
        } catch (e: Exception) {
            "Unknown date"
        }

        return FeedItem(
            id = id,
            uploaderName = uploaderName,
            uploaderInitials = initials,
            uploadDate = dateFormatted,
            title = title,
            description = description.ifBlank { "$documentType resource for $subject" },
            tags = listOfNotNull(subject.ifBlank { null }, semester.ifBlank { null }, documentType.ifBlank { null }),
            fileType = fileTypeEnum,
            upvotes = upvotes,
            comments = 0,
            downloadsCount = downloadsCount,
            isUpvoted = false,
            isSaved = false,
            thumbnailUrl = thumbnailUrl,
            thumbnailGenerated = thumbnailGenerated,
            thumbnailType = thumbnailType,
            thumbnailUrls = thumbnailUrls,
            documentType = documentType,
            type = documentType,
            subject = subject,
            examYear = examYear,
            examType = examType,
            section = section,
            sectionDisplay = sectionDisplay,
            youtubeThumbnailUrl = youtubeThumbnailUrl
        )
    }
}

fun Map<String, Any>.toDocumentDetail(id: String, collection: String = "notes"): DocumentDetail {
    val fileUrlsList = (this["fileUrls"] as? List<*>)?.mapNotNull { it as? String }
        ?: listOfNotNull(this["downloadUrl"] as? String ?: this["fileUrl"] as? String ?: this["youtubeUrl"] as? String)

    return DocumentDetail(
        id = id,
        title = (this["title"] as? String ?: "Untitled Document").removeFileExtension(),
        description = this["description"] as? String ?: "",
        branch = this["branch"] as? String ?: "",
        semester = this["semester"] as? String ?: "",
        subject = run {
            val rawDisplaySubject = this["displaySubject"] as? String
            val rawSubject = this["subject"] as? String ?: ""
            if (!rawDisplaySubject.isNullOrBlank()) rawDisplaySubject else rawSubject
        },
        documentType = this["documentType"] as? String ?: this["type"] as? String ?: "Notes",
        uploaderId = this["uploaderId"] as? String ?: "",
        uploaderName = this["uploaderName"] as? String ?: "Anonymous",
        uploaderPhotoUrl = this["uploaderPhotoUrl"] as? String ?: "",
        uploadedAt = (this["uploadedAt"] as? Long) ?: (this["uploadTimestamp"] as? Long) ?: 0L,
        downloadsCount = if (collection == "notes") {
            ((this["downloadsCount"] ?: this["downloads"]) as? Number)?.toInt() ?: 0
        } else {
            ((this["downloads"] ?: this["downloadsCount"]) as? Number)?.toInt() ?: 0
        },
        upvotes = (this["upvotes"] as? Long)?.toInt() ?: (this["likesCount"] as? Long)?.toInt() ?: 0,
        bookmarks = (this["bookmarks"] as? Long)?.toInt() ?: 0,
        fileUrls = fileUrlsList,
        fileSize = (this["fileSize"] as? Long) ?: 0L,
        fileExtension = this["fileExtension"] as? String ?: "",
        fileType = this["fileType"] as? String ?: "pdf",
        attachmentCount = (this["attachmentCount"] as? Long)?.toInt() ?: fileUrlsList.size,
        thumbnailUrl = this["thumbnailUrl"] as? String,
        thumbnailGenerated = this["thumbnailGenerated"] as? Boolean,
        thumbnailType = this["thumbnailType"] as? String,
        thumbnailUrls = (this["thumbnailUrls"] as? List<*>)?.mapNotNull { it as? String }
            ?: listOfNotNull(this["thumbnailUrl"] as? String),
        examYear = (this["examYear"] ?: this["year"])?.toString(),
        examType = this["examType"]?.toString(),
        section = this["section"] as? String,
        sectionDisplay = this["sectionDisplay"] as? String,
        youtubeThumbnailUrl = this["youtubeThumbnailUrl"] as? String,
        collection = collection,
        displaySubject = this["displaySubject"] as? String,
        college = this["college"] as? String ?: "",
        processingStatus = this["processingStatus"] as? String,
        processingError = this["processingError"] as? String
    )
}

fun String.removeFileExtension(): String {
    val extensions = listOf(".pdf", ".docx", ".pptx", ".jpg", ".jpeg", ".png", ".webp", ".gif")
    for (ext in extensions) {
        if (this.endsWith(ext, ignoreCase = true)) {
            return this.substring(0, this.length - ext.length)
        }
    }
    return this
}

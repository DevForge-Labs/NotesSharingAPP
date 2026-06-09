package com.pravor.notessharing.data.download

data class DownloadedDocument(
    val documentId: String,
    val downloadedAt: Long,
    val title: String = "",
    val subject: String = "",
    val uploaderName: String = "Anonymous",
    val uploaderContributorLevel: String = "Bronze Contributor",
    val documentType: String = "Notes",
    val fileUrls: List<String> = emptyList(),
    val thumbnailUrl: String? = null,
    val examYear: String? = null,
    val examType: String? = null,
    val sectionDisplay: String? = null,
    val upvotes: Int = 0,
    val downloads: Int = 0,
    val localThumbnailPath: String? = null
)

data class DownloadedAttachment(
    val documentId: String,
    val storagePath: String,
    val localPath: String
)

package com.pravor.notessharing.data.download

data class DownloadedDocument(
    val documentId: String,
    val downloadedAt: Long
)

data class DownloadedAttachment(
    val documentId: String,
    val storagePath: String,
    val localPath: String
)

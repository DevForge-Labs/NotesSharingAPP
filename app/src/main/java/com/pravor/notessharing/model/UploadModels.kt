package com.pravor.notessharing.model

import androidx.compose.runtime.Immutable

enum class UploadType(val label: String) {
    Pdf("PDF"),
    Images("Images"),
    Youtube("YouTube Link")
}

enum class UploadFileSource {
    DocumentPicker,
    Gallery,
    Camera
}

@Immutable
data class SelectedUploadFile(
    val uri: String,
    val displayName: String,
    val sizeBytes: Long,
    val source: UploadFileSource
)

@Immutable
data class UploadItem(
    val id: String,
    val branch: String,
    val year: String,
    val subject: String,
    val type: UploadType,
    val uriList: List<String>,
    val youtubeUrl: String?,
    val timestamp: Long,
    val totalSizeBytes: Long
)

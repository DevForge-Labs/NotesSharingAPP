package com.pravor.notessharing.model

import androidx.compose.runtime.Immutable

enum class UploadType(val label: String) {
    Pyq("PYQ"),
    Notes("Notes"),
    CheatSheet("Cheat Sheet"),
    Assignment("Assignment"),
    Youtube("YouTube Resource")
}

val UploadType.firestoreValue: String
    get() = when (this) {
        UploadType.CheatSheet -> "CheatSheet"
        else -> this.label
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

private val YoutubeVideoPattern = "^(?:https?:\\/\\/)?(?:www\\.)?(?:youtube\\.com\\/(?:watch\\?(?:.*&)?v=|shorts\\/|live\\/|embed\\/|v\\/)|youtu\\.be\\/)([a-zA-Z0-9_-]{11})".toRegex(RegexOption.IGNORE_CASE)
private val YoutubePlaylistPattern = "[?&]list=([a-zA-Z0-9_-]+)".toRegex(RegexOption.IGNORE_CASE)

fun extractYoutubeVideoId(url: String): String? {
    val trimmed = url.trim()
    if (trimmed.isBlank()) return null
    val matchResult = YoutubeVideoPattern.find(trimmed)
    return matchResult?.groupValues?.get(1)
}

fun extractYoutubePlaylistId(url: String): String? {
    val trimmed = url.trim()
    if (trimmed.isBlank()) return null
    val matchResult = YoutubePlaylistPattern.find(trimmed)
    return matchResult?.groupValues?.get(1)
}

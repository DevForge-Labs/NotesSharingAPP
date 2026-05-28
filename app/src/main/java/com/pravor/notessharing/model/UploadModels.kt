package com.pravor.notessharing.model

import androidx.compose.runtime.Immutable

enum class UploadType(val label: String) {
    Pyq("PYQ"),
    Notes("Notes"),
    CheatSheet("Cheat Sheet"),
    Assignment("Assignment"),
    Youtube("YouTube Resource")
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

fun extractYoutubeVideoId(url: String): String? {
    val trimmed = url.trim()
    if (trimmed.isBlank()) return null
    val pattern = "^(?:https?:\\/\\/)?(?:www\\.)?(?:youtube\\.com\\/(?:[^\\/\\n\\s]+\\/\\S+\\/|(?:v|e(?:mbed)?)\\/|\\S*?[?&]v=)|youtu\\.be\\/|youtube\\.com\\/shorts\\/)([a-zA-Z0-9_-]{11})".toRegex(RegexOption.IGNORE_CASE)
    val matchResult = pattern.find(trimmed)
    return matchResult?.groupValues?.get(1)
}

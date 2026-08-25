package com.pravor.notessharing.domain.model.classroom

import androidx.compose.runtime.Immutable

enum class AttachmentType {
    DRIVE_FILE,
    YOUTUBE,
    LINK,
    FORM,
    UNKNOWN
}

@Immutable
data class ClassroomAttachment(
    val title: String,
    val linkUrl: String,
    val type: AttachmentType,
    val thumbnailUrl: String? = null
)

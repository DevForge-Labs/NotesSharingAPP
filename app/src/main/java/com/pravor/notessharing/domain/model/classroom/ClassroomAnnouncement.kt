package com.pravor.notessharing.domain.model.classroom

import androidx.compose.runtime.Immutable

@Immutable
data class ClassroomAnnouncement(
    val id: String,
    val text: String,
    val creationTime: String? = null,
    val updateTime: String? = null,
    val alternateLink: String? = null,
    val attachments: List<ClassroomAttachment> = emptyList()
)

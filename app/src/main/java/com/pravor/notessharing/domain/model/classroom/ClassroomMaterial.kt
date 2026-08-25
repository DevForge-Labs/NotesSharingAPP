package com.pravor.notessharing.domain.model.classroom

import androidx.compose.runtime.Immutable

@Immutable
data class ClassroomMaterial(
    val id: String,
    val title: String,
    val description: String? = null,
    val creationTime: String? = null,
    val updateTime: String? = null,
    val alternateLink: String? = null,
    val attachments: List<ClassroomAttachment> = emptyList()
)

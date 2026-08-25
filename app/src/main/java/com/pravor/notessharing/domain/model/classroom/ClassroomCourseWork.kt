package com.pravor.notessharing.domain.model.classroom

import androidx.compose.runtime.Immutable

@Immutable
data class ClassroomCourseWork(
    val id: String,
    val title: String,
    val description: String? = null,
    val dueFormatted: String? = null,
    val creationTime: String? = null,
    val alternateLink: String? = null,
    val attachments: List<ClassroomAttachment> = emptyList()
)

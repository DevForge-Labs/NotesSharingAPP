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
    val associatedWithDeveloper: Boolean = false,
    val attachments: List<ClassroomAttachment> = emptyList()
)

val ClassroomAttachment.isGoogleForm: Boolean
    get() = type == AttachmentType.FORM || (type == AttachmentType.LINK && isGoogleFormUrl(linkUrl))

fun isGoogleFormUrl(url: String): Boolean {
    val lower = url.lowercase()
    return lower.contains("docs.google.com/forms") ||
           lower.contains("forms.gle") ||
           lower.contains("forms.google.com")
}

val ClassroomCourseWork.primaryFormAttachment: ClassroomAttachment?
    get() = attachments.firstOrNull { it.isGoogleForm }

val ClassroomCourseWork.primaryExternalLinkAttachment: ClassroomAttachment?
    get() = attachments.firstOrNull { it.type == AttachmentType.LINK && !it.isGoogleForm }

val ClassroomCourseWork.hasActionableExternalTask: Boolean
    get() = primaryFormAttachment != null || (primaryExternalLinkAttachment != null && attachments.none { it.type == AttachmentType.DRIVE_FILE })

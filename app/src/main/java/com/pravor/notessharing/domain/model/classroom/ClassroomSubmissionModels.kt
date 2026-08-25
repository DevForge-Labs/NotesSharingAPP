package com.pravor.notessharing.domain.model.classroom

enum class SubmissionState {
    NEW,
    CREATED,
    TURNED_IN,
    RETURNED,
    RECLAIMED_BY_STUDENT,
    UNKNOWN
}

data class SubmissionAttachment(
    val id: String,
    val title: String,
    val linkUrl: String,
    val type: AttachmentType,
    val thumbnailUrl: String? = null
)

data class ClassroomStudentSubmission(
    val id: String,
    val courseId: String,
    val courseWorkId: String,
    val userId: String,
    val state: SubmissionState,
    val late: Boolean = false,
    val assignedGrade: Double? = null,
    val attachments: List<SubmissionAttachment> = emptyList(),
    val alternateLink: String? = null
)

sealed class SubmissionProgress {
    object Idle : SubmissionProgress()
    data class UploadingToDrive(val fileName: String) : SubmissionProgress()
    data class AttachingToClassroom(val fileName: String) : SubmissionProgress()
    object TurningIn : SubmissionProgress()
    data class Success(val message: String = "Assignment successfully submitted!") : SubmissionProgress()
    data class Error(val errorMessage: String) : SubmissionProgress()
}

data class SelectedSubmissionFile(
    val uri: android.net.Uri,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String
)

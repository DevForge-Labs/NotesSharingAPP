package com.pravor.notessharing.ui.features.upload.edit

import androidx.compose.runtime.Immutable
import com.pravor.notessharing.domain.model.DocumentDetail
import com.pravor.notessharing.domain.model.EditableAttachment
import com.pravor.notessharing.domain.model.UploadType

@Immutable
data class UploadEditUiState(
    val isLoading: Boolean = true,
    val document: DocumentDetail? = null,
    val collectionName: String = "notes",
    val uploadType: UploadType = UploadType.Notes,
    val existingAttachments: List<EditableAttachment.ExistingRemote> = emptyList(),
    val removedAttachmentIds: Set<String> = emptySet(),
    val newlyAddedAttachments: List<EditableAttachment.NewlyAddedLocal> = emptyList(),
    val isSaving: Boolean = false,
    val saveProgress: Float = 0f,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false,
    val showDiscardDialog: Boolean = false
) {
    val activeExistingAttachments: List<EditableAttachment.ExistingRemote>
        get() = existingAttachments.filterNot { it.id in removedAttachmentIds }

    val allActiveAttachments: List<EditableAttachment>
        get() = activeExistingAttachments + newlyAddedAttachments

    val hasChanges: Boolean
        get() = removedAttachmentIds.isNotEmpty() || newlyAddedAttachments.isNotEmpty()

    val totalSizeBytes: Long
        get() = allActiveAttachments.sumOf { it.sizeBytes }

    val isValidForType: Boolean
        get() = when (uploadType) {
            UploadType.Pyq -> allActiveAttachments.size == 1 && allActiveAttachments.first().isPdf
            else -> allActiveAttachments.isNotEmpty()
        }

    val canSave: Boolean
        get() = !isSaving && hasChanges && isValidForType && errorMessage == null
}

package com.pravor.notessharing.state

import androidx.compose.runtime.Immutable
import com.pravor.notessharing.model.SelectedUploadFile
import com.pravor.notessharing.model.UploadItem
import com.pravor.notessharing.model.UploadType

@Immutable
data class UploadUiState(
    val branches: List<String> = emptyList(),
    val years: List<String> = emptyList(),
    val selectedBranch: String = "",
    val selectedYear: String = "",
    val subject: String = "",
    val selectedType: UploadType? = null,
    val selectedFiles: List<SelectedUploadFile> = emptyList(),
    val youtubeUrl: String = "",
    val errorMessage: String? = null,
    val savedUpload: UploadItem? = null,
    val isSaving: Boolean = false
) {
    val metadataComplete: Boolean
        get() = selectedBranch.isNotBlank() && selectedYear.isNotBlank() && subject.isNotBlank()

    val totalSizeBytes: Long
        get() = selectedFiles.sumOf { it.sizeBytes }
}

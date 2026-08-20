package com.pravor.notessharing.state

import androidx.compose.runtime.Immutable
import com.pravor.notessharing.domain.model.SelectedUploadFile
import com.pravor.notessharing.domain.model.UploadType

@Immutable
data class CatalogSubject(
    val id: String,
    val name: String
)

@Immutable
data class YoutubePreview(
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val url: String
)

@Immutable
data class UploadUiState(
    val branches: List<String> = emptyList(),
    val semesters: List<String> = emptyList(),
    val groups: List<String> = listOf("Group A", "Group B"),
    val selectedBranch: String = "",
    val selectedSemester: String = "",
    val selectedGroup: String = "",
    val subject: String = "",
    val subjectId: String = "",
    val useCatalogDropdown: Boolean = false,
    val subjectCatalogKeyExists: Boolean = false,
    val catalogSubjects: List<CatalogSubject> = emptyList(),
    val selectedType: UploadType? = null,
    val selectedFiles: List<SelectedUploadFile> = emptyList(),
    val youtubeUrl: String = "",
    val youtubeResourceType: String = "video",
    val description: String = "",
    val section: String = "",
    val title: String = "",
    
    // PYQ specific metadata
    val examYears: List<String> = emptyList(),
    val selectedExamYear: String = "",
    val examTypes: List<String> = emptyList(),
    val selectedExamType: String = "",
    
    // YouTube oEmbed preview
    val youtubePreview: YoutubePreview? = null,
    val isFetchingYoutube: Boolean = false,
    val youtubeError: String? = null,

    // Status and progress
    val errorMessage: String? = null,
    val uploadSuccess: Boolean = false,
    val isSaving: Boolean = false,
    val uploadProgress: Float = 0f
) {
    val metadataComplete: Boolean
        get() {
            val isFirstYear = selectedSemester == "Semester 1" || selectedSemester == "Semester 2"
            val groupComplete = !isFirstYear || selectedGroup.isNotBlank()
            return selectedBranch.isNotBlank() && selectedSemester.isNotBlank() && groupComplete && subject.isNotBlank() && selectedType != null
        }

    val totalSizeBytes: Long
        get() = selectedFiles.sumOf { it.sizeBytes }
}

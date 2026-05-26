package com.pratyush.notessharing.viewmodel

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pratyush.notessharing.data.LocalUploadRepository
import com.pratyush.notessharing.model.SelectedUploadFile
import com.pratyush.notessharing.model.UploadFileSource
import com.pratyush.notessharing.model.UploadType
import com.pratyush.notessharing.state.UploadUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UploadViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LocalUploadRepository(application)
    private var pendingCameraUri: Uri? = null

    private val _uiState = MutableStateFlow(
        UploadUiState(
            branches = listOf("Computer Science", "Mechanical", "Civil", "Electrical", "ECE"),
            years = listOf("1st Year", "2nd Year", "3rd Year", "4th Year")
        )
    )
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    fun selectBranch(branch: String) {
        _uiState.update { it.copy(selectedBranch = branch, errorMessage = null, savedUpload = null) }
    }

    fun selectYear(year: String) {
        _uiState.update { it.copy(selectedYear = year, errorMessage = null, savedUpload = null) }
    }

    fun updateSubject(subject: String) {
        _uiState.update { it.copy(subject = subject, errorMessage = null, savedUpload = null) }
    }

    fun selectUploadType(type: UploadType) {
        val current = _uiState.value
        if (current.selectedType != null && current.selectedType != type && (current.selectedFiles.isNotEmpty() || current.youtubeUrl.isNotBlank())) {
            _uiState.update { it.copy(errorMessage = "You can only upload one content type at a time.") }
            return
        }
        _uiState.update { it.copy(selectedType = type, errorMessage = null, savedUpload = null) }
    }

    fun addPickedUris(uris: List<Uri>, type: UploadType, source: UploadFileSource) {
        if (uris.isEmpty()) return
        val current = _uiState.value
        if (!current.metadataComplete) {
            _uiState.update { it.copy(errorMessage = "Choose branch, year, and subject before selecting files.") }
            return
        }
        if (!ensureType(type)) return
        if (type == UploadType.Youtube) {
            _uiState.update { it.copy(errorMessage = "YouTube uploads only support one link.") }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val files = uris.map { uri ->
                takeReadPermission(uri)
                repository.resolveSelectedFile(uri, source)
            }
            _uiState.update {
                it.copy(
                    selectedType = type,
                    selectedFiles = it.selectedFiles + files,
                    errorMessage = null,
                    savedUpload = null
                )
            }
        }
    }

    fun removeFile(file: SelectedUploadFile) {
        _uiState.update { state ->
            val files = state.selectedFiles.filterNot { it.uri == file.uri }
            state.copy(
                selectedFiles = files,
                selectedType = if (files.isEmpty() && state.youtubeUrl.isBlank()) null else state.selectedType,
                savedUpload = null
            )
        }
    }

    fun updateYoutubeUrl(url: String) {
        if (!ensureType(UploadType.Youtube)) return
        _uiState.update {
            it.copy(
                selectedType = UploadType.Youtube,
                youtubeUrl = url,
                selectedFiles = emptyList(),
                errorMessage = null,
                savedUpload = null
            )
        }
    }

    fun createCameraUri(): Uri {
        val context = getApplication<Application>()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val imageDir = File(context.filesDir, "camera_uploads").apply { mkdirs() }
        val imageFile = File(imageDir, "IMG_$timestamp.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
        pendingCameraUri = uri
        return uri
    }

    fun onCameraCaptureResult(success: Boolean) {
        val uri = pendingCameraUri ?: return
        pendingCameraUri = null
        if (success) {
            addPickedUris(listOf(uri), UploadType.Images, UploadFileSource.Camera)
        }
    }

    fun upload() {
        val state = _uiState.value
        val type = state.selectedType
        when {
            !state.metadataComplete -> {
                _uiState.update { it.copy(errorMessage = "Branch, year, and subject are required.") }
                return
            }
            type == null -> {
                _uiState.update { it.copy(errorMessage = "Choose an upload type.") }
                return
            }
            type == UploadType.Youtube && !isValidYoutubeUrl(state.youtubeUrl) -> {
                _uiState.update { it.copy(errorMessage = "Paste a valid YouTube URL.") }
                return
            }
            type != UploadType.Youtube && state.selectedFiles.isEmpty() -> {
                _uiState.update { it.copy(errorMessage = "Select at least one file.") }
                return
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                repository.saveUpload(
                    branch = state.selectedBranch,
                    year = state.selectedYear,
                    subject = state.subject,
                    type = type,
                    selectedFiles = state.selectedFiles,
                    youtubeUrl = state.youtubeUrl
                )
            }.onSuccess { item ->
                _uiState.update {
                    it.copy(
                        selectedFiles = emptyList(),
                        youtubeUrl = "",
                        selectedType = null,
                        isSaving = false,
                        savedUpload = item,
                        errorMessage = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = throwable.message ?: "Upload could not be saved locally."
                    )
                }
            }
        }
    }

    private fun ensureType(type: UploadType): Boolean {
        val current = _uiState.value
        val hasExistingContent = current.selectedFiles.isNotEmpty() || current.youtubeUrl.isNotBlank()
        return if (current.selectedType != null && current.selectedType != type && hasExistingContent) {
            _uiState.update { it.copy(errorMessage = "You can only upload one content type at a time.") }
            false
        } else {
            _uiState.update { it.copy(selectedType = type, errorMessage = null, savedUpload = null) }
            true
        }
    }

    private fun isValidYoutubeUrl(url: String): Boolean {
        val normalized = url.trim().lowercase()
        return (normalized.startsWith("http://") || normalized.startsWith("https://")) &&
            (normalized.contains("youtube.com") || normalized.contains("youtu.be"))
    }

    private fun takeReadPermission(uri: Uri) {
        runCatching {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }
}

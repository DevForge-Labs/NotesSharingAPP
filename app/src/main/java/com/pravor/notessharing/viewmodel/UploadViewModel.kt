package com.pravor.notessharing.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pravor.notessharing.data.UploadRepository
import com.pravor.notessharing.model.SelectedUploadFile
import com.pravor.notessharing.model.UploadFileSource
import com.pravor.notessharing.model.UploadType
import com.pravor.notessharing.state.UploadUiState
import com.pravor.notessharing.state.YoutubePreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UploadViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UploadRepository(application)
    private var pendingCameraUri: Uri? = null
    private var youtubeFetchJob: Job? = null

    private val _uiState = MutableStateFlow(
        UploadUiState(
            branches = listOf("Computer Science", "Mechanical", "Civil", "Electrical", "ECE"),
            semesters = listOf(
                "Semester 1", "Semester 2", "Semester 3", "Semester 4",
                "Semester 5", "Semester 6", "Semester 7", "Semester 8"
            ),
            examYears = listOf("2026", "2025", "2024", "2023", "2022", "2021", "2020"),
            examTypes = listOf("Midsem", "Endsem")
        )
    )
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    fun selectBranch(branch: String) {
        _uiState.update { it.copy(selectedBranch = branch, errorMessage = null, uploadSuccess = false) }
    }

    fun selectSemester(semester: String) {
        _uiState.update { it.copy(selectedSemester = semester, errorMessage = null, uploadSuccess = false) }
    }

    fun updateSubject(subject: String) {
        _uiState.update { it.copy(subject = subject, errorMessage = null, uploadSuccess = false) }
    }

    fun selectUploadType(type: UploadType) {
        val current = _uiState.value
        // Clear files or youtube link if type is switched
        _uiState.update {
            it.copy(
                selectedType = type,
                selectedFiles = emptyList(),
                youtubeUrl = "",
                youtubePreview = null,
                youtubeError = null,
                selectedExamYear = "",
                selectedExamType = "",
                errorMessage = null,
                uploadSuccess = false
            )
        }
    }

    fun selectExamYear(year: String) {
        _uiState.update { it.copy(selectedExamYear = year, errorMessage = null, uploadSuccess = false) }
    }

    fun selectExamType(type: String) {
        _uiState.update { it.copy(selectedExamType = type, errorMessage = null, uploadSuccess = false) }
    }

    fun addPickedUris(uris: List<Uri>, type: UploadType, source: UploadFileSource) {
        if (uris.isEmpty()) return
        val current = _uiState.value
        if (!current.metadataComplete) {
            _uiState.update { it.copy(errorMessage = "Choose branch, semester, subject, and document type first.") }
            return
        }
        
        // Enforce rules per type:
        // PYQ: exactly 1 PDF, no images.
        if (current.selectedType == UploadType.Pyq) {
            if (uris.size > 1 || current.selectedFiles.isNotEmpty()) {
                _uiState.update { it.copy(errorMessage = "PYQ only allows a single PDF document.") }
                return
            }
            val resolvedUri = uris.first()
            val name = getFileName(resolvedUri)
            if (!name.endsWith(".pdf", ignoreCase = true)) {
                _uiState.update { it.copy(errorMessage = "PYQ must be a PDF document.") }
                return
            }
        }

        // Notes / Cheat Sheets / Assignments: PDF or Images allowed. No YouTube.
        if (current.selectedType in listOf(UploadType.Notes, UploadType.CheatSheet, UploadType.Assignment)) {
            for (uri in uris) {
                val name = getFileName(uri)
                val isPdf = name.endsWith(".pdf", ignoreCase = true)
                val isImage = name.endsWith(".jpg", ignoreCase = true) || name.endsWith(".jpeg", ignoreCase = true) || name.endsWith(".png", ignoreCase = true) || name.endsWith(".webp", ignoreCase = true)
                if (!isPdf && !isImage) {
                    _uiState.update { it.copy(errorMessage = "Only PDF and Image files are allowed.") }
                    return
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val files = uris.map { uri ->
                takeReadPermission(uri)
                repository.resolveSelectedFile(uri, source)
            }
            _uiState.update {
                it.copy(
                    selectedFiles = it.selectedFiles + files,
                    errorMessage = null,
                    uploadSuccess = false
                )
            }
        }
    }

    fun removeFile(file: SelectedUploadFile) {
        _uiState.update { state ->
            val files = state.selectedFiles.filterNot { it.uri == file.uri }
            state.copy(
                selectedFiles = files,
                uploadSuccess = false
            )
        }
    }

    fun updateYoutubeUrl(url: String) {
        _uiState.update {
            it.copy(
                youtubeUrl = url,
                youtubePreview = null,
                youtubeError = null,
                errorMessage = null,
                uploadSuccess = false
            )
        }
        youtubeFetchJob?.cancel()
        if (url.isBlank()) return

        if (!isValidYoutubeUrl(url)) {
            _uiState.update { it.copy(youtubeError = "Invalid YouTube URL format.") }
            return
        }

        youtubeFetchJob = viewModelScope.launch {
            delay(500) // Debounce typing
            _uiState.update { it.copy(isFetchingYoutube = true, youtubeError = null) }
            runCatching {
                fetchYoutubeMetadata(url)
            }.onSuccess { preview ->
                _uiState.update { it.copy(youtubePreview = preview, isFetchingYoutube = false) }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isFetchingYoutube = false,
                        youtubeError = "Failed to load video details: ${e.message}"
                    )
                }
            }
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
            val currentType = _uiState.value.selectedType ?: UploadType.Notes
            addPickedUris(listOf(uri), currentType, UploadFileSource.Camera)
        }
    }

    fun upload() {
        val state = _uiState.value
        val type = state.selectedType
        
        // Metadata validations
        if (!state.metadataComplete) {
            _uiState.update { it.copy(errorMessage = "Branch, Semester, Subject, and Document Type are required.") }
            return
        }

        // Type specific validations
        when (type) {
            UploadType.Pyq -> {
                if (state.selectedFiles.size != 1) {
                    _uiState.update { it.copy(errorMessage = "PYQ requires exactly one PDF.") }
                    return
                }
                if (state.selectedExamYear.isBlank()) {
                    _uiState.update { it.copy(errorMessage = "Exam Year is mandatory for PYQ.") }
                    return
                }
                if (state.selectedExamType.isBlank()) {
                    _uiState.update { it.copy(errorMessage = "Exam Type is mandatory for PYQ.") }
                    return
                }
            }
            UploadType.Youtube -> {
                if (state.youtubeUrl.isBlank() || state.youtubePreview == null) {
                    _uiState.update { it.copy(errorMessage = "A valid YouTube URL and video details are required.") }
                    return
                }
            }
            else -> {
                if (state.selectedFiles.isEmpty()) {
                    _uiState.update { it.copy(errorMessage = "Please select at least one file to upload.") }
                    return
                }
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, uploadProgress = 0f) }
            runCatching {
                repository.uploadDocument(
                    branch = state.selectedBranch,
                    semester = state.selectedSemester,
                    subject = state.subject,
                    type = type!!,
                    selectedFiles = state.selectedFiles,
                    youtubeUrl = state.youtubeUrl,
                    youtubePreview = state.youtubePreview,
                    examYear = if (type == UploadType.Pyq) state.selectedExamYear else null,
                    examType = if (type == UploadType.Pyq) state.selectedExamType else null
                ) { progress ->
                    _uiState.update { it.copy(uploadProgress = progress) }
                }
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        selectedFiles = emptyList(),
                        youtubeUrl = "",
                        youtubePreview = null,
                        selectedExamYear = "",
                        selectedExamType = "",
                        isSaving = false,
                        uploadSuccess = true,
                        errorMessage = null,
                        uploadProgress = 1.0f
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "An error occurred during upload."
                    )
                }
            }
        }
    }

    fun clearUploadSuccess() {
        _uiState.update {
            UploadUiState(
                branches = it.branches,
                semesters = it.semesters,
                examYears = it.examYears,
                examTypes = it.examTypes
            )
        }
    }

    private fun isValidYoutubeUrl(url: String): Boolean {
        val normalized = url.trim().lowercase()
        return (normalized.startsWith("http://") || normalized.startsWith("https://")) &&
                (normalized.contains("youtube.com") || normalized.contains("youtu.be"))
    }

    private fun getFileName(uri: Uri): String {
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: "file"
        try {
            getApplication<Application>().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    name = cursor.getString(nameIndex) ?: name
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return name
    }

    private suspend fun fetchYoutubeMetadata(url: String): YoutubePreview = withContext(Dispatchers.IO) {
        val encodedUrl = URLEncoder.encode(url, "UTF-8")
        val connection = URL("https://www.youtube.com/oembed?url=$encodedUrl&format=json").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.doInput = true
        
        val responseCode = connection.responseCode
        if (responseCode == 200) {
            val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonObj = JSONObject(jsonText)
            val title = jsonObj.optString("title", "YouTube Video")
            val channelTitle = jsonObj.optString("author_name", "Unknown Channel")
            val thumbnailUrl = jsonObj.optString("thumbnail_url", "")
            YoutubePreview(
                title = title,
                channelTitle = channelTitle,
                thumbnailUrl = thumbnailUrl,
                url = url
            )
        } else {
            throw Exception("HTTP $responseCode")
        }
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

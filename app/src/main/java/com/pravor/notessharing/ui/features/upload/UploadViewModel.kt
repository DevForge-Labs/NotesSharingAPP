package com.pravor.notessharing.ui.features.upload

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*

import com.pravor.notessharing.domain.model.*
import com.pravor.notessharing.data.repository.*
import com.pravor.notessharing.core.util.*

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.pravor.notessharing.data.repository.ProfileRepository
import com.pravor.notessharing.core.util.NormalizationUtil
import com.pravor.notessharing.data.repository.UploadRepository
import com.pravor.notessharing.domain.model.SelectedUploadFile
import com.pravor.notessharing.domain.model.UploadFileSource
import com.pravor.notessharing.domain.model.UploadType
import com.pravor.notessharing.domain.model.extractYoutubeVideoId
import com.pravor.notessharing.domain.model.extractYoutubePlaylistId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
    private var subjectCatalogDocument: Map<String, Any>? = null

    private val _uiState = MutableStateFlow(
        UploadUiState(
            branches = com.pravor.notessharing.domain.model.AcademicCatalog.branches,
            semesters = com.pravor.notessharing.domain.model.AcademicCatalog.semesters,
            examYears = listOf("2026", "2025", "2024", "2023", "2022", "2021", "2020"),
            examTypes = listOf("Midsem", "Endsem")
        )
    )
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    private var userCollegeId: String? = null

    init {
        loadSubjectCatalog()
    }

    private fun loadSubjectCatalog() {
        viewModelScope.launch {
            try {
                val currentUid = FirebaseAuth.getInstance().currentUser?.uid
                val college = if (currentUid != null) {
                    ProfileRepository().getProfile(currentUid)?.college
                } else {
                    null
                }
                userCollegeId = college?.let { NormalizationUtil.normalizeCollege(it) }

                val snapshot = FirebaseFirestore.getInstance()
                    .collection("app_config")
                    .document("subject_catalog")
                    .get()
                    .await()
                if (snapshot.exists()) {
                    subjectCatalogDocument = snapshot.data
                    updateBranchesFromCatalog()
                    resolveSubjectForCurrentKey()
                }
            } catch (e: Exception) {
                // Ignore and use manual fallback
            }
        }
    }

    private fun updateBranchesFromCatalog() {
        val doc = subjectCatalogDocument ?: return
        val collegeId = userCollegeId ?: return
        val collegeCatalog = doc[collegeId] as? Map<*, *> ?: return
        
        val derivedBranches = collegeCatalog.keys
            .map { it.toString() }
            .filter { !it.startsWith("GROUP_", ignoreCase = true) }
            .map { it.uppercase(Locale.ROOT) }

        _uiState.update { it.copy(branches = derivedBranches) }
    }

    private fun resolveSubjectForCurrentKey() {
        val state = _uiState.value
        val sem = state.selectedSemester
        val branch = state.selectedBranch
        val group = state.selectedGroup

        val doc = subjectCatalogDocument
        val collegeId = userCollegeId
        if (doc == null || collegeId == null) {
            _uiState.update { it.copy(
                catalogSubjects = emptyList(),
                useCatalogDropdown = false,
                subjectCatalogKeyExists = false
            ) }
            return
        }

        val collegeCatalog = doc[collegeId] as? Map<*, *>
        if (collegeCatalog == null) {
            _uiState.update { it.copy(
                catalogSubjects = emptyList(),
                useCatalogDropdown = false,
                subjectCatalogKeyExists = false
            ) }
            return
        }

        val isFirstYear = sem == "Semester 1" || sem == "Semester 2"
        val catalogData = if (isFirstYear) {
            val groupKey = when {
                sem == "Semester 1" -> {
                    if (group == "Group A") "GROUP_A"
                    else if (group == "Group B") "GROUP_B"
                    else null
                }
                sem == "Semester 2" -> {
                    if (group == "Group A") "GROUP_B"
                    else if (group == "Group B") "GROUP_A"
                    else null
                }
                else -> null
            }
            if (groupKey == null) {
                _uiState.update { it.copy(
                    catalogSubjects = emptyList(),
                    useCatalogDropdown = false,
                    subjectCatalogKeyExists = false
                ) }
                return
            }
            collegeCatalog.entries.firstOrNull {
                it.key.toString().equals(groupKey, ignoreCase = true)
            }?.value
        } else {
            if (branch.isBlank() || sem.isBlank()) {
                _uiState.update { it.copy(
                    catalogSubjects = emptyList(),
                    useCatalogDropdown = false,
                    subjectCatalogKeyExists = false
                ) }
                return
            }
            
            val branchCatalog = collegeCatalog.entries.firstOrNull {
                it.key.toString().equals(branch, ignoreCase = true)
            }?.value as? Map<*, *>
            
            var semesterData = branchCatalog?.entries?.firstOrNull {
                it.key.toString().equals(sem, ignoreCase = true)
            }?.value
            
            if (semesterData == null) {
                val semNum = sem.filter { it.isDigit() }
                if (semNum.isNotEmpty()) {
                    semesterData = branchCatalog?.entries?.firstOrNull {
                        it.key.toString() == semNum
                    }?.value
                }
            }
            semesterData
        }

        if (catalogData != null) {
            val resolvedSubjects = mutableListOf<CatalogSubject>()
            if (catalogData is Map<*, *>) {
                for ((subId, subVal) in catalogData) {
                    val id = subId.toString()
                    val name = when (subVal) {
                        is Map<*, *> -> subVal["name"]?.toString() ?: id
                        is String -> subVal
                        else -> id
                    }
                    resolvedSubjects.add(CatalogSubject(id, name))
                }
            } else if (catalogData is List<*>) {
                for (item in catalogData) {
                    if (item is Map<*, *>) {
                        val id = item["subjectId"]?.toString() ?: item["id"]?.toString() ?: ""
                        val name = item["name"]?.toString() ?: item["subject"]?.toString() ?: id
                        if (id.isNotEmpty()) {
                            resolvedSubjects.add(CatalogSubject(id, name))
                        }
                    }
                }
            }

            resolvedSubjects.sortBy { it.name }

            if (resolvedSubjects.isNotEmpty()) {
                val currentInList = resolvedSubjects.any { it.name.equals(state.subject, ignoreCase = true) }
                if (currentInList) {
                    val matched = resolvedSubjects.first { it.name.equals(state.subject, ignoreCase = true) }
                    _uiState.update { it.copy(
                        catalogSubjects = resolvedSubjects,
                        useCatalogDropdown = true,
                        subjectCatalogKeyExists = true,
                        subject = matched.name,
                        subjectId = matched.id
                    ) }
                } else {
                    _uiState.update { it.copy(
                        catalogSubjects = resolvedSubjects,
                        useCatalogDropdown = true,
                        subjectCatalogKeyExists = true,
                        subject = "",
                        subjectId = ""
                    ) }
                }
                return
            }
        }

        // Key not found or empty -> fallback to manual free-text entry. Clear subjectId, preserve subject
        _uiState.update { it.copy(
            catalogSubjects = emptyList(),
            useCatalogDropdown = false,
            subjectCatalogKeyExists = false,
            subjectId = ""
        ) }
    }

    fun selectBranch(branch: String) {
        _uiState.update { it.copy(
            selectedBranch = branch,
            selectedSemester = "",
            selectedGroup = "",
            subject = "",
            subjectId = "",
            catalogSubjects = emptyList(),
            useCatalogDropdown = false,
            subjectCatalogKeyExists = false,
            errorMessage = null,
            uploadSuccess = false
        ) }
        resolveSubjectForCurrentKey()
    }

    fun selectSemester(semester: String) {
        val wasFirstYear = _uiState.value.selectedSemester == "Semester 1" || _uiState.value.selectedSemester == "Semester 2"
        val isFirstYear = semester == "Semester 1" || semester == "Semester 2"

        _uiState.update { 
            it.copy(
                selectedSemester = semester,
                selectedGroup = "",
                selectedBranch = if (isFirstYear || wasFirstYear || semester.isBlank()) "" else it.selectedBranch,
                subject = "",
                subjectId = "",
                catalogSubjects = emptyList(),
                useCatalogDropdown = false,
                subjectCatalogKeyExists = false,
                errorMessage = null,
                uploadSuccess = false
            )
        }
        resolveSubjectForCurrentKey()
    }

    fun selectGroup(group: String) {
        _uiState.update { it.copy(
            selectedGroup = group,
            subject = "",
            subjectId = "",
            catalogSubjects = emptyList(),
            useCatalogDropdown = false,
            subjectCatalogKeyExists = false,
            errorMessage = null,
            uploadSuccess = false
        ) }
        resolveSubjectForCurrentKey()
    }

    fun updateSubject(subject: String) {
        _uiState.update { it.copy(
            subject = subject,
            subjectId = "", // Clear subjectId in manual text entry mode
            errorMessage = null,
            uploadSuccess = false
        ) }
    }

    fun selectCatalogSubject(catalogSubject: CatalogSubject) {
        _uiState.update { it.copy(
            subject = catalogSubject.name,
            subjectId = catalogSubject.id,
            errorMessage = null,
            uploadSuccess = false
        ) }
    }

    fun selectUploadType(type: UploadType) {
        val current = _uiState.value
        // Clear files or youtube link if type is switched
        _uiState.update {
            it.copy(
                selectedType = type,
                selectedFiles = emptyList(),
                youtubeUrl = "",
                youtubeResourceType = "video",
                description = "",
                section = "",
                title = "",
                youtubePreview = null,
                youtubeError = null,
                selectedExamYear = "",
                selectedExamType = "",
                errorMessage = null,
                uploadSuccess = false
            )
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title, errorMessage = null, uploadSuccess = false) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description, errorMessage = null, uploadSuccess = false) }
    }

    fun updateSection(section: String) {
        _uiState.update { it.copy(section = section, errorMessage = null, uploadSuccess = false) }
    }

    fun selectYoutubeResourceType(resourceType: String) {
        _uiState.update {
            it.copy(
                youtubeResourceType = resourceType,
                youtubeUrl = "",
                youtubePreview = null,
                youtubeError = null,
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
                _uiState.update { it.copy(errorMessage = "PYQs support only a single PDF upload.") }
                return
            }
            val resolvedUri = uris.first()
            val name = getFileName(resolvedUri)
            if (!name.endsWith(".pdf", ignoreCase = true)) {
                _uiState.update { it.copy(errorMessage = "PYQs support only a single PDF upload.") }
                return
            }
        }

        // Notes / Cheat Sheets / Assignments: PDF, PowerPoint (.ppt, .pptx), or images (JPG, JPEG, PNG, WEBP) allowed.
        if (current.selectedType in listOf(UploadType.Notes, UploadType.CheatSheet, UploadType.Assignment)) {
            val allowedExtensions = listOf("pdf", "jpg", "jpeg", "png", "webp", "ppt", "pptx")
            for (uri in uris) {
                val name = getFileName(uri)
                val ext = name.substringAfterLast('.', "").lowercase()
                if (ext !in allowedExtensions) {
                    _uiState.update { it.copy(errorMessage = "Only PDF, PowerPoint (.ppt, .pptx), and image files are allowed.") }
                    return
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val resolvedFiles = uris.map { uri ->
                takeReadPermission(uri)
                repository.resolveSelectedFile(uri, source)
            }
            
            _uiState.update { state ->
                val candidateFiles = state.selectedFiles + resolvedFiles
                val validationError = validateSelectedFiles(candidateFiles)
                state.copy(
                    selectedFiles = candidateFiles,
                    errorMessage = validationError,
                    uploadSuccess = false
                )
            }
        }
    }

    private fun validateSelectedFiles(files: List<SelectedUploadFile>): String? {
        if (files.isEmpty()) return null
        
        val pdfCount = files.count { it.displayName.endsWith(".pdf", ignoreCase = true) }
        val imageCount = files.count { 
            val ext = it.displayName.substringAfterLast('.', "").lowercase()
            ext in listOf("jpg", "jpeg", "png", "webp")
        }
        
        val selectedType = _uiState.value.selectedType
        if (selectedType == UploadType.Pyq && (pdfCount > 1 || files.size > 1)) {
            return "PYQs support only a single PDF upload."
        }
        
        return null
    }

    fun removeFile(file: SelectedUploadFile) {
        _uiState.update { state ->
            val files = state.selectedFiles.filterNot { it.uri == file.uri }
            val validationError = validateSelectedFiles(files)
            state.copy(
                selectedFiles = files,
                errorMessage = validationError,
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
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isFetchingYoutube = false,
                        youtubeError = "Video preview unavailable. The YouTube link can still be uploaded."
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
            val isFirstYear = state.selectedSemester == "Semester 1" || state.selectedSemester == "Semester 2"
            val msg = if (isFirstYear) {
                "Branch, Semester, Group, Subject, and Document Type are required."
            } else {
                "Branch, Semester, Subject, and Document Type are required."
            }
            _uiState.update { it.copy(errorMessage = msg) }
            return
        }

        // Type specific validations
        when (type) {
            UploadType.Pyq -> {
                if (state.selectedFiles.size != 1) {
                    _uiState.update { it.copy(errorMessage = "PYQs support only a single PDF upload.") }
                    return
                }
                if (!state.selectedFiles.first().displayName.endsWith(".pdf", ignoreCase = true)) {
                    _uiState.update { it.copy(errorMessage = "PYQs support only a single PDF upload.") }
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
            UploadType.Notes, UploadType.CheatSheet -> {
                if (state.title.isBlank()) {
                    _uiState.update { it.copy(errorMessage = "Title is required.") }
                    return
                }
                if (state.selectedFiles.isEmpty()) {
                    _uiState.update { it.copy(errorMessage = "Please select at least one file to upload.") }
                    return
                }
                val validationError = validateSelectedFiles(state.selectedFiles)
                if (validationError != null) {
                    _uiState.update { it.copy(errorMessage = validationError) }
                    return
                }
            }
            UploadType.Assignment -> {
                if (state.section.isBlank()) {
                    _uiState.update { it.copy(errorMessage = "Section is required.") }
                    return
                }
                if (state.title.isBlank()) {
                    _uiState.update { it.copy(errorMessage = "Title is required.") }
                    return
                }
                if (state.selectedFiles.isEmpty()) {
                    _uiState.update { it.copy(errorMessage = "Please select at least one file to upload.") }
                    return
                }
                val validationError = validateSelectedFiles(state.selectedFiles)
                if (validationError != null) {
                    _uiState.update { it.copy(errorMessage = validationError) }
                    return
                }
            }
            UploadType.Youtube -> {
                val isPlaylist = state.youtubeResourceType == "playlist"
                val hasValidUrl = if (isPlaylist) {
                    extractYoutubePlaylistId(state.youtubeUrl) != null
                } else {
                    extractYoutubeVideoId(state.youtubeUrl) != null
                }
                if (state.youtubeUrl.isBlank() || !hasValidUrl) {
                    val label = if (isPlaylist) "playlist" else "YouTube"
                    _uiState.update { it.copy(errorMessage = "A valid $label URL is required.") }
                    return
                }
            }
            null -> {
                _uiState.update { it.copy(errorMessage = "Please select a document type.") }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, uploadProgress = 0f) }
            val uploadStartTime = System.currentTimeMillis()
            com.pravor.notessharing.core.analytics.AnalyticsManager.logUploadStarted(
                contentType = type!!.name,
                subject = state.subject,
                semester = state.selectedSemester,
                fileCount = if (type == UploadType.Youtube) 1 else state.selectedFiles.size
            )
            runCatching {
                when (type) {
                    UploadType.Pyq -> {
                        repository.uploadDocument(
                            branch = state.selectedBranch,
                            semester = state.selectedSemester,
                            subject = state.subject,
                            subjectId = state.subjectId,
                            type = type,
                            selectedFiles = state.selectedFiles,
                            youtubeUrl = state.youtubeUrl,
                            youtubePreview = state.youtubePreview,
                            examYear = state.selectedExamYear,
                            examType = state.selectedExamType,
                            description = state.description
                        ) { progress ->
                            _uiState.update { it.copy(uploadProgress = progress) }
                        }
                    }
                    UploadType.Notes -> {
                        repository.uploadNotes(
                            branch = state.selectedBranch,
                            semester = state.selectedSemester,
                            subject = state.subject,
                            subjectId = state.subjectId,
                            selectedFiles = state.selectedFiles,
                            title = state.title,
                            description = state.description
                        ) { progress ->
                            _uiState.update { it.copy(uploadProgress = progress) }
                        }
                    }
                    UploadType.CheatSheet -> {
                        repository.uploadCheatSheet(
                            branch = state.selectedBranch,
                            semester = state.selectedSemester,
                            subject = state.subject,
                            subjectId = state.subjectId,
                            selectedFiles = state.selectedFiles,
                            title = state.title,
                            description = state.description
                        ) { progress ->
                            _uiState.update { it.copy(uploadProgress = progress) }
                        }
                    }
                    UploadType.Assignment -> {
                        val normalizedSection = state.section
                            .trim()
                            .lowercase(Locale.ROOT)
                            .replace(Regex("[\\s-]+"), "")
                        
                        val prefix = normalizedSection.takeWhile { it.isLetter() }.uppercase(Locale.ROOT)
                        val suffix = normalizedSection.dropWhile { it.isLetter() }
                        val sectionDisplay = if (prefix.isNotEmpty() && suffix.isNotEmpty()) {
                            "$prefix-$suffix"
                        } else {
                            normalizedSection.uppercase(Locale.ROOT)
                        }

                        repository.uploadAssignment(
                            branch = state.selectedBranch,
                            semester = state.selectedSemester,
                            subject = state.subject,
                            subjectId = state.subjectId,
                            selectedFiles = state.selectedFiles,
                            title = state.title,
                            description = state.description,
                            section = normalizedSection,
                            sectionDisplay = sectionDisplay
                        ) { progress ->
                            _uiState.update { it.copy(uploadProgress = progress) }
                        }
                    }
                    UploadType.Youtube -> {
                        repository.uploadYouTubeResource(
                            branch = state.selectedBranch,
                            semester = state.selectedSemester,
                            subject = state.subject,
                            subjectId = state.subjectId,
                            youtubeUrl = state.youtubeUrl,
                            youtubePreview = state.youtubePreview,
                            youtubeResourceType = state.youtubeResourceType,
                            description = state.description
                        ) { progress ->
                            _uiState.update { it.copy(uploadProgress = progress) }
                        }
                    }
                }
            }.onSuccess {
                val durationMs = System.currentTimeMillis() - uploadStartTime
                com.pravor.notessharing.core.analytics.AnalyticsManager.logUploadCompleted(
                    contentType = type.name,
                    subject = state.subject,
                    durationMs = durationMs
                )
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
                com.pravor.notessharing.core.analytics.AnalyticsManager.logUploadFailed(
                    contentType = type.name,
                    subject = state.subject,
                    errorReason = e.javaClass.simpleName
                )
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
        val hasValidPrefix = (normalized.startsWith("http://") || normalized.startsWith("https://")) &&
                (normalized.contains("youtube.com") || normalized.contains("youtu.be"))
        if (!hasValidPrefix) return false
        
        return if (_uiState.value.youtubeResourceType == "playlist") {
            extractYoutubePlaylistId(url) != null
        } else {
            extractYoutubeVideoId(url) != null
        }
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

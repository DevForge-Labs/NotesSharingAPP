package com.pravor.notessharing.ui.features.upload.edit

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pravor.notessharing.data.repository.DocumentDetailRepository
import com.pravor.notessharing.data.repository.UploadRepository
import com.pravor.notessharing.domain.model.EditableAttachment
import com.pravor.notessharing.domain.model.UploadFileSource
import com.pravor.notessharing.domain.model.UploadType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UploadEditViewModel(application: Application) : AndroidViewModel(application) {
    private val docRepository = DocumentDetailRepository()
    private val uploadRepository = UploadRepository(application)
    private var pendingCameraUri: Uri? = null

    private val _uiState = MutableStateFlow(UploadEditUiState())
    val uiState: StateFlow<UploadEditUiState> = _uiState.asStateFlow()

    fun loadDocument(documentId: String) {
        if (documentId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Invalid document ID.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val doc = docRepository.getDocument(documentId)
                if (doc == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Document not found.") }
                    return@launch
                }

                val currentUid = FirebaseAuth.getInstance().currentUser?.uid
                if (currentUid.isNullOrBlank() || doc.uploaderId != currentUid) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "You are not authorized to edit attachments for this document."
                        )
                    }
                    return@launch
                }

                val resolvedType = when (doc.documentType.lowercase(Locale.ROOT).replace(" ", "")) {
                    "pyq" -> UploadType.Pyq
                    "cheatsheet" -> UploadType.CheatSheet
                    "assignment" -> UploadType.Assignment
                    else -> UploadType.Notes
                }

                val existingList = doc.fileUrls.mapIndexed { index, url ->
                    val storagePath = doc.storagePaths.getOrNull(index)
                        ?: extractStoragePathFromUrl(url)
                    val displayName = extractDisplayName(storagePath, url, doc.title, index, doc.fileUrls.size)
                    val ext = displayName.substringAfterLast('.', "").lowercase(Locale.ROOT)
                    val isPdf = ext == "pdf"
                    val isImg = ext in listOf("jpg", "jpeg", "png", "webp", "gif") ||
                            url.contains(".jpg", ignoreCase = true) ||
                            url.contains(".jpeg", ignoreCase = true) ||
                            url.contains(".png", ignoreCase = true) ||
                            url.contains(".webp", ignoreCase = true)
                    val size = if (doc.fileUrls.isNotEmpty()) doc.fileSize / doc.fileUrls.size else 0L
                    val thumb = doc.thumbnailUrls.getOrNull(index) ?: (if (isImg) url else doc.thumbnailUrl)

                    EditableAttachment.ExistingRemote(
                        id = storagePath.ifBlank { url },
                        displayName = displayName,
                        sizeBytes = size,
                        isPdf = isPdf,
                        isImage = isImg,
                        downloadUrl = url,
                        storagePath = storagePath,
                        thumbnailUrl = thumb
                    )
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        document = doc,
                        collectionName = doc.collection,
                        uploadType = resolvedType,
                        existingAttachments = existingList,
                        removedAttachmentIds = emptySet(),
                        newlyAddedAttachments = emptyList(),
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load document."
                    )
                }
            }
        }
    }

    fun addPickedUris(uris: List<Uri>, source: UploadFileSource) {
        if (uris.isEmpty()) return
        val state = _uiState.value
        val type = state.uploadType

        if (type == UploadType.Pyq) {
            val totalRemaining = state.activeExistingAttachments.size + state.newlyAddedAttachments.size + uris.size
            if (totalRemaining > 1) {
                _uiState.update { it.copy(errorMessage = "PYQs support only a single PDF attachment. Remove the existing one first.") }
                return
            }
        }

        val allowedExtensions = listOf("pdf", "jpg", "jpeg", "png", "webp", "ppt", "pptx")
        for (uri in uris) {
            val name = getFileName(uri)
            val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
            if (type == UploadType.Pyq && ext != "pdf") {
                _uiState.update { it.copy(errorMessage = "PYQs support only PDF attachments.") }
                return
            }
            if (ext !in allowedExtensions) {
                _uiState.update { it.copy(errorMessage = "Only PDF, PowerPoint (.ppt, .pptx), and image files are allowed.") }
                return
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val newItems = mutableListOf<EditableAttachment.NewlyAddedLocal>()
            for (uri in uris) {
                takeReadPermission(uri)
                val meta = queryMetadata(uri)
                val ext = meta.first.substringAfterLast('.', "").lowercase(Locale.ROOT)
                val isPdf = ext == "pdf"
                val isImg = ext in listOf("jpg", "jpeg", "png", "webp")

                if (meta.second > 100 * 1024 * 1024) {
                    _uiState.update { it.copy(errorMessage = "File ${meta.first} exceeds the 100 MB limit.") }
                    return@launch
                }

                newItems.add(
                    EditableAttachment.NewlyAddedLocal(
                        id = uri.toString(),
                        displayName = meta.first,
                        sizeBytes = meta.second,
                        isPdf = isPdf,
                        isImage = isImg,
                        uri = uri.toString(),
                        source = source
                    )
                )
            }

            _uiState.update {
                it.copy(
                    newlyAddedAttachments = it.newlyAddedAttachments + newItems,
                    errorMessage = null
                )
            }
        }
    }

    fun removeAttachment(attachment: EditableAttachment) {
        _uiState.update { state ->
            when (attachment) {
                is EditableAttachment.ExistingRemote -> {
                    state.copy(
                        removedAttachmentIds = state.removedAttachmentIds + attachment.id,
                        errorMessage = null
                    )
                }
                is EditableAttachment.NewlyAddedLocal -> {
                    state.copy(
                        newlyAddedAttachments = state.newlyAddedAttachments.filterNot { it.id == attachment.id },
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun restoreAttachment(attachmentId: String) {
        _uiState.update {
            it.copy(
                removedAttachmentIds = it.removedAttachmentIds - attachmentId,
                errorMessage = null
            )
        }
    }

    fun saveChanges() {
        val state = _uiState.value
        if (!state.canSave) return
        val doc = state.document ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, saveProgress = 0f) }

            val remainingExisting = state.activeExistingAttachments
            val newlyAdded = state.newlyAddedAttachments
            val removedRemote = state.existingAttachments.filter { it.id in state.removedAttachmentIds }

            runCatching {
                uploadRepository.updateDocumentAttachments(
                    collectionName = state.collectionName,
                    documentId = doc.id,
                    subject = doc.subject,
                    type = state.uploadType,
                    existingKept = remainingExisting,
                    newlyAdded = newlyAdded,
                    removedRemote = removedRemote
                ) { progress ->
                    _uiState.update { it.copy(saveProgress = progress) }
                }
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveSuccess = true,
                        saveProgress = 1.0f,
                        errorMessage = null
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Failed to save attachment changes."
                    )
                }
            }
        }
    }

    fun createCameraUri(): Uri {
        val context = getApplication<Application>()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val imageDir = File(context.filesDir, "camera_uploads").apply { mkdirs() }
        val imageFile = File(imageDir, "EDIT_IMG_$timestamp.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
        pendingCameraUri = uri
        return uri
    }

    fun onCameraCaptureResult(success: Boolean) {
        val uri = pendingCameraUri ?: return
        pendingCameraUri = null
        if (success) {
            addPickedUris(listOf(uri), UploadFileSource.Camera)
        }
    }

    fun showDiscardConfirmation() {
        _uiState.update { it.copy(showDiscardDialog = true) }
    }

    fun dismissDiscardConfirmation() {
        _uiState.update { it.copy(showDiscardDialog = false) }
    }

    private fun takeReadPermission(uri: Uri) {
        runCatching {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    private fun queryMetadata(uri: Uri): Pair<String, Long> {
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: "Selected file"
        var size = 0L
        try {
            getApplication<Application>().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
                    if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        if (size <= 0L) {
            try {
                size = getApplication<Application>().contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
            } catch (e: Exception) {
                // Ignore
            }
        }
        return name to size.coerceAtLeast(0L)
    }

    private fun getFileName(uri: Uri): String {
        return queryMetadata(uri).first
    }

    private fun extractStoragePathFromUrl(url: String): String {
        return try {
            if (url.contains("/o/")) {
                val encodedPath = url.substringAfter("/o/").substringBefore("?")
                URLDecoder.decode(encodedPath, "UTF-8")
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun extractDisplayName(
        storagePath: String,
        url: String,
        fallbackTitle: String,
        index: Int,
        total: Int
    ): String {
        if (storagePath.isNotBlank()) {
            val fileName = storagePath.substringAfterLast('/')
            if (fileName.isNotBlank()) return fileName
        }
        val cleanUrl = url.substringBefore('?')
        val urlSegment = cleanUrl.substringAfterLast('/')
        if (urlSegment.isNotBlank() && urlSegment.contains('.')) {
            return urlSegment
        }
        return if (total > 1) "$fallbackTitle (Part ${index + 1})" else fallbackTitle
    }
}

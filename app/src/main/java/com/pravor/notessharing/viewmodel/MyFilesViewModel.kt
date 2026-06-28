package com.pravor.notessharing.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pravor.notessharing.data.DocumentDetailRepository
import com.pravor.notessharing.data.download.DownloadDataStoreManager
import com.pravor.notessharing.data.download.DownloadService
import com.pravor.notessharing.model.FileType
import com.pravor.notessharing.model.StudyFile
import com.pravor.notessharing.state.MyFilesContent
import com.pravor.notessharing.state.MyFilesUiState
import android.content.Context
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MyFilesViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow<MyFilesUiState>(MyFilesUiState.Loading)
    val uiState: StateFlow<MyFilesUiState> = _uiState.asStateFlow()

    private val firestore = FirebaseFirestore.getInstance()
    private val docRepository = DocumentDetailRepository()

    private var realUploaded: List<StudyFile> = emptyList()
    private var downloadedDocs: List<StudyFile> = emptyList()

    private var isObservingDownloads = false
    private var isDownloadsLoaded = false
    private var isUploadedLoaded = false

    init {
        loadDownloads(application)
        loadMyFiles()
    }

    fun loadDownloads(context: Context) {
        if (isObservingDownloads) return
        isObservingDownloads = true

        val manager = DownloadDataStoreManager(context.applicationContext)
        viewModelScope.launch {
            manager.downloadedDocumentsFlow.collect { docs ->
                val studyFiles = mutableListOf<StudyFile>()
                for (doc in docs) {
                    val detail = docRepository.getDocument(doc.documentId)
                    if (detail != null) {
                        studyFiles.add(docDetailToStudyFile(detail, doc.downloadedAt, doc.localThumbnailPath))
                    } else {
                        // Check if the local files for this document actually exist on device
                        val attachments = manager.getDownloadedAttachments().filter { it.documentId == doc.documentId }
                        val pdfExists = attachments.isNotEmpty() && attachments.all { java.io.File(it.localPath).exists() }
                        val thumbnailExists = !doc.localThumbnailPath.isNullOrBlank() && java.io.File(doc.localThumbnailPath).exists()
                        
                        if (pdfExists) {
                            val fileTypeEnum = when (doc.documentType.lowercase(java.util.Locale.ROOT).trim()) {
                                "pyq", "pyqs" -> FileType.Pyq
                                "cheat sheet", "cheatsheet", "cheatsheets" -> FileType.CheatSheet
                                "assignment", "assignments" -> FileType.Notes
                                "notes" -> FileType.Notes
                                else -> FileType.Pdf
                            }
                            val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
                            val downloadDateStr = "Downloaded " + sdf.format(java.util.Date(doc.downloadedAt))
                            
                            studyFiles.add(
                                StudyFile(
                                    id = doc.documentId,
                                    title = doc.title.ifBlank { "Archived Download" },
                                    uploadDate = downloadDateStr,
                                    fileType = fileTypeEnum,
                                    downloadsCount = doc.downloadsCount,
                                    upvotes = doc.upvotes,
                                    thumbnailUrl = doc.thumbnailUrl,
                                    subject = doc.subject.ifBlank { "General" },
                                    documentType = doc.documentType,
                                    examYear = doc.examYear,
                                    examType = doc.examType,
                                    sectionDisplay = doc.sectionDisplay,
                                    availability = com.pravor.notessharing.model.ResourceAvailability.ARCHIVED_DOWNLOAD,
                                    localThumbnailPath = if (thumbnailExists) doc.localThumbnailPath else null
                                )
                            )
                        } else {
                            // Local PDF is missing -> remove the stale download
                            manager.removeDownload(doc.documentId)
                        }
                    }
                }
                downloadedDocs = studyFiles
                isDownloadsLoaded = true
                updateUiState()
            }
        }
    }

    fun loadMyFiles() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid == null) {
            realUploaded = emptyList()
            isUploadedLoaded = true
            updateUiState()
            return
        }

        viewModelScope.launch {
            try {
                val collections = listOf("documents", "notes", "pyqs", "assignments", "cheatsheets", "videos")
                val allDocs = coroutineScope {
                    val deferreds = collections.map { col ->
                        async {
                            try {
                                firestore.collection(col)
                                    .whereEqualTo("uploaderId", currentUid)
                                    .get()
                                    .await()
                                    .documents
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }
                    }
                    deferreds.awaitAll().flatten()
                }
                
                realUploaded = allDocs.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    documentToStudyFile(data)
                }
                isUploadedLoaded = true
                updateUiState()
            } catch (e: Exception) {
                realUploaded = emptyList()
                isUploadedLoaded = true
                updateUiState()
            }
        }
    }

    private fun updateUiState() {
        if (!isDownloadsLoaded || !isUploadedLoaded) {
            _uiState.update { MyFilesUiState.Loading }
            return
        }
        _uiState.update {
            if (downloadedDocs.isEmpty() && realUploaded.isEmpty()) {
                MyFilesUiState.Empty
            } else {
                MyFilesUiState.Success(
                    MyFilesContent(
                        savedFiles = downloadedDocs,
                        uploadedFiles = realUploaded
                    )
                )
            }
        }
    }

    fun deleteDownload(documentId: String, context: Context) {
        viewModelScope.launch {
            DownloadService.deleteDownload(context.applicationContext, documentId)
        }
    }

    private fun docDetailToStudyFile(
        detail: com.pravor.notessharing.model.DocumentDetail,
        downloadedAt: Long,
        localThumbnailPath: String?
    ): StudyFile {
        val fileTypeEnum = when (detail.documentType.lowercase(java.util.Locale.ROOT).trim()) {
            "pyq", "pyqs" -> FileType.Pyq
            "cheat sheet", "cheatsheet", "cheatsheets" -> FileType.CheatSheet
            "assignment", "assignments" -> FileType.Notes
            "notes" -> FileType.Notes
            else -> FileType.Pdf
        }

        val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
        val downloadDateStr = "Downloaded " + sdf.format(java.util.Date(downloadedAt))

        return StudyFile(
            id = detail.id,
            title = detail.title,
            uploadDate = downloadDateStr,
            fileType = fileTypeEnum,
            downloadsCount = detail.downloadsCount,
            upvotes = detail.upvotes,
            thumbnailUrl = detail.thumbnailUrl,
            subject = detail.subject,
            documentType = detail.documentType,
            examYear = detail.examYear,
            examType = detail.examType,
            sectionDisplay = detail.sectionDisplay,
            localThumbnailPath = localThumbnailPath
        )
    }

    private fun documentToStudyFile(doc: Map<String, Any>): StudyFile {
        val id = doc["documentId"] as? String ?: ""
        val title = doc["title"] as? String ?: ""
        val uploadedAt = doc["uploadedAt"] as? Long ?: (doc["uploadTimestamp"] as? Long ?: System.currentTimeMillis())
        val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
        val uploadDate = "Uploaded " + sdf.format(java.util.Date(uploadedAt))
        
        val docType = doc["documentType"] as? String ?: (doc["type"] as? String ?: "Notes")
        val fileType = when (docType) {
            "PYQ" -> FileType.Pyq
            "CheatSheet", "Cheat Sheet" -> FileType.CheatSheet
            "Assignment" -> FileType.Notes
            "Notes" -> FileType.Notes
            "YouTube Resource", "Videos" -> FileType.Video
            else -> FileType.Pdf
        }
        
        val upvotes = (doc["upvotes"] as? Long ?: (doc["likesCount"] as? Long ?: 0L)).toInt()
        val downloadsCount = (doc["downloadsCount"] as? Long ?: (doc["downloads"] as? Long ?: 0L)).toInt()
        
        val thumbnailUrl = doc["thumbnailUrl"] as? String
        val subject = doc["subject"] as? String
        val examYear = doc["examYear"] as? String
        val examType = doc["examType"] as? String
        val sectionDisplay = doc["sectionDisplay"] as? String ?: doc["section"] as? String

        return StudyFile(
            id = id,
            title = title,
            uploadDate = uploadDate,
            fileType = fileType,
            downloadsCount = downloadsCount,
            upvotes = upvotes,
            thumbnailUrl = thumbnailUrl,
            subject = subject,
            documentType = docType,
            examYear = examYear,
            examType = examType,
            sectionDisplay = sectionDisplay
        )
    }
}

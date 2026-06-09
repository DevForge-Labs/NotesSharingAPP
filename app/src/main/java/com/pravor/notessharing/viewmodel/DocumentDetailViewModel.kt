package com.pravor.notessharing.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pravor.notessharing.data.DocumentDetailRepository
import com.pravor.notessharing.data.download.DownloadDataStoreManager
import com.pravor.notessharing.data.download.DownloadService
import com.pravor.notessharing.data.download.DownloadTracker
import com.pravor.notessharing.data.download.DownloadForegroundService
import com.pravor.notessharing.model.DocumentDetail
import com.pravor.notessharing.model.toDocumentDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface DocumentDetailUiState {
    data object Loading : DocumentDetailUiState
    data class Error(val message: String) : DocumentDetailUiState
    data class Success(
        val document: DocumentDetail,
        val contributorLevel: String,
        val relatedDocuments: List<DocumentDetail>,
        val isArchived: Boolean = false
    ) : DocumentDetailUiState
}

sealed interface DownloadState {
    data object NotDownloaded : DownloadState
    data class Downloading(val progress: Float) : DownloadState
    data object Downloaded : DownloadState
}

class DocumentDetailViewModel(
    private val repository: DocumentDetailRepository = DocumentDetailRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow<DocumentDetailUiState>(DocumentDetailUiState.Loading)
    val uiState: StateFlow<DocumentDetailUiState> = _uiState.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.NotDownloaded)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    init {
        android.util.Log.d("DETAILS_DEBUG", "DetailsViewModel Created")
    }

    private val upvoteRepository = com.pravor.notessharing.upvotes.UpvoteRepository()
    private val auth = FirebaseAuth.getInstance()

    fun toggleUpvote(itemId: String) {
        val currentUid = auth.currentUser?.uid ?: return
        val doc = (_uiState.value as? DocumentDetailUiState.Success)?.document ?: return
        val col = upvoteRepository.getCollectionForDocType(doc.documentType)

        viewModelScope.launch {
            upvoteRepository.toggleUpvote(
                documentId = itemId,
                collectionName = col,
                currentUpvotes = doc.upvotes,
                userId = currentUid
            )
        }
    }

    fun observeUpvotes(docId: String, docType: String) {
        val currentUid = auth.currentUser?.uid
        viewModelScope.launch {
            if (currentUid != null) {
                upvoteRepository.loadInitialUpvotesIfNeeded(currentUid)
            }
            val col = upvoteRepository.getCollectionForDocType(docType)
            upvoteRepository.observeVisibleDocuments("DetailsScreen", listOf(docId to col))
        }
    }

    fun clearUpvotesObservation() {
        upvoteRepository.observeVisibleDocuments("DetailsScreen", emptyList())
    }

    fun loadDocumentDetail(documentId: String, context: Context) {
        val currentState = _uiState.value
        if (currentState is DocumentDetailUiState.Success && currentState.document.id == documentId) {
            android.util.Log.d("DETAILS_DEBUG", "loadDocumentDetail: Already loaded, skipping fetch")
            return
        }

        android.util.Log.d("DETAILS_DEBUG", "Fetching document data")
        _uiState.value = DocumentDetailUiState.Loading

        val db = DownloadDataStoreManager(context.applicationContext)

        // Observe download status for this document from DataStore & active tracker
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                db.isDocumentDownloadedFlow(documentId),
                DownloadTracker.getDownloadStateFlow(documentId)
            ) { downloaded, trackerState ->
                if (downloaded) {
                    DownloadState.Downloaded
                } else {
                    trackerState
                }
            }.collect { combinedState ->
                _downloadState.value = combinedState
            }
        }

        viewModelScope.launch {
            // Check if document is downloaded
            val isDownloadedLocally = db.isDocumentDownloaded(documentId)
            if (isDownloadedLocally) {
                val attachments = db.getDownloadedAttachments().filter { it.documentId == documentId }
                val allFilesExist = attachments.isNotEmpty() && attachments.all { java.io.File(it.localPath).exists() }
                
                if (allFilesExist) {
                    try {
                        val docDetail = fetchFromFirestore(documentId)
                        if (docDetail != null) {
                            val contributorLevel = repository.getUploaderContributorLevel(docDetail.uploaderId) ?: "Bronze Contributor"
                            val relatedDocs = repository.getRelatedDocuments(docDetail)
                            observeUpvotes(docDetail.id, docDetail.documentType)
                            _uiState.value = DocumentDetailUiState.Success(
                                document = docDetail,
                                contributorLevel = contributorLevel,
                                relatedDocuments = relatedDocs,
                                isArchived = false
                            )
                            return@launch
                        } else {
                            // Document missing from Firestore, but local file exists! Resolve from local DataStore metadata
                            val downloadedDocsList = db.getDownloadedDocuments()
                            val localDoc = downloadedDocsList.find { it.documentId == documentId }
                            if (localDoc != null) {
                                val archivedDocDetail = com.pravor.notessharing.model.DocumentDetail(
                                    id = documentId,
                                    title = localDoc.title.ifBlank { "Archived Download" },
                                    description = "This resource has been removed from the platform but remains available on your device.",
                                    branch = "",
                                    semester = "",
                                    subject = localDoc.subject.ifBlank { "General" },
                                    documentType = localDoc.documentType,
                                    uploaderId = "",
                                    uploaderName = localDoc.uploaderName,
                                    uploaderPhotoUrl = "",
                                    uploadedAt = localDoc.downloadedAt,
                                    downloads = localDoc.downloads,
                                    upvotes = localDoc.upvotes,
                                    bookmarks = 0,
                                    fileUrls = localDoc.fileUrls,
                                    fileSize = 0,
                                    fileExtension = "",
                                    fileType = "pdf",
                                    attachmentCount = localDoc.fileUrls.size,
                                    thumbnailUrl = localDoc.localThumbnailPath ?: localDoc.thumbnailUrl,
                                    thumbnailUrls = if (!localDoc.localThumbnailPath.isNullOrBlank()) listOf(localDoc.localThumbnailPath) else emptyList()
                                )
                                _uiState.value = DocumentDetailUiState.Success(
                                    document = archivedDocDetail,
                                    contributorLevel = localDoc.uploaderContributorLevel,
                                    relatedDocuments = emptyList(),
                                    isArchived = true
                                )
                                return@launch
                            }
                        }
                    } catch (e: Exception) {
                        // Fail silently, fall back to standard flow
                    }
                } else {
                    // Local file missing - clean up DataStore gracefully and show error
                    db.removeDownload(documentId)
                    _uiState.value = DocumentDetailUiState.Error("This download is no longer available on your device.")
                    return@launch
                }
            }

            try {
                val docDetail = fetchFromFirestore(documentId)
                if (docDetail != null) {
                    val contributorLevel = repository.getUploaderContributorLevel(docDetail.uploaderId) ?: "Bronze Contributor"
                    val relatedDocs = repository.getRelatedDocuments(docDetail)
                    observeUpvotes(docDetail.id, docDetail.documentType)
                    _uiState.value = DocumentDetailUiState.Success(
                        document = docDetail,
                        contributorLevel = contributorLevel,
                        relatedDocuments = relatedDocs,
                        isArchived = false
                    )
                } else {
                    _uiState.value = DocumentDetailUiState.Error("Document details not found.")
                }
            } catch (e: Exception) {
                _uiState.value = DocumentDetailUiState.Error(e.localizedMessage ?: "Failed to fetch document details.")
            }
        }
    }

    private suspend fun fetchFromFirestore(documentId: String): DocumentDetail? {
        val collections = listOf("documents", "notes", "pyqs", "assignments", "cheatsheets")
        var targetCol: String? = null
        val firestore = FirebaseFirestore.getInstance()
        var snapshot: com.google.firebase.firestore.DocumentSnapshot? = null
        for (col in collections) {
            try {
                // Try from local cache source first
                val snap = firestore.collection(col).document(documentId).get(com.google.firebase.firestore.Source.CACHE).await()
                if (snap.exists() && snap.data != null) {
                    targetCol = col
                    snapshot = snap
                    break
                }
            } catch (e: Exception) {
                try {
                    val snap = firestore.collection(col).document(documentId).get().await()
                    if (snap.exists() && snap.data != null) {
                        targetCol = col
                        snapshot = snap
                        break
                    }
                } catch (e2: Exception) {
                    // Try next collection
                }
            }
        }

        if (targetCol != null && snapshot != null) {
            val data = snapshot.data
            if (data != null) {
                return data.toDocumentDetail(documentId)
            }
        }
        return repository.getDocument(documentId)
    }

    fun downloadDocument(context: Context) {
        val doc = (_uiState.value as? DocumentDetailUiState.Success)?.document ?: return
        
        // Prevent duplicate downloads: check if already downloading
        if (DownloadTracker.isDownloading(doc.id)) {
            android.util.Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "downloadDocument: Download already active for ID=${doc.id}, ignoring request.")
            return
        }

        // Start Foreground Service to handle persistent download
        val intent = android.content.Intent(context, DownloadForegroundService::class.java).apply {
            action = DownloadForegroundService.ACTION_START_DOWNLOAD
            putExtra(DownloadForegroundService.EXTRA_DOC_ID, doc.id)
            putExtra(DownloadForegroundService.EXTRA_DOC_TITLE, doc.title)
            putExtra(DownloadForegroundService.EXTRA_DOC_TYPE, doc.documentType)
            putExtra(DownloadForegroundService.EXTRA_UPLOADER_ID, doc.uploaderId)
            putStringArrayListExtra(DownloadForegroundService.EXTRA_FILE_URLS, java.util.ArrayList(doc.fileUrls))
        }
        
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
        
        android.util.Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "downloadDocument: Attempting to start download service. docId=${doc.id}, notificationsEnabled=$notificationsEnabled")

        try {
            if (notificationsEnabled) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                    android.util.Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "downloadDocument: Called startForegroundService successfully.")
                } else {
                    context.startService(intent)
                    android.util.Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "downloadDocument: Called startService successfully (API < 26).")
                }
            } else {
                context.startService(intent)
                android.util.Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "downloadDocument: Called startService successfully (Notifications disabled).")
            }
        } catch (e: Exception) {
            android.util.Log.e("DOWNLOAD_NOTIFICATION_DEBUG", "downloadDocument: Failed to start service: ${e.message}. Attempting fallback to startService.", e)
            try {
                context.startService(intent)
                android.util.Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "downloadDocument: Fallback startService succeeded.")
            } catch (fallbackEx: Exception) {
                android.util.Log.e("DOWNLOAD_NOTIFICATION_DEBUG", "downloadDocument: Fallback startService also failed: ${fallbackEx.message}", fallbackEx)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        clearUpvotesObservation()
    }
}

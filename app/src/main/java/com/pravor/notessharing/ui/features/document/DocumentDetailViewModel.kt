package com.pravor.notessharing.ui.features.document

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*

import com.pravor.notessharing.data.repository.BookmarkRepository

import com.pravor.notessharing.data.local.preferences.*

import com.pravor.notessharing.data.service.*

import com.pravor.notessharing.domain.model.*
import com.pravor.notessharing.data.repository.*
import com.pravor.notessharing.core.util.*

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pravor.notessharing.data.repository.DocumentDetailRepository
import com.pravor.notessharing.data.local.preferences.DownloadDataStoreManager
import com.pravor.notessharing.data.service.DownloadService
import com.pravor.notessharing.data.service.DownloadTracker
import com.pravor.notessharing.data.service.DownloadForegroundService
import com.pravor.notessharing.data.service.ShareStorageProvider
import com.pravor.notessharing.domain.model.DocumentDetail
import com.pravor.notessharing.domain.model.toDocumentDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.io.File

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

sealed interface ShareEvent {
    data class Success(val files: List<File>) : ShareEvent
    data class Error(val message: String) : ShareEvent
}

class DocumentDetailViewModel(
    private val repository: DocumentDetailRepository = DocumentDetailRepository(),
    private val storageProvider: ShareStorageProvider? = null
) : ViewModel() {
    private var loadedDocumentId: String? = null

    private val _uiState = MutableStateFlow<DocumentDetailUiState>(DocumentDetailUiState.Loading)
    val uiState: StateFlow<DocumentDetailUiState> = _uiState.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.NotDownloaded)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _shareLoading = MutableStateFlow(false)
    val shareLoading: StateFlow<Boolean> = _shareLoading.asStateFlow()

    private val _shareEvent = MutableSharedFlow<ShareEvent>()
    val shareEvent: SharedFlow<ShareEvent> = _shareEvent.asSharedFlow()

    private val upvoteRepository = com.pravor.notessharing.data.repository.UpvoteRepository()
    private val bookmarkRepository = com.pravor.notessharing.data.repository.BookmarkRepository()
    private val auth = FirebaseAuth.getInstance()

    init {
        android.util.Log.d("DETAILS_DEBUG", "DetailsViewModel Created")

        viewModelScope.launch {
            val currentUid = auth.currentUser?.uid
            if (currentUid != null) {
                bookmarkRepository.loadInitialBookmarksIfNeeded(currentUid)
                upvoteRepository.loadInitialUpvotesIfNeeded(currentUid)
            }
        }

        viewModelScope.launch {
            com.pravor.notessharing.data.repository.BookmarkRepository.bookmarksFlow.collect { bookmarks ->
                val bookmarkedIds = bookmarks.map { it.id }.toSet()
                _uiState.update { current ->
                    if (current is DocumentDetailUiState.Success) {
                        val updatedDoc = current.document.copy(
                            isBookmarked = bookmarkedIds.contains(current.document.id)
                        )
                        val updatedRelated = current.relatedDocuments.map { doc ->
                            doc.copy(isBookmarked = bookmarkedIds.contains(doc.id))
                        }
                        current.copy(
                            document = updatedDoc,
                            relatedDocuments = updatedRelated
                        )
                    } else {
                        current
                    }
                }
            }
        }

        viewModelScope.launch {
            combine(
                com.pravor.notessharing.data.repository.UpvoteRepository.upvotesFlow,
                com.pravor.notessharing.data.repository.UpvoteRepository.upvoteCountsFlow
            ) { upvotesMap, upvoteCountsMap ->
                Pair(upvotesMap, upvoteCountsMap)
            }.collect { (upvotesMap, upvoteCountsMap) ->
                _uiState.update { current ->
                    if (current is DocumentDetailUiState.Success) {
                        val mainId = current.document.id
                        val mainIsUpvoted = upvotesMap[mainId] ?: false
                        val mainUpvotesCount = upvoteCountsMap[mainId] ?: current.document.upvotes
                        
                        val updatedDoc = current.document.copy(
                            isUpvoted = mainIsUpvoted,
                            upvotes = mainUpvotesCount
                        )
                        val updatedRelated = current.relatedDocuments.map { doc ->
                            val rId = doc.id
                            val rIsUpvoted = upvotesMap[rId] ?: false
                            val rUpvotesCount = upvoteCountsMap[rId] ?: doc.upvotes
                            doc.copy(
                                isUpvoted = rIsUpvoted,
                                upvotes = rUpvotesCount
                            )
                        }
                        current.copy(
                            document = updatedDoc,
                            relatedDocuments = updatedRelated
                        )
                    } else {
                        current
                    }
                }
            }
        }

        viewModelScope.launch {
            com.pravor.notessharing.data.repository.UpvoteRepository.downloadCountsFlow.collect { downloadCountsMap ->
                _uiState.update { current ->
                    if (current is DocumentDetailUiState.Success) {
                        val mainId = current.document.id
                        val mainDownloads = downloadCountsMap[mainId] ?: current.document.downloadsCount
                        
                        val updatedDoc = current.document.copy(
                            downloadsCount = mainDownloads
                        )
                        val updatedRelated = current.relatedDocuments.map { doc ->
                            val rId = doc.id
                            val rDownloads = downloadCountsMap[rId] ?: doc.downloadsCount
                            doc.copy(downloadsCount = rDownloads)
                        }
                        current.copy(
                            document = updatedDoc,
                            relatedDocuments = updatedRelated
                        )
                    } else {
                        current
                    }
                }
            }
        }
    }

    fun toggleUpvote(itemId: String) {
        val currentUid = auth.currentUser?.uid ?: return
        val successState = (_uiState.value as? DocumentDetailUiState.Success) ?: return
        
        val (col, currentUpvotes) = if (successState.document.id == itemId) {
            val type = successState.document.documentType
            val col = upvoteRepository.getCollectionForDocType(type)
            Pair(col, successState.document.upvotes)
        } else {
            val relatedDoc = successState.relatedDocuments.find { it.id == itemId } ?: return
            val type = relatedDoc.documentType
            val col = upvoteRepository.getCollectionForDocType(type)
            Pair(col, relatedDoc.upvotes)
        }

        viewModelScope.launch {
            upvoteRepository.toggleUpvote(
                documentId = itemId,
                collectionName = col,
                currentUpvotes = currentUpvotes,
                userId = currentUid
            )
        }
    }

    fun toggleBookmark(itemId: String) {
        val currentUid = auth.currentUser?.uid ?: return
        val successState = (_uiState.value as? DocumentDetailUiState.Success) ?: return
        
        val doc = if (successState.document.id == itemId) {
            successState.document
        } else {
            successState.relatedDocuments.find { it.id == itemId } ?: return
        }

        val wasBookmarked = com.pravor.notessharing.data.repository.BookmarkRepository.bookmarksFlow.value.any { it.id == itemId }

        viewModelScope.launch {
            if (wasBookmarked) {
                bookmarkRepository.removeBookmark(itemId, currentUid)
            } else {
                val docType = doc.documentType.ifBlank { "Notes" }
                val fileType = when (docType.lowercase(java.util.Locale.US)) {
                    "pyq" -> com.pravor.notessharing.domain.model.FileType.Pyq
                    "cheat sheet", "cheatsheet", "cheatsheets" -> com.pravor.notessharing.domain.model.FileType.CheatSheet
                    "assignment" -> com.pravor.notessharing.domain.model.FileType.Notes
                    "video" -> com.pravor.notessharing.domain.model.FileType.Video
                    else -> com.pravor.notessharing.domain.model.FileType.Pdf
                }
                val studyFile = com.pravor.notessharing.domain.model.StudyFile(
                    id = doc.id,
                    title = doc.title,
                    uploadDate = "Saved",
                    fileType = fileType,
                    downloadsCount = doc.downloadsCount,
                    upvotes = doc.upvotes,
                    thumbnailUrl = doc.thumbnailUrl,
                    subject = doc.subject,
                    documentType = docType
                )
                bookmarkRepository.addBookmark(studyFile, currentUid)
            }
        }
    }

    private fun DocumentDetail.withLiveMetadata(): DocumentDetail {
        val bookmarkedIds = com.pravor.notessharing.data.repository.BookmarkRepository.bookmarksFlow.value.map { it.id }.toSet()
        val upvotesMap = com.pravor.notessharing.data.repository.UpvoteRepository.upvotesFlow.value
        val upvoteCountsMap = com.pravor.notessharing.data.repository.UpvoteRepository.upvoteCountsFlow.value
        val downloadCountsMap = com.pravor.notessharing.data.repository.UpvoteRepository.downloadCountsFlow.value

        return this.copy(
            isBookmarked = bookmarkedIds.contains(this.id),
            isUpvoted = upvotesMap[this.id] == true,
            upvotes = upvoteCountsMap[this.id] ?: this.upvotes,
            downloadsCount = downloadCountsMap[this.id] ?: this.downloadsCount
        )
    }

    fun observeUpvotes(docId: String, docType: String, relatedDocuments: List<DocumentDetail> = emptyList()) {
        val currentUid = auth.currentUser?.uid
        viewModelScope.launch {
            if (currentUid != null) {
                upvoteRepository.loadInitialUpvotesIfNeeded(currentUid)
            }
            val mainCol = upvoteRepository.getCollectionForDocType(docType)
            val targets = mutableListOf(docId to mainCol)
            for (doc in relatedDocuments) {
                val col = upvoteRepository.getCollectionForDocType(doc.documentType)
                targets.add(doc.id to col)
            }
            upvoteRepository.observeVisibleDocuments("DetailsScreen_$docId", targets)
        }
    }

    fun clearUpvotesObservation() {
        val docId = loadedDocumentId
        if (docId != null) {
            upvoteRepository.observeVisibleDocuments("DetailsScreen_$docId", emptyList())
        }
    }

    fun loadDocumentDetail(documentId: String, context: Context) {
        loadedDocumentId = documentId
        
        val currentUid = auth.currentUser?.uid
        if (!currentUid.isNullOrBlank()) {
            viewModelScope.launch {
                com.pravor.notessharing.data.repository.ReportRepository.instance.hasUserReported(documentId, currentUid, forceRefresh = true)
            }
        }

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
                            android.util.Log.d("REC_TRACE", "[DOC_VM] 6. Received by ViewModel (cached flow) count=${relatedDocs.size}")
                            observeUpvotes(docDetail.id, docDetail.documentType, relatedDocs)
                            
                            val uiStateToSet = DocumentDetailUiState.Success(
                                document = docDetail.withLiveMetadata(),
                                contributorLevel = contributorLevel,
                                relatedDocuments = relatedDocs.map { it.withLiveMetadata() },
                                isArchived = false
                            )
                            android.util.Log.d("REC_TRACE", "[DOC_VM] 7. Exposed through UI State success (cached flow) count=${uiStateToSet.relatedDocuments.size}")
                            _uiState.value = uiStateToSet
                            return@launch
                        } else {
                            // Document missing from Firestore, but local file exists! Resolve from local DataStore metadata
                            val downloadedDocsList = db.getDownloadedDocuments()
                            val localDoc = downloadedDocsList.find { it.documentId == documentId }
                            if (localDoc != null) {
                                val archivedDocDetail = com.pravor.notessharing.domain.model.DocumentDetail(
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
                                    downloadsCount = localDoc.downloadsCount,
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
                                    document = archivedDocDetail.withLiveMetadata(),
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
                    android.util.Log.d("REC_TRACE", "[DOC_VM] 6. Received by ViewModel count=${relatedDocs.size}")
                    observeUpvotes(docDetail.id, docDetail.documentType, relatedDocs)
                    
                    val uiStateToSet = DocumentDetailUiState.Success(
                        document = docDetail.withLiveMetadata(),
                        contributorLevel = contributorLevel,
                        relatedDocuments = relatedDocs.map { it.withLiveMetadata() },
                        isArchived = false
                    )
                    android.util.Log.d("REC_TRACE", "[DOC_VM] 7. Exposed through UI State success count=${uiStateToSet.relatedDocuments.size}")
                    _uiState.value = uiStateToSet
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
                return data.toDocumentDetail(documentId, targetCol)
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

    fun shareDocument() {
        val successState = (_uiState.value as? DocumentDetailUiState.Success) ?: return
        val doc = successState.document
        val provider = storageProvider ?: return

        if (_shareLoading.value) return

        viewModelScope.launch {
            _shareLoading.value = true
            try {
                val urls = doc.fileUrls
                if (urls.isEmpty()) {
                    throw Exception("No files to share.")
                }

                val preparedFiles = withContext(Dispatchers.IO) {
                    val tempFileList = mutableListOf<File>()
                    for (url in urls) {
                        // 1. Check if downloaded locally in permanent storage
                        val localFile = provider.getDownloadedAttachmentFile(doc.id, url)
                        if (localFile != null && localFile.exists()) {
                            tempFileList.add(localFile)
                            continue
                        }

                        // 2. Check if cached locally in temporary cache
                        val cachedFile = provider.getShareCacheFile(doc.id, url)
                        if (cachedFile.exists() && cachedFile.length() > 0) {
                            tempFileList.add(cachedFile)
                            continue
                        }

                        // 3. Otherwise, download it to the temporary cache
                        DownloadService.downloadFile(url, cachedFile) { _, _ -> }
                        if (cachedFile.exists() && cachedFile.length() > 0) {
                            tempFileList.add(cachedFile)
                        } else {
                            throw Exception("Failed to download attachment.")
                        }
                    }
                    tempFileList
                }

                if (preparedFiles.isNotEmpty()) {
                    _shareEvent.emit(ShareEvent.Success(preparedFiles))
                } else {
                    throw Exception("No attachments found to share.")
                }
            } catch (e: Exception) {
                android.util.Log.e("SHARE_DEBUG", "Error preparing files: ${e.message}", e)
                _shareEvent.emit(ShareEvent.Error(e.localizedMessage ?: "Failed to prepare files for sharing."))
            } finally {
                _shareLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        val docId = loadedDocumentId
        if (docId != null) {
            com.pravor.notessharing.data.repository.ReportRepository.instance.removeReportListener(docId)
        }
        clearUpvotesObservation()
    }

    companion object {
        fun provideFactory(
            context: Context,
            repository: DocumentDetailRepository = DocumentDetailRepository()
        ): androidx.lifecycle.ViewModelProvider.Factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DocumentDetailViewModel(
                    repository = repository,
                    storageProvider = com.pravor.notessharing.data.service.AndroidShareStorageProvider(context.applicationContext)
                ) as T
            }
        }
    }
}

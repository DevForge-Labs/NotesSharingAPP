package com.pravor.notessharing.ui.features.document

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.pravor.notessharing.data.local.preferences.DownloadDataStoreManager
import com.pravor.notessharing.data.repository.BookmarkRepository
import com.pravor.notessharing.data.repository.ContinueLearningRepository
import com.pravor.notessharing.data.repository.RecentlyOpenedRepository
import com.pravor.notessharing.data.repository.UpvoteRepository
import com.pravor.notessharing.data.repository.ViewTrackingRepository
import com.pravor.notessharing.ui.common.states.DocumentErrorState
import com.pravor.notessharing.ui.common.utils.FileSharingUtils
import com.pravor.notessharing.ui.features.document.components.DocumentDetailSkeleton
import com.pravor.notessharing.ui.features.document.components.DocumentDetailSuccessContent
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@Composable
fun DocumentDetailRoute(
    documentId: String,
    onBackClick: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToPdfViewer: (documentId: String, fileUrl: String, title: String) -> Unit,
    onNavigateToImageViewer: (documentId: String, fileUrl: String, title: String) -> Unit,
) {
    val context = LocalContext.current
    val viewModel: DocumentDetailViewModel = viewModel(
        factory = DocumentDetailViewModel.provideFactory(context.applicationContext)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val shareLoading by viewModel.shareLoading.collectAsStateWithLifecycle()

    LaunchedEffect(documentId) {
        viewModel.loadDocumentDetail(documentId, context)
    }

    LaunchedEffect(viewModel) {
        viewModel.shareEvent.collect { event ->
            when (event) {
                is ShareEvent.Success -> {
                    FileSharingUtils.shareFiles(context, event.files)
                }
                is ShareEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is DocumentDetailUiState.Success) {
            val doc = state.document
            RecentlyOpenedRepository(context).saveLastOpened(
                id = doc.id,
                type = "document",
                title = doc.title,
                subject = doc.subject,
                youtubeVideoId = null,
                uploaderName = doc.uploaderName,
                thumbnailUrl = doc.thumbnailUrl,
                thumbnailGenerated = doc.thumbnailGenerated,
                thumbnailType = doc.thumbnailType,
                thumbnailUrls = doc.thumbnailUrls,
                documentType = doc.documentType,
                typeField = doc.documentType,
                examYear = doc.examYear,
                section = doc.section,
                sectionDisplay = doc.sectionDisplay
            )
            ContinueLearningRepository(context).saveLastOpened(
                id = doc.id,
                type = "document",
                title = doc.title,
                subject = doc.subject,
                youtubeVideoId = null,
                uploaderName = doc.uploaderName,
                thumbnailUrl = doc.thumbnailUrl,
                thumbnailGenerated = doc.thumbnailGenerated,
                thumbnailType = doc.thumbnailType,
                thumbnailUrls = doc.thumbnailUrls,
                documentType = doc.documentType,
                typeField = doc.documentType,
                examYear = doc.examYear,
                section = doc.section,
                sectionDisplay = doc.sectionDisplay
            )
            com.pravor.notessharing.core.widget.WidgetUpdateManager.updateAllWidgets(context)
        }
    }

    DocumentDetailScreen(
        documentId = documentId,
        uiState = uiState,
        downloadState = downloadState,
        shareLoading = shareLoading,
        onBackClick = onBackClick,
        onNavigateToDetail = onNavigateToDetail,
        onUpvoteClick = viewModel::toggleUpvote,
        onBookmarkClick = viewModel::toggleBookmark,
        onNavigateToPdfViewer = onNavigateToPdfViewer,
        onNavigateToImageViewer = onNavigateToImageViewer,
        onRetry = { viewModel.loadDocumentDetail(documentId, context) },
        onDownloadClick = { viewModel.downloadDocument(context) },
        onShareClick = { viewModel.shareDocument() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    documentId: String,
    uiState: DocumentDetailUiState,
    downloadState: DownloadState,
    shareLoading: Boolean,
    onBackClick: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onUpvoteClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    onNavigateToPdfViewer: (documentId: String, fileUrl: String, title: String) -> Unit,
    onNavigateToImageViewer: (documentId: String, fileUrl: String, title: String) -> Unit,
    onRetry: () -> Unit,
    onDownloadClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val context = LocalContext.current
    val currentDownloadState = remember { mutableStateOf(downloadState) }
    currentDownloadState.value = downloadState
    
    DisposableEffect(Unit) {
        onDispose {
            if (currentDownloadState.value is DownloadState.Downloading) {
                Toast.makeText(
                    context.applicationContext,
                    "Download will continue in the background.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val bookmarks by BookmarkRepository.bookmarksFlow.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showRemoveBookmarkDialog by remember { mutableStateOf(false) }
    var pendingRemoveUpvoteId by remember { mutableStateOf<String?>(null) }

    val handleUpvoteClick = remember(onUpvoteClick) {
        { itemId: String ->
            val wasUpvoted = UpvoteRepository.upvotesFlow.value[itemId] ?: false
            if (wasUpvoted) {
                pendingRemoveUpvoteId = itemId
            } else {
                onUpvoteClick(itemId)
            }
        }
    }

    LaunchedEffect(currentUid) {
        if (currentUid.isNotEmpty()) {
            BookmarkRepository().loadInitialBookmarksIfNeeded(currentUid)
        }
    }

    val isBookmarked = remember(bookmarks, uiState) {
        val docId = (uiState as? DocumentDetailUiState.Success)?.document?.id ?: ""
        docId.isNotEmpty() && bookmarks.any { it.id == docId }
    }

    val isArchived = remember(uiState) {
        (uiState as? DocumentDetailUiState.Success)?.isArchived ?: false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val titleText = when (uiState) {
                        is DocumentDetailUiState.Success -> {
                            val doc = uiState.document
                            doc.title.ifBlank { doc.subject.ifBlank { "Untitled Document" } }
                        }
                        else -> "Document Details"
                    }
                    Text(
                        text = titleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (isArchived) return@IconButton
                        if (currentUid.isEmpty()) {
                            Toast.makeText(context, "Please sign in to bookmark documents", Toast.LENGTH_SHORT).show()
                            return@IconButton
                        }
                        val doc = (uiState as? DocumentDetailUiState.Success)?.document
                        if (doc != null) {
                            if (isBookmarked) {
                                showRemoveBookmarkDialog = true
                            } else {
                                onBookmarkClick(doc.id)
                            }
                        }
                    }) {
                        Icon(
                            imageVector = if (isBookmarked && !isArchived) Icons.Filled.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (isBookmarked && !isArchived) "Unbookmark Document" else "Bookmark Document",
                            tint = if (isArchived) Color(0xFF94A3B8).copy(alpha = 0.5f)
                                   else if (isBookmarked) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        val stateType = remember(uiState) {
            when (uiState) {
                is DocumentDetailUiState.Loading -> 0
                is DocumentDetailUiState.Error -> 1
                is DocumentDetailUiState.Success -> 2
            }
        }
        Crossfade(
            targetState = stateType,
            label = "document-detail-crossfade",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { type ->
            when (type) {
                0 -> {
                    DocumentDetailSkeleton()
                }
                1 -> {
                    val errorState = uiState as? DocumentDetailUiState.Error
                    if (errorState != null) {
                        val isDeleted = errorState.message.contains("not found", ignoreCase = true)
                        if (isDeleted) {
                            LaunchedEffect(documentId) {
                                val recentRepo = RecentlyOpenedRepository(context)
                                if (recentRepo.getLastOpened()?.id == documentId) {
                                    recentRepo.clearLastOpened()
                                }
                                val contRepo = ContinueLearningRepository(context)
                                if (contRepo.getLastOpened()?.id == documentId) {
                                    contRepo.clearLastOpened()
                                }
                                try {
                                    val downloadManager = DownloadDataStoreManager(context)
                                    val attachments = downloadManager.getDownloadedAttachments()
                                        .filter { it.documentId == documentId }
                                    attachments.forEach { attachment ->
                                        try {
                                            val file = File(attachment.localPath)
                                            if (file.exists()) {
                                                file.delete()
                                            }
                                        } catch (e: Exception) {
                                            // Ignore
                                        }
                                    }
                                    downloadManager.removeDownload(documentId)
                                } catch (e: Exception) {
                                    // Ignore
                                }
                                com.pravor.notessharing.core.widget.WidgetUpdateManager.updateAllWidgets(context)
                            }
                        }
                        DocumentErrorState(
                            onRetry = onRetry,
                            title = if (isDeleted) "Not Available" else "Oops!",
                            message = if (isDeleted) "This resource is no longer available." else errorState.message
                        )
                    }
                }
                2 -> {
                    val successState = uiState as? DocumentDetailUiState.Success
                    if (successState != null) {
                        DocumentDetailSuccessContent(
                            doc = successState.document,
                            contributorLevel = successState.contributorLevel,
                            relatedDocuments = successState.relatedDocuments.filter { doc ->
                                val cleanedTitle = doc.title.trim().lowercase(Locale.ROOT)
                                val cleanedSubject = doc.subject.trim().lowercase(Locale.ROOT)
                                val cleanedDisplaySubject = (doc.displaySubject ?: "").trim().lowercase(Locale.ROOT)
                                
                                val isTitleValid = doc.title.isNotBlank() &&
                                    cleanedTitle != "untitled document" &&
                                    cleanedTitle != "untitled" &&
                                    cleanedTitle != "unknown" &&
                                    cleanedTitle != "placeholder" &&
                                    !cleanedTitle.startsWith("untitled")
                                
                                val isSubjectValid = doc.subject.isNotBlank() &&
                                    cleanedSubject != "unknown" &&
                                    cleanedSubject != "untitled" &&
                                    cleanedSubject != "placeholder" &&
                                    !cleanedSubject.startsWith("untitled")
                                
                                val isDisplaySubjectValid = doc.displaySubject.isNullOrBlank() || (
                                    cleanedDisplaySubject != "unknown" &&
                                    cleanedDisplaySubject != "untitled" &&
                                    cleanedDisplaySubject != "placeholder"
                                )

                                val hasFiles = doc.fileUrls.isNotEmpty() && doc.fileUrls.any { it.isNotBlank() }

                                isTitleValid && isSubjectValid && isDisplaySubjectValid && hasFiles
                            },
                            downloadState = downloadState,
                            shareLoading = shareLoading,
                            onNavigateToDetail = onNavigateToDetail,
                            onDownloadClick = onDownloadClick,
                            onShareClick = { _ -> onShareClick() },
                            onBottomShareClick = onShareClick,
                            onUpvoteClick = handleUpvoteClick,
                            onBookmarkClick = onBookmarkClick,
                            onShowRemoveUpvoteDialog = {
                                pendingRemoveUpvoteId = successState.document.id
                            },
                            onAttachmentClick = { url ->
                                val isPdf = url.contains(".pdf", ignoreCase = true) || url.contains("dummy.pdf")
                                val isImage = url.contains(".jpg", ignoreCase = true) || url.contains(".jpeg", ignoreCase = true) ||
                                              url.contains(".png", ignoreCase = true) || url.contains(".webp", ignoreCase = true) ||
                                              url.contains("unsplash.com", ignoreCase = true)

                                if (isPdf) {
                                    onNavigateToPdfViewer(successState.document.id, url, successState.document.title)
                                } else if (isImage) {
                                    onNavigateToImageViewer(successState.document.id, url, successState.document.title)
                                } else {
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                        context.startActivity(intent)
                                        scope.launch {
                                            ViewTrackingRepository().incrementViewCountDirect(
                                                successState.document.id,
                                                successState.document.collection,
                                                successState.document.documentType
                                            )
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No app available to open this link", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            isArchived = successState.isArchived
                        )
                    }
                }
            }
        }
    }

    if (showRemoveBookmarkDialog) {
        val doc = (uiState as? DocumentDetailUiState.Success)?.document
        AlertDialog(
            onDismissRequest = { showRemoveBookmarkDialog = false },
            title = { Text(text = "Remove this bookmark?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (doc != null && currentUid.isNotEmpty()) {
                            onBookmarkClick(doc.id)
                        }
                        showRemoveBookmarkDialog = false
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRemoveBookmarkDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (pendingRemoveUpvoteId != null) {
        AlertDialog(
            onDismissRequest = { pendingRemoveUpvoteId = null },
            title = { Text(text = "Remove your upvote?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val itemId = pendingRemoveUpvoteId
                        if (itemId != null) {
                            onUpvoteClick(itemId)
                        }
                        pendingRemoveUpvoteId = null
                    }
                ) {
                    Text("Remove Upvote")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingRemoveUpvoteId = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

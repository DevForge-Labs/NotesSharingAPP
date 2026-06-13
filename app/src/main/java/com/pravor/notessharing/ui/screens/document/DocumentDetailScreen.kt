package com.pravor.notessharing.ui.screens.document

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.*
import com.pravor.notessharing.model.FileType
import com.pravor.notessharing.model.StudyFile
import com.pravor.notessharing.bookmarks.BookmarkRepository
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pravor.notessharing.data.RecentlyOpenedRepository
import com.pravor.notessharing.model.DocumentDetail
import com.pravor.notessharing.ui.components.StatePanel
import com.pravor.notessharing.ui.components.utils.getSubjectColor
import com.pravor.notessharing.ui.components.utils.normalizeSubject
import com.pravor.notessharing.ui.navigation.LocalBottomBarPadding
import com.pravor.notessharing.viewmodel.DocumentDetailUiState
import com.pravor.notessharing.viewmodel.DocumentDetailViewModel
import kotlin.math.sin
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath

@Composable
fun DocumentDetailRoute(
    documentId: String,
    onBackClick: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToPdfViewer: (documentId: String, fileUrl: String, title: String) -> Unit,
    onNavigateToImageViewer: (documentId: String, fileUrl: String, title: String) -> Unit,
    viewModel: DocumentDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(documentId) {
        viewModel.loadDocumentDetail(documentId, context)
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
            com.pravor.notessharing.data.ContinueLearningRepository(context).saveLastOpened(
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
            com.pravor.notessharing.widget.WidgetUpdateManager.updateAllWidgets(context)
        }
    }

    DocumentDetailScreen(
        documentId = documentId,
        uiState = uiState,
        downloadState = downloadState,
        onBackClick = onBackClick,
        onNavigateToDetail = onNavigateToDetail,
        onUpvoteClick = viewModel::toggleUpvote,
        onBookmarkClick = viewModel::toggleBookmark,
        onNavigateToPdfViewer = onNavigateToPdfViewer,
        onNavigateToImageViewer = onNavigateToImageViewer,
        onRetry = { viewModel.loadDocumentDetail(documentId, context) },
        onDownloadClick = { viewModel.downloadDocument(context) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    documentId: String,
    uiState: DocumentDetailUiState,
    downloadState: com.pravor.notessharing.viewmodel.DownloadState,
    onBackClick: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onUpvoteClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    onNavigateToPdfViewer: (documentId: String, fileUrl: String, title: String) -> Unit,
    onNavigateToImageViewer: (documentId: String, fileUrl: String, title: String) -> Unit,
    onRetry: () -> Unit,
    onDownloadClick: () -> Unit
) {
    android.util.Log.d("DETAILS_DEBUG", "DetailsScreen Composed")
    val context = LocalContext.current
    val currentDownloadState = remember { mutableStateOf(downloadState) }
    currentDownloadState.value = downloadState
    
    DisposableEffect(Unit) {
        onDispose {
            if (currentDownloadState.value is com.pravor.notessharing.viewmodel.DownloadState.Downloading) {
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
            val wasUpvoted = com.pravor.notessharing.upvotes.UpvoteRepository.upvotesFlow.value[itemId] ?: false
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
                        style = MaterialTheme.typography.titleLarge, // Increased size for better readability
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
                                val contRepo = com.pravor.notessharing.data.ContinueLearningRepository(context)
                                if (contRepo.getLastOpened()?.id == documentId) {
                                    contRepo.clearLastOpened()
                                }
                                try {
                                    val downloadManager = com.pravor.notessharing.data.download.DownloadDataStoreManager(context)
                                    val attachments = downloadManager.getDownloadedAttachments()
                                        .filter { it.documentId == documentId }
                                    attachments.forEach { attachment ->
                                        try {
                                            val file = java.io.File(attachment.localPath)
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
                                com.pravor.notessharing.widget.WidgetUpdateManager.updateAllWidgets(context)
                            }
                        }
                        com.pravor.notessharing.ui.components.states.DocumentErrorState(
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
                            relatedDocuments = successState.relatedDocuments,
                            downloadState = downloadState,
                            onNavigateToDetail = onNavigateToDetail,
                            onDownloadClick = onDownloadClick,
                            onShareClick = { fileUrl ->
                                // TODO: Implement file sharing flow
                                Toast.makeText(
                                    context,
                                    "Share functionality coming soon",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
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
                                    android.util.Log.d("PDF_DEBUG", "Opening PDF")
                                    android.util.Log.d("PDF_DEBUG", "DocumentId=${successState.document.id}")
                                    android.util.Log.d("PDF_DEBUG", "FileUrl=$url")
                                    onNavigateToPdfViewer(successState.document.id, url, successState.document.title)
                                } else if (isImage) {
                                    onNavigateToImageViewer(successState.document.id, url, successState.document.title)
                                } else {
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                        context.startActivity(intent)
                                        scope.launch {
                                            com.pravor.notessharing.data.ViewTrackingRepository().incrementViewCountDirect(
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

@Composable
fun DocumentDetailSuccessContent(
    doc: DocumentDetail,
    contributorLevel: String,
    relatedDocuments: List<DocumentDetail>,
    downloadState: com.pravor.notessharing.viewmodel.DownloadState,
    onNavigateToDetail: (String) -> Unit,
    onDownloadClick: () -> Unit,
    onShareClick: (String) -> Unit,
    onUpvoteClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    onShowRemoveUpvoteDialog: () -> Unit,
    onAttachmentClick: (String) -> Unit,
    isArchived: Boolean = false
) {
    val bottomPadding = LocalBottomBarPadding.current
    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (isArchived) {
            item(key = "archived-warning-banner") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF94A3B8).copy(alpha = 0.12f)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF94A3B8).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Archived",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(28.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Archived Download",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                )
                            )
                            Text(
                                text = "This resource has been removed from NotesSharing but remains available on your device.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFF64748B)
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. DOCUMENT PREVIEW SECTION (Mixed adapters inside preview section handles horizontal/vertical margins)
        item(key = "preview-section") {
            AttachmentPreviewSection(
                doc = doc,
                onDownloadClick = { url -> onDownloadClick() },
                onShareClick = onShareClick,
                onAttachmentClick = onAttachmentClick,
                modifier = Modifier // No horizontal padding here so images scroll edge-to-edge
            )
        }

        // Redesigned Metadata Card
        item(key = "redesigned-metadata-card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val rawDocType = doc.documentType.lowercase(java.util.Locale.ROOT).trim()
                    val docTypeTag = when (rawDocType) {
                        "pyq" -> if (!doc.examYear.isNullOrBlank()) "PYQ | ${doc.examYear}" else "PYQ"
                        "assignment" -> {
                            val sec = doc.sectionDisplay ?: doc.section
                            if (!sec.isNullOrBlank()) "ASSIGNMENT | $sec" else "ASSIGNMENT"
                        }
                        "cheatsheet", "cheat sheet" -> "CHEAT SHEET"
                        else -> doc.documentType.uppercase(java.util.Locale.ROOT)
                    }

                    val accentColor = when (rawDocType) {
                        "pyq" -> Color(0xFFE57373) // Softer Red
                        "assignment" -> Color(0xFF81C784) // Softer Green
                        "cheatsheet", "cheat sheet" -> Color(0xFFFFD54F) // Softer Amber
                        else -> Color(0xFF64B5F6) // Softer Blue
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = docTypeTag,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    fontSize = 12.sp,
                                    color = accentColor
                                )
                            )
                        }
                    }

                    val subjectSemesterText = remember(doc.subject, doc.semester) {
                        buildString {
                            append(doc.subject)
                            if (doc.semester.isNotBlank()) {
                                append(" | ")
                                append(doc.semester)
                            }
                        }
                    }
                    Text(
                        text = subjectSemesterText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }

        // Redesigned Action Card
        item(key = "redesigned-action-card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NotesSharingDownloadIndicator(
                        downloadState = if (isArchived) com.pravor.notessharing.viewmodel.DownloadState.Downloaded else downloadState,
                        onClick = {
                            if (!isArchived) {
                                onDownloadClick()
                            }
                        }
                    )

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .width(2.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    )

                    // Interactive Upvote Button Section
                    UpvoteButtonSection(
                        docId = doc.id,
                        initialUpvotes = doc.upvotes,
                        currentUid = currentUid,
                        onUpvoteClick = onUpvoteClick,
                        onShowRemoveDialog = onShowRemoveUpvoteDialog,
                        enabled = !isArchived
                    )
                }
            }
        }

        // Description if present
        if (doc.description.isNotBlank()) {
            item(key = "description-section") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = doc.description,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 4. UPLOADER DETAILS SECTION
        item(key = "uploader-section") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(modifier = Modifier.padding(18.dp)) {
                    UploaderInfoCard(
                        uploaderName = doc.uploaderName,
                        uploaderPhotoUrl = doc.uploaderPhotoUrl,
                        contributorLevel = contributorLevel,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 5. RELATED DOCUMENTS
        android.util.Log.d("REC_TRACE", "[DOC_UI] Composable render: isArchived=$isArchived relatedDocumentsCount=${relatedDocuments.size}")
        if (!isArchived && relatedDocuments.isNotEmpty()) {
            item(key = "related-section") {
                RelatedDocumentsSection(
                    relatedDocuments = relatedDocuments,
                    onNavigateToDetail = onNavigateToDetail,
                    onBookmarkClick = onBookmarkClick,
                    onUpvoteClick = onUpvoteClick
                )
            }
        }
    }
}

@Composable
fun UpvoteButtonSection(
    docId: String,
    initialUpvotes: Int,
    currentUid: String,
    onUpvoteClick: (String) -> Unit,
    onShowRemoveDialog: () -> Unit,
    enabled: Boolean = true
) {
    val upvotesMap by com.pravor.notessharing.upvotes.UpvoteRepository.upvotesFlow.collectAsStateWithLifecycle()
    val upvoteCountsMap by com.pravor.notessharing.upvotes.UpvoteRepository.upvoteCountsFlow.collectAsStateWithLifecycle()

    val isUpvoted = remember(upvotesMap, docId) {
        upvotesMap[docId] == true
    }
    val upvoteCount = remember(upvoteCountsMap, docId) {
        upvoteCountsMap[docId] ?: initialUpvotes
    }

    val context = LocalContext.current
    val upvoteColor = if (!enabled) {
        Color(0xFF94A3B8)
    } else if (isUpvoted) {
        Color(0xFFFFB74D)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) {
                if (currentUid.isEmpty()) {
                    Toast.makeText(context, "Please sign in to upvote", Toast.LENGTH_SHORT).show()
                    return@clickable
                }
                if (isUpvoted) {
                    onShowRemoveDialog()
                } else {
                    onUpvoteClick(docId)
                }
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ThumbUp,
                contentDescription = "Upvote",
                tint = upvoteColor,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "$upvoteCount",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = upvoteColor
                )
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isUpvoted) "Upvoted" else "Upvote",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = upvoteColor
            )
        )
    }
}

@Composable
fun DocumentDetailSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(24.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(8.dp))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(28.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(6.dp))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(16.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(4.dp))
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(16.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(4.dp))
                        )
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(12.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .width(100.dp)
                .height(20.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(4.dp))
        )

        repeat(2) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(16.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(4.dp))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.3f)
                                .height(12.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun NotesSharingDownloadIndicator(
    downloadState: com.pravor.notessharing.viewmodel.DownloadState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (downloadState as? com.pravor.notessharing.viewmodel.DownloadState.Downloading)?.progress
    val isDownloaded = downloadState is com.pravor.notessharing.viewmodel.DownloadState.Downloaded

    // Completion / Transition states
    var completionPhase by remember { mutableStateOf(CompletionPhase.NOT_STARTED) }

    LaunchedEffect(isDownloaded) {
        if (isDownloaded) {
            if (completionPhase == CompletionPhase.NOT_STARTED) {
                completionPhase = CompletionPhase.SETTLING
                kotlinx.coroutines.delay(500) // Settling time for liquid wave
                completionPhase = CompletionPhase.RIPPLE
                kotlinx.coroutines.delay(400) // Ripple / pulse duration
                completionPhase = CompletionPhase.FINISHED
            }
        } else {
            completionPhase = CompletionPhase.NOT_STARTED
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "indicator-animations")
    
    // 1. Wave phase animation for liquid wave translation
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave-phase"
    )

    // 2. Continuous breathing animation for checkmark & glow in completed state
    val breathingValue by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing-glow"
    )

    // 3. Shimmer sweep phase that triggers every 3 seconds for subtle repeating sweep on checkmark
    val shimmerPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                0.0f at 0
                0.0f at 1800
                1.0f at 2600
                1.0f at 3000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer-phase"
    )

    // 4. Smooth liquid level rising progress representation
    val smoothProgress by animateFloatAsState(
        targetValue = if (isDownloaded) 1f else (progress ?: 0f),
        animationSpec = tween(500, easing = EaseOutQuad),
        label = "smooth-progress"
    )

    // 5. Wave amplitude decay representation (calm waves: 2.dp base intensity)
    val targetAmplitude = when {
        isDownloaded && completionPhase == CompletionPhase.SETTLING -> 0f
        isDownloaded && completionPhase == CompletionPhase.NOT_STARTED -> 2f // in dp
        progress != null -> 2f // in dp
        else -> 0f
    }
    val waveAmplitude by animateFloatAsState(
        targetValue = targetAmplitude,
        animationSpec = tween(500, easing = EaseOutQuad),
        label = "wave-amplitude"
    )

    // 6. Drawn checkmark animation progress
    val checkDrawProgress by animateFloatAsState(
        targetValue = if (completionPhase == CompletionPhase.FINISHED || completionPhase == CompletionPhase.RIPPLE) 1f else 0f,
        animationSpec = tween(600, easing = EaseOutCubic),
        label = "check-draw-progress"
    )

    // Success bounce scale animation
    var triggerPulse by remember { mutableStateOf(false) }
    LaunchedEffect(isDownloaded) {
        if (isDownloaded) {
            triggerPulse = true
        }
    }
    val successScale by animateFloatAsState(
        targetValue = if (triggerPulse) 1.15f else 1.0f,
        animationSpec = keyframes {
            durationMillis = 600
            1.0f at 0
            1.15f at 180
            0.95f at 380
            1.0f at 600
        },
        finishedListener = { triggerPulse = false },
        label = "success-scale"
    )

    // Ripple expansion progress
    val rippleProgress by animateFloatAsState(
        targetValue = if (completionPhase == CompletionPhase.RIPPLE) 1f else 0f,
        animationSpec = tween(400, easing = EaseOutCubic),
        label = "ripple-progress"
    )

    // Glow intensity animation
    val glowAlpha by animateFloatAsState(
        targetValue = if (completionPhase == CompletionPhase.RIPPLE) 1f else 0f,
        animationSpec = keyframes {
            durationMillis = 400
            0f at 0
            1f at 150
            0f at 400
        },
        label = "glow-alpha"
    )

    val accentColor = MaterialTheme.colorScheme.primary
    val bubbleBgColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    val glassBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(bubbleBgColor)
            .clickable(enabled = progress == null && !isDownloaded) { onClick() }
            .graphicsLayer {
                scaleX = successScale
                scaleY = successScale
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val strokeWidth = 2.dp.toPx()
            val radius = (w - strokeWidth) / 2f
            val centerX = w / 2f
            val centerY = h / 2f

            // Perfect circular bubble path
            val circlePath = Path().apply {
                addOval(
                    Rect(
                        center = Offset(centerX, centerY),
                        radius = radius
                    )
                )
            }

            if (completionPhase == CompletionPhase.FINISHED) {
                // 1. Keep the liquid-filled circle completely full (Do NOT drain or remove the liquid)
                drawPath(
                    path = circlePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accentColor,
                            accentColor.copy(alpha = 0.7f)
                        )
                    )
                )

                // 2. Draw outer glowing boundary
                drawPath(
                    path = circlePath,
                    color = accentColor.copy(alpha = 0.4f + 0.3f * breathingValue),
                    style = Stroke(width = strokeWidth)
                )

                // 3. Draw animated stroked checkmark inside the circle in White (visible against the accent-color liquid background)
                val startX = w * 0.32f
                val startY = h * 0.50f
                val midX = w * 0.46f
                val midY = h * 0.64f
                val endX = w * 0.68f
                val endY = h * 0.36f

                val checkPath = Path().apply {
                    moveTo(startX, startY)
                    lineTo(midX, midY)
                    lineTo(endX, endY)
                }

                // Shimmer sweep effect linear gradient on the white checkmark
                val checkColor = Color.White.copy(alpha = 0.8f + 0.2f * breathingValue)
                val shimmerColor = Color.White
                
                val stop1 = (shimmerPhase - 0.15f).coerceIn(0f, 1f)
                val stop2 = shimmerPhase.coerceIn(0f, 1f)
                val stop3 = (shimmerPhase + 0.15f).coerceIn(0f, 1f)

                val checkBrush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to checkColor,
                        stop1 to checkColor,
                        stop2 to shimmerColor,
                        stop3 to checkColor,
                        1.0f to checkColor
                    ),
                    start = Offset(w * 0.2f, h * 0.2f),
                    end = Offset(w * 0.8f, h * 0.8f)
                )

                drawPath(
                    path = checkPath,
                    brush = checkBrush,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            } else if (completionPhase == CompletionPhase.RIPPLE || completionPhase == CompletionPhase.SETTLING) {
                // 1. Draw container background
                drawPath(
                    path = circlePath,
                    color = bubbleBgColor
                )

                // 2. Draw clipping waving liquid path (fully filled but settling)
                clipPath(circlePath) {
                    val baseLevelY = centerY - radius
                    val wavePath = Path().apply {
                        moveTo(0f, h)
                        lineTo(0f, baseLevelY)
                        
                        val segments = 40
                        val segmentWidth = w / segments
                        val amplitudePx = waveAmplitude.dp.toPx()
                        val frequency = 2 * Math.PI.toFloat() / w
                        
                        for (i in 0..segments) {
                            val x = i * segmentWidth
                            val y = baseLevelY + sin(x * frequency + wavePhase) * amplitudePx
                            lineTo(x, y)
                        }
                        
                        lineTo(w, h)
                        close()
                    }

                    drawPath(
                        path = wavePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                accentColor,
                                accentColor.copy(alpha = 0.7f)
                            )
                        )
                    )
                }

                // 3. Draw container circular glass boundary
                drawPath(
                    path = circlePath,
                    color = glassBorderColor,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // 4. Draw ripple expanding ring
                if (completionPhase == CompletionPhase.RIPPLE) {
                    val rippleRadius = radius * 0.6f + (radius * 0.6f * rippleProgress)
                    val rippleAlpha = (1f - rippleProgress) * 0.8f
                    drawCircle(
                        color = accentColor,
                        radius = rippleRadius,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = 3.dp.toPx() * (1f - rippleProgress)),
                        alpha = rippleAlpha
                    )
                }

                // 5. Draw radial glow
                if (glowAlpha > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.4f * glowAlpha),
                                Color.Transparent
                            ),
                            center = Offset(centerX, centerY),
                            radius = radius * 1.3f
                        )
                    )
                }

                // 6. Draw partial checkmark as it starts revealing (in White against the liquid background)
                if (checkDrawProgress > 0f) {
                    val startX = w * 0.32f
                    val startY = h * 0.50f
                    val midX = w * 0.46f
                    val midY = h * 0.64f
                    val endX = w * 0.68f
                    val endY = h * 0.36f

                    val checkPath = Path().apply {
                        moveTo(startX, startY)
                        if (checkDrawProgress <= 0.4f) {
                            val fraction = checkDrawProgress / 0.4f
                            lineTo(
                                startX + (midX - startX) * fraction,
                                startY + (midY - startY) * fraction
                            )
                        } else {
                            lineTo(midX, midY)
                            val fraction = (checkDrawProgress - 0.4f) / 0.6f
                            lineTo(
                                midX + (endX - midX) * fraction,
                                midY + (endY - midY) * fraction
                            )
                        }
                    }

                    drawPath(
                        path = checkPath,
                        color = Color.White,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            } else if (progress != null) {
                // DYNAMIC DOWNLOADING STATE: Circular container with waving liquid rising
                drawPath(
                    path = circlePath,
                    color = bubbleBgColor
                )

                clipPath(circlePath) {
                    val baseLevelY = centerY + radius - (2 * radius * smoothProgress)
                    val wavePath = Path().apply {
                        moveTo(0f, h)
                        lineTo(0f, baseLevelY)
                        
                        val segments = 40
                        val segmentWidth = w / segments
                        val amplitudePx = waveAmplitude.dp.toPx()
                        val frequency = 2 * Math.PI.toFloat() / w
                        
                        for (i in 0..segments) {
                            val x = i * segmentWidth
                            val y = baseLevelY + sin(x * frequency + wavePhase) * amplitudePx
                            lineTo(x, y)
                        }
                        
                        lineTo(w, h)
                        close()
                    }

                    drawPath(
                        path = wavePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                accentColor,
                                accentColor.copy(alpha = 0.7f)
                            )
                        )
                    )
                }

                drawPath(
                    path = circlePath,
                    color = glassBorderColor,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            } else {
                // IDLE DOWNLOAD STATE
                drawPath(
                    path = circlePath,
                    color = bubbleBgColor
                )
                drawPath(
                    path = circlePath,
                    color = glassBorderColor,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        // Overlay text or icon centered in the circle
        if (completionPhase == CompletionPhase.FINISHED) {
            // Drawn directly on Canvas
        } else if (completionPhase == CompletionPhase.RIPPLE || completionPhase == CompletionPhase.SETTLING) {
            // Drawn on Canvas
        } else if (progress != null) {
            Text(
                text = "${(smoothProgress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (smoothProgress > 0.55f) Color.White else MaterialTheme.colorScheme.onSurface
                )
            )
        } else {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Download All",
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

enum class CompletionPhase {
    NOT_STARTED,
    SETTLING,
    RIPPLE,
    FINISHED
}

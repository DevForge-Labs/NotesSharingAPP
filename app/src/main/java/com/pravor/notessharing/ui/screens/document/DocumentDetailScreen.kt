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
    val context = LocalContext.current

    LaunchedEffect(documentId) {
        viewModel.loadDocumentDetail(documentId)
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
        }
    }

    DocumentDetailScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onNavigateToDetail = onNavigateToDetail,
        onUpvoteClick = viewModel::toggleUpvote,
        onNavigateToPdfViewer = onNavigateToPdfViewer,
        onNavigateToImageViewer = onNavigateToImageViewer
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    uiState: DocumentDetailUiState,
    onBackClick: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onUpvoteClick: (String) -> Unit,
    onNavigateToPdfViewer: (documentId: String, fileUrl: String, title: String) -> Unit,
    onNavigateToImageViewer: (documentId: String, fileUrl: String, title: String) -> Unit
) {
    android.util.Log.d("DETAILS_DEBUG", "DetailsScreen Composed")
    val context = LocalContext.current
    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val bookmarks by BookmarkRepository.bookmarksFlow.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showRemoveBookmarkDialog by remember { mutableStateOf(false) }
    var pendingRemoveUpvoteId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUid) {
        if (currentUid.isNotEmpty()) {
            BookmarkRepository().loadInitialBookmarksIfNeeded(currentUid)
        }
    }

    val isBookmarked = remember(bookmarks, uiState) {
        val docId = (uiState as? DocumentDetailUiState.Success)?.document?.id ?: ""
        docId.isNotEmpty() && bookmarks.any { it.id == docId }
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
                        if (currentUid.isEmpty()) {
                            Toast.makeText(context, "Please sign in to bookmark documents", Toast.LENGTH_SHORT).show()
                            return@IconButton
                        }
                        val doc = (uiState as? DocumentDetailUiState.Success)?.document
                        if (doc != null) {
                            if (isBookmarked) {
                                showRemoveBookmarkDialog = true
                            } else {
                                scope.launch {
                                    val bookmarkRepository = BookmarkRepository()
                                    val fileTypeEnum = when (doc.documentType.lowercase(java.util.Locale.US).replace(" ", "")) {
                                        "pyq" -> FileType.Pyq
                                        "cheatsheet", "cheat sheet" -> FileType.CheatSheet
                                        "assignment" -> FileType.Notes
                                        "notes" -> FileType.Notes
                                        else -> FileType.Pdf
                                    }
                                    val studyFile = StudyFile(
                                        id = doc.id,
                                        title = doc.title,
                                        uploadDate = "Saved",
                                        fileType = fileTypeEnum,
                                        downloads = doc.downloads,
                                        upvotes = doc.upvotes,
                                        thumbnailUrl = doc.thumbnailUrl,
                                        subject = doc.subject,
                                        documentType = doc.documentType
                                    )
                                    bookmarkRepository.addBookmark(studyFile, currentUid)
                                }
                            }
                        }
                    }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (isBookmarked) "Unbookmark Document" else "Bookmark Document",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
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
        Crossfade(
            targetState = uiState,
            label = "document-detail-crossfade",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { state ->
            when (state) {
                DocumentDetailUiState.Loading -> {
                    DocumentDetailSkeleton()
                }

                is DocumentDetailUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        StatePanel(
                            title = "Unable to load document",
                            message = state.message
                        )
                    }
                }

                is DocumentDetailUiState.Success -> {
                    DocumentDetailSuccessContent(
                        doc = state.document,
                        contributorLevel = state.contributorLevel,
                        relatedDocuments = state.relatedDocuments,
                        onNavigateToDetail = onNavigateToDetail,
                        onDownloadClick = { fileUrl ->
                            // TODO: Implement file download flow
                            Toast.makeText(
                                context,
                                "Download functionality coming soon",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onShareClick = { fileUrl ->
                            // TODO: Implement file sharing flow
                            Toast.makeText(
                                context,
                                "Share functionality coming soon",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onUpvoteClick = onUpvoteClick,
                        onShowRemoveUpvoteDialog = {
                            pendingRemoveUpvoteId = state.document.id
                        },
                        onAttachmentClick = { url ->
                            val isPdf = url.contains(".pdf", ignoreCase = true) || url.contains("dummy.pdf")
                            val isImage = url.contains(".jpg", ignoreCase = true) || url.contains(".jpeg", ignoreCase = true) ||
                                          url.contains(".png", ignoreCase = true) || url.contains(".webp", ignoreCase = true) ||
                                          url.contains("unsplash.com", ignoreCase = true)

                            if (isPdf) {
                                android.util.Log.d("PDF_DEBUG", "Opening PDF")
                                android.util.Log.d("PDF_DEBUG", "DocumentId=${state.document.id}")
                                android.util.Log.d("PDF_DEBUG", "FileUrl=$url")
                                onNavigateToPdfViewer(state.document.id, url, state.document.title)
                            } else if (isImage) {
                                onNavigateToImageViewer(state.document.id, url, state.document.title)
                            } else {
                                Toast.makeText(context, "Preview not supported yet", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
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
                            scope.launch {
                                BookmarkRepository().removeBookmark(doc.id, currentUid)
                            }
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
            title = { Text(text = "Remove this upvote?") },
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
                    Text("Remove")
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
    onNavigateToDetail: (String) -> Unit,
    onDownloadClick: (String) -> Unit,
    onShareClick: (String) -> Unit,
    onUpvoteClick: (String) -> Unit,
    onShowRemoveUpvoteDialog: () -> Unit,
    onAttachmentClick: (String) -> Unit
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
        // 3. DOCUMENT PREVIEW SECTION (Mixed adapters inside preview section handles horizontal/vertical margins)
        item(key = "preview-section") {
            AttachmentPreviewSection(
                doc = doc,
                onDownloadClick = onDownloadClick,
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
                    // Download All Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                doc.fileUrls.forEach { url ->
                                    onDownloadClick(url)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download All",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Download All",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

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
                        onShowRemoveDialog = onShowRemoveUpvoteDialog
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
        if (relatedDocuments.isNotEmpty()) {
            item(key = "related-section") {
                RelatedDocumentsSection(
                    relatedDocuments = relatedDocuments,
                    onNavigateToDetail = onNavigateToDetail
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
    onShowRemoveDialog: () -> Unit
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
    val upvoteColor = if (isUpvoted) Color(0xFFFFB74D) else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable {
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

package com.pravor.notessharing.ui.screens.document

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.pravor.notessharing.viewmodel.DocumentDetailUiState
import com.pravor.notessharing.viewmodel.DocumentDetailViewModel

@Composable
fun DocumentDetailRoute(
    documentId: String,
    onBackClick: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
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
        onNavigateToDetail = onNavigateToDetail
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    uiState: DocumentDetailUiState,
    onBackClick: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val context = LocalContext.current

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
                        // TODO: Implement document bookmarking/saving state toggle flow in Firestore
                        Toast.makeText(context, "Bookmark functionality coming soon", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = "Bookmark Document")
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
                            Toast.makeText(context, "Download functionality coming soon", Toast.LENGTH_SHORT).show()
                        },
                        onShareClick = { fileUrl ->
                            // TODO: Implement file sharing flow
                            Toast.makeText(context, "Share functionality coming soon", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentDetailSuccessContent(
    doc: DocumentDetail,
    contributorLevel: String,
    relatedDocuments: List<DocumentDetail>,
    onNavigateToDetail: (String) -> Unit,
    onDownloadClick: (String) -> Unit,
    onShareClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. SUBJECT SECTION (Pill/Chip visual with Upvotes on the far right)
        item(key = "subject-section") {
            if (doc.subject.isNotBlank()) {
                val normalized = remember(doc.subject) { normalizeSubject(doc.subject) }
                val subjectColor = remember(normalized) { getSubjectColor(normalized) }
                val displayColor = if (subjectColor == Color(0xFF78909C)) {
                    MaterialTheme.colorScheme.primary
                } else {
                    subjectColor
                }
                // Interactive Upvote State (Local UI Feedback keyed by document ID)
                var isUpvoted by remember(doc.id) { mutableStateOf(false) }
                val upvoteCount = if (isUpvoted) 1 else 0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp), // Compensate for the clickable upvote Row's horizontal padding
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Subject Chip
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = displayColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, displayColor.copy(alpha = 0.5f)),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = displayColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = doc.subject,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = displayColor
                                )
                            )
                        }
                    }

                    // Interactive Upvote Indicator (Far Right)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                isUpvoted = !isUpvoted
                                // Future ready action integration:
                                // onUpvoteClick(doc.id)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val indicatorColor = if (isUpvoted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = "Upvotes",
                            tint = indicatorColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "$upvoteCount",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = indicatorColor
                            )
                        )
                    }
                }
            }
        }

        // 2. DOCUMENT INFO SECTION (Redesigned metadata chips)
        item(key = "metadata-section") {
            DocumentMetadataSection(
                doc = doc,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 3. DOCUMENT PREVIEW SECTION (Mixed adapters inside preview section handles horizontal/vertical margins)
        item(key = "preview-section") {
            AttachmentPreviewSection(
                doc = doc,
                onDownloadClick = onDownloadClick,
                onShareClick = onShareClick,
                modifier = Modifier // No horizontal padding here so images scroll edge-to-edge
            )
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

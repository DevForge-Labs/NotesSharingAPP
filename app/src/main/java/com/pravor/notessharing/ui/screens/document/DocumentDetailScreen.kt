package com.pravor.notessharing.ui.screens.document

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pravor.notessharing.data.RecentlyOpenedRepository
import com.pravor.notessharing.model.DocumentDetail
import com.pravor.notessharing.ui.components.StatePanel
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
                thumbnailType = doc.thumbnailType
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
                        is DocumentDetailUiState.Success -> uiState.document.title
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
        // 1. DOCUMENT PREVIEW SECTION (Mixed adapters inside preview section handles horizontal/vertical margins)
        item(key = "preview-section") {
            AttachmentPreviewSection(
                doc = doc,
                onDownloadClick = onDownloadClick,
                onShareClick = onShareClick,
                modifier = Modifier // No horizontal padding here so images scroll edge-to-edge
            )
        }

        // 2. DOCUMENT INFO SECTION
        item(key = "metadata-section") {
            DocumentMetadataSection(
                doc = doc,
                contributorLevel = contributorLevel,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Description if present
        if (doc.description.isNotBlank()) {
            item(key = "description-section") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = doc.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 3. RELATED DOCUMENTS
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

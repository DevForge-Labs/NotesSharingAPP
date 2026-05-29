package com.pravor.notessharing.ui.screens.explore

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pravor.notessharing.model.FeedItem
import com.pravor.notessharing.model.FileType
import com.pravor.notessharing.model.TrendingNote
import com.pravor.notessharing.state.ExploreUiState
import com.pravor.notessharing.ui.components.StatePanel
import com.pravor.notessharing.ui.components.home_components.HomeFeedCard
import com.pravor.notessharing.viewmodel.ExploreViewModel

@Composable
fun TrendingNotesRoute(
    onBackClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    viewModel: ExploreViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TrendingNotesScreen(uiState = uiState, onBackClick = onBackClick, onDocumentClick = onDocumentClick)
}

@Composable
fun TrendingNotesScreen(
    uiState: ExploreUiState,
    onBackClick: () -> Unit,
    onDocumentClick: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val repository = remember { com.pravor.notessharing.data.UploadRepository(context) }
    var selectedUploadForViewer by remember { mutableStateOf<com.pravor.notessharing.ui.components.UploadViewerData?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            ExploreUiState.Loading -> StatePanel("Finding topics", "Scanning dummy campus trends", loading = true)
            ExploreUiState.Empty -> StatePanel("Nothing trending", "Explore content will appear here")
            is ExploreUiState.Error -> StatePanel("Explore failed", uiState.message)
            is ExploreUiState.Success -> ExploreExpandedListScaffold(
                title = "Trending Notes",
                onBackClick = onBackClick
            ) {
                items(uiState.content.trendingNotes, key = { it.id }, contentType = { "trending-note" }) { note ->
                    HomeFeedCard(
                        item = note.toFeedItem(),
                        onClick = { onDocumentClick(note.id) },
                        onUpvoteClick = {},
                        onBookmarkClick = {}
                    )
                }
            }
        }

        selectedUploadForViewer?.let { viewerData ->
            com.pravor.notessharing.ui.components.GroupedUploadViewerDialog(
                title = viewerData.title,
                fileUrls = viewerData.fileUrls,
                onDismiss = { selectedUploadForViewer = null },
                onFileClick = { url ->
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            )
        }
    }
}

private fun TrendingNote.toFeedItem(): FeedItem {
    val initials = subject
        .split(" ")
        .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
        .take(2)
        .joinToString("")
        .ifBlank { "TN" }

    return FeedItem(
        id = id,
        uploaderName = subject,
        uploaderInitials = initials,
        uploadDate = "Trending now",
        title = title,
        description = "$subject notes rated ${String.format("%.1f", rating)} by students.",
        tags = listOf(subject, "Trending"),
        fileType = FileType.Notes,
        upvotes = upvotes,
        comments = 0,
        downloads = downloads,
        isUpvoted = false,
        isSaved = isBookmarked
    )
}

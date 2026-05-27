package com.pravor.notessharing.ui.screens.explore

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pravor.notessharing.model.FeedItem
import com.pravor.notessharing.model.FileType
import com.pravor.notessharing.model.TrendingNote
import com.pravor.notessharing.state.ExploreUiState
import com.pravor.notessharing.ui.components.StatePanel
import com.pravor.notessharing.ui.screens.home.HomeFeedCard
import com.pravor.notessharing.viewmodel.ExploreViewModel

@Composable
fun TrendingNotesRoute(
    onBackClick: () -> Unit,
    viewModel: ExploreViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TrendingNotesScreen(uiState = uiState, onBackClick = onBackClick)
}

@Composable
fun TrendingNotesScreen(
    uiState: ExploreUiState,
    onBackClick: () -> Unit
) {
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
                    onClick = {},
                    onUpvoteClick = {},
                    onBookmarkClick = {}
                )
            }
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

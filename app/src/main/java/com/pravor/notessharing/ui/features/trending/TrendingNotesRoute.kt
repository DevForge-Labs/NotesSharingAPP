package com.pravor.notessharing.ui.features.trending

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*

import com.pravor.notessharing.core.util.*

import androidx.compose.runtime.Composable
import com.pravor.notessharing.core.util.RefreshCooldownManager
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pravor.notessharing.domain.model.TrendingNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

@Composable
fun TrendingNotesRoute(
    onBackClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    viewModel: TrendingNotesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()

    var pendingRemoveBookmarkNote by remember { mutableStateOf<TrendingNote?>(null) }
    var pendingRemoveUpvoteNote by remember { mutableStateOf<TrendingNote?>(null) }

    TrendingNotesScreen(
        uiState = uiState,
        isRefreshing = isRefreshing,
        isLoadingMore = isLoadingMore,
        onBackClick = onBackClick,
        onDocumentClick = onDocumentClick,
        onRefresh = {
            RefreshCooldownManager.runWithCooldown("trending") {
                viewModel.refresh()
            }
        },
        onLoadMore = { viewModel.loadMore() },
        onBookmarkClick = { note ->
            if (note.isBookmarked) {
                pendingRemoveBookmarkNote = note
            } else {
                viewModel.toggleBookmark(note)
            }
        },
        onUpvoteClick = { note ->
            if (note.isUpvoted) {
                pendingRemoveUpvoteNote = note
            } else {
                viewModel.toggleUpvote(note)
            }
        }
    )

    if (pendingRemoveBookmarkNote != null) {
        AlertDialog(
            onDismissRequest = { pendingRemoveBookmarkNote = null },
            title = { Text(text = "Remove this bookmark?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val note = pendingRemoveBookmarkNote
                        if (note != null) {
                            viewModel.toggleBookmark(note)
                        }
                        pendingRemoveBookmarkNote = null
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingRemoveBookmarkNote = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (pendingRemoveUpvoteNote != null) {
        AlertDialog(
            onDismissRequest = { pendingRemoveUpvoteNote = null },
            title = { Text(text = "Remove this upvote?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val note = pendingRemoveUpvoteNote
                        if (note != null) {
                            viewModel.toggleUpvote(note)
                        }
                        pendingRemoveUpvoteNote = null
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingRemoveUpvoteNote = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}


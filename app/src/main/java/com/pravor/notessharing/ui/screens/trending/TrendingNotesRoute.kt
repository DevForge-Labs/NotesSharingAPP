package com.pravor.notessharing.ui.screens.trending

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TrendingNotesRoute(
    onBackClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    viewModel: TrendingNotesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()

    TrendingNotesScreen(
        uiState = uiState,
        isRefreshing = isRefreshing,
        isLoadingMore = isLoadingMore,
        onBackClick = onBackClick,
        onDocumentClick = onDocumentClick,
        onRefresh = { viewModel.refresh() },
        onLoadMore = { viewModel.loadMore() },
        onBookmarkClick = { viewModel.toggleBookmark(it) }
    )
}

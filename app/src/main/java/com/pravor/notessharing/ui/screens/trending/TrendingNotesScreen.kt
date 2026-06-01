package com.pravor.notessharing.ui.screens.trending

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pravor.notessharing.model.TrendingNote
import com.pravor.notessharing.ui.components.trending_components.TrendingNotesContent

@Composable
fun TrendingNotesScreen(
    uiState: TrendingNotesUiState,
    isRefreshing: Boolean,
    isLoadingMore: Boolean,
    onBackClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onBookmarkClick: (TrendingNote) -> Unit,
    modifier: Modifier = Modifier
) {
    TrendingNotesContent(
        uiState = uiState,
        isRefreshing = isRefreshing,
        isLoadingMore = isLoadingMore,
        onBackClick = onBackClick,
        onDocumentClick = onDocumentClick,
        onRefresh = onRefresh,
        onLoadMore = onLoadMore,
        onBookmarkClick = onBookmarkClick,
        modifier = modifier
    )
}

package com.pravor.notessharing.ui.screens.trending

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import java.util.concurrent.atomic.AtomicInteger
import androidx.compose.ui.Modifier
import com.pravor.notessharing.domain.model.TrendingNote
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
    onUpvoteClick: (TrendingNote) -> Unit,
    modifier: Modifier = Modifier
) {
    val recompositionCount = remember { AtomicInteger(0) }
    SideEffect {
        android.util.Log.d("RECOMPOSE", "[RECOMPOSE] TrendingNotesScreen count=${recompositionCount.incrementAndGet()}")
    }

    TrendingNotesContent(
        uiState = uiState,
        isRefreshing = isRefreshing,
        isLoadingMore = isLoadingMore,
        onBackClick = onBackClick,
        onDocumentClick = onDocumentClick,
        onRefresh = onRefresh,
        onLoadMore = onLoadMore,
        onBookmarkClick = onBookmarkClick,
        onUpvoteClick = onUpvoteClick,
        modifier = modifier
    )
}

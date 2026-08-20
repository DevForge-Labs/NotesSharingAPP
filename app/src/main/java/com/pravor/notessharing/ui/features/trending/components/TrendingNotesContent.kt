package com.pravor.notessharing.ui.features.trending.components

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.pravor.notessharing.ui.common.CustomPullRefreshIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import com.pravor.notessharing.data.repository.DocumentDetailRepository
import com.pravor.notessharing.domain.model.TrendingNote
import com.pravor.notessharing.ui.common.AdaptiveScrollbar
import com.pravor.notessharing.ui.common.StatePanel
import com.pravor.notessharing.ui.navigation.LocalBottomBarPadding
import com.pravor.notessharing.ui.features.trending.TrendingNotesUiState
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendingNotesContent(
    uiState: TrendingNotesUiState,
    isRefreshing: Boolean,
    isLoadingMore: Boolean,
    onBackClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onBookmarkClick: (TrendingNote) -> Unit,
    onUpvoteClick: (TrendingNote) -> Unit,
    modifier: Modifier = Modifier,
    detailRepository: DocumentDetailRepository = remember { DocumentDetailRepository() }
) {
    val bottomPadding = LocalBottomBarPadding.current
    Scaffold(
        topBar = {
            TrendingNotesHeader(onBackClick = onBackClick)
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { paddingValues ->
        val pullToRefreshState = rememberPullToRefreshState()
        val pullProgress = if (isRefreshing) 1f else pullToRefreshState.distanceFraction.coerceIn(0f, 1f)
        val blurRadius = (pullProgress * 2).dp
        val dimAlpha = pullProgress * 0.08f

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = pullToRefreshState,
            indicator = {
                CustomPullRefreshIndicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(blurRadius)
                    .drawWithContent {
                        drawContent()
                        if (dimAlpha > 0f) {
                            drawRect(Color.Black.copy(alpha = dimAlpha))
                        }
                    }
            ) {
                when (uiState) {
                TrendingNotesUiState.Loading -> StatePanel("Finding topics", "Scanning campus trends", loading = true)
                TrendingNotesUiState.Empty -> StatePanel("Nothing trending", "Explore content will appear here")
                is TrendingNotesUiState.Error -> StatePanel("Explore failed", uiState.message)
                is TrendingNotesUiState.Success -> {
                    val documentNotes = uiState.trendingNotes
                    val listState = rememberLazyListState()

                    LaunchedEffect(listState) {
                        androidx.compose.runtime.snapshotFlow {
                            Pair(listState.firstVisibleItemIndex, listState.layoutInfo.visibleItemsInfo.size)
                        }
                        .distinctUntilChanged()
                        .collect { (firstVisible, visibleCount) ->
                            if (visibleCount > 0) {
                                android.util.Log.d("PERF", "[PERF] First visible item=$firstVisible")
                                android.util.Log.d("PERF", "[PERF] Visible items count=$visibleCount")
                            }
                        }
                    }

                    // Detect when scrolling near the bottom (infinite scroll threshold)
                    val shouldLoadMore = remember(listState) {
                        derivedStateOf {
                            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                            if (lastVisibleItem == null) {
                                false
                            } else {
                                lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 3
                            }
                        }
                    }

                    LaunchedEffect(shouldLoadMore.value) {
                        if (shouldLoadMore.value) {
                            onLoadMore()
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp + bottomPadding),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(
                                items = documentNotes,
                                key = { index, note -> note.id.ifBlank { "trending-note-card-$index" } },
                                contentType = { _, _ -> "trending-note-card" }
                            ) { _, note ->
                                TrendingNoteDiscoveryCard(
                                    note = note,
                                    detailRepository = detailRepository,
                                    onClick = { onDocumentClick(note.id) },
                                    onBookmarkClick = { onBookmarkClick(note) },
                                    onUpvoteClick = { onUpvoteClick(note) }
                                )
                            }

                            // Show loading indicator at the bottom if loading more pages
                            if (isLoadingMore) {
                                item(key = "loading-more", contentType = "loading") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                        AdaptiveScrollbar(listState = listState)
                    }
                }
            }
        }
    }
}
}

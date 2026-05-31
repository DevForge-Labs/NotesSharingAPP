package com.pravor.notessharing.ui.components.trending_components

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pravor.notessharing.data.DocumentDetailRepository
import com.pravor.notessharing.ui.components.AdaptiveScrollbar
import com.pravor.notessharing.ui.components.StatePanel
import com.pravor.notessharing.ui.navigation.LocalBottomBarPadding
import com.pravor.notessharing.ui.screens.trending.TrendingNotesUiState

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

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                TrendingNotesUiState.Loading -> StatePanel("Finding topics", "Scanning campus trends", loading = true)
                TrendingNotesUiState.Empty -> StatePanel("Nothing trending", "Explore content will appear here")
                is TrendingNotesUiState.Error -> StatePanel("Explore failed", uiState.message)
                is TrendingNotesUiState.Success -> {
                    val documentNotes = uiState.trendingNotes
                    val listState = rememberLazyListState()

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
                                    onClick = { onDocumentClick(note.id) }
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

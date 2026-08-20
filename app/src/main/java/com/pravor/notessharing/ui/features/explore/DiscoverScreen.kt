package com.pravor.notessharing.ui.features.explore

import com.pravor.notessharing.ui.features.explore.components.DiscoverFeedItem as DiscoverFeedItemCard

import com.pravor.notessharing.ui.features.explore.components.*

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pravor.notessharing.domain.model.DiscoverFeedItem
import com.pravor.notessharing.ui.common.ExploreUiState
import com.pravor.notessharing.ui.common.StatePanel

@Composable
fun DiscoverRoute(
    onBackClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    viewModel: ExploreViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    DiscoverScreen(
        uiState = uiState,
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.loadRealDocuments(isPullToRefresh = true) },
        onBackClick = onBackClick,
        onDocumentClick = onDocumentClick,
        onVideoClick = onVideoClick
    )
}

@Composable
fun DiscoverScreen(
    uiState: ExploreUiState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onBackClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    onVideoClick: (String) -> Unit
) {
    when (uiState) {
        ExploreUiState.Loading -> StatePanel("Finding topics", "Scanning campus trends", loading = true)
        ExploreUiState.Empty -> StatePanel("Nothing trending", "Explore content will appear here")
        is ExploreUiState.Error -> StatePanel("Explore failed", uiState.message)
        is ExploreUiState.Success -> ExploreExpandedListScaffold(
            title = "Discover",
            onBackClick = onBackClick,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh
        ) {
            itemsIndexed(
                items = uiState.content.discoverItems,
                key = { index, item -> item.id.ifBlank { "discover_$index" } },
                contentType = { _, item ->
                    when (item) {
                        is DiscoverFeedItem.Collection -> "discover-collection"
                        is DiscoverFeedItem.ContributorPost -> "discover-contributor"
                        is DiscoverFeedItem.Note -> "discover-note"
                        is DiscoverFeedItem.Video -> "discover-video"
                    }
                }
            ) { _, item ->
                DiscoverFeedItemCard(item, onClick = {
                    if (item is DiscoverFeedItem.Video) {
                        onVideoClick(item.id)
                    } else {
                        onDocumentClick(item.id)
                    }
                })
            }
        }
    }
}

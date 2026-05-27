package com.pravor.notessharing.ui.screens.explore

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pravor.notessharing.model.DiscoverFeedItem
import com.pravor.notessharing.state.ExploreUiState
import com.pravor.notessharing.ui.components.StatePanel
import com.pravor.notessharing.ui.components.explore_components.DiscoverFeedItem as DiscoverFeedItemCard
import com.pravor.notessharing.viewmodel.ExploreViewModel

@Composable
fun DiscoverRoute(
    onBackClick: () -> Unit,
    viewModel: ExploreViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DiscoverScreen(uiState = uiState, onBackClick = onBackClick)
}

@Composable
fun DiscoverScreen(
    uiState: ExploreUiState,
    onBackClick: () -> Unit
) {
    when (uiState) {
        ExploreUiState.Loading -> StatePanel("Finding topics", "Scanning dummy campus trends", loading = true)
        ExploreUiState.Empty -> StatePanel("Nothing trending", "Explore content will appear here")
        is ExploreUiState.Error -> StatePanel("Explore failed", uiState.message)
        is ExploreUiState.Success -> ExploreExpandedListScaffold(
            title = "Discover",
            onBackClick = onBackClick
        ) {
            items(
                items = uiState.content.discoverItems,
                key = { it.id },
                contentType = {
                    when (it) {
                        is DiscoverFeedItem.Collection -> "discover-collection"
                        is DiscoverFeedItem.ContributorPost -> "discover-contributor"
                        is DiscoverFeedItem.Note -> "discover-note"
                        is DiscoverFeedItem.Video -> "discover-video"
                    }
                }
            ) { item ->
                DiscoverFeedItemCard(item)
            }
        }
    }
}

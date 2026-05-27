package com.pravor.notessharing.ui.screens.explore

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pravor.notessharing.state.ExploreUiState
import com.pravor.notessharing.ui.components.StatePanel
import com.pravor.notessharing.ui.components.explore_components.VideoRecommendationCard
import com.pravor.notessharing.viewmodel.ExploreViewModel

@Composable
fun RecommendedVideosRoute(
    onBackClick: () -> Unit,
    viewModel: ExploreViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RecommendedVideosScreen(uiState = uiState, onBackClick = onBackClick)
}

@Composable
fun RecommendedVideosScreen(
    uiState: ExploreUiState,
    onBackClick: () -> Unit
) {
    when (uiState) {
        ExploreUiState.Loading -> StatePanel("Finding topics", "Scanning dummy campus trends", loading = true)
        ExploreUiState.Empty -> StatePanel("Nothing trending", "Explore content will appear here")
        is ExploreUiState.Error -> StatePanel("Explore failed", uiState.message)
        is ExploreUiState.Success -> ExploreExpandedListScaffold(
            title = "Recommended Videos",
            onBackClick = onBackClick
        ) {
            items(uiState.content.videoRecommendations, key = { it.id }, contentType = { "video" }) { video ->
                VideoRecommendationCard(video)
            }
        }
    }
}

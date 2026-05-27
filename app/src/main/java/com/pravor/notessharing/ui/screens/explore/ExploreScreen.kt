package com.pravor.notessharing.ui.screens.explore

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pravor.notessharing.state.ExploreUiState
import com.pravor.notessharing.ui.components.StatePanel
import com.pravor.notessharing.ui.components.explore_components.ExploreSuccessContent
import com.pravor.notessharing.viewmodel.ExploreViewModel

@Composable
fun ExploreRoute(
    onTrendingSeeMoreClick: () -> Unit,
    onRecommendedVideosSeeMoreClick: () -> Unit,
    onDiscoverSeeMoreClick: () -> Unit,
    viewModel: ExploreViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExploreScreen(
        uiState = uiState,
        onTrendingSeeMoreClick = onTrendingSeeMoreClick,
        onRecommendedVideosSeeMoreClick = onRecommendedVideosSeeMoreClick,
        onDiscoverSeeMoreClick = onDiscoverSeeMoreClick
    )
}

@Composable
fun ExploreScreen(
    uiState: ExploreUiState,
    onTrendingSeeMoreClick: () -> Unit,
    onRecommendedVideosSeeMoreClick: () -> Unit,
    onDiscoverSeeMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    Crossfade(targetState = uiState, label = "explore-state", modifier = modifier.fillMaxSize()) { state ->
        when (state) {
            ExploreUiState.Loading -> StatePanel("Finding topics", "Scanning dummy campus trends", loading = true, modifier = Modifier.padding(top = 96.dp))
            ExploreUiState.Empty -> StatePanel("Nothing trending", "Explore content will appear here", modifier = Modifier.padding(top = 96.dp))
            is ExploreUiState.Error -> StatePanel("Explore failed", state.message, modifier = Modifier.padding(top = 96.dp))
            is ExploreUiState.Success -> ExploreSuccessContent(
                content = state.content,
                listState = listState,
                onTrendingSeeMoreClick = onTrendingSeeMoreClick,
                onRecommendedVideosSeeMoreClick = onRecommendedVideosSeeMoreClick,
                onDiscoverSeeMoreClick = onDiscoverSeeMoreClick
            )
        }
    }
}

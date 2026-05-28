package com.pravor.notessharing.ui.screens.explore

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
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
    onDocumentClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    viewModel: ExploreViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExploreScreen(
        uiState = uiState,
        onTrendingSeeMoreClick = onTrendingSeeMoreClick,
        onRecommendedVideosSeeMoreClick = onRecommendedVideosSeeMoreClick,
        onDiscoverSeeMoreClick = onDiscoverSeeMoreClick,
        onDocumentClick = onDocumentClick,
        onVideoClick = onVideoClick
    )
}

@Composable
fun ExploreScreen(
    uiState: ExploreUiState,
    onTrendingSeeMoreClick: () -> Unit,
    onRecommendedVideosSeeMoreClick: () -> Unit,
    onDiscoverSeeMoreClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val repository = remember { com.pravor.notessharing.data.UploadRepository(context) }
    var selectedUploadForViewer by remember { mutableStateOf<com.pravor.notessharing.ui.components.UploadViewerData?>(null) }

    val listState = rememberLazyListState()

    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(targetState = uiState, label = "explore-state", modifier = Modifier.fillMaxSize()) { state ->
            when (state) {
                ExploreUiState.Loading -> StatePanel("Finding topics", "Scanning dummy campus trends", loading = true, modifier = Modifier.padding(top = 96.dp))
                ExploreUiState.Empty -> StatePanel("Nothing trending", "Explore content will appear here", modifier = Modifier.padding(top = 96.dp))
                is ExploreUiState.Error -> StatePanel("Explore failed", state.message, modifier = Modifier.padding(top = 96.dp))
                is ExploreUiState.Success -> ExploreSuccessContent(
                    content = state.content,
                    listState = listState,
                    onTrendingSeeMoreClick = onTrendingSeeMoreClick,
                    onRecommendedVideosSeeMoreClick = onRecommendedVideosSeeMoreClick,
                    onDiscoverSeeMoreClick = onDiscoverSeeMoreClick,
                    onDocumentClick = onDocumentClick,
                    onVideoClick = onVideoClick
                )
            }
        }

        selectedUploadForViewer?.let { viewerData ->
            com.pravor.notessharing.ui.components.GroupedUploadViewerDialog(
                title = viewerData.title,
                fileUrls = viewerData.fileUrls,
                onDismiss = { selectedUploadForViewer = null },
                onFileClick = { url ->
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            )
        }
    }
}

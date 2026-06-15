package com.pravor.notessharing.ui.screens.explore

import androidx.compose.animation.Crossfade
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.pravor.notessharing.ui.components.CustomPullRefreshIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.concurrent.atomic.AtomicInteger
import com.pravor.notessharing.state.ExploreUiState
import com.pravor.notessharing.state.CatalogSubject
import com.pravor.notessharing.ui.components.StatePanel
import com.pravor.notessharing.ui.components.explore_components.ExploreSuccessContent
import com.pravor.notessharing.viewmodel.ExploreViewModel
import com.pravor.notessharing.model.TrendingNote
import com.pravor.notessharing.model.VideoRecommendation
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

@Composable
fun ExploreRoute(
    onTrendingSeeMoreClick: () -> Unit,
    onRecommendedVideosSeeMoreClick: () -> Unit,
    onDiscoverSeeMoreClick: () -> Unit = {},
    onExamPrepSeeMoreClick: () -> Unit,
    onAssignmentsSeeMoreClick: () -> Unit,
    onSubjectSeeMoreClick: (String) -> Unit,
    onDocumentClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    viewModel: ExploreViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val allowedSubjects by viewModel.allowedSubjects.collectAsStateWithLifecycle()

    var pendingRemoveBookmarkNote by remember { mutableStateOf<TrendingNote?>(null) }
    var pendingRemoveBookmarkVideo by remember { mutableStateOf<VideoRecommendation?>(null) }
    var pendingRemoveUpvoteData by remember { mutableStateOf<Triple<String, String?, Int>?>(null) }

    val onBookmarkClickRemembered = remember(viewModel) {
        { note: TrendingNote ->
            if (note.isBookmarked) {
                pendingRemoveBookmarkNote = note
            } else {
                viewModel.toggleBookmark(note)
            }
        }
    }

    val onVideoBookmarkClickRemembered = remember(viewModel) {
        { video: VideoRecommendation ->
            if (video.isBookmarked) {
                pendingRemoveBookmarkVideo = video
            } else {
                viewModel.toggleVideoBookmark(video)
            }
        }
    }

    val onUpvoteClickRemembered = remember(viewModel) {
        { id: String, docType: String?, currentUpvotes: Int ->
            val wasUpvoted = com.pravor.notessharing.upvotes.UpvoteRepository.upvotesFlow.value[id] ?: false
            if (wasUpvoted) {
                pendingRemoveUpvoteData = Triple(id, docType, currentUpvotes)
            } else {
                viewModel.toggleUpvote(id, docType, currentUpvotes)
            }
        }
    }

    val onRefreshRemembered = remember(viewModel) {
        { viewModel.loadRealDocuments(isPullToRefresh = true) }
    }

    ExploreScreen(
        uiState = uiState,
        isRefreshing = isRefreshing,
        allowedSubjects = allowedSubjects,
        onRefresh = onRefreshRemembered,
        onTrendingSeeMoreClick = onTrendingSeeMoreClick,
        onRecommendedVideosSeeMoreClick = onRecommendedVideosSeeMoreClick,
        onDiscoverSeeMoreClick = onDiscoverSeeMoreClick,
        onExamPrepSeeMoreClick = onExamPrepSeeMoreClick,
        onAssignmentsSeeMoreClick = onAssignmentsSeeMoreClick,
        onSubjectSeeMoreClick = onSubjectSeeMoreClick,
        onDocumentClick = onDocumentClick,
        onVideoClick = onVideoClick,
        onBookmarkClick = onBookmarkClickRemembered,
        onVideoBookmarkClick = onVideoBookmarkClickRemembered,
        onUpvoteClick = onUpvoteClickRemembered
    )

        if (pendingRemoveBookmarkNote != null) {
            AlertDialog(
                onDismissRequest = { pendingRemoveBookmarkNote = null },
                title = { Text(text = "Remove this bookmark?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val note = pendingRemoveBookmarkNote
                            if (note != null) {
                                viewModel.toggleBookmark(note)
                            }
                            pendingRemoveBookmarkNote = null
                        }
                    ) {
                        Text("Remove")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { pendingRemoveBookmarkNote = null }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (pendingRemoveBookmarkVideo != null) {
            AlertDialog(
                onDismissRequest = { pendingRemoveBookmarkVideo = null },
                title = { Text(text = "Remove this bookmark?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val video = pendingRemoveBookmarkVideo
                            if (video != null) {
                                viewModel.toggleVideoBookmark(video)
                            }
                            pendingRemoveBookmarkVideo = null
                        }
                    ) {
                        Text("Remove")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { pendingRemoveBookmarkVideo = null }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (pendingRemoveUpvoteData != null) {
            AlertDialog(
                onDismissRequest = { pendingRemoveUpvoteData = null },
                title = { Text(text = "Remove this upvote?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val data = pendingRemoveUpvoteData
                            if (data != null) {
                                viewModel.toggleUpvote(data.first, data.second, data.third)
                            }
                            pendingRemoveUpvoteData = null
                        }
                    ) {
                        Text("Remove")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { pendingRemoveUpvoteData = null }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    uiState: ExploreUiState,
    isRefreshing: Boolean,
    allowedSubjects: List<CatalogSubject>,
    onRefresh: () -> Unit,
    onTrendingSeeMoreClick: () -> Unit,
    onRecommendedVideosSeeMoreClick: () -> Unit,
    onDiscoverSeeMoreClick: () -> Unit = {},
    onExamPrepSeeMoreClick: () -> Unit,
    onAssignmentsSeeMoreClick: () -> Unit,
    onSubjectSeeMoreClick: (String) -> Unit,
    onDocumentClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    onBookmarkClick: (TrendingNote) -> Unit,
    onVideoBookmarkClick: (VideoRecommendation) -> Unit = {},
    onUpvoteClick: (String, String?, Int) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val recompositionCount = remember { AtomicInteger(0) }
    SideEffect {
        android.util.Log.d("RECOMPOSE", "[RECOMPOSE] ExploreScreen count=${recompositionCount.incrementAndGet()}")
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val repository = remember { com.pravor.notessharing.data.UploadRepository(context) }
    var selectedUploadForViewer by remember { mutableStateOf<com.pravor.notessharing.ui.components.UploadViewerData?>(null) }
 
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow {
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

    val pullToRefreshState = rememberPullToRefreshState()
    val pullProgress = if (isRefreshing) 1f else pullToRefreshState.distanceFraction.coerceIn(0f, 1f)
    val blurRadius = (pullProgress * 2).dp
    val dimAlpha = pullProgress * 0.08f

    Box(modifier = modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = pullToRefreshState,
            indicator = {
                CustomPullRefreshIndicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    restingOffset = 112.dp,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            },
            modifier = Modifier.fillMaxSize()
        ) {
            val stateType = when (uiState) {
                ExploreUiState.Loading -> "loading"
                ExploreUiState.Empty -> "empty"
                is ExploreUiState.Error -> "error"
                is ExploreUiState.Success -> "success"
            }
            Crossfade(
                targetState = stateType,
                label = "explore-state",
                modifier = Modifier
                    .fillMaxSize()
                    .blur(blurRadius)
                    .drawWithContent {
                        drawContent()
                        if (dimAlpha > 0f) {
                            drawRect(Color.Black.copy(alpha = dimAlpha))
                        }
                    }
            ) { type ->
                when (type) {
                    "loading" -> StatePanel("Finding topics", "Scanning campus trends", loading = true, modifier = Modifier.padding(top = 96.dp))
                    "empty" -> StatePanel("Nothing trending", "Explore content will appear here", modifier = Modifier.padding(top = 96.dp))
                    "error" -> {
                        val errorState = uiState as? ExploreUiState.Error
                        StatePanel("Explore failed", errorState?.message ?: "", modifier = Modifier.padding(top = 96.dp))
                    }
                    "success" -> {
                        val successState = uiState as? ExploreUiState.Success
                        if (successState != null) {
                            ExploreSuccessContent(
                                content = successState.content,
                                listState = listState,
                                allowedSubjects = allowedSubjects,
                                onTrendingSeeMoreClick = onTrendingSeeMoreClick,
                                onRecommendedVideosSeeMoreClick = onRecommendedVideosSeeMoreClick,
                                onDiscoverSeeMoreClick = onDiscoverSeeMoreClick,
                                onExamPrepSeeMoreClick = onExamPrepSeeMoreClick,
                                onAssignmentsSeeMoreClick = onAssignmentsSeeMoreClick,
                                onSubjectSeeMoreClick = onSubjectSeeMoreClick,
                                onDocumentClick = onDocumentClick,
                                onVideoClick = onVideoClick,
                                onBookmarkClick = onBookmarkClick,
                                onVideoBookmarkClick = onVideoBookmarkClick,
                                onUpvoteClick = onUpvoteClick
                            )
                        }
                    }
                }
            }
        }

        selectedUploadForViewer?.let { viewerData ->
            com.pravor.notessharing.ui.components.GroupedUploadViewerDialog(
                title = viewerData.title,
                fileUrls = viewerData.fileUrls,
                onDismiss = { selectedUploadForViewer = null },
                onFileClick = { url ->
                    try {
                        if (viewerData.id.isNotEmpty()) {
                            coroutineScope.launch {
                                com.pravor.notessharing.data.ViewTrackingRepository().incrementViewCount(viewerData.id)
                            }
                        }
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

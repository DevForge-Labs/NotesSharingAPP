package com.pravor.notessharing.ui.screens.explore

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.pravor.notessharing.ui.components.CustomPullRefreshIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pravor.notessharing.data.DocumentDetailRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.concurrent.atomic.AtomicInteger
import com.pravor.notessharing.model.TrendingNote
import com.pravor.notessharing.model.VideoRecommendation
import com.pravor.notessharing.ui.components.StatePanel
import com.pravor.notessharing.ui.components.explore_components.VideoRecommendationCard
import com.pravor.notessharing.ui.components.trending_components.TrendingNoteDiscoveryCard
import com.pravor.notessharing.ui.components.utils.getSubjectColor
import com.pravor.notessharing.ui.components.utils.getSubjectDisplayName
import com.pravor.notessharing.ui.components.utils.normalizeSubject
import com.pravor.notessharing.ui.navigation.LocalBottomBarPadding
import com.pravor.notessharing.viewmodel.SubjectResourcesViewModel

enum class ResourceFilter(val label: String) {
    All("All"),
    Notes("Notes"),
    PYQs("PYQs"),
    CheatSheets("Cheat Sheets"),
    Assignments("Assignments"),
    Videos("Videos")
}

@Composable
fun SubjectResourcesRoute(
    subjectName: String,
    onBackClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    viewModel: SubjectResourcesViewModel = viewModel(
        factory = SubjectResourcesViewModel.provideFactory(
            application = androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application,
            subjectName = subjectName
        )
    )
) {
    val resources by viewModel.resources.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

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

    SubjectResourcesScreen(
        subjectName = subjectName,
        resources = resources,
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.loadResources() },
        onBackClick = onBackClick,
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
fun SubjectResourcesScreen(
    subjectName: String,
    resources: List<Any>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onBackClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    onBookmarkClick: (TrendingNote) -> Unit,
    onVideoBookmarkClick: (VideoRecommendation) -> Unit,
    onUpvoteClick: (String, String?, Int) -> Unit
) {
    val recompositionCount = remember { AtomicInteger(0) }
    SideEffect {
        android.util.Log.d("RECOMPOSE", "[RECOMPOSE] SubjectResourcesScreen count=${recompositionCount.incrementAndGet()}")
    }

    val bottomPadding = LocalBottomBarPadding.current
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

    val detailRepository = remember { DocumentDetailRepository() }

    val normalized = remember(subjectName) { normalizeSubject(subjectName) }
    val accentColor = remember(normalized) { getSubjectColor(normalized) }
    val displayName = remember(subjectName, normalized) { getSubjectDisplayName(subjectName, normalized) }

    var selectedFilter by remember { mutableStateOf(ResourceFilter.All) }

    val filteredResources = remember(resources, selectedFilter) {
        when (selectedFilter) {
            ResourceFilter.All -> resources
            ResourceFilter.Notes -> resources.filter { res ->
                if (res is TrendingNote) {
                    val docType = res.documentType.ifBlank { res.type ?: "" }.lowercase(java.util.Locale.ROOT).trim()
                    val isValid = docType == "notes" || docType == "note" || docType == "documents" || docType == "document" || docType == "pdf" || docType.isBlank()
                    val isExcluded = docType == "pyq" || docType == "pyqs" || docType == "cheatsheet" || docType == "cheatsheets" || docType == "cheat sheet" || docType == "assignment" || docType == "assignments" || docType == "video" || docType == "videos"
                    isValid && !isExcluded
                } else false
            }
            ResourceFilter.PYQs -> resources.filter { res ->
                if (res is TrendingNote) {
                    val docType = res.documentType.ifBlank { res.type ?: "" }.lowercase(java.util.Locale.ROOT).trim()
                    docType == "pyq" || docType == "pyqs"
                } else false
            }
            ResourceFilter.CheatSheets -> resources.filter { res ->
                if (res is TrendingNote) {
                    val docType = res.documentType.ifBlank { res.type ?: "" }.lowercase(java.util.Locale.ROOT).trim()
                    docType == "cheatsheet" || docType == "cheatsheets" || docType == "cheat sheet"
                } else false
            }
            ResourceFilter.Assignments -> resources.filter { res ->
                if (res is TrendingNote) {
                    val docType = res.documentType.ifBlank { res.type ?: "" }.lowercase(java.util.Locale.ROOT).trim()
                    docType == "assignment" || docType == "assignments"
                } else false
            }
            ResourceFilter.Videos -> resources.filter { res ->
                res is VideoRecommendation
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = displayName.trim().uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${resources.size} ${if (resources.size == 1) "Resource" else "Resources"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(40.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
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
            Column(
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
                // Filter Chips Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(ResourceFilter.values()) { filter ->
                        val isSelected = filter == selectedFilter
                        val chipColor = if (isSelected) accentColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                        Surface(
                            onClick = { selectedFilter = filter },
                            shape = RoundedCornerShape(50.dp),
                            color = chipColor,
                            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = filter.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Resources list
                if (filteredResources.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        StatePanel(
                            title = "No resources found",
                            message = "No uploads exist yet for ${selectedFilter.label} in ${displayName.trim()}."
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp + bottomPadding),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(
                                items = filteredResources,
                                key = { index, res ->
                                    when (res) {
                                        is TrendingNote -> "note_${res.id}_$index"
                                        is VideoRecommendation -> "video_${res.id}_$index"
                                        else -> "res_$index"
                                    }
                                },
                                contentType = { _, res ->
                                    when (res) {
                                        is TrendingNote -> "note-card"
                                        is VideoRecommendation -> "video-card"
                                        else -> "unknown-card"
                                    }
                                }
                            ) { _, res ->
                                when (res) {
                                    is TrendingNote -> {
                                        val onClickClick = remember(res.id) { { onDocumentClick(res.id) } }
                                        val onBookmarkClickRemembered = remember(res) { { onBookmarkClick(res) } }
                                        val onUpvoteClickRemembered = remember(res.id, res.documentType, res.upvotes) {
                                            { onUpvoteClick(res.id, res.documentType, res.upvotes) }
                                        }

                                        TrendingNoteDiscoveryCard(
                                            note = res,
                                            detailRepository = detailRepository,
                                            onClick = onClickClick,
                                            onBookmarkClick = onBookmarkClickRemembered,
                                            onUpvoteClick = onUpvoteClickRemembered
                                        )
                                    }
                                    is VideoRecommendation -> {
                                        val onVideoClickClick = remember(res.id) { { onVideoClick(res.id) } }
                                        val onVideoBookmarkClickRemembered = remember(res) { { onVideoBookmarkClick(res) } }
                                        val onUpvoteClickRemembered = remember(res.id, res.documentType, res.upvotes) {
                                            { onUpvoteClick(res.id, res.documentType, res.upvotes) }
                                        }

                                        VideoRecommendationCard(
                                            video = res,
                                            isUpvoted = res.isUpvoted,
                                            onBookmarkClick = onVideoBookmarkClickRemembered,
                                            onClick = onVideoClickClick,
                                            onUpvoteClick = onUpvoteClickRemembered
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

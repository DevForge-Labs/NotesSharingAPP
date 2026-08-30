package com.pravor.notessharing.ui.features.explore

import com.pravor.notessharing.ui.features.explore.components.*

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*


import com.pravor.notessharing.ui.common.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pravor.notessharing.ui.common.ExploreUiState
import com.pravor.notessharing.ui.common.components.StatePanel

import com.pravor.notessharing.ui.navigation.LocalBottomBarPadding
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pravor.notessharing.domain.model.VideoRecommendation

@Composable
fun RecommendedVideosRoute(
    onBackClick: () -> Unit,
    onVideoClick: (String) -> Unit,
    viewModel: ExploreViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var pendingRemoveBookmarkVideo by remember { mutableStateOf<VideoRecommendation?>(null) }

    val onBookmarkClickRemembered = remember(viewModel) {
        { video: VideoRecommendation ->
            if (video.isBookmarked) {
                pendingRemoveBookmarkVideo = video
            } else {
                viewModel.toggleVideoBookmark(video)
            }
        }
    }

    RecommendedVideosScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onVideoClick = onVideoClick,
        onBookmarkClick = onBookmarkClickRemembered
    )

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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendedVideosScreen(
    uiState: ExploreUiState,
    onBackClick: () -> Unit,
    onVideoClick: (String) -> Unit,
    onBookmarkClick: (VideoRecommendation) -> Unit = {}
) {
    val bottomPadding = LocalBottomBarPadding.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Recommended Videos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                ExploreUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                ExploreUiState.Empty -> {
                    EmptyVideosState()
                }
                is ExploreUiState.Error -> {
                    StatePanel("Explore failed", uiState.message)
                }
                is ExploreUiState.Success -> {
                    var searchQuery by remember { mutableStateOf("") }
                    var selectedSubject by remember { mutableStateOf("") }

                    val allVideos = remember(uiState.content.videos) {
                        uiState.content.videos.map { it.toVideoRecommendation() }
                    }

                    val subjectList = remember(allVideos) {
                        allVideos.mapNotNull { it.subject.takeIf { s -> s.isNotBlank() } }
                            .map { com.pravor.notessharing.data.repository.SubjectCatalogRepository.getInstance().resolveDisplayName(it, it).trim() }
                            .distinct()
                    }

                    val filteredVideos = remember(allVideos, searchQuery, selectedSubject) {
                        val q = searchQuery.trim().lowercase(java.util.Locale.ROOT)
                        val normSelected = if (selectedSubject.isNotBlank()) com.pravor.notessharing.ui.common.utils.normalizeSubject(selectedSubject) else ""

                        allVideos.filter { video ->
                            if (normSelected.isNotEmpty()) {
                                val normSubj = com.pravor.notessharing.ui.common.utils.normalizeSubject(video.subject)
                                if (normSubj != normSelected) {
                                    return@filter false
                                }
                            }
                            if (q.isNotEmpty()) {
                                val matchesTitle = video.title.lowercase(java.util.Locale.ROOT).contains(q)
                                val matchesChannel = video.channelName.lowercase(java.util.Locale.ROOT).contains(q)
                                val matchesSubj = video.subject.lowercase(java.util.Locale.ROOT).contains(q)
                                if (!matchesTitle && !matchesChannel && !matchesSubj) {
                                    return@filter false
                                }
                            }
                            true
                        }
                    }

                    if (allVideos.isEmpty()) {
                        EmptyVideosState()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 14.dp + bottomPadding),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item(key = "videos-controls", contentType = "controls") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    com.pravor.notessharing.ui.common.components.LocalSearchBar(
                                        query = searchQuery,
                                        onQueryChange = { searchQuery = it },
                                        placeholderText = "Search recommended videos..."
                                    )

                                    if (subjectList.isNotEmpty()) {
                                        com.pravor.notessharing.ui.common.components.SubjectFilterRow(
                                            subjects = subjectList,
                                            selectedSubject = selectedSubject,
                                            onSelectSubject = { selectedSubject = it }
                                        )
                                    }
                                }
                            }

                            if (filteredVideos.isEmpty()) {
                                item(key = "empty-filtered-videos", contentType = "empty") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No videos match your search.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            } else {
                                itemsIndexed(filteredVideos, key = { index, video -> video.id.ifBlank { "video_$index" } }, contentType = { _, _ -> "video" }) { _, video ->
                                    val onBookmarkClickRemembered = remember(video) {
                                        { onBookmarkClick(video) }
                                    }
                                    VideoRecommendationCard(
                                        video = video,
                                        isUpvoted = video.isUpvoted,
                                        onBookmarkClick = onBookmarkClickRemembered,
                                        onClick = { onVideoClick(video.id) }
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

@Composable
private fun EmptyVideosState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No videos available yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Check back later for curated campus videos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

package com.pravor.notessharing.ui.features.myfiles

import com.pravor.notessharing.ui.features.myfiles.BookmarkUiState

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*


import com.pravor.notessharing.ui.common.*

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pravor.notessharing.ui.common.AdaptiveScrollbar
import com.pravor.notessharing.ui.common.NotesSearchBar
import com.pravor.notessharing.ui.common.components.StatePanel
import com.pravor.notessharing.ui.common.components.StudyHubShelfCard
import com.pravor.notessharing.ui.common.components.SectionHeader
import com.pravor.notessharing.ui.features.explore.components.VideoRecommendationCard
import com.pravor.notessharing.ui.features.myfiles.components.PremiumEmptyState
import com.pravor.notessharing.ui.navigation.LocalBottomBarPadding
import com.pravor.notessharing.domain.model.VideoRecommendation
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookmarksScreen(
    onBackClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookmarkViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scrollState = rememberScrollState()
    val bottomPadding = LocalBottomBarPadding.current
    var selectedFilter by remember { mutableStateOf("All") }
    var pendingRemoveBookmarkFile by remember { mutableStateOf<com.pravor.notessharing.domain.model.StudyFile?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val upvotesMap by com.pravor.notessharing.data.repository.UpvoteRepository.upvotesFlow.collectAsStateWithLifecycle()
    val upvoteCountsMap by com.pravor.notessharing.data.repository.UpvoteRepository.upvoteCountsFlow.collectAsStateWithLifecycle()
    val upvoteRepository = remember { com.pravor.notessharing.data.repository.UpvoteRepository() }
    val scope = rememberCoroutineScope()
    val currentUid = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid }

    var videoDetailsMap by remember { mutableStateOf<Map<String, com.pravor.notessharing.domain.model.VideoDetail>>(emptyMap()) }
    val videoRepository = remember { com.pravor.notessharing.data.repository.VideoDetailRepository() }

    LaunchedEffect(Unit) {
        viewModel.loadBookmarksForCurrentUser()
    }

    LaunchedEffect(uiState) {
        if (uiState is BookmarkUiState.Success) {
            val bookmarks = (uiState as BookmarkUiState.Success).bookmarks
            val paths = bookmarks.map { file ->
                val col = when (file.documentType?.lowercase(java.util.Locale.ROOT)?.trim()) {
                    "notes", "note" -> "notes"
                    "pyq", "pyqs" -> "pyqs"
                    "assignment", "assignments" -> "assignments"
                    "cheat sheet", "cheatsheet", "cheatsheets" -> "cheatsheets"
                    "video", "videos", "youtube resource" -> "videos"
                    else -> "documents"
                }
                file.id to col
            }
            upvoteRepository.observeVisibleDocuments("Bookmarks", paths)

            // Asynchronously fetch video details for all bookmarked videos/playlists
            val videoBookmarks = bookmarks.filter { it.isVideo() }
            videoBookmarks.forEach { file ->
                if (!videoDetailsMap.containsKey(file.id)) {
                    launch {
                        val detail = videoRepository.getVideo(file.id)
                        if (detail != null) {
                            videoDetailsMap = videoDetailsMap + (file.id to detail)
                        } else {
                            // Video no longer exists in Firestore!
                            // Automatically remove the stale bookmark record
                            viewModel.removeBookmark(file.id)
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            upvoteRepository.observeVisibleDocuments("Bookmarks", emptyList())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Bookmarks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = uiState, label = "bookmarks-state", modifier = Modifier.fillMaxSize()) { state ->
                when (state) {
                    BookmarkUiState.Loading -> StatePanel(
                        title = "Loading bookmarks",
                        message = "Preparing your saved resources",
                        loading = true,
                        modifier = Modifier.padding(top = 96.dp)
                    )
                    is BookmarkUiState.Error -> StatePanel(
                        title = "Bookmarks unavailable",
                        message = state.message,
                        modifier = Modifier.padding(top = 96.dp)
                    )
                    BookmarkUiState.Empty, is BookmarkUiState.Success -> {
                        val bookmarks = if (state is BookmarkUiState.Success) {
                            state.bookmarks
                        } else {
                            emptyList()
                        }

                        val displayEmpty = bookmarks.isEmpty()
                        val filteredFiles by remember(bookmarks, selectedFilter, searchQuery) {
                            derivedStateOf {
                                if (displayEmpty) {
                                    emptyList()
                                } else {
                                    bookmarks
                                        .filter { it.matchesFilter(selectedFilter) }
                                        .filter { it.matchesSearchQuery(searchQuery) }
                                }
                            }
                        }

                        Box(Modifier.fillMaxSize()) {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(top = 8.dp, bottom = 14.dp + bottomPadding),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // 1. Subtitle Header Item
                                item(key = "header_subtitle") {
                                    Text(
                                        text = "Quick access to saved study material",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 18.dp)
                                    )
                                }

                                // 2. Search Bar Item
                                item(key = "header_search") {
                                    Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                                        NotesSearchBar(
                                            placeholder = "Search saved resources...",
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            isReadOnly = false,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                // 3. Filter Chips Row Item
                                item(key = "header_chips") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(scrollState)
                                            .padding(horizontal = 18.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val filters = listOf("All", "Notes", "PYQs", "Assignments", "Cheat Sheets", "Videos")

                                        filters.forEach { filter ->
                                            val isSelected = filter == selectedFilter
                                            val background = if (isSelected) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainerHigh
                                            }
                                            val contentColor = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                            val borderColor = if (isSelected) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                            } else {
                                                Color.Transparent
                                            }

                                            Surface(
                                                modifier = Modifier.clickable { selectedFilter = filter },
                                                shape = RoundedCornerShape(12.dp),
                                                color = background,
                                                border = BorderStroke(1.dp, borderColor)
                                            ) {
                                                Text(
                                                    text = filter,
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = contentColor,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }

                                // 4. Content Items or Empty State
                                if (displayEmpty) {
                                    item(key = "empty_state_all") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 40.dp, bottom = 40.dp)
                                        ) {
                                            if (selectedFilter == "All") {
                                                PremiumEmptyState(
                                                    title = "No bookmarks",
                                                    message = null,
                                                    icon = Icons.Default.Bookmark,
                                                    accentColor = Color(0xFFFFB45C)
                                                )
                                            } else {
                                                val emptyTitle = when (selectedFilter) {
                                                    "Notes" -> "No notes"
                                                    "PYQs" -> "No PYQs"
                                                    "Assignments" -> "No assignments"
                                                    "Cheat Sheets" -> "No cheat sheets"
                                                    "Videos" -> "No videos"
                                                    else -> "No content"
                                                }
                                                val emptyIcon = when (selectedFilter) {
                                                    "Notes" -> Icons.Default.Description
                                                    "PYQs" -> Icons.Default.Help
                                                    "Assignments" -> Icons.Default.Assignment
                                                    "Cheat Sheets" -> Icons.Default.Bolt
                                                    "Videos" -> Icons.Default.PlayArrow
                                                    else -> Icons.Default.FilePresent
                                                }
                                                val emptyColor = when (selectedFilter) {
                                                    "Notes" -> Color(0xFF58D6D1)
                                                    "PYQs" -> Color(0xFFFFB45C)
                                                    "Assignments" -> Color(0xFF7AD7FF)
                                                    "Cheat Sheets" -> Color(0xFFC7A6FF)
                                                    "Videos" -> Color(0xFFFF6B6B)
                                                    else -> Color(0xFFCFD8DC)
                                                }
                                                PremiumEmptyState(
                                                    title = emptyTitle,
                                                    message = null,
                                                    icon = emptyIcon,
                                                    accentColor = emptyColor
                                                )
                                            }
                                        }
                                    }
                                } else if (filteredFiles.isEmpty()) {
                                    item(key = "empty_state_filtered") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 40.dp, bottom = 40.dp)
                                        ) {
                                            if (searchQuery.isNotEmpty()) {
                                                PremiumEmptyState(
                                                    title = "No matching bookmarks found.",
                                                    message = null,
                                                    icon = Icons.Default.Bookmark,
                                                    accentColor = Color(0xFFFFB45C)
                                                )
                                            } else {
                                                val emptyTitle = when (selectedFilter) {
                                                    "Notes" -> "No notes"
                                                    "PYQs" -> "No PYQs"
                                                    "Assignments" -> "No assignments"
                                                    "Cheat Sheets" -> "No cheat sheets"
                                                    "Videos" -> "No videos"
                                                    else -> "No content"
                                                }
                                                val emptyIcon = when (selectedFilter) {
                                                    "Notes" -> Icons.Default.Description
                                                    "PYQs" -> Icons.Default.Help
                                                    "Assignments" -> Icons.Default.Assignment
                                                    "Cheat Sheets" -> Icons.Default.Bolt
                                                    "Videos" -> Icons.Default.PlayArrow
                                                    else -> Icons.Default.FilePresent
                                                }
                                                val emptyColor = when (selectedFilter) {
                                                    "Notes" -> Color(0xFF58D6D1)
                                                    "PYQs" -> Color(0xFFFFB45C)
                                                    "Assignments" -> Color(0xFF7AD7FF)
                                                    "Cheat Sheets" -> Color(0xFFC7A6FF)
                                                    "Videos" -> Color(0xFFFF6B6B)
                                                    else -> Color(0xFFCFD8DC)
                                                }
                                                PremiumEmptyState(
                                                    title = emptyTitle,
                                                    message = null,
                                                    icon = emptyIcon,
                                                    accentColor = emptyColor
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    items(filteredFiles, key = { it.id }) { file ->
                                        Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                                            val isVideo = file.isVideo()
                                            val detail = videoDetailsMap[file.id]
                                            val isPlaylist = if (detail != null) {
                                                detail.youtubeResourceType == "playlist"
                                            } else {
                                                (file.documentType ?: "").lowercase(java.util.Locale.ROOT).trim().contains("playlist")
                                            }
                                            
                                            val upvoteCount = upvoteCountsMap[file.id] ?: file.upvotes
                                            val fileWithLiveStats = file.copy(
                                                upvotes = upvoteCount,
                                                documentType = if (isVideo) {
                                                    if (isPlaylist) "Playlist" else "Video"
                                                } else {
                                                    file.documentType
                                                }
                                            )
                                            StudyHubShelfCard(
                                                file = fileWithLiveStats,
                                                onClick = {
                                                    if (isVideo) {
                                                        onVideoClick(file.id)
                                                    } else {
                                                        onDocumentClick(file.id)
                                                    }
                                                },
                                                onBookmarkClick = {
                                                    pendingRemoveBookmarkFile = fileWithLiveStats
                                                }
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

    if (pendingRemoveBookmarkFile != null) {
        AlertDialog(
            onDismissRequest = { pendingRemoveBookmarkFile = null },
            title = { Text(text = "Remove this bookmark?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val file = pendingRemoveBookmarkFile
                        if (file != null) {
                            viewModel.removeBookmark(file.id)
                        }
                        pendingRemoveBookmarkFile = null
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingRemoveBookmarkFile = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Extracted matchesFilter helper based on the app's existing document classification
private fun com.pravor.notessharing.domain.model.StudyFile.matchesFilter(filter: String): Boolean {
    if (filter == "All") return true
    val docType = this.documentType ?: this.fileType.label
    val rawDocType = docType.lowercase(java.util.Locale.ROOT).trim()
    val isPyq = rawDocType.contains("pyq")
    val isCheatSheet = rawDocType.contains("cheat") || rawDocType.contains("formula")
    val isAssignment = rawDocType.contains("assignment")
    val isNotes = rawDocType.contains("notes")
    val isVideo = this.fileType == com.pravor.notessharing.domain.model.FileType.Video || rawDocType.contains("video") || rawDocType.contains("youtube")

    return when (filter) {
        "Notes" -> isNotes
        "PYQs" -> isPyq
        "Assignments" -> isAssignment
        "Cheat Sheets" -> isCheatSheet
        "Videos" -> isVideo
        else -> false
    }
}

private fun com.pravor.notessharing.domain.model.StudyFile.isVideo(): Boolean {
    val docType = this.documentType ?: this.fileType.label
    val rawDocType = docType.lowercase(java.util.Locale.ROOT).trim()
    return this.fileType == com.pravor.notessharing.domain.model.FileType.Video || rawDocType.contains("video") || rawDocType.contains("youtube")
}

package com.pravor.notessharing.ui.screens.myfiles

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pravor.notessharing.bookmarks.BookmarkUiState
import com.pravor.notessharing.bookmarks.BookmarkViewModel
import com.pravor.notessharing.ui.components.AdaptiveScrollbar
import com.pravor.notessharing.ui.components.NotesSearchBar
import com.pravor.notessharing.ui.components.StatePanel
import com.pravor.notessharing.ui.components.StudyHubShelfCard
import com.pravor.notessharing.ui.navigation.LocalBottomBarPadding

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

    LaunchedEffect(Unit) {
        viewModel.loadBookmarksForCurrentUser()
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
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen header content matching unified family language
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Quick access to saved study material",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar
                NotesSearchBar(
                    placeholder = "Search saved resources...",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Chips/Filters Row - Horizontally scrollable
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState),
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

            Spacer(modifier = Modifier.height(12.dp))

            // Main Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
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
                            val filteredFiles = if (displayEmpty) {
                                emptyList()
                            } else {
                                bookmarks.filter { it.matchesFilter(selectedFilter) }
                            }
                            
                            if (displayEmpty) {
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
                            } else {
                                if (filteredFiles.isEmpty()) {
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
                                } else {
                                    Box(Modifier.fillMaxSize()) {
                                        LazyColumn(
                                            state = listState,
                                            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 14.dp + bottomPadding),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            items(filteredFiles, key = { it.id }) { file ->
                                                StudyHubShelfCard(
                                                    file = file,
                                                    onClick = {
                                                        if (file.fileType == com.pravor.notessharing.model.FileType.Video) {
                                                            onVideoClick(file.id)
                                                        } else {
                                                            onDocumentClick(file.id)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                        AdaptiveScrollbar(listState = listState)
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

// Extracted matchesFilter helper based on the app's existing document classification
private fun com.pravor.notessharing.model.StudyFile.matchesFilter(filter: String): Boolean {
    if (filter == "All") return true
    val docType = this.documentType ?: this.fileType.label
    val rawDocType = docType.lowercase(java.util.Locale.ROOT).trim()
    val isPyq = rawDocType.contains("pyq")
    val isCheatSheet = rawDocType.contains("cheat") || rawDocType.contains("formula")
    val isAssignment = rawDocType.contains("assignment")
    val isNotes = rawDocType.contains("notes")
    val isVideo = this.fileType == com.pravor.notessharing.model.FileType.Video || rawDocType.contains("video") || rawDocType.contains("youtube")

    return when (filter) {
        "Notes" -> isNotes
        "PYQs" -> isPyq
        "Assignments" -> isAssignment
        "Cheat Sheets" -> isCheatSheet
        "Videos" -> isVideo
        else -> false
    }
}

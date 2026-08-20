package com.pravor.notessharing.ui.features.myfiles

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pravor.notessharing.ui.common.MyFilesUiState
import com.pravor.notessharing.ui.common.AdaptiveScrollbar
import com.pravor.notessharing.ui.common.NotesSearchBar
import com.pravor.notessharing.ui.common.StatePanel
import com.pravor.notessharing.ui.common.StudyHubShelfCard
import com.pravor.notessharing.ui.navigation.LocalBottomBarPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyUploadsScreen(
    onBackClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    viewModel: MyFilesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scrollState = rememberScrollState()
    val bottomPadding = LocalBottomBarPadding.current
    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Uploads",
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
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = uiState, label = "my-uploads-state", modifier = Modifier.fillMaxSize()) { state ->
                when (state) {
                    MyFilesUiState.Loading -> com.pravor.notessharing.ui.common.loading.StudyLoadingIndicator(
                        text = "Loading your uploads...",
                        modifier = Modifier.fillMaxSize()
                    )
                    is MyFilesUiState.Error -> StatePanel(
                        title = "Uploads unavailable",
                        message = state.message,
                        modifier = Modifier.padding(top = 96.dp)
                    )
                    is MyFilesUiState.Success, MyFilesUiState.Empty -> {
                        val uploadedFiles = if (state is MyFilesUiState.Success) {
                            state.content.uploadedFiles
                        } else {
                            emptyList()
                        }
                        
                        val displayEmpty = uploadedFiles.isEmpty()
                        val filteredFiles by remember(uploadedFiles, selectedFilter, searchQuery) {
                            derivedStateOf {
                                if (displayEmpty) {
                                    emptyList()
                                } else {
                                    uploadedFiles
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
                                        text = "Your contributions to the community",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 18.dp)
                                    )
                                }

                                // 2. Search Bar Item
                                item(key = "header_search") {
                                    Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                                        NotesSearchBar(
                                            placeholder = "Search uploaded files...",
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
                                                    title = "No uploads yet",
                                                    message = "Start contributing notes, PYQs and study material",
                                                    icon = Icons.Default.UploadFile,
                                                    accentColor = Color(0xFF58D6D1)
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
                                                    title = "No matching uploads found.",
                                                    message = null,
                                                    icon = Icons.Default.UploadFile,
                                                    accentColor = Color(0xFF58D6D1)
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
                                            StudyHubShelfCard(
                                                file = file,
                                                onClick = {
                                                    if (file.fileType == com.pravor.notessharing.domain.model.FileType.Video) {
                                                        onVideoClick(file.id)
                                                    } else {
                                                        onDocumentClick(file.id)
                                                    }
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

@Composable
fun PremiumEmptyState(
    title: String,
    message: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.UploadFile,
    accentColor: Color = Color(0xFF58D6D1),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.04f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.15f)),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor.copy(alpha = 0.8f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.1.sp,
                    fontSize = 20.sp
                ),
                color = Color(0xFFE2E8F0),
                textAlign = TextAlign.Center
            )

            if (!message.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp,
                        letterSpacing = 0.15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.pravor.notessharing.model.FileType
import com.pravor.notessharing.model.StudyFile
import com.pravor.notessharing.state.MyFilesUiState
import com.pravor.notessharing.ui.components.AdaptiveScrollbar
import com.pravor.notessharing.ui.components.NotesSearchBar
import com.pravor.notessharing.ui.components.StatePanel
import com.pravor.notessharing.ui.components.StudyHubShelfCard
import com.pravor.notessharing.ui.navigation.LocalBottomBarPadding
import com.pravor.notessharing.viewmodel.MyFilesViewModel

@Composable
fun MyFilesRoute(
    onBackClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    viewModel: MyFilesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadDownloads(context)
    }

    MyFilesScreen(
        onBackClick = onBackClick,
        uiState = uiState,
        onDocumentClick = onDocumentClick,
        onVideoClick = onVideoClick,
        onDeleteDownload = { docId -> viewModel.deleteDownload(docId, context) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFilesScreen(
    onBackClick: () -> Unit,
    uiState: MyFilesUiState,
    onDocumentClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    onDeleteDownload: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scrollState = rememberScrollState()
    var selectedFilter by remember { mutableStateOf("All") }
    var pendingDeleteFile by remember { mutableStateOf<StudyFile?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Files",
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
            // Header Content aligned with unified layout hierarchy
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Your downloaded study collection",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar
                NotesSearchBar(
                    placeholder = "Search downloaded files...",
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
                Crossfade(targetState = uiState, label = "my-files-state", modifier = Modifier.fillMaxSize()) { state ->
                    when (state) {
                        MyFilesUiState.Loading -> StatePanel(
                            title = "Loading downloads",
                            message = "Collecting your downloaded study collection",
                            loading = true,
                            modifier = Modifier.padding(top = 96.dp)
                        )
                        MyFilesUiState.Empty -> {
                            if (selectedFilter == "All") {
                                PremiumEmptyState(
                                    title = "No downloads",
                                    message = null,
                                    icon = Icons.Default.Download,
                                    accentColor = Color(0xFFCFD8DC)
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
                        is MyFilesUiState.Error -> StatePanel(
                            title = "Downloads unavailable",
                            message = state.message,
                            modifier = Modifier.padding(top = 96.dp)
                        )
                        is MyFilesUiState.Success -> {
                            val downloadedFiles = state.content.savedFiles
                            val displayEmpty = downloadedFiles.isEmpty()
                            val filteredFiles = if (displayEmpty) {
                                emptyList()
                            } else {
                                downloadedFiles.filter { it.matchesFilter(selectedFilter) }
                            }

                            if (displayEmpty) {
                                if (selectedFilter == "All") {
                                    PremiumEmptyState(
                                        title = "No downloads",
                                        message = null,
                                        icon = Icons.Default.Download,
                                        accentColor = Color(0xFFCFD8DC)
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
                                    val bottomPadding = LocalBottomBarPadding.current
                                    Box(Modifier.fillMaxSize()) {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            state = listState,
                                            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 14.dp + bottomPadding),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            items(filteredFiles, key = { it.id }, contentType = { "study-file" }) { file ->
                                                StudyHubShelfCard(
                                                    file = file,
                                                    onClick = {
                                                        if (file.id.contains("video", ignoreCase = true)) {
                                                            onVideoClick(file.id)
                                                        } else {
                                                            onDocumentClick(file.id)
                                                        }
                                                    },
                                                    onDeleteClick = {
                                                        pendingDeleteFile = file
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

    if (pendingDeleteFile != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteFile = null },
            title = { Text(text = "Remove Download?") },
            text = { Text(text = "This will remove the offline copy from your device. The document will remain available online.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val file = pendingDeleteFile
                        if (file != null) {
                            onDeleteDownload(file.id)
                        }
                        pendingDeleteFile = null
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingDeleteFile = null }
                ) {
                    Text("Cancel")
                }
            }
        )
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

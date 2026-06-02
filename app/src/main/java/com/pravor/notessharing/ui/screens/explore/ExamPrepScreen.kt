package com.pravor.notessharing.ui.screens.explore

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pravor.notessharing.model.TrendingNote
import com.pravor.notessharing.state.ExploreUiState
import com.pravor.notessharing.ui.components.StatePanel
import com.pravor.notessharing.ui.components.trending_components.TrendingNoteDiscoveryCard
import com.pravor.notessharing.data.DocumentDetailRepository
import com.pravor.notessharing.viewmodel.ExploreViewModel

@Composable
fun ExamPrepRoute(
    onBackClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    viewModel: ExploreViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val detailRepository = remember { DocumentDetailRepository() }

    var pendingRemoveBookmarkNote by remember { mutableStateOf<TrendingNote?>(null) }

    val onBookmarkClickRemembered = remember(viewModel) {
        { note: TrendingNote ->
            if (note.isBookmarked) {
                pendingRemoveBookmarkNote = note
            } else {
                viewModel.toggleBookmark(note)
            }
        }
    }

    ExamPrepScreen(
        uiState = uiState,
        detailRepository = detailRepository,
        onBackClick = onBackClick,
        onDocumentClick = onDocumentClick,
        onBookmarkClick = onBookmarkClickRemembered,
        onUpvoteClick = { id, docType, upvotes ->
            viewModel.toggleUpvote(id, docType, upvotes)
        }
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
}

@Composable
fun ExamPrepScreen(
    uiState: ExploreUiState,
    detailRepository: DocumentDetailRepository,
    onBackClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    onBookmarkClick: (TrendingNote) -> Unit,
    onUpvoteClick: (String, String?, Int) -> Unit
) {
    var selectedBranch by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("") } // "PYQ", "Cheat Sheet", or "" for all

    when (uiState) {
        ExploreUiState.Loading -> StatePanel("Finding topics", "Scanning dummy campus trends", loading = true)
        ExploreUiState.Empty -> StatePanel("Nothing trending", "Explore content will appear here")
        is ExploreUiState.Error -> StatePanel("Explore failed", uiState.message)
        is ExploreUiState.Success -> {
            val allTrendingNotes = uiState.content.trendingNotes

            // Frontend filtering of Exam Prep resources
            val examPrepNotes = remember(allTrendingNotes, selectedBranch, selectedType) {
                allTrendingNotes.filter { note ->
                    val docType = note.documentType.ifBlank { note.type ?: "" }.lowercase(java.util.Locale.ROOT).trim()
                    
                    // Match PYQs and Cheat Sheets
                    val isPyq = docType == "pyq" || docType == "pyqs"
                    val isCheatSheet = docType == "cheatsheet" || docType == "cheat sheet" || docType == "cheatsheets"
                    
                    if (!isPyq && !isCheatSheet) return@filter false
                    
                    // Apply selective type filtering
                    if (selectedType == "PYQ" && !isPyq) return@filter false
                    if (selectedType == "Cheat Sheet" && !isCheatSheet) return@filter false

                    // Apply selective branch filtering
                    if (selectedBranch.isNotEmpty()) {
                        val noteBranch = note.branch.lowercase(java.util.Locale.ROOT)
                        val queryBranch = selectedBranch.lowercase(java.util.Locale.ROOT)
                        if (!noteBranch.contains(queryBranch) && !queryBranch.contains(noteBranch)) return@filter false
                    }

                    true
                }
            }

            ExploreExpandedListScaffold(
                title = "Exam Prep",
                onBackClick = onBackClick
            ) {
                item(key = "exam-prep-filters", contentType = "filters") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Dropdown filter for branch
                        var dropdownExpanded by remember { mutableStateOf(false) }
                        Box {
                            Surface(
                                onClick = { dropdownExpanded = true },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val branchDisplayLabel = when (selectedBranch) {
                                        "Computer Science" -> "CS"
                                        "" -> "Branch"
                                        else -> selectedBranch
                                    }
                                    Text(
                                        text = "$branchDisplayLabel ▼",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Branches") },
                                    onClick = {
                                        selectedBranch = ""
                                        dropdownExpanded = false
                                    }
                                )
                                listOf(
                                    "Computer Science" to "Computer Science (CS)",
                                    "Electrical" to "Electrical",
                                    "Civil" to "Civil",
                                    "ECE" to "ECE",
                                    "Mechanical" to "Mechanical"
                                ).forEach { (value, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            selectedBranch = value
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // PYQ filter chip
                        val isPyqSelected = selectedType == "PYQ"
                        Surface(
                            onClick = { selectedType = if (isPyqSelected) "" else "PYQ" },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isPyqSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(if (isPyqSelected) 1.5.dp else 1.dp, if (isPyqSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = "PYQs",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isPyqSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Cheat Sheets filter chip
                        val isCsSelected = selectedType == "Cheat Sheet"
                        Surface(
                            onClick = { selectedType = if (isCsSelected) "" else "Cheat Sheet" },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCsSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(if (isCsSelected) 1.5.dp else 1.dp, if (isCsSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = "Cheat Sheets",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isCsSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (examPrepNotes.isEmpty()) {
                    item(key = "exam-prep-empty", contentType = "empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No exam prep resources found matching the filter criteria.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        items = examPrepNotes,
                        key = { index, note -> note.id.ifBlank { "exam-prep-card-$index" } },
                        contentType = { _, _ -> "exam-prep-card" }
                    ) { _, note ->
                        TrendingNoteDiscoveryCard(
                            note = note,
                            detailRepository = detailRepository,
                            onClick = { onDocumentClick(note.id) },
                            onBookmarkClick = { onBookmarkClick(note) },
                            onUpvoteClick = { onUpvoteClick(note.id, note.documentType, note.upvotes) }
                        )
                    }
                }
            }
        }
    }
}

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
fun AssignmentsRoute(
    onBackClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    viewModel: ExploreViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
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

    AssignmentsScreen(
        uiState = uiState,
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.loadRealDocuments(isPullToRefresh = true) },
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
fun AssignmentsScreen(
    uiState: ExploreUiState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    detailRepository: DocumentDetailRepository,
    onBackClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    onBookmarkClick: (TrendingNote) -> Unit,
    onUpvoteClick: (String, String?, Int) -> Unit
) {
    var selectedBranch by remember { mutableStateOf("") }

    when (uiState) {
        ExploreUiState.Loading -> StatePanel("Finding topics", "Scanning campus trends", loading = true)
        ExploreUiState.Empty -> StatePanel("Nothing trending", "Explore content will appear here")
        is ExploreUiState.Error -> StatePanel("Explore failed", uiState.message)
        is ExploreUiState.Success -> {
            val allTrendingNotes = uiState.content.trendingNotes

            // Frontend filtering of Assignments resources
            val assignmentNotes = remember(allTrendingNotes, selectedBranch) {
                allTrendingNotes.filter { note ->
                    val docType = note.documentType.ifBlank { note.type ?: "" }.lowercase(java.util.Locale.ROOT).trim()
                    
                    // Match Assignments
                    val isAssignment = docType == "assignment" || docType == "assignments"
                    
                    if (!isAssignment) return@filter false

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
                title = "Assignments",
                onBackClick = onBackClick,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh
            ) {
                item(key = "assignments-filters", contentType = "filters") {
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
                    }
                }

                if (assignmentNotes.isEmpty()) {
                    item(key = "assignments-empty", contentType = "empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No assignment resources found matching the branch.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        items = assignmentNotes,
                        key = { index, note -> note.id.ifBlank { "assignment-card-$index" } },
                        contentType = { _, _ -> "assignment-card" }
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

package com.pravor.notessharing.ui.features.explore

import com.pravor.notessharing.ui.features.explore.components.*

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*

import com.pravor.notessharing.ui.features.trending.components.TrendingNoteDiscoveryCardContent
import com.pravor.notessharing.ui.features.trending.components.*

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
import com.pravor.notessharing.domain.model.TrendingNote
import com.pravor.notessharing.ui.common.ExploreUiState
import com.pravor.notessharing.ui.common.components.StatePanel
import com.pravor.notessharing.ui.features.trending.components.TrendingNoteDiscoveryCard
import com.pravor.notessharing.data.repository.DocumentDetailRepository

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
    var searchQuery by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("") }

    when (uiState) {
        ExploreUiState.Loading -> StatePanel("Finding topics", "Scanning campus trends", loading = true)
        ExploreUiState.Empty -> StatePanel("Nothing trending", "Explore content will appear here")
        is ExploreUiState.Error -> StatePanel("Explore failed", uiState.message)
        is ExploreUiState.Success -> {
            val allTrendingNotes = uiState.content.assignments

            val subjectList = remember(allTrendingNotes) {
                allTrendingNotes.mapNotNull { it.subject.takeIf { s -> s.isNotBlank() } }
                    .map { com.pravor.notessharing.data.repository.SubjectCatalogRepository.getInstance().resolveDisplayName(it, it).trim() }
                    .distinct()
            }

            // Frontend filtering of Assignments resources using pre-ranked dataset
            val assignmentNotes = remember(allTrendingNotes, searchQuery, selectedSubject) {
                val q = searchQuery.trim().lowercase(java.util.Locale.ROOT)
                val normSelected = if (selectedSubject.isNotBlank()) com.pravor.notessharing.ui.common.utils.normalizeSubject(selectedSubject) else ""

                allTrendingNotes.filter { note ->
                    // Apply selective subject filtering
                    if (normSelected.isNotEmpty()) {
                        val noteNormSubj = com.pravor.notessharing.ui.common.utils.normalizeSubject(note.subject)
                        val noteDispSubj = com.pravor.notessharing.ui.common.utils.normalizeSubject(note.displaySubject ?: "")
                        if (noteNormSubj != normSelected && noteDispSubj != normSelected) {
                            return@filter false
                        }
                    }

                    // Apply local search query
                    if (q.isNotEmpty()) {
                        val matchesTitle = note.title.lowercase(java.util.Locale.ROOT).contains(q)
                        val matchesSubj = note.subject.lowercase(java.util.Locale.ROOT).contains(q)
                        val matchesDesc = note.description.lowercase(java.util.Locale.ROOT).contains(q)
                        val matchesUploader = note.uploaderName.lowercase(java.util.Locale.ROOT).contains(q)
                        if (!matchesTitle && !matchesSubj && !matchesDesc && !matchesUploader) {
                            return@filter false
                        }
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
                item(key = "assignments-controls", contentType = "controls") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        com.pravor.notessharing.ui.common.components.LocalSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholderText = "Search assignments..."
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

                if (assignmentNotes.isEmpty()) {
                    item(key = "assignments-empty", contentType = "empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No assignment resources found matching the search criteria.",
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

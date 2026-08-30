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

import com.pravor.notessharing.domain.model.ResourceType

@Composable
fun ExamPrepRoute(
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

    ExamPrepScreen(
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
fun ExamPrepScreen(
    uiState: ExploreUiState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    detailRepository: DocumentDetailRepository,
    onBackClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    onBookmarkClick: (TrendingNote) -> Unit,
    onUpvoteClick: (String, String?, Int) -> Unit
) {
    var selectedType by remember { mutableStateOf("") } // "PYQ", "Cheat Sheet", or "" for all
    var searchQuery by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("") }

    when (uiState) {
        ExploreUiState.Loading -> StatePanel("Finding topics", "Scanning campus trends", loading = true)
        ExploreUiState.Empty -> StatePanel("Nothing trending", "Explore content will appear here")
        is ExploreUiState.Error -> StatePanel("Explore failed", uiState.message)
        is ExploreUiState.Success -> {
            val allTrendingNotes = uiState.content.examPrep

            val subjectList = remember(allTrendingNotes) {
                allTrendingNotes.mapNotNull { it.subject.takeIf { s -> s.isNotBlank() } }
                    .map { com.pravor.notessharing.data.repository.SubjectCatalogRepository.getInstance().resolveDisplayName(it, it).trim() }
                    .distinct()
            }

            // Frontend filtering of Exam Prep resources using pre-ranked dataset
            val examPrepNotes = remember(allTrendingNotes, selectedType, searchQuery, selectedSubject) {
                val q = searchQuery.trim().lowercase(java.util.Locale.ROOT)
                val normSelected = if (selectedSubject.isNotBlank()) com.pravor.notessharing.ui.common.utils.normalizeSubject(selectedSubject) else ""

                allTrendingNotes.filter { note ->
                    val docType = (note.documentType.ifBlank { note.type ?: "" }).trim().lowercase(java.util.Locale.ROOT)
                    val isPyq = note.resourceType == ResourceType.PYQ || docType in listOf("pyq", "pyqs")
                    val isCheatSheet = note.resourceType == ResourceType.CHEAT_SHEET || docType in listOf("cheat sheet", "cheatsheet", "cheatsheets", "cheat_sheet")

                    // Apply selective type filtering
                    if (selectedType == "PYQ" && !isPyq) return@filter false
                    if (selectedType == "Cheat Sheet" && !isCheatSheet) return@filter false

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
                        val matchesYear = note.examYear?.lowercase(java.util.Locale.ROOT)?.contains(q) ?: false
                        if (!matchesTitle && !matchesSubj && !matchesDesc && !matchesUploader && !matchesYear) {
                            return@filter false
                        }
                    }

                    true
                }
            }

            ExploreExpandedListScaffold(
                title = "Exam Prep",
                onBackClick = onBackClick,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh
            ) {
                item(key = "exam-prep-controls", contentType = "controls") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        com.pravor.notessharing.ui.common.components.LocalSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholderText = "Search PYQs and Cheat Sheets..."
                        )

                        if (subjectList.isNotEmpty()) {
                            com.pravor.notessharing.ui.common.components.SubjectFilterRow(
                                subjects = subjectList,
                                selectedSubject = selectedSubject,
                                onSelectSubject = { selectedSubject = it }
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // All Types filter chip
                            val isAllTypeSelected = selectedType.isEmpty()
                            Surface(
                                onClick = { selectedType = "" },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isAllTypeSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                                border = BorderStroke(if (isAllTypeSelected) 1.5.dp else 1.dp, if (isAllTypeSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    text = "All Types",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isAllTypeSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isAllTypeSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
                                    fontWeight = if (isPyqSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isPyqSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Cheat Sheet filter chip
                            val isCheatSheetSelected = selectedType == "Cheat Sheet"
                            Surface(
                                onClick = { selectedType = if (isCheatSheetSelected) "" else "Cheat Sheet" },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCheatSheetSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                                border = BorderStroke(if (isCheatSheetSelected) 1.5.dp else 1.dp, if (isCheatSheetSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    text = "Cheat Sheets",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isCheatSheetSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCheatSheetSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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

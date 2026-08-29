package com.pravor.notessharing.ui.features.explore.components

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayCircleFilled
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pravor.notessharing.domain.model.TrendingNote
import com.pravor.notessharing.domain.model.VideoRecommendation
import com.pravor.notessharing.domain.model.ResourceType
import com.pravor.notessharing.ui.common.ExploreContent
import com.pravor.notessharing.ui.common.components.SectionHeader
import com.pravor.notessharing.ui.navigation.LocalBottomBarPadding

@Composable
fun ExploreSuccessContent(
    content: ExploreContent,
    listState: LazyListState,
    allowedSubjects: List<com.pravor.notessharing.ui.features.upload.CatalogSubject>,
    onTrendingSeeMoreClick: () -> Unit,
    onRecommendedVideosSeeMoreClick: () -> Unit,
    onDiscoverSeeMoreClick: () -> Unit = {},
    onExamPrepSeeMoreClick: () -> Unit,
    onAssignmentsSeeMoreClick: () -> Unit,
    onSubjectSeeMoreClick: (String) -> Unit,
    onDocumentClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    onBookmarkClick: (TrendingNote) -> Unit,
    onVideoBookmarkClick: (VideoRecommendation) -> Unit = {},
    onUpvoteClick: (String, String?, Int) -> Unit = { _, _, _ -> },
    onSearchClick: () -> Unit
) {
    val bottomPadding = LocalBottomBarPadding.current
    val isDark = isSystemInDarkTheme()

    val trendingRowState = rememberLazyListState()
    var isTrendingScrollable by remember { mutableStateOf(false) }

    val examPrepRowState = rememberLazyListState()
    var isExamPrepScrollable by remember { mutableStateOf(false) }

    val assignmentsRowState = rememberLazyListState()
    var isAssignmentsScrollable by remember { mutableStateOf(false) }
    val trendingColor = if (isDark) Color(0xFFFF9F55) else Color(0xFFE65100)
    val videosColor = if (isDark) Color(0xFFFF6B6B) else Color(0xFFC0392B)
    val examPrepColor = if (isDark) Color(0xFFC7A6FF) else Color(0xFF8E44AD)
    val assignmentsColor = if (isDark) Color(0xFF7CB7FF) else Color(0xFF1B67B3)
    val subjectsColor = if (isDark) Color(0xFF73E0B1) else Color(0xFF167356)

    // 1. Trending Notes (Preview takes 7)
    val filteredTrending = content.notes
    val visibleTrendingNotes = remember(filteredTrending) {
        filteredTrending.take(7)
    }

    // 2. Curated Recommended Videos / Playlists (Preview takes 3)
    val visibleRecommendedVideos = remember(content.videos) {
        content.videos.take(3)
    }

    // 3. Exam Prep resources (Preview takes 7)
    val filteredExamPrep = content.examPrep
    val visibleExamPrep = remember(filteredExamPrep) {
        filteredExamPrep.take(7)
    }

    // 4. Assignments (Preview takes 7)
    val filteredAssignments = content.assignments
    val visibleAssignments = remember(filteredAssignments) {
        filteredAssignments.take(7)
    }

    // 5. Subject Section resources grouping (semester-aware and catalog-driven)
    val resourcesBySubject = remember(content.notes, content.examPrep, content.assignments, content.videos, allowedSubjects) {
        allowedSubjects.map { catalogSubject ->
            val matchingResources = mutableListOf<Any>()
            val normalizedCatId = com.pravor.notessharing.ui.common.utils.normalizeSubject(catalogSubject.id)
            val normalizedCatName = com.pravor.notessharing.ui.common.utils.normalizeSubject(catalogSubject.name)

            val allItems = content.notes + content.examPrep + content.assignments + content.videos
            allItems.forEach { note ->
                if (note.subject.isNotBlank()) {
                    val normalizedRes = com.pravor.notessharing.ui.common.utils.normalizeSubject(note.subject)
                    if (normalizedRes == normalizedCatId || normalizedRes == normalizedCatName) {
                        matchingResources.add(note)
                    }
                }
            }

            Pair(catalogSubject, matchingResources)
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            state = listState,
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 14.dp + bottomPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "explore-header", contentType = "header") {
                ExploreHeader(onSearchClick = onSearchClick)
            }

            // Trending Notes Section
            item(key = "trending-title", contentType = "section") {
                SectionHeader(
                    title = "Trending Notes",
                    icon = Icons.Rounded.LocalFireDepartment,
                    iconTint = trendingColor,
                    onSeeMoreClick = if (filteredTrending.size > visibleTrendingNotes.size && !isTrendingScrollable) onTrendingSeeMoreClick else null
                )
            }
            if (visibleTrendingNotes.isEmpty()) {
                item(key = "trending-empty", contentType = "empty") {
                    Text(
                        text = "No trending notes available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                item(key = "trending-carousel", contentType = "carousel") {
                    ScrollableRowWithIndicator(
                        state = trendingRowState,
                        onSeeMoreClick = if (filteredTrending.size > visibleTrendingNotes.size) onTrendingSeeMoreClick else null,
                        accentColor = trendingColor,
                        onScrollableChanged = { isTrendingScrollable = it }
                    ) {
                        itemsIndexed(
                            items = visibleTrendingNotes,
                            key = { index, note -> note.id.ifBlank { "trending_note_$index" } },
                            contentType = { _, _ -> "trending-note" }
                        ) { _, note ->
                            val onBookmarkClickRemembered = remember(note) {
                                { onBookmarkClick(note) }
                            }
                            val onClickRemembered = remember(note.id) {
                                { onDocumentClick(note.id) }
                            }
                            val onUpvoteClickRemembered = remember(note.id, note.documentType, note.upvotes) {
                                { onUpvoteClick(note.id, note.documentType, note.upvotes) }
                            }
                            TrendingNoteCard(
                                note = note,
                                onBookmarkClick = onBookmarkClickRemembered,
                                onClick = onClickRemembered,
                                onUpvoteClick = onUpvoteClickRemembered
                            )
                        }
                    }
                }
            }

            // Recommended Videos Section
            item(key = "videos-title", contentType = "section") {
                SectionHeader(
                    title = "Recommended Videos",
                    icon = Icons.Rounded.PlayCircleFilled,
                    iconTint = videosColor,
                    onSeeMoreClick = null
                )
            }
            if (visibleRecommendedVideos.isEmpty()) {
                item(key = "videos-empty", contentType = "empty") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, videosColor.copy(alpha = 0.28f)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF160D10).copy(alpha = 0.74f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = videosColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "No videos available yet",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                item(key = "videos-container", contentType = "videos_container") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF141922).copy(alpha = 0.74f),
                        border = BorderStroke(1.dp, videosColor.copy(alpha = 0.28f)),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            visibleRecommendedVideos.forEach { video ->
                                androidx.compose.runtime.key(video.id) {
                                    val onClickRemembered = remember(video.id) {
                                        { onVideoClick(video.id) }
                                    }
                                    val onUpvoteClickRemembered = remember(video.id, video.documentType, video.upvotes) {
                                        { onUpvoteClick(video.id, video.documentType, video.upvotes) }
                                    }
                                    val onBookmarkClickRemembered = remember(video) {
                                        { onVideoBookmarkClick(video.toVideoRecommendation()) }
                                    }
                                    VideoRecommendationCard(
                                        video = video.toVideoRecommendation(),
                                        isUpvoted = video.isUpvoted,
                                        onClick = onClickRemembered,
                                        onUpvoteClick = onUpvoteClickRemembered,
                                        onBookmarkClick = onBookmarkClickRemembered
                                    )
                                }
                            }
                            
                            if (content.videos.size > 3) {
                                FilledTonalButton(
                                    onClick = onRecommendedVideosSeeMoreClick,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "See More Videos",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Exam Prep Section
            item(key = "examprep-title", contentType = "section") {
                SectionHeader(
                    title = "Exam Prep",
                    icon = Icons.Rounded.EditNote,
                    iconTint = examPrepColor,
                    onSeeMoreClick = if (filteredExamPrep.size > visibleExamPrep.size && !isExamPrepScrollable) onExamPrepSeeMoreClick else null
                )
            }
            if (visibleExamPrep.isEmpty()) {
                item(key = "examprep-empty", contentType = "empty") {
                    Text(
                        text = "No exam prep resources available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                item(key = "examprep-carousel", contentType = "carousel") {
                    ScrollableRowWithIndicator(
                        state = examPrepRowState,
                        onSeeMoreClick = if (filteredExamPrep.size > visibleExamPrep.size) onExamPrepSeeMoreClick else null,
                        accentColor = examPrepColor,
                        onScrollableChanged = { isExamPrepScrollable = it }
                    ) {
                        itemsIndexed(
                            items = visibleExamPrep,
                            key = { index, note -> note.id.ifBlank { "examprep_note_$index" } },
                            contentType = { _, _ -> "examprep-note" }
                        ) { _, note ->
                            val onBookmarkClickRemembered = remember(note) {
                                { onBookmarkClick(note) }
                            }
                            val onClickRemembered = remember(note.id) {
                                { onDocumentClick(note.id) }
                            }
                            val onUpvoteClickRemembered = remember(note.id, note.documentType, note.upvotes) {
                                { onUpvoteClick(note.id, note.documentType, note.upvotes) }
                            }
                            TrendingNoteCard(
                                note = note,
                                onBookmarkClick = onBookmarkClickRemembered,
                                onClick = onClickRemembered,
                                onUpvoteClick = onUpvoteClickRemembered
                            )
                        }
                    }
                }
            }

            // Assignments Section
            item(key = "assignments-title", contentType = "section") {
                SectionHeader(
                    title = "Assignments",
                    icon = Icons.AutoMirrored.Rounded.Assignment,
                    iconTint = assignmentsColor,
                    onSeeMoreClick = if (filteredAssignments.size > visibleAssignments.size && !isAssignmentsScrollable) onAssignmentsSeeMoreClick else null
                )
            }
            if (visibleAssignments.isEmpty()) {
                item(key = "assignments-empty", contentType = "empty") {
                    Text(
                        text = "No assignment resources available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                item(key = "assignments-carousel", contentType = "carousel") {
                    ScrollableRowWithIndicator(
                        state = assignmentsRowState,
                        onSeeMoreClick = if (filteredAssignments.size > visibleAssignments.size) onAssignmentsSeeMoreClick else null,
                        accentColor = assignmentsColor,
                        onScrollableChanged = { isAssignmentsScrollable = it }
                    ) {
                        itemsIndexed(
                            items = visibleAssignments,
                            key = { index, note -> note.id.ifBlank { "assignment_note_$index" } },
                            contentType = { _, _ -> "assignment-note" }
                        ) { _, note ->
                            val onBookmarkClickRemembered = remember(note) {
                                { onBookmarkClick(note) }
                            }
                            val onClickRemembered = remember(note.id) {
                                { onDocumentClick(note.id) }
                            }
                            val onUpvoteClickRemembered = remember(note.id, note.documentType, note.upvotes) {
                                { onUpvoteClick(note.id, note.documentType, note.upvotes) }
                            }
                            TrendingNoteCard(
                                note = note,
                                onBookmarkClick = onBookmarkClickRemembered,
                                onClick = onClickRemembered,
                                onUpvoteClick = onUpvoteClickRemembered
                            )
                        }
                    }
                }
            }

            // Subject Hero Section (All-in-one grouped by subject)
            item(key = "subjecthero-title", contentType = "section") {
                SectionHeader(
                    title = "Subjects",
                    icon = Icons.Rounded.School,
                    iconTint = subjectsColor
                )
            }
            if (resourcesBySubject.isEmpty()) {
                item(key = "subjecthero-empty", contentType = "empty") {
                    Text(
                        text = "No subjects grouped yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                itemsIndexed(
                    items = resourcesBySubject,
                    key = { _, pair -> "subject_${pair.first.id}" },
                    contentType = { _, _ -> "subject-card" }
                ) { _, (catalogSubject, resources) ->
                    SubjectHeroCard(
                        subjectName = catalogSubject.name,
                        subjectId = catalogSubject.id,
                        resources = resources,
                        onDocumentClick = onDocumentClick,
                        onVideoClick = onVideoClick,
                        onSeeMoreClick = { onSubjectSeeMoreClick(catalogSubject.id) }
                    )
                }
            }
        }
        ClimbingMascotScrollbar(listState = listState) { modifier, isScrolling ->
            MonkeyMascot(modifier = modifier, isScrolling = isScrolling)
        }
    }
}

@Composable
fun SubjectHeroCard(
    subjectName: String,
    subjectId: String,
    resources: List<Any>,
    onDocumentClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    onSeeMoreClick: () -> Unit
) {
    val normalized = remember(subjectId) { com.pravor.notessharing.ui.common.utils.normalizeSubject(subjectId) }
    val accentColor = remember(normalized) { com.pravor.notessharing.ui.common.utils.getSubjectColor(normalized) }
    val displayName = remember(subjectName, normalized) { com.pravor.notessharing.ui.common.utils.getSubjectDisplayName(subjectName, normalized) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.32f)),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF13171E).copy(alpha = 0.74f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.25f),
                    modifier = Modifier.size(8.dp, 24.dp)
                ) {}
                Text(
                    text = displayName.trim().uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // List resources
            if (resources.isEmpty()) {
                Text(
                    text = "No resources yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                )
            } else {
                resources.take(4).forEachIndexed { index, res ->
                    val (title, icon, onClick) = when (res) {
                        is TrendingNote -> {
                            if (res.resourceType == ResourceType.VIDEO || res.resourceType == ResourceType.PLAYLIST) {
                                Triple(res.title, Icons.Default.PlayArrow, { onVideoClick(res.id) })
                            } else {
                                val docType = res.documentType.ifBlank { res.type ?: "Notes" }.lowercase(java.util.Locale.ROOT).trim()
                                val resIcon = when {
                                    docType.contains("pyq") -> Icons.Default.Help
                                    docType.contains("assignment") -> Icons.Default.Assignment
                                    docType.contains("cheat") -> Icons.Default.Bolt
                                    else -> Icons.Default.Description
                                }
                                Triple(res.title.ifBlank { res.subject }, resIcon, { onDocumentClick(res.id) })
                            }
                        }
                        is VideoRecommendation -> {
                            Triple(res.title, Icons.Default.PlayArrow, { onVideoClick(res.id) })
                        }
                        else -> Triple("Curated Resource", Icons.Default.Description, {})
                    }

                    Surface(
                        onClick = onClick,
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (index < resources.size - 1 && index < 3) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            if (resources.size > 4) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                // See More ▼ Row (only show when resources.size > 4)
                Surface(
                    onClick = onSeeMoreClick,
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "See More ▼",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                }
            }
        }
    }
}

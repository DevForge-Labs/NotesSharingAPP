package com.pravor.notessharing.ui.screens.explore

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pravor.notessharing.model.Contributor
import com.pravor.notessharing.model.DiscoverFeedItem
import com.pravor.notessharing.model.RevisionCard
import com.pravor.notessharing.model.StudyCollection
import com.pravor.notessharing.model.TrendingNote
import com.pravor.notessharing.model.VideoRecommendation
import com.pravor.notessharing.state.ExploreContent
import com.pravor.notessharing.state.ExploreUiState
import com.pravor.notessharing.ui.components.AdaptiveScrollbar
import com.pravor.notessharing.ui.components.Avatar
import com.pravor.notessharing.ui.components.ScrollbarOrientation
import com.pravor.notessharing.ui.components.SectionHeader
import com.pravor.notessharing.ui.components.StatePanel
import com.pravor.notessharing.viewmodel.ExploreViewModel

@Composable
fun ExploreRoute(viewModel: ExploreViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExploreScreen(uiState = uiState)
}

@Composable
fun ExploreScreen(uiState: ExploreUiState, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()

    Crossfade(targetState = uiState, label = "explore-state", modifier = modifier.fillMaxSize()) { state ->
        when (state) {
            ExploreUiState.Loading -> StatePanel("Finding topics", "Scanning dummy campus trends", loading = true, modifier = Modifier.padding(top = 96.dp))
            ExploreUiState.Empty -> StatePanel("Nothing trending", "Explore content will appear here", modifier = Modifier.padding(top = 96.dp))
            is ExploreUiState.Error -> StatePanel("Explore failed", state.message, modifier = Modifier.padding(top = 96.dp))
            is ExploreUiState.Success -> ExploreSuccessContent(
                content = state.content,
                listState = listState
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExploreSuccessContent(
    content: ExploreContent,
    listState: LazyListState
) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "explore-header", contentType = "header") {
                ExploreHeader()
            }
            item(key = "trending-title", contentType = "section") {
                SectionHeader("🔥 Trending Notes")
            }
            item(key = "trending-carousel", contentType = "carousel") {
                ScrollableRowWithIndicator {
                    items(content.trendingNotes, key = { it.id }, contentType = { "trending-note" }) { note ->
                        TrendingNoteCard(note)
                    }
                }
            }
            item(key = "videos-title", contentType = "section") {
                SectionHeader("📺 Recommended Videos")
            }
            items(
                items = content.videoRecommendations,
                key = { it.id },
                contentType = { "video" }
            ) { video ->
                VideoRecommendationCard(video)
            }
            item(key = "collections-title", contentType = "section") {
                SectionHeader("📚 Study Collections")
            }
            item(key = "collections-carousel", contentType = "carousel") {
                ScrollableRowWithIndicator {
                    items(content.studyCollections, key = { it.id }, contentType = { "collection" }) { collection ->
                        CollectionCard(collection)
                    }
                }
            }
            item(key = "subjects-title", contentType = "section") {
                SectionHeader("📖 Subjects")
            }
            item(key = "subject-hubs", contentType = "chips") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    content.subjectHubs.forEach { subject ->
                        SubjectChip(subject)
                    }
                }
            }
            item(key = "contributors-title", contentType = "section") {
                SectionHeader("⭐ Top Contributors")
            }
            item(key = "contributors-carousel", contentType = "carousel") {
                ScrollableRowWithIndicator {
                    items(content.topContributors, key = { it.id }, contentType = { "contributor" }) { contributor ->
                        ContributorCard(contributor)
                    }
                }
            }
            item(key = "exam-banner", contentType = "banner") {
                ExamPrepBanner()
            }
            item(key = "revision-title", contentType = "section") {
                SectionHeader("💡 Quick Revision")
            }
            item(key = "revision-carousel", contentType = "carousel") {
                ScrollableRowWithIndicator {
                    items(content.revisionCards, key = { it.id }, contentType = { "revision" }) { revision ->
                        RevisionCard(revision)
                    }
                }
            }
            item(key = "discover-title", contentType = "section") {
                SectionHeader("🌍 Discover")
            }
            items(
                items = content.discoverItems,
                key = { it.id },
                contentType = {
                    when (it) {
                        is DiscoverFeedItem.Collection -> "discover-collection"
                        is DiscoverFeedItem.ContributorPost -> "discover-contributor"
                        is DiscoverFeedItem.Note -> "discover-note"
                        is DiscoverFeedItem.Video -> "discover-video"
                    }
                }
            ) { item ->
                DiscoverFeedItem(item)
            }
        }
        AdaptiveScrollbar(listState = listState)
    }
}

@Composable
private fun ExploreHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "Explore",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 5.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Search notes, subjects, playlists...",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = {}) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun ScrollableRowWithIndicator(
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    val rowState = rememberLazyListState()
    Box {
        LazyRow(
            state = rowState,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 6.dp),
            content = content
        )
        AdaptiveScrollbar(
            listState = rowState,
            orientation = ScrollbarOrientation.Horizontal
        )
    }
}

@Composable
fun TrendingNoteCard(note: TrendingNote) {
    PressScaleSurface(
        modifier = Modifier.width(216.dp),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            Modifier
                .padding(14.dp)
        ) {
            Thumbnail(
                label = note.subject,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(40.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = note.subject,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SmallMetric(Icons.Default.Download, note.downloads.toString())
                Spacer(Modifier.width(10.dp))
                SmallMetric(Icons.Default.Star, String.format("%.1f", note.rating))
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = if (note.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(note.upvotes.toString())
            }
        }
    }
}

@Composable
fun VideoRecommendationCard(video: VideoRecommendation) {
    PressScaleSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Thumbnail(
                label = video.duration,
                modifier = Modifier
                    .width(116.dp)
                    .height(76.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = video.channelName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {},
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Watch")
                }
            }
        }
    }
}

@Composable
fun CollectionCard(collection: StudyCollection) {
    PressScaleSurface(
        modifier = Modifier.width(226.dp),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = collection.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${collection.notes} notes | ${collection.pyqs} PYQs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${collection.playlists} playlists | ${collection.cheatSheets} cheat sheets",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SubjectChip(subject: String) {
    val background by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "subject-chip-background"
    )
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = background,
        tonalElevation = 3.dp
    ) {
        Text(
            text = subject,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ContributorCard(contributor: Contributor) {
    PressScaleSurface(
        modifier = Modifier.width(164.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Avatar(contributor.initials)
            Spacer(Modifier.height(10.dp))
            Text(
                text = contributor.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${contributor.uploads} uploads | ${String.format("%.1f", contributor.rating)} rating",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {},
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text("Follow")
            }
        }
    }
}

@Composable
private fun ExamPrepBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Text(
                text = "🎯 Mid-Sem Preparation Hub",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Most downloaded notes this week, curated for quick revision before exams.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f)
            )
        }
    }
}

@Composable
fun RevisionCard(revision: RevisionCard) {
    PressScaleSurface(
        modifier = Modifier.width(214.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = revision.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            revision.points.take(4).forEachIndexed { index, point ->
                Text(
                    text = "${index + 1}. $point",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DiscoverFeedItem(item: DiscoverFeedItem) {
    PressScaleSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        when (item) {
            is DiscoverFeedItem.Note -> DiscoverRow(
                marker = "N",
                title = item.title,
                subtitle = "${item.subject} | ${item.downloads} downloads",
                icon = Icons.Default.BookmarkBorder
            )
            is DiscoverFeedItem.Video -> DiscoverRow(
                marker = "V",
                title = item.title,
                subtitle = "${item.channelName} | ${item.duration}",
                icon = Icons.Default.PlayArrow
            )
            is DiscoverFeedItem.Collection -> DiscoverRow(
                marker = "C",
                title = item.title,
                subtitle = "${item.resourceCount} resources in this collection",
                icon = Icons.Default.Bookmark
            )
            is DiscoverFeedItem.ContributorPost -> DiscoverRow(
                marker = item.initials,
                title = item.name,
                subtitle = item.message,
                icon = Icons.Default.Star
            )
        }
    }
}

@Composable
private fun DiscoverRow(
    marker: String,
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = marker.take(2),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun Thumbnail(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.tertiaryContainer
                    )
                ),
                RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
    }
}

@Composable
private fun SmallMetric(icon: ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PressScaleSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "card-press-scale")

    Surface(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {}
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
        content = content
    )
}

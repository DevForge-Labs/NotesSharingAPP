package com.pravor.notessharing.ui.components.explore_components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pravor.notessharing.model.DiscoverFeedItem
import com.pravor.notessharing.state.ExploreContent
import com.pravor.notessharing.ui.components.AdaptiveScrollbar
import com.pravor.notessharing.ui.components.SectionHeader

@Composable
fun ExploreSuccessContent(
    content: ExploreContent,
    listState: LazyListState,
    onTrendingSeeMoreClick: () -> Unit,
    onRecommendedVideosSeeMoreClick: () -> Unit,
    onDiscoverSeeMoreClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    onVideoClick: (String) -> Unit
) {
    val visibleTrendingNotes = content.trendingNotes.take(7)
    val visibleRecommendedVideos = content.videoRecommendations.take(4)
    val visibleDiscoverItems = content.discoverItems.take(4)

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader("🔥 Trending Notes", modifier = Modifier.weight(1f))
                    if (content.trendingNotes.size > visibleTrendingNotes.size) {
                        TextButton(onClick = onTrendingSeeMoreClick) {
                            Text("See More")
                        }
                    }
                }
            }
            item(key = "trending-carousel", contentType = "carousel") {
                ScrollableRowWithIndicator {
                    itemsIndexed(
                        items = visibleTrendingNotes,
                        key = { index, note -> note.id.ifBlank { "trending_note_$index" } },
                        contentType = { _, _ -> "trending-note" }
                    ) { _, note ->
                        TrendingNoteCard(note, onClick = { onDocumentClick(note.id) })
                    }
                }
            }
            item(key = "videos-title", contentType = "section") {
                SectionHeader("📺 Recommended Videos")
            }
            if (visibleRecommendedVideos.isEmpty()) {
                item(key = "videos-empty", contentType = "empty") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
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
                itemsIndexed(
                    items = visibleRecommendedVideos,
                    key = { index, video -> video.id.ifBlank { "video_$index" } },
                    contentType = { _, _ -> "video" }
                ) { _, video ->
                    VideoRecommendationCard(
                        video = video,
                        onClick = { onVideoClick(video.id) }
                    )
                }
                if (content.videoRecommendations.size > visibleRecommendedVideos.size) {
                    item(key = "videos-see-more", contentType = "action") {
                        TextButton(
                            onClick = onRecommendedVideosSeeMoreClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("See More")
                        }
                    }
                }
            }
            item(key = "collections-title", contentType = "section") {
                SectionHeader("📚 Study Collections")
            }
            item(key = "collections-carousel", contentType = "carousel") {
                ScrollableRowWithIndicator {
                    itemsIndexed(
                        items = content.studyCollections,
                        key = { index, collection -> collection.id.ifBlank { "collection_$index" } },
                        contentType = { _, _ -> "collection" }
                    ) { _, collection ->
                        CollectionCard(collection)
                    }
                }
            }
            item(key = "revision-title", contentType = "section") {
                SectionHeader("💡 Quick Revision")
            }
            item(key = "revision-carousel", contentType = "carousel") {
                ScrollableRowWithIndicator {
                    itemsIndexed(
                        items = content.revisionCards,
                        key = { index, revision -> revision.id.ifBlank { "revision_$index" } },
                        contentType = { _, _ -> "revision" }
                    ) { _, revision ->
                        RevisionCard(revision)
                    }
                }
            }
            item(key = "discover-title", contentType = "section") {
                SectionHeader("🌍 Discover")
            }
            itemsIndexed(
                items = visibleDiscoverItems,
                key = { index, item -> item.id.ifBlank { "discover_$index" } },
                contentType = { _, item ->
                    when (item) {
                        is DiscoverFeedItem.Collection -> "discover-collection"
                        is DiscoverFeedItem.ContributorPost -> "discover-contributor"
                        is DiscoverFeedItem.Note -> "discover-note"
                        is DiscoverFeedItem.Video -> "discover-video"
                    }
                }
            ) { _, item ->
                DiscoverFeedItem(
                    item = item,
                    onClick = {
                        if (item is DiscoverFeedItem.Video) {
                            onVideoClick(item.id)
                        } else {
                            onDocumentClick(item.id)
                        }
                    }
                )
            }
            if (content.discoverItems.size > visibleDiscoverItems.size) {
                item(key = "discover-see-more", contentType = "action") {
                    TextButton(
                        onClick = onDiscoverSeeMoreClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("See More")
                    }
                }
            }
        }
        AdaptiveScrollbar(listState = listState)
    }
}

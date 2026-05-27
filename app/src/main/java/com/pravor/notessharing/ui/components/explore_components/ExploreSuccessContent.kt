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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onDiscoverSeeMoreClick: () -> Unit
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
                    items(visibleTrendingNotes, key = { it.id }, contentType = { "trending-note" }) { note ->
                        TrendingNoteCard(note)
                    }
                }
            }
            item(key = "videos-title", contentType = "section") {
                SectionHeader("📺 Recommended Videos")
            }
            items(
                items = visibleRecommendedVideos,
                key = { it.id },
                contentType = { "video" }
            ) { video ->
                VideoRecommendationCard(video)
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
                items = visibleDiscoverItems,
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

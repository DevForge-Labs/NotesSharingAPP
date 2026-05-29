package com.pravor.notessharing.ui.components.home_components

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pravor.notessharing.state.HomeContent
import com.pravor.notessharing.state.MyFilesUiState
import com.pravor.notessharing.ui.components.AdaptiveScrollbar
import com.pravor.notessharing.ui.components.CompactStudyFileRow
import com.pravor.notessharing.ui.components.NotesSearchBar
import com.pravor.notessharing.ui.components.SectionHeader

@Composable
fun HomeSuccessContent(
    content: HomeContent,
    myFilesUiState: MyFilesUiState,
    onUpvoteClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    onViewAllLibraryClick: () -> Unit,
    onSeeMoreClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    val libraryFiles = when (myFilesUiState) {
        is MyFilesUiState.Success -> (myFilesUiState.content.savedFiles + myFilesUiState.content.uploadedFiles).take(5)
        else -> emptyList()
    }
    val visibleFeedItems = content.feedItems.take(4)

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "home-title", contentType = "header") {
                Text(
                    text = "Study Social",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }
            item(key = "home-search", contentType = "search") {
                NotesSearchBar("Search notes, PYQs, PDFs...")
            }
            if (content.recentlyOpened != null) {
                item(key = "continue-title", contentType = "section") {
                    Spacer(Modifier.height(4.dp))
                    SectionHeader("Continue Reading")
                }
                item(key = "continue-card", contentType = "continue-reading") {
                    ContinueReadingCard(
                        item = content.recentlyOpened,
                        onClick = { onDocumentClick(content.recentlyOpened.id) }
                    )
                }
            }
            item(key = "for-you-title", contentType = "section") {
                Spacer(Modifier.height(8.dp))
                SectionHeader("For You")
            }
            if (visibleFeedItems.isEmpty()) {
                item(key = "for-you-empty", contentType = "empty") {
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
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "No content available for your semester yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(visibleFeedItems, key = { it.id }, contentType = { "feed-card" }) { feedItem ->
                    HomeFeedCard(
                        item = feedItem,
                        onClick = { onDocumentClick(feedItem.id) },
                        onUpvoteClick = { onUpvoteClick(feedItem.id) },
                        onBookmarkClick = { onBookmarkClick(feedItem.id) }
                    )
                }
                if (content.feedItems.size > visibleFeedItems.size) {
                    item(key = "for-you-see-more", contentType = "action") {
                        TextButton(
                            onClick = onSeeMoreClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("See More")
                        }
                    }
                }
            }
            item(key = "library-title", contentType = "section") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader("Your Library", modifier = Modifier.weight(1f))
                    TextButton(onClick = onViewAllLibraryClick) {
                        Text("View All")
                    }
                }
            }
            if (libraryFiles.isEmpty()) {
                item(key = "library-empty", contentType = "empty") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Text(
                            text = "Start exploring notes",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(libraryFiles, key = { "library-${it.id}" }, contentType = { "library-file" }) { file ->
                    CompactStudyFileRow(file = file, onClick = { onDocumentClick(file.id) })
                }
            }
        }
        AdaptiveScrollbar(listState = listState)
    }
}

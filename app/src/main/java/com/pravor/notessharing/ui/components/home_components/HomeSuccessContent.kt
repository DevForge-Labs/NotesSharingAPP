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
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
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
import com.pravor.notessharing.ui.navigation.LocalBottomBarPadding
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.runtime.getValue

@Composable
fun HomeSuccessContent(
    content: HomeContent,
    myFilesUiState: MyFilesUiState,
    bookmarksCount: Int,
    onUpvoteClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    onMyUploadsClick: () -> Unit,
    onMyBookmarksClick: () -> Unit,
    onMyDownloadsClick: () -> Unit,
    onViewAllLibraryClick: () -> Unit,
    onSeeMoreClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    val bottomPadding = LocalBottomBarPadding.current
    val infiniteTransition = rememberInfiniteTransition(label = "feed-shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "feed-shimmer-alpha"
    )
    val libraryFiles = when (myFilesUiState) {
        is MyFilesUiState.Success -> (myFilesUiState.content.savedFiles + myFilesUiState.content.uploadedFiles).take(5)
        else -> emptyList()
    }
    val visibleFeedItems = content.feedItems.take(6)

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            state = listState,
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 14.dp + bottomPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "home-title", contentType = "header") {
                Text(
                    text = "Study Social",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.25.sp,
                        fontSize = 24.sp
                    ),
                    color = Color(0xFFF5F7FA)
                )
            }
            item(key = "home-search", contentType = "search") {
                NotesSearchBar("Search notes, PYQs, PDFs...")
            }
            if (content.recentlyOpened != null) {
                item(key = "continue-title", contentType = "section") {
                    Spacer(Modifier.height(6.dp))
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
                Spacer(Modifier.height(6.dp))
                SectionHeader("For You")
            }
            if (content.isLoadingFeed) {
                // Show a premium dark skeleton grid of 6 placeholders matching 2-column paired layout
                (0 until 3).forEach { rowIndex ->
                    item(key = "for-you-skeleton-row-$rowIndex", contentType = "for-you-skeleton-row") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ForYouGridCardSkeleton(alpha = alpha, modifier = Modifier.weight(1f))
                            ForYouGridCardSkeleton(alpha = alpha, modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else if (visibleFeedItems.isEmpty()) {
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
                val gridRows = visibleFeedItems.chunked(2)
                gridRows.forEachIndexed { rowIndex, rowItems ->
                    item(key = "for-you-row-$rowIndex", contentType = "for-you-grid-row") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { feedItem ->
                                ForYouGridCard(
                                    item = feedItem,
                                    onClick = { onDocumentClick(feedItem.id) },
                                    onBookmarkClick = { onBookmarkClick(feedItem.id) },
                                    onUpvoteClick = { onUpvoteClick(feedItem.id) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowItems.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
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
            item(key = "study-hub-title", contentType = "section") {
                Spacer(Modifier.height(12.dp))
                SectionHeader("Study Hub")
            }

            item(key = "study-hub-uploads", contentType = "study-hub-card") {
                val uploadsCount = when (myFilesUiState) {
                    is MyFilesUiState.Success -> myFilesUiState.content.uploadedFiles.size
                    else -> 0
                }
                val uploadsText = "$uploadsCount study materials"
                StudyHubCard(
                    title = "Uploads",
                    metadata = uploadsText,
                    contextHint = "Your contributions to the community",
                    icon = Icons.Default.UploadFile,
                    accentColor = Color(0xFF58D6D1), // Premium soft teal/blue
                    cardBrush = Brush.verticalGradient(listOf(Color(0xFF13201F), Color(0xFF0C1312))),
                    onClick = onMyUploadsClick
                )
            }

            item(key = "study-hub-bookmarks", contentType = "study-hub-card") {
                val bookmarksText = "$bookmarksCount saved resources"
                StudyHubCard(
                    title = "Bookmarks",
                    metadata = bookmarksText,
                    contextHint = "Quick access to saved study material",
                    icon = Icons.Default.Bookmark,
                    accentColor = Color(0xFFFFB45C), // Premium warm tint
                    cardBrush = Brush.verticalGradient(listOf(Color(0xFF241C15), Color(0xFF16110D))),
                    onClick = onMyBookmarksClick
                )
            }

            item(key = "study-hub-downloads", contentType = "study-hub-card") {
                StudyHubCard(
                    title = "Downloads",
                    metadata = "0 downloaded files",
                    contextHint = "Your downloaded study collection",
                    icon = Icons.Default.Download,
                    accentColor = Color(0xFFCFD8DC), // Muted slate/academic tint
                    cardBrush = Brush.verticalGradient(listOf(Color(0xFF1D2124), Color(0xFF111315))),
                    onClick = onMyDownloadsClick
                )
            }
        }
        AdaptiveScrollbar(listState = listState)
    }
}

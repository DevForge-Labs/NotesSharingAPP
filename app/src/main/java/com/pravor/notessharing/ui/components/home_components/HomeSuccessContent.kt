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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pravor.notessharing.state.HomeContent
import com.pravor.notessharing.state.MyFilesUiState
import com.pravor.notessharing.ui.components.explore_components.ClimbingMascotScrollbar
import com.pravor.notessharing.ui.components.explore_components.MonkeyMascot
import com.pravor.notessharing.ui.components.CompactStudyFileRow
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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle

@Composable
fun HomeSuccessContent(
    content: HomeContent,
    myFilesUiState: MyFilesUiState,
    uploadsCount: Int,
    bookmarksCount: Int,
    activeDownloadsCount: Int,
    unreadNotificationsCount: Int = 0,
    onBellClick: () -> Unit = {},
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
            item(key = "home-greeting", contentType = "greeting") {
                SmartBannerSlot(
                    unreadCount = unreadNotificationsCount,
                    onBellClick = onBellClick
                )
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
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(
                            targetValue = if (isPressed) 0.97f else 1.0f,
                            animationSpec = tween(durationMillis = 100),
                            label = "see-more-scale"
                        )
                        val buttonElevation by animateFloatAsState(
                            targetValue = if (isPressed) 1f else 4f,
                            animationSpec = tween(durationMillis = 100),
                            label = "see-more-elevation"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .graphicsLayer(scaleX = scale, scaleY = scale)
                                .shadow(
                                    elevation = buttonElevation.dp,
                                    shape = RoundedCornerShape(24.dp),
                                    clip = false,
                                    ambientColor = Color(0xFF14B8A6).copy(alpha = 0.15f),
                                    spotColor = Color(0xFF14B8A6).copy(alpha = 0.3f)
                                )
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF0F766E), // Deep teal
                                            Color(0xFF115E59)  // Dark teal
                                        )
                                    )
                                )
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = androidx.compose.foundation.LocalIndication.current,
                                    onClick = onSeeMoreClick
                                )
                                .border(
                                    BorderStroke(1.dp, Color(0xFF2DD4BF).copy(alpha = 0.25f)),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "See More",
                                style = TextStyle(
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }
                }
            }
            item(key = "study-hub-title", contentType = "section") {
                Spacer(Modifier.height(12.dp))
                SectionHeader("Study Hub")
            }

            item(key = "study-hub-uploads", contentType = "study-hub-card") {
                val uploadsText = "$uploadsCount study materials"
                StudyHubCard(
                    title = "Uploads",
                    metadata = uploadsText,
                    contextHint = "Your contributions to the community",
                    icon = Icons.Default.UploadFile,
                    lottieAsset = "App_animations/uploading_screen_logo.json",
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
                    lottieAsset = "App_animations/bookmark_screen_icon.json",
                    accentColor = Color(0xFFFFB45C), // Premium warm tint
                    cardBrush = Brush.verticalGradient(listOf(Color(0xFF241C15), Color(0xFF16110D))),
                    onClick = onMyBookmarksClick
                )
            }

            item(key = "study-hub-downloads", contentType = "study-hub-card") {
                val downloadedCount = when (myFilesUiState) {
                    is MyFilesUiState.Success -> myFilesUiState.content.savedFiles.size
                    else -> 0
                }
                val downloadsText = if (downloadedCount == 1) "1 Document" else "$downloadedCount Documents"
                
                val activeDownloadsText = if (activeDownloadsCount > 0) {
                    val dotsTransition = rememberInfiniteTransition(label = "downloadsDotTransition")
                    val dotCount by dotsTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1500, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "activeDotCount"
                    )
                    val dots = when (dotCount.toInt()) {
                        0 -> "."
                        1 -> ".."
                        else -> "..."
                    }
                    val label = if (activeDownloadsCount == 1) "download" else "downloads"
                    "$activeDownloadsCount $label in progress$dots"
                } else {
                    null
                }

                StudyHubCard(
                    title = "Downloads",
                    metadata = downloadsText,
                    contextHint = "Your downloaded study collection",
                    icon = Icons.Default.Download,
                    lottieAsset = "App_animations/download_screen_logo.json",
                    accentColor = Color(0xFFCFD8DC), // Muted slate/academic tint
                    cardBrush = Brush.verticalGradient(listOf(Color(0xFF1D2124), Color(0xFF111315))),
                    onClick = onMyDownloadsClick,
                    secondaryMetadata = activeDownloadsText
                )
            }
        }
        ClimbingMascotScrollbar(listState = listState) { modifier, isScrolling ->
            MonkeyMascot(modifier = modifier, isScrolling = isScrolling)
        }
    }
}

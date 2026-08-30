package com.pravor.notessharing.ui.features.home.components

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.components.CompactStudyFileRow
import com.pravor.notessharing.ui.common.components.SectionHeader

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
import androidx.compose.material.icons.filled.Add
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
import com.pravor.notessharing.ui.common.HomeContent
import com.pravor.notessharing.ui.common.MyFilesUiState
import com.pravor.notessharing.ui.features.explore.components.ClimbingMascotScrollbar
import com.pravor.notessharing.ui.features.explore.components.MonkeyMascot
import com.pravor.notessharing.ui.common.components.CompactStudyFileRow
import com.pravor.notessharing.ui.common.components.SectionHeader
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

import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import com.pravor.notessharing.ui.theme.ElectricBlue

@Composable
fun HomeSuccessContent(
    content: HomeContent,
    myFilesUiState: MyFilesUiState = MyFilesUiState.Loading,
    uploadsCount: Int,
    bookmarksCount: Int,
    activeDownloadsCount: Int,
    unreadNotificationsCount: Int = 0,
    isGreetingVisible: Boolean = true,
    onBellClick: () -> Unit = {},
    onUpvoteClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    onMyUploadsClick: () -> Unit,
    onUploadClick: () -> Unit = {},
    onMyBookmarksClick: () -> Unit,
    onMyDownloadsClick: () -> Unit,
    onViewAllLibraryClick: () -> Unit,
    onSeeMoreClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    val bottomPadding = LocalBottomBarPadding.current
    val visibleFeedItems = content.feedItems.take(6)

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            state = listState,
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 16.dp + bottomPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "home-greeting", contentType = "greeting") {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isGreetingVisible,
                    enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) + 
                            androidx.compose.animation.expandVertically(animationSpec = androidx.compose.animation.core.tween(400)),
                    exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(350, easing = androidx.compose.animation.core.FastOutSlowInEasing)) + 
                           androidx.compose.animation.shrinkVertically(animationSpec = androidx.compose.animation.core.tween(450, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                ) {
                    SmartBannerSlot(
                        unreadCount = unreadNotificationsCount,
                        onBellClick = onBellClick
                    )
                }
            }
            if (content.recentlyOpened != null) {
                item(key = "continue-title", contentType = "section") {
                    SectionHeader("Continue Reading")
                }
                item(key = "continue-card", contentType = "continue-reading") {
                    val roId = content.recentlyOpened.id
                    val onContinueClick = remember(roId) { { onDocumentClick(roId) } }
                    ContinueReadingCard(
                        item = content.recentlyOpened,
                        onClick = onContinueClick
                    )
                }
            }
            item(key = "for-you-title", contentType = "section") {
                SectionHeader("For You", onSeeMoreClick = if (content.feedItems.size > visibleFeedItems.size) onSeeMoreClick else null)
            }
            if (content.isLoadingFeed && visibleFeedItems.isEmpty()) {
                (0 until 3).forEach { rowIndex ->
                    item(key = "for-you-skeleton-row-$rowIndex", contentType = "for-you-skeleton-row") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ForYouGridCardSkeleton(modifier = Modifier.weight(1f))
                            ForYouGridCardSkeleton(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else if (visibleFeedItems.isEmpty()) {
                item(key = "for-you-empty", contentType = "empty") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
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
                                modifier = Modifier.size(40.dp)
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
                gridRows.forEach { rowItems ->
                    val rowKey = "for-you-row-${rowItems.joinToString("-") { it.id }}"
                    item(key = rowKey, contentType = "for-you-grid-row") {
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
                            targetValue = if (isPressed) 0.98f else 1.0f,
                            animationSpec = tween(durationMillis = 100),
                            label = "see-more-scale"
                        )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer(scaleX = scale, scaleY = scale)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = androidx.compose.foundation.LocalIndication.current,
                                    onClick = onSeeMoreClick
                                ),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.38f)),
                            color = Color.Transparent,
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0xFF13202C).copy(alpha = 0.74f),
                                                Color(0xFF0B131A).copy(alpha = 0.78f)
                                            )
                                        )
                                    )
                                    .padding(vertical = 14.dp, horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Explore All Study Resources",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = ElectricBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        letterSpacing = 0.2.sp
                                    )
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
            item(key = "study-hub-title", contentType = "section") {
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
                    accentColor = Color(0xFF58D6D1),
                    cardBrush = Brush.verticalGradient(listOf(Color(0xFF112222).copy(alpha = 0.72f), Color(0xFF0C1414).copy(alpha = 0.75f), Color(0xFF090A0E).copy(alpha = 0.78f))),
                    onClick = onMyUploadsClick,
                    actionContent = {
                        Surface(
                            onClick = onUploadClick,
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF58D6D1),
                            contentColor = Color(0xFF0C1312),
                            modifier = Modifier.size(36.dp),
                            shadowElevation = 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New Upload",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
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
                    accentColor = Color(0xFFFFB45C),
                    cardBrush = Brush.verticalGradient(listOf(Color(0xFF241C15).copy(alpha = 0.72f), Color(0xFF14100D).copy(alpha = 0.75f), Color(0xFF090A0E).copy(alpha = 0.78f))),
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
                    accentColor = Color(0xFF7AD7FF),
                    cardBrush = Brush.verticalGradient(listOf(Color(0xFF131F2A).copy(alpha = 0.72f), Color(0xFF0D141C).copy(alpha = 0.75f), Color(0xFF090A0E).copy(alpha = 0.78f))),
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

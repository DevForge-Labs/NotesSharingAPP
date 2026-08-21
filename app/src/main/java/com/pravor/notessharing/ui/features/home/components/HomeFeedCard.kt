package com.pravor.notessharing.ui.features.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pravor.notessharing.domain.model.FeedItem
import com.pravor.notessharing.domain.model.FileType
import com.pravor.notessharing.domain.model.extractYoutubePlaylistId
import com.pravor.notessharing.ui.common.VideoPlaceholder
import com.pravor.notessharing.ui.common.components.Avatar
import com.pravor.notessharing.ui.common.components.StatItem

@Composable
fun HomeFeedCard(
    item: FeedItem,
    onClick: () -> Unit,
    onUpvoteClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        if (item.fileType == FileType.Video) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(116.dp)
                        .height(76.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    val isYouTubeResource = item.type == "YouTube Resource" || item.documentType == "YouTube Resource"
                    val finalImageUrl = if (isYouTubeResource) {
                        if (!item.thumbnailUrl.isNullOrBlank()) {
                            item.thumbnailUrl
                        } else if (!item.youtubeThumbnailUrl.isNullOrBlank()) {
                            item.youtubeThumbnailUrl
                        } else {
                            null
                        }
                    } else {
                        if (!item.thumbnailUrl.isNullOrBlank()) {
                            item.thumbnailUrl
                        } else {
                            null
                        }
                    }

                    var hasThumbnailError by remember(finalImageUrl) { mutableStateOf(finalImageUrl.isNullOrBlank()) }
                    if (!hasThumbnailError && !finalImageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = finalImageUrl,
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            onError = { hasThumbnailError = true }
                        )
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.4f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        VideoPlaceholder(modifier = Modifier.fillMaxSize())
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        val isYouTubePlaylist = item.youtubeVideoId.isNullOrBlank() ||
                            (!item.youtubeUrl.isNullOrBlank() && extractYoutubePlaylistId(item.youtubeUrl) != null)
                        val fileTypeLabel = if (isYouTubePlaylist) "YouTube Playlist" else "YouTube Video"
                        FileTypeBadge(fileTypeLabel)
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "By ${item.uploaderName} • ${item.uploadDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatItem(Icons.Default.ThumbUp, item.upvotes.toString())
                            StatItem(Icons.Default.Bookmark, item.bookmarksCount.toString())
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ActionIconButton(
                                selected = item.isUpvoted,
                                selectedIcon = Icons.Filled.ThumbUp,
                                unselectedIcon = Icons.Outlined.ThumbUp,
                                contentDescription = "Upvote",
                                onClick = onUpvoteClick,
                                size = 32.dp,
                                iconSize = 16.dp
                            )
                            ActionIconButton(
                                selected = item.isSaved,
                                selectedIcon = Icons.Filled.Bookmark,
                                unselectedIcon = Icons.Filled.BookmarkBorder,
                                contentDescription = "Bookmark",
                                onClick = onBookmarkClick,
                                size = 32.dp,
                                iconSize = 16.dp
                            )
                        }
                    }
                }
            }
        } else {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(text = item.uploaderInitials, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = item.uploaderName,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.uploadDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    FileTypeBadge(item.fileType.label)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        StatItem(Icons.Default.ThumbUp, item.upvotes.toString())
                        StatItem(Icons.Default.Download, item.downloadsCount.toString())
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ActionIconButton(
                            selected = item.isUpvoted,
                            selectedIcon = Icons.Filled.ThumbUp,
                            unselectedIcon = Icons.Outlined.ThumbUp,
                            contentDescription = "Upvote",
                            onClick = onUpvoteClick
                        )
                        ActionIconButton(
                            selected = item.isSaved,
                            selectedIcon = Icons.Filled.Bookmark,
                            unselectedIcon = Icons.Filled.BookmarkBorder,
                            contentDescription = "Bookmark",
                            onClick = onBookmarkClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionIconButton(
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp
) {
    val scale by animateFloatAsState(if (selected) 1.06f else 1f, label = "home-feed-action-scale")
    val tint by animateColorAsState(
        targetValue = if (selected) {
            if (selectedIcon == Icons.Filled.ThumbUp || selectedIcon == Icons.Default.ThumbUp) {
                Color(0xFFFFB74D)
            } else {
                MaterialTheme.colorScheme.primary
            }
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "home-feed-action-tint"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(size)
                .scale(scale)
        ) {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize),
                tint = tint
            )
        }
    }
}

@Composable
private fun FileTypeBadge(label: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontWeight = FontWeight.Bold
        )
    }
}

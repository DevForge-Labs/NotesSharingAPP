package com.pravor.notessharing.ui.components.explore_components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pravor.notessharing.model.VideoRecommendation
import com.pravor.notessharing.ui.components.StatItem
import com.pravor.notessharing.ui.components.VideoPlaceholder

private const val THUMBNAIL_QUALITY_HQ = "hqdefault"

@Composable
fun VideoRecommendationCard(
    video: VideoRecommendation,
    onClick: () -> Unit = {}
) {
    PressScaleSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: YouTube Thumbnail with error fallback and optional duration overlay
            Box(
                modifier = Modifier
                    .width(116.dp)
                    .height(76.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                var hasThumbnailError by remember { mutableStateOf(video.youtubeVideoId.isBlank()) }
                
                if (!hasThumbnailError) {
                    AsyncImage(
                        model = "https://img.youtube.com/vi/${video.youtubeVideoId}/$THUMBNAIL_QUALITY_HQ.jpg",
                        contentDescription = video.subject,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onError = { hasThumbnailError = true }
                    )
                    
                    // Centered small glassmorphic play button overlay
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
                    
                    if (video.duration.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Black.copy(alpha = 0.75f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                        ) {
                            Text(
                                text = video.duration,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                } else {
                    VideoPlaceholder(modifier = Modifier.fillMaxSize())
                }
            }
            
            Spacer(Modifier.width(14.dp))
            
            // Right: Video details
            Column(Modifier.weight(1f)) {
                Text(
                    text = video.subject.ifBlank { video.title },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = video.channelName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(Modifier.height(8.dp))
                
                // Real stats: Upvotes and bookmarks count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatItem(
                        icon = Icons.Default.ThumbUp,
                        value = video.upvotes.toString()
                    )
                    StatItem(
                        icon = Icons.Default.Bookmark,
                        value = video.bookmarks.toString()
                    )
                }
            }
        }
    }
}

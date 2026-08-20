package com.pravor.notessharing.ui.features.explore.components

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
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
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.pravor.notessharing.domain.model.VideoRecommendation
import com.pravor.notessharing.ui.common.StatItem
import com.pravor.notessharing.ui.common.VideoPlaceholder
import com.pravor.notessharing.ui.common.utils.SubjectBadge

private val CardShape = RoundedCornerShape(24.dp)
private val BadgeShape = RoundedCornerShape(12.dp)
private val DurationShape = RoundedCornerShape(4.dp)
private val OverlayPlayColor = Color.Black.copy(alpha = 0.4f)
private val OverlayDurationColor = Color.Black.copy(alpha = 0.75f)

@Composable
fun VideoRecommendationCard(
    video: VideoRecommendation,
    isUpvoted: Boolean = false,
    onUpvoteClick: () -> Unit = {},
    onBookmarkClick: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    val isYouTubePlaylist = remember(video.youtubeVideoId, video.youtubeUrl) {
        video.youtubeVideoId.isBlank() || 
        (video.youtubeUrl.isNotBlank() && com.pravor.notessharing.domain.model.extractYoutubePlaylistId(video.youtubeUrl) != null)
    }
    val fileTypeLabel = remember(isYouTubePlaylist) { if (isYouTubePlaylist) "Playlist" else "Video" }

    val theme = remember(fileTypeLabel) { com.pravor.notessharing.ui.common.getStudyResourceTheme(fileTypeLabel) }
    val accentColor = theme.accentColor
    val cardBrush = theme.cardBrush

    val badgeBgColor = remember(accentColor) { accentColor.copy(alpha = 0.08f) }
    val badgeBorderColor = remember(accentColor) { accentColor.copy(alpha = 0.3f) }
    val cardBorderColor = remember(accentColor) { accentColor.copy(alpha = 0.12f) }

    val cardBorder = remember(cardBorderColor) { BorderStroke(1.dp, cardBorderColor) }
    val badgeBorder = remember(badgeBorderColor) { BorderStroke(0.5.dp, badgeBorderColor) }

    PressScaleSurface(
        modifier = Modifier
            .fillMaxWidth()
            .border(cardBorder, CardShape),
        shape = CardShape,
        brush = cardBrush,
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
                val finalImageUrl = remember(video.thumbnailUrl, video.youtubeThumbnailUrl) {
                    if (!video.thumbnailUrl.isNullOrBlank()) {
                        video.thumbnailUrl
                    } else if (!video.youtubeThumbnailUrl.isNullOrBlank()) {
                        video.youtubeThumbnailUrl
                    } else {
                        null
                    }
                }

                val context = LocalContext.current
                val imageRequest = remember(finalImageUrl, context) {
                    if (finalImageUrl != null) {
                        ImageRequest.Builder(context)
                            .data(finalImageUrl)
                            .crossfade(true)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .size(300, 200)
                            .build()
                    } else {
                        null
                    }
                }

                var hasThumbnailError by remember(finalImageUrl) { mutableStateOf(finalImageUrl.isNullOrBlank()) }
                
                if (!hasThumbnailError && imageRequest != null) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = video.subject,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onError = { hasThumbnailError = true }
                    )
                    
                    // Centered small glassmorphic play button overlay
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(OverlayPlayColor)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    if (video.duration.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .clip(DurationShape)
                                .background(OverlayDurationColor)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = video.duration,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    VideoPlaceholder(modifier = Modifier.fillMaxSize())
                }
            }
            
            Spacer(Modifier.width(14.dp))
            
            // Middle: Video details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Row 1: Resource Type & Semester
                val semesterText = remember(video.semester) { video.semester.replace("Semester ", "Sem ") }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .clip(BadgeShape)
                            .background(badgeBgColor)
                            .border(badgeBorder, BadgeShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = fileTypeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(BadgeShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = semesterText,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Row 2: Subject Badge & Engagement stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SubjectBadge(subject = video.subject)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.clickable { onUpvoteClick() },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ThumbUp,
                                contentDescription = "Upvotes",
                                modifier = Modifier.size(17.dp),
                                tint = if (isUpvoted) Color(0xFFFFB74D) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = video.upvotes.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isUpvoted) Color(0xFFFFB74D) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(
                            modifier = Modifier.clickable { onBookmarkClick() },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (video.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                modifier = Modifier.size(17.dp),
                                tint = if (video.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

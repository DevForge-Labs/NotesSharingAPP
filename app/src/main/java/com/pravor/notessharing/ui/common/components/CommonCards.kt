package com.pravor.notessharing.ui.common.components

import com.pravor.notessharing.domain.model.FileType
import com.pravor.notessharing.domain.model.StudyFile
import com.pravor.notessharing.ui.common.DocumentPlaceholder

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ThumbUp
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pravor.notessharing.domain.model.FeedItem
import com.pravor.notessharing.domain.model.ResourceAvailability
import com.pravor.notessharing.ui.common.theme.getStudyResourceTheme
import java.io.File

@Composable
fun FeedCard(
    item: FeedItem,
    onUpvoteClick: () -> Unit,
    onClick: () -> Unit,
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
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(text = item.uploaderInitials, modifier = Modifier.size(34.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.uploaderName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                FileTypeBadge(item.fileType)
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
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                StatItem(
                    icon = Icons.Default.ThumbUp,
                    value = item.upvotes.toString(),
                    modifier = Modifier.clickable(onClick = onUpvoteClick)
                )
                StatItem(Icons.Default.Download, item.downloadsCount.toString())
            }
        }
    }
}

@Composable
fun StudyFileCard(
    file: StudyFile,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FilePresent,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = file.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${file.uploadDate} | ${file.fileType.label} | ${file.downloadsCount} downloads | ${file.upvotes} upvotes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CompactStudyFileRow(
    file: StudyFile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FilePresent,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = file.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${file.uploadDate} | ${file.downloadsCount} downloads",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun StudyHubShelfCard(
    file: StudyFile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onBookmarkClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null
) {
    val theme = getStudyResourceTheme(file.documentType ?: file.fileType.label)
    val docTypeStr = theme.docTypeStr
    val accentColor = theme.accentColor
    val cardBrush = theme.cardBrush
    val isVideoOnly = docTypeStr == "Video"
    val isPlaylist = docTypeStr == "Playlist"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.12f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(108.dp)
                    .height(76.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                var hasImageLoaded by remember { mutableStateOf(false) }
                var imageLoadError by remember { mutableStateOf(false) }

                val imageModel = remember(file.thumbnailUrl, file.localThumbnailPath, file.availability) {
                    if (file.availability == ResourceAvailability.ARCHIVED_DOWNLOAD) {
                        file.localThumbnailPath?.let { File(it) }
                    } else {
                        if (!file.localThumbnailPath.isNullOrBlank()) {
                            File(file.localThumbnailPath)
                        } else {
                            file.thumbnailUrl
                        }
                    }
                }

                if (imageModel != null) {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = file.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onSuccess = { hasImageLoaded = true },
                        onError = { imageLoadError = true }
                    )
                }

                val showFallback =
                    (file.thumbnailUrl.isNullOrBlank() || imageLoadError) && !hasImageLoaded
                if (showFallback) {
                    DocumentPlaceholder(
                        documentType = docTypeStr,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (isVideoOnly || isPlaylist) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.4f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isVideoOnly) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.List,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (file.availability == ResourceAvailability.ARCHIVED_DOWNLOAD) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF94A3B8).copy(alpha = 0.12f),
                            border = BorderStroke(0.5.dp, Color(0xFF94A3B8).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "ARCHIVED",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    val subjectText = file.subject?.trim()?.ifBlank { null } ?: "General"
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accentColor.copy(alpha = 0.06f),
                        border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = subjectText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = accentColor.copy(alpha = 0.85f),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accentColor.copy(alpha = 0.12f),
                        border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = docTypeStr.uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = if (file.availability == ResourceAvailability.ARCHIVED_DOWNLOAD) "Removed from platform" else file.uploadDate,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = if (file.availability == ResourceAvailability.ARCHIVED_DOWNLOAD) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = Color(0xFF64B5F6),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = file.downloadsCount.toString(),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color(0xFF64B5F6).copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ThumbUp,
                                contentDescription = null,
                                tint = Color(0xFFFFB74D),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = file.upvotes.toString(),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color(0xFFFFB74D).copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (onDeleteClick != null) {
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Download",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else if (onBookmarkClick != null) {
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = onBookmarkClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "Bookmark",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

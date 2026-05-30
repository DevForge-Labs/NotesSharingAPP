package com.pravor.notessharing.ui.components.home_components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import coil.compose.AsyncImage
import com.pravor.notessharing.model.FeedItem
import com.pravor.notessharing.model.FileType
import com.pravor.notessharing.ui.components.Avatar
import com.pravor.notessharing.ui.components.StatItem

private const val THUMBNAIL_QUALITY_HQ = "hqdefault"

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
                // Left: Compact YouTube Thumbnail with play icon overlay
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

                    android.util.Log.d(
                        "YouTubeHomeThumbnail",
                        "FeedCard: Title: ${item.title}, Type: ${item.documentType ?: item.type}, thumbnailUrl: ${item.thumbnailUrl}, youtubeThumbnailUrl: ${item.youtubeThumbnailUrl}, Final URL: $finalImageUrl"
                    )

                    var hasThumbnailError by remember(finalImageUrl) { mutableStateOf(finalImageUrl.isNullOrBlank()) }
                    if (!hasThumbnailError && !finalImageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = finalImageUrl,
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            onError = { hasThumbnailError = true }
                        )
                        // Centered glassmorphic play button overlay
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
                        // Fallback UI
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFF1E1E24), Color(0xFF121214))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Right: Details in compact vertical stack
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
                        Spacer(Modifier.width(8.dp))
                        FileTypeBadge(item.fileType)
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        StatItem(Icons.Default.ThumbUp, item.upvotes.toString())
                        StatItem(Icons.Default.Download, item.downloads.toString())
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
    size: androidx.compose.ui.unit.Dp = 40.dp,
    iconSize: androidx.compose.ui.unit.Dp = 20.dp
) {
    val scale by animateFloatAsState(if (selected) 1.06f else 1f, label = "home-feed-action-scale")
    val tint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun FileTypeBadge(fileType: FileType) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Text(
            text = fileType.label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ForYouGridCard(
    item: FeedItem,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVideo = item.fileType == FileType.Video
    val rawDocType = (item.documentType ?: item.type)
        ?.lowercase(java.util.Locale.ROOT)?.trim()

    val isPyq = when (rawDocType) {
        "pyq" -> true
        "cheatsheet", "cheat sheet", "assignment", "notes" -> false
        else -> item.fileType == FileType.Pyq ||
                item.tags.any { it.equals("pyq", ignoreCase = true) } ||
                item.title.contains("pyq", ignoreCase = true) ||
                item.description.contains("pyq", ignoreCase = true)
    }

    val isCheatSheet = when (rawDocType) {
        "cheatsheet", "cheat sheet" -> true
        "pyq", "assignment", "notes" -> false
        else -> item.fileType == FileType.CheatSheet ||
                item.tags.any { it.equals("cheat sheet", ignoreCase = true) || it.equals("cheatsheet", ignoreCase = true) || it.equals("formula", ignoreCase = true) } ||
                item.title.contains("cheat", ignoreCase = true) ||
                item.title.contains("formula", ignoreCase = true) ||
                item.description.contains("cheat", ignoreCase = true) ||
                item.description.contains("formula", ignoreCase = true)
    }

    val isAssignment = when (rawDocType) {
        "assignment" -> true
        "pyq", "cheatsheet", "cheat sheet", "notes" -> false
        else -> item.fileType == FileType.LabManual ||
                item.tags.any { it.equals("assignment", ignoreCase = true) } ||
                item.title.contains("assignment", ignoreCase = true) ||
                item.description.contains("assignment", ignoreCase = true)
    }

    val isNotes = when (rawDocType) {
        "notes" -> true
        "pyq", "cheatsheet", "cheat sheet", "assignment" -> false
        else -> item.fileType == FileType.Notes ||
                item.tags.any { it.equals("notes", ignoreCase = true) || it.equals("lecture", ignoreCase = true) } ||
                item.title.contains("notes", ignoreCase = true) ||
                item.title.contains("lecture", ignoreCase = true) ||
                item.description.contains("notes", ignoreCase = true) ||
                item.description.contains("lecture", ignoreCase = true)
    }

    val docTypeStr = when {
        isVideo -> "YouTube"
        isPyq -> "PYQ"
        isAssignment -> "Assignment"
        isCheatSheet -> "Cheat Sheet"
        isNotes -> "Notes"
        else -> "PDF"
    }

    val accentColor = when {
        isVideo -> Color(0xFFFF6B6B)
        isNotes -> Color(0xFF58D6D1)
        isPyq -> Color(0xFFFFB45C)
        isAssignment -> Color(0xFF7AD7FF)
        isCheatSheet -> Color(0xFFC7A6FF)
        else -> Color(0xFFCFD8DC)
    }

    val cardBrush = when {
        isVideo -> Brush.verticalGradient(listOf(Color(0xFF231A1B), Color(0xFF130E0F)))
        isNotes -> Brush.verticalGradient(listOf(Color(0xFF13201F), Color(0xFF0C1312)))
        isPyq -> Brush.verticalGradient(listOf(Color(0xFF241C15), Color(0xFF16110D)))
        isCheatSheet -> Brush.verticalGradient(listOf(Color(0xFF1E1724), Color(0xFF120E16)))
        isAssignment -> Brush.verticalGradient(listOf(Color(0xFF141F23), Color(0xFF0C1316)))
        else -> Brush.verticalGradient(listOf(Color(0xFF1D2124), Color(0xFF111315)))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.12f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush)
                .padding(10.dp)
        ) {
            // 1. TOP: Thumbnail Area (Hero visual)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.4f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.18f)), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                var hasImageLoaded by remember { mutableStateOf(false) }
                var imageLoadError by remember { mutableStateOf(false) }

                if (isVideo) {
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

                    android.util.Log.d(
                        "YouTubeHomeThumbnail",
                        "GridCard: Title: ${item.title}, Type: ${item.documentType ?: item.type}, thumbnailUrl: ${item.thumbnailUrl}, youtubeThumbnailUrl: ${item.youtubeThumbnailUrl}, Final URL: $finalImageUrl"
                    )

                    var hasThumbnailError by remember(finalImageUrl) { mutableStateOf(finalImageUrl.isNullOrBlank()) }
                    if (!hasThumbnailError && !finalImageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = finalImageUrl,
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            onSuccess = { hasImageLoaded = true },
                            onError = { hasThumbnailError = true; imageLoadError = true }
                        )
                        if (hasImageLoaded) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                accentColor.copy(alpha = 0.12f),
                                                accentColor.copy(alpha = 0.05f),
                                                Color.Black.copy(alpha = 0.45f)
                                            )
                                        )
                                    )
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.55f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        com.pravor.notessharing.ui.components.DocumentPlaceholder(documentType = "Video", modifier = Modifier.fillMaxSize())
                    }
                } else {
                    val imageUrl = if (!item.thumbnailUrl.isNullOrBlank()) item.thumbnailUrl else null
                    if (!imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                .data(imageUrl)
                                .crossfade(true)
                                .size(280, 200)
                                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                                .build(),
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            onSuccess = { hasImageLoaded = true },
                            onError = { imageLoadError = true }
                        )
                        if (hasImageLoaded) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                accentColor.copy(alpha = 0.12f),
                                                accentColor.copy(alpha = 0.05f),
                                                Color.Black.copy(alpha = 0.45f)
                                            )
                                        )
                                    )
                            )
                        }
                    }
                    val showFallback = (imageUrl.isNullOrBlank() || imageLoadError) && !hasImageLoaded
                    if (showFallback) {
                        com.pravor.notessharing.ui.components.DocumentPlaceholder(documentType = docTypeStr, modifier = Modifier.fillMaxSize())
                    }
                }

                // Overlay Badge/Chip on top of the thumbnail in top-right corner
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = docTypeStr.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // 2. MIDDLE: Title (2-line zone)
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                minLines = 2, // Fixed height symmetry!
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            // 3. Metadata Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Subject chip
                val subjText = item.subject?.trim()?.ifBlank { null } ?: "General"
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.06f),
                    border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.2f)),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = subjText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = accentColor.copy(alpha = 0.85f),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Conditional metadata chip
                val metaText = when {
                    isPyq -> item.examYear?.trim()?.ifBlank { null }
                    isAssignment -> (item.sectionDisplay ?: item.section)?.trim()?.ifBlank { null }
                    else -> null
                }

                if (!metaText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.12f),
                        border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = metaText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 4. BOTTOM: Bottom Metadata Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // downloads metric
                     Row(
                         verticalAlignment = Alignment.CenterVertically,
                         horizontalArrangement = Arrangement.spacedBy(4.dp)
                     ) {
                         Icon(
                             imageVector = Icons.Default.Download,
                             contentDescription = null,
                             tint = Color(0xFF64B5F6),
                             modifier = Modifier.size(16.dp)
                         )
                         Text(
                             text = item.downloads.toString(),
                             style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                             color = Color(0xFF64B5F6).copy(alpha = 0.9f),
                             fontWeight = FontWeight.Medium
                         )
                     }
                     // upvotes metric
                     Row(
                         verticalAlignment = Alignment.CenterVertically,
                         horizontalArrangement = Arrangement.spacedBy(4.dp)
                     ) {
                         Icon(
                             imageVector = Icons.Default.ThumbUp,
                             contentDescription = null,
                             tint = Color(0xFFFFB74D),
                             modifier = Modifier.size(16.dp)
                         )
                         Text(
                             text = item.upvotes.toString(),
                             style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                             color = Color(0xFFFFB74D).copy(alpha = 0.9f),
                             fontWeight = FontWeight.Medium
                         )
                     }
                }

                // Interactive bookmark button only
                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                         imageVector = if (item.isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                         contentDescription = "Bookmark",
                         tint = MaterialTheme.colorScheme.primary,
                         modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StudyHubCard(
    title: String,
    metadata: String,
    contextHint: String,
    icon: ImageVector,
    accentColor: Color,
    cardBrush: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "study-hub-press-scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Custom fluid tactile press animation
                onClick = onClick
            ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.12f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // TOP: Icon + Title + Metadata Row
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = accentColor.copy(alpha = 0.12f),
                        modifier = Modifier.size(44.dp),
                        border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.3f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = metadata,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = accentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // BOTTOM: Context helper hint
                Text(
                    text = contextHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Far right chevron tactile indicator
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ForYouGridCardSkeleton(
    alpha: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF181B1F), Color(0xFF0F1113))))
                .padding(10.dp)
        ) {
            // 1. TOP: Thumbnail placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.4f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.06f * alpha))
            )

            Spacer(Modifier.height(10.dp))

            // 2. MIDDLE: Title placeholders (2 lines of text)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.06f * alpha))
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.06f * alpha))
            )

            Spacer(Modifier.height(12.dp))

            // 3. Metadata Chips Row placeholder
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.06f * alpha))
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.06f * alpha))
                )
            }

            Spacer(Modifier.height(12.dp))

            // 4. BOTTOM: Bottom Metadata Row placeholder
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.06f * alpha))
                    )
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.06f * alpha))
                    )
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f * alpha))
                )
            }
        }
    }
}

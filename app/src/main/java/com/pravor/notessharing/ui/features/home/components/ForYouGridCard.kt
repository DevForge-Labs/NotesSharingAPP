package com.pravor.notessharing.ui.features.home.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.pravor.notessharing.domain.model.FeedItem
import com.pravor.notessharing.domain.model.FileType
import com.pravor.notessharing.domain.model.extractYoutubePlaylistId
import com.pravor.notessharing.ui.common.DocumentPlaceholder
import com.pravor.notessharing.ui.common.theme.getStudyResourceTheme
import java.util.Locale

private val CardBorderShape = RoundedCornerShape(22.dp)
private val ThumbnailShape = RoundedCornerShape(14.dp)
private val BadgeShape = RoundedCornerShape(6.dp)
private val PlayCircleShape = CircleShape

@Composable
fun ForYouGridCard(
    item: FeedItem,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onUpvoteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isVideo = item.fileType == FileType.Video
    val rawDocType = (item.documentType ?: item.type)?.lowercase(Locale.ROOT)?.trim()

    val docTypeStr = remember(item.id, rawDocType, item.fileType) {
        when (rawDocType) {
            "pyq" -> "PYQ"
            "cheatsheet", "cheat sheet" -> "Cheat Sheet"
            "assignment" -> "Assignment"
            "notes" -> "Notes"
            "video", "youtube resource" -> if (item.youtubeVideoId.isNullOrBlank() || (!item.youtubeUrl.isNullOrBlank() && extractYoutubePlaylistId(item.youtubeUrl) != null)) "YouTube Playlist" else "YouTube Video"
            else -> when {
                isVideo -> if (item.youtubeVideoId.isNullOrBlank() || (!item.youtubeUrl.isNullOrBlank() && extractYoutubePlaylistId(item.youtubeUrl) != null)) "YouTube Playlist" else "YouTube Video"
                item.fileType == FileType.Pyq || item.tags.any { it.equals("pyq", ignoreCase = true) } || item.title.contains("pyq", ignoreCase = true) -> "PYQ"
                item.fileType == FileType.CheatSheet || item.tags.any { it.equals("cheat sheet", ignoreCase = true) || it.equals("cheatsheet", ignoreCase = true) || it.equals("formula", ignoreCase = true) } || item.title.contains("cheat", ignoreCase = true) || item.title.contains("formula", ignoreCase = true) -> "Cheat Sheet"
                item.fileType == FileType.LabManual || item.tags.any { it.equals("assignment", ignoreCase = true) } || item.title.contains("assignment", ignoreCase = true) -> "Assignment"
                else -> "Notes"
            }
        }
    }

    val isPyq = remember(docTypeStr) { docTypeStr == "PYQ" }
    val isAssignment = remember(docTypeStr) { docTypeStr == "Assignment" }

    val theme = remember(docTypeStr, item.id) { getStudyResourceTheme(docTypeStr, item.id) }
    val accentColor = theme.accentColor
    val cardBrush = theme.cardBrush

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = CardBorderShape,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush)
                .padding(11.dp)
        ) {
            ForYouCardThumbnail(item = item, docTypeStr = docTypeStr, isVideo = isVideo, accentColor = accentColor)
            Spacer(Modifier.height(8.dp))
            ForYouCardInfo(item = item, isPyq = isPyq, isAssignment = isAssignment, accentColor = accentColor)
            Spacer(Modifier.height(8.dp))
            ForYouCardActions(item = item, accentColor = accentColor, onUpvoteClick = onUpvoteClick, onBookmarkClick = onBookmarkClick)
        }
    }
}

@Composable
private fun ForYouCardThumbnail(
    item: FeedItem,
    docTypeStr: String,
    isVideo: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.08f)
            .clip(ThumbnailShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.22f)), ThumbnailShape),
        contentAlignment = Alignment.Center
    ) {
        var imageLoadError by remember(item.id) { mutableStateOf(false) }

        if (isVideo) {
            val isYouTubeResource = item.type == "YouTube Resource" || item.documentType == "YouTube Resource"
            val finalImageUrl = if (isYouTubeResource) {
                if (!item.thumbnailUrl.isNullOrBlank()) item.thumbnailUrl
                else if (!item.youtubeThumbnailUrl.isNullOrBlank()) item.youtubeThumbnailUrl
                else null
            } else {
                if (!item.thumbnailUrl.isNullOrBlank()) item.thumbnailUrl else null
            }

            if (!imageLoadError && !finalImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = finalImageUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onError = { imageLoadError = true }
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.10f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.50f)
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(PlayCircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                DocumentPlaceholder(documentType = "Video", modifier = Modifier.fillMaxSize())
            }
        } else {
            val imageUrl = if (!item.thumbnailUrl.isNullOrBlank()) item.thumbnailUrl else null
            if (!imageUrl.isNullOrBlank() && !imageLoadError) {
                val context = LocalContext.current
                val imageRequest = remember(imageUrl) {
                    ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(false)
                        .size(320, 300)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build()
                }
                AsyncImage(
                    model = imageRequest,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onError = { imageLoadError = true }
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.10f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.50f)
                                )
                            )
                        )
                )
            } else {
                DocumentPlaceholder(documentType = docTypeStr, modifier = Modifier.fillMaxSize())
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .clip(BadgeShape)
                .background(Color.Black.copy(alpha = 0.65f))
                .border(BorderStroke(0.5.dp, accentColor.copy(alpha = 0.4f)), BadgeShape)
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(
                text = docTypeStr.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ForYouCardInfo(
    item: FeedItem,
    isPyq: Boolean,
    isAssignment: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val catalogRepo = remember {
        try {
            com.pravor.notessharing.data.repository.SubjectCatalogRepository.getInstance()
        } catch (e: Exception) {
            null
        }
    }
    val catalogVersion by (catalogRepo?.catalogVersionFlow ?: remember { kotlinx.coroutines.flow.MutableStateFlow(0L) }).collectAsState()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp,
                letterSpacing = 0.15.sp
            ),
            color = Color.White,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val rawSubj = item.subject?.trim()?.ifBlank { null } ?: "General"
            val subjText = remember(item.id, item.subject, item.subjectId, catalogVersion) {
                catalogRepo?.resolveShortName(item.subjectId ?: rawSubj, rawSubj) ?: rawSubj
            }
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(BadgeShape)
                    .background(accentColor.copy(alpha = 0.08f))
                    .border(BorderStroke(0.5.dp, accentColor.copy(alpha = 0.22f)), BadgeShape)
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = subjText,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val metaText = when {
                isPyq -> item.examYear?.trim()?.ifBlank { null }
                isAssignment -> (item.sectionDisplay ?: item.section)?.trim()?.ifBlank { null }
                else -> null
            }

            if (!metaText.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(BadgeShape)
                        .background(accentColor.copy(alpha = 0.08f))
                        .border(BorderStroke(0.5.dp, accentColor.copy(alpha = 0.22f)), BadgeShape)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = metaText,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ForYouCardActions(
    item: FeedItem,
    accentColor: Color,
    onUpvoteClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                    text = item.downloadsCount.toString(),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = Color(0xFF64B5F6).copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.clickable { onUpvoteClick() }
            ) {
                val upvoteTint = if (item.isUpvoted) Color(0xFFFFB74D) else MaterialTheme.colorScheme.onSurfaceVariant
                Icon(
                    imageVector = if (item.isUpvoted) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                    contentDescription = "Upvote",
                    tint = upvoteTint,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = item.upvotes.toString(),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = upvoteTint.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        IconButton(
            onClick = onBookmarkClick,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = if (item.isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                contentDescription = "Bookmark",
                tint = if (item.isSaved) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
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
    modifier: Modifier = Modifier,
    secondaryMetadata: String? = null,
    lottieAsset: String? = null,
    lottieScale: Float = 1.35f,
    actionContent: @Composable (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
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
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = accentColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.32f)),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (lottieAsset != null) {
                            val lottieCompositionResult = rememberLottieComposition(
                                LottieCompositionSpec.Asset(lottieAsset)
                            )
                            val lottieComposition = lottieCompositionResult.value
                            val lottieProgress by animateLottieCompositionAsState(
                                composition = lottieComposition,
                                iterations = LottieConstants.IterateForever
                            )

                            if (lottieComposition != null) {
                                LottieAnimation(
                                    composition = lottieComposition,
                                    progress = { lottieProgress },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .scale(lottieScale)
                                )
                            } else {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.5.sp,
                            letterSpacing = 0.15.sp
                        ),
                        color = Color.White
                    )

                    Text(
                        text = metadata,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = accentColor
                    )

                    if (secondaryMetadata != null) {
                        Text(
                            text = secondaryMetadata,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = accentColor.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = contextHint,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (actionContent != null) {
                actionContent()
            } else {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = accentColor.copy(alpha = 0.9f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private val SkeletonBgBrush = Brush.verticalGradient(listOf(Color(0xFF181B1F), Color(0xFF0F1113)))
private val SkeletonLine4Shape = RoundedCornerShape(4.dp)

@Composable
fun ForYouGridCardSkeleton(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton-shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton-shimmer-alpha"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardBorderShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SkeletonBgBrush)
                .padding(11.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.08f)
                    .clip(ThumbnailShape)
                    .background(Color.White.copy(alpha = 0.06f * alpha))
            )

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(14.dp)
                    .clip(SkeletonLine4Shape)
                    .background(Color.White.copy(alpha = 0.06f * alpha))
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(14.dp)
                    .clip(SkeletonLine4Shape)
                    .background(Color.White.copy(alpha = 0.06f * alpha))
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(20.dp)
                        .clip(BadgeShape)
                        .background(Color.White.copy(alpha = 0.06f * alpha))
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(20.dp)
                        .clip(BadgeShape)
                        .background(Color.White.copy(alpha = 0.06f * alpha))
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
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


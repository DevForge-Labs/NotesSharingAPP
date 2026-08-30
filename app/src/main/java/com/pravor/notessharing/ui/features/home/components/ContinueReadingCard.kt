package com.pravor.notessharing.ui.features.home.components

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*
import com.pravor.notessharing.ui.common.components.*
import com.pravor.notessharing.ui.common.theme.*

import com.pravor.notessharing.core.util.formatRelativeTime
import com.pravor.notessharing.core.util.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.util.concurrent.atomic.AtomicInteger
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.geometry.Offset
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.pravor.notessharing.domain.model.FeedItem
import com.pravor.notessharing.ui.common.utils.SubjectBadge
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

private fun downloadThumbnailFile(imageUrl: String, destinationFile: File): Boolean {
    var connection: HttpURLConnection? = null
    return try {
        destinationFile.parentFile?.mkdirs()
        val url = URL(imageUrl)
        connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.requestMethod = "GET"
        connection.connect()

        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            connection.inputStream.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } else {
            false
        }
    } catch (e: java.lang.Exception) {
        false
    } finally {
        connection?.disconnect()
    }
}

@Composable
fun ContinueReadingCard(
    item: FeedItem?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (item == null) return

    val recompositionCount = remember { AtomicInteger(0) }
    SideEffect {
        android.util.Log.d("RECOMPOSE", "[RECOMPOSE] ContinueReadingCard count=${recompositionCount.incrementAndGet()}")
    }

    val isVideo = item.fileType == com.pravor.notessharing.domain.model.FileType.Video
    
    val repository = remember { com.pravor.notessharing.data.repository.DocumentDetailRepository() }
    val videoRepository = remember { com.pravor.notessharing.data.repository.VideoDetailRepository() }

    val directUrl = remember(item.id, item.thumbnailUrl, item.youtubeThumbnailUrl) {
        if (!item.thumbnailUrl.isNullOrBlank()) item.thumbnailUrl
        else if (!item.youtubeThumbnailUrl.isNullOrBlank()) item.youtubeThumbnailUrl
        else null
    }

    val initialIsImage = remember(directUrl, isVideo) {
        if (directUrl.isNullOrBlank()) false
        else isVideo || directUrl.contains(".jpg", ignoreCase = true) ||
                directUrl.contains(".jpeg", ignoreCase = true) ||
                directUrl.contains(".png", ignoreCase = true) ||
                directUrl.contains(".webp", ignoreCase = true) ||
                directUrl.contains("unsplash.com", ignoreCase = true) ||
                directUrl.contains("firebasestorage.googleapis.com", ignoreCase = true)
    }

    var firstFileUrl by remember(item.id) { mutableStateOf(directUrl) }
    var isImage by remember(item.id) { mutableStateOf(initialIsImage) }
    var isLoading by remember(item.id) { mutableStateOf(directUrl == null) }
    var resolvedDocType by remember(item.id) { mutableStateOf(item.documentType) }
    val context = LocalContext.current

    LaunchedEffect(item.id, directUrl) {
        if (!directUrl.isNullOrBlank()) {
            firstFileUrl = directUrl
            isImage = initialIsImage
            resolvedDocType = item.documentType
            isLoading = false
            return@LaunchedEffect
        }

        // Fetch document metadata in a background thread only if no thumbnail URL was present
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val remoteUrlToUse = if (isVideo) {
                    val videoDoc = videoRepository.getVideo(item.id)
                    if (videoDoc != null && !videoDoc.thumbnailUrl.isNullOrBlank()) {
                        videoDoc.thumbnailUrl
                    } else if (videoDoc != null && !videoDoc.youtubeThumbnailUrl.isNullOrBlank()) {
                        videoDoc.youtubeThumbnailUrl
                    } else {
                        null
                    }
                } else {
                    val doc = repository.getDocument(item.id, collectionName = item.documentType)
                    if (doc != null && !doc.thumbnailUrl.isNullOrBlank()) {
                        doc.thumbnailUrl
                    } else if (doc != null && !doc.youtubeThumbnailUrl.isNullOrBlank()) {
                        doc.youtubeThumbnailUrl
                    } else if (doc != null && doc.fileUrls.isNotEmpty()) {
                        doc.fileUrls.first()
                    } else {
                        null
                    }
                }

                val isImg = if (!remoteUrlToUse.isNullOrBlank()) {
                    isVideo || remoteUrlToUse.contains(".jpg", ignoreCase = true) ||
                            remoteUrlToUse.contains(".jpeg", ignoreCase = true) ||
                            remoteUrlToUse.contains(".png", ignoreCase = true) ||
                            remoteUrlToUse.contains(".webp", ignoreCase = true) ||
                            remoteUrlToUse.contains("unsplash.com", ignoreCase = true) ||
                            remoteUrlToUse.contains("firebasestorage.googleapis.com", ignoreCase = true)
                } else false

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    firstFileUrl = remoteUrlToUse
                    isImage = isImg
                    resolvedDocType = item.documentType
                    isLoading = false
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    firstFileUrl = null
                    isImage = false
                    isLoading = false
                }
            }
        }
    }
    
    val rawDocType = remember(item.id, item.documentType, item.type, resolvedDocType) {
        (item.documentType ?: item.type ?: resolvedDocType)?.lowercase(java.util.Locale.ROOT)?.trim()
    }

    val badgeText = remember(item.id, rawDocType, isVideo) {
        val isYouTubePlaylist = isVideo && (
            item.youtubeVideoId.isNullOrBlank() ||
            (!item.youtubeUrl.isNullOrBlank() && com.pravor.notessharing.domain.model.extractYoutubePlaylistId(item.youtubeUrl) != null)
        )
        when {
            isYouTubePlaylist -> "YouTube Playlist"
            isVideo -> "YouTube Video"
            rawDocType == "pyq" || item.fileType == com.pravor.notessharing.domain.model.FileType.Pyq -> "PYQ"
            rawDocType == "cheatsheet" || rawDocType == "cheat sheet" || item.fileType == com.pravor.notessharing.domain.model.FileType.CheatSheet -> "Cheat Sheet"
            rawDocType == "assignment" || item.fileType == com.pravor.notessharing.domain.model.FileType.LabManual -> "Assignment"
            else -> "Notes"
        }
    }

    val isPyq = remember(badgeText) { badgeText == "PYQ" }
    val isCheatSheet = remember(badgeText) { badgeText == "Cheat Sheet" }
    val isAssignment = remember(badgeText) { badgeText == "Assignment" }
    val isNotes = remember(badgeText) { badgeText == "Notes" }

    val previewIcon = remember(badgeText, isVideo) {
        when {
            isVideo -> Icons.Default.PlayArrow
            badgeText == "PYQ" -> Icons.Default.Help
            badgeText == "Assignment" -> Icons.Default.Assignment
            badgeText == "Cheat Sheet" -> Icons.Default.Bolt
            badgeText == "Notes" -> Icons.Default.Description
            else -> Icons.Default.FilePresent
        }
    }

    val theme = remember(badgeText, item.id) { getStudyResourceTheme(badgeText, item.id) }
    val accentColor = theme.accentColor

    val catalogRepo = remember {
        try {
            com.pravor.notessharing.data.repository.SubjectCatalogRepository.getInstance()
        } catch (e: Exception) {
            null
        }
    }
    val catalogVersion by (catalogRepo?.catalogVersionFlow ?: remember { kotlinx.coroutines.flow.MutableStateFlow(0L) }).collectAsState()

    val actionText = if (isVideo) "Continue Watching" else "Continue Reading"
    val lastOpenedText = remember(item.uploadDate, isVideo) { formatRelativeTime(item.uploadDate, isVideo = isVideo) }
    val subtitleText = remember(item.id, item.subject, item.subjectId, item.examYear, item.sectionDisplay, item.section, isVideo, badgeText, catalogVersion) {
        val rawSubj = item.subject?.trim()?.ifBlank { null } ?: "General"
        val subj = catalogRepo?.resolveShortName(item.subjectId ?: rawSubj, rawSubj) ?: rawSubj
        when {
            isVideo || isNotes || isCheatSheet -> subj
            isPyq -> {
                val year = item.examYear?.trim()
                if (!year.isNullOrBlank()) "$subj • $year" else subj
            }
            isAssignment -> {
                val secDisp = item.sectionDisplay?.trim()?.ifBlank { null } ?: item.section?.trim()?.ifBlank { null }
                if (!secDisp.isNullOrBlank()) "$subj • $secDisp" else subj
            }
            else -> item.description.ifBlank { item.tags.firstOrNull().orEmpty() }.ifBlank { "General" }
        }
    }
    
    val cardBrush = remember(theme) {
        Brush.linearGradient(
            colors = listOf(
                theme.gradientColors[0],
                theme.gradientColors[1],
                Color(0xFF090A0E).copy(alpha = 0.78f)
            )
        )
    }
    
    val supportingText = remember(item.id, item.description, item.tags) {
        item.description.ifBlank { item.tags.firstOrNull().orEmpty() }.ifBlank { "General" }
    }
    val cardPadding = 14.dp
    val previewWidth = 120.dp
    val previewHeight = 94.dp
    val previewGap = 14.dp
    val previewShape = remember { RoundedCornerShape(16.dp) }
    val cardShape = remember { RoundedCornerShape(24.dp) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = cardShape,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush)
                .padding(cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.width(previewWidth),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(previewHeight)
                        .clip(previewShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)), previewShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isVideo) {
                        val isYouTubeVideo = !item.youtubeVideoId.isNullOrBlank()
                        val finalImageUrl = if (isYouTubeVideo) {
                            if (!item.thumbnailUrl.isNullOrBlank()) item.thumbnailUrl
                            else if (!item.youtubeThumbnailUrl.isNullOrBlank()) item.youtubeThumbnailUrl
                            else if (!firstFileUrl.isNullOrBlank()) firstFileUrl
                            else null
                        } else {
                            val isYouTubeResource = item.type == "YouTube Resource" || item.documentType == "YouTube Resource"
                            if (isYouTubeResource) {
                                if (!item.thumbnailUrl.isNullOrBlank()) item.thumbnailUrl
                                else if (!item.youtubeThumbnailUrl.isNullOrBlank()) item.youtubeThumbnailUrl
                                else if (!firstFileUrl.isNullOrBlank()) firstFileUrl
                                else null
                            } else {
                                if (!item.thumbnailUrl.isNullOrBlank()) item.thumbnailUrl
                                else if (!firstFileUrl.isNullOrBlank()) firstFileUrl
                                else null
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
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.45f)
                                            )
                                        )
                                    )
                            )
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.60f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        } else {
                            com.pravor.notessharing.ui.common.VideoPlaceholder(modifier = Modifier.fillMaxSize())
                        }
                    } else {
                        var imageLoadError by remember(item.id) { mutableStateOf(false) }

                        val showThumbnail = isImage && !firstFileUrl.isNullOrBlank() && !imageLoadError
                        if (showThumbnail) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                val imageRequest = remember(firstFileUrl) {
                                    ImageRequest.Builder(context)
                                        .data(run {
                                            val url = firstFileUrl
                                            if (url != null && !url.startsWith("http")) java.io.File(url) else url
                                        })
                                        .crossfade(false)
                                        .size(300, 200)
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
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.2f),
                                                    Color.Black.copy(alpha = 0.45f)
                                                )
                                            )
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .align(Alignment.BottomEnd)
                                        .graphicsLayer(translationX = -8f, translationY = -8f)
                                        .background(accentColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = previewIcon,
                                        contentDescription = null,
                                        tint = Color(0xFF0A0E14),
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        } else {
                            val placeholderType = when {
                                isNotes -> "Notes"
                                isPyq -> "PYQ"
                                isCheatSheet -> "Cheat Sheet"
                                isAssignment -> "Assignment"
                                else -> "PDF"
                            }
                            com.pravor.notessharing.ui.common.DocumentPlaceholder(
                                documentType = placeholderType,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = lastOpenedText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        lineHeight = 13.sp
                    ),
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(previewGap))

            Column(
                modifier = Modifier
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.30f))
                    ) {
                        Text(
                            text = badgeText.uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (isVideo || isPyq || isNotes || isCheatSheet || isAssignment) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = subtitleText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFFCBD5E1),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else if (supportingText.isNotBlank()) {
                    SubjectBadge(subject = supportingText)
                }

                Spacer(Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onClick,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color(0xFF07121E)
                        )
                    ) {
                        Text(
                            text = actionText,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShimmerPlaceholder(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer-translate"
    )

    val shimmerColors = listOf(
        Color(0xFF2A2C39),
        Color(0xFF3F4257),
        Color(0xFF2A2C39)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush)
    )
}

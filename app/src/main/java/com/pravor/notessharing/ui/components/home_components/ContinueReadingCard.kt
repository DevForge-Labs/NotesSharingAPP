package com.pravor.notessharing.ui.components.home_components

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.pravor.notessharing.model.FeedItem
import com.pravor.notessharing.ui.components.utils.SubjectBadge
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

    val isVideo = item.fileType == com.pravor.notessharing.model.FileType.Video
    
    val repository = remember { com.pravor.notessharing.data.DocumentDetailRepository() }
    val videoRepository = remember { com.pravor.notessharing.data.VideoDetailRepository() }
    var firstFileUrl by remember(item.id) { mutableStateOf<String?>(null) }
    var isImage by remember(item.id) { mutableStateOf(false) }
    var isLoading by remember(item.id) { mutableStateOf(true) }
    var resolvedDocType by remember(item.id) { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(item.id) {
        val isYouTubeVideo = isVideo && !item.youtubeVideoId.isNullOrBlank()
        if (isYouTubeVideo && (!item.thumbnailUrl.isNullOrBlank() || !item.youtubeThumbnailUrl.isNullOrBlank())) {
            isLoading = false
            return@LaunchedEffect
        }
        if (!isYouTubeVideo && isVideo && (!item.thumbnailUrl.isNullOrBlank() || !item.youtubeThumbnailUrl.isNullOrBlank())) {
            isLoading = false
            return@LaunchedEffect
        }

        val cacheDir = File(context.cacheDir, "continue-reading")
        val localFile = File(cacheDir, "${item.id}.jpg")
        val prefs = context.getSharedPreferences("continue_reading_file_cache", Context.MODE_PRIVATE)

        // 1. Check if local cache file exists
        if (localFile.exists()) {
            val cachedRemoteUrl = prefs.getString("${item.id}_remote_url", null)
            val cachedDocType = prefs.getString("${item.id}_doc_type", null)
            val remoteUrlToUse = if (!item.thumbnailUrl.isNullOrBlank()) item.thumbnailUrl else null

            // If the thumbnail URL hasn't changed, reuse the local file directly and skip network
            if (remoteUrlToUse == null || remoteUrlToUse == cachedRemoteUrl) {
                firstFileUrl = localFile.absolutePath
                isImage = true
                resolvedDocType = cachedDocType
                isLoading = false
                return@LaunchedEffect
            } else {
                // Invalidate cache if the URL has changed
                try {
                    localFile.delete()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }

        // 2. Fetch and download the file to persistent local storage in a background thread
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // If item.thumbnailUrl is blank, query the database for latest document metadata
                val remoteUrlToUse = if (!item.thumbnailUrl.isNullOrBlank()) {
                    item.thumbnailUrl
                } else if (!item.youtubeThumbnailUrl.isNullOrBlank()) {
                    item.youtubeThumbnailUrl
                } else {
                    if (isVideo) {
                        val videoDoc = videoRepository.getVideo(item.id)
                        if (videoDoc != null && !videoDoc.thumbnailUrl.isNullOrBlank()) {
                            videoDoc.thumbnailUrl
                        } else if (videoDoc != null && !videoDoc.youtubeThumbnailUrl.isNullOrBlank()) {
                            videoDoc.youtubeThumbnailUrl
                        } else {
                            null
                        }
                    } else {
                        val doc = repository.getDocument(item.id)
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
                }

                if (!remoteUrlToUse.isNullOrBlank()) {
                    if (isVideo) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            firstFileUrl = remoteUrlToUse
                            isImage = true
                            resolvedDocType = item.documentType
                        }
                    } else {
                        val isImg = remoteUrlToUse.contains(".jpg", ignoreCase = true) ||
                                    remoteUrlToUse.contains(".jpeg", ignoreCase = true) ||
                                    remoteUrlToUse.contains(".png", ignoreCase = true) ||
                                    remoteUrlToUse.contains(".webp", ignoreCase = true) ||
                                    remoteUrlToUse.contains("unsplash.com", ignoreCase = true) ||
                                    remoteUrlToUse.contains("firebasestorage.googleapis.com", ignoreCase = true)

                        if (isImg) {
                            val success = downloadThumbnailFile(remoteUrlToUse, localFile)
                            if (success) {
                                prefs.edit()
                                    .putString("${item.id}_remote_url", remoteUrlToUse)
                                    .putString("${item.id}_doc_type", item.documentType)
                                    .apply()

                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    firstFileUrl = localFile.absolutePath
                                    isImage = true
                                    resolvedDocType = item.documentType
                                }
                            } else {
                                // Fallback to direct remote URL if download fails
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    firstFileUrl = remoteUrlToUse
                                    isImage = true
                                    resolvedDocType = item.documentType
                                }
                            }
                        } else {
                            // Not an image file
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                firstFileUrl = remoteUrlToUse
                                isImage = false
                                resolvedDocType = item.documentType
                            }
                        }
                    }
                } else {
                    // No URL available
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        firstFileUrl = null
                        isImage = false
                        resolvedDocType = item.documentType
                    }
                }
            } catch (e: Exception) {
                // Fallback
            } finally {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }
    
    val rawDocType = (item.documentType ?: item.type ?: resolvedDocType)
        ?.lowercase(java.util.Locale.ROOT)?.trim()

    val isPyq = when (rawDocType) {
        "pyq" -> true
        "cheatsheet", "cheat sheet", "assignment", "notes" -> false
        else -> item.fileType == com.pravor.notessharing.model.FileType.Pyq ||
                item.tags.any { it.equals("pyq", ignoreCase = true) } ||
                item.title.contains("pyq", ignoreCase = true) ||
                item.description.contains("pyq", ignoreCase = true)
    }

    val isCheatSheet = when (rawDocType) {
        "cheatsheet", "cheat sheet" -> true
        "pyq", "assignment", "notes" -> false
        else -> item.fileType == com.pravor.notessharing.model.FileType.CheatSheet ||
                item.tags.any { it.equals("cheat sheet", ignoreCase = true) || it.equals("cheatsheet", ignoreCase = true) || it.equals("formula", ignoreCase = true) } ||
                item.title.contains("cheat", ignoreCase = true) ||
                item.title.contains("formula", ignoreCase = true) ||
                item.description.contains("cheat", ignoreCase = true) ||
                item.description.contains("formula", ignoreCase = true)
    }

    val isAssignment = when (rawDocType) {
        "assignment" -> true
        "pyq", "cheatsheet", "cheat sheet", "notes" -> false
        else -> item.fileType == com.pravor.notessharing.model.FileType.LabManual ||
                item.tags.any { it.equals("assignment", ignoreCase = true) } ||
                item.title.contains("assignment", ignoreCase = true) ||
                item.description.contains("assignment", ignoreCase = true)
    }

    val isNotes = when (rawDocType) {
        "notes" -> true
        "pyq", "cheatsheet", "cheat sheet", "assignment" -> false
        else -> item.fileType == com.pravor.notessharing.model.FileType.Notes ||
                item.tags.any { it.equals("notes", ignoreCase = true) || it.equals("lecture", ignoreCase = true) } ||
                item.title.contains("notes", ignoreCase = true) ||
                item.title.contains("lecture", ignoreCase = true) ||
                item.description.contains("notes", ignoreCase = true) ||
                item.description.contains("lecture", ignoreCase = true)
    }

    val previewIcon = when {
        isVideo -> Icons.Default.PlayArrow
        isPyq -> Icons.Default.Help
        isAssignment -> Icons.Default.Assignment
        isCheatSheet -> Icons.Default.Bolt
        isNotes -> Icons.Default.Description
        else -> Icons.Default.FilePresent
    }

    val isYouTubePlaylist = isVideo && (
        item.youtubeVideoId.isNullOrBlank() ||
        (!item.youtubeUrl.isNullOrBlank() && com.pravor.notessharing.model.extractYoutubePlaylistId(item.youtubeUrl) != null)
    )

    val badgeText = when {
        isYouTubePlaylist -> "YouTube Playlist"
        isVideo -> "YouTube Video"
        isNotes -> "Notes"
        isPyq -> "PYQ"
        isAssignment -> "Assignment"
        isCheatSheet -> "Cheat Sheet"
        else -> "PDF"
    }

    val theme = com.pravor.notessharing.ui.components.getStudyResourceTheme(badgeText)
    val accentColor = theme.accentColor

    val actionText = if (isVideo) "Continue Watching" else "Continue Reading"
    val lastOpenedText = formatRelativeTime(item.uploadDate, isVideo = isVideo)
    val supportingText = item.description.ifBlank { item.tags.firstOrNull().orEmpty() }.ifBlank { "General" }
    val subtitleText = remember(item, isVideo, isPyq, isAssignment, isNotes, isCheatSheet) {
        val subj = item.subject?.trim()?.ifBlank { null } ?: "General"
        when {
            isVideo || isNotes || isCheatSheet -> subj
            isPyq -> {
                val year = item.examYear?.trim()
                if (!year.isNullOrBlank()) "$subj   •   $year" else subj
            }
            isAssignment -> {
                val secDisp = item.sectionDisplay?.trim()?.ifBlank { null } ?: item.section?.trim()?.ifBlank { null }
                if (!secDisp.isNullOrBlank()) "$subj   •   $secDisp" else subj
            }
            else -> supportingText
        }
    }
    
    val cardBrush = Brush.linearGradient(
        colors = listOf(
            theme.gradientColors[0],
            theme.gradientColors[1],
            accentColor.copy(alpha = if (badgeText == "PDF") 0.22f else 0.18f)
        )
    )
    
    val cardHeight = 148.dp
    val cardPadding = 12.dp
    val previewWidth = 132.dp
    val previewHeight = 104.dp
    val previewShape = RoundedCornerShape(20.dp)
    val previewGap = 16.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = cardHeight)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(cardBrush)
                .padding(cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .width(previewWidth)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(previewHeight)
                        .clip(previewShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    if (isVideo) {
                        val isYouTubeVideo = !item.youtubeVideoId.isNullOrBlank()
                        val finalImageUrl = if (isYouTubeVideo) {
                            if (!item.thumbnailUrl.isNullOrBlank()) {
                                item.thumbnailUrl
                            } else if (!item.youtubeThumbnailUrl.isNullOrBlank()) {
                                item.youtubeThumbnailUrl
                            } else if (!firstFileUrl.isNullOrBlank()) {
                                firstFileUrl
                            } else {
                                null
                            }
                        } else {
                            val isYouTubeResource = item.type == "YouTube Resource" || item.documentType == "YouTube Resource"
                            if (isYouTubeResource) {
                                if (!item.thumbnailUrl.isNullOrBlank()) {
                                    item.thumbnailUrl
                                } else if (!item.youtubeThumbnailUrl.isNullOrBlank()) {
                                    item.youtubeThumbnailUrl
                                } else if (!firstFileUrl.isNullOrBlank()) {
                                    firstFileUrl
                                } else {
                                    null
                                }
                            } else {
                                if (!item.thumbnailUrl.isNullOrBlank()) {
                                    item.thumbnailUrl
                                } else if (!firstFileUrl.isNullOrBlank()) {
                                    firstFileUrl
                                } else {
                                    null
                                }
                            }
                        }

                        android.util.Log.d(
                            "YouTubeHomeThumbnail",
                            "ContinueReadingCard: Title: ${item.title}, Resource Type: ${if (isYouTubeVideo) "YouTube Video" else "Playlist/Other"}, thumbnailUrl: ${item.thumbnailUrl}, youtubeThumbnailUrl: ${item.youtubeThumbnailUrl}, Final URL: $finalImageUrl"
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
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.42f)
                                            )
                                        )
                                    )
                            )
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.58f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                accentColor.copy(alpha = 0.24f),
                                                Color(0xFF10151D)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }
                    } else {
                        var hasImageLoaded by remember(item.id) { mutableStateOf(false) }
                        var imageLoadError by remember(item.id) { mutableStateOf(false) }

                        if (isLoading) {
                            ShimmerPlaceholder()
                        } else {
                            val showThumbnail = isImage && !firstFileUrl.isNullOrBlank() && !imageLoadError
                            if (showThumbnail) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(run {
                                                val url = firstFileUrl
                                                if (url != null && !url.startsWith("http")) java.io.File(url) else url
                                            })
                                            .crossfade(true)
                                            .size(300, 200)
                                            .memoryCachePolicy(CachePolicy.ENABLED)
                                            .diskCachePolicy(CachePolicy.ENABLED)
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
                                                            Color.Transparent,
                                                            Color.Black.copy(alpha = 0.2f),
                                                            Color.Black.copy(alpha = 0.42f)
                                                        )
                                                    )
                                                )
                                        )
                                    }
                                    // Clean minimal icon tag floating at bottom-right
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .align(Alignment.BottomEnd)
                                            .graphicsLayer(translationX = -12f, translationY = -8f)
                                            .background(accentColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = previewIcon,
                                            contentDescription = null,
                                            tint = Color(0xFF10151D),
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }

                            val showShimmer = isImage && !firstFileUrl.isNullOrBlank() && !hasImageLoaded && !imageLoadError
                            val showFallback = (!isImage || firstFileUrl.isNullOrBlank() || imageLoadError) && !showShimmer

                            if (showShimmer) {
                                ShimmerPlaceholder()
                            } else if (showFallback) {
                            val placeholderType = when {
                                isNotes -> "Notes"
                                isPyq -> "PYQ"
                                isCheatSheet -> "Cheat Sheet"
                                isAssignment -> "Assignment"
                                else -> "PDF"
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                accentColor.copy(alpha = 0.22f),
                                                accentColor.copy(alpha = 0.05f),
                                                Color(0xFF0D1117)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                // Subtle background horizontal grid lines
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val width = size.width
                                    val height = size.height
                                    val numGridLines = 5
                                    for (i in 1..numGridLines) {
                                        val y = height * (i.toFloat() / (numGridLines + 1))
                                        drawLine(
                                            color = accentColor.copy(alpha = 0.08f),
                                            start = androidx.compose.ui.geometry.Offset(0f, y),
                                            end = androidx.compose.ui.geometry.Offset(width, y),
                                            strokeWidth = 1.dp.toPx()
                                        )
                                    }
                                }

                                when (placeholderType) {
                                    "Notes" -> {
                                        // Layered document stack
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // Back document
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 44.dp, height = 58.dp)
                                                    .graphicsLayer(rotationZ = -5f, translationX = -4f)
                                                    .background(Color(0xFF161F1E), RoundedCornerShape(4.dp))
                                                    .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                                            )
                                            // Front document
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 44.dp, height = 58.dp)
                                                    .background(Color(0xFF1D2A29), RoundedCornerShape(4.dp))
                                                    .border(1.2.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                                    .padding(6.dp)
                                            ) {
                                                Column(
                                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    // Header bar
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth(0.5f)
                                                            .height(3.dp)
                                                            .background(accentColor, RoundedCornerShape(1.5.dp))
                                                    )
                                                    // Content lines
                                                    Box(modifier = Modifier.fillMaxWidth().height(1.5.dp).background(accentColor.copy(alpha = 0.25f), RoundedCornerShape(1.dp)))
                                                    Box(modifier = Modifier.fillMaxWidth().height(1.5.dp).background(accentColor.copy(alpha = 0.25f), RoundedCornerShape(1.dp)))
                                                    Box(modifier = Modifier.fillMaxWidth(0.8f).height(1.5.dp).background(accentColor.copy(alpha = 0.25f), RoundedCornerShape(1.dp)))
                                                    Box(modifier = Modifier.fillMaxWidth(0.6f).height(1.5.dp).background(accentColor.copy(alpha = 0.25f), RoundedCornerShape(1.dp)))
                                                }
                                            }
                                        }
                                    }
                                    "PYQ" -> {
                                        // Exam sheet with subtle numbered sections
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 46.dp, height = 62.dp)
                                                    .background(Color(0xFF221B14), RoundedCornerShape(4.dp))
                                                    .border(1.2.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                                    .padding(6.dp)
                                            ) {
                                                Column(
                                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    // Q1 header row
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        // Q1 text
                                                        Box(
                                                            modifier = Modifier
                                                                .width(12.dp)
                                                                .height(3.dp)
                                                                .background(accentColor, RoundedCornerShape(1.5.dp))
                                                        )
                                                        // Question mark
                                                        Text(
                                                            text = "?",
                                                            color = accentColor.copy(alpha = 0.4f),
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    // Content lines
                                                    Box(modifier = Modifier.fillMaxWidth().height(1.5.dp).background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(1.dp)))
                                                    Box(modifier = Modifier.fillMaxWidth(0.9f).height(1.5.dp).background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(1.dp)))
                                                    
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    
                                                    // Q2 header
                                                    Box(
                                                        modifier = Modifier
                                                            .width(12.dp)
                                                            .height(3.dp)
                                                            .background(accentColor, RoundedCornerShape(1.5.dp))
                                                    )
                                                    Box(modifier = Modifier.fillMaxWidth().height(1.5.dp).background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(1.dp)))
                                                }
                                            }
                                        }
                                    }
                                    "Cheat Sheet" -> {
                                        // Structured quick-reference columns
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 50.dp, height = 64.dp)
                                                    .background(Color(0xFF1E1726), RoundedCornerShape(4.dp))
                                                    .border(1.2.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                                    .padding(4.dp)
                                            ) {
                                                Column(
                                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    // Cheat Sheet Title
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth(0.7f)
                                                            .height(3.dp)
                                                            .background(accentColor, RoundedCornerShape(1.5.dp))
                                                    )
                                                    // Split 2-column layout representing reference blocks
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().height(20.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                    ) {
                                                        // Block 1
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .fillMaxHeight()
                                                                .background(accentColor.copy(alpha = 0.08f), RoundedCornerShape(2.dp))
                                                                .border(0.5.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(2.dp))
                                                        )
                                                        // Block 2
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .fillMaxHeight()
                                                                .background(accentColor.copy(alpha = 0.08f), RoundedCornerShape(2.dp))
                                                                .border(0.5.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(2.dp))
                                                        )
                                                    }
                                                    // Bottom formulas
                                                    Box(modifier = Modifier.fillMaxWidth().height(1.5.dp).background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(1.dp)))
                                                    Box(modifier = Modifier.fillMaxWidth(0.8f).height(1.5.dp).background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(1.dp)))
                                                }
                                            }
                                        }
                                    }
                                    "Assignment" -> {
                                        // Clipboard checklist layout
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // Combined Clipboard + Clip container
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                // Clipboard Clip top
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = 14.dp, height = 4.dp)
                                                        .background(Color(0xFF33454E), RoundedCornerShape(topStart = 1.dp, topEnd = 1.dp))
                                                )
                                                // Clipboard Sheet
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = 44.dp, height = 58.dp)
                                                        .background(Color(0xFF141F24), RoundedCornerShape(4.dp))
                                                        .border(1.2.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 5.dp)
                                                ) {
                                                    Column(
                                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                                        modifier = Modifier.fillMaxSize()
                                                    ) {
                                                        Spacer(modifier = Modifier.height(1.dp))
                                                        // Checklist rows
                                                        repeat(3) { index ->
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                // Checked checkbox
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(5.dp)
                                                                        .background(
                                                                            if (index < 2) accentColor else Color.Transparent,
                                                                            CircleShape
                                                                        )
                                                                        .border(0.5.dp, accentColor, CircleShape)
                                                                )
                                                                // Line
                                                                Box(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth(if (index == 0) 0.8f else if (index == 1) 0.5f else 0.7f)
                                                                        .height(1.5.dp)
                                                                        .background(accentColor.copy(alpha = if (index < 2) 0.2f else 0.4f), RoundedCornerShape(1.dp))
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    else -> {
                                        // Elegant minimalist PDF visual
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 44.dp, height = 58.dp)
                                                    .background(Color(0xFF1B1E21), RoundedCornerShape(4.dp))
                                                    .border(1.2.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                                    .padding(5.dp)
                                            ) {
                                                Column(
                                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    // "PDF" miniature tag
                                                    Box(
                                                        modifier = Modifier
                                                            .size(width = 14.dp, height = 6.dp)
                                                            .background(Color(0xFFFF5252), RoundedCornerShape(1.dp)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "PDF",
                                                            color = Color.White,
                                                            fontSize = 4.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            lineHeight = 4.sp
                                                        )
                                                    }
                                                    // Text lines
                                                    Box(modifier = Modifier.fillMaxWidth().height(1.5.dp).background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(1.dp)))
                                                    Box(modifier = Modifier.fillMaxWidth(0.9f).height(1.5.dp).background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(1.dp)))
                                                    Box(modifier = Modifier.fillMaxWidth(0.7f).height(1.5.dp).background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(1.dp)))
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                // Clean minimal icon tag floating at bottom-right
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .align(Alignment.BottomEnd)
                                        .graphicsLayer(translationX = -12f, translationY = -8f)
                                        .background(accentColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = previewIcon,
                                        contentDescription = null,
                                        tint = Color(0xFF10151D),
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = lastOpenedText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        lineHeight = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(previewGap))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = accentColor.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = badgeText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(4.dp))

                    if (isVideo || isPyq || isNotes || isCheatSheet || isAssignment) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = subtitleText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        if (supportingText.isNotBlank()) {
                            SubjectBadge(subject = supportingText)
                        }
                    }
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onClick,
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 40.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color(0xFF10151D)
                            )
                        ) {
                            Text(
                                text = actionText,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    lineHeight = 16.sp
                                ),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
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

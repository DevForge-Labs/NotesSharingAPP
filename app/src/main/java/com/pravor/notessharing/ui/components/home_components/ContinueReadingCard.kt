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
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.pravor.notessharing.model.FeedItem

// Singleton memory cache to prevent redundant database hits on scroll recompositions
object ContinueReadingPreviewCache {
    private val cache = java.util.concurrent.ConcurrentHashMap<String, Triple<String, Boolean, String?>>()

    fun get(itemId: String): Triple<String, Boolean, String?>? = cache[itemId]

    fun put(itemId: String, url: String, isImage: Boolean, docType: String?) {
        cache[itemId] = Triple(url, isImage, docType)
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
    var firstFileUrl by remember(item.id) { mutableStateOf<String?>(null) }
    var isImage by remember(item.id) { mutableStateOf(false) }
    var isLoading by remember(item.id) { mutableStateOf(true) }
    var resolvedDocType by remember(item.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(item.id) {
        if (isVideo) {
            isLoading = false
            return@LaunchedEffect
        }

        if (!item.thumbnailUrl.isNullOrBlank()) {
            firstFileUrl = item.thumbnailUrl
            isImage = true
            isLoading = false
            return@LaunchedEffect
        }

        val cached = ContinueReadingPreviewCache.get(item.id)
        if (cached != null) {
            firstFileUrl = cached.first
            isImage = cached.second
            resolvedDocType = cached.third
            isLoading = false
            return@LaunchedEffect
        }

        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val doc = repository.getDocument(item.id)
                if (doc != null) {
                    val urlToUse = if (!doc.thumbnailUrl.isNullOrBlank()) {
                        doc.thumbnailUrl
                    } else if (doc.fileUrls.isNotEmpty()) {
                        doc.fileUrls.first()
                    } else {
                        null
                    }
                    val isImg = urlToUse != null && (
                        !doc.thumbnailUrl.isNullOrBlank() ||
                        urlToUse.contains(".jpg", ignoreCase = true) ||
                        urlToUse.contains(".jpeg", ignoreCase = true) ||
                        urlToUse.contains(".png", ignoreCase = true) ||
                        urlToUse.contains(".webp", ignoreCase = true) ||
                        urlToUse.contains("unsplash.com", ignoreCase = true)
                    )

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        firstFileUrl = urlToUse
                        isImage = isImg
                        resolvedDocType = doc.documentType
                        ContinueReadingPreviewCache.put(item.id, urlToUse ?: "", isImg, doc.documentType)
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

    val accentColor = when {
        isVideo -> Color(0xFFFF6B6B)
        isNotes -> Color(0xFF58D6D1)
        isPyq -> Color(0xFFFFB45C)
        isAssignment -> Color(0xFF7AD7FF)
        isCheatSheet -> Color(0xFFC7A6FF)
        else -> Color(0xFFCFD8DC) // Soft platinum/slate metallic neutral tone
    }

    val badgeText = when {
        isVideo -> "YouTube Video"
        isNotes -> "Notes"
        isPyq -> "PYQ"
        isAssignment -> "Assignment"
        isCheatSheet -> "Cheat Sheet"
        else -> "PDF"
    }

    val actionText = if (isVideo) "Continue Watching" else "Continue Reading"
    val lastOpenedText = formatRelativeTime(item.uploadDate, isVideo = isVideo)
    val supportingText = item.description.ifBlank { item.tags.firstOrNull().orEmpty() }.ifBlank { "General" }
    
    val cardBrush = if (isVideo) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF261D1E),
                Color(0xFF171213),
                accentColor.copy(alpha = 0.18f)
            )
        )
    } else {
        when {
            isNotes -> Brush.linearGradient(
                colors = listOf(
                    Color(0xFF182625),
                    Color(0xFF101717),
                    accentColor.copy(alpha = 0.18f)
                )
            )
            isPyq -> Brush.linearGradient(
                colors = listOf(
                    Color(0xFF28211A),
                    Color(0xFF191410),
                    accentColor.copy(alpha = 0.18f)
                )
            )
            isCheatSheet -> Brush.linearGradient(
                colors = listOf(
                    Color(0xFF211B28),
                    Color(0xFF151119),
                    accentColor.copy(alpha = 0.18f)
                )
            )
            isAssignment -> Brush.linearGradient(
                colors = listOf(
                    Color(0xFF172328),
                    Color(0xFF0F161A),
                    accentColor.copy(alpha = 0.18f)
                )
            )
            else -> Brush.linearGradient(
                colors = listOf(
                    Color(0xFF23272A),
                    Color(0xFF141618),
                    accentColor.copy(alpha = 0.22f)
                )
            )
        }
    }
    
    val cardHeight = 148.dp
    val cardPadding = 12.dp
    val previewWidth = 132.dp
    val previewHeight = 104.dp
    val previewShape = RoundedCornerShape(20.dp)
    val previewGap = 16.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
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
                        var hasThumbnailError by remember { mutableStateOf(item.youtubeVideoId.isNullOrBlank()) }
                        if (!hasThumbnailError) {
                            AsyncImage(
                                model = "https://img.youtube.com/vi/${item.youtubeVideoId}/mqdefault.jpg",
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
                                            .data(firstFileUrl)
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
                    style = MaterialTheme.typography.labelSmall,
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

                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onClick,
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            modifier = Modifier.height(40.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color(0xFF10151D)
                            )
                        ) {
                            Text(
                                text = actionText,
                                style = MaterialTheme.typography.labelMedium,
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

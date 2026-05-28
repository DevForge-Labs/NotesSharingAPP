package com.pravor.notessharing.ui.components.explore_components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.pravor.notessharing.data.DocumentDetailRepository
import com.pravor.notessharing.model.TrendingNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Singleton memory cache to prevent redundant database hits on scroll recompositions
object TrendingPreviewCache {
    private val cache = java.util.concurrent.ConcurrentHashMap<String, Pair<String, Boolean>>()

    fun get(noteId: String): Pair<String, Boolean>? = cache[noteId]

    fun put(noteId: String, url: String, isImage: Boolean) {
        cache[noteId] = Pair(url, isImage)
    }
}

@Composable
fun TrendingNoteCard(note: TrendingNote, onClick: () -> Unit = {}) {
    var firstAttachmentUrl by remember { mutableStateOf<String?>(null) }
    var isImage by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    val repository = remember { DocumentDetailRepository() }

    LaunchedEffect(note.id) {
        if (!note.thumbnailUrl.isNullOrBlank()) {
            firstAttachmentUrl = note.thumbnailUrl
            isImage = true
            isLoading = false
            return@LaunchedEffect
        }

        val cached = TrendingPreviewCache.get(note.id)
        if (cached != null) {
            firstAttachmentUrl = cached.first
            isImage = cached.second
            isLoading = false
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            try {
                val doc = repository.getDocument(note.id)
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
                    
                    withContext(Dispatchers.Main) {
                        firstAttachmentUrl = urlToUse
                        isImage = isImg
                        if (urlToUse != null) {
                            TrendingPreviewCache.put(note.id, urlToUse, isImg)
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback
            } finally {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    PressScaleSurface(
        modifier = Modifier.width(216.dp),
        shape = RoundedCornerShape(26.dp),
        onClick = onClick
    ) {
        Column(
            Modifier.padding(14.dp)
        ) {
            val docType = getDocumentTypeFromTitle(note.title)
            val previewIcon = when (docType) {
                "PYQ" -> Icons.Default.Help
                "Assignment" -> Icons.Default.Assignment
                "Cheat Sheet" -> Icons.Default.Bolt
                else -> Icons.Default.Description
            }
            val accentColor = when (docType) {
                "PYQ" -> Color(0xFFFFA4A2) // Softer red
                "Assignment" -> Color(0xFFA5D6A7) // Softer green
                "Cheat Sheet" -> Color(0xFFFFE082) // Softer amber
                else -> Color(0xFF90CAF9) // Softer blue
            }
            val previewGradient = when (docType) {
                "PYQ" -> listOf(Color(0xFF381F1F), Color(0xFF251414))
                "Assignment" -> listOf(Color(0xFF1C2E20), Color(0xFF132016))
                "Cheat Sheet" -> listOf(Color(0xFF322A1E), Color(0xFF221C14))
                else -> listOf(Color(0xFF202A38), Color(0xFF151C26))
            }
            
            // 1. PREVIEW THUMBNAIL (Box)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(previewGradient)
                    )
                    .border(
                        BorderStroke(1.dp, accentColor.copy(alpha = 0.15f)),
                        RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                var hasImageLoaded by remember { mutableStateOf(false) }

                if (isLoading) {
                    ShimmerPlaceholder()
                } else {
                    // Render actual image thumbnail if available
                    if (isImage && !firstAttachmentUrl.isNullOrBlank()) {
                        var imageLoadError by remember { mutableStateOf(false) }
                        if (!imageLoadError) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(firstAttachmentUrl)
                                    .crossfade(true)
                                    .size(300, 200) // Downsample thumbnail size to preserve GPU memory
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .build(),
                                contentDescription = note.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                onSuccess = { hasImageLoaded = true },
                                onError = { imageLoadError = true }
                            )

                            if (hasImageLoaded) {
                                // Subtle overlay shading for visual depth
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.2f),
                                                    Color.Black.copy(alpha = 0.4f)
                                                )
                                            )
                                        )
                                )
                            }
                        }
                    }

                    // Render stylized visual fallback graphic
                    val showShimmer = isImage && !firstAttachmentUrl.isNullOrBlank() && !hasImageLoaded
                    val showFallback = (!isImage || firstAttachmentUrl.isNullOrBlank() || !isImage) && !showShimmer
                    
                    if (showShimmer) {
                        ShimmerPlaceholder()
                    } else if (showFallback) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Box(
                                modifier = Modifier.size(width = 54.dp, height = 72.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    val foldSize = 10.dp.toPx()
                                    val cornerRadius = 6.dp.toPx()

                                    val path = Path().apply {
                                        moveTo(0f, cornerRadius)
                                        quadraticTo(0f, 0f, cornerRadius, 0f)
                                        lineTo(w - foldSize, 0f)
                                        lineTo(w, foldSize)
                                        lineTo(w, h - cornerRadius)
                                        quadraticTo(w, h, w - cornerRadius, h)
                                        lineTo(cornerRadius, h)
                                        quadraticTo(0f, h, 0f, h - cornerRadius)
                                        close()
                                    }

                                    drawPath(
                                        path = path,
                                        color = Color(0xFF2E3544).copy(alpha = 0.88f)
                                    )

                                    val foldPath = Path().apply {
                                        moveTo(w - foldSize, 0f)
                                        lineTo(w - foldSize, foldSize - 1.5.dp.toPx())
                                        quadraticTo(w - foldSize, foldSize, w - foldSize + 1.5.dp.toPx(), foldSize)
                                        lineTo(w, foldSize)
                                        close()
                                    }
                                    drawPath(
                                        path = foldPath,
                                        color = Color(0xFF414B60).copy(alpha = 0.95f)
                                    )

                                    drawPath(
                                        path = path,
                                        color = Color.White.copy(alpha = 0.12f),
                                        style = Stroke(width = 1.dp.toPx())
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 6.dp, end = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = previewIcon,
                                        contentDescription = null,
                                        tint = accentColor.copy(alpha = 0.85f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    
                                    Spacer(Modifier.height(8.dp))
                                    
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(30.dp)
                                                .height(2.dp)
                                                .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(1.dp))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(22.dp)
                                                .height(2.dp)
                                                .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(1.dp))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(26.dp)
                                                .height(2.dp)
                                                .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(1.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 2. SUBJECT NAME (Main Title)
            Text(
                text = note.subject,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            // 3. DOCUMENT TYPE CHIP (Metadata below subject name)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accentColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
            ) {
                Text(
                    text = docType.uppercase(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            // 4. STATS ROW
            Row(verticalAlignment = Alignment.CenterVertically) {
                SmallMetric(Icons.Default.Download, note.downloads.toString())
                Spacer(Modifier.width(10.dp))
                SmallMetric(Icons.Default.ThumbUp, note.upvotes.toString())
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = if (note.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            // 5. ACTION BUTTON ROW
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(note.upvotes.toString())
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

private fun getDocumentTypeFromTitle(title: String): String {
    val t = title.lowercase(java.util.Locale.ROOT)
    return when {
        t.contains("pyq") || t.contains("solved") || t.contains("exam") || t.contains("paper") -> "PYQ"
        t.contains("cheat") || t.contains("formula") || t.contains("quick") || t.contains("sheet") -> "Cheat Sheet"
        t.contains("lab") || t.contains("assignment") || t.contains("manual") || t.contains("practice") -> "Assignment"
        else -> "Notes"
    }
}

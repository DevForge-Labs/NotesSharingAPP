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
import com.pravor.notessharing.ui.components.DocumentPlaceholder
import com.pravor.notessharing.ui.components.utils.SubjectBadge
import com.pravor.notessharing.ui.components.utils.getDocumentTypeFromTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.compose.foundation.clickable
import androidx.compose.runtime.*

// Singleton memory cache to prevent redundant database hits on scroll recompositions
object TrendingPreviewCache {
    private val cache = java.util.concurrent.ConcurrentHashMap<String, Pair<String, Boolean>>()

    fun get(noteId: String): Pair<String, Boolean>? = cache[noteId]

    fun put(noteId: String, url: String, isImage: Boolean) {
        cache[noteId] = Pair(url, isImage)
    }
}

object TrendingCardDiagnostics {
    val totalRequests = java.util.concurrent.atomic.AtomicInteger(0)
    val cacheHits = java.util.concurrent.atomic.AtomicInteger(0)
    val cacheMisses = java.util.concurrent.atomic.AtomicInteger(0)
}

@Composable
fun TrendingNoteCard(
    note: TrendingNote,
    onBookmarkClick: () -> Unit = {},
    onClick: () -> Unit = {},
    onUpvoteClick: () -> Unit = {}
) {
    val recompositionCount = remember { java.util.concurrent.atomic.AtomicInteger(0) }
    SideEffect {
        android.util.Log.d("RECOMPOSE", "[RECOMPOSE] TrendingNoteCard id=${note.id} count=${recompositionCount.incrementAndGet()}")
    }

    val cached = remember(note.id, note.thumbnailUrl) {
        val res = if (!note.thumbnailUrl.isNullOrBlank()) {
            Pair(note.thumbnailUrl, true)
        } else {
            TrendingPreviewCache.get(note.id)
        }
        if (res == null) {
            val misses = TrendingCardDiagnostics.cacheMisses.incrementAndGet()
            android.util.Log.d("PERF", "[PERF] Metadata cache MISS id=${note.id}")
        }
        res
    }

    var firstAttachmentUrl by remember(note.id) { mutableStateOf(cached?.first) }
    var isImage by remember(note.id) { mutableStateOf(cached?.second ?: false) }
    var isLoading by remember(note.id) { mutableStateOf(cached == null) }

    val repository = remember { DocumentDetailRepository() }

    LaunchedEffect(note.id) {
        if (cached != null) return@LaunchedEffect

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

    val documentTypeField = if (note.documentType.isNotBlank()) note.documentType else null
    val typeField = note.type

    val rawDocType = (documentTypeField ?: typeField)
        ?.lowercase(java.util.Locale.ROOT)?.trim()

    val docType = when (rawDocType) {
        "pyq" -> "PYQ"
        "cheatsheet", "cheat sheet" -> "Cheat Sheet"
        "assignment" -> "Assignment"
        "notes" -> "Notes"
        else -> getDocumentTypeFromTitle(note.title)
    }
    
    val theme = com.pravor.notessharing.ui.components.getStudyResourceTheme(docType)
    val accentColor = theme.accentColor
    val cardBrush = theme.cardBrush

    val previewIcon = when (docType) {
        "PYQ" -> Icons.Default.Help
        "Assignment" -> Icons.Default.Assignment
        "Cheat Sheet" -> Icons.Default.Bolt
        else -> Icons.Default.Description
    }

    PressScaleSurface(
        modifier = Modifier.width(216.dp),
        shape = RoundedCornerShape(26.dp),
        brush = cardBrush,
        onClick = onClick
    ) {
        Column(
            Modifier.padding(14.dp)
        ) {
            // 1. PREVIEW THUMBNAIL (Box)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(theme.gradientColors)
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
                            val context = LocalContext.current
                            val imageRequest = remember(firstAttachmentUrl) {
                                ImageRequest.Builder(context)
                                    .data(firstAttachmentUrl)
                                    .crossfade(true)
                                    .size(300, 200) // Downsample thumbnail size to preserve GPU memory
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .build()
                            }
                            AsyncImage(
                                model = imageRequest,
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
                        DocumentPlaceholder(documentType = docType, modifier = Modifier.fillMaxSize())
                    }
                }

                // Overlay badge on thumbnail in top-right corner
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = docType.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 2. SUBJECT NAME (Main Title)
            val isTitleValid = note.title.isNotBlank() && note.title != "Untitled Document"
            val displayTitle = if (isTitleValid) note.title else note.subject

            Text(
                text = displayTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 3. SUBJECT BADGE (Primary position below title area)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (note.subject.isNotBlank()) {
                    SubjectBadge(subject = note.subject)
                }

                // Conditional Year Badge for PYQ documents (Aligned Far Right)
                if (docType == "PYQ" && !note.examYear.isNullOrBlank()) {
                    val normalizedSubject = remember(note.subject) { com.pravor.notessharing.ui.components.utils.normalizeSubject(note.subject) }
                    val subjectBadgeColor = remember(normalizedSubject) { com.pravor.notessharing.ui.components.utils.getSubjectColor(normalizedSubject) }
                    val yearColor = subjectBadgeColor
                    val yearBgColor = subjectBadgeColor.copy(alpha = 0.12f)
                    val yearBorderColor = subjectBadgeColor.copy(alpha = 0.45f)

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = yearBgColor,
                        border = BorderStroke(1.dp, yearBorderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = note.examYear,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = yearColor,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 4. STATS ROW
            Row(verticalAlignment = Alignment.CenterVertically) {
                SmallMetric(Icons.Default.Download, note.downloadsCount.toString())
                Spacer(Modifier.width(10.dp))
                SmallMetric(Icons.Default.ThumbUp, note.upvotes.toString())
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = if (note.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onBookmarkClick() }
                )
            }

            Spacer(Modifier.height(10.dp))

            // 5. ACTION BUTTON ROW
            val buttonColor = if (note.isUpvoted) Color(0xFFFFB74D) else MaterialTheme.colorScheme.primaryContainer
            val buttonContentColor = if (note.isUpvoted) Color(0xFF141A23) else MaterialTheme.colorScheme.onPrimaryContainer
            Button(
                onClick = onUpvoteClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = buttonContentColor
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(note.upvotes.toString())
            }
        }
    }
}

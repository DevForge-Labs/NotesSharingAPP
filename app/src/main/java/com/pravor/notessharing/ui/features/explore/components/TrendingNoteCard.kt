package com.pravor.notessharing.ui.features.explore.components

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*
import com.pravor.notessharing.ui.common.theme.*

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
import androidx.compose.material.icons.filled.School
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.pravor.notessharing.data.repository.DocumentDetailRepository
import com.pravor.notessharing.domain.model.TrendingNote
import com.pravor.notessharing.ui.common.DocumentPlaceholder
import com.pravor.notessharing.ui.common.utils.SubjectBadge
import com.pravor.notessharing.ui.common.utils.getDocumentTypeFromTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun NoPropagationBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    event.changes.forEach { it.consume() }
                }
            }
        },
        content = content
    )
}

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

private val OverlayShadingBrush = Brush.verticalGradient(
    colors = listOf(
        Color.Transparent,
        Color.Black.copy(alpha = 0.2f),
        Color.Black.copy(alpha = 0.4f)
    )
)

@Composable
fun TrendingNoteCard(
    note: TrendingNote,
    onBookmarkClick: () -> Unit = {},
    onClick: () -> Unit = {},
    onUpvoteClick: () -> Unit = {}
) {

    val cached = remember(note.id, note.thumbnailUrl) {
        val res = if (!note.thumbnailUrl.isNullOrBlank()) {
            Pair(note.thumbnailUrl, true)
        } else {
            TrendingPreviewCache.get(note.id)
        }
        if (res == null) {
            val misses = TrendingCardDiagnostics.cacheMisses.incrementAndGet()
            if (com.pravor.notessharing.BuildConfig.DEBUG) {
                android.util.Log.d("PERF", "[PERF] Metadata cache MISS id=${note.id}")
            }
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
                val targetCollection = if (note.documentType.isNotBlank()) note.documentType else note.type
                val doc = repository.getDocument(note.id, collectionName = targetCollection)
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

    val docInfo: Triple<String, StudyResourceTheme, ImageVector> = remember(note.documentType, note.type, note.title) {
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
        
        val theme = getStudyResourceTheme(docType)
        val previewIcon = when (docType) {
            "PYQ" -> Icons.Default.Help
            "Assignment" -> Icons.Default.Assignment
            "Cheat Sheet" -> Icons.Default.Bolt
            else -> Icons.Default.Description
        }
        Triple(docType, theme, previewIcon)
    }

    val docType = docInfo.first
    val theme = docInfo.second
    val previewIcon = docInfo.third
    val accentColor = theme.accentColor
    val cardBrush = theme.cardBrush

    val displayTitle = remember(note.title, note.subject) {
        val isTitleValid = note.title.isNotBlank() && note.title != "Untitled Document"
        if (isTitleValid) note.title else note.subject
    }

    val badgeSubject = remember(note.displaySubject, note.subject) {
        when {
            !note.displaySubject.isNullOrBlank() -> note.displaySubject
            note.subject.isNotBlank() -> note.subject
            else -> "Unknown"
        }
    }

    val finalBorderColor = remember(accentColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            (accentColor.red * 255f).toInt().coerceIn(0, 255),
            (accentColor.green * 255f).toInt().coerceIn(0, 255),
            (accentColor.blue * 255f).toInt().coerceIn(0, 255),
            hsv
        )
        // Brighten and increase vibrancy slightly for a subtle themed glow
        hsv[1] = (hsv[1] * 1.15f).coerceAtMost(1.0f)
        hsv[2] = (hsv[2] * 1.20f).coerceAtMost(1.0f)
        Color(android.graphics.Color.HSVToColor(hsv)).copy(alpha = 0.35f)
    }

    PressScaleSurface(
        modifier = Modifier
            .width(216.dp)
            .height(CAROUSEL_CARD_HEIGHT)
            .border(BorderStroke(1.5.dp, finalBorderColor), RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        brush = cardBrush,
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Layer: Thumbnail (Image / Fallback / Shimmer)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(theme.thumbnailBrush)
            ) {
                var hasImageLoaded by remember { mutableStateOf(false) }

                if (isLoading) {
                    ShimmerPlaceholder(modifier = Modifier.fillMaxSize())
                } else {
                    // Render actual image thumbnail if available
                    if (isImage && !firstAttachmentUrl.isNullOrBlank()) {
                        var imageLoadError by remember { mutableStateOf(false) }
                        if (!imageLoadError) {
                            val context = LocalContext.current
                            val imageRequest = remember(firstAttachmentUrl, context) {
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
                                        .background(OverlayShadingBrush)
                                )
                            }
                        }
                    }

                    // Render stylized visual fallback graphic
                    val showShimmer = isImage && !firstAttachmentUrl.isNullOrBlank() && !hasImageLoaded
                    val showFallback = (!isImage || firstAttachmentUrl.isNullOrBlank() || !isImage) && !showShimmer
                    
                    if (showShimmer) {
                        ShimmerPlaceholder(modifier = Modifier.fillMaxSize())
                    } else if (showFallback) {
                        DocumentPlaceholder(documentType = docType, modifier = Modifier.fillMaxSize())
                    }
                }
            }

            // Translucent vertical gradient for text/chip legibility with atmospheric depth
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0.48f to Color.Transparent,
                            0.68f to Color(0xFF1C1C1E).copy(alpha = 0.78f),
                            0.88f to Color(0xFF090A0E).copy(alpha = 0.82f)
                        )
                    )
            )

            // Foreground Content Layer
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(vertical = 14.dp)
            ) {
                // Top placeholder box to preserve exact height and badge positioning
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(108.dp)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Overlay badge in top-right corner
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

                Spacer(Modifier.height(36.dp))

                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )

            // 3. SUBJECT BADGE (Primary position below title area)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SubjectBadge(
                    subject = badgeSubject,
                    disableNormalization = !note.displaySubject.isNullOrBlank()
                )

                // Conditional Section Badge for Assignment documents (Aligned Far Right)
                val secDisp = note.sectionDisplay?.trim() ?: ""
                if (docType == "Assignment" && secDisp.isNotBlank()) {
                    val normalizedSubject = remember(note.subject) { com.pravor.notessharing.ui.common.utils.normalizeSubject(note.subject) }
                    val subjectColor = remember(normalizedSubject) { com.pravor.notessharing.ui.common.utils.getSubjectColor(normalizedSubject) }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = subjectColor.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, subjectColor.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = subjectColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = secDisp,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = subjectColor,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Conditional Year Badge for PYQ documents (Aligned Far Right)
                if (docType == "PYQ" && !note.examYear.isNullOrBlank()) {
                    val normalizedSubject = remember(note.subject) { com.pravor.notessharing.ui.common.utils.normalizeSubject(note.subject) }
                    val subjectBadgeColor = remember(normalizedSubject) { com.pravor.notessharing.ui.common.utils.getSubjectColor(normalizedSubject) }
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

            Spacer(Modifier.weight(1f))

            // 4. STATS ROW (Anchored bottom footer)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp)
            ) {
                // Download Metric
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Downloads",
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = note.downloadsCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64B5F6).copy(alpha = 0.9f),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.width(10.dp))

                // Clickable Upvote Metric
                NoPropagationBox {
                    val scale by animateFloatAsState(
                        targetValue = if (note.isUpvoted) 1.15f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "upvote-scale"
                    )
                    
                    val upvoteColor = if (note.isUpvoted) Color(0xFFFFB74D) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .scale(scale)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onUpvoteClick() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = "Upvote",
                            tint = upvoteColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = note.upvotes.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = upvoteColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                // Bookmark Button
                NoPropagationBox {
                    Icon(
                        imageVector = if (note.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(23.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onBookmarkClick() }
                            .padding(2.dp)
                    )
                }
            }
        }
    }
}
}

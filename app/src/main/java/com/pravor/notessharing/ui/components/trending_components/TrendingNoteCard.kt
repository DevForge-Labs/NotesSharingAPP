package com.pravor.notessharing.ui.components.trending_components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.FilePresent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.pravor.notessharing.data.DocumentDetailRepository
import com.pravor.notessharing.model.DocumentDetail
import com.pravor.notessharing.model.TrendingNote
import com.pravor.notessharing.ui.components.Avatar
import com.pravor.notessharing.ui.components.DocumentPlaceholder

@Composable
fun TrendingNoteDiscoveryCard(
    note: TrendingNote,
    detailRepository: DocumentDetailRepository,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // If metadata fields are already pre-loaded/cached, we can display the content instantly!
    val hasCache = remember(note) { note.uploaderName.isNotBlank() }

    var documentDetail by remember { mutableStateOf<DocumentDetail?>(null) }
    var contributorLevel by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(!hasCache) }

    LaunchedEffect(note.id) {
        if (!hasCache) {
            isLoading = true
            val doc = detailRepository.getDocument(note.id)
            documentDetail = doc
            if (doc != null) {
                contributorLevel = detailRepository.getUploaderContributorLevel(doc.uploaderId)
            }
            isLoading = false
        }
    }

    if (isLoading) {
        TrendingNoteDiscoveryShimmerCard(modifier = modifier)
    } else {
        if (hasCache) {
            TrendingNoteDiscoveryCardContentFromNote(
                note = note,
                onClick = onClick,
                modifier = modifier
            )
        } else {
            val doc = documentDetail
            if (doc != null) {
                TrendingNoteDiscoveryCardContent(
                    doc = doc,
                    contributorLevel = contributorLevel ?: "Bronze Contributor",
                    onClick = onClick,
                    modifier = modifier
                )
            } else {
                TrendingNoteDiscoveryCardContentFallback(
                    note = note,
                    onClick = onClick,
                    modifier = modifier
                )
            }
        }
    }
}

@Composable
private fun TrendingNoteDiscoveryCardContent(
    doc: DocumentDetail,
    contributorLevel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Side: Portrait ratio thumbnail preview (visually resembling a document cover)
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                TrendingNoteThumbnail(doc = doc)
            }

            // Right Side Content Column (occupies remaining width)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(160.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Document Title (bold, max 2 lines)
                Text(
                    text = doc.subject,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Uploader Section (Avatar + Stacked Name & Contributor Level)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val initials = if (doc.uploaderName.isNotBlank()) {
                        doc.uploaderName.split(" ")
                            .filter { it.isNotBlank() }
                            .take(2)
                            .map { it.first().uppercase() }
                            .joinToString("")
                            .ifBlank { "PN" }
                    } else {
                        "PN"
                    }

                    if (doc.uploaderPhotoUrl.isNotEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(doc.uploaderPhotoUrl)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .networkCachePolicy(CachePolicy.ENABLED)
                                .crossfade(true)
                                .build(),
                            contentDescription = doc.uploaderName,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Avatar(
                            text = initials,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = doc.uploaderName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = contributorLevel,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Description (max 2 lines, ellipsis, hidden if blank)
                if (doc.description.isNotBlank()) {
                    Text(
                        text = doc.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Spacer(modifier = Modifier.height(1.dp))
                }

                // Bottom Row (Badge aligned left, Stats aligned right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DocumentTypeBadge(type = doc.documentType)

                    TrendingNoteStats(
                        downloads = doc.downloads,
                        upvotes = doc.upvotes,
                        bookmarks = doc.bookmarks
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendingNoteDiscoveryCardContentFromNote(
    note: TrendingNote,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Side: Portrait ratio thumbnail preview (document cover layout)
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                var hasError by remember { mutableStateOf(false) }
                if (!note.thumbnailUrl.isNullOrBlank() && !hasError) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(note.thumbnailUrl)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .networkCachePolicy(CachePolicy.ENABLED)
                            .crossfade(true)
                            .build(),
                        contentDescription = note.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onError = { hasError = true }
                    )
                } else {
                    DocumentPlaceholder(documentType = note.documentType, modifier = Modifier.fillMaxSize())
                }
            }

            // Right Side Content Column (occupies remaining width)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(160.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Title (bold, max 2 lines)
                Text(
                    text = note.subject,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Uploader Section (Avatar + Stacked details)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val initials = if (note.uploaderName.isNotBlank()) {
                        note.uploaderName.split(" ")
                            .filter { it.isNotBlank() }
                            .take(2)
                            .map { it.first().uppercase() }
                            .joinToString("")
                            .ifBlank { "PN" }
                    } else {
                        "PN"
                    }

                    if (note.uploaderPhotoUrl.isNotEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(note.uploaderPhotoUrl)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .networkCachePolicy(CachePolicy.ENABLED)
                                .crossfade(true)
                                .build(),
                            contentDescription = note.uploaderName,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Avatar(
                            text = initials,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = note.uploaderName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = note.contributorLevel,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Description (max 2 lines)
                if (note.description.isNotBlank()) {
                    Text(
                        text = note.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Spacer(modifier = Modifier.height(1.dp))
                }

                // Bottom Row (Type Badge and Stats)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DocumentTypeBadge(type = note.documentType)

                    TrendingNoteStats(
                        downloads = note.downloads,
                        upvotes = note.upvotes,
                        bookmarks = note.bookmarks
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendingNoteDiscoveryCardContentFallback(
    note: TrendingNote,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                FallbackThumbnailImage(note = note)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(160.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = note.subject,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val initials = if (note.uploaderName.isNotBlank()) {
                        note.uploaderName.split(" ")
                            .filter { it.isNotBlank() }
                            .take(2)
                            .map { it.first().uppercase() }
                            .joinToString("")
                            .ifBlank { "PN" }
                    } else {
                        "PN"
                    }

                    Avatar(
                        text = initials,
                        modifier = Modifier.size(30.dp)
                    )

                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = note.uploaderName.ifBlank { "Anonymous" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = note.contributorLevel.ifBlank { "Bronze Contributor" },
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (note.description.isNotBlank()) {
                    Text(
                        text = note.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Spacer(modifier = Modifier.height(1.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DocumentTypeBadge(type = note.documentType)

                    TrendingNoteStats(
                        downloads = note.downloads,
                        upvotes = note.upvotes,
                        bookmarks = note.bookmarks
                    )
                }
            }
        }
    }
}

@Composable
private fun FallbackThumbnailImage(note: TrendingNote) {
    if (!note.thumbnailUrl.isNullOrBlank()) {
        var hasError by remember { mutableStateOf(false) }
        if (!hasError) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(note.thumbnailUrl)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .crossfade(true)
                    .build(),
                contentDescription = note.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = { hasError = true }
            )
        } else {
            DocumentPlaceholder(documentType = note.documentType, modifier = Modifier.fillMaxSize())
        }
    } else {
        DocumentPlaceholder(documentType = note.documentType, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun DocumentTypeBadge(type: String) {
    val (backgroundColor, textColor) = when (type.lowercase(java.util.Locale.ROOT).replace(" ", "")) {
        "notes" -> Pair(Color(0xFFE3F2FD), Color(0xFF1565C0))
        "assignment" -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        "pyq" -> Pair(Color(0xFFFCE4EC), Color(0xFFC2185B))
        "cheatsheet" -> Pair(Color(0xFFFFF8E1), Color(0xFFF57F17))
        else -> Pair(Color(0xFFF3E5F5), Color(0xFF7B1FA2))
    }

    val isDark = !MaterialTheme.colorScheme.primary.toArgb().equals(Color(0xFF1A67B3).toArgb())

    val bg = if (isDark) {
        when (type.lowercase(java.util.Locale.ROOT).replace(" ", "")) {
            "notes" -> Color(0xFF173A5F)
            "assignment" -> Color(0xFF16392F)
            "pyq" -> Color(0xFF51241F)
            "cheatsheet" -> Color(0xFF4A3B18)
            else -> Color(0xFF381E4C)
        }
    } else {
        backgroundColor
    }

    val textCol = if (isDark) {
        when (type.lowercase(java.util.Locale.ROOT).replace(" ", "")) {
            "notes" -> Color(0xFFE4F1FF)
            "assignment" -> Color(0xFFDEFFF0)
            "pyq" -> Color(0xFFFFE7E3)
            "cheatsheet" -> Color(0xFFFFE082)
            else -> Color(0xFFF5E4FF)
        }
    } else {
        textColor
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = type.uppercase(java.util.Locale.ROOT),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textCol,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TrendingNoteDiscoveryShimmerCard(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer-alpha"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = alpha)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .height(160.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(160.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(20.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(20.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(12.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                        )
                        Box(
                            modifier = Modifier
                                .width(70.dp)
                                .height(10.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(14.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(20.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(16.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

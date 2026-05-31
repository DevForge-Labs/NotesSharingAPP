package com.pravor.notessharing.ui.components.trending_components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.pravor.notessharing.model.DocumentDetail
import com.pravor.notessharing.model.TrendingNote
import com.pravor.notessharing.ui.components.Avatar
import com.pravor.notessharing.ui.components.DocumentPlaceholder
import com.pravor.notessharing.ui.components.utils.SubjectBadge

@Composable
fun TrendingNoteDiscoveryCardContent(
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
                val isTitleValid = doc.title.isNotBlank() && doc.title != "Untitled Document"
                val displayTitle = if (isTitleValid) doc.title else doc.subject
                Column {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 23.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (isTitleValid && doc.subject.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        SubjectBadge(subject = doc.subject, isLarge = true, semester = doc.semester)
                    }

                    // Display branch under the title if it is not default and different from subject
                    if (isTitleValid && doc.branch.isNotBlank() && doc.branch != "Computer Science" && doc.branch != doc.subject) {
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1976D2).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFF1976D2).copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Book,
                                    contentDescription = null,
                                    tint = Color(0xFF1976D2),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = doc.branch,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = Color(0xFF1976D2),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
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

                // Semester Information shifted to SubjectBadge

                // Bottom Row (Badge aligned left, Stats aligned right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DocumentTypeBadge(type = doc.documentType, year = doc.examYear)

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
fun TrendingNoteDiscoveryCardContentFromNote(
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
                val isTitleValid = note.title.isNotBlank() && note.title != "Untitled Document"
                val displayTitle = if (isTitleValid) note.title else note.subject
                Column {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 23.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isTitleValid && note.subject.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        SubjectBadge(subject = note.subject, isLarge = true, semester = note.semester)
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

                // Semester Information shifted to SubjectBadge

                // Bottom Row (Type Badge and Stats)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DocumentTypeBadge(type = note.documentType, year = note.examYear)

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
fun TrendingNoteDiscoveryCardContentFallback(
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
                val isTitleValid = note.title.isNotBlank() && note.title != "Untitled Document"
                val displayTitle = if (isTitleValid) note.title else note.subject
                Column {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 23.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isTitleValid && note.subject.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        SubjectBadge(subject = note.subject, isLarge = true, semester = note.semester)
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

                // Semester Information shifted to SubjectBadge

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DocumentTypeBadge(type = note.documentType, year = note.examYear)

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

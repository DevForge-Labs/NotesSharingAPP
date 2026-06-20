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
import androidx.compose.material.icons.filled.School
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
    isBookmarked: Boolean,
    onBookmarkClick: () -> Unit,
    onClick: () -> Unit,
    isUpvoted: Boolean = false,
    onUpvoteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val theme = com.pravor.notessharing.ui.components.getStudyResourceTheme(doc.documentType)
    val accentColor = theme.accentColor
    val cardBrush = theme.cardBrush

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.12f)),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Side: Portrait ratio thumbnail preview (visually resembling a document cover)
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                TrendingNoteThumbnail(doc = doc)
            }

            // Right Side Content Column (occupies remaining width)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Document Title (bold, max 2 lines)
                val isTitleValid = doc.title.isNotBlank() && doc.title != "Untitled Document"
                val displayTitle = if (isTitleValid) doc.title else doc.subject
                Column {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, lineHeight = 22.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    val badgeSubject = when {
                        !doc.displaySubject.isNullOrBlank() -> doc.displaySubject
                        doc.subject.isNotBlank() -> doc.subject
                        else -> "Unknown"
                    }
                    if (isTitleValid) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SubjectBadge(
                                subject = badgeSubject,
                                isLarge = true,
                                semester = doc.semester,
                                disableNormalization = !doc.displaySubject.isNullOrBlank()
                            )

                            val rawDocType = doc.documentType.lowercase(java.util.Locale.ROOT).trim()
                            val secDisp = doc.sectionDisplay?.trim() ?: ""
                            if ((rawDocType == "assignment" || rawDocType == "assignments") && secDisp.isNotBlank()) {
                                val normalizedSubject = remember(doc.subject) { com.pravor.notessharing.ui.components.utils.normalizeSubject(doc.subject) }
                                val subjectColor = remember(normalizedSubject) { com.pravor.notessharing.ui.components.utils.getSubjectColor(normalizedSubject) }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = subjectColor.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, subjectColor.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.School,
                                            contentDescription = null,
                                            tint = subjectColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = secDisp,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = subjectColor,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    DocumentTypeBadge(type = doc.documentType, year = doc.examYear)

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

                // Bottom Row (Stats aligned right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrendingNoteStats(
                        upvotes = doc.upvotes,
                        bookmarks = doc.bookmarks,
                        isBookmarked = isBookmarked,
                        onBookmarkClick = onBookmarkClick,
                        isUpvoted = isUpvoted,
                        onUpvoteClick = onUpvoteClick
                    )
                }
            }
        }
    }
}

@Composable
fun TrendingNoteDiscoveryCardContentFromNote(
    note: TrendingNote,
    isBookmarked: Boolean,
    onBookmarkClick: () -> Unit,
    onClick: () -> Unit,
    isUpvoted: Boolean = false,
    onUpvoteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val theme = com.pravor.notessharing.ui.components.getStudyResourceTheme(note.documentType)
    val accentColor = theme.accentColor
    val cardBrush = theme.cardBrush

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.12f)),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Side: Portrait ratio thumbnail preview (document cover layout)
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(130.dp)
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
                    .height(130.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Title (bold, max 2 lines)
                val isTitleValid = note.title.isNotBlank() && note.title != "Untitled Document"
                val displayTitle = if (isTitleValid) note.title else note.subject
                Column {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, lineHeight = 22.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val badgeSubject = when {
                        !note.displaySubject.isNullOrBlank() -> note.displaySubject
                        note.subject.isNotBlank() -> note.subject
                        else -> "Unknown"
                    }
                    if (isTitleValid) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SubjectBadge(
                                subject = badgeSubject,
                                isLarge = true,
                                semester = note.semester,
                                disableNormalization = !note.displaySubject.isNullOrBlank()
                            )

                            val rawDocType = note.documentType.lowercase(java.util.Locale.ROOT).trim()
                            val secDisp = note.sectionDisplay?.trim() ?: ""
                            if ((rawDocType == "assignment" || rawDocType == "assignments") && secDisp.isNotBlank()) {
                                val normalizedSubject = remember(note.subject) { com.pravor.notessharing.ui.components.utils.normalizeSubject(note.subject) }
                                val subjectColor = remember(normalizedSubject) { com.pravor.notessharing.ui.components.utils.getSubjectColor(normalizedSubject) }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = subjectColor.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, subjectColor.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.School,
                                            contentDescription = null,
                                            tint = subjectColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = secDisp,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = subjectColor,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    DocumentTypeBadge(type = note.documentType, year = note.examYear)
                }

                // Bottom Row (Type Badge and Stats)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrendingNoteStats(
                        upvotes = note.upvotes,
                        bookmarks = note.bookmarks,
                        isBookmarked = isBookmarked,
                        onBookmarkClick = onBookmarkClick,
                        isUpvoted = isUpvoted,
                        onUpvoteClick = onUpvoteClick
                    )
                }
            }
        }
    }
}

@Composable
fun TrendingNoteDiscoveryCardContentFallback(
    note: TrendingNote,
    isBookmarked: Boolean,
    onBookmarkClick: () -> Unit,
    onClick: () -> Unit,
    isUpvoted: Boolean = false,
    onUpvoteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val theme = com.pravor.notessharing.ui.components.getStudyResourceTheme(note.documentType)
    val accentColor = theme.accentColor
    val cardBrush = theme.cardBrush

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.12f)),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                FallbackThumbnailImage(note = note)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                val isTitleValid = note.title.isNotBlank() && note.title != "Untitled Document"
                val displayTitle = if (isTitleValid) note.title else note.subject
                Column {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, lineHeight = 22.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val badgeSubject = when {
                        !note.displaySubject.isNullOrBlank() -> note.displaySubject
                        note.subject.isNotBlank() -> note.subject
                        else -> "Unknown"
                    }
                    if (isTitleValid) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SubjectBadge(
                                subject = badgeSubject,
                                isLarge = true,
                                semester = note.semester,
                                disableNormalization = !note.displaySubject.isNullOrBlank()
                            )

                            val rawDocType = note.documentType.lowercase(java.util.Locale.ROOT).trim()
                            val secDisp = note.sectionDisplay?.trim() ?: ""
                            if ((rawDocType == "assignment" || rawDocType == "assignments") && secDisp.isNotBlank()) {
                                val normalizedSubject = remember(note.subject) { com.pravor.notessharing.ui.components.utils.normalizeSubject(note.subject) }
                                val subjectColor = remember(normalizedSubject) { com.pravor.notessharing.ui.components.utils.getSubjectColor(normalizedSubject) }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = subjectColor.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, subjectColor.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.School,
                                            contentDescription = null,
                                            tint = subjectColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = secDisp,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = subjectColor,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    DocumentTypeBadge(type = note.documentType, year = note.examYear)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrendingNoteStats(
                        upvotes = note.upvotes,
                        bookmarks = note.bookmarks,
                        isBookmarked = isBookmarked,
                        onBookmarkClick = onBookmarkClick,
                        isUpvoted = isUpvoted,
                        onUpvoteClick = onUpvoteClick
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

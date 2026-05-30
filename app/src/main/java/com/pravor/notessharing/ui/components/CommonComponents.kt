package com.pravor.notessharing.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.IconButton
import com.pravor.notessharing.model.Category
import com.pravor.notessharing.model.FeedItem
import com.pravor.notessharing.model.FileType
import com.pravor.notessharing.model.StudyFile
import com.pravor.notessharing.ui.theme.NotesSharingTheme

@Composable
fun NotesSearchBar(
    placeholder: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgBrush = if (isPressed) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1E2836), // harmonized ambient blue-teal top
                Color(0xFF141A23)  // bottom
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF141922), // 3-5% ambient teal/blue-tinted top edge
                Color(0xFF0D1016)  // deep expensive dark bottom
            )
        )
    }

    val borderTint by animateColorAsState(
        targetValue = if (isPressed) Color(0xFF58D6D1).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
        label = "search-border-tint"
    )

    val shadowElevation by animateDpAsState(
        targetValue = if (isPressed) 6.dp else 2.dp,
        label = "search-shadow"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null, // pure tactile custom highlight response
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, borderTint),
        color = Color.Transparent,
        shadowElevation = shadowElevation
    ) {
        Row(
            modifier = Modifier
                .background(bgBrush)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Soft icon container with subtle category tint
            Surface(
                shape = CircleShape,
                color = Color(0xFF58D6D1).copy(alpha = 0.08f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                           imageVector = Icons.Default.Search,
                           contentDescription = null,
                           tint = Color(0xFF58D6D1).copy(alpha = 0.85f),
                           modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.2.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    val accentColor = when {
        title.contains("Continue Reading", ignoreCase = true) -> Color(0xFF58D6D1)
        title.contains("For You", ignoreCase = true) -> Color(0xFFC7A6FF)
        title.contains("Study Hub", ignoreCase = true) -> Color(0xFFFFB45C)
        title.contains("Trending", ignoreCase = true) -> Color(0xFFFFB45C)
        title.contains("Video", ignoreCase = true) -> Color(0xFFFF6B6B)
        title.contains("Collection", ignoreCase = true) -> Color(0xFF7AD7FF)
        title.contains("Revision", ignoreCase = true) -> Color(0xFFC7A6FF)
        title.contains("Discover", ignoreCase = true) -> Color(0xFF58D6D1)
        else -> Color(0xFF94A3B8)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.15.sp,
                fontSize = 18.sp
            ),
            color = Color(0xFFE2E8F0)
        )
    }
}

@Composable
fun CategoryChip(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "category-background"
    )
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "category-content"
    )

    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = background,
        tonalElevation = if (selected) 8.dp else 1.dp
    ) {
        Text(
            text = category.label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            style = MaterialTheme.typography.labelLarge,
            color = content,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun FeedCard(
    item: FeedItem,
    onUpvoteClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(text = item.uploaderInitials, modifier = Modifier.size(34.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.uploaderName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                FileTypeBadge(item.fileType)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                StatItem(
                    icon = Icons.Default.ThumbUp,
                    value = item.upvotes.toString(),
                    modifier = Modifier.clickable(onClick = onUpvoteClick)
                )
                StatItem(Icons.Default.Download, item.downloads.toString())
            }
        }
    }
}

@Composable
fun StudyFileCard(
    file: StudyFile,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FilePresent,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = file.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${file.uploadDate} | ${file.fileType.label} | ${file.downloads} downloads | ${file.upvotes} upvotes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CompactStudyFileRow(
    file: StudyFile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FilePresent,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = file.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${file.uploadDate} | ${file.downloads} downloads",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ProfileStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatePanel(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    loading: Boolean = false
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (loading) {
                CircularProgressIndicator()
            } else {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun Avatar(text: String, modifier: Modifier = Modifier.size(48.dp)) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                ),
                shape = CircleShape
            )
            .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatItem(icon: ImageVector, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(17.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FileTypeBadge(fileType: FileType) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Text(
            text = fileType.label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview
@Composable
private fun SearchPreview() {
    NotesSharingTheme {
        NotesSearchBar("Search notes, subjects, PDFs...")
    }
}

data class UploadViewerData(
    val title: String,
    val fileUrls: List<String>
)

@Composable
fun GroupedUploadViewerDialog(
    title: String,
    fileUrls: List<String>,
    onDismiss: () -> Unit,
    onFileClick: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${fileUrls.size} " + if (fileUrls.size == 1) "attachment" else "attachments",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // File List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(fileUrls) { url ->
                        val fileName = getFileNameFromUrl(url)
                        val icon = when {
                            url.contains(".pdf", ignoreCase = true) || url.contains("dummy.pdf") -> Icons.Default.FilePresent
                            url.contains("youtube.com", ignoreCase = true) || url.contains("youtu.be", ignoreCase = true) -> Icons.Default.Link
                            url.contains(".jpg", ignoreCase = true) || url.contains(".jpeg", ignoreCase = true) || 
                            url.contains(".png", ignoreCase = true) || url.contains(".webp", ignoreCase = true) ||
                            url.contains("unsplash.com") -> Icons.Default.Image
                            else -> Icons.Default.FilePresent
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFileClick(url) },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            tonalElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = fileName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val typeText = when {
                                        url.contains(".pdf", ignoreCase = true) || url.contains("dummy.pdf") -> "PDF Document"
                                        url.contains("youtube.com", ignoreCase = true) || url.contains("youtu.be", ignoreCase = true) -> "YouTube Link"
                                        url.contains(".jpg", ignoreCase = true) || url.contains(".jpeg", ignoreCase = true) || 
                                        url.contains(".png", ignoreCase = true) || url.contains(".webp", ignoreCase = true) ||
                                        url.contains("unsplash.com") -> "Image File"
                                        else -> "Attachment"
                                    }
                                    Text(
                                        text = typeText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Open or Download File",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getFileNameFromUrl(url: String): String {
    return try {
        val decoded = java.net.URLDecoder.decode(url, "UTF-8")
        val path = decoded.substringBefore("?").substringAfterLast("/")
        val name = path.ifBlank { "Attachment" }
        if (name.contains("%")) {
            name.substringAfterLast("%")
        } else {
            name
        }
    } catch (e: Exception) {
        "Attachment"
    }
}

@Composable
fun StudyHubShelfCard(
    file: StudyFile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVideo = file.fileType == FileType.Video
    val docType = file.documentType ?: file.fileType.label
    val rawDocType = docType.lowercase(java.util.Locale.ROOT).trim()

    val isPyq = rawDocType.contains("pyq")
    val isCheatSheet = rawDocType.contains("cheat") || rawDocType.contains("formula")
    val isAssignment = rawDocType.contains("assignment")
    val isNotes = rawDocType.contains("notes")

    val docTypeStr = when {
        isVideo -> "YouTube"
        isPyq -> "PYQ"
        isAssignment -> "Assignment"
        isCheatSheet -> "Cheat Sheet"
        isNotes -> "Notes"
        else -> "PDF"
    }

    val accentColor = when {
        isVideo -> Color(0xFFFF6B6B)
        isNotes -> Color(0xFF58D6D1)
        isPyq -> Color(0xFFFFB45C)
        isAssignment -> Color(0xFF7AD7FF)
        isCheatSheet -> Color(0xFFC7A6FF)
        else -> Color(0xFFCFD8DC)
    }

    val cardBrush = when {
        isVideo -> Brush.verticalGradient(listOf(Color(0xFF231A1B), Color(0xFF130E0F)))
        isNotes -> Brush.verticalGradient(listOf(Color(0xFF13201F), Color(0xFF0C1312)))
        isPyq -> Brush.verticalGradient(listOf(Color(0xFF241C15), Color(0xFF16110D)))
        isCheatSheet -> Brush.verticalGradient(listOf(Color(0xFF1E1724), Color(0xFF120E16)))
        isAssignment -> Brush.verticalGradient(listOf(Color(0xFF141F23), Color(0xFF0C1316)))
        else -> Brush.verticalGradient(listOf(Color(0xFF1D2124), Color(0xFF111315)))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.12f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Left Thumbnail Area
            Box(
                modifier = Modifier
                    .width(108.dp)
                    .height(76.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                var hasImageLoaded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                var imageLoadError by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

                if (!file.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = file.thumbnailUrl,
                        contentDescription = file.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        onSuccess = { hasImageLoaded = true },
                        onError = { imageLoadError = true }
                    )
                }

                val showFallback = (file.thumbnailUrl.isNullOrBlank() || imageLoadError) && !hasImageLoaded
                if (showFallback) {
                    DocumentPlaceholder(documentType = docTypeStr, modifier = Modifier.fillMaxSize())
                }
            }

            Spacer(Modifier.width(14.dp))

            // 2. Right Content Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                // Chips Row: Subject + Category
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Subject Chip
                    val subjectText = file.subject?.trim()?.ifBlank { null } ?: "General"
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accentColor.copy(alpha = 0.06f),
                        border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = subjectText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = accentColor.copy(alpha = 0.85f),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Category Chip
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accentColor.copy(alpha = 0.12f),
                        border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = docTypeStr.uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Upload Date
                Text(
                    text = file.uploadDate,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                // Downloads & Likes Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                            text = file.downloads.toString(),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF64B5F6).copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = null,
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = file.upvotes.toString(),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFFFFB74D).copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

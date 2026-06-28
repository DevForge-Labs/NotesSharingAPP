package com.pravor.notessharing.ui.screens.search.search_components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.graphics.Brush
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
import com.pravor.notessharing.ui.components.DocumentPlaceholder
import com.pravor.notessharing.ui.components.getStudyResourceTheme
import com.pravor.notessharing.ui.screens.search.SearchResultModel

@Composable
fun SearchResultCard(
    result: SearchResultModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = result.type.equals("User", ignoreCase = true)
    
    val normalizedType: String = remember(result.documentType, result.type) {
        if (isUser) {
            "User"
        } else {
            val typeSource = if (result.documentType.isNotBlank()) result.documentType else result.type
            val rawType = typeSource.lowercase(java.util.Locale.ROOT).trim()
            when {
                rawType.contains("pyq") -> "PYQ"
                rawType.contains("assignment") -> "Assignment"
                rawType.contains("cheat") || rawType.contains("formula") -> "Cheat Sheet"
                rawType.contains("notes") || rawType.contains("note") -> "Notes"
                rawType.contains("playlist") -> "Playlist"
                rawType.contains("video") || rawType.contains("youtube") -> "Video"
                else -> typeSource
            }
        }
    }

    val accentColor = remember(normalizedType) {
        if (isUser) {
            Color(0xFFC7A6FF)
        } else {
            getStudyResourceTheme(normalizedType).accentColor
        }
    }

    val displayTitle = remember(result, normalizedType) {
        val cleanTitleWithoutExt = result.title.replace(Regex("\\.(pdf|jpg|jpeg|png|webp|docx|txt|html|zip|rar|mp4|mkv|avi|mov)$", RegexOption.IGNORE_CASE), "").trim()
        when {
            normalizedType == "PYQ" -> {
                val normalizedExamType = when {
                    result.examType.contains("mid", ignoreCase = true) -> "Mid Semester"
                    result.examType.contains("end", ignoreCase = true) -> "End Semester"
                    else -> result.examType
                }
                val parts = mutableListOf<String>()
                if (result.subject.isNotBlank() && !result.subject.equals("Unknown", ignoreCase = true)) {
                    parts.add(result.subject)
                }
                if (normalizedExamType.isNotBlank()) {
                    parts.add(normalizedExamType)
                }
                if (result.examYear.isNotBlank()) {
                    parts.add(result.examYear)
                }
                if (parts.isNotEmpty()) parts.joinToString(" ") else cleanTitleWithoutExt
            }
            normalizedType == "Playlist" && result.playlistTitle.isNotBlank() -> {
                result.playlistTitle
            }
            else -> {
                cleanTitleWithoutExt
            }
        }
    }

    var isImageSuccessfullyLoaded by remember(result.thumbnailUrl) { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(
            width = 1.dp,
            color = accentColor.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Adaptive Thumbnail / Avatar Container
            Box(
                modifier = if (isUser) {
                    Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.1f))
                } else {
                    Modifier
                        .fillMaxHeight()
                        .aspectRatio(1.42f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                },
                contentAlignment = Alignment.Center
            ) {
                if (isUser) {
                    if (result.thumbnailUrl.isNotBlank()) {
                        var hasError by remember(result.thumbnailUrl) { mutableStateOf(false) }
                        if (!hasError) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(result.thumbnailUrl)
                                    .crossfade(true)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .build(),
                                contentDescription = displayTitle,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                onSuccess = { isImageSuccessfullyLoaded = true },
                                onError = { hasError = true }
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = result.type,
                                tint = accentColor,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = result.type,
                            tint = accentColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else {
                    if (result.thumbnailUrl.isNotBlank()) {
                        var hasError by remember(result.thumbnailUrl) { mutableStateOf(false) }
                        if (!hasError) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(result.thumbnailUrl)
                                    .crossfade(true)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .build(),
                                contentDescription = displayTitle,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                onSuccess = { isImageSuccessfullyLoaded = true },
                                onError = { hasError = true }
                            )
                        } else {
                            DocumentPlaceholder(
                                documentType = normalizedType,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        DocumentPlaceholder(
                            documentType = normalizedType,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (isImageSuccessfullyLoaded) {
                        // Subtle vertical black gradient overlay on the bottom 25% of the thumbnail
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .fillMaxHeight(0.25f)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))
                                    )
                                )
                        )
                        // Compact resource type chip aligned at the bottom-right corner
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.6f),
                            border = BorderStroke(0.75.dp, accentColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = normalizedType,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = accentColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Info Details Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = displayTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Subtitle (subject) and channel name
                if (isUser) {
                    if (result.subtitle.isNotBlank()) {
                        Text(
                            text = result.subtitle,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    val displaySubject = if (result.subject.isNotBlank()) result.subject else result.subtitle
                    if (displaySubject.isNotBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = displaySubject,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if ((normalizedType == "Video" || normalizedType == "Playlist") && result.channelName.isNotBlank()) {
                                Text(
                                    text = result.channelName,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Metadata Badges Row
                if (!isUser) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        // Resource Type Badge (shown as fallback in details section only when thumbnail is not loaded)
                        if (!isImageSuccessfullyLoaded) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = accentColor.copy(alpha = 0.1f),
                                border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = normalizedType,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Assignment Section Badge
                        if (normalizedType == "Assignment" && result.sectionDisplay.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = result.sectionDisplay,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // PYQ Year & Exam Type Badges
                        if (normalizedType == "PYQ") {
                            if (result.examYear.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = result.examYear,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (result.examType.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = result.examType,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

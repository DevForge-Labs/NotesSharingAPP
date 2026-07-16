package com.pravor.notessharing.ui.screens.document

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.pravor.notessharing.ui.components.DocumentPlaceholder

@Composable
fun AttachmentPreviewCard(
    url: String,
    fileSize: Long,
    onDownloadClick: () -> Unit,
    onShareClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnailUrl: String? = null,
    documentType: String? = null,
    examYear: String? = null,
    examType: String? = null,
    isSingleAttachment: Boolean = false
) {
    val isPdf = url.contains(".pdf", ignoreCase = true) || url.contains("dummy.pdf")
    val isPyq = documentType?.lowercase(java.util.Locale.ROOT)?.contains("pyq") == true
    
    val fileName = getFileName(url)
    val fileType = if (isPdf) "PDF Document" else "Document File"
 
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFFFB74D).copy(alpha = 0.25f)), // Soft glowing amber border accent
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        if (isSingleAttachment) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF161925), // Deep navy-indigo tinted dark surface
                                Color(0xFF0E1017)
                            )
                        )
                    )
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                // 1. Full-bleed background thumbnail (using "cover" style fit with top alignment)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    var isImageLoaded by remember { mutableStateOf(false) }
                    var hasError by remember { mutableStateOf(false) }

                    if (!thumbnailUrl.isNullOrBlank() && !hasError) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(thumbnailUrl)
                                .crossfade(true)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build(),
                            contentDescription = fileName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop, // "cover" style fit
                            alignment = Alignment.TopCenter, // bias crop to the top
                            onSuccess = { isImageLoaded = true },
                            onError = { hasError = true }
                        )
                        
                        if (!isImageLoaded && !hasError) {
                            ShimmerPlaceholder()
                        }
                    }

                    if (thumbnailUrl.isNullOrBlank() || hasError) {
                        DocumentPlaceholder(
                            documentType = documentType ?: "Notes",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // 2. Share button floating at the top-right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    GlassIconButton(
                        icon = Icons.Default.Share,
                        contentDescription = "Share Attachment",
                        onClick = onShareClick
                    )
                }

                // 3. Subtle dark gradient overlay at the bottom of the card
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.45f)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Black.copy(alpha = 0.9f)
                                )
                            )
                        )
                )

                // 4. Filename + info overlaid at the bottom
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Text(
                        text = "$fileType • ${formatFileSize(fileSize)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF161925), // Deep navy-indigo tinted dark surface
                                Color(0xFF0E1017)
                            )
                        )
                    )
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. LEFT SIDE: Large Thumbnail Card
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    var isImageLoaded by remember { mutableStateOf(false) }
                    var hasError by remember { mutableStateOf(false) }

                    if (!thumbnailUrl.isNullOrBlank() && !hasError) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(thumbnailUrl)
                                .crossfade(true)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build(),
                            contentDescription = fileName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            onSuccess = { isImageLoaded = true },
                            onError = { hasError = true }
                        )
                        
                        if (!isImageLoaded && !hasError) {
                            ShimmerPlaceholder()
                        }
                    }

                    if (thumbnailUrl.isNullOrBlank() || hasError) {
                        DocumentPlaceholder(
                            documentType = documentType ?: "Notes",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 2. RIGHT SIDE: Info Column + Actions
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp), // Align height with the left thumbnail
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$fileType • ${formatFileSize(fileSize)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassIconButton(
                            icon = Icons.Default.Share,
                            contentDescription = "Share Attachment",
                            onClick = onShareClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShimmerPlaceholder(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "attachment-shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "attachment-shimmer-translate"
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

@Composable
fun GenericPreviewCard(
    url: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF455A64),
                        Color(0xFF1A237E)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FilePresent,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "FILE",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        modifier = modifier.size(42.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color(0xFFFFB74D), // Muted Amber Accent
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun getFileName(url: String): String {
    return try {
        val decoded = java.net.URLDecoder.decode(url, "UTF-8")
        val path = decoded.substringBefore("?").substringAfterLast("/")
        val name = path.ifBlank { "Attachment File" }
        if (name.contains("%")) {
            name.substringAfterLast("%")
        } else {
            name
        }
    } catch (e: Exception) {
        "Attachment File"
    }
}

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.toDouble())).toInt()
    return String.format("%.1f %s", size / Math.pow(1024.toDouble(), digitGroups.toDouble()), units[digitGroups])
}

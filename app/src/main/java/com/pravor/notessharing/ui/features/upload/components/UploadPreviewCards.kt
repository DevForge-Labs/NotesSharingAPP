package com.pravor.notessharing.ui.features.upload.components

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pravor.notessharing.domain.model.SelectedUploadFile
import com.pravor.notessharing.ui.common.VideoPlaceholder
import com.pravor.notessharing.ui.features.upload.YoutubePreview

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) String.format("%.1f MB", mb) else String.format("%.0f KB", kb)
}

@Composable
fun PdfPreviewCard(
    file: SelectedUploadFile,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(file.displayName, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatBytes(file.sizeBytes), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(8.dp))
                    Text("• PDF Document", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove PDF")
            }
        }
    }
}

@Composable
fun ImagePreviewSection(
    files: List<SelectedUploadFile>,
    onRemoveFile: (SelectedUploadFile) -> Unit
) {
    if (files.isEmpty()) return
    
    if (files.size == 1) {
        SingleImagePreviewCard(file = files[0], onRemove = { onRemoveFile(files[0]) })
    } else {
        ImageGridPreviewLayout(files = files, onRemoveFile = onRemoveFile)
    }
}

@Composable
fun SingleImagePreviewCard(
    file: SelectedUploadFile,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val imageInfo = remember(file.uri) {
        runCatching {
            val uri = Uri.parse(file.uri)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(stream, null, options)
                Pair(options.outWidth, options.outHeight)
            }
        }.getOrNull()
    }
    val dimensions = imageInfo?.let { "${it.first} x ${it.second}" }
    
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                val bitmap = remember(file.uri, imageInfo) {
                    runCatching {
                        val uri = Uri.parse(file.uri)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            context.contentResolver.loadThumbnail(uri, android.util.Size(640, 360), null)
                        } else {
                            val sampleSize = imageInfo?.let { (w, h) ->
                                var sample = 1
                                while (w / (sample * 2) >= 640 && h / (sample * 2) >= 360) {
                                    sample *= 2
                                }
                                sample
                            } ?: 1
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                val decodeOptions = BitmapFactory.Options().apply {
                                    inSampleSize = sampleSize
                                }
                                BitmapFactory.decodeStream(stream, null, decodeOptions)
                            }
                        }
                    }.getOrNull()
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = file.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center).size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Remove image", modifier = Modifier.size(18.dp))
                }
            }
            
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = file.displayName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatBytes(file.sizeBytes),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (dimensions != null) {
                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = dimensions,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImageGridPreviewLayout(
    files: List<SelectedUploadFile>,
    onRemoveFile: (SelectedUploadFile) -> Unit
) {
    val displayFiles = files.take(4)
    val remainingCount = if (files.size > 4) files.size - 4 else 0
    
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        val chunked = displayFiles.chunked(2)
        chunked.forEachIndexed { rowIndex, rowFiles ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                rowFiles.forEachIndexed { colIndex, file ->
                    val isLastItem = (rowIndex == 1 && colIndex == 1) || (files.size == 2 && rowIndex == 0 && colIndex == 1) || (files.size == 3 && rowIndex == 1 && colIndex == 0)
                    val showOverlay = isLastItem && remainingCount > 0
                    
                    Box(modifier = Modifier.weight(1f)) {
                        GridImagePreviewCard(
                            file = file,
                            showOverlay = showOverlay,
                            remainingCount = remainingCount,
                            onRemove = { onRemoveFile(file) }
                        )
                    }
                }
                if (rowFiles.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun GridImagePreviewCard(
    file: SelectedUploadFile,
    showOverlay: Boolean,
    remainingCount: Int,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val imageInfo = remember(file.uri) {
        runCatching {
            val uri = Uri.parse(file.uri)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(stream, null, options)
                Pair(options.outWidth, options.outHeight)
            }
        }.getOrNull()
    }
    val dimensions = imageInfo?.let { "${it.first}x${it.second}" }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                val bitmap = remember(file.uri, imageInfo) {
                    runCatching {
                        val uri = Uri.parse(file.uri)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            context.contentResolver.loadThumbnail(uri, android.util.Size(320, 240), null)
                        } else {
                            val sampleSize = imageInfo?.let { (w, h) ->
                                var sample = 1
                                while (w / (sample * 2) >= 320 && h / (sample * 2) >= 240) {
                                    sample *= 2
                                }
                                sample
                            } ?: 1
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                val decodeOptions = BitmapFactory.Options().apply {
                                    inSampleSize = sampleSize
                                }
                                BitmapFactory.decodeStream(stream, null, decodeOptions)
                            }
                        }
                    }.getOrNull()
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = file.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (showOverlay) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+$remainingCount",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f), RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Remove image", modifier = Modifier.size(16.dp))
                }
            }
            
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = file.displayName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatBytes(file.sizeBytes),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (dimensions != null) {
                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = dimensions,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun YoutubePreviewCard(
    preview: YoutubePreview?,
    isFetching: Boolean,
    error: String?
) {
    if (preview == null && !isFetching && error == null) {
        return
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(14.dp)) {
            if (isFetching) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().height(74.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Loading video details...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (error != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (preview != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 112.dp, height = 74.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        var hasError by remember(preview.thumbnailUrl) { mutableStateOf(false) }
                        if (preview.thumbnailUrl.isNotBlank() && !hasError) {
                            AsyncImage(
                                model = preview.thumbnailUrl,
                                contentDescription = "Video Thumbnail",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                onError = { hasError = true }
                            )
                        } else {
                            VideoPlaceholder(modifier = Modifier.fillMaxSize())
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = preview.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = preview.channelTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyPreviewCard(
    title: String,
    subtitle: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

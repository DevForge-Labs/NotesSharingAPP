package com.pravor.notessharing.ui.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.net.URLDecoder

data class UploadViewerData(
    val id: String = "",
    val title: String,
    val fileUrls: List<String>
)

fun getFileNameFromUrl(url: String): String {
    return try {
        val decoded = URLDecoder.decode(url, "UTF-8")
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

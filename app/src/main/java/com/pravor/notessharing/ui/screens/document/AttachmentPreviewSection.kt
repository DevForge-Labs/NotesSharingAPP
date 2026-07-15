package com.pravor.notessharing.ui.screens.document

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.rememberLazyListState
import com.pravor.notessharing.ui.components.explore_components.RunningSquirrelScrollbar
import com.pravor.notessharing.model.DocumentDetail

@Composable
fun AttachmentPreviewSection(
    doc: DocumentDetail,
    onDownloadClick: (String) -> Unit,
    onShareClick: (String) -> Unit,
    onAttachmentClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val urls = doc.fileUrls

    if (urls.isEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FilePresent,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No preview available for this document",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    } else {
        // Filter into images and non-images (PDFs/Documents)
        val imageUrls = urls.filter { url ->
            url.contains(".jpg", ignoreCase = true) || url.contains(".jpeg", ignoreCase = true) ||
                    url.contains(".png", ignoreCase = true) || url.contains(".webp", ignoreCase = true) ||
                    url.contains("unsplash.com", ignoreCase = true)
        }
        val pdfUrls = urls.filter { !imageUrls.contains(it) }

        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. IMAGE GALLERY SHOWCASE
            if (imageUrls.isNotEmpty()) {
                if (imageUrls.size == 1) {
                    val url = imageUrls.first()
                    SingleImagePreviewCard(
                        url = url,
                        fileSize = doc.fileSize,
                        onDownloadClick = { onDownloadClick(url) },
                        onShareClick = { onShareClick(url) },
                        onClick = { onAttachmentClick(url) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                } else {
                    val listState = rememberLazyListState()
                    Box(modifier = Modifier.fillMaxWidth()) {
                        LazyRow(
                            state = listState,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            items(imageUrls) { url ->
                                val index = urls.indexOf(url)
                                val baseSize = doc.fileSize / urls.size
                                val indSize = computeDeterministicSize(baseSize, index)

                                ImagePreviewCard(
                                    url = url,
                                    fileSize = indSize,
                                    onDownloadClick = { onDownloadClick(url) },
                                    onShareClick = { onShareClick(url) },
                                    onClick = { onAttachmentClick(url) },
                                    modifier = Modifier
                                        .width(280.dp)
                                        .height(380.dp)
                                )
                            }
                        }
                        RunningSquirrelScrollbar(listState = listState)
                    }
                }
            }

            // 2. PDF & DOCUMENT VERTICAL COLUMN
            if (pdfUrls.isNotEmpty()) {
                val horizontalPadding = if (urls.size == 1) 28.dp else 16.dp
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    pdfUrls.forEach { url ->
                        val index = urls.indexOf(url)
                        val baseSize = doc.fileSize / urls.size
                        val indSize = computeDeterministicSize(baseSize, index)

                        AttachmentPreviewCard(
                            url = url,
                            fileSize = indSize,
                            thumbnailUrl = doc.thumbnailUrls.getOrNull(index),
                            documentType = doc.documentType,
                            examYear = doc.examYear,
                            examType = doc.examType,
                            onDownloadClick = { onDownloadClick(url) },
                            onShareClick = { onShareClick(url) },
                            onClick = { onAttachmentClick(url) },
                            isSingleAttachment = urls.size == 1
                        )
                    }
                }
            }
        }
    }
}

private fun computeDeterministicSize(baseSize: Long, index: Int): Long {
    val offset = when (index) {
        0 -> 1024 * 150L
        1 -> -1024 * 100L
        else -> 0L
    }
    return (baseSize + offset).coerceAtLeast(1024 * 10L)
}

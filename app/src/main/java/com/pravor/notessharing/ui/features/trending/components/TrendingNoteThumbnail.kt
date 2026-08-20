package com.pravor.notessharing.ui.features.trending.components

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.pravor.notessharing.domain.model.DocumentDetail
import com.pravor.notessharing.ui.common.DocumentPlaceholder

@Composable
fun TrendingNoteThumbnail(
    doc: DocumentDetail,
    modifier: Modifier = Modifier
) {
    val previewUrl = remember(doc) {
        if (doc.fileType == "image" || doc.fileExtension.lowercase(java.util.Locale.ROOT) in listOf("jpg", "jpeg", "png", "webp", "gif")) {
            doc.fileUrls.firstOrNull()
        } else {
            doc.thumbnailUrls.firstOrNull() ?: doc.thumbnailUrl
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (!previewUrl.isNullOrBlank()) {
            var hasError by remember { mutableStateOf(false) }
            if (!hasError) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(previewUrl)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .networkCachePolicy(CachePolicy.ENABLED)
                        .crossfade(true)
                        .build(),
                    contentDescription = doc.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onError = { hasError = true }
                )
            } else {
                DocumentPlaceholder(documentType = doc.documentType, modifier = Modifier.fillMaxSize())
            }
        } else {
            DocumentPlaceholder(documentType = doc.documentType, modifier = Modifier.fillMaxSize())
        }
    }
}


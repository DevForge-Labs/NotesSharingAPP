package com.pravor.notessharing.ui.features.document

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pravor.notessharing.ui.common.Avatar

@Composable
fun UploaderInfoCard(
    uploaderName: String,
    uploaderPhotoUrl: String,
    contributorLevel: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        val initials = if (uploaderName.isNotBlank()) {
            uploaderName.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .map { it.first().uppercase() }
                .joinToString("")
                .ifBlank { "UN" }
        } else {
            "UN"
        }

        var isImageError by remember { mutableStateOf(uploaderPhotoUrl.isBlank()) }

        if (!isImageError) {
            AsyncImage(
                model = uploaderPhotoUrl,
                contentDescription = "Uploader Profile",
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentScale = ContentScale.Crop,
                onError = { isImageError = true }
            )
        } else {
            Avatar(text = initials, modifier = Modifier.size(42.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = "Uploaded by: $uploaderName",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))

            val badgeColor = when (contributorLevel) {
                "Gold Contributor" -> Color(0xFFFFD700)
                "Silver Contributor" -> Color(0xFFC0C0C0)
                "Bronze Contributor" -> Color(0xFFCD7F32)
                "Platinum Contributor" -> Color(0xFF00E5FF)
                else -> Color(0xFFD500F9)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = contributorLevel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

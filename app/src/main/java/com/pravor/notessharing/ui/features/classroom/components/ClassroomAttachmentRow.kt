package com.pravor.notessharing.ui.features.classroom.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.pravor.notessharing.domain.model.classroom.AttachmentType
import com.pravor.notessharing.domain.model.classroom.ClassroomAttachment
import com.pravor.notessharing.ui.theme.ElectricBlue
import com.pravor.notessharing.ui.theme.Mint

@Composable
fun ClassroomAttachmentRow(
    attachment: ClassroomAttachment,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val meta = resolveAttachmentMeta(attachment)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141A22)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Thumbnail Preview / Fallback File Type Badge
            AttachmentVisual(
                thumbnailUrl = attachment.thumbnailUrl,
                meta = meta
            )

            // 2. Title & File Type Label
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = attachment.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 19.sp
                )
                Text(
                    text = meta.displayType,
                    style = MaterialTheme.typography.bodySmall,
                    color = meta.accentColor.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium
                )
            }

            // 3. Subtle Trailing Chevron
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open resource",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AttachmentVisual(
    thumbnailUrl: String?,
    meta: AttachmentMeta
) {
    val shape = RoundedCornerShape(10.dp)

    Surface(
        shape = shape,
        color = meta.accentColor.copy(alpha = 0.12f),
        border = BorderStroke(0.5.dp, meta.accentColor.copy(alpha = 0.25f)),
        modifier = Modifier.size(48.dp)
    ) {
        if (!thumbnailUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape),
                contentScale = ContentScale.Crop,
                loading = {
                    FallbackIconBadge(meta)
                },
                error = {
                    FallbackIconBadge(meta)
                }
            )
        } else {
            FallbackIconBadge(meta)
        }
    }
}

@Composable
private fun FallbackIconBadge(meta: AttachmentMeta) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = meta.icon,
            contentDescription = null,
            tint = meta.accentColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

private data class AttachmentMeta(
    val displayType: String,
    val icon: ImageVector,
    val accentColor: Color
)

private fun resolveAttachmentMeta(attachment: ClassroomAttachment): AttachmentMeta {
    val titleLower = attachment.title.lowercase()
    val urlLower = attachment.linkUrl.lowercase()

    return when {
        attachment.type == AttachmentType.FORM || urlLower.contains("docs.google.com/forms") || urlLower.contains("forms.gle") || urlLower.contains("forms.google.com") -> {
            AttachmentMeta("Google Form", Icons.Default.Description, Color(0xFF9C51B6))
        }
        attachment.type == AttachmentType.YOUTUBE || urlLower.contains("youtube.com") || urlLower.contains("youtu.be") -> {
            AttachmentMeta("YouTube Video", Icons.Default.PlayCircleOutline, Color(0xFFFF5252))
        }
        attachment.type == AttachmentType.LINK -> {
            AttachmentMeta("Web Link", Icons.Default.Link, Mint)
        }
        titleLower.endsWith(".pdf") || urlLower.contains(".pdf") -> {
            AttachmentMeta("PDF Document", Icons.Default.PictureAsPdf, Color(0xFFFF6B6B))
        }
        titleLower.endsWith(".pptx") || titleLower.endsWith(".ppt") || urlLower.contains("presentation") -> {
            AttachmentMeta("PowerPoint Presentation", Icons.Default.Slideshow, Color(0xFFFF9E43))
        }
        titleLower.endsWith(".docx") || titleLower.endsWith(".doc") || urlLower.contains("document") -> {
            AttachmentMeta("Word Document", Icons.Default.Description, Color(0xFF4DA3FF))
        }
        titleLower.endsWith(".xlsx") || titleLower.endsWith(".xls") || titleLower.endsWith(".csv") || urlLower.contains("spreadsheets") -> {
            AttachmentMeta("Excel Spreadsheet", Icons.Default.TableChart, Color(0xFF4EBA6F))
        }
        titleLower.endsWith(".png") || titleLower.endsWith(".jpg") || titleLower.endsWith(".jpeg") || titleLower.endsWith(".webp") -> {
            AttachmentMeta("Image File", Icons.Default.Description, Color(0xFFAB47BC))
        }
        else -> {
            AttachmentMeta("Google Drive File", Icons.AutoMirrored.Filled.InsertDriveFile, ElectricBlue)
        }
    }
}

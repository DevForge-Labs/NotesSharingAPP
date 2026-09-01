package com.pravor.notessharing.ui.common.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pravor.notessharing.domain.model.Category

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    accentColor: Color? = null,
    onSeeMoreClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val resolvedAccentColor = accentColor ?: when {
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
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (icon != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = iconTint.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, iconTint.copy(alpha = 0.35f)),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = resolvedAccentColor.copy(alpha = 0.85f),
                    modifier = Modifier
                        .width(4.dp)
                        .height(18.dp)
                ) {}
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = 0.2.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onSeeMoreClick != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onSeeMoreClick)
                ) {
                    Text(
                        text = "See All",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
            trailingContent?.invoke()
        }
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

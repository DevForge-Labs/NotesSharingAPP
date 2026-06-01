package com.pravor.notessharing.ui.components.explore_components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Download
import androidx.compose.ui.graphics.Color

@Composable
fun SmallMetric(icon: ImageVector, value: String) {
    val resolvedTint = when (icon) {
        androidx.compose.material.icons.Icons.Default.ThumbUp,
        androidx.compose.material.icons.Icons.Filled.ThumbUp -> Color(0xFFFFB74D)
        androidx.compose.material.icons.Icons.Default.Download,
        androidx.compose.material.icons.Icons.Filled.Download -> Color(0xFF64B5F6)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = resolvedTint)
        Spacer(Modifier.width(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = resolvedTint.copy(alpha = 0.9f),
            fontWeight = FontWeight.SemiBold
        )
    }
}

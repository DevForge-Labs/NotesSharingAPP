package com.pravor.notessharing.ui.components.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.pravor.notessharing.ui.navigation.AppDestination

@Composable
fun WaterNavItem(
    destination: AppDestination,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Theme-aware active and submerged colors in neon bluish palette
    val targetColor = if (isSelected) {
        Color.White // Active tab: Solid White
    } else {
        if (isDark) {
            Color(0xFFB2F0FF).copy(alpha = 0.75f) // Inactive tab in dark mode: Soft Bright Neon Cyan
        } else {
            Color(0xFF0044AA).copy(alpha = 0.80f) // Inactive tab in light mode: Vivid Neon Blue
        }
    }

    val iconColor by animateColorAsState(
        targetValue = targetColor,
        label = "water-nav-icon-color"
    )

    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = CircleShape,
        modifier = modifier.fillMaxHeight()
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = destination.label,
                modifier = Modifier.size(24.dp),
                tint = iconColor
            )
        }
    }
}

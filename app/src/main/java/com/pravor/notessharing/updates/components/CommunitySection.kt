package com.pravor.notessharing.updates.components

import com.pravor.notessharing.domain.model.UpdatePageModel
import com.pravor.notessharing.domain.model.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.pravor.notessharing.ui.theme.*

@Composable
fun RippleCommunityVisual(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "card_rotation_transition")

    // Slow 9-second rotation cycle (3 seconds per card shift)
    val rotationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationProgress"
    )

    val entryProgress by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "entryProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .graphicsLayer {
                alpha = entryProgress
                scaleX = 0.9f + 0.1f * entryProgress
                scaleY = 0.9f + 0.1f * entryProgress
            },
        contentAlignment = Alignment.Center
    ) {
        val cardData = listOf(
            Triple("Built By Students", "Tailored specifically to tackle university exams, semester prep, and labs.", Icons.Filled.School to ElectricBlue),
            Triple("Community Driven", "Fueled by shared notes, assignments, and study materials from fellow peers.", Icons.Filled.Group to Mint),
            Triple("Made For Learning", "Structured to give you instant access to search, cheat sheets, and videos.", Icons.Filled.AutoAwesome to Coral)
        )

        for (i in 0..2) {
            var localProgress = rotationProgress - i
            if (localProgress < 0f) { localProgress += 3f }

            // Define the 3 state layouts in a vertical stack:
            // State 0: Slot 0 (Top card - small, background)
            // State 1: Slot 1 (Middle card - medium, middleground)
            // State 2: Slot 2 (Bottom card - large, foreground)

            val scale: Float
            val alpha: Float
            val offsetY: androidx.compose.ui.unit.Dp
            val zIndexVal: Float

            if (localProgress < 1f) {
                // Transitioning from Slot 0 (Top) to Slot 1 (Middle) - moving down
                val t = localProgress
                val easedT = FastOutSlowInEasing.transform(t)
                scale = lerp(0.85f, 0.92f, easedT)
                alpha = lerp(0.65f, 0.85f, easedT)
                offsetY = lerpDp((-40).dp, 0.dp, easedT)
                zIndexVal = if (t < 0.5f) 0f else 1f
            } else if (localProgress < 2f) {
                // Transitioning from Slot 1 (Middle) to Slot 2 (Bottom) - moving down
                val t = localProgress - 1f
                val easedT = FastOutSlowInEasing.transform(t)
                scale = lerp(0.92f, 1.0f, easedT)
                alpha = lerp(0.85f, 1.0f, easedT)
                offsetY = lerpDp(0.dp, 40.dp, easedT)
                zIndexVal = if (t < 0.5f) 1f else 2f
            } else {
                // Transitioning from Slot 2 (Bottom) back to Slot 0 (Top) - rising upward
                val t = localProgress - 2f
                val easedT = FastOutSlowInEasing.transform(t)
                scale = lerp(1.0f, 0.85f, easedT)
                alpha = lerp(1.0f, 0.65f, easedT)
                offsetY = lerpDp(40.dp, (-40).dp, easedT)
                zIndexVal = if (t < 0.5f) 2f else 0f
            }

            val (title, description, iconColorPair) = cardData[i]
            val (icon, color) = iconColorPair

            RotatingCard(
                title = title,
                description = description,
                icon = icon,
                color = color,
                scale = scale,
                alpha = alpha,
                modifier = Modifier
                    .offset(y = offsetY)
                    .zIndex(zIndexVal)
            )
        }
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + fraction * (stop - start)
}

private fun lerpDp(start: androidx.compose.ui.unit.Dp, stop: androidx.compose.ui.unit.Dp, fraction: Float): androidx.compose.ui.unit.Dp {
    return start + (stop - start) * fraction
}

@Composable
fun RotatingCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    scale: Float,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .graphicsLayer {
                this.scaleX = scale
                this.scaleY = scale
                this.alpha = alpha
            }
            .width(280.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        shadowElevation = (6 * scale).dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    softWrap = true
                )
            }
        }
    }
}

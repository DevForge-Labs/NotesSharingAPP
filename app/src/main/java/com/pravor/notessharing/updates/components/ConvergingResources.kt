package com.pravor.notessharing.updates.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pravor.notessharing.R

@Composable
fun ConvergingResourcesVisual(isActive: Boolean) {
    val convergeFraction by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(1400, easing = FastOutSlowInEasing),
        label = "converge_fraction"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isActive && convergeFraction > 0.9f) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "converge_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        contentAlignment = Alignment.Center
    ) {
        // Subtle orbits
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = 100.dp.toPx()
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = radius,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            )
        }

        // Central Logo (Unified Destination)
        Surface(
            modifier = Modifier
                .size(96.dp)
                .scale(scale),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            shadowElevation = 8.dp
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                Image(
                    painter = painterResource(R.drawable.app_logo_normal),
                    contentDescription = "NotesSharing Logo",
                    modifier = Modifier.size(60.dp)
                )
            }
        }

        // Scattered resources converging towards the center
        // Top-Left: Chat
        ScatteredItem(
            icon = Icons.Filled.QuestionAnswer,
            text = "Group: ECE PYQs here",
            backgroundColor = Color(0xFF1B5E20), // Dark green bubble
            offsetX = startToCenter(-105.dp, convergeFraction),
            offsetY = startToCenter(-75.dp, convergeFraction),
            alpha = getFadeAlpha(convergeFraction),
            scale = getScaleFactor(convergeFraction)
        )

        // Top-Right: PDF
        ScatteredItem(
            icon = Icons.Filled.Description,
            text = "DBMS_Notes.pdf",
            backgroundColor = Color(0xFFC62828), // Red gradient
            offsetX = startToCenter(105.dp, convergeFraction),
            offsetY = startToCenter(-55.dp, convergeFraction),
            alpha = getFadeAlpha(convergeFraction),
            scale = getScaleFactor(convergeFraction)
        )

        // Bottom-Left: Folder
        ScatteredItem(
            icon = Icons.Filled.Class,
            text = "Drive > Semester 4",
            backgroundColor = Color(0xFFF57F17), // Orange/gold
            offsetX = startToCenter(-95.dp, convergeFraction),
            offsetY = startToCenter(75.dp, convergeFraction),
            alpha = getFadeAlpha(convergeFraction),
            scale = getScaleFactor(convergeFraction)
        )

        // Bottom-Right: Link
        ScatteredItem(
            icon = Icons.Filled.Share,
            text = "http://drive.google...",
            backgroundColor = Color(0xFF1565C0), // Blue
            offsetX = startToCenter(95.dp, convergeFraction),
            offsetY = startToCenter(65.dp, convergeFraction),
            alpha = getFadeAlpha(convergeFraction),
            scale = getScaleFactor(convergeFraction)
        )
    }
}

// Math helpers
private fun startToCenter(start: androidx.compose.ui.unit.Dp, fraction: Float): androidx.compose.ui.unit.Dp {
    return start * (1f - fraction)
}
private fun getFadeAlpha(fraction: Float): Float {
    return (1f - (fraction - 0.75f).coerceAtLeast(0f) * 4f).coerceIn(0f, 1f)
}
private fun getScaleFactor(fraction: Float): Float {
    return (1f - fraction * 0.4f).coerceIn(0.6f, 1f)
}

@Composable
fun ScatteredItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    backgroundColor: Color,
    offsetX: androidx.compose.ui.unit.Dp,
    offsetY: androidx.compose.ui.unit.Dp,
    alpha: Float,
    scale: Float
) {
    Surface(
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .graphicsLayer {
                this.alpha = alpha
                this.scaleX = scale
                this.scaleY = scale
            },
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor.copy(alpha = 0.9f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                maxLines = 1,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

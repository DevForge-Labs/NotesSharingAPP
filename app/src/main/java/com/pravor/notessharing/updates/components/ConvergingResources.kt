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
import androidx.compose.material3.Icon
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
    val infiniteTransition = rememberInfiniteTransition(label = "orbit_transition")

    // Calm 20-second rotation for the orbits
    val baseAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "baseAngle"
    )

    // 12-second cycle for sequential merging: one card merges every 3 seconds
    val cycleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cycleProgress"
    )

    val entryProgress by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "entryProgress"
    )

    // 4 static cards properties
    val baseRadii = listOf(65.dp, 80.dp, 95.dp, 110.dp)
    val cardIcons = listOf(
        Icons.Filled.Description,
        Icons.Filled.QuestionAnswer,
        Icons.Filled.Class,
        Icons.Filled.Share
    )
    val cardTexts = listOf(
        "DBMS Notes",
        "ECE PYQs",
        "Semester 4",
        "Lecture Slides"
    )
    val cardColors = listOf(
        Color(0xFFC62828), // Red
        Color(0xFF1B5E20), // Green
        Color(0xFFF57F17), // Orange
        Color(0xFF1565C0)  // Blue
    )

    // Calculate a subtle logo scale pulse based on whether any card is merging
    var logoPulse = 0f
    for (j in 0..3) {
        var lp = cycleProgress - j
        if (lp < 0f) { lp += 4f }
        val dist = Math.abs(lp - 0.5f)
        if (dist < 0.08f) {
            logoPulse = (1f - (dist / 0.08f)) * 0.08f
            break
        }
    }
    val logoScale = entryProgress * (1f + logoPulse)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        contentAlignment = Alignment.Center
    ) {
        // Orbit paths
        Canvas(modifier = Modifier.fillMaxSize()) {
            baseRadii.forEach { radius ->
                val radiusPx = radius.toPx() * entryProgress
                drawCircle(
                    color = Color.White.copy(alpha = 0.04f * entryProgress),
                    radius = radiusPx,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 2f,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 15f), 0f)
                    )
                )
            }
        }

        // Central static app logo
        Surface(
            modifier = Modifier
                .size(90.dp)
                .scale(logoScale),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
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
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        // Render orbiting cards
        val density = androidx.compose.ui.platform.LocalDensity.current
        for (i in 0..3) {
            var localProgress = cycleProgress - i
            if (localProgress < 0f) { localProgress += 4f }

            var radiusScale = 1f
            var alphaVal = 1f
            var scaleVal = 1f

            if (localProgress in 0f..0.5f) {
                // Merge into center
                val t = localProgress / 0.5f
                radiusScale = 1f - t
                alphaVal = 1f - t
                scaleVal = 1f - 0.3f * t
            } else if (localProgress in 0.5f..1.0f) {
                // Emerge from center
                val t = (localProgress - 0.5f) / 0.5f
                radiusScale = t
                alphaVal = t
                scaleVal = 0.7f + 0.3f * t
            }

            // Orbital angle (cards spaced 90 degrees apart)
            val angle = baseAngle + (i * 90f)
            val radians = Math.toRadians(angle.toDouble())

            val radiusPx = with(density) { (baseRadii[i] * radiusScale * entryProgress).toPx() }
            val offsetX = with(density) { (radiusPx * Math.cos(radians)).toFloat().toDp() }
            val offsetY = with(density) { (radiusPx * Math.sin(radians)).toFloat().toDp() }

            ScatteredItem(
                icon = cardIcons[i],
                text = cardTexts[i],
                backgroundColor = cardColors[i],
                offsetX = offsetX,
                offsetY = offsetY,
                alpha = alphaVal * entryProgress,
                scale = scaleVal
            )
        }
    }
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
            Icon(
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

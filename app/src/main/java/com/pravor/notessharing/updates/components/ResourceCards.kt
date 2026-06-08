package com.pravor.notessharing.updates.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pravor.notessharing.ui.theme.*

@Composable
fun StaggeredCardsVisual(isActive: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.95f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StaggeredCard(
                    title = "Notes",
                    description = "Core lecture topics",
                    icon = Icons.Filled.Book,
                    gradientColors = listOf(ElectricBlue, Color(0xFF1E3A8A)),
                    isActive = isActive,
                    delay = 100,
                    modifier = Modifier.weight(1.2f)
                )
                StaggeredCard(
                    title = "Assignments",
                    description = "Solutions & guides",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    gradientColors = listOf(Mint, Color(0xFF064E3B)),
                    isActive = isActive,
                    delay = 250,
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StaggeredCard(
                    title = "PYQs",
                    description = "Previous Year Questions",
                    icon = Icons.Filled.School,
                    gradientColors = listOf(Coral, Color(0xFF7F1D1D)),
                    isActive = isActive,
                    delay = 400,
                    modifier = Modifier.weight(1f)
                )
                StaggeredCard(
                    title = "Cheat Sheets",
                    description = "Quick formulas",
                    icon = Icons.Filled.AutoAwesome,
                    gradientColors = listOf(Gold, Color(0xFF78350F)),
                    isActive = isActive,
                    delay = 550,
                    modifier = Modifier.weight(1.2f)
                )
            }

            // Row 3 (Video Resources - dynamic height, wraps text fully)
            StaggeredCard(
                title = "Video Resources",
                description = "Curated video lectures from top creators & faculty",
                icon = Icons.Filled.PlayArrow,
                gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFF311B92)),
                isActive = isActive,
                delay = 700,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun StaggeredCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradientColors: List<Color>,
    isActive: Boolean,
    delay: Int,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(500, delayMillis = delay)
    )
    val translateY by animateDpAsState(
        targetValue = if (isActive) 0.dp else 40.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Surface(
        modifier = modifier
            .graphicsLayer { this.alpha = alpha }
            .offset(y = translateY),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(gradientColors))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 3, // Allow text to wrap dynamically up to 3 lines
                        softWrap = true
                    )
                }
            }
        }
    }
}

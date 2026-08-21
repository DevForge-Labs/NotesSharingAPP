package com.pravor.notessharing.ui.features.onboarding.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pravor.notessharing.ui.theme.Coral
import com.pravor.notessharing.ui.theme.ElectricBlue
import com.pravor.notessharing.ui.theme.Gold
import com.pravor.notessharing.ui.theme.Mint

@Composable
fun WhyUseAppVisual(isActive: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FeatureCard(
                title = "Fast Discovery",
                description = "Find notes, PYQs, assignments and videos in seconds.",
                icon = Icons.Filled.Search,
                color = ElectricBlue,
                isActive = isActive,
                delay = 100,
                modifier = Modifier.weight(1f)
            )
            FeatureCard(
                title = "Smart Recs",
                description = "Discover resources from your branch & semester.",
                icon = Icons.Filled.AutoAwesome,
                color = Mint,
                isActive = isActive,
                delay = 250,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FeatureCard(
                title = "Resume Learning",
                description = "Resume where you left off across files & videos.",
                icon = Icons.Filled.PlayArrow,
                color = Coral,
                isActive = isActive,
                delay = 400,
                modifier = Modifier.weight(1f)
            )
            FeatureCard(
                title = "Peer Support",
                description = "Help fellow students by sharing academic resources.",
                icon = Icons.Filled.Share,
                color = Gold,
                isActive = isActive,
                delay = 550,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun FeatureCard(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    isActive: Boolean,
    delay: Int,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(500, delayMillis = delay, easing = LinearOutSlowInEasing),
        label = "feature_alpha"
    )
    val translateY by animateDpAsState(
        targetValue = if (isActive) 0.dp else 24.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "feature_translateY"
    )

    Surface(
        modifier = modifier
            .graphicsLayer {
                this.alpha = alpha
            }
            .offset(y = translateY),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
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

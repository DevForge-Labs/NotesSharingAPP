package com.pravor.notessharing.updates.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.pravor.notessharing.ui.theme.*

@Composable
fun RippleCommunityVisual(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_particles")
    
    val flowProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_flow"
    )

    val entryProgress by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "pulse_entry"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        val isSmallScreen = maxWidth < 380.dp
        val scaleFactor = if (isSmallScreen) 0.85f else 1f

        // Let's compute pixel positions dynamically to match the canvas bezier curves
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            
            // Contributor Node (Bottom Center)
            val contributorX = w / 2f
            val contributorY = h - 35.toDp().toPx()
            
            // Learner 1 (Top Left)
            val l1X = w * 0.22f
            val l1Y = (if (isSmallScreen) 30.dp else 35.dp).toPx()
            
            // Learner 2 (Top Center - Lowered vertically to prevent collision on narrow displays)
            val l2X = w * 0.5f
            val l2Y = (if (isSmallScreen) 90.dp else 105.dp).toPx()
            
            // Learner 3 (Top Right)
            val l3X = w * 0.78f
            val l3Y = (if (isSmallScreen) 30.dp else 35.dp).toPx()

            // Draw Paths
            val path1 = androidx.compose.ui.graphics.Path().apply {
                moveTo(contributorX, contributorY)
                cubicTo(
                    contributorX - 40, contributorY - 60,
                    l1X + 20, l1Y + 60,
                    l1X, l1Y
                )
            }

            val path2 = androidx.compose.ui.graphics.Path().apply {
                moveTo(contributorX, contributorY)
                quadraticTo(
                    contributorX, (contributorY + l2Y) / 2f,
                    l2X, l2Y
                )
            }

            val path3 = androidx.compose.ui.graphics.Path().apply {
                moveTo(contributorX, contributorY)
                cubicTo(
                    contributorX + 40, contributorY - 60,
                    l3X - 20, l3Y + 60,
                    l3X, l3Y
                )
            }

            val drawPathSpec = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 3f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
            )

            drawPath(
                path = path1,
                color = ElectricBlue.copy(alpha = 0.25f * entryProgress),
                style = drawPathSpec
            )
            drawPath(
                path = path2,
                color = Mint.copy(alpha = 0.25f * entryProgress),
                style = drawPathSpec
            )
            drawPath(
                path = path3,
                color = Coral.copy(alpha = 0.25f * entryProgress),
                style = drawPathSpec
            )

            // Draw flowing energy particles along bezier paths
            if (isActive) {
                val t = flowProgress
                val u = 1f - t
                
                // Path 1 Particle
                val p1_0_x = contributorX
                val p1_0_y = contributorY
                val p1_1_x = contributorX - 40f
                val p1_1_y = contributorY - 60f
                val p1_2_x = l1X + 20f
                val p1_2_y = l1Y + 60f
                val p1_3_x = l1X
                val p1_3_y = l1Y
                val part1X = u*u*u*p1_0_x + 3*u*u*t*p1_1_x + 3*u*t*t*p1_2_x + t*t*t*p1_3_x
                val part1Y = u*u*u*p1_0_y + 3*u*u*t*p1_1_y + 3*u*t*t*p1_2_y + t*t*t*p1_3_y
                
                // Path 2 Particle
                val p2_0_x = contributorX
                val p2_0_y = contributorY
                val p2_1_x = contributorX
                val p2_1_y = (contributorY + l2Y) / 2f
                val p2_2_x = l2X
                val p2_2_y = l2Y
                val part2X = u*u*p2_0_x + 2*u*t*p2_1_x + t*t*p2_2_x
                val part2Y = u*u*p2_0_y + 2*u*t*p2_1_y + t*t*p2_2_y
                
                // Path 3 Particle
                val p3_0_x = contributorX
                val p3_0_y = contributorY
                val p3_1_x = contributorX + 40f
                val p3_1_y = contributorY - 60f
                val p3_2_x = l3X - 20f
                val p3_2_y = l3Y + 60f
                val p3_3_x = l3X
                val p3_3_y = l3Y
                val part3X = u*u*u*p3_0_x + 3*u*u*t*p3_1_x + 3*u*t*t*p3_2_x + t*t*t*p3_3_x
                val part3Y = u*u*u*p3_0_y + 3*u*u*t*p3_1_y + 3*u*t*t*p3_2_y + t*t*t*p3_3_y

                drawCircle(color = ElectricBlue, radius = 6f * entryProgress, center = androidx.compose.ui.geometry.Offset(part1X, part1Y))
                drawCircle(color = Mint, radius = 6f * entryProgress, center = androidx.compose.ui.geometry.Offset(part2X, part2Y))
                drawCircle(color = Coral, radius = 6f * entryProgress, center = androidx.compose.ui.geometry.Offset(part3X, part3Y))
            }
        }

        // Bottom Centered Contributor Node
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-10).dp)
                .scale(scaleFactor)
                .graphicsLayer {
                    alpha = entryProgress
                    scaleX = entryProgress
                    scaleY = entryProgress
                },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Upload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "You share a note",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        // Top Left Benefitted Peer (Priya)
        val pulseL1 = if (isActive && flowProgress > 0.85f) 1f - (flowProgress - 0.85f) * 6.6f else 0f
        LearnerNode(
            text = "Priya aced DBMS",
            iconColor = ElectricBlue,
            entryProgress = entryProgress,
            scale = scaleFactor * (1f + pulseL1 * 0.12f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = if (isSmallScreen) 4.dp else 12.dp, 
                    y = if (isSmallScreen) 10.dp else 15.dp
                )
        )

        // Top Center Benefitted Peer (Aman - Lowered to prevent overlaps)
        val pulseL2 = if (isActive && flowProgress > 0.85f) 1f - (flowProgress - 0.85f) * 6.6f else 0f
        LearnerNode(
            text = "Aman cleared CS101",
            iconColor = Mint,
            entryProgress = entryProgress,
            scale = scaleFactor * (1f + pulseL2 * 0.12f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(
                    y = if (isSmallScreen) 70.dp else 85.dp
                )
        )

        // Top Right Benefitted Peer (Sneha)
        val pulseL3 = if (isActive && flowProgress > 0.85f) 1f - (flowProgress - 0.85f) * 6.6f else 0f
        LearnerNode(
            text = "Sneha aced Mid-Sem",
            iconColor = Coral,
            entryProgress = entryProgress,
            scale = scaleFactor * (1f + pulseL3 * 0.12f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(
                    x = if (isSmallScreen) (-4).dp else (-12).dp, 
                    y = if (isSmallScreen) 10.dp else 15.dp
                )
        )
    }
}

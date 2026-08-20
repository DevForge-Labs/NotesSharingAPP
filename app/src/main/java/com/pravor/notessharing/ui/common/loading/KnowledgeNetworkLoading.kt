package com.pravor.notessharing.ui.common.loading

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pravor.notessharing.R
import com.pravor.notessharing.ui.theme.ElectricBlue
import com.pravor.notessharing.ui.theme.Mint
import com.pravor.notessharing.ui.theme.Coral
import com.pravor.notessharing.ui.theme.Gold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

data class NetworkNode(
    val icon: ImageVector,
    val label: String,
    val color: Color
)

@Composable
fun KnowledgeNetworkLoading(
    modifier: Modifier = Modifier
) {
    val nodes = remember {
        listOf(
            NetworkNode(Icons.Filled.Description, "Notes", ElectricBlue),
            NetworkNode(Icons.Filled.FilePresent, "PDFs", Coral),
            NetworkNode(Icons.Filled.PlayCircle, "Videos", Mint),
            NetworkNode(Icons.Filled.Bookmark, "Bookmarks", Gold),
            NetworkNode(Icons.Filled.UploadFile, "Uploads", ElectricBlue),
            NetworkNode(Icons.Filled.School, "Guides", Mint),
            NetworkNode(Icons.AutoMirrored.Filled.Assignment, "Tasks", Coral),
            NetworkNode(Icons.Filled.Explore, "Discover", Gold)
        )
    }

    val nodeCount = nodes.size
    val nodeAlphas = remember { List(nodeCount) { Animatable(0f) } }
    val lineProgresses = remember { List(nodeCount) { Animatable(0f) } }

    LaunchedEffect(Unit) {
        // Phase 1: Nodes fade in sequentially around the logo (~300ms span)
        nodes.forEachIndexed { index, _ ->
            launch {
                delay(index * 35L) // Staggered delays
                nodeAlphas[index].animateTo(1f, animationSpec = tween(250, easing = FastOutSlowInEasing))
            }
        }

        // Phase 2: Connection lines animate outward from the center (~300ms build)
        nodes.forEachIndexed { index, _ ->
            launch {
                delay(80L + index * 25L) // Slightly overlapping build
                lineProgresses[index].animateTo(1f, animationSpec = tween(300, easing = LinearOutSlowInEasing))
            }
        }
    }

    // Infinite transitions for pulsing and breathing
    val infiniteTransition = rememberInfiniteTransition(label = "network_infinite")
    
    // Phase 3: Soft pulses travel through the network
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    // Phase 4: Gentle breathing scale
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    // Floating offsets for each node to look organic
    val floatAnimOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "floating"
    )

    val radius = 130.dp
    val density = LocalDensity.current
    val radiusPx = with(density) { radius.toPx() }
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Breath scale applies to connection canvas & nodes
        Box(
            modifier = Modifier
                .size(380.dp)
                .scale(breatheScale),
            contentAlignment = Alignment.Center
        ) {
            // Draw connection lines and pulse travel dots
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                
                for (i in 0 until nodeCount) {
                    val angleRad = Math.toRadians((i * (360.0 / nodeCount)) - 90.0)
                    val targetX = center.x + radiusPx * cos(angleRad).toFloat()
                    val targetY = center.y + radiusPx * sin(angleRad).toFloat()

                    val lineProg = lineProgresses[i].value
                    val currentEndX = center.x + (targetX - center.x) * lineProg
                    val currentEndY = center.y + (targetY - center.y) * lineProg

                    val nodeColor = nodes[i].color
                    
                    // Draw base connection line
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.15f),
                                nodeColor.copy(alpha = 0.35f * lineProg)
                            ),
                            start = center,
                            end = Offset(currentEndX, currentEndY)
                        ),
                        start = center,
                        end = Offset(currentEndX, currentEndY),
                        strokeWidth = 2f
                    )

                    // Draw moving pulse glow dot
                    if (lineProg > 0.1f) {
                        val pulseProg = pulseProgress
                        val dotX = center.x + (targetX - center.x) * pulseProg
                        val dotY = center.y + (targetY - center.y) * pulseProg
                        
                        // Pulse is only visible if the line has grown past it
                        if (pulseProg <= lineProg) {
                            drawCircle(
                                color = nodeColor,
                                radius = 4f,
                                center = Offset(dotX, dotY),
                                alpha = 0.75f * (1f - pulseProg) // Fade out as it reaches the node
                            )
                            drawCircle(
                                color = nodeColor.copy(alpha = 0.25f),
                                radius = 8f,
                                center = Offset(dotX, dotY),
                                alpha = 0.4f * (1f - pulseProg)
                            )
                        }
                    }
                }
            }

            // Draw Nodes around the center hub
            for (i in 0 until nodeCount) {
                val angleDeg = (i * (360.0 / nodeCount)) - 90.0
                val angleRad = Math.toRadians(angleDeg)
                
                // Add tiny organic float offset
                val floatOffset = 3.dp * sin(floatAnimOffset.toDouble() + i * 1.5).toFloat()
                val currentRadius = radius + floatOffset

                val xOffset = currentRadius * cos(angleRad).toFloat()
                val yOffset = currentRadius * sin(angleRad).toFloat()

                val node = nodes[i]
                val alpha = nodeAlphas[i].value

                Box(
                    modifier = Modifier
                        .offset(x = xOffset, y = yOffset)
                        .alpha(alpha),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(44.dp)
                                .border(
                                    width = 1.dp,
                                    color = node.color.copy(alpha = 0.4f),
                                    shape = CircleShape
                                ),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            shadowElevation = 3.dp
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = node.icon,
                                    contentDescription = node.label,
                                    tint = node.color,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = node.label,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Central logo acting as the knowledge hub
            Surface(
                modifier = Modifier
                    .size(86.dp)
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        shape = CircleShape
                    ),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.app_logo_normal),
                        contentDescription = "Campus Pages Hub",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

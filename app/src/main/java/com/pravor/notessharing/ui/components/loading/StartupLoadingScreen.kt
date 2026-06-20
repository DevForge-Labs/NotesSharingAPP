package com.pravor.notessharing.ui.components.loading

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pravor.notessharing.R
import kotlinx.coroutines.delay

@Composable
fun StartupLoadingScreen(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "startup_loading")

    // Orbit rotation (0 to 360 degrees) over 3.5 seconds
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_rotation"
    )

    // Breathing scale for center logo (0.93f to 1.07f)
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.93f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "center_breathing"
    )

    // Breathing opacity for text
    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "text_alpha"
    )

    val messages = remember {
        listOf(
            "Syncing your profile",
            "Loading your subjects",
            "Preparing your home screen",
            "Finding new resources",
            "Almost ready..."
        )
    }
    var currentMessageIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3000L)
            currentMessageIndex = (currentMessageIndex + 1) % messages.size
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                // Glow effect in background
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Center static app logo
                Image(
                    painter = painterResource(id = R.drawable.app_logo_normal),
                    contentDescription = "NoteShare Logo",
                    modifier = Modifier
                        .size(56.dp)
                        .graphicsLayer {
                            scaleX = breathingScale
                            scaleY = breathingScale
                        }
                )

                // Orbiting element 1: Notes (Description Icon)
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .graphicsLayer { rotationZ = rotationAngle },
                    contentAlignment = Alignment.TopCenter
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                rotationZ = -rotationAngle
                            }
                    )
                }

                // Orbiting element 2: Assignments (Assignment Icon)
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .graphicsLayer { rotationZ = rotationAngle + 90f },
                    contentAlignment = Alignment.TopCenter
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                rotationZ = -(rotationAngle + 90f)
                            }
                    )
                }

                // Orbiting element 3: PYQs (School Icon)
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .graphicsLayer { rotationZ = rotationAngle + 180f },
                    contentAlignment = Alignment.TopCenter
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                rotationZ = -(rotationAngle + 180f)
                            }
                    )
                }

                // Orbiting element 4: Videos (PlayCircle Icon)
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .graphicsLayer { rotationZ = rotationAngle + 270f },
                    contentAlignment = Alignment.TopCenter
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = Color(0xFFFFB74D), // Soft Amber
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                rotationZ = -(rotationAngle + 270f)
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Preparing NoteShare...",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = textAlpha)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Crossfade(
                targetState = messages[currentMessageIndex],
                animationSpec = tween(600),
                label = "subtitle_rotation"
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        letterSpacing = 0.25.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = textAlpha * 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    }
}

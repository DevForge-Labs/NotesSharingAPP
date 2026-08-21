package com.pravor.notessharing.ui.features.document.components

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.EaseOutQuad
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pravor.notessharing.ui.features.document.DownloadState
import kotlinx.coroutines.delay
import kotlin.math.sin

enum class CompletionPhase {
    NOT_STARTED,
    SETTLING,
    RIPPLE,
    FINISHED
}

@Composable
fun NotesSharingDownloadIndicator(
    downloadState: DownloadState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (downloadState as? DownloadState.Downloading)?.progress
    val isDownloaded = downloadState is DownloadState.Downloaded

    var completionPhase by remember { mutableStateOf(CompletionPhase.NOT_STARTED) }

    LaunchedEffect(isDownloaded) {
        if (isDownloaded) {
            if (completionPhase == CompletionPhase.NOT_STARTED) {
                completionPhase = CompletionPhase.SETTLING
                delay(500)
                completionPhase = CompletionPhase.RIPPLE
                delay(400)
                completionPhase = CompletionPhase.FINISHED
            }
        } else {
            completionPhase = CompletionPhase.NOT_STARTED
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "indicator-animations")
    
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave-phase"
    )

    val breathingValue by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing-glow"
    )

    val shimmerPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                0.0f at 0
                0.0f at 1800
                1.0f at 2600
                1.0f at 3000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer-phase"
    )

    val smoothProgress by animateFloatAsState(
        targetValue = if (isDownloaded) 1f else (progress ?: 0f),
        animationSpec = tween(500, easing = EaseOutQuad),
        label = "smooth-progress"
    )

    val targetAmplitude = when {
        isDownloaded && completionPhase == CompletionPhase.SETTLING -> 0f
        isDownloaded && completionPhase == CompletionPhase.NOT_STARTED -> 2f
        progress != null -> 2f
        else -> 0f
    }
    val waveAmplitude by animateFloatAsState(
        targetValue = targetAmplitude,
        animationSpec = tween(500, easing = EaseOutQuad),
        label = "wave-amplitude"
    )

    val checkDrawProgress by animateFloatAsState(
        targetValue = if (completionPhase == CompletionPhase.FINISHED || completionPhase == CompletionPhase.RIPPLE) 1f else 0f,
        animationSpec = tween(600, easing = EaseOutCubic),
        label = "check-draw-progress"
    )

    var triggerPulse by remember { mutableStateOf(false) }
    LaunchedEffect(isDownloaded) {
        if (isDownloaded) {
            triggerPulse = true
        }
    }
    val successScale by animateFloatAsState(
        targetValue = if (triggerPulse) 1.15f else 1.0f,
        animationSpec = keyframes {
            durationMillis = 600
            1.0f at 0
            1.15f at 180
            0.95f at 380
            1.0f at 600
        },
        finishedListener = { triggerPulse = false },
        label = "success-scale"
    )

    val rippleProgress by animateFloatAsState(
        targetValue = if (completionPhase == CompletionPhase.RIPPLE) 1f else 0f,
        animationSpec = tween(400, easing = EaseOutCubic),
        label = "ripple-progress"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (completionPhase == CompletionPhase.RIPPLE) 1f else 0f,
        animationSpec = keyframes {
            durationMillis = 400
            0f at 0
            1f at 150
            0f at 400
        },
        label = "glow-alpha"
    )

    val accentColor = MaterialTheme.colorScheme.primary
    val bubbleBgColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    val glassBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(bubbleBgColor)
            .clickable(enabled = progress == null && !isDownloaded) { onClick() }
            .graphicsLayer {
                scaleX = successScale
                scaleY = successScale
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val strokeWidth = 2.dp.toPx()
            val radius = (w - strokeWidth) / 2f
            val centerX = w / 2f
            val centerY = h / 2f

            val circlePath = Path().apply {
                addOval(
                    Rect(
                        center = Offset(centerX, centerY),
                        radius = radius
                    )
                )
            }

            if (completionPhase == CompletionPhase.FINISHED) {
                drawPath(
                    path = circlePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accentColor,
                            accentColor.copy(alpha = 0.7f)
                        )
                    )
                )

                drawPath(
                    path = circlePath,
                    color = accentColor.copy(alpha = 0.4f + 0.3f * breathingValue),
                    style = Stroke(width = strokeWidth)
                )

                val startX = w * 0.32f
                val startY = h * 0.50f
                val midX = w * 0.46f
                val midY = h * 0.64f
                val endX = w * 0.68f
                val endY = h * 0.36f

                val checkPath = Path().apply {
                    moveTo(startX, startY)
                    lineTo(midX, midY)
                    lineTo(endX, endY)
                }

                val checkColor = Color.White.copy(alpha = 0.8f + 0.2f * breathingValue)
                val shimmerColor = Color.White
                
                val stop1 = (shimmerPhase - 0.15f).coerceIn(0f, 1f)
                val stop2 = shimmerPhase.coerceIn(0f, 1f)
                val stop3 = (shimmerPhase + 0.15f).coerceIn(0f, 1f)

                val checkBrush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to checkColor,
                        stop1 to checkColor,
                        stop2 to shimmerColor,
                        stop3 to checkColor,
                        1.0f to checkColor
                    ),
                    start = Offset(w * 0.2f, h * 0.2f),
                    end = Offset(w * 0.8f, h * 0.8f)
                )

                drawPath(
                    path = checkPath,
                    brush = checkBrush,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            } else if (completionPhase == CompletionPhase.RIPPLE || completionPhase == CompletionPhase.SETTLING) {
                drawPath(
                    path = circlePath,
                    color = bubbleBgColor
                )

                clipPath(circlePath) {
                    val baseLevelY = centerY - radius
                    val wavePath = Path().apply {
                        moveTo(0f, h)
                        lineTo(0f, baseLevelY)
                        
                        val segments = 40
                        val segmentWidth = w / segments
                        val amplitudePx = waveAmplitude.dp.toPx()
                        val frequency = 2 * Math.PI.toFloat() / w
                        
                        for (i in 0..segments) {
                            val x = i * segmentWidth
                            val y = baseLevelY + sin(x * frequency + wavePhase) * amplitudePx
                            lineTo(x, y)
                        }
                        
                        lineTo(w, h)
                        close()
                    }

                    drawPath(
                        path = wavePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                accentColor,
                                accentColor.copy(alpha = 0.7f)
                            )
                        )
                    )
                }

                drawPath(
                    path = circlePath,
                    color = glassBorderColor,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                if (completionPhase == CompletionPhase.RIPPLE) {
                    val rippleRadius = radius * 0.6f + (radius * 0.6f * rippleProgress)
                    val rippleAlpha = (1f - rippleProgress) * 0.8f
                    drawCircle(
                        color = accentColor,
                        radius = rippleRadius,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = 3.dp.toPx() * (1f - rippleProgress)),
                        alpha = rippleAlpha
                    )
                }

                if (glowAlpha > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.4f * glowAlpha),
                                Color.Transparent
                            ),
                            center = Offset(centerX, centerY),
                            radius = radius * 1.3f
                        )
                    )
                }

                if (checkDrawProgress > 0f) {
                    val startX = w * 0.32f
                    val startY = h * 0.50f
                    val midX = w * 0.46f
                    val midY = h * 0.64f
                    val endX = w * 0.68f
                    val endY = h * 0.36f

                    val checkPath = Path().apply {
                        moveTo(startX, startY)
                        if (checkDrawProgress <= 0.4f) {
                            val fraction = checkDrawProgress / 0.4f
                            lineTo(
                                startX + (midX - startX) * fraction,
                                startY + (midY - startY) * fraction
                            )
                        } else {
                            lineTo(midX, midY)
                            val fraction = (checkDrawProgress - 0.4f) / 0.6f
                            lineTo(
                                midX + (endX - midX) * fraction,
                                midY + (endY - midY) * fraction
                            )
                        }
                    }

                    drawPath(
                        path = checkPath,
                        color = Color.White,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            } else if (progress != null) {
                drawPath(
                    path = circlePath,
                    color = bubbleBgColor
                )

                clipPath(circlePath) {
                    val baseLevelY = centerY + radius - (2 * radius * smoothProgress)
                    val wavePath = Path().apply {
                        moveTo(0f, h)
                        lineTo(0f, baseLevelY)
                        
                        val segments = 40
                        val segmentWidth = w / segments
                        val amplitudePx = waveAmplitude.dp.toPx()
                        val frequency = 2 * Math.PI.toFloat() / w
                        
                        for (i in 0..segments) {
                            val x = i * segmentWidth
                            val y = baseLevelY + sin(x * frequency + wavePhase) * amplitudePx
                            lineTo(x, y)
                        }
                        
                        lineTo(w, h)
                        close()
                    }

                    drawPath(
                        path = wavePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                accentColor,
                                accentColor.copy(alpha = 0.7f)
                            )
                        )
                    )
                }

                drawPath(
                    path = circlePath,
                    color = glassBorderColor,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            } else {
                drawPath(
                    path = circlePath,
                    color = bubbleBgColor
                )
                drawPath(
                    path = circlePath,
                    color = glassBorderColor,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        if (completionPhase == CompletionPhase.FINISHED) {
            // Drawn directly on Canvas
        } else if (completionPhase == CompletionPhase.RIPPLE || completionPhase == CompletionPhase.SETTLING) {
            // Drawn on Canvas
        } else if (progress != null) {
            Text(
                text = "${(smoothProgress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (smoothProgress > 0.55f) Color.White else MaterialTheme.colorScheme.onSurface
                )
            )
        } else {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Download All",
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

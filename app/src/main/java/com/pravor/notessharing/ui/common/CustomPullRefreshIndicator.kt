package com.pravor.notessharing.ui.common

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*


import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomPullRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
    restingOffset: androidx.compose.ui.unit.Dp = 16.dp
) {
    // 1. Return early if idle to occupy zero layout space and prevent rendering
    if (state.distanceFraction == 0f && !isRefreshing) {
        return
    }

    val context = LocalContext.current
    
    // Accessibility check: Check if system transitions/animations are enabled
    val isAnimationEnabled = remember(context) {
        try {
            android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE,
                1f
            ) > 0f
        } catch (e: Exception) {
            true
        }
    }
    
    // Calculate progress (clamp to 0f..1f for visual consistency)
    val progress = state.distanceFraction.coerceIn(0f, 1f)
    val isThresholdReached = state.distanceFraction >= 1f && !isRefreshing
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_and_rotate")
    
    // Subtle pulse for the ring when threshold is reached
    val ringPulse = if (isThresholdReached && isAnimationEnabled) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ring_pulse"
        ).value
    } else {
        1f
    }
    
    // Scale-up pulse when threshold is reached
    val scaleMultiplier by animateFloatAsState(
        targetValue = if (isThresholdReached) 1.05f else 1.0f,
        animationSpec = if (isAnimationEnabled) {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        } else {
            snap()
        },
        label = "threshold_scale"
    )

    // Rotation Animatable to allow seamless continuation from pull to refresh state
    val rotationAnimatable = remember { Animatable(0f) }

    // Snap rotation directly to the pull progress (up to 120 degrees) when not refreshing
    LaunchedEffect(progress, isRefreshing) {
        if (!isRefreshing) {
            rotationAnimatable.snapTo(progress * 120f)
        }
    }

    // Infinite constant-speed clockwise rotation when refreshing
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            while (true) {
                val currentVal = rotationAnimatable.value
                rotationAnimatable.animateTo(
                    targetValue = currentVal + 360f,
                    animationSpec = tween(4000, easing = LinearEasing)
                )
            }
        }
    }

    // Haptic Feedback when threshold is reached for the first time during a pull
    val haptic = LocalHapticFeedback.current
    var hasTriggeredHaptic by remember { mutableStateOf(false) }

    LaunchedEffect(state.distanceFraction, isRefreshing) {
        if (isRefreshing) {
            hasTriggeredHaptic = false
        } else if (state.distanceFraction >= 1f) {
            if (!hasTriggeredHaptic) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                hasTriggeredHaptic = true
            }
        } else {
            hasTriggeredHaptic = false
        }
    }

    val ringBaseColor = MaterialTheme.colorScheme.outline
    val ringDrawColor = MaterialTheme.colorScheme.primary
    val descriptionPainter = rememberVectorPainter(Icons.Default.Description)
    val editPainter = rememberVectorPainter(Icons.Default.Edit)
    val bookPainter = rememberVectorPainter(Icons.AutoMirrored.Filled.MenuBook)
    val bookmarkPainter = rememberVectorPainter(Icons.Default.Bookmark)
    
    // Theme-based icon colors
    val iconColor1 = MaterialTheme.colorScheme.primary
    val iconColor2 = MaterialTheme.colorScheme.secondary
    val iconColor3 = MaterialTheme.colorScheme.tertiary
    val iconColor4 = MaterialTheme.colorScheme.primaryContainer

    // 2. Calculate vertical translation to follow the pull gesture
    val density = LocalDensity.current
    val indicatorHeightPx = with(density) { 52.dp.toPx() }
    
    // Rest position is custom restingOffset from the top. Start position is offscreen above (hidden).
    val startOffset = -indicatorHeightPx
    val endOffset = with(density) { restingOffset.toPx() }
    
    val targetTranslationY = if (isRefreshing) {
        endOffset
    } else {
        // Linearly interpolate position based on the pull progress (distanceFraction)
        startOffset + state.distanceFraction * (endOffset - startOffset)
    }
    
    // Fade in and scale up based on the pull progress
    val targetAlpha = if (isRefreshing) 1f else state.distanceFraction.coerceIn(0f, 1f)
    val targetScale = if (isRefreshing) 1f else state.distanceFraction.coerceIn(0.5f, 1f)

    val statusText = when {
        isRefreshing -> "Updating feed..."
        state.distanceFraction >= 1f -> "Release to refresh"
        else -> "Pull to refresh"
    }

    // Display container: Column containing the circular card loader and contextual status text
    // translated vertically and faded/scaled as a single unit
    Column(
        modifier = modifier
            .graphicsLayer {
                translationY = targetTranslationY
                alpha = targetAlpha
                scaleX = targetScale
                scaleY = targetScale
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .shadow(elevation = 6.dp, shape = CircleShape)
                .background(color = MaterialTheme.colorScheme.surfaceContainer)
                .size(52.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.size(40.dp)
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = 12.dp.toPx()
                val strokeWidth = 2.dp.toPx()
                
                // Draw guideline (orbit ring) progressively and scale with threshold pulse
                scale(scaleX = scaleMultiplier, scaleY = scaleMultiplier, pivot = center) {
                    val sweepAngle = if (isRefreshing) 360f else progress * 360f
                    val currentRotation = rotationAnimatable.value
                    
                    drawArc(
                        color = ringBaseColor,
                        startAngle = -90f + currentRotation,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    
                    // Calculate symmetric orbital positions for the 4 icons (separated by 90 degrees)
                    // and compute their alphas based on the 4 quadrants of progress:
                    // 0-25% -> Icon 1 (Description)
                    // 25-50% -> Icon 2 (Edit)
                    // 50-75% -> Icon 3 (MenuBook)
                    // 75-100% -> Icon 4 (Bookmark)
                    val alpha1 = if (isRefreshing) 1f else (progress / 0.25f).coerceIn(0f, 1f)
                    val alpha2 = if (isRefreshing) 1f else ((progress - 0.25f) / 0.25f).coerceIn(0f, 1f)
                    val alpha3 = if (isRefreshing) 1f else ((progress - 0.50f) / 0.25f).coerceIn(0f, 1f)
                    val alpha4 = if (isRefreshing) 1f else ((progress - 0.75f) / 0.25f).coerceIn(0f, 1f)
                    
                    val iconSize = Size(9.dp.toPx(), 9.dp.toPx())
                    val halfWidth = iconSize.width / 2f
                    val halfHeight = iconSize.height / 2f
                    
                    // Icon 1: Description at -90 degrees (primary color)
                    val angle1 = -90f + currentRotation
                    val rad1 = Math.toRadians(angle1.toDouble())
                    val x1 = center.x + radius * cos(rad1).toFloat()
                    val y1 = center.y + radius * sin(rad1).toFloat()
                    if (alpha1 > 0f) {
                        translate(left = x1 - halfWidth, top = y1 - halfHeight) {
                            with(descriptionPainter) {
                                draw(
                                    size = iconSize,
                                    colorFilter = ColorFilter.tint(iconColor1.copy(alpha = alpha1))
                                )
                            }
                        }
                    }
                    
                    // Icon 2: Edit (Pencil) at 0 degrees (secondary color)
                    val angle2 = 0f + currentRotation
                    val rad2 = Math.toRadians(angle2.toDouble())
                    val x2 = center.x + radius * cos(rad2).toFloat()
                    val y2 = center.y + radius * sin(rad2).toFloat()
                    if (alpha2 > 0f) {
                        translate(left = x2 - halfWidth, top = y2 - halfHeight) {
                            with(editPainter) {
                                draw(
                                    size = iconSize,
                                    colorFilter = ColorFilter.tint(iconColor2.copy(alpha = alpha2))
                                )
                            }
                        }
                    }
                    
                    // Icon 3: MenuBook at 90 degrees (tertiary color)
                    val angle3 = 90f + currentRotation
                    val rad3 = Math.toRadians(angle3.toDouble())
                    val x3 = center.x + radius * cos(rad3).toFloat()
                    val y3 = center.y + radius * sin(rad3).toFloat()
                    if (alpha3 > 0f) {
                        translate(left = x3 - halfWidth, top = y3 - halfHeight) {
                            with(bookPainter) {
                                draw(
                                    size = iconSize,
                                    colorFilter = ColorFilter.tint(iconColor3.copy(alpha = alpha3))
                                )
                            }
                        }
                    }
                    
                    // Icon 4: Bookmark at 180 degrees (primaryContainer color)
                    val angle4 = 180f + currentRotation
                    val rad4 = Math.toRadians(angle4.toDouble())
                    val x4 = center.x + radius * cos(rad4).toFloat()
                    val y4 = center.y + radius * sin(rad4).toFloat()
                    if (alpha4 > 0f) {
                        translate(left = x4 - halfWidth, top = y4 - halfHeight) {
                            with(bookmarkPainter) {
                                draw(
                                    size = iconSize,
                                    colorFilter = ColorFilter.tint(iconColor4.copy(alpha = alpha4))
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

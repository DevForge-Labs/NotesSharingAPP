package com.pravor.notessharing.ui.features.explore.components

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.pravor.notessharing.ui.common.rememberScrollbarState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun BoxScope.ClimbingMascotScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    mascotSize: Dp = 26.dp,
    mascot: @Composable (modifier: Modifier, isScrolling: Boolean) -> Unit
) {
    val scrollbarState = rememberScrollbarState(listState)
    
    // Custom visibility logic with 900ms delay when scrolling stops
    var isVisible by remember { mutableStateOf(false) }
    val canScroll by remember(listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val visibleItems = layoutInfo.visibleItemsInfo
            if (totalItems == 0 || visibleItems.isEmpty()) {
                false
            } else {
                val lastVisible = visibleItems.last()
                // True if total items exceed visible items or we can scroll further down
                totalItems > visibleItems.size || lastVisible.index < totalItems - 1
            }
        }
    }

    LaunchedEffect(listState.isScrollInProgress, canScroll) {
        if (listState.isScrollInProgress && canScroll) {
            isVisible = true
        } else {
            delay(900)
            isVisible = false
        }
    }

    // Alpha fade animation
    val alpha by animateFloatAsState(
        targetValue = if (isVisible && canScroll) 1f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "mascot-scrollbar-alpha"
    )

    // Mascot scale pop-in (0.9x to 1.0x)
    val scale by animateFloatAsState(
        targetValue = if (isVisible && canScroll) 1.0f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "mascot-scrollbar-scale"
    )

    // Dampened progress using a low stiffness spring to give a natural "catch up" climbing effect
    val smoothProgress by animateFloatAsState(
        targetValue = scrollbarState.progress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "mascot-scrollbar-progress"
    )

    val density = LocalDensity.current

    if (canScroll && alpha > 0.01f) {
        BoxWithConstraints(
            modifier = modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(mascotSize)
                .padding(vertical = 30.dp) // Fixed vertical padding
                .padding(end = 3.dp) // Hug the right edge (reduced from 12dp)
                .alpha(alpha)
                .zIndex(99f) // High z-index to overlay success cards
        ) {
            val trackHeightPx = with(density) { maxHeight.toPx() }
            val mascotSizePx = with(density) { mascotSize.toPx() }
            val maxOffset = (trackHeightPx - mascotSizePx).coerceAtLeast(0f)

            // Resolve line thickness in pixels
            val vineThicknessPx = with(density) { 2.dp.toPx() }
            val vineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

            // 1. Vine Track (Understated vertical line that matches theme, leaves removed to avoid stray dots)
            Canvas(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxHeight()
                    .width(2.dp)
            ) {
                // Draw thin 2dp vine track line
                drawLine(
                    color = vineColor,
                    start = Offset(size.width / 2, 0f),
                    end = Offset(size.width / 2, size.height),
                    strokeWidth = vineThicknessPx
                )
            }

            // 2. Mascot Thumb wrapper with smooth offset, alpha, and pop-in scale
            Box(
                modifier = Modifier
                    .size(mascotSize)
                    .offset { IntOffset(0, (maxOffset * smoothProgress).roundToInt()) }
                    .scale(scale)
            ) {
                // Call lambda with positional arguments (named parameters prohibited for function types)
                mascot(
                    Modifier.fillMaxSize(),
                    listState.isScrollInProgress
                )
            }
        }
    }
}

@Composable
fun MonkeyMascot(
    modifier: Modifier = Modifier,
    isScrolling: Boolean = false
) {
    // Continuous swing angle oscillator while actively scrolling
    val infiniteTransition = rememberInfiniteTransition(label = "monkey-swing")
    val rawSwingAngle by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 300, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "swing-angle-oscillation"
    )

    // Smooth return to 0 when scrolling stops
    val targetSwing = if (isScrolling) rawSwingAngle else 0f
    val animatedSwing by animateFloatAsState(
        targetValue = targetSwing,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "monkey-swing-angle"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)

        // Palette for clean silhouette
        val monkeyBrown = Color(0xFF795548)
        val faceColor = Color(0xFFF5D6C6)
        val eyeColor = Color(0xFF2E2E2E)

        // Pre-convert Dp units to pixels in DrawScope context to avoid implicit receiver failures
        val tailThicknessPx = 2.dp.toPx()
        val eyeRadiusPx = 1.2.dp.toPx()
        val smileThicknessPx = 1.dp.toPx()

        withTransform({
            // Subtle rotation (±5 degrees max)
            rotate(degrees = animatedSwing, pivot = center)
        }) {
            // a. Simple body (oval silhouette)
            drawOval(
                color = monkeyBrown,
                topLeft = Offset(w * 0.28f, h * 0.52f),
                size = Size(w * 0.44f, h * 0.38f)
            )

            // b. Minimal tail line
            val tailPath = Path().apply {
                moveTo(w * 0.5f, h * 0.85f)
                quadraticTo(w * 0.15f, h * 0.95f, w * 0.18f, h * 0.72f)
            }
            drawPath(
                path = tailPath,
                color = monkeyBrown,
                style = Stroke(width = tailThicknessPx)
            )

            // c. Simple head
            val headRadius = w * 0.28f
            val headCenter = Offset(w / 2f, h * 0.33f)
            drawCircle(
                color = monkeyBrown,
                radius = headRadius,
                center = headCenter
            )

            // d. Simple round ears
            val earRadius = headRadius * 0.4f
            val leftEarCenter = Offset(headCenter.x - headRadius * 0.85f, headCenter.y)
            val rightEarCenter = Offset(headCenter.x + headRadius * 0.85f, headCenter.y)
            // Left Ear
            drawCircle(color = monkeyBrown, radius = earRadius, center = leftEarCenter)
            drawCircle(color = faceColor, radius = earRadius * 0.55f, center = leftEarCenter)
            // Right Ear
            drawCircle(color = monkeyBrown, radius = earRadius, center = rightEarCenter)
            drawCircle(color = faceColor, radius = earRadius * 0.55f, center = rightEarCenter)

            // e. Face Mask (clean silhouette cheeks/snout)
            drawOval(
                color = faceColor,
                topLeft = Offset(headCenter.x - headRadius * 0.7f, headCenter.y - headRadius * 0.4f),
                size = Size(headRadius * 1.4f, headRadius * 1.1f)
            )

            // f. Eyes
            drawCircle(
                color = eyeColor,
                radius = eyeRadiusPx,
                center = Offset(headCenter.x - headRadius * 0.28f, headCenter.y - headRadius * 0.1f)
            )
            drawCircle(
                color = eyeColor,
                radius = eyeRadiusPx,
                center = Offset(headCenter.x + headRadius * 0.28f, headCenter.y - headRadius * 0.1f)
            )

            // g. Tiny smile
            drawArc(
                color = eyeColor,
                startAngle = 10f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(headCenter.x - headRadius * 0.2f, headCenter.y + headRadius * 0.15f),
                size = Size(headRadius * 0.4f, headRadius * 0.25f),
                style = Stroke(width = smileThicknessPx)
            )
        }
    }
}

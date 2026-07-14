package com.pravor.notessharing.ui.components.explore_components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.pravor.notessharing.ui.components.rememberScrollbarState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun SquirrelMascot(
    modifier: Modifier = Modifier,
    isScrolling: Boolean = false
) {
    // Running legs animation oscillator
    val infiniteTransition = rememberInfiniteTransition(label = "squirrel-run")
    
    val legCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "leg-cycle"
    )
    
    val tailBob by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 250, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tail-bob"
    )

    val animatedLegCycle = if (isScrolling) legCycle else 0f
    val animatedTailBob = if (isScrolling) tailBob else 0f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        val squirrelOrange = Color(0xFFE08244)
        val bellyColor = Color(0xFFF7E6D8)
        val eyeColor = Color(0xFF2E2E2E)
        val tailColor = Color(0xFFBD6123)

        // Slight body bounce when running
        val bounceY = if (isScrolling) {
            kotlin.math.sin(animatedLegCycle * 2 * kotlin.math.PI.toFloat()) * 1f
        } else {
            0f
        }

        withTransform({
            translate(top = bounceY)
        }) {
            // 1. Tail (curved fluffy tail, shifted down by 0.11f)
            val tailPath = Path().apply {
                val startX = w * 0.35f
                val startY = h * 0.76f
                moveTo(startX, startY)
                cubicTo(
                    w * 0.1f, h * 0.91f + animatedTailBob,
                    w * 0.05f, h * 0.41f + animatedTailBob,
                    w * 0.25f, h * 0.36f + animatedTailBob
                )
                cubicTo(
                    w * 0.45f, h * 0.31f + animatedTailBob,
                    w * 0.35f, h * 0.71f + animatedTailBob,
                    startX, startY
                )
            }
            drawPath(path = tailPath, color = tailColor)

            // 2. Legs (shorter, start at h * 0.81f and end at h * 0.95f)
            val legStrokeWidth = 2.dp.toPx()
            val angle1 = animatedLegCycle * 2 * kotlin.math.PI.toFloat()
            // Reduced swing radius from w * 0.12f to w * 0.07f to fit shorter legs
            val legOffset1 = kotlin.math.sin(angle1) * (w * 0.07f)
            val legOffset2 = kotlin.math.cos(angle1) * (w * 0.07f)

            // Back leg (behind body)
            drawLine(
                color = squirrelOrange,
                start = Offset(w * 0.45f, h * 0.81f),
                end = Offset(w * 0.45f + legOffset1, h * 0.95f),
                strokeWidth = legStrokeWidth
            )

            // Front leg (front of body)
            drawLine(
                color = squirrelOrange,
                start = Offset(w * 0.63f, h * 0.81f),
                end = Offset(w * 0.63f + legOffset2, h * 0.95f),
                strokeWidth = legStrokeWidth
            )

            // 3. Body (shifted down to top = h * 0.56f)
            drawOval(
                color = squirrelOrange,
                topLeft = Offset(w * 0.35f, h * 0.56f),
                size = Size(w * 0.35f, h * 0.28f)
            )

            // 4. Belly (shifted down to top = h * 0.66f)
            drawOval(
                color = bellyColor,
                topLeft = Offset(w * 0.42f, h * 0.66f),
                size = Size(w * 0.18f, h * 0.16f)
            )

            // 5. Head (shifted down to headCenter Y = h * 0.53f)
            val headCenter = Offset(w * 0.7f, h * 0.53f)
            val headRadius = w * 0.11f
            drawCircle(
                color = squirrelOrange,
                radius = headRadius,
                center = headCenter
            )

            // 6. Ear (shifted down Y coordinates by 0.11f)
            val earPath = Path().apply {
                moveTo(headCenter.x - headRadius * 0.3f, headCenter.y - headRadius * 0.8f)
                lineTo(headCenter.x, headCenter.y - headRadius * 1.5f)
                lineTo(headCenter.x + headRadius * 0.4f, headCenter.y - headRadius * 0.7f)
                close()
            }
            drawPath(path = earPath, color = squirrelOrange)

            // 7. Eye
            drawCircle(
                color = eyeColor,
                radius = 1.2.dp.toPx(),
                center = Offset(headCenter.x + headRadius * 0.3f, headCenter.y - headRadius * 0.2f)
            )
        }
    }
}

@Composable
fun BoxScope.RunningSquirrelScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    mascotSize: Dp = 26.dp
) {
    val scrollbarState = rememberScrollbarState(listState)
    
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

    val alpha by animateFloatAsState(
        targetValue = if (isVisible && canScroll) 1f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "squirrel-scrollbar-alpha"
    )

    val scale by animateFloatAsState(
        targetValue = if (isVisible && canScroll) 1.0f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "squirrel-scrollbar-scale"
    )

    val smoothProgress by animateFloatAsState(
        targetValue = scrollbarState.progress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "squirrel-scrollbar-progress"
    )

    var isRunningRight by remember { mutableStateOf(true) }
    var lastProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(scrollbarState.progress) {
        val diff = scrollbarState.progress - lastProgress
        if (diff > 0.001f) {
            isRunningRight = true
        } else if (diff < -0.001f) {
            isRunningRight = false
        }
        lastProgress = scrollbarState.progress
    }

    val density = LocalDensity.current

    if (canScroll && alpha > 0.01f) {
        BoxWithConstraints(
            modifier = modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(mascotSize + 6.dp)
                .padding(horizontal = 24.dp)
                .alpha(alpha)
                .zIndex(99f)
        ) {
            val trackWidthPx = maxWidth.value * density.density
            val mascotSizePx = mascotSize.value * density.density
            val maxOffset = (trackWidthPx - mascotSizePx).coerceAtLeast(0f)

            val lineThicknessPx = 1.5.dp.value * density.density
            val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

            // 1. Ground line
            Canvas(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(6.dp)
            ) {
                drawLine(
                    color = lineColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = lineThicknessPx
                )
            }

            // 2. Mascot Thumb wrapper
            Box(
                modifier = Modifier
                    .size(mascotSize)
                    .offset { IntOffset((maxOffset * smoothProgress).roundToInt(), 0) }
                    .scale(scaleX = if (isRunningRight) scale else -scale, scaleY = scale)
            ) {
                SquirrelMascot(
                    modifier = Modifier.fillMaxSize(),
                    isScrolling = listState.isScrollInProgress
                )
            }
        }
    }
}

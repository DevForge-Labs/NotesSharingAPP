package com.pravor.notessharing.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pravor.notessharing.ui.theme.ElectricBlue
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LiquidTransferProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    // Smooth progress fill animation
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "animated_progress"
    )

    // Liquid settling animation (wobble reduces when reaching 100%)
    val settleAlpha by animateFloatAsState(
        targetValue = if (progress >= 1f) 0f else 1f,
        animationSpec = tween(durationMillis = 800),
        label = "settle_alpha"
    )

    // Checkmark animation triggered after liquid settles at 100%
    val checkmarkAlpha by animateFloatAsState(
        targetValue = if (progress >= 1f) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 400),
        label = "checkmark_alpha"
    )

    // Infinite transitions for independent wobble & ripple phases (shaking/trembling)
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_wobble")

    val wobblePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wobble_phase"
    )

    val ripplePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_phase"
    )

    // 1. Theme Color Access Fixes (Read colors once at top of @Composable)
    val primaryColor = ElectricBlue
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant

    // 2. Density Optimization (Precompute all dp -> px conversions)
    val containerHeight = 54.dp
    val cornerRadius = 20.dp
    val cornerRadiusPx = with(density) { cornerRadius.toPx() }
    val slantPx = with(density) { 24.dp.toPx() }
    val wobbleAmpPx = with(density) { 1.5.dp.toPx() }
    val rippleAmpPx = with(density) { 0.75.dp.toPx() }
    val depthOffsetPx = with(density) { -2.dp.toPx() }
    val checkStrokeWidthPx = with(density) { 3.5.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(containerHeight)
            .clip(RoundedCornerShape(cornerRadius))
            .background(surfaceVariantColor.copy(alpha = 0.15f))
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        outlineVariantColor,
                        primaryColor.copy(alpha = 0.4f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Dynamic scale factor to flatten wave at 0% and 100%
            val boundaryScale = (4 * animatedProgress * (1 - animatedProgress)).coerceIn(0f, 1f)
            val activeMotionScale = boundaryScale * settleAlpha

            // Container clipping path for rounded corners
            val containerPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = 0f,
                        top = 0f,
                        right = width,
                        bottom = height,
                        cornerRadius = CornerRadius(cornerRadiusPx)
                    )
                )
            }

            clipPath(containerPath) {
                // 1. Draw Back Liquid Wave (Layer 1)
                val backPath = Path()
                backPath.moveTo(0f, 0f)

                val segmentCount = 20
                for (i in 0..segmentCount) {
                    val y = (i.toFloat() / segmentCount) * height
                    val normY = i.toFloat() / segmentCount

                    // Base diagonal slant
                    val slantOffset = slantPx * (normY - 0.5f)
                    // Wobble wave (trembling offset)
                    val wobble = wobbleAmpPx * sin(wobblePhase + normY * 2 * Math.PI.toFloat())

                    // Average horizontal X for back wave
                    val x = (width * animatedProgress) + (slantOffset + wobble) * activeMotionScale
                    backPath.lineTo(x.coerceIn(0f, width), y)
                }
                backPath.lineTo(0f, height)
                backPath.close()

                drawPath(
                    path = backPath,
                    color = primaryColor.copy(alpha = 0.25f)
                )

                // 2. Draw Front Liquid Wave (Layer 2)
                val frontPath = Path()
                frontPath.moveTo(0f, 0f)

                for (i in 0..segmentCount) {
                    val y = (i.toFloat() / segmentCount) * height
                    val normY = i.toFloat() / segmentCount

                    val slantOffset = slantPx * (normY - 0.5f)
                    // Ripple wave (trembling offset)
                    val ripple = rippleAmpPx * cos(ripplePhase + normY * 4 * Math.PI.toFloat())

                    val x = (width * animatedProgress) + (slantOffset + ripple + depthOffsetPx) * activeMotionScale
                    frontPath.lineTo(x.coerceIn(0f, width), y)
                }
                frontPath.lineTo(0f, height)
                frontPath.close()

                // Glow accent paint for liquid fill
                val liquidBrush = Brush.horizontalGradient(
                    colors = listOf(
                        primaryContainerColor.copy(alpha = 0.85f),
                        primaryColor
                    )
                )
                drawPath(
                    path = frontPath,
                    brush = liquidBrush
                )
            }

            // 3. Draw Animated Checkmark if progress is completed
            if (checkmarkAlpha > 0f) {
                val cx = width / 2
                val cy = height / 2
                val checkSize = height * 0.35f

                val p1x = cx - checkSize * 0.5f
                val p1y = cy
                val p2x = cx - checkSize * 0.1f
                val p2y = cy + checkSize * 0.35f
                val p3x = cx + checkSize * 0.6f
                val p3y = cy - checkSize * 0.35f

                val checkPath = Path()
                if (checkmarkAlpha <= 0.4f) {
                    val t = checkmarkAlpha / 0.4f
                    val curX = p1x + (p2x - p1x) * t
                    val curY = p1y + (p2y - p1y) * t
                    checkPath.moveTo(p1x, p1y)
                    checkPath.lineTo(curX, curY)
                } else {
                    val t = (checkmarkAlpha - 0.4f) / 0.6f
                    val curX = p2x + (p3x - p2x) * t
                    val curY = p2y + (p3y - p2y) * t
                    checkPath.moveTo(p1x, p1y)
                    checkPath.lineTo(p2x, p2y)
                    checkPath.lineTo(curX, curY)
                }

                drawPath(
                    path = checkPath,
                    color = Color.White,
                    style = Stroke(
                        width = checkStrokeWidthPx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        // 4. Centered smoothly animated percentage text
        if (checkmarkAlpha < 1f) {
            val displayPct = (animatedProgress * 100).toInt()
            val textAlpha = (1f - checkmarkAlpha).coerceIn(0f, 1f)

            Text(
                text = if (displayPct >= 100) "100%" else "$displayPct%",
                color = Color.White.copy(alpha = textAlpha),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 1.dp)
            )
        }
    }
}

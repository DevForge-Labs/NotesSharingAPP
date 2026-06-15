package com.pravor.notessharing.ui.components.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.pravor.notessharing.ui.components.navigation.model.WaterWaveState
import kotlin.math.cos
import kotlin.math.sin

private data class BubbleConfig(
    val percentX: Float,
    val speedDp: Float,
    val radiusDp: Float,
    val driftScaleDp: Float,
    val driftFreq: Float,
    val phaseOffset: Float
)

@Composable
fun WaterSurface(
    waveState: WaterWaveState,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Recycle Path objects to avoid allocation on every single frame
    val containerPath = remember { Path() }
    val waterPath = remember { Path() }
    val surfaceLinePath = remember { Path() }

    // 12 unique bubble configurations for diverse, randomized paths
    val bubbleConfigs = remember {
        listOf(
            BubbleConfig(0.12f, 22f, 2.2f, 16f, 1.8f, 0.0f),
            BubbleConfig(0.25f, 15f, 1.6f, 22f, 1.2f, 1.5f),
            BubbleConfig(0.38f, 28f, 3.0f, 12f, 2.4f, 3.1f),
            BubbleConfig(0.50f, 18f, 2.0f, 26f, 1.5f, 4.8f),
            BubbleConfig(0.62f, 24f, 2.6f, 14f, 2.0f, 2.2f),
            BubbleConfig(0.75f, 13f, 1.4f, 30f, 1.0f, 0.7f),
            BubbleConfig(0.88f, 26f, 2.8f, 18f, 2.2f, 5.5f),
            BubbleConfig(0.20f, 20f, 2.4f, 15f, 1.6f, 2.7f),
            BubbleConfig(0.45f, 16f, 1.8f, 20f, 1.3f, 1.1f),
            BubbleConfig(0.70f, 25f, 2.8f, 16f, 2.1f, 3.9f),
            BubbleConfig(0.82f, 19f, 2.0f, 24f, 1.4f, 0.3f),
            BubbleConfig(0.95f, 14f, 1.5f, 12f, 1.8f, 4.2f)
        )
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Check for invalid, NaN, or zero canvas sizes defensively
        if (width <= 0f || height <= 0f || width.isNaN() || height.isNaN()) return@Canvas

        // Access target heights tickTrigger to trigger Canvas redraw
        val trigger = waveState.tickTrigger
        val numColumns = waveState.numColumns
        val columnWidth = width / (numColumns - 1)

        containerPath.reset()
        containerPath.addRoundRect(
            roundRect = RoundRect(
                left = 0f,
                top = 0f,
                right = width,
                bottom = height,
                cornerRadius = CornerRadius(30.dp.toPx(), 30.dp.toPx())
            )
        )

        // 1. Draw premium glass container back wall (inside the vessel)
        val glassBackingBrush = Brush.verticalGradient(
            colors = if (isDark) {
                listOf(
                    Color(0xFF0F2540).copy(alpha = 0.55f),
                    Color(0xFF071A2E).copy(alpha = 0.95f)
                )
            } else {
                listOf(
                    Color(0xFFE4ECF5).copy(alpha = 0.65f),
                    Color(0xFFF0F4F8).copy(alpha = 0.95f)
                )
            },
            startY = 0f,
            endY = height
        )
        drawPath(path = containerPath, brush = glassBackingBrush)

        // 2. Build the water body path safely
        waterPath.reset()
        waterPath.moveTo(0f, height)
        waterPath.lineTo(0f, waveState.heights[0].coerceInSafe(0f, height))
        for (i in 1 until numColumns) {
            waterPath.lineTo(i * columnWidth, waveState.heights[i].coerceInSafe(0f, height))
        }
        waterPath.lineTo(width, height)
        waterPath.close()

        // Clip the water rendering to the rounded container
        clipPath(containerPath) {
            // A. Draw main water body
            val waterGradient = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF5EE7FF).copy(alpha = 0.28f), // Translucent light cyan
                    Color(0xFF4CC9F0).copy(alpha = 0.24f), // Translucent mid-top cyan
                    Color(0xFF3A86FF).copy(alpha = 0.20f), // Translucent electric blue
                    Color(0xFF4361EE).copy(alpha = 0.24f), // Translucent royal blue base
                    Color(0xFF5A189A).copy(alpha = 0.30f)  // Translucent deep purple depth
                ),
                startY = (waveState.heights.minOrNull() ?: 0f).coerceInSafe(0f, height),
                endY = height
            )
            drawPath(path = waterPath, brush = waterGradient)

            // B. Draw internal depth reflections & caustics (inside the water)
            clipPath(waterPath) {
                val time = waveState.timeAccumulator

                // Overlapping caustic reflections
                val c1X = width * (0.26f + 0.12f * sin(time * 0.15f))
                val c1Y = height * (0.55f + 0.10f * cos(time * 0.20f))
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF5EE7FF).copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(c1X, c1Y),
                        radius = 50.dp.toPx()
                    ),
                    radius = 50.dp.toPx(),
                    center = Offset(c1X, c1Y)
                )

                val c2X = width * (0.74f + 0.10f * cos(time * 0.12f))
                val c2Y = height * (0.65f + 0.08f * sin(time * 0.18f))
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF4CC9F0).copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(c2X, c2Y),
                        radius = 65.dp.toPx()
                    ),
                    radius = 65.dp.toPx(),
                    center = Offset(c2X, c2Y)
                )

                // C. Tiny slow bubbles trapped under the surface (rising and drifting dynamically in random directions)
                for (config in bubbleConfigs) {
                    val radius = config.radiusDp.dp.toPx()
                    val speedPx = config.speedDp.dp.toPx()
                    val driftScalePx = config.driftScaleDp.dp.toPx()

                    // Multi-directional organic drift (sine & cosine combinations)
                    val driftX = driftScalePx * sin(time * config.driftFreq + config.phaseOffset)
                    val bx = (config.percentX * width + driftX).coerceInSafe(radius + 2f, width - radius - 2f)
                    
                    val colIndex = ((bx / width) * (numColumns - 1)).toInt().coerceIn(0, numColumns - 1)
                    val surfaceY = waveState.heights[colIndex]

                    // Calculate bubble ascent path within the water volume
                    val waterDepth = height - surfaceY - radius - 4f
                    if (waterDepth > 5f) {
                        val cycleDuration = waterDepth / speedPx
                        val phase = (time + config.phaseOffset) % cycleDuration
                        val yOffset = phase * speedPx
                        
                        // Add organic vertical jitter to simulate natural fluid dynamics
                        val verticalJitter = (3f * cos(time * (config.driftFreq * 1.5f) + config.phaseOffset)).dp.toPx()
                        val by = (height - radius - 2f - yOffset + verticalJitter).coerceInSafe(surfaceY + radius + 2f, height - radius)

                        // Bubble outline
                        drawCircle(
                            color = Color.White.copy(alpha = 0.30f),
                            radius = radius,
                            center = Offset(bx, by),
                            style = Stroke(width = 0.8.dp.toPx())
                        )
                        // Bubble core glow
                        drawCircle(
                            color = Color(0xFF5EE7FF).copy(alpha = 0.12f),
                            radius = radius * 0.7f,
                            center = Offset(bx, by)
                        )
                    }
                }
            }

            // D. Draw water surface lines (Liquid-Air Interface)
            surfaceLinePath.reset()
            surfaceLinePath.moveTo(0f, waveState.heights[0].coerceInSafe(0f, height))
            for (i in 1 until numColumns) {
                surfaceLinePath.lineTo(i * columnWidth, waveState.heights[i].coerceInSafe(0f, height))
            }

            // Surface Glow (soft underlying cyan highlight)
            drawPath(
                path = surfaceLinePath,
                color = Color(0xFF5EE7FF).copy(alpha = 0.28f),
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Surface Reflection Line (sharp bright white crest)
            drawPath(
                path = surfaceLinePath,
                color = Color.White.copy(alpha = 0.85f),
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // 3. Draw top bevel reflection of the glass container itself
        val topGlossBrush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.32f),
                Color.White.copy(alpha = 0.06f),
                Color.Transparent
            ),
            startY = 0f,
            endY = 8.dp.toPx()
        )
        drawRect(
            brush = topGlossBrush,
            topLeft = Offset.Zero,
            size = Size(width, 8.dp.toPx())
        )
    }
}

/**
 * Defensive coerceIn implementation that handles cases where minimumValue > maximumValue
 * or values are NaN/infinite due to canvas layout scaling, preventing application crashes.
 */
private fun Float.coerceInSafe(minimumValue: Float, maximumValue: Float): Float {
    if (this.isNaN()) return 0f
    
    val min = if (minimumValue.isNaN() || minimumValue.isInfinite()) 0f else minimumValue
    val max = if (maximumValue.isNaN() || maximumValue.isInfinite()) 0f else maximumValue
    
    val lower = minOf(min, max)
    val upper = maxOf(min, max)
    
    return this.coerceIn(lower, upper)
}

package com.pravor.notessharing.ui.common

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import kotlin.math.sin
import kotlin.random.Random

private const val PI_F = 3.1415927f
private const val TWO_PI_F = 6.2831855f

@Immutable
private data class ProgressBubble(
    val id: Int,
    val xPercent: Float,
    val yPercent: Float,
    val speed: Float,
    val radiusDp: Float,
    val alpha: Float
)

/**
 * Premium animated liquid-water container progress bar.
 * Simulates water coming in from the left and flowing/filling naturally to the right:
 * strong natural fluid slant (bottom surges forward along the container floor),
 * organic right-edge ripples, visible floating glassy bubbles, and Profile-matching deep teal/turquoise palette.
 */
@Composable
fun LiquidTransferProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    statusText: String? = null,
    showSpinner: Boolean = false,
    showCheckmarkOnComplete: Boolean = false
) {
    val density = LocalDensity.current

    // Smooth progress fill transition
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "animated_liquid_progress"
    )

    // Settle ripples into a smooth calm surface as progress completes
    val settleAlpha by animateFloatAsState(
        targetValue = if (progress >= 1f) 0f else 1f,
        animationSpec = tween(durationMillis = 700),
        label = "liquid_settle_alpha"
    )

    // Wave animation transitions (pure Float limits for 60/120fps smoothness)
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_container_waves")

    val backWavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI_F,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "back_wave_phase"
    )

    val frontWavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI_F,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "front_wave_phase"
    )

    val bubbleSwayPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI_F,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubble_sway_phase"
    )

    // Realistic floating glassy bubbles inside the water volume
    val bubbleCount = 9
    var bubbleList by remember {
        mutableStateOf(
            List(bubbleCount) { id ->
                ProgressBubble(
                    id = id,
                    xPercent = Random.nextFloat(),
                    yPercent = Random.nextFloat(),
                    speed = Random.nextFloat() * 0.010f + 0.005f,
                    radiusDp = Random.nextFloat() * 1.8f + 1.2f, // 1.2dp to 3.0dp
                    alpha = Random.nextFloat() * 0.40f + 0.35f
                )
            }
        )
    }

    // Floating bubble animation frame loop (bubbles rise upward and drift with the water)
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis {
                bubbleList = bubbleList.map { bubble ->
                    var newY = bubble.yPercent - bubble.speed
                    var newX = bubble.xPercent + 0.001f // subtle rightward drift with water flow
                    if (newY < 0f) {
                        newY = 1.0f
                        newX = Random.nextFloat()
                    }
                    if (newX > 1.0f) newX = 0f
                    bubble.copy(yPercent = newY, xPercent = newX)
                }
            }
        }
    }

    // Profile-matching liquid color system: Deep dark teal, rich turquoise, soft cyan & restrained highlights
    val surfaceTurquoise = Color(0xFF26C6DA)
    val midTeal = Color(0xFF00838F)
    val deepOceanTeal = Color(0xFF004D40)
    val darkUnderwaterAbyss = Color(0xFF06202A)
    val backWaveTeal = Color(0xFF103B48)
    val surfaceHighlight = Color(0xFF80DEEA)

    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant

    // Precomputed metrics
    val containerHeight = 54.dp
    val cornerRadius = 20.dp
    val cornerRadiusPx = with(density) { cornerRadius.toPx() }
    val slantSpanPx = with(density) { 34.dp.toPx() } // Smooth natural forward fluid curve
    val amp1Px = with(density) { 1.5.dp.toPx() } // Gentle coherent fluid pulse
    val amp2Px = with(density) { 0.8.dp.toPx() }
    val crestStrokeWidthPx = with(density) { 1.8.dp.toPx() }

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
                        surfaceTurquoise.copy(alpha = 0.35f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Base progress fill position (left to right)
            val fillWidth = width * animatedProgress

            // Boundary motion envelope (smoothly flattens at 0% and 100%)
            val boundaryScale = if (animatedProgress <= 0f) {
                0f
            } else {
                (minOf(1f, animatedProgress * 8f) * minOf(1f, (1f - animatedProgress) * 8f)).coerceIn(0f, 1f)
            }
            val activeMotionScale = boundaryScale * settleAlpha

            // Container clipping path for rounded corners
            val containerPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(Offset.Zero, size),
                        cornerRadius = CornerRadius(cornerRadiusPx)
                    )
                )
            }

            // Ambient internal liquid glow
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        deepOceanTeal.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.5f, height * 0.5f),
                    radius = width * 0.7f
                )
            )

            clipPath(containerPath) {
                if (animatedProgress > 0f && fillWidth > 0f) {
                    val segments = 32

                    // Helper to calculate the advancing fluid front X at vertical level normY (0 = top, 1 = bottom):
                    // Forms a complete, smooth, continuous, rounded fluid boundary (top: shorter, mid: smooth, bottom: surges forward)
                    fun getFrontEdgeX(normY: Float): Float {
                        // Smooth, monotonic natural fluid meniscus curve (no random bumps on top)
                        val fluidMeniscus = slantSpanPx * (normY * 0.85f + 0.15f * sin(normY * PI_F) - 0.40f)
                        // Smooth, coherent fluid wave undulation across the whole boundary
                        val wave1 = amp1Px * sin(frontWavePhase + normY * 1.2f * PI_F)
                        val wave2 = amp2Px * sin(frontWavePhase * 1.5f + 0.8f)
                        val coherentMotion = (wave1 + wave2) * activeMotionScale
                        return (fillWidth + fluidMeniscus + coherentMotion).coerceIn(0f, width)
                    }

                    // 1. BACK WAVE (Layer 1 - Deeper underwater parallax refraction)
                    val backPath = Path()
                    backPath.moveTo(0f, 0f)
                    for (i in 0..segments) {
                        val normY = i.toFloat() / segments
                        val y = normY * height
                        val backMeniscus = slantSpanPx * 0.95f * (normY * 0.85f + 0.15f * sin(normY * PI_F) - 0.40f)
                        val bw1 = amp1Px * 0.75f * sin(backWavePhase + normY * 1.2f * PI_F + 0.6f)
                        val bw2 = amp2Px * 0.70f * sin(backWavePhase * 1.5f + 1.2f)
                        val backX = (fillWidth - 3.dp.toPx() + backMeniscus + (bw1 + bw2) * activeMotionScale).coerceIn(0f, width)
                        backPath.lineTo(backX, y)
                    }
                    backPath.lineTo(0f, height)
                    backPath.close()

                    drawPath(
                        path = backPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                backWaveTeal.copy(alpha = 0.50f),
                                deepOceanTeal.copy(alpha = 0.60f)
                            )
                        )
                    )

                    // 2. FRONT WAVE (Layer 2 - Main active liquid body: deep teal bottom -> turquoise surface)
                    val frontPath = Path()
                    val crestPath = Path()
                    frontPath.moveTo(0f, 0f)

                    var firstPoint = true
                    for (i in 0..segments) {
                        val normY = i.toFloat() / segments
                        val y = normY * height
                        val x = getFrontEdgeX(normY)

                        frontPath.lineTo(x, y)
                        if (firstPoint) {
                            crestPath.moveTo(x, y)
                            firstPoint = false
                        } else {
                            crestPath.lineTo(x, y)
                        }
                    }
                    frontPath.lineTo(0f, height)
                    frontPath.close()

                    drawPath(
                        path = frontPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                surfaceTurquoise.copy(alpha = 0.82f),
                                midTeal.copy(alpha = 0.88f),
                                deepOceanTeal.copy(alpha = 0.94f),
                                darkUnderwaterAbyss.copy(alpha = 0.98f)
                            ),
                            startY = 0f,
                            endY = height
                        )
                    )

                    // 3. REALISTIC FLOATING GLASSY BUBBLES inside the water volume
                    bubbleList.forEach { bubble ->
                        val normY = bubble.yPercent
                        val edgeAtY = getFrontEdgeX(normY)
                        if (edgeAtY > 12f) {
                            val bubbleY = normY * height
                            val sway = sin(bubbleSwayPhase + bubble.id.toFloat()) * 2.8.dp.toPx()
                            val bubbleX = (bubble.xPercent * (edgeAtY - 8f) + sway).coerceIn(4f, edgeAtY - 2f)

                            val radiusPx = bubble.radiusDp.dp.toPx()
                            // Outer glassy bubble ring
                            drawCircle(
                                color = Color.White.copy(alpha = bubble.alpha),
                                radius = radiusPx,
                                center = Offset(bubbleX, bubbleY)
                            )
                            // Inner soft glow
                            drawCircle(
                                color = surfaceHighlight.copy(alpha = bubble.alpha * 0.45f),
                                radius = radiusPx * 0.7f,
                                center = Offset(bubbleX, bubbleY)
                            )
                            // Specular crest highlight
                            drawCircle(
                                color = Color.White.copy(alpha = bubble.alpha * 0.85f),
                                radius = radiusPx * 0.35f,
                                center = Offset(bubbleX - radiusPx * 0.3f, bubbleY - radiusPx * 0.3f)
                            )
                        }
                    }

                    // 4. SURFACE REFLECTION SHINE along the advancing fluid front
                    if (activeMotionScale > 0.05f) {
                        drawPath(
                            path = crestPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.70f),
                                    surfaceHighlight.copy(alpha = 0.80f),
                                    Color.White.copy(alpha = 0.35f)
                                ),
                                startY = 0f,
                                endY = height
                            ),
                            style = Stroke(
                                width = crestStrokeWidthPx,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                // 5. Glassmorphism Top Shine Highlight
                val topShinePath = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(width, 0f)
                    lineTo(width, height * 0.20f)
                    quadraticTo(
                        width * 0.5f,
                        height * 0.30f,
                        0f,
                        height * 0.20f
                    )
                    close()
                }
                drawPath(
                    path = topShinePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
            }
        }

        // Foreground content: Clean Percentage ONLY with crisp text shadow
        val textShadow = Shadow(
            color = Color.Black.copy(alpha = 0.85f),
            offset = Offset(1.5f, 1.5f),
            blurRadius = 4f
        )

        val displayText = statusText ?: "${(animatedProgress * 100).toInt()}%"

        Text(
            text = displayText,
            color = Color.White,
            fontSize = 17.sp,
            style = MaterialTheme.typography.titleMedium.copy(shadow = textShadow),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}



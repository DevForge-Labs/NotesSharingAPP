package com.pravor.notessharing.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pravor.notessharing.model.Profile
import com.pravor.notessharing.model.calculateLevelProgress
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// High-performance pure Float approximation of PI
private const val PI_F = 3.1415927f
private const val TWO_PI_F = 6.2831855f
private const val HALF_PI_F = 1.5707963f

@Immutable
private data class Bubble(
    val id: Int,
    val xPercent: Float,
    val yPercent: Float,
    val speed: Float,
    val radiusDp: Float,
    val alpha: Float
)

@Composable
fun LiquidContributorCard(
    profile: Profile,
    modifier: Modifier = Modifier
) {
    val progressInfo = calculateLevelProgress(profile.uploads)
    
    // Level transition animation state
    var displayedProgress by remember { mutableFloatStateOf(progressInfo.progress) }
    var displayedLevel by remember { mutableIntStateOf(progressInfo.currentLevel) }
    var isTransitioning by remember { mutableStateOf(false) }
    
    // Listen to level changes to trigger drain-then-refill animation
    LaunchedEffect(progressInfo.currentLevel) {
        if (displayedLevel != progressInfo.currentLevel) {
            isTransitioning = true
            // 1. Drain smoothly to 0%
            animate(
                initialValue = displayedProgress,
                targetValue = 0f,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
            ) { value, _ ->
                displayedProgress = value
            }
            // 2. Pause briefly (e.g. 250ms)
            delay(250)
            // 3. Update the level text/badge
            displayedLevel = progressInfo.currentLevel
            // 4. Refill to new target progress
            animate(
                initialValue = 0f,
                targetValue = progressInfo.progress,
                animationSpec = tween(durationMillis = 1100, easing = LinearOutSlowInEasing)
            ) { value, _ ->
                displayedProgress = value
            }
            isTransitioning = false
        } else {
            displayedProgress = progressInfo.progress
        }
    }
    
    // Listen to progress changes if level remains the same
    LaunchedEffect(progressInfo.progress) {
        if (!isTransitioning && displayedLevel == progressInfo.currentLevel) {
            animate(
                initialValue = displayedProgress,
                targetValue = progressInfo.progress,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) { value, _ ->
                displayedProgress = value
            }
        }
    }

    // Colors customized per level to add neon glows matching contributor level identity
    val levelColor = when (displayedLevel) {
        1 -> Color(0xFFCD7F32) // Bronze
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFFFD700) // Gold
        4 -> Color(0xFF00E5FF) // Cyan Platinum
        else -> Color(0xFFD500F9) // Purple Mythic
    }

    val baseCardBg = Color(0xFF0D1117)
    val textShadow = Shadow(
        color = Color.Black.copy(alpha = 0.75f),
        offset = Offset(2f, 2f),
        blurRadius = 6f
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(28.dp),
                clip = false,
                ambientColor = levelColor.copy(alpha = 0.2f),
                spotColor = levelColor.copy(alpha = 0.35f)
            )
            .border(
                BorderStroke(
                    width = 1.2.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.03f)
                        )
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = baseCardBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            // 1. Water waves background + Sensor-based motion sloshing
            LiquidBackground(
                progress = displayedProgress,
                levelColor = levelColor
            )

            // 2. Glassmorphism overlay highlights & top reflection shine
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Top shine highlight
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, size.height * 0.18f)
                    quadraticTo(
                        size.width * 0.5f,
                        size.height * 0.28f,
                        0f,
                        size.height * 0.18f
                    )
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.0f)
                        )
                    )
                )

                // Subtle inner glow border
                val innerBorderPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(Offset.Zero, size),
                            cornerRadius = CornerRadius(28.dp.toPx())
                        )
                    )
                }
                drawPath(
                    path = innerBorderPath,
                    color = Color.White.copy(alpha = 0.05f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // 3. Card Content Layer (ABOVE liquid layer and fully readable)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top row: badge & level title + upload status badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            levelColor.copy(alpha = 0.35f),
                                            levelColor.copy(alpha = 0.08f)
                                        )
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(1.dp, levelColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = "Level Badge",
                                tint = levelColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Level $displayedLevel Contributor",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    shadow = textShadow
                                )
                            )
                            Text(
                                text = "${profile.upvotes} upvotes earned",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    shadow = textShadow
                                )
                            )
                        }
                    }

                    // Floating Glass Capsule showing upload count
                    Box(
                        modifier = Modifier
                            .background(
                                Color.Black.copy(alpha = 0.35f),
                                RoundedCornerShape(12.dp)
                            )
                            .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "${profile.uploads} Uploads",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                shadow = textShadow
                            )
                        )
                    }
                }

                // Bottom section: Progress indicator detail & XP requirements
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (displayedLevel == 5) "Max level reached!" else "Progress to Level ${displayedLevel + 1}",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                shadow = textShadow
                            )
                        )

                        Text(
                            text = "${(displayedProgress * 100).toInt()}%",
                            color = levelColor,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                shadow = textShadow
                            )
                        )
                    }

                    // Progress detail subtitle
                    val xpRemaining = if (displayedLevel == 5) {
                        "Congratulations, elite status achieved!"
                    } else {
                        val needed = progressInfo.targetUploads - profile.uploads
                        "$needed more upload${if (needed > 1) "s" else ""} needed for Level ${progressInfo.nextLevel}"
                    }

                    Text(
                        text = xpRemaining,
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            shadow = textShadow
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// Reusable static Float calculation for wave height - pure Float calculations
private fun calculateWaveHeight(
    x: Float,
    width: Float,
    height: Float,
    phase: Float,
    progress: Float,
    amplitude: Float,
    tilt: Float,
    multiplier: Float,
    phaseOffset: Float
): Float {
    val normX = x / width
    val wave = sin(phase + normX * TWO_PI_F * multiplier + phaseOffset) * amplitude
    val tiltOffset = (x - width * 0.5f) * -tilt
    val baseWaterY = height * (1f - progress)
    return (baseWaterY + tiltOffset + wave).coerceIn(0f, height)
}

@Composable
private fun LiquidBackground(
    progress: Float,
    levelColor: Color
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    
    // Wave animation phases (uses pure Float limits to avoid promotions to Double)
    val infiniteTransition = rememberInfiniteTransition(label = "wave_physics")
    val backWavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI_F,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "back_wave_phase"
    )
    
    val frontWavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI_F,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
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

    // Accelerometer variables - optimized with mutableFloatStateOf
    var rawSensorX by remember { mutableFloatStateOf(0f) }
    var rawSensorY by remember { mutableFloatStateOf(9.8f) }
    var shakeIntensity by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        var lastX = 0f
        var lastY = 9.8f
        var lastShakeTime = 0L
        val shakeThreshold = 2.0f // Threshold for detecting shakes
        val shakeCooldownMs = 400L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                val rx = event.values[0]
                val ry = event.values[1]
                
                // Low-pass filter for smooth tilt responses
                rawSensorX = rawSensorX * 0.9f + rx * 0.1f
                rawSensorY = rawSensorY * 0.9f + ry * 0.1f
                
                // Shake detection based on jerk (delta-acceleration) with cooldown logic
                val dx = rx - lastX
                val dy = ry - lastY
                val jerk = sqrt(dx * dx + dy * dy)
                val now = System.currentTimeMillis()
                if (jerk > shakeThreshold && now - lastShakeTime > shakeCooldownMs) {
                    shakeIntensity = (shakeIntensity + jerk * 0.12f).coerceIn(0f, 1f)
                    lastShakeTime = now
                }

                lastX = rx
                lastY = ry
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    // Smooth physics-based tilt and shake decay using animateFloatAsState
    val animatedTilt by animateFloatAsState(
        targetValue = (-rawSensorX / 9.81f).coerceIn(-0.4f, 0.4f),
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "animated_tilt"
    )

    // Decays the shake intensity back to 0 over time
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameMillis {
                if (shakeIntensity > 0f) {
                    shakeIntensity = maxOf(0f, shakeIntensity - 0.015f)
                }
            }
        }
    }

    val animatedShakeAmp by animateFloatAsState(
        targetValue = shakeIntensity,
        animationSpec = spring(stiffness = Spring.StiffnessVeryLow, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "animated_shake"
    )

    // Precompute densities
    val baseWaveAmp = with(density) { 5.dp.toPx() }
    val shakeAmpBonus = with(density) { 15.dp.toPx() }
    val bubbleCount = (10 + (progress * 15f).toInt()).coerceIn(10, 25)

    // Particle Bubbles initialization
    var bubbleList by remember(bubbleCount) {
        mutableStateOf(
            List(bubbleCount) { id ->
                Bubble(
                    id = id,
                    xPercent = Random.nextFloat(),
                    yPercent = Random.nextFloat(),
                    speed = Random.nextFloat() * 0.008f + 0.004f,
                    radiusDp = Random.nextFloat() * 2.5f + 1.2f,
                    alpha = Random.nextFloat() * 0.5f + 0.25f
                )
            }
        )
    }

    // Run bubble float animation loop
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameMillis {
                bubbleList = bubbleList.map { bubble ->
                    var newY = bubble.yPercent - bubble.speed * (1f + animatedShakeAmp * 2f)
                    var newX = bubble.xPercent
                    if (newY < 0f) {
                        newY = 1.0f
                        newX = Random.nextFloat()
                    }
                    bubble.copy(yPercent = newY, xPercent = newX)
                }
            }
        }
    }

    // Preallocated Path objects to avoid allocation in DrawScope
    val backPath = remember { Path() }
    val frontPath = remember { Path() }
    val surfaceShinePath = remember { Path() }

    // Light glowing color palette
    val cyanGlow = Color(0xFF00F5FF)
    val aquaBlue = Color(0xFF00EBFF)
    val softTeal = Color(0xFF5DF9E2)
    val purpleTint = Color(0xFF9E8FFF)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
    ) {
        val width = size.width
        val height = size.height
        val cornerRadiusPx = 28.dp.toPx()

        val boundaryScale = sin(progress * PI_F).coerceIn(0f, 1f)
        val activeAmplitude = (baseWaveAmp + animatedShakeAmp * shakeAmpBonus) * boundaryScale

        // Draw background subtle color glow
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    levelColor.copy(alpha = 0.12f),
                    Color.Transparent
                ),
                center = Offset(width * 0.5f, height * 0.7f),
                radius = width * 0.8f
            )
        )

        val containerPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(Offset.Zero, size),
                    cornerRadius = CornerRadius(cornerRadiusPx)
                )
            )
        }

        clipPath(containerPath) {
            if (progress > 0f) {
                // 1. BACK WAVE (Layer 1 - Deep soft glow)
                backPath.reset()
                backPath.moveTo(0f, height)
                backPath.lineTo(0f, calculateWaveHeight(0f, width, height, backWavePhase, progress, activeAmplitude, animatedTilt, 1.1f, 0f))
                val segments = 30
                for (i in 1..segments) {
                    val x = (i.toFloat() / segments) * width
                    val y = calculateWaveHeight(x, width, height, backWavePhase, progress, activeAmplitude, animatedTilt, 1.1f, 0f)
                    backPath.lineTo(x, y)
                }
                backPath.lineTo(width, height)
                backPath.close()

                drawPath(
                    path = backPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            purpleTint.copy(alpha = 0.28f),
                            softTeal.copy(alpha = 0.45f)
                        )
                    )
                )

                // 2. FRONT WAVE (Layer 2 - Active liquid foreground)
                frontPath.reset()
                frontPath.moveTo(0f, height)
                frontPath.lineTo(0f, calculateWaveHeight(0f, width, height, frontWavePhase, progress, activeAmplitude, animatedTilt, 1.5f, HALF_PI_F))
                for (i in 1..segments) {
                    val x = (i.toFloat() / segments) * width
                    val y = calculateWaveHeight(x, width, height, frontWavePhase, progress, activeAmplitude, animatedTilt, 1.5f, HALF_PI_F)
                    frontPath.lineTo(x, y)
                }
                frontPath.lineTo(width, height)
                frontPath.close()

                drawPath(
                    path = frontPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            cyanGlow.copy(alpha = 0.78f),
                            aquaBlue.copy(alpha = 0.85f),
                            levelColor.copy(alpha = 0.15f)
                        )
                    )
                )

                // 3. Floating Bubbles Layer
                bubbleList.forEach { bubble ->
                    val bubbleX = bubble.xPercent * width
                    // Front wave surface at bubbleX
                    val surfaceY = calculateWaveHeight(bubbleX, width, height, frontWavePhase, progress, activeAmplitude, animatedTilt, 1.5f, HALF_PI_F)
                    
                    // Bubble is placed relative to height and surfaceY
                    val bubbleY = surfaceY + (height - surfaceY) * bubble.yPercent

                    // Sways bubbles side-to-side for organic rising motion
                    val sway = sin(bubbleSwayPhase + bubble.id.toFloat()) * 4.dp.toPx()
                    val finalX = (bubbleX + sway).coerceIn(0f, width)

                    // Bubble fades out as it nears surface
                    val alphaFade = if (bubble.yPercent < 0.2f) bubble.yPercent / 0.2f else 1.0f
                    val finalAlpha = bubble.alpha * alphaFade

                    if (bubbleY < height) {
                        // Glassy Bubble border/circle
                        drawCircle(
                            color = Color.White.copy(alpha = finalAlpha),
                            radius = bubble.radiusDp.dp.toPx(),
                            center = Offset(finalX, bubbleY)
                        )

                        // Highlight sheen inside the bubble
                        drawCircle(
                            color = Color.White.copy(alpha = finalAlpha * 0.6f),
                            radius = (bubble.radiusDp * 0.4f).dp.toPx(),
                            center = Offset(finalX - (bubble.radiusDp * 0.2f).dp.toPx(), bubbleY - (bubble.radiusDp * 0.2f).dp.toPx())
                        )
                    }
                }

                // 4. Subtle Liquid Reflection Shine overlay on wave surface
                surfaceShinePath.reset()
                surfaceShinePath.moveTo(0f, calculateWaveHeight(0f, width, height, frontWavePhase, progress, activeAmplitude, animatedTilt, 1.5f, HALF_PI_F))
                for (i in 1..segments) {
                    val x = (i.toFloat() / segments) * width
                    val y = calculateWaveHeight(x, width, height, frontWavePhase, progress, activeAmplitude, animatedTilt, 1.5f, HALF_PI_F)
                    surfaceShinePath.lineTo(x, y)
                }
                drawPath(
                    path = surfaceShinePath,
                    color = Color.White.copy(alpha = 0.25f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }
    }
}

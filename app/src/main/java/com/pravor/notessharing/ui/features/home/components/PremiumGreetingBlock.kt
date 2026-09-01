package com.pravor.notessharing.ui.features.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.pravor.notessharing.ui.theme.ElectricBlue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

sealed interface SmartBannerState {
    object GreetingMode : SmartBannerState
}

@Composable
fun SmartBannerSlot(
    modifier: Modifier = Modifier,
    state: SmartBannerState = SmartBannerState.GreetingMode,
    shouldPlayWave: Boolean = false,
    onWaveCompleted: () -> Unit = {}
) {
    when (state) {
        is SmartBannerState.GreetingMode -> {
            PremiumGreetingBlock(
                modifier = modifier,
                shouldPlayWave = shouldPlayWave,
                onWaveCompleted = onWaveCompleted
            )
        }
    }
}

@Composable
private fun PremiumGreetingBlock(
    modifier: Modifier = Modifier,
    shouldPlayWave: Boolean = false,
    onWaveCompleted: () -> Unit = {}
) {
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val baseGreeting = when (currentHour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }

    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val displayName = remember(currentUser) {
        val rawName = currentUser?.displayName?.trim()?.split(" ")?.firstOrNull()
        rawName?.lowercase()?.replaceFirstChar { it.uppercase() }?.takeIf { it.isNotEmpty() }
    }

    val greeting = if (displayName != null) {
        "$baseGreeting, $displayName"
    } else {
        baseGreeting
    }

    val subtitle = remember { curatedOneLiners.random() }

    val cardAlpha = remember { Animatable(0f) }
    val cardOffsetY = remember { Animatable(16f) }
    val cardScale = remember { Animatable(0.98f) }
    
    val subtitleAlpha = remember { Animatable(0f) }
    val subtitleOffsetY = remember { Animatable(6f) }

    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        launch {
            cardAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            )
        }
        launch {
            cardOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            )
        }
        launch {
            cardScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            )
        }
        
        delay(90)
        
        launch {
            subtitleAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
            )
        }
        launch {
            subtitleOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
            )
        }
    }

    val cardBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF13202C).copy(alpha = 0.74f),
            Color(0xFF0B131A).copy(alpha = 0.78f)
        )
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .graphicsLayer {
                alpha = cardAlpha.value
                translationY = with(density) { cardOffsetY.value.dp.toPx() }
                scaleX = cardScale.value
                scaleY = cardScale.value
            },
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.38f)),
        color = Color.Transparent,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush)
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Section: Avatar | Time Salutation + Name Wave
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. User Avatar with Electric Blue Accent Stroke
                HomeUserAvatar(
                    photoUrl = currentUser?.photoUrl?.toString(),
                    displayName = displayName ?: currentUser?.email
                )

                // 2. Salutation & Name with Waving Hand
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = baseGreeting,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.5.sp,
                            letterSpacing = 0.2.sp,
                            lineHeight = 17.sp
                        ),
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = displayName ?: "Scholar",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.5.sp,
                                letterSpacing = 0.2.sp,
                                lineHeight = 23.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Spacer(Modifier.width(5.dp))

                        WavingHandEmoji(
                            shouldAnimate = shouldPlayWave,
                            onAnimationEnd = onWaveCompleted
                        )
                    }
                }
            }

            // Bottom Section: Independent Actual Greeting / Motivational Quote
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.50f),
                border = BorderStroke(0.75.dp, ElectricBlue.copy(alpha = 0.18f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = subtitleAlpha.value
                        translationY = with(density) { subtitleOffsetY.value.dp.toPx() }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(ElectricBlue.copy(alpha = 0.85f), CircleShape)
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp,
                            letterSpacing = 0.1.sp
                        ),
                        color = Color(0xFFCBD5E1),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeUserAvatar(
    photoUrl: String?,
    displayName: String?
) {
    val initial = when {
        !displayName.isNullOrBlank() -> displayName.trim().first().uppercaseChar().toString()
        else -> "U"
    }

    Surface(
        shape = CircleShape,
        color = Color(0xFF1E293B),
        border = BorderStroke(1.5.dp, ElectricBlue.copy(alpha = 0.50f)),
        modifier = Modifier.size(52.dp)
    ) {
        if (!photoUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = photoUrl,
                contentDescription = "User Profile",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                error = {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = ElectricBlue
                        )
                    }
                },
                loading = {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = ElectricBlue
                        )
                    }
                }
            )
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = ElectricBlue
                )
            }
        }
    }
}

@Composable
private fun WavingHandEmoji(
    modifier: Modifier = Modifier,
    shouldAnimate: Boolean = true,
    onAnimationEnd: () -> Unit = {}
) {
    val context = LocalContext.current
    val areAnimationsEnabled = remember(context) {
        try {
            val scale = android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            scale > 0f
        } catch (e: Exception) {
            true
        }
    }

    val handRotation = remember { Animatable(0f) }

    LaunchedEffect(shouldAnimate, areAnimationsEnabled) {
        if (shouldAnimate && areAnimationsEnabled) {
            // Wave 1
            handRotation.animateTo(14f, tween(130, easing = FastOutSlowInEasing))
            handRotation.animateTo(-10f, tween(130, easing = FastOutSlowInEasing))
            // Wave 2
            handRotation.animateTo(14f, tween(130, easing = FastOutSlowInEasing))
            handRotation.animateTo(-8f, tween(130, easing = FastOutSlowInEasing))
            // Wave 3
            handRotation.animateTo(10f, tween(120, easing = FastOutSlowInEasing))
            handRotation.animateTo(0f, tween(140, easing = FastOutSlowInEasing))
            onAnimationEnd()
        } else {
            handRotation.snapTo(0f)
        }
    }

    Text(
        text = "👋",
        style = MaterialTheme.typography.titleMedium.copy(
            fontSize = 17.sp,
            lineHeight = 22.sp
        ),
        modifier = modifier.graphicsLayer {
            rotationZ = handRotation.value
            transformOrigin = TransformOrigin(0.7f, 0.9f)
        }
    )
}


private val curatedOneLiners = listOf(
    "Steady progress builds confidence",
    "A focused session goes a long way",
    "Small progress adds up over time",
    "Consistency makes preparation easier",
    "A calm session can be highly productive",
    "Good preparation reduces pressure",
    "Today's effort supports tomorrow's success",
    "Focus often beats intensity",
    "A little revision goes a long way",
    "Steady preparation brings confidence",
    "A focused hour can make a difference",
    "Small efforts build strong results",
    "Progress grows through consistency",
    "Learning works best with patience",
    "Preparation becomes easier with rhythm",
    "Consistency supports better outcomes",
    "A little focus goes a long way",
    "Good habits simplify preparation",
    "Careful revision strengthens understanding",
    "Small improvements compound over time",
    "Learning rewards consistency",
    "Preparation works best when steady",
    "Progress is built one session at a time",
    "A thoughtful review strengthens memory",
    "Focused effort builds confidence"
)

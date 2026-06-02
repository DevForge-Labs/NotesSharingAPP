package com.pravor.notessharing.ui.components.home_components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface SmartBannerState {
    object GreetingMode : SmartBannerState
    // Future extensibility:
    // data class ExamCampaign(val title: String, val subtitle: String, val accentColor: Color) : SmartBannerState
}

@Composable
fun SmartBannerSlot(
    modifier: Modifier = Modifier,
    state: SmartBannerState = SmartBannerState.GreetingMode
) {
    when (state) {
        is SmartBannerState.GreetingMode -> {
            PremiumGreetingBlock(modifier = modifier)
        }
    }
}

@Composable
private fun PremiumGreetingBlock(
    modifier: Modifier = Modifier
) {
    val baseGreeting = "Good Morning"

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

    // Animatable variables calibrated for perceptible, smooth card entrance reveals
    val cardAlpha = remember { Animatable(0f) }
    val cardOffsetY = remember { Animatable(16f) }
    val cardScale = remember { Animatable(0.98f) }
    
    val subtitleAlpha = remember { Animatable(0f) }
    val subtitleOffsetY = remember { Animatable(8f) }

    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        // Animating greeting card container fade, slide, and scale upward
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
        
        // Exact 90ms staggered reveal delay
        delay(90)
        
        // Animating supporting one-liner fade & slide upward inside the card
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
            Color(0xFF141922), // premium ambient teal-blue top
            Color(0xFF0D1016)  // deep dark slate bottom
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
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF58D6D1).copy(alpha = 0.08f)),
        color = Color.Transparent, // Custom background gradient applied inside column
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .background(cardBrush)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    letterSpacing = 0.15.sp,
                    lineHeight = 26.sp
                ),
                color = Color(0xFFF5F7FA)
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    letterSpacing = 0.25.sp,
                    lineHeight = 18.sp
                ),
                color = Color(0xFF94A3B8),
                modifier = Modifier.graphicsLayer {
                    alpha = subtitleAlpha.value
                    translationY = with(density) { subtitleOffsetY.value.dp.toPx() }
                }
            )
        }
    }
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

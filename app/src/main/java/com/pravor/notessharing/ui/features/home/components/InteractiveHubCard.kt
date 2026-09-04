package com.pravor.notessharing.ui.features.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pravor.notessharing.core.analytics.InteractiveHubAnalytics
import com.pravor.notessharing.domain.model.InteractiveHubSession
import com.pravor.notessharing.domain.model.InteractiveHubType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun InteractiveHubCard(
    session: InteractiveHubSession,
    onCtaClick: (String) -> Unit,
    onSurveyVote: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Log impression once per session
    LaunchedEffect(session.sessionId) {
        InteractiveHubAnalytics.logImpression(context, session)
    }

    // Survey voting state for instantaneous visual feedback
    var selectedOption by remember(session.sessionId) { mutableStateOf<String?>(null) }
    var isSubmitted by remember(session.sessionId) { mutableStateOf(false) }
    var isCardVisible by remember(session.sessionId) { mutableStateOf(true) }

    // Subtle gradient border animation
    val infiniteTransition = rememberInfiniteTransition(label = "hub_border_glow")
    val borderOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_offset"
    )

    val borderBrush = remember(borderOffset) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF6366F1), // Indigo
                Color(0xFFA855F7), // Purple
                Color(0xFFEC4899), // Pink
                Color(0xFF38BDF8)  // Light Blue
            )
        )
    }

    val cardBackground = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF111422),
                Color(0xFF0B0D16)
            )
        )
    }

    val cardShape = remember { RoundedCornerShape(22.dp) }

    AnimatedVisibility(
        visible = isCardVisible,
        enter = fadeIn(tween(400)),
        exit = fadeOut(tween(550, easing = FastOutSlowInEasing)) +
                shrinkVertically(
                    animationSpec = tween(650, easing = FastOutSlowInEasing),
                    shrinkTowards = Alignment.Top
                ),
        modifier = modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Subtle ambient glow behind the card
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF6366F1).copy(alpha = 0.12f), Color.Transparent),
                            center = center,
                            radius = size.maxDimension / 1.4f
                        )
                    )
                },
        shape = cardShape,
        border = BorderStroke(1.2.dp, borderBrush),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBackground)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. Primary Title (Main Focus)
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 2. Body Text (Clear & Legible)
                Text(
                    text = session.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE2E8F0),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Interactive Action Section
                when (session.hubType) {
                    InteractiveHubType.SURVEY -> {
                        // Smoothly transition between options and confirmation badge
                        AnimatedContent(
                            targetState = isSubmitted && selectedOption != null,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(350, easing = FastOutSlowInEasing)) togetherWith
                                        fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing))
                            },
                            label = "survey_feedback"
                        ) { submitted ->
                            if (submitted) {
                                // Instant visual feedback for recorded response
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF064E3B).copy(alpha = 0.75f))
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF34D399),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Response recorded (${selectedOption})",
                                        color = Color(0xFFA7F3D0),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                val options = if (session.surveyOptions.isNotEmpty()) session.surveyOptions else listOf("YES", "NO")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    options.forEach { option ->
                                        val isCurrentSelected = selectedOption == option
                                        val interactionSource = remember { MutableInteractionSource() }
                                        val isPressed by interactionSource.collectIsPressedAsState()
                                        val scale by animateFloatAsState(
                                            targetValue = if (isPressed) 0.96f else 1.0f,
                                            animationSpec = tween(durationMillis = 100),
                                            label = "survey_btn_scale"
                                        )

                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .graphicsLayer(scaleX = scale, scaleY = scale)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable(
                                                    interactionSource = interactionSource,
                                                    indication = null,
                                                    enabled = selectedOption == null
                                                ) {
                                                    selectedOption = option
                                                    isSubmitted = true
                                                    InteractiveHubAnalytics.logSurveyResponse(context, session, option)
                                                    coroutineScope.launch {
                                                        // 1. Let the user see the green confirmation checkmark
                                                        kotlinx.coroutines.delay(1200)
                                                        // 2. Smoothly animate card exit (fade + vertical shrink)
                                                        isCardVisible = false
                                                        // 3. Wait for the 650ms collapse animation to finish before removing state
                                                        kotlinx.coroutines.delay(700)
                                                        onSurveyVote(session.sessionId, option)
                                                    }
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isCurrentSelected) Color(0xFF4F46E5) else Color(0xFF1E2235),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isCurrentSelected) Color(0xFF818CF8) else Color(0xFF33384F)
                                            )
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = option,
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    letterSpacing = 0.4.sp
                                                )
                                            }
                                        }
                                }
                            }
                        }
                    }
                }

                    InteractiveHubType.ANNOUNCEMENT,
                    InteractiveHubType.PROMOTION -> {
                        // Navigation CTA Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val cta = session.ctaText?.trim()?.ifBlank { null } ?: "LET'S GO"
                            val destination = session.targetDestination ?: "exam_prep"
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            val scale by animateFloatAsState(
                                targetValue = if (isPressed) 0.95f else 1.0f,
                                animationSpec = tween(durationMillis = 100),
                                label = "hub_cta_scale"
                            )

                            Surface(
                                modifier = Modifier
                                    .graphicsLayer(scaleX = scale, scaleY = scale)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null
                                    ) {
                                        InteractiveHubAnalytics.logClick(context, session)
                                        onCtaClick(destination)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Transparent,
                                shadowElevation = 4.dp
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(Color(0xFF6366F1), Color(0xFF9333EA))
                                            )
                                        )
                                        .padding(horizontal = 14.dp, vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = cta,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.5.sp
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

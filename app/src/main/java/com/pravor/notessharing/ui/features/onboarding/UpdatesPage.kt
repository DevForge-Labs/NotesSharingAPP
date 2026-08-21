package com.pravor.notessharing.ui.features.onboarding

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pravor.notessharing.domain.model.UpdatePageModel
import com.pravor.notessharing.ui.features.onboarding.components.ConvergingResourcesVisual
import com.pravor.notessharing.ui.features.onboarding.components.RippleCommunityVisual
import com.pravor.notessharing.ui.features.onboarding.components.StaggeredCardsVisual
import com.pravor.notessharing.ui.features.onboarding.components.WhyUseAppVisual

@Composable
fun UpdatesPage(
    model: UpdatePageModel,
    isActive: Boolean,
    onJoinCommunityClick: () -> Unit,
    onSignInClick: () -> Unit,
    pageOffset: Float = 0f,
    modifier: Modifier = Modifier
) {
    when (model.layoutType) {
        UpdatePageModel.LayoutType.CONVERGING_RESOURCES -> {
            StandardOnboardingLayout(
                headline = model.headline,
                supportingText = model.supportingText,
                visual = { ConvergingResourcesVisual(isActive = isActive) },
                isActive = isActive,
                pageOffset = pageOffset,
                modifier = modifier
            )
        }
        UpdatePageModel.LayoutType.STAGGERED_CARDS -> {
            StandardOnboardingLayout(
                headline = model.headline,
                supportingText = model.supportingText,
                visual = { StaggeredCardsVisual(isActive = isActive) },
                isActive = isActive,
                pageOffset = pageOffset,
                modifier = modifier
            )
        }
        UpdatePageModel.LayoutType.RIPPLE_COMMUNITY -> {
            StandardOnboardingLayout(
                headline = model.headline,
                supportingText = model.supportingText,
                visual = { RippleCommunityVisual(isActive = isActive) },
                isActive = isActive,
                pageOffset = pageOffset,
                modifier = modifier
            )
        }
        UpdatePageModel.LayoutType.WHY_USE_APP -> {
            StandardOnboardingLayout(
                headline = model.headline,
                supportingText = model.supportingText,
                visual = { WhyUseAppVisual(isActive = isActive) },
                isActive = isActive,
                pageOffset = pageOffset,
                modifier = modifier
            )
        }
        UpdatePageModel.LayoutType.WELCOME_INSPIRING -> {
            FinalWelcomeOnboardingLayout(
                headline = model.headline,
                supportingText = model.supportingText,
                onJoinCommunityClick = onJoinCommunityClick,
                onSignInClick = onSignInClick,
                isActive = isActive,
                pageOffset = pageOffset,
                modifier = modifier
            )
        }
    }
}

@Composable
fun StandardOnboardingLayout(
    headline: String,
    supportingText: String,
    visual: @Composable () -> Unit,
    isActive: Boolean,
    pageOffset: Float = 0f,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val visualParallaxPx = remember(pageOffset) {
        with(density) { (-pageOffset * 50.dp.toPx()) }
    }
    val textParallaxPx = remember(pageOffset) {
        with(density) { (-pageOffset * 20.dp.toPx()) }
    }

    val illustrationAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(600, delayMillis = 0, easing = LinearOutSlowInEasing),
        label = "illustrationAlpha"
    )
    val illustrationOffsetY by animateDpAsState(
        targetValue = if (isActive) 0.dp else 20.dp,
        animationSpec = tween(600, delayMillis = 0, easing = LinearOutSlowInEasing),
        label = "illustrationOffsetY"
    )

    val titleAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(600, delayMillis = 150, easing = LinearOutSlowInEasing),
        label = "titleAlpha"
    )
    val titleOffsetY by animateDpAsState(
        targetValue = if (isActive) 0.dp else 12.dp,
        animationSpec = tween(600, delayMillis = 150, easing = LinearOutSlowInEasing),
        label = "titleOffsetY"
    )

    val descriptionAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(600, delayMillis = 300, easing = LinearOutSlowInEasing),
        label = "descriptionAlpha"
    )
    val descriptionOffsetY by animateDpAsState(
        targetValue = if (isActive) 0.dp else 12.dp,
        animationSpec = tween(600, delayMillis = 300, easing = LinearOutSlowInEasing),
        label = "descriptionOffsetY"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .offset(y = illustrationOffsetY)
                .graphicsLayer {
                    translationX = visualParallaxPx
                    alpha = illustrationAlpha
                }
        ) {
            visual()
        }
        
        Spacer(modifier = Modifier.height(28.dp))
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                translationX = textParallaxPx
            }
        ) {
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .offset(y = titleOffsetY)
                    .graphicsLayer { alpha = titleAlpha }
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .offset(y = descriptionOffsetY)
                    .graphicsLayer { alpha = descriptionAlpha }
            )
        }
    }
}

@Composable
fun FinalWelcomeOnboardingLayout(
    headline: String,
    supportingText: String,
    onJoinCommunityClick: () -> Unit,
    onSignInClick: () -> Unit,
    isActive: Boolean,
    pageOffset: Float = 0f,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val visualParallaxPx = remember(pageOffset) {
        with(density) { (-pageOffset * 50.dp.toPx()) }
    }
    val textParallaxPx = remember(pageOffset) {
        with(density) { (-pageOffset * 20.dp.toPx()) }
    }
    val actionParallaxPx = remember(pageOffset) {
        with(density) { (-pageOffset * 10.dp.toPx()) }
    }

    val headerAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(600, delayMillis = 0, easing = LinearOutSlowInEasing),
        label = "headerAlpha"
    )
    val headerOffsetY by animateDpAsState(
        targetValue = if (isActive) 0.dp else 20.dp,
        animationSpec = tween(600, delayMillis = 0, easing = LinearOutSlowInEasing),
        label = "headerOffsetY"
    )

    val bodyAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(600, delayMillis = 150, easing = LinearOutSlowInEasing),
        label = "bodyAlpha"
    )
    val bodyOffsetY by animateDpAsState(
        targetValue = if (isActive) 0.dp else 12.dp,
        animationSpec = tween(600, delayMillis = 150, easing = LinearOutSlowInEasing),
        label = "bodyOffsetY"
    )

    val actionAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(600, delayMillis = 300, easing = LinearOutSlowInEasing),
        label = "actionAlpha"
    )
    val actionOffsetY by animateDpAsState(
        targetValue = if (isActive) 0.dp else 12.dp,
        animationSpec = tween(600, delayMillis = 300, easing = LinearOutSlowInEasing),
        label = "actionOffsetY"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .offset(y = headerOffsetY)
                    .graphicsLayer {
                        translationX = visualParallaxPx
                        alpha = headerAlpha
                    }
                    .size(96.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.School,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(y = bodyOffsetY)
                    .graphicsLayer {
                        translationX = textParallaxPx
                        alpha = bodyAlpha
                    }
            ) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "COLLABORATIVE ACADEMICS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BulletRow(text = "Find semester resources in seconds")
                            BulletRow(text = "Share notes to support your classmates")
                            BulletRow(text = "Prepare and succeed together as a peer community")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "ACADEMIC RESOURCES AVAILABLE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryChip(label = "Notes")
                        CategoryChip(label = "Assignments")
                        CategoryChip(label = "PYQs")
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryChip(label = "Cheat Sheets")
                        CategoryChip(label = "Videos")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
                .offset(y = actionOffsetY)
                .graphicsLayer {
                    translationX = actionParallaxPx
                    alpha = actionAlpha
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val ctaScale by animateFloatAsState(targetValue = if (isPressed) 0.96f else 1.0f, label = "ctaScale")

            Button(
                onClick = onJoinCommunityClick,
                interactionSource = interactionSource,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .graphicsLayer {
                        scaleX = ctaScale
                        scaleY = ctaScale
                    },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "Join the Community",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.clickable(onClick = onSignInClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sign in to continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
}

@Composable
private fun BulletRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CategoryChip(label: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            fontWeight = FontWeight.Medium
        )
    }
}

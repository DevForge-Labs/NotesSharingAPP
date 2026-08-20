package com.pravor.notessharing.ui.components.profile_components

import com.pravor.notessharing.core.util.*

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.pravor.notessharing.domain.model.Profile
import kotlin.math.max
import kotlin.math.min

@Composable
private fun PressScaleCard(
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && onClick != null) 0.985f else 1f, label = "profile-card-press")

    val cardModifier = if (onClick != null) {
        modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    } else {
        modifier
    }

    Card(
        modifier = cardModifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        content = { content() }
    )
}

private fun extractAccentColor(bitmap: Bitmap, fallbackColor: Color): Color {
    return try {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 1, 1, true)
        val colorInt = scaledBitmap.getPixel(0, 0)
        scaledBitmap.recycle()

        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(colorInt, hsv)
        val value = hsv[2]
        val saturation = hsv[1]

        // Reject if too dark, gray/neutral, or too white/washed-out
        if (value < 0.25f || saturation < 0.2f || (value > 0.9f && saturation < 0.15f)) {
            fallbackColor
        } else {
            Color(colorInt)
        }
    } catch (e: Exception) {
        fallbackColor
    }
}

private fun hsvToComposeColor(hue: Float, saturation: Float, value: Float): Color {
    val hsv = floatArrayOf(hue, saturation, value)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

private fun getBranchAbbreviation(branchIdOrName: String): String {
    val clean = branchIdOrName.trim().lowercase(java.util.Locale.ROOT)
    if (clean.contains("computer science") || clean == "cs" || clean == "cse") return "CSE"
    if (clean.contains("information technology") || clean == "it") return "IT"
    if (clean.contains("electronics") || clean == "ece") return "ECE"
    if (clean.contains("electrical") || clean == "eee") return "EEE"
    if (clean.contains("mechanical") || clean == "mech" || clean == "me") return "ME"
    if (clean.contains("civil") || clean == "ce") return "CE"
    if (clean.contains("biotech") || clean == "bt") return "BT"
    
    // Fallback: resolve canonical ID and uppercase it
    val resolvedId = com.pravor.notessharing.core.util.LegacyAcademicCompatibilityResolver.resolveBranchId(branchIdOrName)
    return resolvedId.uppercase(java.util.Locale.ROOT)
}

private val SectionFormatRegex = Regex("([a-zA-Z]+)\\s*[-–—]?\\s*(\\d+)")

private fun formatSection(section: String): String {
    val trimmed = section.trim()
    if (trimmed.isEmpty()) return ""
    
    // Matches letters, optional spaces/dash, then numbers (e.g. cse-28, CSE-28, cse - 28)
    val matchResult = SectionFormatRegex.find(trimmed)
    return if (matchResult != null) {
        val letters = matchResult.groupValues[1].uppercase()
        val numbers = matchResult.groupValues[2]
        "$letters-$numbers"
    } else {
        trimmed.uppercase()
    }
}

@Composable
fun ProfileHeaderCard(
    profile: Profile,
    resolvedCollegeName: String,
    resolvedBranchName: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var avatarBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isImageError by remember { mutableStateOf(false) }

    LaunchedEffect(profile.profileImageUrl) {
        if (profile.profileImageUrl.isNotEmpty()) {
            try {
                val loader = coil.ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(profile.profileImageUrl)
                    .allowHardware(false)
                    .build()
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val drawable = result.drawable
                    if (drawable is BitmapDrawable) {
                        avatarBitmap = drawable.bitmap
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            avatarBitmap = null
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val dominantColor = remember(avatarBitmap, primaryColor) {
        avatarBitmap?.let { bitmap ->
            extractAccentColor(bitmap, primaryColor)
        } ?: primaryColor
    }

    val hsv = remember(dominantColor) {
        val h = FloatArray(3)
        android.graphics.Color.colorToHSV(dominantColor.toArgb(), h)
        h
    }
    val hue = hsv[0]
    val saturation = hsv[1]

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Dynamic accent color for avatar ring and radial glow
    val accentColor = remember(hue, saturation, isDark) {
        if (isDark) {
            hsvToComposeColor(hue, max(0.6f, saturation), 0.9f)
        } else {
            hsvToComposeColor(hue, max(0.7f, saturation), 0.65f)
        }
    }

    // Dynamic Gradient generation
    val gradientBrush = remember(hue, saturation, isDark) {
        val hueShift = when (hue) {
            in 330.0f..360.0f -> 30f
            in 0.0f..20.0f -> 30f
            in 20.0f..70.0f -> -25f
            in 70.0f..150.0f -> 40f
            in 150.0f..250.0f -> 30f
            in 250.0f..330.0f -> -35f
            else -> 20f
        }
        val endHue = (hue + hueShift + 360f) % 360f

        val (startColor, endColor) = if (isDark) {
            val sColor = hsvToComposeColor(hue, max(0.5f, saturation), 0.22f)
            val eColor = hsvToComposeColor(endHue, max(0.6f, saturation), 0.10f)
            sColor to eColor
        } else {
            val sColor = hsvToComposeColor(hue, min(0.25f, saturation), 0.95f)
            val eColor = hsvToComposeColor(endHue, min(0.30f, saturation), 0.88f)
            sColor to eColor
        }

        Brush.linearGradient(
            colors = listOf(startColor, endColor)
        )
    }

    // --- Entrance Animation Trigger ---
    var animateTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateTrigger = true
    }

    // 1. Avatar animations
    val avatarAlphaAnim by animateFloatAsState(
        targetValue = if (animateTrigger) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing),
        label = "avatar_alpha"
    )
    val avatarScaleAnim by animateFloatAsState(
        targetValue = if (animateTrigger) 1f else 0.85f,
        animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing),
        label = "avatar_scale"
    )

    // 2. Name animations
    val nameAlphaAnim by animateFloatAsState(
        targetValue = if (animateTrigger) 1f else 0f,
        animationSpec = tween(durationMillis = 400, delayMillis = 100, easing = LinearOutSlowInEasing),
        label = "name_alpha"
    )
    val nameOffsetYAnim by animateDpAsState(
        targetValue = if (animateTrigger) 0.dp else 12.dp,
        animationSpec = tween(durationMillis = 400, delayMillis = 100, easing = LinearOutSlowInEasing),
        label = "name_offset"
    )

    // 3. Pill animations
    val pillAlphaAnim by animateFloatAsState(
        targetValue = if (animateTrigger) 1f else 0f,
        animationSpec = tween(durationMillis = 400, delayMillis = 200, easing = LinearOutSlowInEasing),
        label = "pill_alpha"
    )
    val pillOffsetYAnim by animateDpAsState(
        targetValue = if (animateTrigger) 0.dp else 12.dp,
        animationSpec = tween(durationMillis = 400, delayMillis = 200, easing = LinearOutSlowInEasing),
        label = "pill_offset"
    )

    // --- Breathing Aura Glow Animation ---
    val infiniteTransition = rememberInfiniteTransition(label = "aura_pulse")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f, // Aura scale 1.05 refinement
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_scale"
    )
    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.40f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_alpha"
    )

    PressScaleCard(
        shape = RoundedCornerShape(32.dp),
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .background(gradientBrush)
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val avatarSize = 110.dp

            // Avatar container with Staggered Entrance and Breathing Glow
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = avatarAlphaAnim
                        scaleX = avatarScaleAnim
                        scaleY = avatarScaleAnim
                    }
                    .padding(bottom = 14.dp)
            ) {
                // Breathing Aura Glow
                Box(
                    modifier = Modifier
                        .size(avatarSize + 24.dp)
                        .graphicsLayer {
                            scaleX = breathingScale
                            scaleY = breathingScale
                        }
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = breathingAlpha),
                                    accentColor.copy(alpha = 0.05f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Avatar Container with Accent Ring
                Box(
                    modifier = Modifier
                        .size(avatarSize)
                        .border(2.5.dp, accentColor, CircleShape)
                        .padding(4.dp)
                        .clip(CircleShape)
                ) {
                    if (profile.profileImageUrl.isNotEmpty() && !isImageError) {
                        AsyncImage(
                            model = profile.profileImageUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .matchParentSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            onError = { isImageError = true }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(accentColor, accentColor.copy(alpha = 0.7f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = profile.initials,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 32.sp
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Name with Entrance transition
            Text(
                text = profile.name,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    fontSize = 25.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = nameAlphaAnim
                    translationY = nameOffsetYAnim.toPx()
                }
            )

            // Premium Metadata Capsule Pill (Content Width Centered Pill)
            val academicText = remember(profile.branch, profile.semester, profile.section) {
                val branchDisplay = getBranchAbbreviation(profile.branch)
                val semNum = profile.semester.filter { it.isDigit() }
                val semesterDisplay = if (semNum.isNotEmpty()) "Sem-$semNum" else profile.semester
                val sectionDisplay = formatSection(profile.section)

                buildList {
                    if (branchDisplay.isNotBlank()) add(branchDisplay)
                    if (semesterDisplay.isNotBlank()) add(semesterDisplay)
                    if (sectionDisplay.isNotBlank()) add(sectionDisplay)
                }.joinToString(" | ")
            }

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = pillAlphaAnim
                        translationY = pillOffsetYAnim.toPx()
                    }
                    .padding(top = 10.dp)
                    .wrapContentWidth(Alignment.CenterHorizontally) // Explicit content-width centered pill
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(100.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = accentColor.copy(alpha = 0.12f), // Pill border opacity refinement to 0.12
                        shape = RoundedCornerShape(100.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = academicText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

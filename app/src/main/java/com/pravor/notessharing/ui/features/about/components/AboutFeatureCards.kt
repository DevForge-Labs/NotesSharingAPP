package com.pravor.notessharing.ui.features.about.components

import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

@Composable
fun AboutSectionWrapper(
    index: Int,
    isVisible: Boolean,
    content: @Composable () -> Unit
) {
    val alpha = animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 650,
            delayMillis = index * 70,
            easing = EaseOutCubic
        ),
        label = "section-alpha-$index"
    )
    val translationY = animateFloatAsState(
        targetValue = if (isVisible) 0f else 40f,
        animationSpec = tween(
            durationMillis = 650,
            delayMillis = index * 70,
            easing = EaseOutCubic
        ),
        label = "section-translate-$index"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha.value
                this.translationY = translationY.value
            }
    ) {
        content()
    }
}

@Composable
fun HomeExperienceCard() {
    PremiumInfoCard(
        title = "Personalized Home Experience",
        description = "The Home screen is designed specifically around each student to match their study cycle.",
        icon = Icons.Default.Home
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            FeatureRow(
                title = "Continue Reading",
                description = "Quickly resume previously opened materials. Never lose track of study progress."
            )
            FeatureRow(
                title = "For You Feed",
                description = "A personalized recommendation feed powered by engagement and relevance. Students can discover notes, assignments, study materials, and exam resources."
            )
            FeatureRow(
                title = "Quick Access Library",
                description = "Instant access to bookmarks, uploads, and downloads. Everything important remains just one tap away."
            )
        }
    }
}

@Composable
fun ExploreDiscoveryCard() {
    PremiumInfoCard(
        title = "Discover What Matters",
        description = "The Explore experience helps students discover content beyond their immediate interests.",
        icon = Icons.Default.Explore
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            FeatureRow(
                title = "Subject Hero Sections",
                description = "Beautiful subject-focused discovery experiences help students quickly explore academic resources across a wide range of disciplines."
            )
            FeatureRow(
                title = "Trending Content & Popular Uploads",
                description = "Discover resources gaining high attention across your campus, and access study materials with the highest appreciation."
            )
            FeatureRow(
                title = "Fresh Discoveries",
                description = "New uploads receive visibility so every student contributor gets an opportunity to reach learners."
            )
        }
    }
}

@Composable
fun SocialLearningCard() {
    PremiumInfoCard(
        title = "Learning Meets Community",
        description = "Campus Pages is more than a note-sharing application. It is a community-driven social learning ecosystem.",
        icon = Icons.Default.Share
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            FeatureRow(
                title = "Upvotes & Community Driven Quality",
                description = "Students can upvote useful resources. The most valuable learning resources naturally rise higher, balancing popularity with freshness."
            )
            FeatureRow(
                title = "Engagement Signals",
                description = "Bookmarks, downloads, views, and community interaction help surface quality content for future generations of students."
            )
        }
    }
}

@Composable
fun UploadContributionCard() {
    PremiumInfoCard(
        title = "Share Knowledge",
        description = "Students can contribute resources to help others succeed. Every contribution helps strengthen the learning community.",
        icon = Icons.Default.CloudUpload
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
            ) {
                UploadAnimationCanvas()
            }

            FeatureRow(
                title = "Supported Formats",
                description = "Contribute your Lecture Notes, Semester Assignments, Lab Materials, Exam Resources, or customized Study Guides."
            )
        }
    }
}

@Composable
fun UploadAnimationCanvas() {
    val infiniteTransition = rememberInfiniteTransition(label = "upload-canvas")
    
    val arrowOffset by infiniteTransition.animateFloat(
        initialValue = 20f,
        targetValue = -20f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow"
    )

    val p1Y by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "p1"
    )
    val p2Y by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, delayMillis = 400, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "p2"
    )
    val p3Y by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, delayMillis = 900, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "p3"
    )
    val p4Y by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 1400, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "p4"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f + 15f

        val docWidth = 72f
        val docHeight = 98f
        val docLeft = cx - docWidth / 2f
        val docTop = cy - docHeight / 2f

        drawRoundRect(
            color = primaryColor,
            topLeft = Offset(docLeft, docTop),
            size = androidx.compose.ui.geometry.Size(docWidth, docHeight),
            cornerRadius = CornerRadius(12f, 12f),
            style = Stroke(width = 4.5f)
        )

        val foldSize = 22f
        val foldPath = Path().apply {
            moveTo(docLeft + docWidth - foldSize, docTop)
            lineTo(docLeft + docWidth, docTop + foldSize)
            lineTo(docLeft + docWidth - foldSize, docTop + foldSize)
            close()
        }
        drawPath(foldPath, color = primaryColor)

        val arrowHeadY = cy - 10f + arrowOffset
        val arrowPath = Path().apply {
            moveTo(cx, arrowHeadY - 16f)
            lineTo(cx - 15f, arrowHeadY)
            lineTo(cx - 6f, arrowHeadY)
            lineTo(cx - 6f, arrowHeadY + 18f)
            lineTo(cx + 6f, arrowHeadY + 18f)
            lineTo(cx + 6f, arrowHeadY)
            lineTo(cx + 15f, arrowHeadY)
            close()
        }
        drawPath(arrowPath, color = secondaryColor)

        drawCircle(
            color = primaryColor.copy(alpha = p1Y),
            radius = 8f,
            center = Offset(cx - 90f, cy + 30f - p1Y * 110f)
        )
        drawCircle(
            color = secondaryColor.copy(alpha = p2Y),
            radius = 7f,
            center = Offset(cx + 85f, cy + 10f - p2Y * 120f)
        )
        drawCircle(
            color = tertiaryColor.copy(alpha = p3Y),
            radius = 9f,
            center = Offset(cx - 40f, cy - 35f - p3Y * 70f)
        )
        drawCircle(
            color = primaryColor.copy(alpha = p4Y),
            radius = 6f,
            center = Offset(cx + 42f, cy - 15f - p4Y * 85f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhyCampusPagesChipsSection() {
    val chips = listOf(
        "Personalized Recommendations",
        "Continue Reading",
        "Social Learning",
        "Smart Discovery",
        "Trending Resources",
        "Subject Exploration",
        "Community Contributions",
        "Fast Search",
        "Bookmarking",
        "Offline Downloads"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Why Students Love Us",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            chips.forEach { chipText ->
                SuggestionChip(
                    onClick = { },
                    label = {
                        Text(
                            text = chipText,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

@Composable
fun BuiltForStudentsAndFooter(
    versionName: String,
    buildNumber: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse-heart")
    val heartScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heart-scale"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
            ),
            modifier = Modifier.border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Built For Students",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Campus Pages was built with a simple vision: Make academic resources easier to discover, easier to share, and easier to learn from.\n\nWhether preparing for exams, completing assignments, or exploring new topics, Campus Pages helps students stay connected with the knowledge that matters most.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Made for Students ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Love",
                    tint = Color.Red,
                    modifier = Modifier
                        .size(18.dp)
                        .scale(heartScale)
                )
            }

            Text(
                text = "Campus Pages v$versionName ($buildNumber)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "© ${Calendar.getInstance().get(Calendar.YEAR)} DevForge Labs. All rights reserved.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PremiumInfoCard(
    title: String,
    description: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            content()
        }
    }
}

@Composable
fun FeatureRow(
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

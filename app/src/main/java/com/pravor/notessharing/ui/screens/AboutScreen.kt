package com.pravor.notessharing.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pravor.notessharing.R
import kotlinx.coroutines.delay
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    // Privacy Policy Coming Soon dialog state
    var showPrivacyDialog by remember { mutableStateOf(false) }

    // Retrieve Version and Build Info
    val packageInfo = remember(context) {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: Exception) {
            null
        }
    }
    val versionName = packageInfo?.versionName ?: "1.0.0"
    val buildNumber = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        packageInfo?.longVersionCode?.toString() ?: "1"
    } else {
        @Suppress("DEPRECATION")
        packageInfo?.versionCode?.toString() ?: "1"
    }

    // Entrance Animation State
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    // Floating Background Gradients
    val infiniteTransition = rememberInfiniteTransition(label = "background-gradients")
    val animX1 by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "x1"
    )
    val animY1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "y1"
    )
    val animX2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "x2"
    )
    val animY2 by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(11000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "y2"
    )

    // Helper function to safely launch URLs
    val openUrl = remember(context) {
        { url: String ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fail-safe
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Floating gradient circles in background
        val primaryColor = MaterialTheme.colorScheme.primary
        val tertiaryColor = MaterialTheme.colorScheme.tertiary
        val backgroundSolid = MaterialTheme.colorScheme.background

        Canvas(modifier = Modifier.fillMaxSize().background(backgroundSolid)) {
            val sizeMax = max(size.width, size.height)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width * animX1, size.height * animY1),
                    radius = sizeMax * 0.45f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tertiaryColor.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(size.width * animX2, size.height * animY2),
                    radius = sizeMax * 0.40f
                )
            )
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                LargeTopAppBar(
                    title = {
                        Text(
                            text = "About Campus Pages",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f)
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 32.dp,
                    start = 20.dp,
                    end = 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Section 1: Hero Header
                item {
                    AboutSectionWrapper(index = 0, isVisible = isVisible) {
                        HeroHeaderCard(
                            versionName = versionName,
                            buildNumber = buildNumber
                        )
                    }
                }

                // Section 2: Home Experience
                item {
                    AboutSectionWrapper(index = 1, isVisible = isVisible) {
                        HomeExperienceCard()
                    }
                }

                // Section 3: Explore & Discovery
                item {
                    AboutSectionWrapper(index = 2, isVisible = isVisible) {
                        ExploreDiscoveryCard()
                    }
                }

                // Section 4: Social Learning Platform
                item {
                    AboutSectionWrapper(index = 3, isVisible = isVisible) {
                        SocialLearningCard()
                    }
                }

                // Section 5: Upload & Contribution (Enhanced with larger showcase illustration)
                item {
                    AboutSectionWrapper(index = 4, isVisible = isVisible) {
                        UploadContributionCard()
                    }
                }

                // Section 6: Why Campus Pages (Feature chips)
                item {
                    AboutSectionWrapper(index = 5, isVisible = isVisible) {
                        WhyCampusPagesChipsSection()
                    }
                }

                // Section: Meet the Developers (Founder designation changes)
                item {
                    AboutSectionWrapper(index = 6, isVisible = isVisible) {
                        MeetDevelopersSection(onLinkClick = openUrl)
                    }
                }

                // Section 5: External Links & Privacy Policy Popup
                item {
                    AboutSectionWrapper(index = 7, isVisible = isVisible) {
                        ExternalLinksSection(
                            onPrivacyPolicyClick = { showPrivacyDialog = true }
                        )
                    }
                }

                // Section 7: Built For Students & Section 8: Footer
                item {
                    AboutSectionWrapper(index = 8, isVisible = isVisible) {
                        BuiltForStudentsAndFooter(
                            versionName = versionName,
                            buildNumber = buildNumber
                        )
                    }
                }
            }
        }
    }

    // Coming Soon dialogue for Privacy Policy
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = {
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "The Privacy Policy for Campus Pages is coming soon. Please check back later!",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text(text = "OK", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}

/**
 * Animated wrapper to stagger the entrance of sections on screen load
 */
@Composable
private fun AboutSectionWrapper(
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

/**
 * Section 1: Hero Header Card
 */
@Composable
private fun HeroHeaderCard(
    versionName: String,
    buildNumber: String
) {
    // Subtly animate the tagline color/glow
    val infiniteTransition = rememberInfiniteTransition(label = "tagline-anim")
    val taglineAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tagline-alpha"
    )
    val taglineScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tagline-scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Glowing App Logo container
            Surface(
                modifier = Modifier
                    .size(96.dp)
                    .border(
                        2.dp,
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 6.dp
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo_normal),
                    contentDescription = "Campus Pages Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Campus Pages",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Your Campus. Your Notes. Your Community.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Tagline Animation
            Text(
                text = "Learn Together. Grow Together.",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = taglineAlpha),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .scale(taglineScale)
                    .background(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Campus Pages is a modern student-first platform designed to help learners discover, share, organize, and collaborate around high-quality academic resources. It combines note sharing, content discovery, social engagement, and personalized learning into a single seamless experience.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Subdued Version/Build Badge
            Text(
                text = "v$versionName (Build $buildNumber)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Section 2: Home Experience Card
 */
@Composable
private fun HomeExperienceCard() {
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

/**
 * Section 3: Explore & Discovery Card (Refined generically, future-ready)
 */
@Composable
private fun ExploreDiscoveryCard() {
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

/**
 * Section 4: Social Learning Platform Card
 */
@Composable
private fun SocialLearningCard() {
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

/**
 * Section 5: Upload & Contribution Card (Enhanced with larger showcase illustration)
 */
@Composable
private fun UploadContributionCard() {
    PremiumInfoCard(
        title = "Share Knowledge",
        description = "Students can contribute resources to help others succeed. Every contribution helps strengthen the learning community.",
        icon = Icons.Default.CloudUpload
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // Enhanced focal upload animation panel
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp), // Increased size
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

/**
 * Custom Canvas drawing an animated uploading document (Larger, more premium focal style)
 */
@Composable
private fun UploadAnimationCanvas() {
    val infiniteTransition = rememberInfiniteTransition(label = "upload-canvas")
    
    // Animate arrow vertically (larger movement range)
    val arrowOffset by infiniteTransition.animateFloat(
        initialValue = 20f,
        targetValue = -20f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow"
    )

    // Animate 4 particles floating upwards with varied delay and speeds
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

        // Draw Document Frame (scaled up)
        val docWidth = 72f
        val docHeight = 98f
        val docLeft = cx - docWidth / 2f
        val docTop = cy - docHeight / 2f

        // Draw simple document container
        drawRoundRect(
            color = primaryColor,
            topLeft = Offset(docLeft, docTop),
            size = androidx.compose.ui.geometry.Size(docWidth, docHeight),
            cornerRadius = CornerRadius(12f, 12f),
            style = Stroke(width = 4.5f)
        )

        // Draw diagonal fold at top-right
        val foldSize = 22f
        val foldPath = Path().apply {
            moveTo(docLeft + docWidth - foldSize, docTop)
            lineTo(docLeft + docWidth, docTop + foldSize)
            lineTo(docLeft + docWidth - foldSize, docTop + foldSize)
            close()
        }
        drawPath(foldPath, color = primaryColor)

        // Draw Upward Arrow (scaled up)
        val arrowHeadY = cy - 10f + arrowOffset
        val arrowPath = Path().apply {
            // Arrow Head
            moveTo(cx, arrowHeadY - 16f)
            lineTo(cx - 15f, arrowHeadY)
            lineTo(cx - 6f, arrowHeadY)
            // Arrow Shaft
            lineTo(cx - 6f, arrowHeadY + 18f)
            lineTo(cx + 6f, arrowHeadY + 18f)
            lineTo(cx + 6f, arrowHeadY)
            lineTo(cx + 15f, arrowHeadY)
            close()
        }
        drawPath(arrowPath, color = secondaryColor)

        // Draw Floating Particles (wider distribution & larger sizes)
        // Particle 1 (Far-Left)
        drawCircle(
            color = primaryColor.copy(alpha = p1Y),
            radius = 8f,
            center = Offset(cx - 90f, cy + 30f - p1Y * 110f)
        )
        // Particle 2 (Far-Right)
        drawCircle(
            color = secondaryColor.copy(alpha = p2Y),
            radius = 7f,
            center = Offset(cx + 85f, cy + 10f - p2Y * 120f)
        )
        // Particle 3 (Near-Left)
        drawCircle(
            color = tertiaryColor.copy(alpha = p3Y),
            radius = 9f,
            center = Offset(cx - 40f, cy - 35f - p3Y * 70f)
        )
        // Particle 4 (Near-Right)
        drawCircle(
            color = primaryColor.copy(alpha = p4Y),
            radius = 6f,
            center = Offset(cx + 42f, cy - 15f - p4Y * 85f)
        )
    }
}

/**
 * Section 6: Why Campus Pages Feature Chips
 */
@Composable
private fun WhyCampusPagesChipsSection() {
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
                    onClick = { /* Read only click */ },
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

/**
 * Meet the Developers Section
 */
@Composable
private fun MeetDevelopersSection(
    onLinkClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Meet the Developers",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )

        DeveloperCard(
            name = "Pratyush Nishank",
            role = "Founder",
            initials = "PN",
            githubUrl = "https://github.com/pratyush-deve",
            linkedinUrl = "https://www.linkedin.com/in/pratyush-nishank/",
            onLinkClick = onLinkClick,
            gradientColors = listOf(Color(0xFF6366F1), Color(0xFFA855F7)) // Indigo to Purple
        )

        DeveloperCard(
            name = "Apoorva Deep",
            role = "Founder",
            initials = "AD",
            githubUrl = "https://github.com/cdr-APD",
            linkedinUrl = "https://www.linkedin.com/in/apoorva-deep/",
            onLinkClick = onLinkClick,
            gradientColors = listOf(Color(0xFF3B82F6), Color(0xFF10B981)) // Blue to Emerald
        )
    }
}

@Composable
private fun DeveloperCard(
    name: String,
    role: String,
    initials: String,
    githubUrl: String,
    linkedinUrl: String,
    onLinkClick: (String) -> Unit,
    gradientColors: List<Color>
) {
    Card(
        modifier = Modifier.border(
            1.dp,
            Brush.linearGradient(listOf(gradientColors[0].copy(alpha = 0.2f), Color.Transparent)),
            shape = RoundedCornerShape(22.dp)
        ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Custom avatar initials badge with glowing radial gradient
            Surface(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.radialGradient(gradientColors)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = role,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Social Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = { onLinkClick(githubUrl) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "GitHub profile of $name",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = { onLinkClick(linkedinUrl) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "LinkedIn profile of $name",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Section 5: External Links Section
 */
@Composable
private fun ExternalLinksSection(
    onPrivacyPolicyClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Resources & Links",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )

        OutlinedButton(
            onClick = onPrivacyPolicyClick, // Triggers "Coming Soon" Alert Dialog
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Icon(
                imageVector = Icons.Default.PrivacyTip,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Privacy Policy",
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Sections 7-8: Built For Students & Footer Card
 */
@Composable
private fun BuiltForStudentsAndFooter(
    versionName: String,
    buildNumber: String
) {
    // Pulse animation for the heart icon
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
        // Built For Students Card
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

        // Elegant Footer
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
                text = "© ${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)} DevForge Labs. All rights reserved.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Custom template for features cards
 */
@Composable
private fun PremiumInfoCard(
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
private fun FeatureRow(
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

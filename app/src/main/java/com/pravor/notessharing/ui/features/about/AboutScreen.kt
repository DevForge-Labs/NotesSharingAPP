package com.pravor.notessharing.ui.features.about

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pravor.notessharing.ui.features.about.components.AboutSectionWrapper
import com.pravor.notessharing.ui.features.about.components.BuiltForStudentsAndFooter
import com.pravor.notessharing.ui.features.about.components.ExploreDiscoveryCard
import com.pravor.notessharing.ui.features.about.components.ExternalLinksSection
import com.pravor.notessharing.ui.features.about.components.HeroHeaderCard
import com.pravor.notessharing.ui.features.about.components.HomeExperienceCard
import com.pravor.notessharing.ui.features.about.components.MeetDevelopersSection
import com.pravor.notessharing.ui.features.about.components.SocialLearningCard
import com.pravor.notessharing.ui.features.about.components.UploadContributionCard
import com.pravor.notessharing.ui.features.about.components.WhyCampusPagesChipsSection
import kotlinx.coroutines.delay
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

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

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

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
                item {
                    AboutSectionWrapper(index = 0, isVisible = isVisible) {
                        HeroHeaderCard(
                            versionName = versionName,
                            buildNumber = buildNumber
                        )
                    }
                }

                item {
                    AboutSectionWrapper(index = 1, isVisible = isVisible) {
                        HomeExperienceCard()
                    }
                }

                item {
                    AboutSectionWrapper(index = 2, isVisible = isVisible) {
                        ExploreDiscoveryCard()
                    }
                }

                item {
                    AboutSectionWrapper(index = 3, isVisible = isVisible) {
                        SocialLearningCard()
                    }
                }

                item {
                    AboutSectionWrapper(index = 4, isVisible = isVisible) {
                        UploadContributionCard()
                    }
                }

                item {
                    AboutSectionWrapper(index = 5, isVisible = isVisible) {
                        WhyCampusPagesChipsSection()
                    }
                }

                item {
                    AboutSectionWrapper(index = 6, isVisible = isVisible) {
                        MeetDevelopersSection(onLinkClick = openUrl)
                    }
                }

                item {
                    AboutSectionWrapper(index = 7, isVisible = isVisible) {
                        ExternalLinksSection(
                            onPrivacyPolicyClick = {
                                openUrl("https://devforge-labs.github.io/NotesSharingAPP/privacy-policy.html")
                            }
                        )
                    }
                }

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
}

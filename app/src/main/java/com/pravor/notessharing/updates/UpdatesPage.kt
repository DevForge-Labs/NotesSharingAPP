package com.pravor.notessharing.updates

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pravor.notessharing.updates.components.ConvergingResourcesVisual
import com.pravor.notessharing.updates.components.RippleCommunityVisual
import com.pravor.notessharing.updates.components.StaggeredCardsVisual
import com.pravor.notessharing.updates.model.UpdatePageModel

@Composable
fun UpdatesPage(
    model: UpdatePageModel,
    isActive: Boolean,
    onJoinCommunityClick: () -> Unit,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (model.layoutType) {
        UpdatePageModel.LayoutType.CONVERGING_RESOURCES -> {
            StandardOnboardingLayout(
                headline = model.headline,
                supportingText = model.supportingText,
                visual = { ConvergingResourcesVisual(isActive = isActive) },
                modifier = modifier
            )
        }
        UpdatePageModel.LayoutType.STAGGERED_CARDS -> {
            StandardOnboardingLayout(
                headline = model.headline,
                supportingText = model.supportingText,
                visual = { StaggeredCardsVisual(isActive = isActive) },
                modifier = modifier
            )
        }
        UpdatePageModel.LayoutType.RIPPLE_COMMUNITY -> {
            StandardOnboardingLayout(
                headline = model.headline,
                supportingText = model.supportingText,
                visual = { RippleCommunityVisual(isActive = isActive) },
                modifier = modifier
            )
        }
        UpdatePageModel.LayoutType.WELCOME_INSPIRING -> {
            FinalWelcomeOnboardingLayout(
                headline = model.headline,
                supportingText = model.supportingText,
                onJoinCommunityClick = onJoinCommunityClick,
                onSignInClick = onSignInClick,
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        visual()
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = headline,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = supportingText,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp)
        )
    }
}

@Composable
fun FinalWelcomeOnboardingLayout(
    headline: String,
    supportingText: String,
    onJoinCommunityClick: () -> Unit,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            // Inspiring visual icon box
            Box(
                modifier = Modifier
                    .size(90.dp)
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
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Headline
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Mission list
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BulletRow(text = "Learn from your seniors.")
                    BulletRow(text = "Help your juniors.")
                    BulletRow(text = "Build something valuable together.")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Social Proof Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatBadge(
                    stat = "10K+",
                    label = "Active Students",
                    modifier = Modifier.weight(1f)
                )
                StatBadge(
                    stat = "8K+",
                    label = "Resources Shared",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Category Chips Label
            Text(
                text = "RESOURCE CATEGORIES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Chips Container
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
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Action Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Join the Community CTA
            Button(
                onClick = onJoinCommunityClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(20.dp)),
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

            // Sign In Link
            Row(
                modifier = Modifier.clickable(onClick = onSignInClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sign in to continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
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
private fun StatBadge(stat: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stat,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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

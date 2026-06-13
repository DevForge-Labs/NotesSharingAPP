package com.pravor.notessharing.updates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import com.pravor.notessharing.updates.components.PageIndicator
import com.pravor.notessharing.updates.data.OnboardingPages
import kotlinx.coroutines.launch

@Composable
fun UpdatesPager(
    onJoinCommunityClick: () -> Unit,
    onSignInClick: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = OnboardingPages.pages
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceContainerLowest
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar with Skip Button (Screens 1 to 3)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (pagerState.currentPage < pages.lastIndex) {
                    TextButton(
                        onClick = onSkipClick
                    ) {
                        Text(
                            text = "Skip",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // Horizontal Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                val absOffset = kotlin.math.abs(pageOffset).coerceIn(0f, 1f)
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (pageOffset > 0) {
                                // Receding page (left)
                                alpha = (1f - pageOffset).coerceIn(0f, 1f)
                                val scale = (1f - 0.06f * pageOffset).coerceIn(0.94f, 1f)
                                scaleX = scale
                                scaleY = scale
                                translationX = pageOffset * size.width
                            } else {
                                // Incoming page (right)
                                alpha = 1f - absOffset
                                val scale = 1f - 0.04f * absOffset
                                scaleX = scale
                                scaleY = scale
                                translationX = 0f
                            }
                        }
                ) {
                    UpdatesPage(
                        model = pages[page],
                        isActive = pagerState.currentPage == page,
                        onJoinCommunityClick = onJoinCommunityClick,
                        onSignInClick = onSignInClick,
                        pageOffset = pageOffset,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Bottom Navigation Controller for Onboarding (Screens 1 to 3)
            if (pagerState.currentPage < pages.lastIndex) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pill Indicator
                    PageIndicator(
                        pageCount = pages.size,
                        currentPage = pagerState.currentPage
                    )

                    // Navigation Button with press-scale feedback
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val ctaScale by animateFloatAsState(targetValue = if (isPressed) 0.96f else 1.0f, label = "ctaScale")
                    
                    val buttonText = if (pagerState.currentPage == pages.lastIndex - 1) "Continue" else "Next"
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        interactionSource = interactionSource,
                        modifier = Modifier.graphicsLayer {
                            scaleX = ctaScale
                            scaleY = ctaScale
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = buttonText,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

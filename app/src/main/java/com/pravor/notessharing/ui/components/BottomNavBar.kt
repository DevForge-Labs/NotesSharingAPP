package com.pravor.notessharing.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pravor.notessharing.ui.navigation.AppDestination

private data class ResponsiveNavTokens(
    val outerHorizontalPadding: Dp,
    val boxPadding: Dp,
    val itemHorizontalPadding: Dp,
    val iconLabelSpacer: Dp,
    val textStyle: TextStyle
)

@Composable
fun BottomNavBar(
    destinations: List<AppDestination>,
    currentRoute: String?,
    onDestinationClick: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val mandatoryGestureInset = WindowInsets.mandatorySystemGestures.asPaddingValues().calculateBottomPadding()
    val isGestureMode = mandatoryGestureInset > 0.dp
    val bottomPadding = if (isGestureMode) 0.dp else 20.dp

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f)
    val labelMediumStyle = MaterialTheme.typography.labelMedium
    val labelSmallStyle = MaterialTheme.typography.labelSmall

    val borderBrush = remember(primaryColor, secondaryColor) {
        Brush.linearGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.22f),
                secondaryColor.copy(alpha = 0.15f),
                primaryColor.copy(alpha = 0.22f)
            )
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val availableWidth = maxWidth

        // Derive responsive layout tokens dynamically based on available width constraints.
        // Recalculated ONLY when container dimensions change (e.g. device width, rotation, split screen),
        // never during animation frames.
        val tokens = remember(availableWidth, labelMediumStyle, labelSmallStyle) {
            when {
                availableWidth >= 400.dp -> {
                    ResponsiveNavTokens(
                        outerHorizontalPadding = 18.dp,
                        boxPadding = 6.dp,
                        itemHorizontalPadding = 6.dp,
                        iconLabelSpacer = 6.dp,
                        textStyle = labelMediumStyle
                    )
                }
                availableWidth >= 350.dp -> {
                    ResponsiveNavTokens(
                        outerHorizontalPadding = 14.dp,
                        boxPadding = 4.dp,
                        itemHorizontalPadding = 4.dp,
                        iconLabelSpacer = 4.dp,
                        textStyle = labelMediumStyle
                    )
                }
                else -> {
                    ResponsiveNavTokens(
                        outerHorizontalPadding = 10.dp,
                        boxPadding = 2.dp,
                        itemHorizontalPadding = 3.dp,
                        iconLabelSpacer = 3.dp,
                        textStyle = labelSmallStyle
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = tokens.outerHorizontalPadding,
                    top = 10.dp,
                    end = tokens.outerHorizontalPadding,
                    bottom = bottomPadding
                ),
            shape = RoundedCornerShape(30.dp),
            color = surfaceContainerHigh,
            border = BorderStroke(0.8.dp, borderBrush),
            tonalElevation = 10.dp,
            shadowElevation = 14.dp
        ) {
            Box(
                modifier = Modifier.padding(tokens.boxPadding)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    destinations.forEach { destination ->
                        val selected = currentRoute == destination.route

                        val itemColor by animateColorAsState(
                            targetValue = if (selected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            label = "bottom-nav-content"
                        )

                        val backgroundColor by animateColorAsState(
                            targetValue = if (selected) primaryColor.copy(alpha = 0.08f) else Color.Transparent,
                            label = "bottom-nav-bg"
                        )

                        val borderColor by animateColorAsState(
                            targetValue = if (selected) primaryColor.copy(alpha = 0.24f) else Color.Transparent,
                            label = "bottom-nav-border"
                        )

                        Surface(
                            onClick = { onDestinationClick(destination) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(24.dp),
                            color = backgroundColor,
                            border = BorderStroke(1.dp, borderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = tokens.itemHorizontalPadding),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label,
                                    modifier = Modifier.size(20.dp),
                                    tint = itemColor
                                )
                                AnimatedVisibility(
                                    visible = selected,
                                    enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start, clip = false),
                                    exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start, clip = false)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Spacer(modifier = Modifier.width(tokens.iconLabelSpacer))
                                        Text(
                                            text = destination.label,
                                            style = tokens.textStyle,
                                            color = itemColor,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Visible
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






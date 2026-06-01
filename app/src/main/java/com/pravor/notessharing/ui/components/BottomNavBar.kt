package com.pravor.notessharing.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pravor.notessharing.ui.navigation.AppDestination

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

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start = 18.dp,
                top = 10.dp,
                end = 18.dp,
                bottom = bottomPadding
            ),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
        border = BorderStroke(
            width = 0.8.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                )
            )
        ),
        tonalElevation = 10.dp,
        shadowElevation = 14.dp
    ) {
        Box(
            modifier = Modifier.padding(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                destinations.forEach { destination ->
                    val selected = currentRoute == destination.route
                    
                    val itemColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        label = "bottom-nav-content"
                    )
                    
                    val backgroundColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
                        label = "bottom-nav-bg"
                    )
                    
                    val borderColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.24f) else Color.Transparent,
                        label = "bottom-nav-border"
                    )
                    
                    val horizontalPadding by animateDpAsState(
                        targetValue = if (selected) 14.dp else 10.dp,
                        label = "bottom-nav-padding"
                    )

                    Surface(
                        onClick = { onDestinationClick(destination) },
                        shape = RoundedCornerShape(24.dp),
                        color = backgroundColor,
                        border = BorderStroke(1.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 15.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                                modifier = Modifier.size(20.dp),
                                tint = itemColor
                            )
                            AnimatedVisibility(visible = selected) {
                                Text(
                                    text = destination.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = itemColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

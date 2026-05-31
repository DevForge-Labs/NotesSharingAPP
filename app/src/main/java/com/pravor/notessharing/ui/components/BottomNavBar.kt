package com.pravor.notessharing.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
        tonalElevation = 10.dp,
        shadowElevation = 14.dp
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            destinations.forEach { destination ->
                val selected = currentRoute == destination.route
                val itemColor by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "bottom-nav-content"
                )
                val itemBackground by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    label = "bottom-nav-background"
                )
                val horizontalPadding by animateDpAsState(
                    targetValue = if (selected) 14.dp else 10.dp,
                    label = "bottom-nav-padding"
                )

                Surface(
                    modifier = Modifier.clickable { onDestinationClick(destination) },
                    shape = RoundedCornerShape(24.dp),
                    color = itemBackground
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

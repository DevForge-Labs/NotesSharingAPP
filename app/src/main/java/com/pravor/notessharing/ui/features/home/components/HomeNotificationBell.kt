package com.pravor.notessharing.ui.features.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pravor.notessharing.ui.theme.ElectricBlue
import kotlinx.coroutines.delay

@Composable
fun HomeNotificationBell(
    unreadCount: Int,
    onBellClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bellRotation = remember { Animatable(0f) }
    LaunchedEffect(unreadCount) {
        if (unreadCount > 0) {
            while (true) {
                bellRotation.animateTo(-4f, animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing))
                bellRotation.animateTo(4f, animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing))
                bellRotation.animateTo(-3f, animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing))
                bellRotation.animateTo(3f, animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing))
                bellRotation.animateTo(0f, animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing))
                delay(8000)
            }
        } else {
            bellRotation.snapTo(0f)
        }
    }

    Surface(
        shape = CircleShape,
        color = ElectricBlue.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.30f)),
        shadowElevation = 3.dp,
        modifier = modifier.size(38.dp)
    ) {
        IconButton(
            onClick = onBellClick,
            modifier = Modifier.fillMaxSize()
        ) {
            BadgedBox(
                badge = {
                    if (unreadCount > 0) {
                        val badgeText = if (unreadCount > 99) "99+" else unreadCount.toString()
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = ElectricBlue,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            rotationZ = bellRotation.value
                            transformOrigin = TransformOrigin(0.5f, 0.0f)
                        }
                )
            }
        }
    }
}

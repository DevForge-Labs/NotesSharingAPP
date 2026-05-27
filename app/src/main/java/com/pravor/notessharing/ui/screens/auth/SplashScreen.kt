package com.pravor.notessharing.ui.screens.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pravor.notessharing.state.SessionState
import com.pravor.notessharing.auth.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    viewModel: AuthViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToAuth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sessionState by viewModel.sessionState.collectAsState()
    
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = tween(1000))
    }
    
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(1000))
    }
    
    LaunchedEffect(sessionState) {
        if (sessionState != SessionState.Checking) {
            // Let the splash animation run for at least 1.5 seconds for visual premium feel
            delay(1500)
            if (sessionState == SessionState.LoggedIn) {
                onNavigateToHome()
            } else {
                onNavigateToAuth()
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale.value).alpha(alpha.value)
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.FilePresent,
                        contentDescription = "App Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Notes Sharing",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Share Knowledge • Study Together",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

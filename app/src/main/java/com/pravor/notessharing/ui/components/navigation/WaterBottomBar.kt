package com.pravor.notessharing.ui.components.navigation

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pravor.notessharing.ui.navigation.AppDestination
import com.pravor.notessharing.ui.components.navigation.model.WaterWaveState

@Composable
fun WaterBottomBar(
    destinations: List<AppDestination>,
    currentRoute: String?,
    onDestinationClick: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val waveState = remember { WaterWaveState() }
    var showDebugPanel by remember { mutableStateOf(false) }

    val selectedIndex = remember(currentRoute, destinations) {
        destinations.indexOfFirst { it.route == currentRoute }.takeIf { it != -1 }
    }

    var layoutWidth by remember { mutableStateOf(0f) }
    var layoutHeight by remember { mutableStateOf(0f) }

    // Tab coordinates animatables
    val centerX = remember { Animatable(0f) }
    var isInitialized by remember { mutableStateOf(false) }
    var isInitializedPhysics by remember { mutableStateOf(false) }

    // Animate tab horizontal coordinate smoothly
    LaunchedEffect(selectedIndex, layoutWidth) {
        if (layoutWidth > 0f && selectedIndex != null) {
            val targetX = (selectedIndex + 0.5f) * (layoutWidth / destinations.size)

            if (!isInitialized) {
                centerX.snapTo(targetX)
                isInitialized = true
                return@LaunchedEffect
            }

            // Animate horizontally with custom spring curves (inertia and speed)
            centerX.animateTo(
                targetValue = targetX,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    val density = LocalDensity.current.density

    // Run the physics step loop every frame using Choreographer ticks
    LaunchedEffect(layoutWidth, layoutHeight) {
        if (layoutWidth > 0f && layoutHeight > 0f) {
            // Smooth initial baseline initialization to prevent a startup splash
            val baseline = layoutHeight * (1f - waveState.fillLevel)
            if (!isInitializedPhysics) {
                for (i in 0 until waveState.numColumns) {
                    waveState.heights[i] = baseline
                }
                isInitializedPhysics = true
            }

            var lastFrameTime = 0L
            while (true) {
                withFrameMillis { frameTime ->
                    val dt = if (lastFrameTime == 0L) 0.016f else (frameTime - lastFrameTime) / 1000f
                    lastFrameTime = frameTime

                    if (!waveState.isReducedMotion) {
                        waveState.timeAccumulator += dt
                    }

                    WaterPhysics.step(
                        state = waveState,
                        width = layoutWidth,
                        height = layoutHeight,
                        centerX = centerX.value,
                        density = density
                    )
                }
            }
        }
    }

    val mandatoryGestureInset = WindowInsets.mandatorySystemGestures.asPaddingValues().calculateBottomPadding()
    val isGestureMode = mandatoryGestureInset > 0.dp
    val bottomPadding = if (isGestureMode) 0.dp else 20.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start = 18.dp,
                top = 10.dp,
                end = 18.dp,
                bottom = bottomPadding
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Dynamic floating debug settings panel
        if (showDebugPanel) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Water Physics Debug Panel",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { showDebugPanel = false }) {
                            Text("Close")
                        }
                    }

                    // Reduced Motion Fallback Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Static Fallback (Reduced Motion)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = waveState.isReducedMotion,
                            onCheckedChange = { waveState.isReducedMotion = it }
                        )
                    }

                    if (!waveState.isReducedMotion) {
                        // Water Fill Level slider
                        Column {
                            val formattedLevel = ((waveState.fillLevel * 100).toInt()).toString() + "%"
                            Text(
                                text = "Water Fill Level: $formattedLevel",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = waveState.fillLevel,
                                onValueChange = { waveState.fillLevel = it },
                                valueRange = 0.50f..0.90f
                            )
                        }

                        // Depression Depth slider
                        Column {
                            val formattedDepth = ((waveState.depressionDepth * 10).toInt() / 10f).toString() + "dp"
                            Text(
                                text = "Depression Depth: $formattedDepth",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = waveState.depressionDepth,
                                onValueChange = { waveState.depressionDepth = it },
                                valueRange = 15f..55f
                            )
                        }

                        // Tension slider
                        Column {
                            val formattedTension = ((waveState.tension * 1000).toInt() / 1000f).toString()
                            Text(
                                text = "Surface Tension: $formattedTension",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = waveState.tension,
                                onValueChange = { waveState.tension = it },
                                valueRange = 0.005f..0.04f
                            )
                        }

                        // Damping slider
                        Column {
                            val formattedDamping = ((waveState.damping * 100).toInt() / 100f).toString()
                            Text(
                                text = "Fluid Damping: $formattedDamping",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = waveState.damping,
                                onValueChange = { waveState.damping = it },
                                valueRange = 0.90f..0.99f
                            )
                        }
                    }
                }
            }
        }

        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        val backingColor = if (isDark) Color(0xFF071A2E) else Color(0xFFF0F4F8)

        // Main Water Bar Glass Container (Glassmorphic solid backing container)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            shape = RoundedCornerShape(30.dp),
            color = backingColor, // Fully opaque premium backing
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.60f),
                        Color.White.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.50f)
                    )
                )
            ),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(30.dp))
                    // Long press to toggle the debug settings panel
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                showDebugPanel = !showDebugPanel
                            }
                        )
                    }
            ) {
                // 1. Background Liquid Water Layer
                if (layoutWidth > 0f && layoutHeight > 0f) {
                    WaterSurface(
                        waveState = waveState,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 2. Render destinations on top (foreground layer for perfect visibility)
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val w = constraints.maxWidth.toFloat()
                    val h = constraints.maxHeight.toFloat()
                    
                    LaunchedEffect(w, h) {
                        layoutWidth = w
                        layoutHeight = h
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        destinations.forEachIndexed { index, destination ->
                            val isSelected = index == selectedIndex
                            WaterNavItem(
                                destination = destination,
                                isSelected = isSelected,
                                onClick = { onDestinationClick(destination) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }
    }
}

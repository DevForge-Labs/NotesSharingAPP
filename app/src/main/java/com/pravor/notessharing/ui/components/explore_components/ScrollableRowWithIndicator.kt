package com.pravor.notessharing.ui.components.explore_components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pravor.notessharing.ui.components.AdaptiveScrollbar
import com.pravor.notessharing.ui.components.ScrollbarOrientation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun ScrollableRowWithIndicator(
    state: LazyListState = rememberLazyListState(),
    onSeeMoreClick: (() -> Unit)? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onScrollableChanged: ((Boolean) -> Unit)? = null,
    content: LazyListScope.() -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Detect if scrollable (has content beyond viewport bounds)
    val scrollable by remember(state) {
        derivedStateOf {
            state.canScrollForward || state.canScrollBackward
        }
    }

    LaunchedEffect(scrollable) {
        onScrollableChanged?.invoke(scrollable)
    }

    // Interactive Overscroll Reveal parameters
    val overscrollAmount = remember { Animatable(0f) }
    var isStickyRevealed by remember { mutableStateOf(false) }
    var hapticTriggered by remember { mutableStateOf(false) }
    var collapseJob by remember { mutableStateOf<Job?>(null) }

    val density = LocalDensity.current
    val thresholdPx = remember(density) { with(density) { 90.dp.toPx() } }
    val maxOverscrollPx = remember(density) { with(density) { 150.dp.toPx() } }
    val stickyRevealPx = remember(density) { with(density) { 100.dp.toPx() } }

    val nestedScrollConnection = remember(onSeeMoreClick, scrollable, thresholdPx, maxOverscrollPx) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Only intercept drag overscroll if the row is scrollable and See More is available
                if (onSeeMoreClick != null && scrollable && source == NestedScrollSource.UserInput && available.x < 0) {
                    collapseJob?.cancel()
                    collapseJob = null

                    val newOverscroll = (overscrollAmount.value + available.x).coerceAtLeast(-maxOverscrollPx)
                    coroutineScope.launch {
                        overscrollAmount.snapTo(newOverscroll)
                    }

                    // Trigger light haptic feedback once when crossing threshold
                    val currentOverscroll = abs(newOverscroll)
                    if (currentOverscroll >= thresholdPx) {
                        if (!hapticTriggered) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            hapticTriggered = true
                        }
                    } else {
                        hapticTriggered = false
                    }

                    return Offset(available.x, 0f)
                }
                return Offset.Zero
            }

            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Intercept scroll back when overscrolled
                if (onSeeMoreClick != null && scrollable && source == NestedScrollSource.UserInput && available.x > 0 && overscrollAmount.value < 0) {
                    collapseJob?.cancel()
                    collapseJob = null

                    val newOverscroll = (overscrollAmount.value + available.x).coerceAtMost(0f)
                    coroutineScope.launch {
                        overscrollAmount.snapTo(newOverscroll)
                    }

                    val currentOverscroll = abs(newOverscroll)
                    if (currentOverscroll >= thresholdPx) {
                        if (!hapticTriggered) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            hapticTriggered = true
                        }
                    } else {
                        hapticTriggered = false
                    }

                    return Offset(available.x, 0f)
                }
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onSeeMoreClick != null && scrollable) {
                    Modifier
                        .nestedScroll(nestedScrollConnection)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    // Detect touch down / touch move to cancel auto collapse
                                    if (event.changes.any { it.pressed }) {
                                        collapseJob?.cancel()
                                        collapseJob = null
                                    }

                                    // Detect finger lift/release
                                    if (event.changes.all { !it.pressed }) {
                                        val currentOverscroll = overscrollAmount.value
                                        if (currentOverscroll != 0f) {
                                            if (abs(currentOverscroll) >= thresholdPx) {
                                                // Release after threshold -> sticky reveal
                                                isStickyRevealed = true
                                                coroutineScope.launch {
                                                    overscrollAmount.animateTo(
                                                        targetValue = -stickyRevealPx,
                                                        animationSpec = spring(
                                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                                            stiffness = Spring.StiffnessLow
                                                        )
                                                    )
                                                }

                                                // Collapse back after a short delay if user doesn't tap
                                                collapseJob?.cancel()
                                                collapseJob = coroutineScope.launch {
                                                    delay(2500)
                                                    isStickyRevealed = false
                                                    overscrollAmount.animateTo(
                                                        targetValue = 0f,
                                                        animationSpec = spring(
                                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                                            stiffness = Spring.StiffnessMedium
                                                        )
                                                    )
                                                }
                                            } else {
                                                // Release before threshold -> spring back fully
                                                isStickyRevealed = false
                                                coroutineScope.launch {
                                                    overscrollAmount.animateTo(
                                                        targetValue = 0f,
                                                        animationSpec = spring(
                                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                                            stiffness = Spring.StiffnessLow
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                } else Modifier
            )
    ) {
        if (onSeeMoreClick != null && scrollable) {
            // Revealed Action: Rounded Pill, Dark Surface, Accent-colored icon/text
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .width(64.dp)
                    .height(130.dp) // Fits vertically centered row contents nicely
                    .clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f))
                    .border(
                        BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                        RoundedCornerShape(26.dp)
                    )
                    .clickable(enabled = isStickyRevealed) {
                        collapseJob?.cancel()
                        collapseJob = null
                        isStickyRevealed = false
                        coroutineScope.launch {
                            overscrollAmount.snapTo(0f)
                        }
                        onSeeMoreClick()
                    }
                    .graphicsLayer {
                        val progress = (abs(overscrollAmount.value) / thresholdPx).coerceIn(0f, 1f)
                        alpha = progress
                        scaleX = 0.8f + 0.2f * progress
                        scaleY = 0.8f + 0.2f * progress
                        translationX = 20.dp.toPx() * (1f - progress)
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "See\nMore",
                        style = MaterialTheme.typography.labelSmall.copy(
                            lineHeight = 12.sp,
                            textAlign = TextAlign.Center
                        ),
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // LazyRow Content Container (translates left under overscroll)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    // Apply touch resistance translation
                    translationX = overscrollAmount.value * 0.7f
                }
        ) {
            LazyRow(
                state = state,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 6.dp),
                content = content
            )
            AdaptiveScrollbar(
                listState = state,
                orientation = ScrollbarOrientation.Horizontal
            )
        }
    }
}

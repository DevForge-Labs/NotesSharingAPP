package com.pratyush.notessharing.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

enum class ScrollbarOrientation {
    Vertical,
    Horizontal
}

@Stable
data class ScrollbarState(
    val progress: Float,
    val thumbFraction: Float,
    val isVisible: Boolean
)

@Composable
fun rememberScrollbarState(listState: LazyListState): ScrollbarState {
    val state by remember(listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val visibleItems = layoutInfo.visibleItemsInfo

            if (totalItems == 0 || visibleItems.isEmpty()) {
                ScrollbarState(progress = 0f, thumbFraction = 1f, isVisible = false)
            } else {
                val firstVisible = visibleItems.first()
                val lastVisible = visibleItems.last()
                val viewportSize = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).coerceAtLeast(1)
                val averageItemSize = visibleItems.sumOf { it.size }.toFloat() / visibleItems.size
                val estimatedContentSize = (averageItemSize * totalItems).coerceAtLeast(viewportSize.toFloat())
                val currentOffset = (firstVisible.index * averageItemSize - firstVisible.offset).coerceAtLeast(0f)
                val maxOffset = (estimatedContentSize - viewportSize).coerceAtLeast(1f)
                val visibleFraction = (viewportSize / estimatedContentSize).coerceIn(0.08f, 1f)
                val canScroll = totalItems > visibleItems.size || lastVisible.index < totalItems - 1

                ScrollbarState(
                    progress = (currentOffset / maxOffset).coerceIn(0f, 1f),
                    thumbFraction = visibleFraction,
                    isVisible = listState.isScrollInProgress && canScroll
                )
            }
        }
    }
    return state
}

@Composable
fun BoxScope.AdaptiveScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    orientation: ScrollbarOrientation = ScrollbarOrientation.Vertical,
    thickness: Dp = 3.dp,
    minThumbSize: Dp = 36.dp
) {
    val scrollbarState = rememberScrollbarState(listState)
    val alpha by animateFloatAsState(
        targetValue = if (scrollbarState.isVisible) 0.75f else 0f,
        label = "scrollbar-alpha"
    )
    val animatedProgress by animateFloatAsState(
        targetValue = scrollbarState.progress,
        label = "scrollbar-progress"
    )
    val density = LocalDensity.current
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)

    if (orientation == ScrollbarOrientation.Vertical) {
        BoxWithConstraints(
            modifier = modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(thickness)
                .padding(vertical = 20.dp)
                .alpha(alpha)
                .background(trackColor, RoundedCornerShape(99.dp))
        ) {
            val trackHeightPx = with(density) { maxHeight.toPx() }
            val thumbHeight = maxOf(minThumbSize, maxHeight * scrollbarState.thumbFraction)
            val maxOffset = (trackHeightPx - with(density) { thumbHeight.toPx() }).coerceAtLeast(0f)
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, (maxOffset * animatedProgress).roundToInt()) }
                    .width(thickness)
                    .height(thumbHeight)
                    .background(thumbColor, RoundedCornerShape(99.dp))
            )
        }
    } else {
        BoxWithConstraints(
            modifier = modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(thickness)
                .padding(horizontal = 20.dp)
                .alpha(alpha)
                .background(trackColor, RoundedCornerShape(99.dp))
        ) {
            val trackWidthPx = with(density) { maxWidth.toPx() }
            val thumbWidth = maxOf(minThumbSize, maxWidth * scrollbarState.thumbFraction)
            val maxOffset = (trackWidthPx - with(density) { thumbWidth.toPx() }).coerceAtLeast(0f)
            Box(
                modifier = Modifier
                    .offset { IntOffset((maxOffset * animatedProgress).roundToInt(), 0) }
                    .height(thickness)
                    .width(thumbWidth)
                    .background(thumbColor, RoundedCornerShape(99.dp))
            )
        }
    }
}

package com.pravor.notessharing.ui.common

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*


import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import com.pravor.notessharing.ui.features.explore.components.ClimbingMascotScrollbar
import com.pravor.notessharing.ui.features.explore.components.MonkeyMascot
import com.pravor.notessharing.ui.features.explore.components.RunningSquirrelScrollbar
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
    if (orientation == ScrollbarOrientation.Vertical) {
        ClimbingMascotScrollbar(
            listState = listState,
            modifier = modifier
        ) { mod, isScrolling ->
            MonkeyMascot(modifier = mod, isScrolling = isScrolling)
        }
    } else {
        RunningSquirrelScrollbar(
            listState = listState,
            modifier = modifier
        )
    }
}

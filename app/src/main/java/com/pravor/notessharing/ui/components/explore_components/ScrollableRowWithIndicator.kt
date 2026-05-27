package com.pravor.notessharing.ui.components.explore_components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.pravor.notessharing.ui.components.AdaptiveScrollbar
import com.pravor.notessharing.ui.components.ScrollbarOrientation

@Composable
fun ScrollableRowWithIndicator(
    content: LazyListScope.() -> Unit
) {
    val rowState = rememberLazyListState()
    Box {
        LazyRow(
            state = rowState,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 6.dp),
            content = content
        )
        AdaptiveScrollbar(
            listState = rowState,
            orientation = ScrollbarOrientation.Horizontal
        )
    }
}

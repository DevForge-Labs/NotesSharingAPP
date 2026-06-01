package com.pravor.notessharing.ui.components.imageViewing

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pravor.notessharing.ui.components.loading.StudyLoadingIndicator

@Composable
fun ImageLoadingView(
    modifier: Modifier = Modifier
) {
    StudyLoadingIndicator(
        text = "Opening Image...",
        modifier = modifier
    )
}

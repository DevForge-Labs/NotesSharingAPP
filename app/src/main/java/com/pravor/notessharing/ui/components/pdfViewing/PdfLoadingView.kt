package com.pravor.notessharing.ui.components.pdfViewing

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pravor.notessharing.ui.components.loading.StudyLoadingIndicator

@Composable
fun PdfLoadingView(
    modifier: Modifier = Modifier
) {
    StudyLoadingIndicator(
        text = "Opening Document...",
        modifier = modifier
    )
}

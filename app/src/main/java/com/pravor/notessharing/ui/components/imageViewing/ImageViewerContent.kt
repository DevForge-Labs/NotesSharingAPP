package com.pravor.notessharing.ui.components.imageViewing

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.io.File

@Composable
fun ImageViewerContent(
    imageFile: File,
    modifier: Modifier = Modifier
) {
    ZoomableImage(
        imageFile = imageFile,
        modifier = modifier
    )
}

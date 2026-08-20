package com.pravor.notessharing.ui.features.documentViewing.components

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*

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

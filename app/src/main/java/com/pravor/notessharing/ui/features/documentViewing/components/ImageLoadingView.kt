package com.pravor.notessharing.ui.features.documentViewing.components

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pravor.notessharing.ui.common.loading.StudyLoadingIndicator

@Composable
fun ImageLoadingView(
    modifier: Modifier = Modifier
) {
    StudyLoadingIndicator(
        text = "Opening Image...",
        modifier = modifier
    )
}

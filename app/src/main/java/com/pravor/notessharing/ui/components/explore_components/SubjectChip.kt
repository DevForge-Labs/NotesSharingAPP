package com.pravor.notessharing.ui.components.explore_components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SubjectChip(subject: String) {
    val background by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "subject-chip-background"
    )
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = background,
        tonalElevation = 3.dp
    ) {
        Text(
            text = subject,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

package com.pravor.notessharing.ui.features.document

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pravor.notessharing.domain.model.DocumentDetail

@Composable
fun DocumentMetadataSection(
    doc: DocumentDetail,
    modifier: Modifier = Modifier
) {
    val rawDocType = doc.documentType.lowercase(java.util.Locale.ROOT).trim()
    val docType = when (rawDocType) {
        "pyq" -> "PYQ"
        "cheatsheet", "cheat sheet" -> "Cheat Sheet"
        "assignment" -> "Assignment"
        "notes" -> "Notes"
        else -> "Notes"
    }

    val icon = when (docType) {
        "PYQ" -> Icons.Default.Help
        "Assignment" -> Icons.Default.Assignment
        "Cheat Sheet" -> Icons.Default.Bolt
        else -> Icons.Default.Description
    }

    val accentColor = when (docType) {
        "PYQ" -> Color(0xFFE57373) // Softer Red
        "Assignment" -> Color(0xFF81C784) // Softer Green
        "Cheat Sheet" -> Color(0xFFFFD54F) // Softer Amber
        else -> Color(0xFF64B5F6) // Softer Blue
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Document Type Chip
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = accentColor.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
            modifier = Modifier.height(36.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = docType,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Semester Chip
        if (doc.semester.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.height(36.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = doc.semester,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

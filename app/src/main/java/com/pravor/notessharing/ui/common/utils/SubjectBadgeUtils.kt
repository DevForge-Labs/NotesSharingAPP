package com.pravor.notessharing.ui.common.utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.pravor.notessharing.data.repository.SubjectCatalogRepository
import java.util.Locale
import kotlin.math.abs

/**
 * Rich, vibrant color palette for dynamic hash-based subject badge colors.
 * Ensures consistent, high-contrast, beautiful styling across light and dark modes.
 */
val SubjectBadgePalette = listOf(
    Color(0xFF2563EB), // Blue 600
    Color(0xFF7C3AED), // Violet 600
    Color(0xFF059669), // Emerald 600
    Color(0xFFD97706), // Amber 600
    Color(0xFFDC2626), // Red 600
    Color(0xFFDB2777), // Pink 600
    Color(0xFF4F46E5), // Indigo 600
    Color(0xFF0891B2), // Cyan 600
    Color(0xFF0D9488), // Teal 600
    Color(0xFFEA580C), // Orange 600
    Color(0xFF9333EA), // Purple 600
    Color(0xFF65A30D), // Lime 600
    Color(0xFF0284C7), // Sky 600
    Color(0xFFE11D48), // Rose 600
    Color(0xFF475569), // Slate 600
    Color(0xFF4338CA), // Deep Indigo
    Color(0xFF047857), // Deep Emerald
    Color(0xFFB45309), // Deep Amber
    Color(0xFFBE185D), // Deep Pink
    Color(0xFF16A34A), // Green 600
    Color(0xFFC026D3), // Fuchsia 600
    Color(0xFF0E7490), // Deep Cyan
    Color(0xFFB91C1C), // Deep Red
    Color(0xFF6D28D9)  // Deep Violet
)

private val MultipleSpacesRegex = Regex("\\s+")

/**
 * Normalizes subject string for hashing and comparison.
 */
fun normalizeSubject(subject: String): String {
    return subject.lowercase(Locale.ROOT)
        .trim()
        .replace(MultipleSpacesRegex, " ")
}

/**
 * Deterministically computes a unique, vibrant color for each subject using hashing.
 */
fun getSubjectColor(subject: String): Color {
    val clean = normalizeSubject(subject)
    if (clean.isBlank()) return Color(0xFF64748B)
    val hash = abs(clean.hashCode())
    return SubjectBadgePalette[hash % SubjectBadgePalette.size]
}

/**
 * Returns canonical short name from catalog or normalized fallback.
 */
fun getSubjectDisplayName(originalSubject: String, normalizedSubject: String = ""): String {
    if (originalSubject.isBlank()) return ""
    try {
        val repo = SubjectCatalogRepository.getInstance()
        val shortName = repo.resolveShortName(originalSubject, originalSubject)
        if (shortName.isNotBlank()) {
            return shortName.trim()
        }
    } catch (e: Exception) {
        // Fallback below
    }
    return originalSubject.trim()
}

/**
 * Formats semester string into compact format (e.g. "Semester 5" -> "Sem-5").
 */
fun formatSemesterForSubject(semesterStr: String): String {
    val digits = semesterStr.filter { it.isDigit() }
    val semNumber = digits.toIntOrNull()
    return if (semNumber != null) {
        "Sem-$semNumber"
    } else {
        if (semesterStr.isNotBlank() && semesterStr != "Not Set") {
            semesterStr.replace("Sem ", "Sem-")
        } else {
            "Sem-1"
        }
    }
}

/**
 * Authoritative pill-styled Composable displaying the resource subject badge.
 * - Shows actual canonical short name (e.g. "DAA", "SE", "CN", "OS") from SubjectCatalogRepository.
 * - Uses dynamic hash-based coloring.
 * - Tapping reveals a tooltip with the full subject display name.
 */
@Composable
fun SubjectBadge(
    subject: String,
    modifier: Modifier = Modifier,
    isLarge: Boolean = false,
    semester: String? = null,
    disableNormalization: Boolean = false,
    subjectId: String? = null
) {
    if (subject.isBlank()) return

    val catalogRepo = remember {
        try {
            SubjectCatalogRepository.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    // Dynamic hash-based color derived from canonical subject identifier
    val color = remember(subjectId, subject) {
        val key = subjectId?.takeIf { it.isNotBlank() } ?: subject
        getSubjectColor(key)
    }

    // Canonical short badge name (e.g. "DAA", "SE", "CN") from the catalog
    val shortBadgeName = remember(subject, subjectId, disableNormalization, catalogRepo) {
        if (disableNormalization) {
            subject.trim()
        } else {
            val fromCatalog = catalogRepo?.resolveShortName(subjectId ?: subject, subject)
            if (!fromCatalog.isNullOrBlank()) {
                fromCatalog.trim()
            } else {
                subject.trim().uppercase(Locale.ROOT)
            }
        }
    }

    // Full human-readable display name for the tooltip
    val fullDisplayName = remember(subject, subjectId, catalogRepo) {
        val resolved = catalogRepo?.resolveDisplayName(subjectId ?: subject, subject)
        if (!resolved.isNullOrBlank()) resolved.trim() else subject.trim()
    }

    val displayWithSem = remember(shortBadgeName, semester) {
        if (!semester.isNullOrBlank()) {
            "$shortBadgeName  |  ${formatSemesterForSubject(semester)}"
        } else {
            shortBadgeName
        }
    }

    var showTooltip by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = color.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
            modifier = Modifier
                .clickable { showTooltip = true }
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = if (isLarge) 10.dp else 8.dp,
                    vertical = if (isLarge) 6.dp else 4.dp
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isLarge) 6.dp else 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(if (isLarge) 16.dp else 14.dp)
                )
                Text(
                    text = displayWithSem,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = if (isLarge) 11.sp else 10.sp),
                    color = color,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = if (isLarge) 200.dp else 120.dp)
                )
            }
        }

        if (showTooltip) {
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, -90),
                onDismissRequest = { showTooltip = false },
                properties = PopupProperties(focusable = true)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .padding(8.dp)
                        .widthIn(max = 240.dp)
                ) {
                    Text(
                        text = fullDisplayName,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

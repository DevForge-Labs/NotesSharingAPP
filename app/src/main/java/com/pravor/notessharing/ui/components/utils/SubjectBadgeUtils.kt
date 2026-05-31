package com.pravor.notessharing.ui.components.utils

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

/**
 * Normalizes subject names by:
 * 1. Converting to lowercase.
 * 2. Trimming leading/trailing whitespace.
 * 3. Replacing multiple internal spaces with a single space.
 * 4. Resolving common subject aliases.
 */
fun normalizeSubject(subject: String): String {
    val normalized = subject.lowercase()
        .trim()
        .replace(Regex("\\s+"), " ")

    return when (normalized) {
        "ds", "data structure", "data structures" -> "ds"
        "dbms", "database management system", "database management systems" -> "dbms"
        "os", "operating system", "operating systems" -> "os"
        "cn", "computer network", "computer networks" -> "cn"
        "afl", "automata and formal languages", "automata" -> "afl"
        "coa", "computer organization and architecture", "computer organization" -> "coa"
        "oop", "object oriented programming", "object oriented programming using java", "object oriented programming with java" -> "oop"
        "daa", "design and analysis of algorithms", "design & analysis of algorithms" -> "daa"
        "se", "software engineering" -> "se"
        "ai", "artificial intelligence" -> "ai"
        "ml", "machine learning" -> "ml"
        "dl", "deep learning" -> "dl"
        "physics", "phy" -> "physics"
        "chemistry", "chem" -> "chemistry"
        "mathematics", "maths", "math", "discrete mathematics", "discrete maths", "dm" -> "mathematics"
        "statistics", "stats" -> "statistics"
        "evs", "environmental studies" -> "evs"
        "scls" -> "scls"
        "java" -> "java"
        "python" -> "python"
        "c programming", "c language", "c" -> "c programming"
        "c++", "cpp" -> "c++"
        "web development", "web dev", "web technology", "web tech" -> "web development"
        "android development", "android dev" -> "android development"
        "cloud computing", "cloud" -> "cloud computing"
        "cyber security", "cybersecurity", "security" -> "cyber security"
        "STW", "stw" ,"Stw"-> "STW"
        else -> normalized
    }
}

/**
 * Maps normalized subject names to fixed colors.
 * Fallback color is a neutral Slate/Gray.
 */
fun getSubjectColor(normalizedSubject: String): Color {
    return when (normalizedSubject) {
        "ds" -> Color(0xFF1E88E5)           // Blue
        "dbms" -> Color(0xFF43A047)         // Green
        "os" -> Color(0xFFFB8C00)           // Orange
        "cn" -> Color(0xFF8E24AA)           // Purple
        "physics" -> Color(0xFF00ACC1)      // Cyan
        "chemistry" -> Color(0xFFD81B60)    // Pink
        "mathematics" -> Color(0xFF3949AB)  // Indigo
        "ai" -> Color(0xFF7C4DFF)           // Violet
        "ml" -> Color(0xFF00897B)           // Emerald / Teal-Green
        // Maps for other known subjects to have standard/consistent colors
        "dl" -> Color(0xFF00C853)           // Bright Green
        "java" -> Color(0xFFE65100)         // Dark Orange
        "python" -> Color(0xFF0277BD)       // Light Blue
        "c programming", "c++" -> Color(0xFF0097A7) // Cyan/Teal
        "web development" -> Color(0xFF009688) // Teal
        "android development" -> Color(0xFF558B2F) // Light Green
        "cloud computing" -> Color(0xFF1565C0) // Dark Blue
        "cyber security" -> Color(0xFFC62828) // Red
        "STW" -> Color(0xFFFFF334)
        else -> Color(0xFF5EB5DC)           //  (Fallback)
    }
}

/**
 * Returns standardized display name for predefined subjects, or capitalized original for others.
 */
fun getSubjectDisplayName(originalSubject: String, normalizedSubject: String): String {
    return when (normalizedSubject) {
        "ds" -> " DS"
        "dbms" -> " DBMS"
        "os" -> " OS"
        "cn" -> " CN"
        "afl" -> " AFL"
        "coa" -> " COA"
        "oop" -> " OOPJ"
        "daa" -> " DAA"
        "se" -> " Software Engineering"
        "ai" -> " AI"
        "ml" -> " ML"
        "dl" -> "DL"
        "physics" -> "Physics"
        "chemistry" -> "Chemistry"
        "mathematics" -> "Maths"
        "statistics" -> "Statistics"
        "evs" -> " EVS"
        "scls" -> " SCLS"
        "java" -> " Java"
        "python" -> "Python"
        "c programming" -> "C Prog"
        "c++" -> " C++"
        "web development" -> "Web Dev"
        "android development" -> "Android Dev"
        "cloud computing" -> "Cloud Computing"
        "cyber security" -> "Cyber Security"
        "STW" -> "STW"
        else -> originalSubject
    }
}

/**
 * A pill-styled Composable displaying the resource subject.
 * Tapping it reveals a popup with the full name of the subject.
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

@Composable
fun SubjectBadge(
    subject: String,
    modifier: Modifier = Modifier,
    isLarge: Boolean = false,
    semester: String? = null
) {
    if (subject.isBlank()) return

    val normalized = remember(subject) { normalizeSubject(subject) }
    val color = remember(normalized) { getSubjectColor(normalized) }
    val displayName = remember(subject, normalized) { getSubjectDisplayName(subject, normalized) }
    val displayWithSem = remember(displayName, semester) {
        if (!semester.isNullOrBlank()) {
            "$displayName  |  ${formatSemesterForSubject(semester)}"
        } else {
            displayName
        }
    }

    var showTooltip by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = color.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
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
                        text = subject,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

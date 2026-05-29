package com.pravor.notessharing.ui.components.trending_components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DocumentTypeBadge(type: String) {
    val (backgroundColor, textColor) = when (type.lowercase(java.util.Locale.ROOT).replace(" ", "")) {
        "notes" -> Pair(Color(0xFFE3F2FD), Color(0xFF1565C0))
        "assignment" -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        "pyq" -> Pair(Color(0xFFFCE4EC), Color(0xFFC2185B))
        "cheatsheet" -> Pair(Color(0xFFFFF8E1), Color(0xFFF57F17))
        else -> Pair(Color(0xFFF3E5F5), Color(0xFF7B1FA2))
    }

    val isDark = !MaterialTheme.colorScheme.primary.toArgb().equals(Color(0xFF1A67B3).toArgb())

    val bg = if (isDark) {
        when (type.lowercase(java.util.Locale.ROOT).replace(" ", "")) {
            "notes" -> Color(0xFF173A5F)
            "assignment" -> Color(0xFF16392F)
            "pyq" -> Color(0xFF51241F)
            "cheatsheet" -> Color(0xFF4A3B18)
            else -> Color(0xFF381E4C)
        }
    } else {
        backgroundColor
    }

    val textCol = if (isDark) {
        when (type.lowercase(java.util.Locale.ROOT).replace(" ", "")) {
            "notes" -> Color(0xFFE4F1FF)
            "assignment" -> Color(0xFFDEFFF0)
            "pyq" -> Color(0xFFFFE7E3)
            "cheatsheet" -> Color(0xFFFFE082)
            else -> Color(0xFFF5E4FF)
        }
    } else {
        textColor
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = type.uppercase(java.util.Locale.ROOT),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textCol,
            fontWeight = FontWeight.Bold
        )
    }
}

fun getContributorColor(level: String?): Color {
    val lvl = level ?: "Bronze Contributor"
    return when (lvl.trim()) {
        "Gold Contributor", "Gold" -> Color(0xFFFFD700)
        "Silver Contributor", "Silver" -> Color(0xFFC0C0C0)
        "Bronze Contributor", "Bronze" -> Color(0xFFCD7F32)
        "Platinum Contributor", "Platinum" -> Color(0xFF00E5FF)
        else -> Color(0xFFCD7F32)
    }
}

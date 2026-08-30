package com.pravor.notessharing.ui.common.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pravor.notessharing.ui.common.utils.getSubjectColor
import com.pravor.notessharing.ui.common.utils.normalizeSubject

@Composable
fun SubjectFilterRow(
    subjects: List<String>,
    selectedSubject: String,
    onSelectSubject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "All" Chip
        val isAllSelected = selectedSubject.isBlank() || selectedSubject.equals("All", ignoreCase = true)
        val allBgColor by animateColorAsState(
            targetValue = if (isAllSelected) Color(0xFF38BDF8).copy(alpha = 0.20f) else Color(0xFF1E293B).copy(alpha = 0.6f),
            label = "all_bg"
        )
        val allBorderColor by animateColorAsState(
            targetValue = if (isAllSelected) Color(0xFF38BDF8) else Color(0xFF334155).copy(alpha = 0.5f),
            label = "all_border"
        )
        val allTextColor by animateColorAsState(
            targetValue = if (isAllSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
            label = "all_text"
        )

        Surface(
            onClick = { onSelectSubject("") },
            shape = RoundedCornerShape(12.dp),
            color = allBgColor,
            border = BorderStroke(1.dp, allBorderColor),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Text(
                text = "All",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = allTextColor,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
            )
        }

        // Subject Badge Chips
        subjects.forEach { subject ->
            if (subject.isNotBlank()) {
                val normalized = remember(subject) { normalizeSubject(subject) }
                val accentColor = remember(normalized) { getSubjectColor(normalized) }
                val isSelected = selectedSubject.equals(subject, ignoreCase = true) ||
                        (selectedSubject.isNotBlank() && normalizeSubject(selectedSubject) == normalized)

                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) accentColor.copy(alpha = 0.22f) else Color(0xFF1E293B).copy(alpha = 0.6f),
                    label = "subj_bg_${subject}"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) accentColor else Color(0xFF334155).copy(alpha = 0.5f),
                    label = "subj_border_${subject}"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) accentColor else Color(0xFFCBD5E1),
                    label = "subj_text_${subject}"
                )

                Surface(
                    onClick = {
                        if (isSelected) {
                            onSelectSubject("")
                        } else {
                            onSelectSubject(subject)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = bgColor,
                    border = BorderStroke(1.dp, borderColor),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Text(
                        text = subject.trim().uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}

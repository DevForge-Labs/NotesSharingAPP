package com.pravor.notessharing.ui.features.classroom.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pravor.notessharing.ui.theme.ElectricBlue

@Composable
fun ClassroomClassesHeader(
    hasHiddenCourses: Boolean,
    showFilterButton: Boolean,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "My Classes",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        if (showFilterButton) {
            Surface(
                shape = CircleShape,
                color = if (hasHiddenCourses) ElectricBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = if (hasHiddenCourses) BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.35f)) else null,
                modifier = Modifier.size(36.dp)
            ) {
                IconButton(
                    onClick = onFilterClick,
                    modifier = Modifier.fillMaxSize()
                ) {
                    BadgedBox(
                        badge = {
                            if (hasHiddenCourses) {
                                Badge(
                                    containerColor = ElectricBlue,
                                    modifier = Modifier.size(6.dp)
                                ) {}
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Manage class visibility",
                            tint = if (hasHiddenCourses) ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

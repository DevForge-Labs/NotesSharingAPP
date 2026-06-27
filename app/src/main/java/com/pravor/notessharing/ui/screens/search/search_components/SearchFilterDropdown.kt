package com.pravor.notessharing.ui.screens.search.search_components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.FeaturedPlayList
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pravor.notessharing.ui.screens.search.FilterOption

@Composable
fun SearchFilterDropdown(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    selectedFilters: Set<FilterOption>,
    onFilterOptionClick: (FilterOption) -> Unit,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = Modifier
                .width(230.dp)
                .background(
                    color = Color(0xFF141922),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFF58D6D1).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .background(Color(0xFF141922))
                    .padding(vertical = 8.dp, horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterOption.values().forEach { option ->
                    val isSelected = selectedFilters.contains(option)
                    val (accentColor, icon) = when (option) {
                        FilterOption.NOTES -> Pair(Color(0xFF58D6D1), Icons.Default.Description)
                        FilterOption.ASSIGNMENTS -> Pair(Color(0xFF7AD7FF), Icons.AutoMirrored.Filled.Assignment)
                        FilterOption.VIDEOS -> Pair(Color(0xFFFF6B6B), Icons.Default.PlayArrow)
                        FilterOption.CHEAT_SHEETS -> Pair(Color(0xFFC7A6FF), Icons.Default.Bolt)
                        FilterOption.PYQS -> Pair(Color(0xFFFFB45C), Icons.AutoMirrored.Filled.Help)
                        FilterOption.PLAYLISTS -> Pair(Color(0xFFFF6B6B), Icons.AutoMirrored.Filled.FeaturedPlayList)
                    }

                    Surface(
                        onClick = { onFilterOptionClick(option) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) accentColor.copy(alpha = 0.12f) else Color(0xFF1E2836).copy(alpha = 0.3f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) accentColor.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.05f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = accentColor.copy(alpha = 0.1f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(28.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = option.displayName,
                                        tint = accentColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Text(
                                text = option.displayName,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                modifier = Modifier.weight(1f)
                            )

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = Color.White.copy(alpha = 0.08f)
                )

                Surface(
                    onClick = {
                        onResetFilters()
                        onDismissRequest()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {
                        Text(
                            text = "Reset Filters",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

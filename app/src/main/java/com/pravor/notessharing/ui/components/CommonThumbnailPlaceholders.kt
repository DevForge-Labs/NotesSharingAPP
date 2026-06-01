package com.pravor.notessharing.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pravor.notessharing.model.FileType

@Composable
fun NotesPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF2980B9), Color(0xFF1F3C56))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Rule paper sheet
            Box(
                modifier = Modifier
                    .size(width = 58.dp, height = 76.dp)
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Top header line
                    Box(Modifier.width(28.dp).height(3.dp).background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(1.5.dp)))
                    // Ruled lines
                    repeat(4) {
                         Box(Modifier.fillMaxWidth().height(1.5.dp).background(Color.White.copy(alpha = 0.2f)))
                    }
                }
            }
        }
    }
}

@Composable
fun AssignmentPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFD35400), Color(0xFF8E3E0F))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Clipboard shape
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 72.dp)
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
            ) {
                // Top clipboard notch
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(6.dp)
                        .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                        .align(Alignment.TopCenter)
                )
                
                // Checklists
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp, start = 8.dp, end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Checkbox with check tick
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(1.5.dp))
                                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(1.5.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(Color.White, CircleShape)
                                )
                            }
                            // Line simulating item description
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(2.dp)
                                    .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(1.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PyqPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF16A085), Color(0xFF0F5A4B))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "PYQ",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                ),
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Draw 2 horizontal lines simulating questions
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.width(50.dp).height(2.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(1.dp)))
                Box(Modifier.width(36.dp).height(2.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(1.dp)))
            }
        }
    }
}

@Composable
fun CheatSheetPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF8E44AD), Color(0xFF5D237A))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 60.dp, height = 75.dp)
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = "</>",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Box(Modifier.width(32.dp).height(2.dp).background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(1.dp)))
                        Box(Modifier.width(24.dp).height(2.dp).background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(1.dp)))
                        Box(Modifier.width(28.dp).height(2.dp).background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(1.dp)))
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF34495E), Color(0xFF1A252F))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Left filmstrip
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(8) {
                Box(
                    modifier = Modifier
                        .size(width = 6.dp, height = 8.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                )
            }
        }
        // Right filmstrip
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(8) {
                Box(
                    modifier = Modifier
                        .size(width = 6.dp, height = 8.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                )
            }
        }
        // Center Play Button
        Surface(
            shape = CircleShape,
            color = Color.White,
            modifier = Modifier.size(54.dp),
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF1A252F),
                    modifier = Modifier
                        .size(32.dp)
                        .padding(start = 2.dp)
                )
            }
        }
    }
}

@Composable
fun PlaylistPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF34495E), Color(0xFF1A252F))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Left filmstrip
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(8) {
                Box(
                    modifier = Modifier
                        .size(width = 6.dp, height = 8.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                )
            }
        }
        // Right filmstrip
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(8) {
                Box(
                    modifier = Modifier
                        .size(width = 6.dp, height = 8.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                )
            }
        }
        // Center Playlist Button
        Surface(
            shape = CircleShape,
            color = Color.White,
            modifier = Modifier.size(54.dp),
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(Modifier.width(20.dp).height(3.dp).background(Color(0xFF1A252F), RoundedCornerShape(1.5.dp)))
                    Spacer(Modifier.height(3.dp))
                    Box(Modifier.width(20.dp).height(3.dp).background(Color(0xFF1A252F), RoundedCornerShape(1.5.dp)))
                    Spacer(Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(Modifier.width(10.dp).height(3.dp).background(Color(0xFF1A252F), RoundedCornerShape(1.5.dp)))
                        Spacer(Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF1A252F),
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentPlaceholder(
    documentType: String,
    modifier: Modifier = Modifier
) {
    val normalized = documentType.lowercase(java.util.Locale.ROOT).replace(" ", "").replace("_", "")
    when {
        normalized.contains("notes") -> NotesPlaceholder(modifier)
        normalized.contains("assignment") -> AssignmentPlaceholder(modifier)
        normalized.contains("pyq") -> PyqPlaceholder(modifier)
        normalized.contains("cheatsheet") -> CheatSheetPlaceholder(modifier)
        normalized.contains("playlist") -> PlaylistPlaceholder(modifier)
        normalized.contains("video") || normalized.contains("youtube") -> VideoPlaceholder(modifier)
        else -> NotesPlaceholder(modifier) // Fallback default
    }
}

@Composable
fun DocumentPlaceholder(
    fileType: FileType,
    modifier: Modifier = Modifier
) {
    when (fileType) {
        FileType.Notes -> NotesPlaceholder(modifier)
        FileType.Pyq -> PyqPlaceholder(modifier)
        FileType.CheatSheet -> CheatSheetPlaceholder(modifier)
        FileType.Video -> VideoPlaceholder(modifier)
        FileType.Pdf -> NotesPlaceholder(modifier) // standard pdf mappings to Notes styling
        else -> NotesPlaceholder(modifier)
    }
}

package com.pravor.notessharing.ui.components.explore_components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pravor.notessharing.model.TrendingNote

@Composable
fun TrendingNoteCard(note: TrendingNote, onClick: () -> Unit = {}) {
    PressScaleSurface(
        modifier = Modifier.width(216.dp),
        shape = RoundedCornerShape(26.dp),
        onClick = onClick
    ) {
        Column(
            Modifier
                .padding(14.dp)
        ) {
            val docType = getDocumentTypeFromTitle(note.title)
            val previewIcon = when (docType) {
                "PYQ" -> Icons.Default.Help
                "Assignment" -> Icons.Default.Assignment
                "Cheat Sheet" -> Icons.Default.Bolt
                else -> Icons.Default.Description
            }
            val accentColor = when (docType) {
                "PYQ" -> Color(0xFFFFA4A2) // Softer red
                "Assignment" -> Color(0xFFA5D6A7) // Softer green
                "Cheat Sheet" -> Color(0xFFFFE082) // Softer amber
                else -> Color(0xFF90CAF9) // Softer blue
            }
            val previewGradient = when (docType) {
                "PYQ" -> listOf(Color(0xFF381F1F), Color(0xFF251414))
                "Assignment" -> listOf(Color(0xFF1C2E20), Color(0xFF132016))
                "Cheat Sheet" -> listOf(Color(0xFF322A1E), Color(0xFF221C14))
                else -> listOf(Color(0xFF202A38), Color(0xFF151C26))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp)
                    .background(
                        Brush.linearGradient(previewGradient),
                        RoundedCornerShape(20.dp)
                    )
                    .border(
                        BorderStroke(1.dp, accentColor.copy(alpha = 0.15f)),
                        RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    // Document page graphic
                    Box(
                        modifier = Modifier.size(width = 54.dp, height = 72.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val foldSize = 10.dp.toPx()
                            val cornerRadius = 6.dp.toPx()

                            val path = Path().apply {
                                moveTo(0f, cornerRadius)
                                quadraticTo(0f, 0f, cornerRadius, 0f)
                                lineTo(w - foldSize, 0f)
                                lineTo(w, foldSize)
                                lineTo(w, h - cornerRadius)
                                quadraticTo(w, h, w - cornerRadius, h)
                                lineTo(cornerRadius, h)
                                quadraticTo(0f, h, 0f, h - cornerRadius)
                                close()
                            }

                             drawPath(
                                path = path,
                                color = Color(0xFF2E3544).copy(alpha = 0.88f)
                            )

                            val foldPath = Path().apply {
                                moveTo(w - foldSize, 0f)
                                lineTo(w - foldSize, foldSize - 1.5.dp.toPx())
                                quadraticTo(w - foldSize, foldSize, w - foldSize + 1.5.dp.toPx(), foldSize)
                                lineTo(w, foldSize)
                                close()
                            }
                            drawPath(
                                path = foldPath,
                                color = Color(0xFF414B60).copy(alpha = 0.95f)
                            )

                            drawPath(
                                path = path,
                                color = Color.White.copy(alpha = 0.12f),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 6.dp, end = 6.dp)
                        ) {
                            Icon(
                                imageVector = previewIcon,
                                contentDescription = null,
                                tint = accentColor.copy(alpha = 0.85f),
                                modifier = Modifier.size(22.dp)
                            )
                            
                            Spacer(Modifier.height(8.dp))
                            
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(30.dp)
                                        .height(2.dp)
                                        .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(1.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height(2.dp)
                                        .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(1.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .width(26.dp)
                                        .height(2.dp)
                                        .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(1.dp))
                                )
                            }
                        }
                    }
                }
                
                // Overlay the chip badge at the bottom center of the document preview box
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 6.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = docType.uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(40.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = note.subject,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SmallMetric(Icons.Default.Download, note.downloads.toString())
                Spacer(Modifier.width(10.dp))
                SmallMetric(Icons.Default.ThumbUp, note.upvotes.toString())
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = if (note.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(note.upvotes.toString())
            }
        }
    }
}

private fun getDocumentTypeFromTitle(title: String): String {
    val t = title.lowercase(java.util.Locale.ROOT)
    return when {
        t.contains("pyq") || t.contains("solved") || t.contains("exam") || t.contains("paper") -> "PYQ"
        t.contains("cheat") || t.contains("formula") || t.contains("quick") || t.contains("sheet") -> "Cheat Sheet"
        t.contains("lab") || t.contains("assignment") || t.contains("manual") || t.contains("practice") -> "Assignment"
        else -> "Notes"
    }
}

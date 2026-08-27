package com.pravor.notessharing.ui.features.classroom.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pravor.notessharing.domain.model.classroom.ClassroomCourse

@Composable
fun ClassroomCourseCard(
    course: ClassroomCourse,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0
) {
    val theme = remember(course.id, course.name, index) {
        getCourseTheme(course, index)
    }

    // Extremely lightweight, GPU-only ambient drift phase
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_shards")
    val ambientPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambientPhase"
    )

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.18f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        colors = theme.gradientColors,
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .drawBehind {
                    drawRandomizedCourseShapes(course.id, course.name, theme.accentColor, ambientPhase)
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                // 1. Top Section: [ Classroom Icon ]  [ Subject Name ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Glass Icon Box with Classroom School Icon & Glow
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = theme.accentColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.35f)),
                        modifier = Modifier.size(50.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = theme.accentColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Course Title (Sits beside the icon, wraps naturally and responsively)
                    Text(
                        text = course.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 0.2.sp
                        ),
                        color = Color.White,
                        lineHeight = 22.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Generous vertical breathing space
                Spacer(Modifier.height(14.dp))

                // 2. Bottom Section: [ Teacher / Section Info (Left) ]  [ Navigation Arrow (Bottom-Right) ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val teacher = course.teacher
                    val metadataParts = listOfNotNull(
                        course.section?.takeIf { it.isNotBlank() },
                        course.room?.takeIf { it.isNotBlank() }?.let { "Room $it" }
                    )

                    // Left Column: Teacher and/or Section Metadata
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (teacher != null && teacher.name.isNotBlank()) {
                            TeacherRow(teacher = teacher, accentColor = theme.accentColor)
                        }

                        if (metadataParts.isNotEmpty()) {
                            Text(
                                text = metadataParts.joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                color = Color.White.copy(alpha = 0.60f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else if (teacher == null || teacher.name.isBlank()) {
                            // Subtle fallback label when no teacher or section exists
                            Text(
                                text = "Active Class",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                color = Color.White.copy(alpha = 0.50f)
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    // Bottom-Right Circular Navigation Arrow (Dynamically matches card's accent theme)
                    Surface(
                        shape = CircleShape,
                        color = theme.accentColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.32f)),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Open course details",
                                tint = theme.accentColor.copy(alpha = 0.90f),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

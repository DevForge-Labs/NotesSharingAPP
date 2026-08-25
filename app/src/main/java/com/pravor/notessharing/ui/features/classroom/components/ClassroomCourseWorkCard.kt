package com.pravor.notessharing.ui.features.classroom.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pravor.notessharing.domain.model.classroom.ClassroomAttachment
import com.pravor.notessharing.domain.model.classroom.ClassroomCourseWork
import com.pravor.notessharing.domain.model.classroom.SubmissionState
import com.pravor.notessharing.ui.theme.ElectricBlue
import com.pravor.notessharing.ui.theme.Mint

@Composable
fun ClassroomCourseWorkCard(
    courseWork: ClassroomCourseWork,
    onAttachmentClick: (ClassroomAttachment) -> Unit,
    submissionState: SubmissionState? = null,
    onSubmitClick: ((ClassroomCourseWork) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isSubmitted = submissionState == SubmissionState.TURNED_IN ||
            submissionState == SubmissionState.RETURNED

    val formattedDueDate = formatDueDateTime(courseWork.dueFormatted)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF141920), Color(0xFF0E1217))
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header: Icon + Title & Due Date Hierarchy
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFB45C).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFFFFB45C).copy(alpha = 0.3f)),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = null,
                            tint = Color(0xFFFFB45C),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Primary Heading: Title (natural wrapping)
                    Text(
                        text = courseWork.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )

                    // Secondary Subordinate: Due date directly below title
                    if (!formattedDueDate.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                tint = if (isSubmitted) MaterialTheme.colorScheme.onSurfaceVariant else Mint,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = formattedDueDate,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSubmitted) MaterialTheme.colorScheme.onSurfaceVariant else Mint,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 2. Description
            if (!courseWork.description.isNullOrBlank()) {
                ClassroomFormattedText(
                    rawText = courseWork.description
                )
            }

            // 3. Attachments
            if (courseWork.attachments.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    courseWork.attachments.forEach { attachment ->
                        ClassroomAttachmentRow(
                            attachment = attachment,
                            onClick = { onAttachmentClick(attachment) }
                        )
                    }
                }
            }

            // 4. Action Area: Real Submission Status
            if (onSubmitClick != null) {
                Spacer(Modifier.height(2.dp))
                if (isSubmitted) {
                    // --- Already Submitted State: Subtle Green-Tinted Action Container ---
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Mint.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Mint.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable { onSubmitClick(courseWork) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Mint,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "View Submission",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Mint
                            )
                        }
                    }
                } else {
                    // --- Not Submitted State: Clear Primary "Submit Work" Action ---
                    OutlinedButton(
                        onClick = { onSubmitClick(courseWork) },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ElectricBlue
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Submit Work",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Formats a raw due date string into "Due MMM d, yyyy · h:mm a" cleanly.
 */
private fun formatDueDateTime(rawDue: String?): String? {
    if (rawDue.isNullOrBlank()) return null
    return try {
        val clean = rawDue.removePrefix("Due ").trim()
        val parts = clean.split(",")
        val datePart = parts[0].trim()
        val timePart = if (parts.size > 1) parts[1].trim() else null

        val dmy = datePart.split("/")
        if (dmy.size == 3) {
            val day = dmy[0].toIntOrNull()
            val month = dmy[1].toIntOrNull()
            val year = dmy[2].toIntOrNull()
            if (day != null && month != null && year != null) {
                val months = arrayOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                val monthStr = if (month in 1..12) months[month] else month.toString()
                val formattedDate = "$monthStr $day, $year"

                val formattedTime = if (timePart != null) {
                    val hm = timePart.split(":")
                    if (hm.size >= 2) {
                        val hours = hm[0].toIntOrNull() ?: 0
                        val mins = hm[1].toIntOrNull() ?: 0
                        val amPm = if (hours >= 12) "PM" else "AM"
                        val h12 = if (hours % 12 == 0) 12 else if (hours > 12) hours - 12 else hours
                        String.format("%d:%02d %s", h12, mins, amPm)
                    } else timePart
                } else null

                if (formattedTime != null) {
                    "Due $formattedDate · $formattedTime"
                } else {
                    "Due $formattedDate"
                }
            } else {
                if (clean.startsWith("Due", ignoreCase = true)) clean else "Due $clean"
            }
        } else {
            if (clean.startsWith("Due", ignoreCase = true)) clean else "Due $clean"
        }
    } catch (e: Exception) {
        if (rawDue.startsWith("Due", ignoreCase = true)) rawDue else "Due $rawDue"
    }
}

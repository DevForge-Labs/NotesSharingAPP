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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.pravor.notessharing.domain.model.classroom.AttachmentType
import com.pravor.notessharing.domain.model.classroom.ClassroomAttachment
import com.pravor.notessharing.domain.model.classroom.ClassroomCourseWork
import com.pravor.notessharing.domain.model.classroom.SubmissionState
import com.pravor.notessharing.domain.model.classroom.hasActionableExternalTask
import com.pravor.notessharing.domain.model.classroom.ClassroomDateUtils
import com.pravor.notessharing.domain.model.classroom.primaryExternalLinkAttachment
import com.pravor.notessharing.domain.model.classroom.primaryFormAttachment
import com.pravor.notessharing.ui.theme.ElectricBlue
import com.pravor.notessharing.ui.theme.Mint

@Composable
fun ClassroomCourseWorkCard(
    courseWork: ClassroomCourseWork,
    onAttachmentClick: (ClassroomAttachment) -> Unit,
    submissionState: SubmissionState? = null,
    isLocallyDone: Boolean = false,
    isMarkingDone: Boolean = false,
    courseName: String? = null,
    onSubmitClick: ((ClassroomCourseWork) -> Unit)? = null,
    onOpenExternalTaskClick: ((ClassroomCourseWork, String) -> Unit)? = null,
    onMarkAsDoneClick: ((ClassroomCourseWork) -> Unit)? = null,
    onDoneStatusClick: ((ClassroomCourseWork) -> Unit)? = null,
    modifier: Modifier = Modifier,
    accentColor: Color = ElectricBlue
) {
    val isRealSubmitted = submissionState == SubmissionState.TURNED_IN ||
            submissionState == SubmissionState.RETURNED

    val formattedDueDate = ClassroomDateUtils.formatDueDateTime(courseWork.dueFormatted)

    val formAttachment = courseWork.primaryFormAttachment
    val externalLinkAttachment = courseWork.primaryExternalLinkAttachment
    val isExternalTask = formAttachment != null || (externalLinkAttachment != null && courseWork.attachments.none { it.type == AttachmentType.DRIVE_FILE })
    val externalUrl = formAttachment?.linkUrl ?: externalLinkAttachment?.linkUrl

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.20f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xD9141923), Color(0xD40E1217))
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

                    // Secondary Subordinate: Course Name if provided
                    if (!courseName.isNullOrBlank()) {
                        Text(
                            text = courseName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor,
                            fontSize = 12.sp
                        )
                    }

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
                                tint = if (isRealSubmitted || isLocallyDone) MaterialTheme.colorScheme.onSurfaceVariant else Mint,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = formattedDueDate,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isRealSubmitted || isLocallyDone) MaterialTheme.colorScheme.onSurfaceVariant else Mint,
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

            // 4. Action Area: Real Submission Status vs External Task vs Upload
            Spacer(Modifier.height(2.dp))
            if (isRealSubmitted) {
                // --- Real Google Classroom Submitted State ---
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Mint.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Mint.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { onSubmitClick?.invoke(courseWork) }
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
            } else if (isLocallyDone) {
                // --- Campus Pages Local Done State (Tappable clean status) ---
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Mint.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Mint.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDoneStatusClick?.invoke(courseWork) }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Mint,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Done",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Mint
                            )
                            Text(
                                text = "Completed in Campus Pages",
                                style = MaterialTheme.typography.bodySmall,
                                color = Mint.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            } else if (isExternalTask && externalUrl != null) {
                // --- Google Form / External Link Task Action ---
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val taskButtonLabel = if (formAttachment != null) "Open Google Form" else "Open Link"
                    val taskButtonIcon = if (formAttachment != null) Icons.Default.Description else Icons.Default.Link

                    Button(
                        onClick = { onOpenExternalTaskClick?.invoke(courseWork, externalUrl) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color(0xFF07121E)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(
                            imageVector = taskButtonIcon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = taskButtonLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    OutlinedButton(
                        onClick = { onMarkAsDoneClick?.invoke(courseWork) },
                        enabled = !isMarkingDone,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        if (isMarkingDone) {
                            CircularProgressIndicator(
                                color = accentColor,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Marking as done...",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Mint,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Mark as Done",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else if (onSubmitClick != null) {
                // --- Normal File Submission: "Submit Work" Action ---
                OutlinedButton(
                    onClick = { onSubmitClick(courseWork) },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = accentColor
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

package com.pravor.notessharing.ui.features.classroom.components

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pravor.notessharing.data.classroom.ClassroomSubmissionRepository
import com.pravor.notessharing.data.classroom.SubmitAssignmentResult
import com.pravor.notessharing.domain.model.classroom.AttachmentType
import com.pravor.notessharing.domain.model.classroom.ClassroomAttachment
import com.pravor.notessharing.domain.model.classroom.ClassroomCourseWork
import com.pravor.notessharing.domain.model.classroom.ClassroomStudentSubmission
import com.pravor.notessharing.domain.model.classroom.SelectedSubmissionFile
import com.pravor.notessharing.domain.model.classroom.SubmissionProgress
import com.pravor.notessharing.domain.model.classroom.SubmissionState
import com.pravor.notessharing.ui.theme.ElectricBlue
import com.pravor.notessharing.ui.theme.Mint
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassroomSubmissionBottomSheet(
    courseId: String,
    courseWork: ClassroomCourseWork,
    onDismiss: () -> Unit,
    onAttachmentClick: (ClassroomAttachment) -> Unit,
    onSubmissionSuccess: ((ClassroomStudentSubmission) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val submissionRepo = remember { ClassroomSubmissionRepository.getInstance(context) }

    var submission by remember { mutableStateOf<ClassroomStudentSubmission?>(null) }
    var isLoadingSubmission by remember { mutableStateOf(true) }
    var selectedFile by remember { mutableStateOf<SelectedSubmissionFile?>(null) }
    var progressState by remember { mutableStateOf<SubmissionProgress>(SubmissionProgress.Idle) }

    // Consent Launcher for incremental authorization
    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // User granted consent, retry submission
            val file = selectedFile
            if (file != null) {
                scope.launch {
                    executeSubmission(
                        context = context,
                        submissionRepo = submissionRepo,
                        courseId = courseId,
                        courseWork = courseWork,
                        file = file,
                        onProgress = { progressState = it },
                        onSuccess = { updated ->
                            submission = updated
                            progressState = SubmissionProgress.Success()
                            if (updated != null) {
                                onSubmissionSuccess?.invoke(updated)
                            }
                        },
                        onConsentRequired = {
                            progressState = SubmissionProgress.Error("Permission consent was not completed.")
                        }
                    )
                }
            }
        } else {
            progressState = SubmissionProgress.Error("Classroom submission permission is required to turn in assignments.")
        }
    }

    // File picker for PDFs, docs, images
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileDetails = resolveFileDetails(context, uri)
            selectedFile = fileDetails
            progressState = SubmissionProgress.Idle
        }
    }

    // Initial load of submission status
    LaunchedEffect(courseWork.id) {
        isLoadingSubmission = true
        val result = submissionRepo.getSubmission(courseId, courseWork.id)
        result.onSuccess { sub ->
            submission = sub
        }
        isLoadingSubmission = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF111720),
        tonalElevation = 6.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
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
                    Column {
                        Text(
                            text = courseWork.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!courseWork.dueFormatted.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Event,
                                    contentDescription = null,
                                    tint = Mint,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Due ${courseWork.dueFormatted}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Mint,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2. Submission Status
            if (isLoadingSubmission) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ElectricBlue, modifier = Modifier.size(28.dp))
                }
            } else {
                val isTurnedIn = submission?.state == SubmissionState.TURNED_IN ||
                        submission?.state == SubmissionState.RETURNED

                if (isTurnedIn) {
                    // --- Already Turned In View ---
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Mint.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Mint.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Mint,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = if (submission?.state == SubmissionState.RETURNED) "Graded / Returned" else "Turned In",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Mint
                                )
                                Text(
                                    text = "Your work is submitted on Google Classroom.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Submitted Attachments list
                    if (submission?.attachments?.isNotEmpty() == true) {
                        Text(
                            text = "Submitted Attachments",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            submission?.attachments?.forEach { att ->
                                ClassroomAttachmentRow(
                                    attachment = ClassroomAttachment(
                                        title = att.title,
                                        linkUrl = att.linkUrl,
                                        type = att.type,
                                        thumbnailUrl = att.thumbnailUrl
                                    ),
                                    onClick = {
                                        onAttachmentClick(
                                            ClassroomAttachment(
                                                title = att.title,
                                                linkUrl = att.linkUrl,
                                                type = att.type,
                                                thumbnailUrl = att.thumbnailUrl
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // --- Not Submitted / Upload & Turn In Flow ---
                    Text(
                        text = "Your Work",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // File selection area
                    if (selectedFile == null) {
                        OutlinedButton(
                            onClick = { filePickerLauncher.launch("*/*") },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Attach File (PDF, Doc, Image)",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        // File Selected Chip Card
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = ElectricBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = selectedFile!!.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = formatFileSize(selectedFile!!.sizeBytes),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (progressState is SubmissionProgress.Idle || progressState is SubmissionProgress.Error) {
                                    IconButton(onClick = { selectedFile = null }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove File",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Progress / Status Messages
                    AnimatedVisibility(
                        visible = progressState !is SubmissionProgress.Idle,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        when (val state = progressState) {
                            is SubmissionProgress.UploadingToDrive -> {
                                ProgressStateRow(
                                    message = "Uploading '${state.fileName}' to Google Drive...",
                                    icon = Icons.Default.CloudUpload,
                                    isLoading = true
                                )
                            }
                            is SubmissionProgress.AttachingToClassroom -> {
                                ProgressStateRow(
                                    message = "Attaching to Classroom assignment...",
                                    icon = Icons.AutoMirrored.Filled.Assignment,
                                    isLoading = true
                                )
                            }
                            is SubmissionProgress.TurningIn -> {
                                ProgressStateRow(
                                    message = "Turning in assignment...",
                                    icon = Icons.Default.CheckCircle,
                                    isLoading = true
                                )
                            }
                            is SubmissionProgress.Success -> {
                                ProgressStateRow(
                                    message = state.message,
                                    icon = Icons.Default.CheckCircle,
                                    isLoading = false,
                                    tint = Mint
                                )
                            }
                            is SubmissionProgress.Error -> {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Error,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = state.errorMessage,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            SubmissionProgress.Idle -> {}
                        }
                    }

                    // Submit Action Button
                    val isSubmitting = progressState is SubmissionProgress.UploadingToDrive ||
                            progressState is SubmissionProgress.AttachingToClassroom ||
                            progressState is SubmissionProgress.TurningIn

                    Button(
                        onClick = {
                            val file = selectedFile ?: return@Button
                            scope.launch {
                                executeSubmission(
                                    context = context,
                                    submissionRepo = submissionRepo,
                                    courseId = courseId,
                                    courseWork = courseWork,
                                    file = file,
                                    onProgress = { progressState = it },
                                    onSuccess = { updated ->
                                        submission = updated
                                        progressState = SubmissionProgress.Success()
                                        if (updated != null) {
                                            onSubmissionSuccess?.invoke(updated)
                                        }
                                    },
                                    onConsentRequired = { recoveryIntent ->
                                        consentLauncher.launch(recoveryIntent)
                                    }
                                )
                            }
                        },
                        enabled = selectedFile != null && !isSubmitting,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricBlue,
                            disabledContainerColor = ElectricBlue.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = Color(0xFF07121E),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Submitting...",
                                color = Color(0xFF07121E),
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "Turn In Assignment",
                                color = Color(0xFF07121E),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProgressStateRow(
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isLoading: Boolean,
    tint: Color = ElectricBlue
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = tint,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private suspend fun executeSubmission(
    context: Context,
    submissionRepo: ClassroomSubmissionRepository,
    courseId: String,
    courseWork: ClassroomCourseWork,
    file: SelectedSubmissionFile,
    onProgress: (SubmissionProgress) -> Unit,
    onSuccess: (ClassroomStudentSubmission?) -> Unit,
    onConsentRequired: (android.content.Intent) -> Unit
) {
    val result = submissionRepo.submitAssignment(
        context = context,
        courseId = courseId,
        courseWorkId = courseWork.id,
        fileUri = file.uri,
        fileName = file.name,
        mimeType = file.mimeType,
        onProgress = onProgress
    )

    when (result) {
        is SubmitAssignmentResult.Success -> {
            onSuccess(result.submission)
        }
        is SubmitAssignmentResult.ConsentRequired -> {
            onConsentRequired(result.recoveryIntent)
        }
        is SubmitAssignmentResult.Error -> {
            onProgress(SubmissionProgress.Error(result.message))
        }
    }
}

private fun resolveFileDetails(context: Context, uri: Uri): SelectedSubmissionFile {
    var name = "submission_file"
    var size = 0L
    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0) {
                        name = cursor.getString(nameIdx) ?: name
                    }
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIdx >= 0) {
                        size = cursor.getLong(sizeIdx)
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback
        }
    }
    return SelectedSubmissionFile(uri, name, size, mimeType)
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "Unknown size"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format("%.1f MB", mb)
    } else {
        String.format("%.0f KB", kb)
    }
}

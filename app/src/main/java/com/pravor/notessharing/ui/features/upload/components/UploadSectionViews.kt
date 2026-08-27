package com.pravor.notessharing.ui.features.upload.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.pravor.notessharing.domain.model.SelectedUploadFile
import com.pravor.notessharing.domain.model.UploadType
import com.pravor.notessharing.ui.common.LiquidTransferProgressBar
import com.pravor.notessharing.ui.common.components.SectionHeader
import com.pravor.notessharing.ui.features.upload.YoutubePreview

@Composable
fun EmptyUploadState(metadataComplete: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            val lottieCompositionResult = rememberLottieComposition(
                LottieCompositionSpec.Asset("App_animations/upload_start_metadata.json")
            )
            val lottieComposition = lottieCompositionResult.value
            val lottieProgress by animateLottieCompositionAsState(
                composition = lottieComposition,
                iterations = LottieConstants.IterateForever
            )

            if (lottieComposition != null) {
                LottieAnimation(
                    composition = lottieComposition,
                    progress = { lottieProgress },
                    modifier = Modifier
                        .size(125.dp)
                        .offset(y = (-18).dp)
                        .alpha(0.85f)
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(58.dp))

                Text(
                    text = if (metadataComplete) "Choose a content type" else "Start with metadata",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (metadataComplete) "PDFs, images, and YouTube links cannot be mixed." else "Branch, semester, subject, and document type are required before file selection.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun PyqUploadSection(
    files: List<SelectedUploadFile>,
    onPickPdfs: () -> Unit,
    onRemoveFile: (SelectedUploadFile) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onPickPdfs,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            shape = RoundedCornerShape(18.dp),
            enabled = files.isEmpty()
        ) {
            Icon(Icons.Default.Description, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Select PDF")
        }
        if (files.isEmpty()) {
            EmptyPreviewCard("No PDF selected", "Tap Select PDF to choose the exam paper (PDF only).")
        } else {
            files.forEach { file ->
                PdfPreviewCard(file = file, onRemove = { onRemoveFile(file) })
            }
        }
    }
}

@Composable
fun CombinedUploadSection(
    files: List<SelectedUploadFile>,
    onPickPdfs: () -> Unit,
    onPickImages: () -> Unit,
    onCaptureImage: () -> Unit,
    onRemoveFile: (SelectedUploadFile) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onPickPdfs,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Default.Description, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add Documents")
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onPickImages, modifier = Modifier.weight(1f), shape = RoundedCornerShape(18.dp)) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Gallery")
            }
            Button(onClick = onCaptureImage, modifier = Modifier.weight(1f), shape = RoundedCornerShape(18.dp)) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Camera")
            }
        }
        
        if (files.isEmpty()) {
            EmptyPreviewCard("No files selected", "Tap Add Documents, Gallery or Camera to select documents or images.")
        } else {
            val docFiles = files.filter { 
                it.displayName.endsWith(".pdf", ignoreCase = true) ||
                it.displayName.endsWith(".ppt", ignoreCase = true) ||
                it.displayName.endsWith(".pptx", ignoreCase = true)
            }
            val imageFiles = files.filter { 
                !it.displayName.endsWith(".pdf", ignoreCase = true) &&
                !it.displayName.endsWith(".ppt", ignoreCase = true) &&
                !it.displayName.endsWith(".pptx", ignoreCase = true)
            }
            
            if (docFiles.isNotEmpty()) {
                docFiles.forEach { file ->
                    PdfPreviewCard(file = file, onRemove = { onRemoveFile(file) })
                }
            }
            
            if (imageFiles.isNotEmpty()) {
                ImagePreviewSection(files = imageFiles, onRemoveFile = onRemoveFile)
            }
        }
    }
}

@Composable
fun YoutubeUploadSection(
    youtubeUrl: String,
    youtubeResourceType: String,
    isFetching: Boolean,
    preview: YoutubePreview?,
    error: String?,
    onYoutubeUrlChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = youtubeUrl,
            onValueChange = onYoutubeUrlChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (youtubeResourceType == "playlist") "Paste Playlist URL" else "Paste Video URL") },
            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
        YoutubePreviewCard(preview = preview, isFetching = isFetching, error = error)
    }
}

@Composable
fun LiveUploadStats(
    fileCount: Int,
    totalSizeBytes: Long
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Selected Files: $fileCount",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            val sizeMb = String.format("%.2f MB", totalSizeBytes / (1024f * 1024f))
            Text(
                text = "Total Size: $sizeMb",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun UploadSummaryCard(uiState: com.pravor.notessharing.ui.features.upload.UploadUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader("Upload Preview")

            PreviewRow(
                label = "Branch",
                value = uiState.selectedBranch.ifBlank { "Required" }
            )

            PreviewRow(
                label = "Semester",
                value = uiState.selectedSemester.ifBlank { "Required" }
            )

            val isFirstYear = uiState.selectedSemester == "Semester 1" || uiState.selectedSemester == "Semester 2"
            if (isFirstYear && uiState.selectedGroup.isNotBlank()) {
                PreviewRow(
                    label = "Group",
                    value = uiState.selectedGroup
                )
            }

            PreviewRow(
                label = "Subject",
                value = uiState.subject.ifBlank { "Required" }
            )

            PreviewRow(
                label = "Document Type",
                value = uiState.selectedType?.label ?: "Not selected"
            )

            if (uiState.selectedType == UploadType.Pyq) {
                if (uiState.selectedExamYear.isNotBlank()) {
                    PreviewRow(label = "Exam Year", value = uiState.selectedExamYear)
                }
                if (uiState.selectedExamType.isNotBlank()) {
                    PreviewRow(label = "Exam Type", value = uiState.selectedExamType)
                }
            } else if (uiState.selectedType == UploadType.Assignment) {
                if (uiState.section.isNotBlank()) {
                    PreviewRow(label = "Section", value = uiState.section)
                }
                if (uiState.title.isNotBlank()) {
                    PreviewRow(label = "Title", value = uiState.title)
                }
            } else if (uiState.selectedType == UploadType.Notes || uiState.selectedType == UploadType.CheatSheet) {
                if (uiState.title.isNotBlank()) {
                    PreviewRow(label = "Title", value = uiState.title)
                }
            }

            val fileCountText = if (uiState.selectedType == UploadType.Youtube) {
                if (uiState.youtubeUrl.isNotBlank()) "1 Link" else "0"
            } else {
                uiState.selectedFiles.size.toString()
            }
            PreviewRow(
                label = "Files",
                value = fileCountText
            )

            val totalSizeText = if (uiState.selectedType == UploadType.Youtube) {
                "—"
            } else {
                formatBytes(uiState.totalSizeBytes)
            }
            PreviewRow(
                label = "Total Size",
                value = totalSizeText
            )
        }
    }
}

@Composable
private fun PreviewRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun StatusMessages(uiState: com.pravor.notessharing.ui.features.upload.UploadUiState) {
    if (uiState.errorMessage != null) {
        Text(
            text = uiState.errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun UploadButton(
    isSaving: Boolean,
    enabled: Boolean,
    progress: Float,
    onUpload: () -> Unit
) {
    if (isSaving) {
        val progressInt = (progress * 100).toInt()
        LiquidTransferProgressBar(
            progress = progress,
            statusText = "$progressInt%",
            showSpinner = false,
            showCheckmarkOnComplete = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        )
    } else {
        val primaryColor = MaterialTheme.colorScheme.primary
        val onPrimary = MaterialTheme.colorScheme.onPrimary
        val disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        val disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (enabled) primaryColor else disabledColor)
                .clickable(
                    enabled = enabled,
                    onClick = onUpload
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = null,
                    tint = if (enabled) onPrimary else disabledContentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Upload Document",
                    color = if (enabled) onPrimary else disabledContentColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}



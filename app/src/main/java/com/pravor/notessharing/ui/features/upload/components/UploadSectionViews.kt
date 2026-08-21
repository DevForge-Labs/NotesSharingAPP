package com.pravor.notessharing.ui.features.upload.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.pravor.notessharing.domain.model.SelectedUploadFile
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
            Text("Add PDFs")
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
            EmptyPreviewCard("No files selected", "Tap Add PDFs, Gallery or Camera to select documents or images.")
        } else {
            val pdfFiles = files.filter { it.displayName.endsWith(".pdf", ignoreCase = true) }
            val imageFiles = files.filter { !it.displayName.endsWith(".pdf", ignoreCase = true) }
            
            if (pdfFiles.isNotEmpty()) {
                pdfFiles.forEach { file ->
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
    if (!uiState.metadataComplete) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Summary",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${uiState.selectedBranch} • ${uiState.selectedSemester}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Section ${uiState.section} • ${uiState.subject}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
    Button(
        onClick = onUpload,
        enabled = enabled && !isSaving,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        if (isSaving) {
            androidx.compose.material3.CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            val progressInt = (progress * 100).toInt()
            Text(if (progressInt > 0) "Uploading... $progressInt%" else "Processing...")
        } else {
            Text("Upload Now", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}



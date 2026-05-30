 package com.pravor.notessharing.ui.screens.upload

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.pravor.notessharing.model.SelectedUploadFile
import com.pravor.notessharing.model.UploadFileSource
import com.pravor.notessharing.model.UploadType
import com.pravor.notessharing.state.UploadUiState
import com.pravor.notessharing.state.YoutubePreview
import com.pravor.notessharing.ui.components.AdaptiveScrollbar
import com.pravor.notessharing.ui.components.SectionHeader
import com.pravor.notessharing.ui.components.LiquidTransferProgressBar
import com.pravor.notessharing.viewmodel.UploadViewModel

@Composable
fun UploadRoute(
    onUploadSuccess: () -> Unit,
    viewModel: UploadViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.uploadSuccess) {
        if (uiState.uploadSuccess) {
            focusManager.clearFocus()
            onUploadSuccess()
            viewModel.clearUploadSuccess()
        }
    }

    val selectedType = uiState.selectedType ?: UploadType.Notes
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.addPickedUris(uris, selectedType, UploadFileSource.DocumentPicker)
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.addPickedUris(uris, selectedType, UploadFileSource.Gallery)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        viewModel.onCameraCaptureResult(success)
    }

    UploadScreen(
        uiState = uiState,
        onBranchChange = viewModel::selectBranch,
        onSemesterChange = viewModel::selectSemester,
        onSubjectChange = viewModel::updateSubject,
        onTitleChange = viewModel::updateTitle,
        onDescriptionChange = viewModel::updateDescription,
        onSectionChange = viewModel::updateSection,
        onYoutubeResourceTypeChange = viewModel::selectYoutubeResourceType,
        onTypeSelected = viewModel::selectUploadType,
        onExamYearChange = viewModel::selectExamYear,
        onExamTypeChange = viewModel::selectExamType,
        onPickPdfs = { pdfPicker.launch(arrayOf("application/pdf")) },
        onPickImages = { imagePicker.launch(arrayOf("image/*")) },
        onCaptureImage = { cameraLauncher.launch(viewModel.createCameraUri()) },
        onYoutubeUrlChange = viewModel::updateYoutubeUrl,
        onRemoveFile = viewModel::removeFile,
        onUpload = viewModel::upload
    )
}

@Composable
fun UploadScreen(
    uiState: UploadUiState,
    onBranchChange: (String) -> Unit,
    onSemesterChange: (String) -> Unit,
    onSubjectChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSectionChange: (String) -> Unit,
    onYoutubeResourceTypeChange: (String) -> Unit,
    onTypeSelected: (UploadType) -> Unit,
    onExamYearChange: (String) -> Unit,
    onExamTypeChange: (String) -> Unit,
    onPickPdfs: () -> Unit,
    onPickImages: () -> Unit,
    onCaptureImage: () -> Unit,
    onYoutubeUrlChange: (String) -> Unit,
    onRemoveFile: (SelectedUploadFile) -> Unit,
    onUpload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val clearFocusOnScroll = remember(focusManager) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                focusManager.clearFocus()
                return Offset.Zero
            }
        }
    }

    val isFormValid = uiState.metadataComplete && uiState.errorMessage == null && when (uiState.selectedType) {
        UploadType.Pyq -> uiState.selectedFiles.size == 1 && uiState.selectedExamYear.isNotBlank() && uiState.selectedExamType.isNotBlank()
        UploadType.Youtube -> uiState.youtubeUrl.isNotBlank() && (
            if (uiState.youtubeResourceType == "playlist") {
                com.pravor.notessharing.model.extractYoutubePlaylistId(uiState.youtubeUrl) != null
            } else {
                com.pravor.notessharing.model.extractYoutubeVideoId(uiState.youtubeUrl) != null
            }
        )
        UploadType.Notes, UploadType.CheatSheet -> uiState.selectedFiles.isNotEmpty() && uiState.title.isNotBlank()
        UploadType.Assignment -> uiState.selectedFiles.isNotEmpty() && uiState.title.isNotBlank() && uiState.section.isNotBlank()
        null -> false
    }

    Box(
        modifier
            .fillMaxSize()
            .clearFocusOnOutsideTap { focusManager.clearFocus() }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .nestedScroll(clearFocusOnScroll),
            state = listState,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "upload-header", contentType = "header") {
                UploadHeader()
            }
            item(key = "metadata", contentType = "metadata") {
                MetadataSection(
                    uiState = uiState,
                    onBranchChange = onBranchChange,
                    onSemesterChange = onSemesterChange,
                    onSubjectChange = onSubjectChange,
                    onTitleChange = onTitleChange,
                    onDescriptionChange = onDescriptionChange,
                    onSectionChange = onSectionChange,
                    onYoutubeResourceTypeChange = onYoutubeResourceTypeChange,
                    onTypeSelected = onTypeSelected,
                    onExamYearChange = onExamYearChange,
                    onExamTypeChange = onExamTypeChange
                )
            }
            item(key = "content-picker", contentType = "content-picker") {
                Crossfade(targetState = uiState.selectedType, label = "upload-type-content") { type ->
                    when (type) {
                        UploadType.Pyq -> PyqUploadSection(uiState.selectedFiles, onPickPdfs, onRemoveFile)
                        UploadType.Notes, UploadType.CheatSheet, UploadType.Assignment -> CombinedUploadSection(
                            files = uiState.selectedFiles,
                            onPickPdfs = onPickPdfs,
                            onPickImages = onPickImages,
                            onCaptureImage = onCaptureImage,
                            onRemoveFile = onRemoveFile
                        )
                        UploadType.Youtube -> YoutubeUploadSection(
                            youtubeUrl = uiState.youtubeUrl,
                            youtubeResourceType = uiState.youtubeResourceType,
                            isFetching = uiState.isFetchingYoutube,
                            preview = uiState.youtubePreview,
                            error = uiState.youtubeError,
                            onYoutubeUrlChange = onYoutubeUrlChange
                        )
                        null -> EmptyUploadState(uiState.metadataComplete)
                    }
                }
            }
            if (uiState.selectedType != UploadType.Youtube && uiState.selectedType != null) {
                item(key = "live-stats", contentType = "stats") {
                    LiveUploadStats(
                        fileCount = uiState.selectedFiles.size,
                        totalSizeBytes = uiState.totalSizeBytes
                    )
                }
            }
            item(key = "summary", contentType = "summary") {
                UploadSummaryCard(uiState)
            }
            item(key = "error-success", contentType = "status") {
                StatusMessages(uiState)
            }
            item(key = "upload-button", contentType = "button") {
                UploadButton(
                    isSaving = uiState.isSaving,
                    enabled = isFormValid,
                    progress = uiState.uploadProgress,
                    onUpload = onUpload
                )
            }
        }
        AdaptiveScrollbar(listState = listState)
    }
}

private fun Modifier.clearFocusOnOutsideTap(onClearFocus: () -> Unit): Modifier =
    pointerInput(onClearFocus) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            onClearFocus()
        }
    }

@Composable
private fun UploadHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Upload",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Share notes, images, PDFs, or curated YouTube resources with your classmates.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MetadataSection(
    uiState: UploadUiState,
    onBranchChange: (String) -> Unit,
    onSemesterChange: (String) -> Unit,
    onSubjectChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSectionChange: (String) -> Unit,
    onYoutubeResourceTypeChange: (String) -> Unit,
    onTypeSelected: (UploadType) -> Unit,
    onExamYearChange: (String) -> Unit,
    onExamTypeChange: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader("Study Metadata")
            
            DropdownField(
                label = "Branch",
                value = uiState.selectedBranch,
                options = uiState.branches,
                onValueChange = onBranchChange
            )
            
            DropdownField(
                label = "Semester",
                value = uiState.selectedSemester,
                options = uiState.semesters,
                onValueChange = onSemesterChange
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = uiState.subject,
                    onValueChange = onSubjectChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Subject") },
                    placeholder = { Text("DBMS, Operating Systems, DSA...") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
                val showSubjectError = uiState.subject.isBlank() && (uiState.selectedBranch.isNotBlank() || uiState.selectedSemester.isNotBlank())
                if (showSubjectError) {
                    Text(
                        text = "Subject is required",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            DropdownFieldUploadType(
                label = "Document Type",
                value = uiState.selectedType,
                options = UploadType.values().toList(),
                onValueChange = onTypeSelected
            )

            if (uiState.selectedType == UploadType.Youtube) {
                DropdownField(
                    label = "Type",
                    value = if (uiState.youtubeResourceType == "playlist") "Playlist" else "Video",
                    options = listOf("Video", "Playlist"),
                    onValueChange = { selected ->
                        onYoutubeResourceTypeChange(selected.lowercase(java.util.Locale.ROOT))
                    }
                )
            }

            if (uiState.selectedType == UploadType.Assignment) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = uiState.section,
                        onValueChange = onSectionChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Section") },
                        placeholder = { Text("Enter section (e.g. CSE-32, ECE 2)...") },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    val showSectionError = uiState.section.isBlank() && uiState.selectedType == UploadType.Assignment
                    if (showSectionError) {
                        Text(
                            text = "Section is required",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            if (uiState.selectedType in listOf(UploadType.Notes, UploadType.CheatSheet, UploadType.Assignment)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = onTitleChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Title") },
                        placeholder = { Text("Enter a title for this upload...") },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    val showTitleError = uiState.title.isBlank() && uiState.selectedType != null
                    if (showTitleError) {
                        Text(
                            text = "Title is required",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            if (uiState.selectedType == UploadType.Pyq) {
                DropdownField(
                    label = "Exam Year",
                    value = uiState.selectedExamYear,
                    options = uiState.examYears,
                    onValueChange = onExamYearChange
                )
                if (uiState.selectedExamYear.isBlank() && uiState.selectedExamType.isNotBlank()) {
                    Text(
                        text = "Exam Year is required",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                DropdownField(
                    label = "Exam Type",
                    value = uiState.selectedExamType,
                    options = uiState.examTypes,
                    onValueChange = onExamTypeChange
                )
                if (uiState.selectedExamType.isBlank() && uiState.selectedExamYear.isNotBlank()) {
                    Text(
                        text = "Exam Type is required",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            val descriptionPlaceholder = when (uiState.selectedType) {
                UploadType.Notes -> "Mention teacher name or section name..."
                UploadType.Assignment -> "Mention teacher name or section name..."
                UploadType.CheatSheet -> "How it helps..."
                else -> "Optional description..."
            }

            if (uiState.selectedType != UploadType.Pyq && uiState.selectedType != UploadType.Youtube) {
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = onDescriptionChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Description") },
                    placeholder = { Text(descriptionPlaceholder, maxLines = 2) },
                    singleLine = false,
                    minLines = 1,
                    maxLines = 10,
                    shape = RoundedCornerShape(18.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            label = { Text(label) },
            placeholder = { Text("Select $label") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(18.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownFieldUploadType(
    label: String,
    value: UploadType?,
    options: List<UploadType>,
    onValueChange: (UploadType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value?.label ?: "",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            label = { Text(label) },
            placeholder = { Text("Select $label") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(18.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyUploadState(metadataComplete: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Upload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = if (metadataComplete) "Choose a content type" else "Start with metadata",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (metadataComplete) "PDFs, images, and YouTube links cannot be mixed." else "Branch, semester, subject, and document type are required before file selection.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PyqUploadSection(
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
private fun CombinedUploadSection(
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
fun PdfPreviewCard(
    file: SelectedUploadFile,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(file.displayName, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatBytes(file.sizeBytes), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(8.dp))
                    Text("• PDF Document", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove PDF")
            }
        }
    }
}

@Composable
fun ImagePreviewSection(
    files: List<SelectedUploadFile>,
    onRemoveFile: (SelectedUploadFile) -> Unit
) {
    if (files.isEmpty()) return
    
    if (files.size == 1) {
        SingleImagePreviewCard(file = files[0], onRemove = { onRemoveFile(files[0]) })
    } else {
        ImageGridPreviewLayout(files = files, onRemoveFile = onRemoveFile)
    }
}

@Composable
fun SingleImagePreviewCard(
    file: SelectedUploadFile,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val imageInfo = remember(file.uri) {
        runCatching {
            val uri = Uri.parse(file.uri)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(stream, null, options)
                Pair(options.outWidth, options.outHeight)
            }
        }.getOrNull()
    }
    val dimensions = imageInfo?.let { "${it.first} x ${it.second}" }
    
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                val bitmap = remember(file.uri) {
                    runCatching {
                        val uri = Uri.parse(file.uri)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            context.contentResolver.loadThumbnail(uri, android.util.Size(640, 360), null)
                        } else {
                            context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
                        }
                    }.getOrNull()
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = file.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center).size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Remove image", modifier = Modifier.size(18.dp))
                }
            }
            
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = file.displayName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatBytes(file.sizeBytes),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (dimensions != null) {
                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = dimensions,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImageGridPreviewLayout(
    files: List<SelectedUploadFile>,
    onRemoveFile: (SelectedUploadFile) -> Unit
) {
    val displayFiles = files.take(4)
    val remainingCount = if (files.size > 4) files.size - 4 else 0
    
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        val chunked = displayFiles.chunked(2)
        chunked.forEachIndexed { rowIndex, rowFiles ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                rowFiles.forEachIndexed { colIndex, file ->
                    val isLastItem = (rowIndex == 1 && colIndex == 1) || (files.size == 2 && rowIndex == 0 && colIndex == 1) || (files.size == 3 && rowIndex == 1 && colIndex == 0)
                    val showOverlay = isLastItem && remainingCount > 0
                    
                    Box(modifier = Modifier.weight(1f)) {
                        GridImagePreviewCard(
                            file = file,
                            showOverlay = showOverlay,
                            remainingCount = remainingCount,
                            onRemove = { onRemoveFile(file) }
                        )
                    }
                }
                if (rowFiles.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun GridImagePreviewCard(
    file: SelectedUploadFile,
    showOverlay: Boolean,
    remainingCount: Int,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val imageInfo = remember(file.uri) {
        runCatching {
            val uri = Uri.parse(file.uri)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(stream, null, options)
                Pair(options.outWidth, options.outHeight)
            }
        }.getOrNull()
    }
    val dimensions = imageInfo?.let { "${it.first}x${it.second}" }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                val bitmap = remember(file.uri) {
                    runCatching {
                        val uri = Uri.parse(file.uri)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            context.contentResolver.loadThumbnail(uri, android.util.Size(320, 240), null)
                        } else {
                            context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
                        }
                    }.getOrNull()
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = file.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (showOverlay) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+$remainingCount",
                            color = androidx.compose.ui.graphics.Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f), RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Remove image", modifier = Modifier.size(16.dp))
                }
            }
            
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = file.displayName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatBytes(file.sizeBytes),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (dimensions != null) {
                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = dimensions,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YoutubeUploadSection(
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
fun YoutubePreviewCard(
    preview: YoutubePreview?,
    isFetching: Boolean,
    error: String?
) {
    if (preview == null && !isFetching && error == null) {
        return
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(14.dp)) {
            if (isFetching) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().height(74.dp)
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Loading video details...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (error != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (preview != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 112.dp, height = 74.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        if (preview.thumbnailUrl.isNotBlank()) {
                            AsyncImage(
                                model = preview.thumbnailUrl,
                                contentDescription = "Video Thumbnail",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.OndemandVideo,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = preview.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = preview.channelTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveUploadStats(
    fileCount: Int,
    totalSizeBytes: Long
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MiniStat("Selected Files", fileCount.toString(), Modifier.weight(1f))
        MiniStat("Total Size", formatBytes(totalSizeBytes), Modifier.weight(1f))
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun UploadSummaryCard(uiState: UploadUiState) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionHeader("Upload Preview")
            SummaryRow("Branch", uiState.selectedBranch.ifBlank { "Required" })
            SummaryRow("Semester", uiState.selectedSemester.ifBlank { "Required" })
            SummaryRow("Subject", uiState.subject.ifBlank { "Required" })
            SummaryRow("Document Type", uiState.selectedType?.label ?: "Not selected")
            
            if (uiState.selectedType == UploadType.Pyq) {
                SummaryRow("Exam Year", uiState.selectedExamYear.ifBlank { "Required" })
                SummaryRow("Exam Type", uiState.selectedExamType.ifBlank { "Required" })
            }
            
            if (uiState.selectedType == UploadType.Assignment) {
                SummaryRow("Section", uiState.section.ifBlank { "Required" })
            }
            
            if (uiState.selectedType == UploadType.Youtube) {
                val label = if (uiState.youtubeResourceType == "playlist") "Playlist URL" else "Video URL"
                SummaryRow(label, uiState.youtubeUrl.ifBlank { "Not provided" })
            } else {
                SummaryRow("Files", uiState.selectedFiles.size.toString())
                SummaryRow("Total Size", formatBytes(uiState.totalSizeBytes))
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.width(110.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            softWrap = true,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
private fun StatusMessages(uiState: UploadUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        uiState.errorMessage?.let { message ->
            StatusCard(message = message, icon = Icons.Default.ErrorOutline, error = true)
        }
        if (uiState.uploadSuccess) {
            StatusCard(message = "Upload completed successfully!", icon = Icons.Default.CheckCircle, error = false)
        }
    }
}

@Composable
private fun StatusCard(
    message: String,
    icon: ImageVector,
    error: Boolean
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
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
        LiquidTransferProgressBar(progress = progress)
    } else {
        Button(
            onClick = onUpload,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            enabled = enabled,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Upload, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Upload Document", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyPreviewCard(
    title: String,
    subtitle: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) String.format("%.1f MB", mb) else String.format("%.0f KB", kb)
}

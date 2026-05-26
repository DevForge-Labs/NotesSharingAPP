package com.pratyush.notessharing.ui.screens.upload

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
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pratyush.notessharing.model.SelectedUploadFile
import com.pratyush.notessharing.model.UploadFileSource
import com.pratyush.notessharing.model.UploadType
import com.pratyush.notessharing.state.UploadUiState
import com.pratyush.notessharing.ui.components.AdaptiveScrollbar
import com.pratyush.notessharing.ui.components.SectionHeader
import com.pratyush.notessharing.viewmodel.UploadViewModel

@Composable
fun UploadRoute(viewModel: UploadViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.addPickedUris(uris, UploadType.Pdf, UploadFileSource.DocumentPicker)
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.addPickedUris(uris, UploadType.Images, UploadFileSource.Gallery)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        viewModel.onCameraCaptureResult(success)
    }

    UploadScreen(
        uiState = uiState,
        onBranchChange = viewModel::selectBranch,
        onYearChange = viewModel::selectYear,
        onSubjectChange = viewModel::updateSubject,
        onTypeSelected = viewModel::selectUploadType,
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
    onYearChange: (String) -> Unit,
    onSubjectChange: (String) -> Unit,
    onTypeSelected: (UploadType) -> Unit,
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
                    onYearChange = onYearChange,
                    onSubjectChange = onSubjectChange
                )
            }
            item(key = "type-selector", contentType = "type-selector") {
                UploadTypeSelector(
                    selectedType = uiState.selectedType,
                    enabled = uiState.metadataComplete,
                    onTypeSelected = onTypeSelected
                )
            }
            item(key = "content-picker", contentType = "content-picker") {
                Crossfade(targetState = uiState.selectedType, label = "upload-type-content") { type ->
                    when (type) {
                        UploadType.Pdf -> PdfUploadSection(uiState.selectedFiles, onPickPdfs, onRemoveFile)
                        UploadType.Images -> ImageUploadSection(uiState.selectedFiles, onPickImages, onCaptureImage, onRemoveFile)
                        UploadType.Youtube -> YoutubeUploadSection(uiState.youtubeUrl, onYoutubeUrlChange)
                        null -> EmptyUploadState(uiState.metadataComplete)
                    }
                }
            }
            item(key = "live-stats", contentType = "stats") {
                LiveUploadStats(
                    fileCount = uiState.selectedFiles.size,
                    totalSizeBytes = uiState.totalSizeBytes
                )
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
    onYearChange: (String) -> Unit,
    onSubjectChange: (String) -> Unit
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
                label = "Year",
                value = uiState.selectedYear,
                options = uiState.years,
                onValueChange = onYearChange
            )
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

@Composable
fun UploadTypeSelector(
    selectedType: UploadType?,
    enabled: Boolean,
    onTypeSelected: (UploadType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("Select Upload Type")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            UploadTypeCard(
                title = "PDF",
                subtitle = "Documents",
                icon = Icons.Default.Description,
                selected = selectedType == UploadType.Pdf,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                onClick = { onTypeSelected(UploadType.Pdf) }
            )
            UploadTypeCard(
                title = "Images",
                subtitle = "Camera/Gallery",
                icon = Icons.Default.Image,
                selected = selectedType == UploadType.Images,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                onClick = { onTypeSelected(UploadType.Images) }
            )
            UploadTypeCard(
                title = "YouTube",
                subtitle = "Single link",
                icon = Icons.Default.OndemandVideo,
                selected = selectedType == UploadType.Youtube,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                onClick = { onTypeSelected(UploadType.Youtube) }
            )
        }
        AnimatedVisibility(!enabled) {
            Text(
                text = "Complete metadata before selecting files.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UploadTypeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        label = "upload-type-container"
    )
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        label = "upload-type-content"
    )

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 8.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(28.dp))
            Text(title, color = content, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                color = if (selected) content.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyUploadState(metadataComplete: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp),
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
                text = if (metadataComplete) "PDFs, images, and YouTube links cannot be mixed." else "Branch, year, and subject are required before file selection.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PdfUploadSection(
    files: List<SelectedUploadFile>,
    onPickPdfs: () -> Unit,
    onRemoveFile: (SelectedUploadFile) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = onPickPdfs, shape = RoundedCornerShape(18.dp)) {
            Icon(Icons.Default.Description, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add PDFs")
        }
        if (files.isEmpty()) {
            EmptyPreviewCard("No PDFs selected", "Tap Add PDFs to choose one or more PDF documents.")
        } else {
            files.forEach { file ->
                PdfPreviewCard(file = file, onRemove = { onRemoveFile(file) })
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
                Text(formatBytes(file.sizeBytes), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove PDF")
            }
        }
    }
}

@Composable
private fun ImageUploadSection(
    files: List<SelectedUploadFile>,
    onPickImages: () -> Unit,
    onCaptureImage: () -> Unit,
    onRemoveFile: (SelectedUploadFile) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
            EmptyPreviewCard("No images selected", "Open camera or gallery. Camera and gallery images can be mixed.")
        } else {
            ImagePreviewGrid(files = files, onRemoveFile = onRemoveFile)
        }
    }
}

@Composable
fun ImagePreviewGrid(
    files: List<SelectedUploadFile>,
    onRemoveFile: (SelectedUploadFile) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(104.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(((files.size + 2) / 3 * 122).coerceAtLeast(122).dp),
        userScrollEnabled = false,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(files, key = { it.uri }) { file ->
            ImagePreviewTile(file = file, onRemove = { onRemoveFile(file) })
        }
    }
}

@Composable
private fun ImagePreviewTile(
    file: SelectedUploadFile,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val bitmap = remember(file.uri) {
        runCatching {
            val uri = Uri.parse(file.uri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(uri, android.util.Size(220, 220), null)
            } else {
                context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
            }
        }.getOrNull()
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
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
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(28.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f), RoundedCornerShape(10.dp))
        ) {
            Icon(Icons.Default.Close, contentDescription = "Remove image", modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun YoutubeUploadSection(
    youtubeUrl: String,
    onYoutubeUrlChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = youtubeUrl,
            onValueChange = onYoutubeUrlChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Paste YouTube URL") },
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
        YoutubePreviewCard(youtubeUrl)
    }
}

@Composable
fun YoutubePreviewCard(url: String) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 112.dp, height = 74.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.primaryContainer)
                        ),
                        RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.OndemandVideo, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "DBMS Full Course",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = url.ifBlank { "youtube.com/..." },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
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
            SummaryRow("Year", uiState.selectedYear.ifBlank { "Required" })
            SummaryRow("Subject", uiState.subject.ifBlank { "Required" })
            SummaryRow("Content Type", uiState.selectedType?.label ?: "Not selected")
            SummaryRow("Files", if (uiState.selectedType == UploadType.Youtube && uiState.youtubeUrl.isNotBlank()) "1 link" else uiState.selectedFiles.size.toString())
            SummaryRow("Total Size", formatBytes(uiState.totalSizeBytes))
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatusMessages(uiState: UploadUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        uiState.errorMessage?.let { message ->
            StatusCard(message = message, icon = Icons.Default.ErrorOutline, error = true)
        }
        uiState.savedUpload?.let { item ->
            StatusCard(message = "Saved locally: ${item.subject} (${item.type.label})", icon = Icons.Default.CheckCircle, error = false)
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
    onUpload: () -> Unit
) {
    Button(
        onClick = onUpload,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        enabled = !isSaving,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        if (isSaving) {
            LinearProgressIndicator(modifier = Modifier.width(96.dp), color = MaterialTheme.colorScheme.onPrimary)
        } else {
            Icon(Icons.Default.Upload, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Save Upload Locally", fontWeight = FontWeight.Bold)
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

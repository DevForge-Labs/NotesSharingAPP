package com.pravor.notessharing.ui.features.upload.edit

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.pravor.notessharing.domain.model.DocumentDetail
import com.pravor.notessharing.domain.model.EditableAttachment
import com.pravor.notessharing.domain.model.UploadFileSource
import com.pravor.notessharing.domain.model.UploadType
import com.pravor.notessharing.ui.common.AdaptiveScrollbar
import com.pravor.notessharing.ui.common.LiquidTransferProgressBar
import com.pravor.notessharing.ui.common.components.StatePanel
import com.pravor.notessharing.ui.common.loading.StudyLoadingIndicator
import com.pravor.notessharing.ui.features.upload.components.formatBytes
import com.pravor.notessharing.ui.navigation.LocalBottomBarPadding

@Composable
fun UploadEditRoute(
    documentId: String,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: UploadEditViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(documentId) {
        viewModel.loadDocument(documentId)
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            onSaveSuccess()
        }
    }

    BackHandler(enabled = uiState.hasChanges && !uiState.isSaving) {
        viewModel.showDiscardConfirmation()
    }

    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.addPickedUris(uris, UploadFileSource.DocumentPicker)
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.addPickedUris(uris, UploadFileSource.Gallery)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        viewModel.onCameraCaptureResult(success)
    }

    UploadEditScreen(
        uiState = uiState,
        onBackClick = {
            if (uiState.hasChanges && !uiState.isSaving) {
                viewModel.showDiscardConfirmation()
            } else {
                onBackClick()
            }
        },
        onPickPdfs = {
            pdfPicker.launch(
                arrayOf(
                    "application/pdf",
                    "application/vnd.ms-powerpoint",
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                )
            )
        },
        onPickImages = { imagePicker.launch(arrayOf("image/*")) },
        onCaptureImage = { cameraLauncher.launch(viewModel.createCameraUri()) },
        onRemoveAttachment = viewModel::removeAttachment,
        onSaveChanges = viewModel::saveChanges,
        onConfirmDiscard = {
            viewModel.dismissDiscardConfirmation()
            onBackClick()
        },
        onDismissDiscard = viewModel::dismissDiscardConfirmation
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadEditScreen(
    uiState: UploadEditUiState,
    onBackClick: () -> Unit,
    onPickPdfs: () -> Unit,
    onPickImages: () -> Unit,
    onCaptureImage: () -> Unit,
    onRemoveAttachment: (EditableAttachment) -> Unit,
    onSaveChanges: () -> Unit,
    onConfirmDiscard: () -> Unit,
    onDismissDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val bottomBarPadding = LocalBottomBarPadding.current
    var showAddAttachmentSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Edit Attachments",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            when {
                uiState.isLoading -> {
                    StudyLoadingIndicator(
                        text = "Loading upload details...",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                uiState.document == null && uiState.errorMessage != null -> {
                    StatePanel(
                        title = "Cannot edit upload",
                        message = uiState.errorMessage,
                        modifier = Modifier.padding(top = 96.dp)
                    )
                }
                uiState.document != null -> {
                    val doc = uiState.document
                    val allActive = uiState.allActiveAttachments
                    val isPyq = uiState.uploadType == UploadType.Pyq
                    val canAddMore = !isPyq || allActive.isEmpty()

                    Box(Modifier.fillMaxSize()) {
                        // Scrolling attachment list
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(
                                start = 18.dp,
                                end = 18.dp,
                                top = 8.dp,
                                bottom = if (bottomBarPadding > 0.dp) bottomBarPadding + 64.dp else 68.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // 1. Compact Resource Information Card
                            item(key = "metadata-summary") {
                                CompactResourceInfoCard(doc = doc, type = uiState.uploadType)
                            }

                            // 2. Integrated Attachments Section Header with Stats
                            item(key = "attachments-header") {
                                AttachmentSectionHeader(
                                    attachmentCount = allActive.size,
                                    totalSizeBytes = uiState.totalSizeBytes,
                                    hasChanges = uiState.hasChanges
                                )
                            }

                            // 3. Warning banner if all attachments removed
                            if (allActive.isEmpty()) {
                                item(key = "validation-empty-banner") {
                                    EmptyAttachmentWarningBanner()
                                }
                            }

                            // 4. Attachment Cards
                            items(allActive, key = { it.id }) { item ->
                                val isExisting = item is EditableAttachment.ExistingRemote
                                AttachmentItemCard(
                                    item = item,
                                    isExisting = isExisting,
                                    onRemove = { onRemoveAttachment(item) }
                                )
                            }

                            // 5. Modern Add Attachment Button
                            item(key = "add-button") {
                                AddAttachmentButton(
                                    isPyq = isPyq,
                                    canAddMore = canAddMore,
                                    onClick = {
                                        if (isPyq) {
                                            if (canAddMore) onPickPdfs()
                                        } else {
                                            showAddAttachmentSheet = true
                                        }
                                    }
                                )
                            }

                            // 6. Error messages if present
                            if (uiState.errorMessage != null) {
                                item(key = "error-message") {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = uiState.errorMessage,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                        )
                                    }
                                }
                            }
                        }

                        AdaptiveScrollbar(
                            listState = listState,
                            modifier = Modifier.padding(bottom = if (bottomBarPadding > 0.dp) bottomBarPadding + 56.dp else 56.dp)
                        )

                        // Floating Save Changes button placed just above the bottom bar
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                            MaterialTheme.colorScheme.background
                                        )
                                    )
                                )
                                .then(
                                    if (bottomBarPadding > 0.dp) {
                                        Modifier.padding(bottom = (bottomBarPadding - 4.dp).coerceAtLeast(0.dp))
                                    } else {
                                        Modifier
                                            .navigationBarsPadding()
                                            .padding(bottom = 8.dp)
                                    }
                                )
                                .padding(
                                    start = 18.dp,
                                    end = 18.dp,
                                    top = 6.dp,
                                    bottom = 2.dp
                                )
                        ) {
                            EditBottomActionArea(
                                allActiveCount = allActive.size,
                                uploadType = uiState.uploadType,
                                isSaving = uiState.isSaving,
                                canSave = uiState.canSave,
                                hasChanges = uiState.hasChanges,
                                saveProgress = uiState.saveProgress,
                                onSave = onSaveChanges,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    // Material 3 Bottom Sheet for choosing attachment source
    if (showAddAttachmentSheet) {
        AddAttachmentBottomSheet(
            onDismiss = { showAddAttachmentSheet = false },
            onPickDocuments = {
                showAddAttachmentSheet = false
                onPickPdfs()
            },
            onPickGallery = {
                showAddAttachmentSheet = false
                onPickImages()
            },
            onCaptureCamera = {
                showAddAttachmentSheet = false
                onCaptureImage()
            }
        )
    }

    // Discard Confirmation Dialog
    if (uiState.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = onDismissDiscard,
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    text = "Discard Changes?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "You have unsaved changes to your attachments. If you leave now, your changes will be discarded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmDiscard) {
                    Text("Discard", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDiscard) {
                    Text("Keep Editing", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

/**
 * Redesigned compact metadata card with subtle type chip and lock indicator.
 */
@Composable
private fun CompactResourceInfoCard(doc: DocumentDetail, type: UploadType) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Line 1: Doc type chip
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Text(
                    text = type.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            // Line 2: Title
            Text(
                text = doc.title.ifBlank { "Untitled Resource" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Line 3: subject chip     branch chip     sem number chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Subject chip
                if (doc.subject.isNotBlank()) {
                    MetaChip(text = doc.subject.trim())
                }

                // Branch chip (in uppercase)
                if (doc.branch.isNotBlank()) {
                    MetaChip(text = doc.branch.trim().uppercase())
                }

                // Sem number chip
                if (doc.semester.isNotBlank()) {
                    val semDigits = Regex("""\d+""").find(doc.semester)?.value
                    val semLabel = if (semDigits != null) "Sem $semDigits" else doc.semester.trim()
                    MetaChip(text = semLabel)
                }
            }
        }
    }
}

@Composable
private fun MetaChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/**
 * Integrated section header with dynamic count and size stats.
 */
@Composable
private fun AttachmentSectionHeader(
    attachmentCount: Int,
    totalSizeBytes: Long,
    hasChanges: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Attachments",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = "$attachmentCount",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
            if (hasChanges) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Edited",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Text(
            text = formatBytes(totalSizeBytes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Warning banner when count drops to 0.
 */
@Composable
private fun EmptyAttachmentWarningBanner() {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "At least one attachment is required. Please add a document or image before saving.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Polished attachment card with consistent remove touch target and subtle badges.
 */
@Composable
private fun AttachmentItemCard(
    item: EditableAttachment,
    isExisting: Boolean,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading Icon / Thumbnail
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                when {
                    item.isPdf -> {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    item is EditableAttachment.ExistingRemote && !item.thumbnailUrl.isNullOrBlank() -> {
                        AsyncImage(
                            model = item.thumbnailUrl,
                            contentDescription = item.displayName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    item is EditableAttachment.NewlyAddedLocal && item.isImage -> {
                        AsyncImage(
                            model = Uri.parse(item.uri),
                            contentDescription = item.displayName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            // Text Metadata
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formatBytes(item.sizeBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isExisting) {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        }
                    ) {
                        Text(
                            text = if (isExisting) "Existing" else "New",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isExisting) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // Consistent Remove Action
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove attachment",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Sleek, single modern button to add attachments.
 */
@Composable
private fun AddAttachmentButton(
    isPyq: Boolean,
    canAddMore: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (canAddMore) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f)
    }

    val contentColor = if (canAddMore) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    val borderColor = if (canAddMore) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = canAddMore, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = when {
                    isPyq && !canAddMore -> "PYQ has 1 PDF (Remove first to replace)"
                    isPyq -> "Add Replacement PDF"
                    else -> "Add Attachments"
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}

/**
 * Polished Material 3 BottomSheet offering Documents, Gallery, and Camera options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAttachmentBottomSheet(
    onDismiss: () -> Unit,
    onPickDocuments: () -> Unit,
    onPickGallery: () -> Unit,
    onCaptureCamera: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Add Attachments",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Option 1: Documents (PDF / PPT)
            AttachmentOptionItem(
                icon = Icons.Default.Description,
                iconTint = MaterialTheme.colorScheme.primary,
                iconBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                title = "Documents",
                subtitle = "PDF, PPT, or PPTX presentation files",
                onClick = onPickDocuments
            )

            // Option 2: Gallery (Images)
            AttachmentOptionItem(
                icon = Icons.Default.AddPhotoAlternate,
                iconTint = MaterialTheme.colorScheme.secondary,
                iconBg = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                title = "Gallery",
                subtitle = "Select diagrams or photos from device",
                onClick = onPickGallery
            )

            // Option 3: Camera
            AttachmentOptionItem(
                icon = Icons.Default.CameraAlt,
                iconTint = MaterialTheme.colorScheme.tertiary,
                iconBg = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                title = "Camera",
                subtitle = "Capture a new picture with camera",
                onClick = onCaptureCamera
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AttachmentOptionItem(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    iconBg: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Compact bottom action area with accessible Save Changes button floating above the app bottom navigation bar.
 */
@Composable
private fun EditBottomActionArea(
    allActiveCount: Int,
    uploadType: UploadType,
    isSaving: Boolean,
    canSave: Boolean,
    hasChanges: Boolean,
    saveProgress: Float,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp,
        tonalElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        if (isSaving) {
            val progressInt = (saveProgress * 100).toInt()
            LiquidTransferProgressBar(
                progress = saveProgress,
                statusText = "Saving attachments... $progressInt%",
                showSpinner = false,
                showCheckmarkOnComplete = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )
        } else {
            val primaryColor = MaterialTheme.colorScheme.primary
            val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
            val disabledBg = MaterialTheme.colorScheme.surfaceContainerHigh
            val disabledContent = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (canSave) primaryColor else disabledBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = canSave, onClick = onSave)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = null,
                        tint = if (canSave) onPrimaryColor else disabledContent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when {
                            allActiveCount == 0 -> "Add at least 1 file"
                            !hasChanges -> "No Changes to Save"
                            uploadType == UploadType.Pyq && allActiveCount != 1 -> "PYQ requires 1 PDF"
                            else -> "Save Changes"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (canSave) onPrimaryColor else disabledContent
                    )
                }
            }
        }
    }
}

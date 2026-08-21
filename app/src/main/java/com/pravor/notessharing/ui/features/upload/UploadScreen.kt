package com.pravor.notessharing.ui.features.upload

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pravor.notessharing.domain.model.SelectedUploadFile
import com.pravor.notessharing.domain.model.UploadFileSource
import com.pravor.notessharing.domain.model.UploadType
import com.pravor.notessharing.ui.features.explore.components.ClimbingMascotScrollbar
import com.pravor.notessharing.ui.features.explore.components.MonkeyMascot
import com.pravor.notessharing.ui.features.upload.components.CombinedUploadSection
import com.pravor.notessharing.ui.features.upload.components.EmptyUploadState
import com.pravor.notessharing.ui.features.upload.components.LiveUploadStats
import com.pravor.notessharing.ui.features.upload.components.MetadataSection
import com.pravor.notessharing.ui.features.upload.components.PyqUploadSection
import com.pravor.notessharing.ui.features.upload.components.StatusMessages
import com.pravor.notessharing.ui.features.upload.components.UploadButton
import com.pravor.notessharing.ui.features.upload.components.UploadHeader
import com.pravor.notessharing.ui.features.upload.components.UploadSummaryCard
import com.pravor.notessharing.ui.features.upload.components.YoutubeUploadSection
import com.pravor.notessharing.ui.features.upload.components.clearFocusOnOutsideTap
import com.pravor.notessharing.ui.navigation.LocalBottomBarPadding

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
        onGroupChange = viewModel::selectGroup,
        onSubjectChange = viewModel::updateSubject,
        onSubjectSelected = viewModel::selectCatalogSubject,
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
    onGroupChange: (String) -> Unit,
    onSubjectChange: (String) -> Unit,
    onSubjectSelected: (CatalogSubject) -> Unit,
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
    val bottomPadding = LocalBottomBarPadding.current
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
                com.pravor.notessharing.domain.model.extractYoutubePlaylistId(uiState.youtubeUrl) != null
            } else {
                com.pravor.notessharing.domain.model.extractYoutubeVideoId(uiState.youtubeUrl) != null
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
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 14.dp + bottomPadding),
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
                    onGroupChange = onGroupChange,
                    onSubjectChange = onSubjectChange,
                    onSubjectSelected = onSubjectSelected,
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
        ClimbingMascotScrollbar(listState = listState) { modifier, isScrolling ->
            MonkeyMascot(modifier = modifier, isScrolling = isScrolling)
        }
    }
}

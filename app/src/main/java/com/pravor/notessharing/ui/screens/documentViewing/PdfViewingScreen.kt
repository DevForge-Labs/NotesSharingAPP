package com.pravor.notessharing.ui.screens.documentViewing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pravor.notessharing.ui.components.pdfViewing.PdfErrorView
import com.pravor.notessharing.ui.components.pdfViewing.PdfLoadingView
import com.pravor.notessharing.ui.components.pdfViewing.PdfViewerContent
import com.pravor.notessharing.ui.theme.NotesSharingTheme
import com.pravor.notessharing.viewmodel.PdfViewingUiState
import com.pravor.notessharing.viewmodel.PdfViewingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewingScreen(
    documentId: String,
    fileUrl: String,
    title: String,
    onBackClick: () -> Unit,
    viewModel: PdfViewingViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(documentId, fileUrl) {
        android.util.Log.d("PDF_DEBUG", "PdfViewingScreen opened")
        android.util.Log.d("PDF_DEBUG", "DocumentId=$documentId")
        android.util.Log.d("PDF_DEBUG", "FileUrl=$fileUrl")
        viewModel.loadPdf(context, documentId, fileUrl)
    }

    DisposableEffect(Unit) {
        onDispose {
            android.util.Log.d("DETAILS_DEBUG", "Leaving PdfViewingScreen")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title.ifBlank { "PDF Viewer" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    val state = uiState
                    if (state is PdfViewingUiState.Success) {
                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Open") },
                                onClick = {
                                    showMenu = false
                                    com.pravor.notessharing.ui.components.utils.FileSharingUtils.openFile(
                                        context = context,
                                        file = state.pdfFile,
                                        mimeType = "application/pdf"
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Open With") },
                                onClick = {
                                    showMenu = false
                                    com.pravor.notessharing.ui.components.utils.FileSharingUtils.openFileWith(
                                        context = context,
                                        file = state.pdfFile,
                                        mimeType = "application/pdf"
                                    )
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                PdfViewingUiState.Loading -> {
                    PdfLoadingView()
                }
                is PdfViewingUiState.Error -> {
                    PdfErrorView(
                        message = state.message,
                        onRetry = {
                            viewModel.loadPdf(context, documentId, fileUrl)
                        }
                    )
                }
                is PdfViewingUiState.Success -> {
                    PdfViewerContent(
                        pdfFile = state.pdfFile,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PdfViewingScreenPreview() {
    NotesSharingTheme {
        PdfViewingScreen(
            documentId = "preview-doc-id",
            fileUrl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
            title = "Preview Document",
            onBackClick = {}
        )
    }
}

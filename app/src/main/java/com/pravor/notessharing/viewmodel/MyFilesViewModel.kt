package com.pravor.notessharing.viewmodel

import androidx.lifecycle.ViewModel
import com.pravor.notessharing.state.MyFilesContent
import com.pravor.notessharing.state.MyFilesUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MyFilesViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<MyFilesUiState>(
        MyFilesUiState.Success(
            MyFilesContent(
                savedFiles = DummyData.savedFiles,
                uploadedFiles = DummyData.uploadedFiles
            )
        )
    )
    val uiState: StateFlow<MyFilesUiState> = _uiState.asStateFlow()
}

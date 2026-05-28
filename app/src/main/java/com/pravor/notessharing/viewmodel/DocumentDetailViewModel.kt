package com.pravor.notessharing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pravor.notessharing.data.DocumentDetailRepository
import com.pravor.notessharing.model.DocumentDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DocumentDetailUiState {
    data object Loading : DocumentDetailUiState
    data class Error(val message: String) : DocumentDetailUiState
    data class Success(
        val document: DocumentDetail,
        val contributorLevel: String,
        val relatedDocuments: List<DocumentDetail>
    ) : DocumentDetailUiState
}

class DocumentDetailViewModel(
    private val repository: DocumentDetailRepository = DocumentDetailRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow<DocumentDetailUiState>(DocumentDetailUiState.Loading)
    val uiState: StateFlow<DocumentDetailUiState> = _uiState.asStateFlow()

    fun loadDocumentDetail(documentId: String) {
        viewModelScope.launch {
            _uiState.value = DocumentDetailUiState.Loading
            try {
                val doc = repository.getDocument(documentId)
                if (doc != null) {
                    val contributorLevel = repository.getUploaderContributorLevel(doc.uploaderId) ?: "Bronze Contributor"
                    val relatedDocs = repository.getRelatedDocuments(doc)
                    _uiState.value = DocumentDetailUiState.Success(
                        document = doc,
                        contributorLevel = contributorLevel,
                        relatedDocuments = relatedDocs
                    )
                } else {
                    _uiState.value = DocumentDetailUiState.Error("Document details not found.")
                }
            } catch (e: Exception) {
                _uiState.value = DocumentDetailUiState.Error(e.localizedMessage ?: "Failed to fetch document details.")
            }
        }
    }
}

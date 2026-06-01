package com.pravor.notessharing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pravor.notessharing.data.DocumentDetailRepository
import com.pravor.notessharing.model.DocumentDetail
import com.pravor.notessharing.model.toDocumentDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

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
    private var docListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun loadDocumentDetail(documentId: String) {
        docListener?.remove()
        _uiState.value = DocumentDetailUiState.Loading
        viewModelScope.launch {
            try {
                val collections = listOf("documents", "notes", "pyqs", "assignments", "cheatsheets")
                var targetCol: String? = null
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                for (col in collections) {
                    val snap = firestore.collection(col).document(documentId).get().await()
                    if (snap.exists()) {
                        targetCol = col
                        break
                    }
                }

                if (targetCol != null) {
                    val docRef = firestore.collection(targetCol).document(documentId)
                    docListener = docRef.addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            _uiState.value = DocumentDetailUiState.Error(error.localizedMessage ?: "Listener failed")
                            return@addSnapshotListener
                        }
                        if (snapshot != null && snapshot.exists()) {
                            val data = snapshot.data
                            if (data != null) {
                                viewModelScope.launch {
                                    val docDetail = data.toDocumentDetail(documentId)
                                    val contributorLevel = repository.getUploaderContributorLevel(docDetail.uploaderId) ?: "Bronze Contributor"
                                    val relatedDocs = repository.getRelatedDocuments(docDetail)
                                    _uiState.value = DocumentDetailUiState.Success(
                                        document = docDetail,
                                        contributorLevel = contributorLevel,
                                        relatedDocuments = relatedDocs
                                    )
                                }
                            }
                        }
                    }
                } else {
                    _uiState.value = DocumentDetailUiState.Error("Document details not found.")
                }
            } catch (e: Exception) {
                _uiState.value = DocumentDetailUiState.Error(e.localizedMessage ?: "Failed to fetch document details.")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        docListener?.remove()
    }
}

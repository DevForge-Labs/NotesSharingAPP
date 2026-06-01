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

    private val upvoteRepository = com.pravor.notessharing.upvotes.UpvoteRepository()
    private val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

    fun toggleUpvote(itemId: String) {
        val currentUid = auth.currentUser?.uid ?: return
        val doc = (_uiState.value as? DocumentDetailUiState.Success)?.document ?: return
        val col = upvoteRepository.getCollectionForDocType(doc.documentType)

        viewModelScope.launch {
            upvoteRepository.toggleUpvote(
                documentId = itemId,
                collectionName = col,
                currentUpvotes = doc.upvotes,
                userId = currentUid
            )
        }
    }

    fun observeUpvotes(docId: String, docType: String) {
        val currentUid = auth.currentUser?.uid
        viewModelScope.launch {
            if (currentUid != null) {
                upvoteRepository.loadInitialUpvotesIfNeeded(currentUid)
            }
            val col = upvoteRepository.getCollectionForDocType(docType)
            upvoteRepository.observeVisibleDocuments("DetailsScreen", listOf(docId to col))
        }
    }

    fun clearUpvotesObservation() {
        upvoteRepository.observeVisibleDocuments("DetailsScreen", emptyList())
    }

    fun loadDocumentDetail(documentId: String) {
        _uiState.value = DocumentDetailUiState.Loading
        viewModelScope.launch {
            try {
                val collections = listOf("documents", "notes", "pyqs", "assignments", "cheatsheets")
                var targetCol: String? = null
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                var snapshot: com.google.firebase.firestore.DocumentSnapshot? = null
                for (col in collections) {
                    val snap = firestore.collection(col).document(documentId).get().await()
                    if (snap.exists()) {
                        targetCol = col
                        snapshot = snap
                        break
                    }
                }

                if (targetCol != null && snapshot != null) {
                    val data = snapshot.data
                    if (data != null) {
                        val docDetail = data.toDocumentDetail(documentId)
                        val contributorLevel = repository.getUploaderContributorLevel(docDetail.uploaderId) ?: "Bronze Contributor"
                        val relatedDocs = repository.getRelatedDocuments(docDetail)
                        observeUpvotes(docDetail.id, docDetail.documentType)
                        _uiState.value = DocumentDetailUiState.Success(
                            document = docDetail,
                            contributorLevel = contributorLevel,
                            relatedDocuments = relatedDocs
                        )
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
        clearUpvotesObservation()
    }
}

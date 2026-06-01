package com.pravor.notessharing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pravor.notessharing.model.FileType
import com.pravor.notessharing.model.StudyFile
import com.pravor.notessharing.state.MyFilesContent
import com.pravor.notessharing.state.MyFilesUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class MyFilesViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<MyFilesUiState>(MyFilesUiState.Loading)
    val uiState: StateFlow<MyFilesUiState> = _uiState.asStateFlow()

    private val firestore = FirebaseFirestore.getInstance()

    init {
        loadMyFiles()
    }

    fun loadMyFiles() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid == null) {
            _uiState.update {
                MyFilesUiState.Success(
                    MyFilesContent(
                        savedFiles = emptyList(),
                        uploadedFiles = emptyList()
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                val collections = listOf("documents", "notes", "pyqs", "assignments", "cheatsheets", "videos")
                val allDocs = coroutineScope {
                    val deferreds = collections.map { col ->
                        async {
                            try {
                                firestore.collection(col)
                                    .whereEqualTo("uploaderId", currentUid)
                                    .get()
                                    .await()
                                    .documents
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }
                    }
                    deferreds.awaitAll().flatten()
                }
                
                val realUploaded = allDocs.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    documentToStudyFile(data)
                }

                _uiState.update {
                    MyFilesUiState.Success(
                        MyFilesContent(
                            savedFiles = emptyList(),
                            uploadedFiles = realUploaded
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    MyFilesUiState.Success(
                        MyFilesContent(
                            savedFiles = emptyList(),
                            uploadedFiles = emptyList()
                        )
                    )
                }
            }
        }
    }

    private fun documentToStudyFile(doc: Map<String, Any>): StudyFile {
        val id = doc["documentId"] as? String ?: ""
        val title = doc["title"] as? String ?: ""
        val uploadTimestamp = doc["uploadedAt"] as? Long ?: (doc["uploadTimestamp"] as? Long ?: System.currentTimeMillis())
        val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
        val uploadDate = "Uploaded " + sdf.format(java.util.Date(uploadTimestamp))
        
        val docType = doc["documentType"] as? String ?: (doc["type"] as? String ?: "Notes")
        val fileType = when (docType) {
            "PYQ" -> FileType.Pyq
            "Cheat Sheet" -> FileType.CheatSheet
            "Assignment" -> FileType.Notes
            "Notes" -> FileType.Notes
            "YouTube Resource", "Videos" -> FileType.Video
            else -> FileType.Pdf
        }
        
        val upvotes = (doc["upvotes"] as? Long ?: (doc["likesCount"] as? Long ?: 0L)).toInt()
        val downloads = (doc["downloads"] as? Long ?: (doc["downloadsCount"] as? Long ?: 0L)).toInt()
        
        val thumbnailUrl = doc["thumbnailUrl"] as? String
        val subject = doc["subject"] as? String
        val examYear = doc["examYear"] as? String
        val examType = doc["examType"] as? String
        val sectionDisplay = doc["sectionDisplay"] as? String ?: doc["section"] as? String

        return StudyFile(
            id = id,
            title = title,
            uploadDate = uploadDate,
            fileType = fileType,
            downloads = downloads,
            upvotes = upvotes,
            thumbnailUrl = thumbnailUrl,
            subject = subject,
            documentType = docType,
            examYear = examYear,
            examType = examType,
            sectionDisplay = sectionDisplay
        )
    }
}

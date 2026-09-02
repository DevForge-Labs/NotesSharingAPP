package com.pravor.notessharing.ui.features.profile

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*

import com.pravor.notessharing.data.repository.ProfileRepository
import com.pravor.notessharing.domain.model.*

import com.pravor.notessharing.core.util.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pravor.notessharing.domain.model.Profile
import com.pravor.notessharing.ui.common.ProfileUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.FirebaseFirestore

import com.pravor.notessharing.ui.common.EditProfileState

class ProfileViewModel(
    private val profileRepository: ProfileRepository = ProfileRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _editState = MutableStateFlow<EditProfileState>(EditProfileState.Idle)
    val editState: StateFlow<EditProfileState> = _editState.asStateFlow()

    private val metadataRepository = com.pravor.notessharing.data.repository.MetadataRepository()

    private val _branches = MutableStateFlow<List<com.pravor.notessharing.data.repository.BranchMetadata>>(emptyList())
    val branches: StateFlow<List<com.pravor.notessharing.data.repository.BranchMetadata>> = _branches.asStateFlow()

    private val _isBranchesLoading = MutableStateFlow(false)
    val isBranchesLoading: StateFlow<Boolean> = _isBranchesLoading.asStateFlow()

    private val _branchesError = MutableStateFlow<String?>(null)
    val branchesError: StateFlow<String?> = _branchesError.asStateFlow()

    fun loadBranchesForCollege(collegeId: String) {
        viewModelScope.launch {
            _isBranchesLoading.value = true
            _branchesError.value = null
            try {
                val list = metadataRepository.getBranchesForCollege(collegeId)
                if (list.isEmpty()) {
                    _branchesError.value = "Unable to load branches"
                } else {
                    _branches.value = list
                }
            } catch (e: Exception) {
                _branchesError.value = "Unable to load branches"
            } finally {
                _isBranchesLoading.value = false
            }
        }
    }

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        val currentFirebaseUser = FirebaseAuth.getInstance().currentUser
        if (currentFirebaseUser == null) {
            _uiState.update { ProfileUiState.Empty }
            return
        }

        val fallbackProfile = Profile(
            uid = currentFirebaseUser.uid,
            name = currentFirebaseUser.displayName ?: "User",
            email = currentFirebaseUser.email ?: "",
            semester = "Not Set",
            profileImageUrl = currentFirebaseUser.photoUrl?.toString() ?: "",
            role = "user",
            totalUploads = 0,
            bookmarks = 0,
            upvotes = 0,
            contributorLevel = 1,
            branch = "Computer Science",
            createdAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            profileRepository.observeProfile(currentFirebaseUser.uid)
                .catch { throwable ->
                    // Fallback safely to current auth user details and defaults, never crash
                    val collegeName = metadataRepository.resolveCollegeName(fallbackProfile.college)
                    val branchName = metadataRepository.resolveBranchName(fallbackProfile.college, fallbackProfile.branch)
                    _uiState.update { ProfileUiState.Success(fallbackProfile, collegeName, branchName) }
                }
                .collect { profile ->
                    val baseProfile = profile ?: fallbackProfile
                    val collegeName = metadataRepository.resolveCollegeName(baseProfile.college)
                    val branchName = metadataRepository.resolveBranchName(baseProfile.college, baseProfile.branch)
                    
                    // Render UI immediately from Room DB (NO SPINNER when cached profile exists!)
                    _uiState.update { ProfileUiState.Success(baseProfile, collegeName, branchName) }

                    // Refresh counts asynchronously in background without blocking UI
                    launch {
                        try {
                            val bookmarksDeferred = async {
                                try {
                                    val countSnap = FirebaseFirestore.getInstance().collection("bookmarks")
                                        .whereEqualTo("userId", currentFirebaseUser.uid)
                                        .count()
                                        .get(com.google.firebase.firestore.AggregateSource.SERVER)
                                        .await()
                                    countSnap.count.toInt()
                                } catch (e: Exception) {
                                    val loadedBookmarks = com.pravor.notessharing.data.repository.BookmarkRepository.bookmarksFlow.value
                                    if (loadedBookmarks.isNotEmpty()) loadedBookmarks.size else baseProfile.bookmarks
                                }
                            }
                            
                            val collections = listOf("notes", "pyqs", "assignments", "cheatsheets", "videos")
                            val upvotesDeferred = collections.map { col ->
                                async {
                                    try {
                                        val querySnapshot = FirebaseFirestore.getInstance().collection(col)
                                            .whereEqualTo("uploaderId", currentFirebaseUser.uid)
                                            .get()
                                            .await()
                                        querySnapshot.documents.sumOf { doc ->
                                            (doc.getLong("upvotes") ?: doc.getLong("likesCount") ?: 0L).toInt()
                                        }
                                    } catch (e: Exception) {
                                        0
                                    }
                                }
                            }
                            
                            val finalBookmarks = bookmarksDeferred.await()
                            val finalUpvotes = upvotesDeferred.awaitAll().sum()
                            
                            if (finalBookmarks != baseProfile.bookmarks || finalUpvotes != baseProfile.upvotes) {
                                val updatedProfile = baseProfile.copy(
                                    bookmarks = finalBookmarks,
                                    upvotes = finalUpvotes
                                )
                                profileRepository.saveLocalProfile(updatedProfile)
                            }
                        } catch (e: Exception) {
                            // Ignore background count refresh failure; UI is already successfully displayed
                        }
                    }
                }
        }
    }

    fun updateProfile(
        name: String,
        semester: String,
        section: String,
        branch: String,
        newLocalImageUri: String?,
        isImageRemoved: Boolean,
        onSuccess: () -> Unit
    ) {
        val currentFirebaseUser = FirebaseAuth.getInstance().currentUser
        if (currentFirebaseUser == null) {
            _editState.update { EditProfileState.Error("User is not authenticated.") }
            return
        }

        if (name.trim().isBlank() || name.trim().length < 2) {
            _editState.update { EditProfileState.Error("Name cannot be blank and must be at least 2 characters.") }
            return
        }

        if (branch.trim().isBlank()) {
            _editState.update { EditProfileState.Error("Branch cannot be blank.") }
            return
        }

        val allowedSemesters = listOf(
            "Semester 1", "Semester 2", "Semester 3", "Semester 4",
            "Semester 5", "Semester 6", "Semester 7", "Semester 8"
        )
        if (!allowedSemesters.contains(semester.trim())) {
            _editState.update { EditProfileState.Error("Please select a valid semester.") }
            return
        }

        viewModelScope.launch {
            _editState.update { EditProfileState.Loading }
            try {
                var finalImageUrl = ""
                val currentProfile = (uiState.value as? ProfileUiState.Success)?.profile
                finalImageUrl = currentProfile?.profileImageUrl ?: ""

                if (isImageRemoved) {
                    finalImageUrl = ""
                } else if (newLocalImageUri != null) {
                    finalImageUrl = profileRepository.uploadProfileImage(currentFirebaseUser.uid, newLocalImageUri)
                }

                val normalizedSection = com.pravor.notessharing.core.util.NormalizationUtil.normalizeSection(section)

                profileRepository.updateProfileFields(
                    uid = currentFirebaseUser.uid,
                    name = name.trim(),
                    semester = semester.trim(),
                    section = normalizedSection,
                    profileImageUrl = finalImageUrl,
                    branch = branch.trim()
                )
                _editState.update { EditProfileState.Success }
                onSuccess()
            } catch (e: Exception) {
                _editState.update { EditProfileState.Error(e.localizedMessage ?: "Failed to update profile.") }
            }
        }
    }

    fun clearEditState() {
        _editState.update { EditProfileState.Idle }
    }
}

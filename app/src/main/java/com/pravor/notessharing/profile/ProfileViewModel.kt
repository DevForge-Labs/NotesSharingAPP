package com.pravor.notessharing.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pravor.notessharing.model.Profile
import com.pravor.notessharing.state.ProfileUiState
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

import com.pravor.notessharing.state.EditProfileState

class ProfileViewModel(
    private val profileRepository: ProfileRepository = ProfileRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _editState = MutableStateFlow<EditProfileState>(EditProfileState.Idle)
    val editState: StateFlow<EditProfileState> = _editState.asStateFlow()

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
            uploads = 0,
            bookmarks = 0,
            upvotes = 0,
            contributorLevel = 1,
            branch = "Computer Science",
            createdAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            _uiState.update { ProfileUiState.Loading }
            profileRepository.observeProfile(currentFirebaseUser.uid)
                .catch { throwable ->
                    // Fallback safely to current auth user details and defaults, never crash
                    _uiState.update { ProfileUiState.Success(fallbackProfile) }
                }
                .collect { profile ->
                    val baseProfile = profile ?: fallbackProfile
                    try {
                        coroutineScope {
                            val bookmarksDeferred = async {
                                try {
                                    FirebaseFirestore.getInstance().collection("bookmarks")
                                        .whereEqualTo("userId", currentFirebaseUser.uid)
                                        .get()
                                        .await()
                                        .size()
                                } catch (e: Exception) {
                                    0
                                }
                            }
                            
                            val collections = listOf("documents", "notes", "pyqs", "assignments", "cheatsheets", "videos")
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
                            
                            val updatedProfile = baseProfile.copy(
                                bookmarks = finalBookmarks,
                                upvotes = finalUpvotes
                            )
                            _uiState.update { ProfileUiState.Success(updatedProfile) }
                        }
                    } catch (e: Exception) {
                        _uiState.update { ProfileUiState.Success(baseProfile) }
                    }
                }
        }
    }

    fun updateProfile(
        name: String,
        semester: String,
        section: String,
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

                val normalizedSection = com.pravor.notessharing.util.NormalizationUtil.normalizeSection(section)

                profileRepository.updateProfileFields(
                    uid = currentFirebaseUser.uid,
                    name = name.trim(),
                    semester = semester.trim(),
                    section = normalizedSection,
                    profileImageUrl = finalImageUrl
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

package com.pravor.notessharing.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pravor.notessharing.state.AuthUiState
import com.pravor.notessharing.state.SessionState
import com.pravor.notessharing.model.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository = AuthRepository()) : ViewModel() {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Checking)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    var tempGoogleProfile: Profile? = null
        private set

    init {
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            _sessionState.update { SessionState.Checking }
            val currentUser = repository.currentUser
            if (currentUser != null) {
                val profile = repository.getUserProfile(currentUser.uid)
                if (profile != null) {
                    tempGoogleProfile = null
                    _sessionState.update { SessionState.LoggedIn }
                } else {
                    tempGoogleProfile = Profile(
                        uid = currentUser.uid,
                        name = currentUser.displayName ?: "User",
                        email = currentUser.email ?: "",
                        profileImageUrl = currentUser.photoUrl?.toString() ?: "",
                        isOnboardingRequired = true
                    )
                    _sessionState.update { SessionState.OnboardingRequired }
                }
            } else {
                tempGoogleProfile = null
                _sessionState.update { SessionState.LoggedOut }
            }
        }
    }

    fun loginWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { AuthUiState.Error("Please fill in all fields.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { AuthUiState.Loading }
            repository.emailLogin(email.trim(), password).collect { result ->
                result.onSuccess { profile ->
                    _uiState.update { AuthUiState.Success(profile) }
                    _sessionState.update { SessionState.LoggedIn }
                }.onFailure { throwable ->
                    _uiState.update { AuthUiState.Error(throwable.localizedMessage ?: "Login failed.") }
                }
            }
        }
    }

    fun signUpWithEmail(
        name: String,
        email: String,
        college: String,
        branch: String,
        semester: String,
        section: String,
        password: String,
        confirmPassword: String
    ) {
        if (name.isBlank() || email.isBlank() || college.isBlank() || branch.isBlank() || semester.isBlank() || section.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            _uiState.update { AuthUiState.Error("All fields are required.") }
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _uiState.update { AuthUiState.Error("Please enter a valid email address.") }
            return
        }
        if (password.length < 8) {
            _uiState.update { AuthUiState.Error("Password must be at least 8 characters.") }
            return
        }
        if (password != confirmPassword) {
            _uiState.update { AuthUiState.Error("Passwords do not match.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { AuthUiState.Loading }
            repository.emailSignUp(
                name = name.trim(),
                email = email.trim(),
                college = college.trim(),
                branch = branch.trim(),
                semester = semester.trim(),
                section = section.trim(),
                password = password
            ).collect { result ->
                result.onSuccess { profile ->
                    _uiState.update { AuthUiState.Success(profile) }
                    _sessionState.update { SessionState.LoggedIn }
                }.onFailure { throwable ->
                    _uiState.update { AuthUiState.Error(throwable.localizedMessage ?: "Sign up failed.") }
                }
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { AuthUiState.Loading }
            repository.googleSignIn(idToken).collect { result ->
                result.onSuccess { profile ->
                    if (profile.isOnboardingRequired) {
                        tempGoogleProfile = profile
                        _uiState.update { AuthUiState.Success(profile) }
                        _sessionState.update { SessionState.OnboardingRequired }
                    } else {
                        tempGoogleProfile = null
                        _uiState.update { AuthUiState.Success(profile) }
                        _sessionState.update { SessionState.LoggedIn }
                    }
                }.onFailure { throwable ->
                    _uiState.update { AuthUiState.Error(throwable.localizedMessage ?: "Google sign-in failed.") }
                }
            }
        }
    }

    fun completeGoogleOnboarding(
        name: String,
        college: String,
        branch: String,
        semester: String,
        section: String
    ) {
        val temp = tempGoogleProfile
        if (temp == null) {
            _uiState.update { AuthUiState.Error("Session expired. Please try signing in again.") }
            return
        }
        if (name.isBlank() || college.isBlank() || branch.isBlank() || semester.isBlank() || section.isBlank()) {
            _uiState.update { AuthUiState.Error("All fields are required.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { AuthUiState.Loading }
            try {
                val profile = Profile(
                    uid = temp.uid,
                    name = name.trim(),
                    email = temp.email,
                    college = college.trim(),
                    branch = branch.trim(),
                    semester = semester.trim(),
                    section = section.trim(),
                    profileImageUrl = temp.profileImageUrl,
                    role = "user",
                    uploads = 0,
                    bookmarks = 0,
                    upvotes = 0,
                    notesUploaded = 0,
                    contributorLevel = 1,
                    createdAt = System.currentTimeMillis()
                )
                repository.createUserProfile(profile)
                tempGoogleProfile = null
                _uiState.update { AuthUiState.Success(profile) }
                _sessionState.update { SessionState.LoggedIn }
            } catch (e: Exception) {
                _uiState.update { AuthUiState.Error(e.localizedMessage ?: "Failed to complete onboarding.") }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            tempGoogleProfile = null
            _sessionState.update { SessionState.LoggedOut }
            _uiState.update { AuthUiState.Idle }
        }
    }

    fun clearState() {
        _uiState.update { AuthUiState.Idle }
    }
}

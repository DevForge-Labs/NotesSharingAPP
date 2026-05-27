package com.pravor.notessharing.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pravor.notessharing.state.AuthUiState
import com.pravor.notessharing.state.SessionState
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

    init {
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            _sessionState.update { SessionState.Checking }
            if (repository.currentUser != null) {
                _sessionState.update { SessionState.LoggedIn }
            } else {
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

    fun signUpWithEmail(name: String, email: String, semester: String, password: String, confirmPassword: String) {
        if (name.isBlank() || email.isBlank() || semester.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
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
            repository.emailSignUp(name.trim(), email.trim(), semester, password).collect { result ->
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
                    _uiState.update { AuthUiState.Success(profile) }
                    _sessionState.update { SessionState.LoggedIn }
                }.onFailure { throwable ->
                    _uiState.update { AuthUiState.Error(throwable.localizedMessage ?: "Google sign-in failed.") }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _sessionState.update { SessionState.LoggedOut }
            _uiState.update { AuthUiState.Idle }
        }
    }

    fun clearState() {
        _uiState.update { AuthUiState.Idle }
    }
}

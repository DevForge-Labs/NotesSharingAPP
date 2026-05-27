package com.pravor.notessharing.state

import com.pravor.notessharing.model.Profile

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Success(val profile: Profile) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

enum class SessionState {
    Checking,
    LoggedIn,
    LoggedOut
}

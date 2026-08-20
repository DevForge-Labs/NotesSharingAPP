package com.pravor.notessharing.ui.features.auth

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*

import com.pravor.notessharing.domain.model.Profile

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Success(val profile: Profile) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

enum class SessionState {
    Checking,
    LoggedIn,
    LoggedOut,
    OnboardingRequired
}

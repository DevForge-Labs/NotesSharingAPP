package com.pravor.notessharing.viewmodel

import androidx.lifecycle.ViewModel
import com.pravor.notessharing.state.ProfileUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<ProfileUiState>(
        ProfileUiState.Success(DummyData.profile)
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
}

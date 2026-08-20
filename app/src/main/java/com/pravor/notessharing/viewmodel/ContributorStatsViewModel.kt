package com.pravor.notessharing.viewmodel

import com.pravor.notessharing.domain.model.*
import com.pravor.notessharing.data.repository.*
import com.pravor.notessharing.core.util.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pravor.notessharing.data.repository.ContributorStatsRepository
import com.pravor.notessharing.domain.model.Profile
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface ContributorStatsUiState {
    data object Loading : ContributorStatsUiState
    data class Success(val profile: Profile) : ContributorStatsUiState
    data object Error : ContributorStatsUiState
}

class ContributorStatsViewModel(
    private val repository: ContributorStatsRepository = ContributorStatsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ContributorStatsUiState>(ContributorStatsUiState.Loading)
    val uiState: StateFlow<ContributorStatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            viewModelScope.launch {
                _uiState.value = ContributorStatsUiState.Loading
                repository.observeStats(uid)
                    .catch {
                        _uiState.value = ContributorStatsUiState.Error
                    }
                    .collect { profile ->
                        if (profile != null) {
                            _uiState.value = ContributorStatsUiState.Success(profile)
                        } else {
                            val fallback = Profile(uid = uid)
                            _uiState.value = ContributorStatsUiState.Success(fallback)
                        }
                    }
            }
        } else {
            _uiState.value = ContributorStatsUiState.Error
        }
    }
}

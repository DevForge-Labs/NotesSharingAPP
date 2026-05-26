package com.pravor.notessharing.viewmodel

import androidx.lifecycle.ViewModel
import com.pravor.notessharing.state.AppSettingsUiState
import com.pravor.notessharing.state.ThemePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AppSettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AppSettingsUiState())
    val uiState: StateFlow<AppSettingsUiState> = _uiState.asStateFlow()

    fun setDarkMode(enabled: Boolean) {
        _uiState.update {
            it.copy(
                darkModeEnabled = enabled,
                themePreference = if (enabled) ThemePreference.Dark else ThemePreference.Light
            )
        }
    }

    fun setThemePreference(preference: ThemePreference, systemDark: Boolean) {
        _uiState.update {
            it.copy(
                themePreference = preference,
                darkModeEnabled = when (preference) {
                    ThemePreference.System -> systemDark
                    ThemePreference.Light -> false
                    ThemePreference.Dark -> true
                }
            )
        }
    }

    fun setNotifications(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }
}

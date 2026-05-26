package com.pratyush.notessharing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pratyush.notessharing.ui.navigation.NotesSharingApp
import com.pratyush.notessharing.ui.theme.NotesSharingTheme
import com.pratyush.notessharing.viewmodel.AppSettingsViewModel

class MainActivity : ComponentActivity() {
    private val appSettingsViewModel: AppSettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appSettings by appSettingsViewModel.uiState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()

            NotesSharingTheme(darkTheme = appSettings.darkModeEnabled) {
                NotesSharingApp(
                    appSettings = appSettings,
                    onDarkModeChange = appSettingsViewModel::setDarkMode,
                    onThemePreferenceChange = { preference ->
                        appSettingsViewModel.setThemePreference(preference, systemDark)
                    },
                    onNotificationsChange = appSettingsViewModel::setNotifications
                )
            }
        }
    }
}

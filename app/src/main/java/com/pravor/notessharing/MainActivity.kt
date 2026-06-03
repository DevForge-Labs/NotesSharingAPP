package com.pravor.notessharing

import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pravor.notessharing.ui.navigation.NotesSharingApp
import com.pravor.notessharing.ui.theme.NotesSharingTheme
import androidx.activity.result.contract.ActivityResultContracts
import com.pravor.notessharing.viewmodel.AppSettingsViewModel
import java.security.MessageDigest

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "MainActivity requestPermissionLauncher - POST_NOTIFICATIONS permission result: $isGranted")
    }
    private val appSettingsViewModel: AppSettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        logAppSignatures()

        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
        appSettingsViewModel.setNotifications(notificationsEnabled)

        // Notification permission request on startup directly with debug logs
        val permissionStateOnStartup = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val isSystemNotificationsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val manager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.areNotificationsEnabled()
        } else {
            true
        }
        Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "MainActivity onCreate - Permission status: $permissionStateOnStartup, App settings enabled: $notificationsEnabled, System notifications enabled: $isSystemNotificationsEnabled")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                val prompted = prefs.getBoolean("notification_permission_prompted", false)
                Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "MainActivity onCreate - POST_NOTIFICATIONS not granted. prompted flag: $prompted")
                if (!prompted) {
                    Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "MainActivity onCreate - Prompting POST_NOTIFICATIONS permission directly.")
                    prefs.edit().putBoolean("notification_permission_prompted", true).apply()
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

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
                    onNotificationsChange = { enabled ->
                        appSettingsViewModel.setNotifications(enabled)
                        getSharedPreferences("app_settings", MODE_PRIVATE)
                            .edit()
                            .putBoolean("notifications_enabled", enabled)
                            .apply()
                    }
                )
            }
        }
    }



    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "MainActivity onNewIntent: document_id=${intent.getStringExtra("document_id")}")
    }

    private fun logAppSignatures() {
        try {
            val packageName = this.packageName
            val packageManager = this.packageManager
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                packageInfo.signatures
            }

            signatures?.forEach { signature ->
                val sha1 = getSignatureHash(signature, "SHA-1")
                val sha256 = getSignatureHash(signature, "SHA-256")
            }
        } catch (e: Exception) {
            Log.e("AuthAudit", "Failed to retrieve signatures", e)
        }
    }

    private fun getSignatureHash(signature: Signature, algorithm: String): String {
        val md = MessageDigest.getInstance(algorithm)
        val digest = md.digest(signature.toByteArray())
        return digest.joinToString(":") { String.format("%02x", it) }.uppercase()
    }
}

package com.pravor.notessharing.ui.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pravor.notessharing.ui.features.profile.components.HeroNotificationsCard
import com.pravor.notessharing.ui.features.profile.components.PreferenceCategoryCard
import com.pravor.notessharing.ui.navigation.LocalBottomBarPadding

@Composable
fun NotificationPreferencesScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationPreferencesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val bottomPadding = LocalBottomBarPadding.current

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Notification Preferences",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    ) { innerPadding ->
        val statusSubtitle = if (uiState.enabledCount == 7) {
            "All notification categories enabled"
        } else {
            "${uiState.enabledCount} of 7 notification categories enabled"
        }

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 12.dp,
                bottom = 12.dp + bottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "master-card") {
                HeroNotificationsCard(
                    masterEnabled = uiState.masterEnabled,
                    statusSubtitle = statusSubtitle,
                    onMasterToggle = { viewModel.toggleMaster(it) }
                )
            }

            item(key = "category-downloads") {
                PreferenceCategoryCard(
                    title = "Downloads",
                    description = "Download progress, completion status, and download-related alerts.",
                    icon = Icons.Default.Download,
                    checked = uiState.downloads,
                    onCheckedChange = { viewModel.toggleCategory("downloads_enabled", it) },
                    enabled = uiState.masterEnabled
                )
            }

            item(key = "category-personal") {
                PreferenceCategoryCard(
                    title = "Personal Notifications",
                    description = "Updates related to your account, uploads, contributor progress, document removals, moderation actions, and contributor achievements.",
                    icon = Icons.Default.Person,
                    checked = uiState.personal,
                    onCheckedChange = { viewModel.toggleCategory("pref_notifications_personal", it) },
                    enabled = uiState.masterEnabled
                )
            }

            item(key = "category-content-alerts") {
                PreferenceCategoryCard(
                    title = "Content Alerts",
                    description = "Notifications when new notes, assignments, PYQs, cheatsheets, videos, or study resources become available for your branch and semester.",
                    icon = Icons.Default.Bookmark,
                    checked = uiState.contentAlerts,
                    onCheckedChange = { viewModel.toggleCategory("pref_notifications_content_alerts", it) },
                    enabled = uiState.masterEnabled
                )
            }

            item(key = "category-announcements") {
                PreferenceCategoryCard(
                    title = "Announcements",
                    description = "Important messages, maintenance notices, feature releases, platform updates, and official Campus Pages communications.",
                    icon = Icons.Default.Notifications,
                    checked = uiState.announcements,
                    onCheckedChange = { viewModel.toggleCategory("pref_notifications_announcements", it) },
                    enabled = uiState.masterEnabled
                )
            }

            item(key = "category-weekly-digest") {
                PreferenceCategoryCard(
                    title = "Weekly Digest",
                    description = "A weekly summary of uploads, trending resources, contributor activity, and study material relevant to your branch and semester.",
                    icon = Icons.Default.History,
                    checked = uiState.weeklyDigest,
                    onCheckedChange = { viewModel.toggleCategory("pref_notifications_weekly_digest", it) },
                    enabled = uiState.masterEnabled
                )
            }

            item(key = "category-exam-alerts") {
                PreferenceCategoryCard(
                    title = "Exam Alerts",
                    description = "Exam-season reminders, revision resources, PYQ recommendations, and important academic preparation alerts.",
                    icon = Icons.Default.Assignment,
                    checked = uiState.examAlerts,
                    onCheckedChange = { viewModel.toggleCategory("pref_notifications_exam_alerts", it) },
                    enabled = uiState.masterEnabled
                )
            }

            item(key = "category-trending") {
                PreferenceCategoryCard(
                    title = "Trending Resources",
                    description = "Notifications when highly downloaded, featured, or trending resources become available.",
                    icon = Icons.Default.TrendingUp,
                    checked = uiState.trendingResources,
                    onCheckedChange = { viewModel.toggleCategory("pref_notifications_trending_resources", it) },
                    enabled = uiState.masterEnabled
                )
            }
        }
    }

    if (uiState.showDisableConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDisableDialog() },
            title = {
                Text(
                    text = "Disable Notifications?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("You may miss important academic updates, new study materials, announcements, and account-related notifications.")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDisableMaster() }
                ) {
                    Text(
                        text = "Disable Anyway",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissDisableDialog() }
                ) {
                    Text("Keep Enabled")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}

package com.pravor.notessharing.ui.screens.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pravor.notessharing.ui.navigation.LocalBottomBarPadding
import com.pravor.notessharing.viewmodel.NotificationPreferencesViewModel

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
                    description = "Important messages, maintenance notices, feature releases, platform updates, and official NotesSharing communications.",
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

@Composable
private fun HeroNotificationsCard(
    masterEnabled: Boolean,
    statusSubtitle: String,
    onMasterToggle: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(30.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                            MaterialTheme.colorScheme.surfaceContainer,
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.20f)
                        )
                    )
                )
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Notifications",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Manage how NotesSharing keeps you informed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = statusSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = masterEnabled,
                    onCheckedChange = onMasterToggle,
                    modifier = Modifier.scale(0.9f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.8f),
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            }
        }
    }
}

@Composable
private fun PreferenceCategoryCard(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val cardAlpha = if (enabled) 1f else 0.5f
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = cardAlpha)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (enabled) 2.dp else 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (enabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.scale(0.85f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.8f),
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
        }
    }
}

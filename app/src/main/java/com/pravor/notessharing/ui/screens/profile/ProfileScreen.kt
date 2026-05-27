package com.pravor.notessharing.ui.screens.profile

import android.annotation.SuppressLint
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Link
import androidx.compose.ui.graphics.Color
import com.pravor.notessharing.model.calculateLevelProgress
import androidx.compose.ui.draw.clip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pravor.notessharing.model.Profile
import com.pravor.notessharing.state.AppSettingsUiState
import com.pravor.notessharing.state.ProfileUiState
import com.pravor.notessharing.state.ThemePreference
import com.pravor.notessharing.ui.components.AdaptiveScrollbar
import com.pravor.notessharing.ui.components.SectionHeader
import com.pravor.notessharing.ui.components.StatePanel
import com.pravor.notessharing.profile.ProfileViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@Composable
fun ProfileRoute(
    appSettings: AppSettingsUiState,
    onDarkModeChange: (Boolean) -> Unit,
    onThemePreferenceChange: (ThemePreference) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onLogoutClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(
        uiState = uiState,
        appSettings = appSettings,
        onThemePreferenceChange = onThemePreferenceChange,
        onNotificationsChange = onNotificationsChange,
        onLogoutClick = onLogoutClick,
        onEditProfileClick = onEditProfileClick
    )
}

@SuppressLint("UnusedCrossfadeTargetStateParameter")
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    appSettings: AppSettingsUiState,
    onThemePreferenceChange: (ThemePreference) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onLogoutClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profileListState = rememberLazyListState()
    val stateKey = when (uiState) {
        ProfileUiState.Loading -> "loading"
        ProfileUiState.Empty -> "empty"
        is ProfileUiState.Error -> "error"
        is ProfileUiState.Success -> "success"
    }

    Crossfade(targetState = stateKey, label = "profile-state", modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            ProfileUiState.Loading -> StatePanel("Loading profile", "Fetching your study identity", loading = true, modifier = Modifier.padding(top = 96.dp))
            ProfileUiState.Empty -> StatePanel("Profile not ready", "Your profile summary will appear here", modifier = Modifier.padding(top = 96.dp))
            is ProfileUiState.Error -> StatePanel("Profile unavailable", state.message, modifier = Modifier.padding(top = 96.dp))
            is ProfileUiState.Success -> ProfileContent(
                profile = state.profile,
                appSettings = appSettings,
                onThemePreferenceChange = onThemePreferenceChange,
                onNotificationsChange = onNotificationsChange,
                onLogoutClick = onLogoutClick,
                onEditProfileClick = onEditProfileClick,
                listState = profileListState
            )
        }
    }
}

@Composable
private fun ProfileContent(
    profile: Profile,
    appSettings: AppSettingsUiState,
    onThemePreferenceChange: (ThemePreference) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onLogoutClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    listState: LazyListState
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "profile-header", contentType = "profile-header") {
                ProfileHeaderCard(profile)
            }
            item(key = "contributor-summary", contentType = "contributor-summary") {
                ContributorCard(profile)
            }
            item(key = "stats-row", contentType = "stats") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Uploads", profile.uploads.toString(), Icons.Default.UploadFile, Modifier.weight(1f))
                    StatCard("Bookmarks", profile.bookmarks.toString(), Icons.Default.Bookmark, Modifier.weight(1f))
                    StatCard("Upvotes", profile.upvotes.toString(), Icons.Default.TrendingUp, Modifier.weight(1f))
                }
            }
            item(key = "uploads-breakdown", contentType = "uploads-breakdown") {
                UploadsBreakdownCard(profile)
            }
            item(key = "quick-actions-title", contentType = "section") {
                SectionHeader("Quick Actions")
            }
            item(key = "quick-actions", contentType = "actions") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickActionButton("My Uploads", Icons.Default.UploadFile, Modifier.weight(1f))
                        QuickActionButton("Bookmarks", Icons.Default.Bookmark, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickActionButton("History", Icons.Default.History, Modifier.weight(1f))
                        QuickActionButton("Edit Profile", Icons.Default.Edit, Modifier.weight(1f), onClick = onEditProfileClick)
                    }
                }
            }
            item(key = "settings-title", contentType = "section") {
                SectionHeader("Settings")
            }
            item(key = "settings-list", contentType = "settings") {
                SettingsList(
                    appSettings = appSettings,
                    onAppearanceClick = { showThemeDialog = true },
                    onNotificationsChange = onNotificationsChange,
                    onLogoutClick = { showLogoutDialog = true }
                )
            }
        }
        AdaptiveScrollbar(listState = listState)
    }

    if (showThemeDialog) {
        ThemeSelectorDialog(
            selectedPreference = appSettings.themePreference,
            onPreferenceSelected = {
                onThemePreferenceChange(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Log Out?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you want to sign out of your account?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogoutClick()
                    }
                ) {
                    Text("Log Out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}

@Composable
fun ProfileHeaderCard(profile: Profile) {
    PressScaleCard(
        shape = RoundedCornerShape(30.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surfaceContainer,
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f)
                        )
                    )
                )
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var isImageError by remember { mutableStateOf(false) }
            if (profile.profileImageUrl.isNotEmpty() && !isImageError) {
                AsyncImage(
                    model = profile.profileImageUrl,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .border(
                            2.dp,
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                            ),
                            CircleShape
                        ),
                    contentScale = ContentScale.Crop,
                    onError = { isImageError = true }
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.initials,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = profile.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${profile.branch} | ${profile.semester}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun ContributorCard(profile: Profile) {
    val progressInfo = calculateLevelProgress(profile.uploads)
    PressScaleCard(
        shape = RoundedCornerShape(26.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondary
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Level ${progressInfo.currentLevel} Contributor",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${profile.uploads} files uploaded | ${profile.upvotes} upvotes earned",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progressInfo.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
            Text(
                text = progressInfo.nextLevelText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun UploadsBreakdownCard(profile: Profile) {
    PressScaleCard(
        shape = RoundedCornerShape(26.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Uploads Breakdown",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StatBreakdownRow(
                    label = "PYQs Uploaded",
                    count = profile.pyqUploads,
                    icon = Icons.Default.Description,
                    tint = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                StatBreakdownRow(
                    label = "Notes Uploaded",
                    count = profile.notesUploads,
                    icon = Icons.Default.Image,
                    tint = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                StatBreakdownRow(
                    label = "Assignments Uploaded",
                    count = profile.assignmentUploads,
                    icon = Icons.Default.Assignment,
                    tint = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                StatBreakdownRow(
                    label = "Cheat Sheets Uploaded",
                    count = profile.cheatSheetUploads,
                    icon = Icons.Default.Bookmark,
                    tint = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                StatBreakdownRow(
                    label = "YouTube Resources",
                    count = profile.youtubeUploads,
                    icon = Icons.Default.Link,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StatBreakdownRow(
    label: String,
    count: Int,
    icon: ImageVector,
    tint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = count.toString(),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    PressScaleCard(
        shape = RoundedCornerShape(22.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    PressScaleCard(
        shape = RoundedCornerShape(22.dp),
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SettingsList(
    appSettings: AppSettingsUiState,
    onAppearanceClick: () -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onLogoutClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            SettingsRow(
                title = "Appearance",
                subtitle = appSettings.themePreference.label,
                icon = Icons.Default.Palette,
                onClick = onAppearanceClick
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            SettingsRow(
                title = "Notifications",
                subtitle = if (appSettings.notificationsEnabled) "Enabled" else "Disabled",
                icon = Icons.Default.Notifications,
                trailing = {
                    Switch(
                        checked = appSettings.notificationsEnabled,
                        onCheckedChange = onNotificationsChange
                    )
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            SettingsRow(
                title = "Privacy",
                subtitle = "Profile visibility",
                icon = Icons.Default.PrivacyTip,
                onClick = {}
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            SettingsRow(
                title = "About",
                subtitle = "Version 1.0",
                icon = Icons.Default.Info,
                onClick = {}
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            SettingsRow(
                title = "Log Out",
                subtitle = "Sign out of your account",
                icon = Icons.Default.ExitToApp,
                onClick = onLogoutClick
            )
        }
    }
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val background by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surfaceContainer,
        label = "settings-row-background"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ThemeSelectorDialog(
    selectedPreference: ThemePreference,
    onPreferenceSelected: (ThemePreference) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ThemePreference.entries.forEach { preference ->
                    ThemeOptionRow(
                        preference = preference,
                        selected = selectedPreference == preference,
                        onClick = { onPreferenceSelected(preference) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@Composable
private fun ThemeOptionRow(
    preference: ThemePreference,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = preference.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = when (preference) {
                    ThemePreference.System -> "Match device theme"
                    ThemePreference.Light -> "Use light theme"
                    ThemePreference.Dark -> "Use dark theme"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PressScaleCard(
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, label = "profile-card-press")

    Card(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        content = { content() }
    )
}

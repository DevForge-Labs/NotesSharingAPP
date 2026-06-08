package com.pravor.notessharing.ui.screens.profile

import android.annotation.SuppressLint
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import com.pravor.notessharing.ui.components.loading.StudyLoadingIndicator
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Info
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.ui.draw.shadow
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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.SwitchDefaults
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pravor.notessharing.model.Profile
import com.pravor.notessharing.state.AppSettingsUiState
import com.pravor.notessharing.state.ProfileUiState
import com.pravor.notessharing.state.ThemePreference
import com.pravor.notessharing.ui.components.AdaptiveScrollbar
import com.pravor.notessharing.ui.components.LiquidContributorCard
import com.pravor.notessharing.ui.components.SectionHeader
import com.pravor.notessharing.ui.components.StatePanel
import com.pravor.notessharing.profile.ProfileViewModel
import com.pravor.notessharing.ui.navigation.LocalBottomBarPadding
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@Composable
fun ProfileRoute(
    appSettings: AppSettingsUiState,
    onDarkModeChange: (Boolean) -> Unit,
    onThemePreferenceChange: (ThemePreference) -> Unit,
    onNotificationPreferencesClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onMyUploadsClick: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(
        uiState = uiState,
        appSettings = appSettings,
        onThemePreferenceChange = onThemePreferenceChange,
        onNotificationPreferencesClick = onNotificationPreferencesClick,
        onLogoutClick = onLogoutClick,
        onEditProfileClick = onEditProfileClick,
        onMyUploadsClick = onMyUploadsClick
    )
}

@SuppressLint("UnusedCrossfadeTargetStateParameter")
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    appSettings: AppSettingsUiState,
    onThemePreferenceChange: (ThemePreference) -> Unit,
    onNotificationPreferencesClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onMyUploadsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profileListState = rememberLazyListState()
    val stateKey = when (uiState) {
        ProfileUiState.Loading -> "loading"
        ProfileUiState.Empty -> "empty"
        is ProfileUiState.Error -> "error"
        is ProfileUiState.Success -> "success"
    }

    Crossfade(
        targetState = stateKey,
        animationSpec = tween(durationMillis = 280),
        label = "profile-state",
        modifier = modifier.fillMaxSize()
    ) {
        when (val state = uiState) {
            ProfileUiState.Loading -> ProfileSkeletonLoading()
            ProfileUiState.Empty -> StatePanel("Profile not ready", "Your profile summary will appear here", modifier = Modifier.padding(top = 96.dp))
            is ProfileUiState.Error -> StatePanel("Profile unavailable", state.message, modifier = Modifier.padding(top = 96.dp))
            is ProfileUiState.Success -> ProfileContent(
                profile = state.profile,
                appSettings = appSettings,
                onThemePreferenceChange = onThemePreferenceChange,
                onNotificationPreferencesClick = onNotificationPreferencesClick,
                onLogoutClick = onLogoutClick,
                onEditProfileClick = onEditProfileClick,
                onMyUploadsClick = onMyUploadsClick,
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
    onNotificationPreferencesClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onMyUploadsClick: () -> Unit,
    listState: LazyListState
) {
    val bottomPadding = LocalBottomBarPadding.current
    var isExpanded by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    var aboutExpanded by remember { mutableStateOf(false) }
    var notificationsExpanded by remember { mutableStateOf(false) }
    var editProfileExpanded by remember { mutableStateOf(false) }
    var closeExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            notificationsExpanded = true
            delay(40)
            aboutExpanded = true
            closeExpanded = true
            delay(40)
            editProfileExpanded = true
        } else {
            editProfileExpanded = false
            delay(40)
            aboutExpanded = false
            closeExpanded = false
            delay(40)
            notificationsExpanded = false
        }
    }

    // Close button animations (middle level, emerges second, collapses second)
    val closeX by animateDpAsState(
        targetValue = if (closeExpanded) 0.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "close-x"
    )
    val closeY by animateDpAsState(
        targetValue = if (closeExpanded) (-72).dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "close-y"
    )
    val closeAlpha by animateFloatAsState(
        targetValue = if (closeExpanded) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "close-alpha"
    )
    val closeScale by animateFloatAsState(
        targetValue = if (closeExpanded) 1f else 0.6f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "close-scale"
    )

    // About item animations (now middle, emerges second)
    val aboutX by animateDpAsState(
        targetValue = if (aboutExpanded) (-90).dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "about-x"
    )
    val aboutY by animateDpAsState(
        targetValue = if (aboutExpanded) (-72).dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "about-y"
    )
    val aboutAlpha by animateFloatAsState(
        targetValue = if (aboutExpanded) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "about-alpha"
    )
    val aboutScale by animateFloatAsState(
        targetValue = if (aboutExpanded) 1f else 0.6f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "about-scale"
    )

    // Notifications item animations (now bottom closest to FAB, emerges first)
    val notificationsX by animateDpAsState(
        targetValue = if (notificationsExpanded) (-72).dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "notifications-x"
    )
    val notificationsY by animateDpAsState(
        targetValue = if (notificationsExpanded) (-1).dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "notifications-y"
    )
    val notificationsAlpha by animateFloatAsState(
        targetValue = if (notificationsExpanded) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "notifications-alpha"
    )
    val notificationsScale by animateFloatAsState(
        targetValue = if (notificationsExpanded) 1f else 0.6f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "notifications-scale"
    )

    // Edit Profile item animations (top, emerges third)
    val editProfileX by animateDpAsState(
        targetValue = if (editProfileExpanded) 0.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "edit-profile-x"
    )
    val editProfileY by animateDpAsState(
        targetValue = if (editProfileExpanded) (-144).dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "edit-profile-y"
    )
    val editProfileAlpha by animateFloatAsState(
        targetValue = if (editProfileExpanded) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "edit-profile-alpha"
    )
    val editProfileScale by animateFloatAsState(
        targetValue = if (editProfileExpanded) 1f else 0.6f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "edit-profile-scale"
    )

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            state = listState,
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 14.dp + bottomPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "profile-header", contentType = "profile-header") {
                ProfileHeaderCard(profile)
            }
            item(key = "contributor-summary", contentType = "contributor-summary") {
                LiquidContributorCard(profile)
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
            item(key = "logout-card", contentType = "logout") {
                PressScaleCard(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    onClick = { showLogoutDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Log Out",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Sign out of your account",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        AdaptiveScrollbar(listState = listState)

        // Overlay Backdrop
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.36f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isExpanded = false
                    }
            )
        }

        // FAB Speed Dial Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp + bottomPadding, end = 24.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            // Settings Button (emerges with About in the space to its right, collapses with About)
            if (closeExpanded || closeAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .offset(x = closeX, y = closeY)
                        .graphicsLayer {
                            alpha = closeAlpha
                            scaleX = closeScale
                            scaleY = closeScale
                        }
                ) {
                    FloatingActionButton(
                        onClick = { isExpanded = false },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings Icon",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // About (Item 3, Middle)
            if (aboutExpanded || aboutAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .offset(x = aboutX, y = aboutY)
                        .graphicsLayer {
                            alpha = aboutAlpha
                            scaleX = aboutScale
                            scaleY = aboutScale
                        }
                ) {
                    SpeedDialItem(
                        label = "About",
                        icon = Icons.Rounded.Info,
                        onClick = {
                            // Dummy, do nothing
                        }
                    )
                }
            }

            // Notifications (Item 2, Bottom)
            if (notificationsExpanded || notificationsAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .offset(x = notificationsX, y = notificationsY)
                        .graphicsLayer {
                            alpha = notificationsAlpha
                            scaleX = notificationsScale
                            scaleY = notificationsScale
                        }
                ) {
                    SpeedDialItem(
                        label = "Notifications",
                        description = "Manage Preferences",
                        icon = Icons.Rounded.Notifications,
                        onClick = {
                            isExpanded = false
                            onNotificationPreferencesClick()
                        }
                    )
                }
            }

            // Edit Profile (Item 1, Top)
            if (editProfileExpanded || editProfileAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .offset(x = editProfileX, y = editProfileY)
                        .graphicsLayer {
                            alpha = editProfileAlpha
                            scaleX = editProfileScale
                            scaleY = editProfileScale
                        }
                ) {
                    SpeedDialItem(
                        label = "Edit Profile",
                        icon = Icons.Rounded.Edit,
                        onClick = {
                            isExpanded = false
                            onEditProfileClick()
                        }
                    )
                }
            }

            // Main FAB Settings / Close
            val rotation by animateFloatAsState(
                targetValue = if (isExpanded) 180f else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                label = "fab-rotation"
            )

            val fabColor by animateColorAsState(
                targetValue = if (isExpanded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                label = "fab-color"
            )

            FloatingActionButton(
                onClick = { isExpanded = !isExpanded },
                shape = CircleShape,
                containerColor = fabColor,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.Close else Icons.Rounded.Settings,
                    contentDescription = if (isExpanded) "Close" else "Settings",
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer(rotationZ = rotation)
                )
            }
        }
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
            val academicText = remember(profile.branch, profile.semester, profile.section) {
                val branchDisplay = com.pravor.notessharing.model.AcademicCatalog.getDisplayBranch(profile.branch)
                val semesterDisplay = profile.semester
                val sectionDisplay = profile.section
                if (sectionDisplay.isNotBlank()) {
                    "$branchDisplay | $semesterDisplay | $sectionDisplay"
                } else {
                    "$branchDisplay | $semesterDisplay"
                }
            }
            Text(
                text = academicText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
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
private fun SpeedDialItem(
    label: String,
    icon: ImageVector,
    description: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .shadow(6.dp, shape = CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.primary,
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 54.dp)
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!description.isNullOrBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
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

@Composable
fun ShimmerArea(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(0.dp)
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer-translate"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 250f, translateAnim - 250f),
        end = Offset(translateAnim, translateAnim)
    )

    Box(
        modifier = modifier
            .background(brush, shape)
    )
}

@Composable
fun ProfileSkeletonLoading(modifier: Modifier = Modifier) {
    val bottomPadding = LocalBottomBarPadding.current
    Box(modifier = modifier.fillMaxSize()) {
        // Background Skeleton List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 14.dp + bottomPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            userScrollEnabled = false // Disable scroll to keep it clean while loading
        ) {
            item {
                // 1. Profile Header Card Skeleton
                Card(
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ShimmerArea(
                            modifier = Modifier.size(86.dp),
                            shape = CircleShape
                        )
                        Spacer(Modifier.height(14.dp))
                        ShimmerArea(
                            modifier = Modifier.width(160.dp).height(24.dp),
                            shape = RoundedCornerShape(6.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        ShimmerArea(
                            modifier = Modifier.width(220.dp).height(16.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            item {
                // 2. Contributor Card Skeleton
                Card(
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                    ) {
                        ShimmerArea(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(28.dp)
                        )
                    }
                }
            }

            item {
                // 3. Stats Row Skeleton
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    repeat(3) {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f))
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 16.dp, horizontal = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                ShimmerArea(
                                    modifier = Modifier.size(22.dp),
                                    shape = CircleShape
                                )
                                ShimmerArea(
                                    modifier = Modifier.width(44.dp).height(20.dp),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                ShimmerArea(
                                    modifier = Modifier.width(60.dp).height(12.dp),
                                    shape = RoundedCornerShape(3.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                // 4. Upload Breakdown Skeleton
                Card(
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ShimmerArea(
                            modifier = Modifier.width(160.dp).height(20.dp),
                            shape = RoundedCornerShape(5.dp)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            repeat(5) { rowIndex ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ShimmerArea(
                                            modifier = Modifier.size(34.dp),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        ShimmerArea(
                                            modifier = Modifier.width(120.dp).height(16.dp),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                    }
                                    ShimmerArea(
                                        modifier = Modifier.width(24.dp).height(16.dp),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                }
                                if (rowIndex < 4) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Center primary StudyLoadingIndicator animation
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Semi-transparent overlay to ensure StudyLoadingIndicator readability and premium look
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.12f))
            )
            StudyLoadingIndicator(
                text = "Loading profile...",
                modifier = Modifier.wrapContentSize()
            )
        }
    }
}

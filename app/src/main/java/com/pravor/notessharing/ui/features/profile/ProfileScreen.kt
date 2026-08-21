package com.pravor.notessharing.ui.features.profile

import android.annotation.SuppressLint
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.pravor.notessharing.domain.model.Profile
import com.pravor.notessharing.ui.common.AppSettingsUiState
import com.pravor.notessharing.ui.common.LiquidContributorCard
import com.pravor.notessharing.ui.common.ProfileUiState
import com.pravor.notessharing.ui.common.components.StatePanel
import com.pravor.notessharing.ui.common.ThemePreference
import com.pravor.notessharing.ui.features.profile.components.PressScaleCard
import com.pravor.notessharing.ui.features.profile.components.ProfileHeaderCard
import com.pravor.notessharing.ui.features.profile.components.ProfileSkeletonLoading
import com.pravor.notessharing.ui.features.profile.components.SpeedDialItem
import com.pravor.notessharing.ui.features.profile.components.StatCard
import com.pravor.notessharing.ui.features.profile.components.UploadsBreakdownCard
import com.pravor.notessharing.ui.navigation.LocalBottomBarPadding
import kotlinx.coroutines.delay

@Composable
fun ProfileRoute(
    appSettings: AppSettingsUiState,
    onDarkModeChange: (Boolean) -> Unit,
    onThemePreferenceChange: (ThemePreference) -> Unit,
    onNotificationPreferencesClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onMyUploadsClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
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
        onMyUploadsClick = onMyUploadsClick,
        onAboutClick = onAboutClick
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
    onAboutClick: () -> Unit,
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
                resolvedCollegeName = state.resolvedCollegeName,
                resolvedBranchName = state.resolvedBranchName,
                appSettings = appSettings,
                onThemePreferenceChange = onThemePreferenceChange,
                onNotificationPreferencesClick = onNotificationPreferencesClick,
                onLogoutClick = onLogoutClick,
                onEditProfileClick = onEditProfileClick,
                onMyUploadsClick = onMyUploadsClick,
                onAboutClick = onAboutClick,
                listState = profileListState
            )
        }
    }
}

@Composable
private fun ProfileContent(
    profile: Profile,
    resolvedCollegeName: String,
    resolvedBranchName: String,
    appSettings: AppSettingsUiState,
    onThemePreferenceChange: (ThemePreference) -> Unit,
    onNotificationPreferencesClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onMyUploadsClick: () -> Unit,
    onAboutClick: () -> Unit,
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
                ProfileHeaderCard(
                    profile = profile,
                    resolvedCollegeName = resolvedCollegeName,
                    resolvedBranchName = resolvedBranchName
                )
            }
            item(key = "contributor-summary", contentType = "contributor-summary") {
                LiquidContributorCard(profile)
            }
            item(key = "stats-row", contentType = "stats") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Uploads", profile.totalUploads.toString(), Icons.Default.UploadFile, Modifier.weight(1f))
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
                            val logoutLottieCompositionResult = rememberLottieComposition(
                                LottieCompositionSpec.Asset("App_animations/log_out.json")
                            )
                            val logoutLottieComposition = logoutLottieCompositionResult.value
                            val logoutLottieProgress by animateLottieCompositionAsState(
                                composition = logoutLottieComposition,
                                iterations = LottieConstants.IterateForever
                            )

                            if (logoutLottieComposition != null) {
                                LottieAnimation(
                                    composition = logoutLottieComposition,
                                    progress = { logoutLottieProgress },
                                    modifier = Modifier.size(30.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp + bottomPadding, end = 24.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
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
                            isExpanded = false
                            onAboutClick()
                        }
                    )
                }
            }

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

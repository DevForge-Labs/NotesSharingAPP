package com.pravor.notessharing.ui.screens.upload

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pravor.notessharing.model.Profile
import com.pravor.notessharing.model.calculateLevelProgress
import com.pravor.notessharing.ui.components.LiquidContributorCard
import com.pravor.notessharing.ui.theme.ElectricBlue
import com.pravor.notessharing.viewmodel.ContributorStatsUiState
import com.pravor.notessharing.viewmodel.ContributorStatsViewModel

import com.pravor.notessharing.ui.navigation.LocalBottomBarPadding

@Composable
fun UploadSuccessRoute(
    onUploadAgain: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: ContributorStatsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Refresh statistics when the success screen is opened
    LaunchedEffect(Unit) {
        viewModel.loadStats()
    }

    UploadSuccessScreen(
        uiState = uiState,
        onUploadAgain = onUploadAgain,
        onBackClick = onBackClick
    )
}

@Composable
fun UploadSuccessScreen(
    uiState: ContributorStatsUiState,
    onUploadAgain: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val bottomPadding = LocalBottomBarPadding.current

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(start = 18.dp, end = 18.dp)
                .verticalScroll(scrollState)
                .padding(bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(14.dp))
            
            // 1. Success Animated Header
            SuccessHeader()

            Spacer(Modifier.height(8.dp))

            when (uiState) {
                is ContributorStatsUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ElectricBlue)
                    }
                }
                is ContributorStatsUiState.Success -> {
                    val profile = uiState.profile

                    // 2. Card 1 — Upload Another Resource Card
                    UploadAgainCard(onUploadAgain)

                    // 3. Card 2 — Contributor Statistics Breakdown
                    ContributorStatsCard(profile)

                    // 4. Card 3 — Contributor Progress Level Card
                    LiquidContributorCard(profile)
                }
                is ContributorStatsUiState.Error -> {
                    Text(
                        text = "Failed to load live statistics.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            }

            // Close / Go Back Button
            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(20.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = SolidColor(MaterialTheme.colorScheme.outlineVariant)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onBackground
                )
            ) {
                Text("Go to Dashboard", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SuccessHeader() {
    var checkScale by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        checkScale = 1f
    }
    
    val scaleAnim by animateFloatAsState(
        targetValue = checkScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "checkmark_bounce"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .scale(scaleAnim)
                .size(76.dp)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), CircleShape)
                .border(2.dp, ElectricBlue, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Success checkmark",
                tint = ElectricBlue,
                modifier = Modifier.size(40.dp)
            )
        }

        Text(
            text = "Upload Successful!",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Your resource is now live and accessible to the community.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
private fun UploadAgainCard(
    onUploadAgain: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Upload another resource?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Help your juniors and classmates by sharing more notes, cheat sheets, assignments, or PYQs.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onUploadAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Upload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Upload Again", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ContributorStatsCard(profile: Profile) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Contributor Statistics",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StatBreakdownRow(
                    label = "Total Uploads",
                    count = profile.totalUploads,
                    icon = Icons.Default.UploadFile,
                    tint = ElectricBlue
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                StatBreakdownRow(
                    label = "PYQs Uploaded",
                    count = profile.pyqUploads,
                    icon = Icons.Default.Description,
                    tint = ElectricBlue
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                StatBreakdownRow(
                    label = "Notes Uploaded",
                    count = profile.notesUploads,
                    icon = Icons.Default.Image,
                    tint = ElectricBlue
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                StatBreakdownRow(
                    label = "Assignments Uploaded",
                    count = profile.assignmentUploads,
                    icon = Icons.Default.Assignment,
                    tint = ElectricBlue
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                StatBreakdownRow(
                    label = "Cheat Sheets Uploaded",
                    count = profile.cheatSheetUploads,
                    icon = Icons.Default.Bookmark,
                    tint = ElectricBlue
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                StatBreakdownRow(
                    label = "YouTube Resources",
                    count = profile.youtubeResourceUploads,
                    icon = Icons.Default.Link,
                    tint = ElectricBlue
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



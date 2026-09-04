package com.pravor.notessharing.ui.features.classroom

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pravor.notessharing.data.classroom.MarkExternalAssignmentResult
import com.pravor.notessharing.domain.model.classroom.ClassroomAttachment
import com.pravor.notessharing.domain.model.classroom.ClassroomStudentSubmission
import com.pravor.notessharing.domain.model.classroom.ClassroomUpcomingAssignment
import com.pravor.notessharing.ui.common.CustomPullRefreshIndicator
import com.pravor.notessharing.ui.features.classroom.components.ClassroomCourseWorkCard
import com.pravor.notessharing.ui.features.classroom.components.ClassroomSubmissionBottomSheet
import com.pravor.notessharing.ui.navigation.LocalBottomBarPadding
import com.pravor.notessharing.ui.theme.ElectricBlue
import com.pravor.notessharing.ui.theme.Mint

@Composable
fun ClassroomUpcomingRoute(
    onBackClick: () -> Unit,
    viewModel: ClassroomUpcomingViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var pendingAssignmentForConsent by remember { mutableStateOf<ClassroomUpcomingAssignment?>(null) }
    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val pending = pendingAssignmentForConsent
            if (pending != null) {
                viewModel.markExternalAssignmentDone(pending.courseId, pending.courseWork)
            }
        } else {
            Toast.makeText(context, "Permission consent was not completed.", Toast.LENGTH_SHORT).show()
        }
        pendingAssignmentForConsent = null
    }

    var explainingAssignment by remember { mutableStateOf<ClassroomUpcomingAssignment?>(null) }
    var undoingAssignment by remember { mutableStateOf<ClassroomUpcomingAssignment?>(null) }

    ClassroomUpcomingScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onRefresh = viewModel::refresh,
        onSubmissionSuccess = viewModel::onSubmissionCompleted,
        onAttachmentClick = { attachment ->
            viewModel.handleAttachmentClick(context, attachment)
        },
        onOpenExternalTaskClick = { _, url ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
            }
        },
        onMarkAsDoneClick = { item ->
            viewModel.markExternalAssignmentDone(
                courseId = item.courseId,
                courseWork = item.courseWork,
                onConsentRequired = { recoveryIntent ->
                    pendingAssignmentForConsent = item
                    consentLauncher.launch(recoveryIntent)
                },
                onResult = { result ->
                    when (result) {
                        is MarkExternalAssignmentResult.TurnedIn -> {
                            Toast.makeText(context, "Turned in on Google Classroom!", Toast.LENGTH_SHORT).show()
                        }
                        is MarkExternalAssignmentResult.ProjectPermissionDenied -> {
                            explainingAssignment = item
                        }
                        is MarkExternalAssignmentResult.AuthenticationError -> {
                            Toast.makeText(context, "Authentication required. Please reconnect Classroom.", Toast.LENGTH_LONG).show()
                        }
                        is MarkExternalAssignmentResult.NetworkError -> {
                            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                        }
                        is MarkExternalAssignmentResult.Error -> {
                            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                }
            )
        },
        onDoneStatusClick = { item ->
            undoingAssignment = item
        }
    )

    // Explanation Dialog
    if (explainingAssignment != null) {
        val target = explainingAssignment!!
        val classroomUrl = target.courseWork.alternateLink
            ?: target.submission?.alternateLink
            ?: "https://classroom.google.com"

        AlertDialog(
            onDismissRequest = { explainingAssignment = null },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color(0xFF141920),
            tonalElevation = 6.dp,
            icon = {
                Surface(
                    shape = CircleShape,
                    color = ElectricBlue.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "Mark as Done",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Google Classroom isn't allowing Campus Pages to mark this assignment as officially turned in.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    Text(
                        text = "You can mark it as Done in Campus Pages to keep track of your progress, or open Google Classroom to complete the official submission there.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            explainingAssignment = null
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(classroomUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open Google Classroom", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricBlue,
                            contentColor = Color(0xFF07121E)
                        ),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Open Google Classroom",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val item = target
                            explainingAssignment = null
                            viewModel.confirmLocalDone(item.courseId, item.courseWork)
                            Toast.makeText(context, "Marked as Done in Campus Pages", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Mint,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Mark as Done Here",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    TextButton(
                        onClick = { explainingAssignment = null },
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        )
    }

    // Undo Confirmation Dialog
    if (undoingAssignment != null) {
        val target = undoingAssignment!!
        AlertDialog(
            onDismissRequest = { undoingAssignment = null },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color(0xFF141920),
            tonalElevation = 6.dp,
            icon = {
                Surface(
                    shape = CircleShape,
                    color = Mint.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Mint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "Assignment marked as done",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "This assignment is marked as Done only in Campus Pages. Google Classroom's official submission status is separate.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val item = target
                            undoingAssignment = null
                            viewModel.undoExternalAssignmentDone(item.courseId, item.courseWork)
                            Toast.makeText(context, "Marked as Not Done", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text(
                            text = "Mark as Not Done",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    TextButton(
                        onClick = { undoingAssignment = null },
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassroomUpcomingScreen(
    uiState: ClassroomUpcomingUiState,
    onBackClick: () -> Unit,
    onRefresh: () -> Unit,
    onSubmissionSuccess: (ClassroomStudentSubmission) -> Unit,
    onAttachmentClick: (ClassroomAttachment) -> Unit,
    onOpenExternalTaskClick: ((ClassroomUpcomingAssignment, String) -> Unit)? = null,
    onMarkAsDoneClick: ((ClassroomUpcomingAssignment) -> Unit)? = null,
    onDoneStatusClick: ((ClassroomUpcomingAssignment) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bottomPadding = LocalBottomBarPadding.current
    val pullToRefreshState = rememberPullToRefreshState()
    var selectedAssignmentForSubmission by remember { mutableStateOf<ClassroomUpcomingAssignment?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Upcoming Assignments",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            state = pullToRefreshState,
            indicator = {
                CustomPullRefreshIndicator(
                    state = pullToRefreshState,
                    isRefreshing = uiState.isRefreshing,
                    restingOffset = 64.dp,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading && uiState.assignments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = ElectricBlue)
                        Text(
                            text = "Loading upcoming assignments...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (uiState.assignments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Mint.copy(alpha = 0.15f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Mint,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Text(
                            text = "You're all caught up!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "There are no upcoming assignments from your visible classes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 18.dp,
                        end = 18.dp,
                        top = 12.dp,
                        bottom = 24.dp + bottomPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(key = "upcoming-header") {
                        val count = uiState.assignments.size
                        val countText = if (count == 1) "1 assignment remaining" else "$count assignments remaining"
                        Text(
                            text = countText,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    items(uiState.assignments, key = { "${it.courseId}_${it.courseWork.id}" }) { item ->
                        ClassroomCourseWorkCard(
                            courseWork = item.courseWork,
                            courseName = item.courseName,
                            submissionState = item.submission?.state,
                            isLocallyDone = item.isLocallyDone,
                            isMarkingDone = uiState.markingDoneIds.contains(item.courseWork.id),
                            onAttachmentClick = onAttachmentClick,
                            onSubmitClick = {
                                selectedAssignmentForSubmission = item
                                com.pravor.notessharing.core.analytics.AnalyticsManager.logClassroomSubmissionOpened(
                                    courseId = item.courseId,
                                    workId = item.courseWork.id
                                )
                            },
                            onOpenExternalTaskClick = { cw, url ->
                                onOpenExternalTaskClick?.invoke(item, url)
                            },
                            onMarkAsDoneClick = {
                                onMarkAsDoneClick?.invoke(item)
                            },
                            onDoneStatusClick = {
                                onDoneStatusClick?.invoke(item)
                            }
                        )
                    }
                }
            }
        }

        // Submission Bottom Sheet
        val activeAssignment = selectedAssignmentForSubmission
        if (activeAssignment != null) {
            ClassroomSubmissionBottomSheet(
                courseId = activeAssignment.courseId,
                courseWork = activeAssignment.courseWork,
                onDismiss = { selectedAssignmentForSubmission = null },
                onAttachmentClick = onAttachmentClick,
                onSubmissionSuccess = onSubmissionSuccess
            )
        }
    }
}

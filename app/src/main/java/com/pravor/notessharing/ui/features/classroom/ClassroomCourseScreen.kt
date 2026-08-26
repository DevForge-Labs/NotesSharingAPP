package com.pravor.notessharing.ui.features.classroom

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pravor.notessharing.data.classroom.MarkExternalAssignmentResult
import com.pravor.notessharing.domain.model.classroom.ClassroomAttachment
import com.pravor.notessharing.domain.model.classroom.ClassroomCourse
import com.pravor.notessharing.domain.model.classroom.ClassroomCourseWork
import com.pravor.notessharing.domain.model.classroom.ClassroomStudentSubmission
import com.pravor.notessharing.ui.common.components.SectionHeader
import com.pravor.notessharing.ui.features.classroom.components.ClassroomAnnouncementCard
import com.pravor.notessharing.ui.features.classroom.components.ClassroomCourseWorkCard
import com.pravor.notessharing.ui.features.classroom.components.ClassroomMaterialCard
import com.pravor.notessharing.ui.features.classroom.components.ClassroomSubmissionBottomSheet
import com.pravor.notessharing.ui.navigation.LocalBottomBarPadding
import com.pravor.notessharing.ui.theme.ElectricBlue

import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.unit.sp
import com.pravor.notessharing.ui.theme.Mint

@Composable
fun ClassroomCourseRoute(
    courseId: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application

    val viewModel: ClassroomCourseViewModel = viewModel(
        key = "course_$courseId",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ClassroomCourseViewModel(application, courseId) as T
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var pendingCourseWorkForConsent by remember { mutableStateOf<ClassroomCourseWork?>(null) }
    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val pending = pendingCourseWorkForConsent
            if (pending != null) {
                viewModel.markExternalAssignmentDone(pending)
            }
        } else {
            Toast.makeText(context, "Permission consent was not completed.", Toast.LENGTH_SHORT).show()
        }
        pendingCourseWorkForConsent = null
    }

    var explainingCourseWork by remember { mutableStateOf<ClassroomCourseWork?>(null) }
    var undoingCourseWork by remember { mutableStateOf<ClassroomCourseWork?>(null) }

    ClassroomCourseScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onRefresh = { viewModel.syncCourseContent(isPullToRefresh = true) },
        onRetry = { viewModel.syncCourseContent(isPullToRefresh = false) },
        onFilterSelected = viewModel::selectFilter,
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
        onMarkAsDoneClick = { courseWork ->
            viewModel.markExternalAssignmentDone(
                courseWork = courseWork,
                onConsentRequired = { recoveryIntent ->
                    pendingCourseWorkForConsent = courseWork
                    consentLauncher.launch(recoveryIntent)
                },
                onResult = { result ->
                    when (result) {
                        is MarkExternalAssignmentResult.TurnedIn -> {
                            Toast.makeText(context, "Turned in on Google Classroom!", Toast.LENGTH_SHORT).show()
                        }
                        is MarkExternalAssignmentResult.ProjectPermissionDenied -> {
                            // Show explanation dialog every time user taps Mark as Done when API restricts turnIn
                            explainingCourseWork = courseWork
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
        onDoneStatusClick = { courseWork ->
            undoingCourseWork = courseWork
        }
    )

    // 1. Mark as Done Explanation Dialog (Google Developer-Project Limitation)
    if (explainingCourseWork != null) {
        val targetCourseWork = explainingCourseWork!!
        val classroomUrl = targetCourseWork.alternateLink
            ?: uiState.submissions[targetCourseWork.id]?.alternateLink
            ?: "https://classroom.google.com"

        AlertDialog(
            onDismissRequest = { explainingCourseWork = null },
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
                            explainingCourseWork = null
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
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
                            val cw = targetCourseWork
                            explainingCourseWork = null
                            viewModel.confirmLocalDone(cw)
                            Toast.makeText(context, "Marked as Done in Campus Pages", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
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
                        onClick = { explainingCourseWork = null },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            dismissButton = null
        )
    }

    // 2. Undo Confirmation Dialog (When user taps an already-done status card)
    if (undoingCourseWork != null) {
        val targetCourseWork = undoingCourseWork!!
        AlertDialog(
            onDismissRequest = { undoingCourseWork = null },
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
                            val cw = targetCourseWork
                            undoingCourseWork = null
                            viewModel.undoExternalAssignmentDone(cw)
                            Toast.makeText(context, "Marked as Not Done", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(
                            text = "Mark as Not Done",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    TextButton(
                        onClick = { undoingCourseWork = null },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            dismissButton = null
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassroomCourseScreen(
    uiState: ClassroomCourseUiState,
    onBackClick: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onFilterSelected: (ClassroomContentFilter) -> Unit,
    onSubmissionSuccess: (ClassroomStudentSubmission) -> Unit,
    onAttachmentClick: (ClassroomAttachment) -> Unit,
    onOpenExternalTaskClick: ((ClassroomCourseWork, String) -> Unit)? = null,
    onMarkAsDoneClick: ((ClassroomCourseWork) -> Unit)? = null,
    onDoneStatusClick: ((ClassroomCourseWork) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bottomPadding = LocalBottomBarPadding.current
    val courseName = uiState.course?.name ?: "Course Details"
    var selectedCourseWorkForSubmission by remember { mutableStateOf<ClassroomCourseWork?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = courseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                actions = {
                    if (uiState.isRefreshing) {
                        CircularProgressIndicator(
                            color = ElectricBlue,
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(20.dp)
                        )
                    } else {
                        IconButton(onClick = onRefresh) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (uiState.isLoading && uiState.course == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = ElectricBlue)
                    Text(
                        text = "Loading course resources...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = 8.dp,
                    bottom = 16.dp + bottomPadding
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Course Header Card
                item(key = "course-header") {
                    CourseHeaderBanner(course = uiState.course)
                }

                // 2. Filter Bar (Single Selection Pill Chips)
                item(key = "content-filter-bar") {
                    ClassroomContentFilterRow(
                        selectedFilter = uiState.selectedFilter,
                        onFilterSelected = onFilterSelected
                    )
                }

                // Error Banner if present
                if (uiState.errorMessage != null) {
                    item(key = "error-banner") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = uiState.errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = onRetry,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                                ) {
                                    Text(
                                        text = "Retry",
                                        color = Color(0xFF07121E),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Announcements Section (Shown when Filter is ALL or ANNOUNCEMENTS)
                if (uiState.selectedFilter == ClassroomContentFilter.ALL ||
                    uiState.selectedFilter == ClassroomContentFilter.ANNOUNCEMENTS
                ) {
                    item(key = "announcements-header") {
                        SectionHeader("Announcements")
                    }

                    if (uiState.announcements.isEmpty()) {
                        item(key = "announcements-empty") {
                            EmptySectionCard(
                                title = "No announcements yet",
                                message = "Important updates posted by instructors will appear here."
                            )
                        }
                    } else {
                        items(uiState.announcements, key = { "announcement_${it.id}" }) { announcement ->
                            ClassroomAnnouncementCard(
                                announcement = announcement,
                                onAttachmentClick = onAttachmentClick
                            )
                        }
                    }
                }

                // 4. Materials Section (Shown when Filter is ALL or MATERIALS)
                if (uiState.selectedFilter == ClassroomContentFilter.ALL ||
                    uiState.selectedFilter == ClassroomContentFilter.MATERIALS
                ) {
                    item(key = "materials-header") {
                        SectionHeader("Materials & Notes")
                    }

                    if (uiState.materials.isEmpty()) {
                        item(key = "materials-empty") {
                            EmptySectionCard(
                                title = "No materials shared yet",
                                message = "Lecture notes, PDFs, and learning resources will appear here."
                            )
                        }
                    } else {
                        items(uiState.materials, key = { "material_${it.id}" }) { material ->
                            ClassroomMaterialCard(
                                material = material,
                                onAttachmentClick = onAttachmentClick
                            )
                        }
                    }
                }

                // 5. Classwork Section (Shown when Filter is ALL or ASSIGNMENTS)
                if (uiState.selectedFilter == ClassroomContentFilter.ALL ||
                    uiState.selectedFilter == ClassroomContentFilter.ASSIGNMENTS
                ) {
                    item(key = "classwork-header") {
                        SectionHeader("Classwork & Assignments")
                    }

                    if (uiState.coursework.isEmpty()) {
                        item(key = "classwork-empty") {
                            EmptySectionCard(
                                title = "No classwork posted yet",
                                message = "Assignments and coursework will appear here."
                            )
                        }
                    } else {
                        items(uiState.coursework, key = { "coursework_${it.id}" }) { item ->
                            val currentSubState = uiState.submissions[item.id]?.state
                            val isLocallyDone = uiState.manualCompletions.contains(item.id)
                            val isMarkingDone = uiState.markingDoneIds.contains(item.id)
                            ClassroomCourseWorkCard(
                                courseWork = item,
                                onAttachmentClick = onAttachmentClick,
                                submissionState = currentSubState,
                                isLocallyDone = isLocallyDone,
                                isMarkingDone = isMarkingDone,
                                onSubmitClick = { selectedCourseWorkForSubmission = item },
                                onOpenExternalTaskClick = onOpenExternalTaskClick,
                                onMarkAsDoneClick = onMarkAsDoneClick,
                                onDoneStatusClick = onDoneStatusClick
                            )
                        }
                    }
                }
            }
        }

        // Submission Bottom Sheet
        val currentCourse = uiState.course
        val activeCourseWork = selectedCourseWorkForSubmission
        if (currentCourse != null && activeCourseWork != null) {
            ClassroomSubmissionBottomSheet(
                courseId = currentCourse.id,
                courseWork = activeCourseWork,
                onDismiss = { selectedCourseWorkForSubmission = null },
                onAttachmentClick = onAttachmentClick,
                onSubmissionSuccess = onSubmissionSuccess
            )
        }
    }
}

@Composable
private fun ClassroomContentFilterRow(
    selectedFilter: ClassroomContentFilter,
    onFilterSelected: (ClassroomContentFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ClassroomContentFilter.values().forEach { filter ->
            val isSelected = filter == selectedFilter
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) ElectricBlue else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier.clickable { onFilterSelected(filter) }
            ) {
                Text(
                    text = filter.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color(0xFF07121E) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun CourseHeaderBanner(course: ClassroomCourse?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF131F2A), Color(0xFF0C141B))
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = ElectricBlue.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.3f)),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Class,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = course?.name ?: "Classroom Course",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    val subtitle = listOfNotNull(
                        course?.section?.takeIf { it.isNotBlank() },
                        course?.descriptionHeading?.takeIf { it.isNotBlank() }
                    ).joinToString(" • ")

                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!course?.room.isNullOrBlank() || !course?.description.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                if (!course?.room.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Room ${course?.room}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
                if (!course?.description.isNullOrBlank()) {
                    Text(
                        text = course?.description.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptySectionCard(
    title: String,
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

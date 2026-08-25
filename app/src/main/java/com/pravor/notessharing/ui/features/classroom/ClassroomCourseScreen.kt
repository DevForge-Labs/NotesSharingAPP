package com.pravor.notessharing.ui.features.classroom

import android.app.Application
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

    ClassroomCourseScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onRefresh = { viewModel.syncCourseContent(isPullToRefresh = true) },
        onRetry = { viewModel.syncCourseContent(isPullToRefresh = false) },
        onFilterSelected = viewModel::selectFilter,
        onSubmissionSuccess = viewModel::onSubmissionCompleted,
        onAttachmentClick = { attachment ->
            viewModel.handleAttachmentClick(context, attachment)
        }
    )
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
                            ClassroomCourseWorkCard(
                                courseWork = item,
                                onAttachmentClick = onAttachmentClick,
                                submissionState = currentSubState,
                                onSubmitClick = { selectedCourseWorkForSubmission = item }
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

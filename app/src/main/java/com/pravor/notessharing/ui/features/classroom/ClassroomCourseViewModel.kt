package com.pravor.notessharing.ui.features.classroom

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pravor.notessharing.data.classroom.ClassroomAuthException
import com.pravor.notessharing.data.classroom.ClassroomForbiddenException
import com.pravor.notessharing.data.classroom.ClassroomNetworkException
import com.pravor.notessharing.data.classroom.ClassroomRepository
import com.pravor.notessharing.data.classroom.ClassroomSubmissionRepository
import com.pravor.notessharing.domain.model.classroom.AttachmentType
import com.pravor.notessharing.domain.model.classroom.ClassroomAnnouncement
import com.pravor.notessharing.domain.model.classroom.ClassroomAttachment
import com.pravor.notessharing.domain.model.classroom.ClassroomCourse
import com.pravor.notessharing.domain.model.classroom.ClassroomCourseWork
import com.pravor.notessharing.domain.model.classroom.ClassroomMaterial
import com.pravor.notessharing.domain.model.classroom.ClassroomStudentSubmission
import com.pravor.notessharing.ui.features.classroom.components.CourseCardTheme
import com.pravor.notessharing.ui.features.classroom.components.CoursePalettes
import com.pravor.notessharing.ui.features.classroom.components.getCourseTheme
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class CourseResources(
    val materials: List<ClassroomMaterial>,
    val announcements: List<ClassroomAnnouncement>,
    val coursework: List<ClassroomCourseWork>,
    val submissions: Map<String, ClassroomStudentSubmission>,
    val manualCompletions: Set<String>
)

class ClassroomCourseViewModel(
    application: Application,
    private val courseId: String
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ClassroomCourseVM"
    }

    private val repository = ClassroomRepository.getInstance(application)
    private val submissionRepository = ClassroomSubmissionRepository.getInstance(application)

    private val _course = MutableStateFlow<ClassroomCourse?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _isRefreshing = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _selectedFilter = MutableStateFlow(ClassroomContentFilter.ALL)
    private val _markingDoneIds = MutableStateFlow<Set<String>>(emptySet())

    private val courseAndIndexFlow = combine(
        repository.observeCourses(),
        repository.observeHiddenCourseIds()
    ) { courses, hiddenIds ->
        val visible = courses.filter { it.id !in hiddenIds }
        val index = visible.indexOfFirst { it.id == courseId }
        val course = visible.find { it.id == courseId } ?: courses.find { it.id == courseId }
        Pair(course, index)
    }

    private val resourcesFlow = combine(
        repository.observeMaterials(courseId),
        repository.observeAnnouncements(courseId),
        repository.observeCourseWork(courseId),
        repository.observeSubmissions(courseId),
        repository.observeManualCompletions(courseId)
    ) { materials, announcements, coursework, submissions, manualCompletions ->
        CourseResources(materials, announcements, coursework, submissions, manualCompletions)
    }

    val uiState: StateFlow<ClassroomCourseUiState> = combine(
        _course,
        courseAndIndexFlow,
        resourcesFlow,
        combine(_markingDoneIds, _selectedFilter) { markingIds, filter -> Pair(markingIds, filter) },
        combine(_isLoading, _isRefreshing, _errorMessage) { loading, refreshing, error ->
            Triple(loading, refreshing, error)
        }
    ) { directCourse, (observedCourse, courseIndex), resources, controlState, status ->
        val finalCourse = directCourse ?: observedCourse
        val theme = if (finalCourse != null) {
            getCourseTheme(finalCourse, courseIndex)
        } else {
            CoursePalettes[0]
        }
        val (materials, announcements, coursework, submissions, manualCompletions) = resources
        val (markingDoneIds, filter) = controlState
        val (isLoading, isRefreshing, error) = status

        ClassroomCourseUiState(
            course = finalCourse,
            courseTheme = theme,
            materials = materials,
            announcements = announcements,
            coursework = coursework,
            submissions = submissions,
            manualCompletions = manualCompletions,
            markingDoneIds = markingDoneIds,
            selectedFilter = filter,
            isLoading = isLoading && materials.isEmpty() && announcements.isEmpty() && coursework.isEmpty() && finalCourse == null,
            isRefreshing = isRefreshing,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ClassroomCourseUiState()
    )

    init {
        loadCachedCourseAndSync()
    }

    private fun loadCachedCourseAndSync() {
        viewModelScope.launch {
            _course.value = repository.getCourse(courseId)
            syncCourseContent(isPullToRefresh = false)
        }
    }

    fun selectFilter(filter: ClassroomContentFilter) {
        _selectedFilter.value = filter
    }

    fun onSubmissionCompleted(submission: ClassroomStudentSubmission) {
        viewModelScope.launch {
            repository.saveSubmission(submission)
        }
    }

    fun markExternalAssignmentDone(
        courseWork: ClassroomCourseWork,
        onConsentRequired: ((android.content.Intent) -> Unit)? = null,
        onResult: ((com.pravor.notessharing.data.classroom.MarkExternalAssignmentResult) -> Unit)? = null
    ) {
        if (_markingDoneIds.value.contains(courseWork.id)) return
        viewModelScope.launch {
            _markingDoneIds.update { it + courseWork.id }
            val result = submissionRepository.turnInExternalAssignment(courseId, courseWork.id)
            _markingDoneIds.update { it - courseWork.id }

            when (result) {
                is com.pravor.notessharing.data.classroom.MarkExternalAssignmentResult.ConsentRequired -> {
                    onConsentRequired?.invoke(result.recoveryIntent)
                }
                else -> {
                    onResult?.invoke(result)
                }
            }
        }
    }

    fun confirmLocalDone(courseWork: ClassroomCourseWork) {
        viewModelScope.launch {
            repository.saveManualCompletion(courseId, courseWork.id, completed = true)
        }
    }

    fun undoExternalAssignmentDone(courseWork: ClassroomCourseWork) {
        viewModelScope.launch {
            repository.saveManualCompletion(courseId, courseWork.id, completed = false)
        }
    }

    fun syncCourseContent(isPullToRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isPullToRefresh) {
                _isRefreshing.value = true
            } else {
                _isLoading.value = true
            }
            _errorMessage.value = null

            try {
                val courseDeferred = async { repository.getCourse(courseId) }
                val materialsDeferred = async { repository.syncMaterials(courseId, force = isPullToRefresh) }
                val announcementsDeferred = async { repository.syncAnnouncements(courseId, force = isPullToRefresh) }
                val courseworkDeferred = async { repository.syncCourseWork(courseId, force = isPullToRefresh) }

                val course = courseDeferred.await()
                val matResult = materialsDeferred.await()
                val annResult = announcementsDeferred.await()
                val cwResult = courseworkDeferred.await()

                if (course != null) {
                    _course.value = course
                }

                // Concurrently fetch student submissions for coursework & persist to Room DB
                val courseworkList = cwResult.getOrNull().orEmpty()
                if (courseworkList.isNotEmpty()) {
                    try {
                        val subList = courseworkList.map { cw ->
                            async {
                                submissionRepository.getSubmission(courseId, cw.id).getOrNull()
                            }
                        }.awaitAll().filterNotNull()

                        if (subList.isNotEmpty()) {
                            repository.saveSubmissions(courseId, subList)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed syncing submission states to Room: ${e.message}")
                    }
                }

                val error = matResult.exceptionOrNull()
                    ?: annResult.exceptionOrNull()
                    ?: cwResult.exceptionOrNull()

                if (error != null) {
                    Log.e(TAG, "Sync error in course $courseId", error)
                    _errorMessage.value = when (error) {
                        is ClassroomAuthException -> "Classroom authorization expired. Please reconnect."
                        is ClassroomForbiddenException -> "Access denied to course materials."
                        is ClassroomNetworkException -> "Network error. Showing cached content."
                        else -> error.message ?: "Unable to sync latest course content."
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error syncing course $courseId", e)
                _errorMessage.value = e.message ?: "An unexpected error occurred."
            }

            _isLoading.value = false
            _isRefreshing.value = false
        }
    }

    fun handleAttachmentClick(context: Context, attachment: ClassroomAttachment) {
        val url = attachment.linkUrl
        if (url.isBlank()) {
            Toast.makeText(context, "Attachment link is unavailable.", Toast.LENGTH_SHORT).show()
            return
        }

        val toastMessage = when {
            attachment.type == AttachmentType.YOUTUBE || url.contains("youtube.com") || url.contains("youtu.be") ->
                "Opening YouTube..."
            url.contains("drive.google.com") || url.contains("docs.google.com") || attachment.type == AttachmentType.DRIVE_FILE ->
                "Opening in Google Drive..."
            else ->
                "Opening link..."
        }

        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot launch attachment URL: $url", e)
            Toast.makeText(context, "No app found to open this resource.", Toast.LENGTH_SHORT).show()
        }
    }
}

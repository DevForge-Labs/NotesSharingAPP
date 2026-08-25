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
import com.pravor.notessharing.domain.model.classroom.AttachmentType
import com.pravor.notessharing.domain.model.classroom.ClassroomAttachment
import com.pravor.notessharing.domain.model.classroom.ClassroomCourse
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClassroomCourseViewModel(
    application: Application,
    private val courseId: String
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ClassroomCourseVM"
    }

    private val repository = ClassroomRepository.getInstance(application)

    private val _course = MutableStateFlow<ClassroomCourse?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _isRefreshing = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val resourcesFlow = combine(
        repository.observeMaterials(courseId),
        repository.observeAnnouncements(courseId),
        repository.observeCourseWork(courseId)
    ) { materials, announcements, coursework ->
        Triple(materials, announcements, coursework)
    }

    val uiState: StateFlow<ClassroomCourseUiState> = combine(
        _course,
        resourcesFlow,
        _isLoading,
        _isRefreshing,
        _errorMessage
    ) { course, resources, isLoading, isRefreshing, error ->
        val (materials, announcements, coursework) = resources
        ClassroomCourseUiState(
            course = course,
            materials = materials,
            announcements = announcements,
            coursework = coursework,
            isLoading = isLoading && materials.isEmpty() && announcements.isEmpty() && coursework.isEmpty() && course == null,
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

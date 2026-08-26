package com.pravor.notessharing.ui.features.classroom

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pravor.notessharing.data.classroom.ClassroomRepository
import com.pravor.notessharing.data.classroom.ClassroomSubmissionRepository
import com.pravor.notessharing.data.classroom.MarkExternalAssignmentResult
import com.pravor.notessharing.domain.model.classroom.AttachmentType
import com.pravor.notessharing.domain.model.classroom.ClassroomAttachment
import com.pravor.notessharing.domain.model.classroom.ClassroomCourseWork
import com.pravor.notessharing.domain.model.classroom.ClassroomStudentSubmission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClassroomUpcomingViewModel(
    application: Application
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ClassroomUpcomingVM"
    }

    private val repository = ClassroomRepository.getInstance(application)
    private val submissionRepository = ClassroomSubmissionRepository.getInstance(application)

    private val _isRefreshing = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _markingDoneIds = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<ClassroomUpcomingUiState> = combine(
        repository.observeUpcomingAssignments(),
        _markingDoneIds,
        combine(_isLoading, _isRefreshing, _errorMessage) { loading, refreshing, error ->
            Triple(loading, refreshing, error)
        }
    ) { assignments, markingIds, (loading, refreshing, error) ->
        ClassroomUpcomingUiState(
            assignments = assignments,
            isLoading = loading && assignments.isEmpty(),
            isRefreshing = refreshing,
            markingDoneIds = markingIds,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ClassroomUpcomingUiState(isLoading = true)
    )

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.syncCourses(force = true)
            } catch (e: Exception) {
                Log.w(TAG, "Refresh failed: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun markExternalAssignmentDone(
        courseId: String,
        courseWork: ClassroomCourseWork,
        onConsentRequired: ((Intent) -> Unit)? = null,
        onResult: ((MarkExternalAssignmentResult) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _markingDoneIds.value = _markingDoneIds.value + courseWork.id
            val result = submissionRepository.turnInExternalAssignment(
                courseId = courseId,
                courseWorkId = courseWork.id
            )
            _markingDoneIds.value = _markingDoneIds.value - courseWork.id

            when (result) {
                is MarkExternalAssignmentResult.ConsentRequired -> {
                    onConsentRequired?.invoke(result.recoveryIntent)
                }
                else -> {
                    onResult?.invoke(result)
                }
            }
        }
    }

    fun confirmLocalDone(courseId: String, courseWork: ClassroomCourseWork) {
        viewModelScope.launch {
            repository.saveManualCompletion(courseId, courseWork.id, completed = true)
        }
    }

    fun undoExternalAssignmentDone(courseId: String, courseWork: ClassroomCourseWork) {
        viewModelScope.launch {
            repository.saveManualCompletion(courseId, courseWork.id, completed = false)
        }
    }

    fun onSubmissionCompleted(submission: ClassroomStudentSubmission) {
        viewModelScope.launch {
            repository.saveSubmission(submission)
            repository.deleteManualCompletion(submission.courseWorkId)
        }
    }

    fun handleAttachmentClick(context: Context, attachment: ClassroomAttachment) {
        when (attachment.type) {
            AttachmentType.DRIVE_FILE -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(attachment.linkUrl))
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not open attachment", Toast.LENGTH_SHORT).show()
                }
            }
            AttachmentType.YOUTUBE, AttachmentType.LINK, AttachmentType.FORM -> {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(attachment.linkUrl))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(attachment.linkUrl))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not open attachment", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

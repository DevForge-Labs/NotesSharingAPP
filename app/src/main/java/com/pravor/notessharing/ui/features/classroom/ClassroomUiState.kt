package com.pravor.notessharing.ui.features.classroom

import androidx.compose.runtime.Immutable
import com.pravor.notessharing.data.classroom.ClassroomAccount
import com.pravor.notessharing.domain.model.classroom.ClassroomCourse

enum class ClassroomSyncStatus {
    SYNCED,
    SYNCING,
    ERROR
}

sealed interface ClassroomUiState {
    data object Disconnected : ClassroomUiState
    data object Loading : ClassroomUiState
    data class Connected(
        val account: ClassroomAccount,
        val allCourses: List<ClassroomCourse> = emptyList(),
        val visibleCourses: List<ClassroomCourse> = emptyList(),
        val hiddenCourseIds: Set<String> = emptySet(),
        val syncStatus: ClassroomSyncStatus = ClassroomSyncStatus.SYNCED,
        val upcomingCount: Int = 0,
        val isCoursesLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val coursesError: String? = null
    ) : ClassroomUiState
    data class Error(val message: String) : ClassroomUiState
    data object Empty : ClassroomUiState
}

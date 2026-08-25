package com.pravor.notessharing.ui.features.classroom

import androidx.compose.runtime.Immutable
import com.pravor.notessharing.domain.model.classroom.ClassroomAnnouncement
import com.pravor.notessharing.domain.model.classroom.ClassroomCourse
import com.pravor.notessharing.domain.model.classroom.ClassroomCourseWork
import com.pravor.notessharing.domain.model.classroom.ClassroomMaterial

@Immutable
data class ClassroomCourseUiState(
    val course: ClassroomCourse? = null,
    val materials: List<ClassroomMaterial> = emptyList(),
    val announcements: List<ClassroomAnnouncement> = emptyList(),
    val coursework: List<ClassroomCourseWork> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

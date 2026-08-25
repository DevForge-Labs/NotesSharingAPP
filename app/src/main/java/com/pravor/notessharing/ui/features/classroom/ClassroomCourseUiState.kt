package com.pravor.notessharing.ui.features.classroom

import androidx.compose.runtime.Immutable
import com.pravor.notessharing.domain.model.classroom.ClassroomAnnouncement
import com.pravor.notessharing.domain.model.classroom.ClassroomCourse
import com.pravor.notessharing.domain.model.classroom.ClassroomCourseWork
import com.pravor.notessharing.domain.model.classroom.ClassroomMaterial
import com.pravor.notessharing.domain.model.classroom.ClassroomStudentSubmission

enum class ClassroomContentFilter(val label: String) {
    ALL("All"),
    ASSIGNMENTS("Assignments"),
    MATERIALS("Class Materials"),
    ANNOUNCEMENTS("Announcements")
}

@Immutable
data class ClassroomCourseUiState(
    val course: ClassroomCourse? = null,
    val materials: List<ClassroomMaterial> = emptyList(),
    val announcements: List<ClassroomAnnouncement> = emptyList(),
    val coursework: List<ClassroomCourseWork> = emptyList(),
    val submissions: Map<String, ClassroomStudentSubmission> = emptyMap(),
    val selectedFilter: ClassroomContentFilter = ClassroomContentFilter.ALL,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

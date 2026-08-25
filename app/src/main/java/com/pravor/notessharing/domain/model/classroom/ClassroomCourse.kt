package com.pravor.notessharing.domain.model.classroom

import androidx.compose.runtime.Immutable

enum class CourseState {
    ACTIVE,
    ARCHIVED,
    PROVISIONED,
    DECLINED,
    SUSPENDED,
    UNKNOWN
}

@Immutable
data class ClassroomTeacher(
    val id: String,
    val name: String,
    val photoUrl: String? = null
)

@Immutable
data class ClassroomCourse(
    val id: String,
    val name: String,
    val section: String? = null,
    val descriptionHeading: String? = null,
    val description: String? = null,
    val room: String? = null,
    val enrollmentCode: String? = null,
    val alternateLink: String? = null,
    val state: CourseState = CourseState.ACTIVE,
    val teacher: ClassroomTeacher? = null
)

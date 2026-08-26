package com.pravor.notessharing.domain.model.classroom

data class ClassroomUpcomingAssignment(
    val courseWork: ClassroomCourseWork,
    val courseId: String,
    val courseName: String,
    val dueEpochMillis: Long,
    val submission: ClassroomStudentSubmission? = null,
    val isLocallyDone: Boolean = false
)

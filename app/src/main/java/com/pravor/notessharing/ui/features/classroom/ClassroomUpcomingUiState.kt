package com.pravor.notessharing.ui.features.classroom

import androidx.compose.runtime.Immutable
import com.pravor.notessharing.domain.model.classroom.ClassroomUpcomingAssignment

@Immutable
data class ClassroomUpcomingUiState(
    val assignments: List<ClassroomUpcomingAssignment> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val markingDoneIds: Set<String> = emptySet(),
    val errorMessage: String? = null
)

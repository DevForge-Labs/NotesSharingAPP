package com.pravor.notessharing.data.classroom

import androidx.compose.runtime.Immutable

@Immutable
data class ClassroomAccount(
    val email: String,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val connectedAt: Long = System.currentTimeMillis()
)

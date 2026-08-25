package com.pravor.notessharing.data.classroom

sealed interface ClassroomAuthState {
    data object Disconnected : ClassroomAuthState
    data object Authorizing : ClassroomAuthState
    data class Connected(val account: ClassroomAccount) : ClassroomAuthState
    data class Error(val message: String, val isRecoverable: Boolean = false) : ClassroomAuthState
}

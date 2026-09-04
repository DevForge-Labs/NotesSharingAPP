package com.pravor.notessharing.ui.features.classroom

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pravor.notessharing.data.classroom.ClassroomAuthException
import com.pravor.notessharing.data.classroom.ClassroomAuthManager
import com.pravor.notessharing.data.classroom.ClassroomAuthState
import com.pravor.notessharing.data.classroom.ClassroomForbiddenException
import com.pravor.notessharing.data.classroom.ClassroomNetworkException
import com.pravor.notessharing.data.classroom.ClassroomRepository
import com.pravor.notessharing.domain.model.classroom.ClassroomCourse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ClassroomViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ClassroomViewModel"
    }

    private val authManager = ClassroomAuthManager.getInstance(application)
    private val repository = ClassroomRepository.getInstance(application)

    private val _isCoursesLoading = MutableStateFlow(false)
    private val _isRefreshing = MutableStateFlow(false)
    private val _coursesError = MutableStateFlow<String?>(null)

    private var lastConnectedEmail: String? = null

    private val upcomingCountFlow = repository.observeUpcomingAssignments()
        .map { it.size }
        .distinctUntilChanged()

    private val coursesAndHiddenFlow = combine(
        repository.observeCourses(),
        repository.observeHiddenCourseIds(),
        upcomingCountFlow
    ) { courses, hiddenIds, upcomingCount ->
        Triple(courses, hiddenIds, upcomingCount)
    }

    val uiState: StateFlow<ClassroomUiState> = combine(
        authManager.authState,
        coursesAndHiddenFlow,
        combine(_isCoursesLoading, _isRefreshing, _coursesError) { loading, refreshing, error ->
            Triple(loading, refreshing, error)
        }
    ) { authState, (cachedCourses, hiddenIds, upcomingCount), (isLoading, isRefreshing, error) ->
        when (authState) {
            is ClassroomAuthState.Disconnected -> {
                lastConnectedEmail = null
                _coursesError.value = null
                ClassroomUiState.Disconnected
            }
            is ClassroomAuthState.Authorizing -> {
                ClassroomUiState.Loading
            }
            is ClassroomAuthState.Connected -> {
                val currentEmail = authState.account.email
                if (lastConnectedEmail != currentEmail) {
                    lastConnectedEmail = currentEmail
                    _coursesError.value = null
                    syncCourses(isPullToRefresh = false)
                }

                val syncStatus = when {
                    isRefreshing || isLoading -> ClassroomSyncStatus.SYNCING
                    error != null -> ClassroomSyncStatus.ERROR
                    else -> ClassroomSyncStatus.SYNCED
                }

                val visible = cachedCourses.filter { it.id !in hiddenIds }

                ClassroomUiState.Connected(
                    account = authState.account,
                    allCourses = cachedCourses,
                    visibleCourses = visible,
                    hiddenCourseIds = hiddenIds,
                    syncStatus = syncStatus,
                    upcomingCount = upcomingCount,
                    isCoursesLoading = isLoading,
                    isRefreshing = isRefreshing,
                    coursesError = error
                )
            }
            is ClassroomAuthState.Error -> {
                ClassroomUiState.Error(authState.message)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ClassroomUiState.Disconnected
    )

    fun syncCourses(isPullToRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isPullToRefresh) {
                _isRefreshing.value = true
            } else {
                _isCoursesLoading.value = true
            }
            _coursesError.value = null

            val result = repository.syncCourses(force = isPullToRefresh)
            result.onSuccess {
                Log.d(TAG, "Courses synced into Room successfully.")
                _coursesError.value = null
            }.onFailure { exception ->
                Log.e(TAG, "Failed to sync courses into Room", exception)
                val errorMessage = when (exception) {
                    is ClassroomAuthException -> "Classroom authorization expired. Please reconnect."
                    is ClassroomForbiddenException -> "Google Classroom access denied. Ensure the Classroom API is enabled."
                    is ClassroomNetworkException -> "Network error. Please check your internet connection."
                    else -> exception.message ?: "Couldn't sync your classes. Please try again."
                }
                _coursesError.value = errorMessage
            }

            _isCoursesLoading.value = false
            _isRefreshing.value = false
        }
    }

    fun saveHiddenCourses(newHiddenIds: Set<String>) {
        viewModelScope.launch {
            repository.setHiddenCourseIds(newHiddenIds)
        }
    }

    fun refreshAuth() {
        authManager.refreshAuthState()
        if (authManager.authState.value is ClassroomAuthState.Connected) {
            syncCourses(isPullToRefresh = true)
        }
    }

    fun getAuthIntent(): Intent {
        com.pravor.notessharing.core.analytics.AnalyticsManager.logClassroomConnectStarted()
        return authManager.getSignInIntent()
    }

    fun handleAuthResult(intent: Intent?) {
        authManager.handleSignInResult(intent)
        when (val state = authManager.authState.value) {
            is ClassroomAuthState.Connected -> {
                com.pravor.notessharing.core.analytics.AnalyticsManager.logClassroomConnectSuccess(0)
            }
            is ClassroomAuthState.Error -> {
                com.pravor.notessharing.core.analytics.AnalyticsManager.logClassroomConnectFailed(state.message)
            }
            else -> {}
        }
    }

    fun disconnectClassroom() {
        viewModelScope.launch {
            com.pravor.notessharing.core.analytics.AnalyticsManager.logClassroomDisconnect()
            repository.clearClassroomDataForCurrentAccount()
            authManager.disconnect()
            _coursesError.value = null
            lastConnectedEmail = null
        }
    }
}

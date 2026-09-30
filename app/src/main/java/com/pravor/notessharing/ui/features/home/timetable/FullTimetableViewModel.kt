package com.pravor.notessharing.ui.features.home.timetable

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pravor.notessharing.data.repository.KayaTimetableRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.Calendar

data class FullTimetableUiState(
    val isLoading: Boolean = false,
    val isConnected: Boolean = true,
    val isSessionExpired: Boolean = false,
    val errorMessage: String? = null,
    val selectedDay: String = "Monday",
    val availableDays: List<String> = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"),
    val allEntries: List<TimetableRowItem> = emptyList(),
    val is24Hour: Boolean = false
) {
    fun entriesForDay(day: String): List<TimetableRowItem> {
        return allEntries.filter { it.day.equals(day, ignoreCase = true) }
    }
}

class FullTimetableViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val kayaRepository = KayaTimetableRepository.getInstance(application)
    private val auth = FirebaseAuth.getInstance()

    val availableDays: List<String> = listOf(
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    )

    private val initialDayArg: String? = savedStateHandle.get<String>("day")

    private val _selectedDay = MutableStateFlow(resolveInitialDay(initialDayArg))
    val selectedDay: StateFlow<String> = _selectedDay.asStateFlow()

    private val _uiState = MutableStateFlow(
        FullTimetableUiState(
            isLoading = true,
            selectedDay = _selectedDay.value,
            availableDays = availableDays,
            is24Hour = android.text.format.DateFormat.is24HourFormat(application)
        )
    )
    val uiState: StateFlow<FullTimetableUiState> = _uiState.asStateFlow()

    private var observationJob: Job? = null

    private val minuteTickerFlow = flow {
        emit(System.currentTimeMillis())
        while (true) {
            val now = System.currentTimeMillis()
            val delayMs = 60_000L - (now % 60_000L) + 50L
            delay(delayMs)
            emit(System.currentTimeMillis())
        }
    }

    init {
        loadCachedTimetable()
        observeTimetable()
    }

    private fun resolveInitialDay(arg: String?): String {
        if (!arg.isNullOrBlank()) {
            val normalized = TimetableTimeUtils.normalizeDay(arg)
            if (availableDays.any { it.equals(normalized, ignoreCase = true) }) {
                return normalized
            }
        }
        return try {
            val dayOfWeek = java.time.LocalDate.now().dayOfWeek
            when (dayOfWeek) {
                java.time.DayOfWeek.MONDAY -> "Monday"
                java.time.DayOfWeek.TUESDAY -> "Tuesday"
                java.time.DayOfWeek.WEDNESDAY -> "Wednesday"
                java.time.DayOfWeek.THURSDAY -> "Thursday"
                java.time.DayOfWeek.FRIDAY -> "Friday"
                java.time.DayOfWeek.SATURDAY -> "Saturday"
                java.time.DayOfWeek.SUNDAY -> "Sunday"
            }
        } catch (e: Throwable) {
            TimetableTimeUtils.getCurrentDayName()
        }
    }

    fun onDaySelected(day: String) {
        val normalized = TimetableTimeUtils.normalizeDay(day)
        _selectedDay.value = normalized
    }

    fun refreshTimeFormat() {
        val is24 = android.text.format.DateFormat.is24HourFormat(getApplication())
        if (_uiState.value.is24Hour != is24) {
            _uiState.value = _uiState.value.copy(is24Hour = is24)
        }
    }

    private fun loadCachedTimetable() {
        viewModelScope.launch(Dispatchers.IO) {
            val uid = auth.currentUser?.uid ?: kayaRepository.getLastConnectedUserId() ?: "anonymous"
            val cached = kayaRepository.getCachedTimetableDirect(uid)
            if (cached.isNotEmpty()) {
                val calendar = Calendar.getInstance()
                val currentDay = TimetableTimeUtils.getCurrentDayName(calendar)
                val currentMinutes = TimetableTimeUtils.getCurrentMinutes(calendar)

                val processed = cached.map { item ->
                    val isActive = TimetableTimeUtils.isClassActive(
                        item = item,
                        selectedDay = currentDay,
                        currentDay = currentDay,
                        currentMinutes = currentMinutes
                    )
                    item.copy(isCurrentClass = isActive)
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isConnected = kayaRepository.isConnected(uid),
                    isSessionExpired = kayaRepository.isSessionExpired(uid),
                    allEntries = processed
                )
            }
        }
    }

    private fun observeTimetable() {
        observationJob?.cancel()
        val uid = auth.currentUser?.uid ?: kayaRepository.getLastConnectedUserId() ?: "anonymous"
        observationJob = viewModelScope.launch {
            combine(
                kayaRepository.observeTimetable(uid),
                _selectedDay,
                minuteTickerFlow
            ) { allEntries, day, _ ->
                val isConnected = kayaRepository.isConnected(uid)
                val isExpired = kayaRepository.isSessionExpired(uid)

                val calendar = Calendar.getInstance()
                val currentDay = TimetableTimeUtils.getCurrentDayName(calendar)
                val currentMinutes = TimetableTimeUtils.getCurrentMinutes(calendar)

                val processed = allEntries.map { item ->
                    val isActive = TimetableTimeUtils.isClassActive(
                        item = item,
                        selectedDay = currentDay,
                        currentDay = currentDay,
                        currentMinutes = currentMinutes
                    )
                    item.copy(isCurrentClass = isActive)
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isConnected = isConnected,
                    isSessionExpired = isExpired,
                    selectedDay = day,
                    availableDays = availableDays,
                    allEntries = processed,
                    is24Hour = android.text.format.DateFormat.is24HourFormat(getApplication())
                )
            }.collect {}
        }
    }
}

package com.pravor.notessharing.data.kaya

sealed class KayaException(message: String, cause: Throwable? = null) : Exception(message, cause)
class KayaAuthException(message: String = "Incorrect KAYA username or password.") : KayaException(message)
class KayaSessionExpiredException(message: String = "Your KAYA session has expired. Please reconnect.") : KayaException(message)
class KayaNetworkException(message: String = "Unable to connect to KAYA right now. Please check your internet connection and try again.", cause: Throwable? = null) : KayaException(message, cause)
class KayaParseException(message: String = "Unable to parse timetable from KAYA dashboard.", cause: Throwable? = null) : KayaException(message, cause)

data class KayaParsedSlot(
    val entryId: String,
    val courseCode: String,
    val courseName: String,
    val section: String,
    val sectionShort: String,
    val faculty: String,
    val facultyCode: String,
    val day: String,
    val period: String,
    val time: String,
    val room: String,
    val fullRoom: String,
    val type: String,
    val slotOrder: Int
)

data class KayaSession(
    val username: String,
    val sessionId: String,
    val csrfToken: String,
    val lastConnectedAt: Long,
    val isSessionExpired: Boolean = false
)

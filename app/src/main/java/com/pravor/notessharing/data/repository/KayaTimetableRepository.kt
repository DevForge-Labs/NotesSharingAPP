package com.pravor.notessharing.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.pravor.notessharing.data.kaya.KayaAuthException
import com.pravor.notessharing.data.kaya.KayaAuthManager
import com.pravor.notessharing.data.kaya.KayaRemoteDataSource
import com.pravor.notessharing.data.kaya.KayaSession
import com.pravor.notessharing.data.kaya.KayaSessionExpiredException
import com.pravor.notessharing.data.kaya.KayaTimetableParser
import com.pravor.notessharing.data.local.dao.KayaTimetableDao
import com.pravor.notessharing.data.local.db.AppDatabase
import com.pravor.notessharing.data.local.entity.KayaTimetableEntity
import androidx.room.withTransaction
import com.pravor.notessharing.ui.features.home.timetable.TimetableRowItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class KayaTimetableRepository private constructor(
    private val context: Context,
    private val database: AppDatabase,
    private val timetableDao: KayaTimetableDao = database.kayaTimetableDao(),
    private val authManager: KayaAuthManager = KayaAuthManager.getInstance(context),
    private val remoteDataSource: KayaRemoteDataSource = KayaRemoteDataSource()
) {

    companion object {
        private const val TAG = "KayaRepository"

        @Volatile
        private var INSTANCE: KayaTimetableRepository? = null

        fun getInstance(context: Context): KayaTimetableRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val db = AppDatabase.getDatabase(context)
                    KayaTimetableRepository(
                        context = context.applicationContext,
                        database = db
                    ).also { INSTANCE = it }
                }
            }
        }
    }

    fun getLastConnectedUserId(): String? = authManager.getLastConnectedUserId()

    private fun getCurrentUserId(): String {
        return FirebaseAuth.getInstance().currentUser?.uid
            ?: authManager.getLastConnectedUserId()
            ?: "anonymous"
    }

    /**
     * Synchronously/directly reads cached timetable entries from RoomDB for instant app cold-start rendering.
     */
    suspend fun getCachedTimetableDirect(userId: String = getCurrentUserId()): List<TimetableRowItem> = withContext(Dispatchers.IO) {
        val entities = timetableDao.getTimetableForUser(userId)
        entities.map { entity ->
            val fullRoomClean = (entity.fullRoom.takeIf { it.isNotBlank() } ?: entity.room)
                .replace(Regex("""^(?i)room\s*[-:]*\s*"""), "")
                .trim()
            val timeRange = com.pravor.notessharing.ui.features.home.timetable.TimetableTimeUtils.parseTimeRange(entity.time)

            TimetableRowItem(
                id = entity.entryId,
                subjectName = entity.courseName,
                rawTime = entity.time,
                startTimeMinutes = timeRange?.first,
                endTimeMinutes = timeRange?.second,
                period = entity.period.takeIf { it.isNotBlank() },
                room = fullRoomClean.takeIf { it.isNotBlank() },
                type = entity.type.takeIf { it.isNotBlank() },
                faculty = entity.faculty.takeIf { it.isNotBlank() },
                day = entity.day
            )
        }
    }

    /**
     * Observes all timetable entries cached in RoomDB for the specified user.
     * Maps database entities to glanceable UI models.
     */
    fun observeTimetable(userId: String = getCurrentUserId()): Flow<List<TimetableRowItem>> {
        return timetableDao.observeTimetable(userId).map { entities ->
            entities.map { entity ->
                val fullRoomClean = (entity.fullRoom.takeIf { it.isNotBlank() } ?: entity.room)
                    .replace(Regex("""^(?i)room\s*[-:]*\s*"""), "")
                    .trim()
                val timeRange = com.pravor.notessharing.ui.features.home.timetable.TimetableTimeUtils.parseTimeRange(entity.time)

                TimetableRowItem(
                    id = entity.entryId,
                    subjectName = entity.courseName,
                    rawTime = entity.time,
                    startTimeMinutes = timeRange?.first,
                    endTimeMinutes = timeRange?.second,
                    period = entity.period.takeIf { it.isNotBlank() },
                    room = fullRoomClean.takeIf { it.isNotBlank() },
                    type = entity.type.takeIf { it.isNotBlank() },
                    faculty = entity.faculty.takeIf { it.isNotBlank() },
                    day = entity.day
                )
            }
        }
    }

    /**
     * Observes timetable entries filtered by day.
     */
    fun observeTimetableByDay(day: String, userId: String = getCurrentUserId()): Flow<List<TimetableRowItem>> {
        return timetableDao.observeTimetableByDay(userId, day).map { entities ->
            entities.map { entity ->
                val fullRoomClean = (entity.fullRoom.takeIf { it.isNotBlank() } ?: entity.room)
                    .replace(Regex("""^(?i)room\s*[-:]*\s*"""), "")
                    .trim()
                val timeRange = com.pravor.notessharing.ui.features.home.timetable.TimetableTimeUtils.parseTimeRange(entity.time)

                TimetableRowItem(
                    id = entity.entryId,
                    subjectName = entity.courseName,
                    rawTime = entity.time,
                    startTimeMinutes = timeRange?.first,
                    endTimeMinutes = timeRange?.second,
                    period = entity.period.takeIf { it.isNotBlank() },
                    room = fullRoomClean.takeIf { it.isNotBlank() },
                    type = entity.type.takeIf { it.isNotBlank() },
                    faculty = entity.faculty.takeIf { it.isNotBlank() },
                    day = entity.day
                )
            }
        }
    }

    /**
     * Synchronizes timetable with KAYA using the 3-tier authentication lifecycle:
     * 1. Direct login if username/password supplied (First-time connection or manual sign in).
     * 2. Silent verification using existing local session cookies.
     * 3. Silent re-authentication using encrypted local credentials if session expired.
     *
     * Cached RoomDB timetable is NEVER cleared on network, session, or authentication failure.
     */
    suspend fun syncTimetable(
        username: String? = null,
        password: String? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        val userId = getCurrentUserId()

        try {
            // Priority 0: Explicit login with username + password
            if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
                val loginResult = authManager.login(username, password)
                if (loginResult.isFailure) {
                    return@withContext Result.failure(loginResult.exceptionOrNull() ?: KayaAuthException())
                }
                val session = loginResult.getOrThrow()
                return@withContext fetchAndPersistTimetable(userId, session)
            }

            // Priority 1: Synchronizing with existing session
            val existingSession = authManager.getSession(userId)
            if (existingSession != null && !existingSession.isSessionExpired) {
                try {
                    return@withContext fetchAndPersistTimetable(userId, existingSession)
                } catch (e: KayaSessionExpiredException) {
                    Log.d(TAG, "Existing session expired on fetch. Proceeding to silent re-authentication.")
                    authManager.markSessionExpired(userId)
                }
            }

            // Priority 2: Silent Re-Authentication using encrypted local credentials
            val storedCreds = authManager.getStoredCredentials(userId)
            if (storedCreds != null) {
                Log.d(TAG, "Attempting silent re-authentication with encrypted credentials for user $userId")
                val reauthResult = authManager.login(storedCreds.username, storedCreds.password)
                if (reauthResult.isSuccess) {
                    val newSession = reauthResult.getOrThrow()
                    Log.d(TAG, "Silent re-authentication successful! Fetching updated timetable.")
                    return@withContext fetchAndPersistTimetable(userId, newSession)
                } else {
                    val ex = reauthResult.exceptionOrNull()
                    Log.w(TAG, "Silent re-authentication failed: ${ex?.message}")
                    authManager.markSessionExpired(userId)
                    // Cached timetable in RoomDB is preserved!
                    return@withContext Result.failure(ex ?: KayaAuthException("Sign-in required."))
                }
            }

            // Priority 3: No valid session and no stored credentials
            authManager.markSessionExpired(userId)
            Result.failure(KayaAuthException("No active KAYA session or credentials. Please connect your account."))
        } catch (e: Exception) {
            Log.e(TAG, "KAYA sync failed for user $userId. Preserving cached data.", e)
            Result.failure(e)
        }
    }

    private suspend fun fetchAndPersistTimetable(userId: String, session: KayaSession): Result<Int> {
        // 1. Fetch dashboard HTML
        val html = remoteDataSource.fetchDashboardHtml(session)

        // 2. Parse timetable chips
        val parsedSlots = KayaTimetableParser.parseDashboardHtml(html)

        // 3. Validate entries before saving
        if (parsedSlots.isEmpty()) {
            Log.w(TAG, "Dashboard contained zero timetable slots. Retaining existing cache.")
            return Result.success(0)
        }

        // 4. Map to Room entities
        val syncTime = System.currentTimeMillis()
        val entities = parsedSlots.map { slot ->
            KayaTimetableEntity(
                userId = userId,
                entryId = slot.entryId,
                courseCode = slot.courseCode,
                courseName = slot.courseName,
                section = slot.section,
                sectionShort = slot.sectionShort,
                faculty = slot.faculty,
                facultyCode = slot.facultyCode,
                day = slot.day,
                period = slot.period,
                time = slot.time,
                room = slot.room,
                fullRoom = slot.fullRoom,
                type = slot.type,
                slotOrder = slot.slotOrder,
                lastSyncedAt = syncTime
            )
        }

        // 5. ATOMIC RoomDB Replacement: Never wipe unless parsed slots are valid
        database.withTransaction {
            timetableDao.deleteForUser(userId)
            timetableDao.insertAll(entities)
        }
        Log.d(TAG, "Successfully synced and saved ${entities.size} timetable slots for user $userId")
        return Result.success(entities.size)
    }

    /**
     * Explicitly disconnects KAYA for the specified user.
     * Deletes locally stored credentials, session cookies, and cached timetable.
     */
    suspend fun disconnectKaya(userId: String = getCurrentUserId()) = withContext(Dispatchers.IO) {
        timetableDao.deleteForUser(userId)
        authManager.disconnect(userId)
        Log.d(TAG, "Explicitly disconnected KAYA and purged timetable cache for user $userId")
    }

    suspend fun clearTimetableForUser(userId: String = getCurrentUserId()) = withContext(Dispatchers.IO) {
        disconnectKaya(userId)
    }

    fun isConnected(userId: String = getCurrentUserId()): Boolean {
        return authManager.isConnected(userId)
    }

    fun isSessionExpired(userId: String = getCurrentUserId()): Boolean {
        return authManager.isSessionExpired(userId)
    }

    fun getStoredUsername(userId: String = getCurrentUserId()): String? {
        return authManager.getStoredUsername(userId)
    }
}

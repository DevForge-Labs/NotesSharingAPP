package com.pravor.notessharing.data.repository

import android.content.Context
import com.pravor.notessharing.NotesSharingApplication
import com.pravor.notessharing.data.local.db.AppDatabase
import com.pravor.notessharing.data.local.dao.UserProfileDao
import com.pravor.notessharing.data.mapper.toDomainModel
import com.pravor.notessharing.data.mapper.toEntity
import com.pravor.notessharing.core.firebase.FirestoreUserService
import com.pravor.notessharing.domain.model.Profile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileRepository(
    context: Context = NotesSharingApplication.appContext,
    private val userService: FirestoreUserService = FirestoreUserService(),
    private val userProfileDao: UserProfileDao = AppDatabase.getDatabase(context).userProfileDao()
) {
    private var activeObserverJob: Job? = null
    private var currentObservedUid: String? = null
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    /**
     * Single Source of Truth: Observes Profile Flow directly from Room Database.
     */
    fun observeProfile(uid: String): Flow<Profile?> {
        syncRemoteProfile(uid)
        return userProfileDao.observeProfile(uid).map { entity ->
            entity?.toDomainModel()
        }
    }

    /**
     * Synchronizes Firestore live profile updates into Room DB.
     * Prevents duplicate snapshot listeners by checking currentObservedUid and job state.
     */
    fun syncRemoteProfile(uid: String) {
        if (currentObservedUid == uid && activeObserverJob?.isActive == true) {
            return
        }
        activeObserverJob?.cancel()
        currentObservedUid = uid
        activeObserverJob = repositoryScope.launch {
            userService.observeUserProfile(uid).collect { remoteProfile ->
                if (remoteProfile != null) {
                    userProfileDao.upsertProfile(remoteProfile.toEntity())
                }
            }
        }
    }

    suspend fun getProfile(uid: String): Profile? = withContext(Dispatchers.IO) {
        val cachedEntity = userProfileDao.getProfile(uid)
        if (cachedEntity != null) {
            return@withContext cachedEntity.toDomainModel()
        }
        val remoteProfile = userService.getUserProfile(uid)
        if (remoteProfile != null) {
            userProfileDao.upsertProfile(remoteProfile.toEntity())
        }
        return@withContext remoteProfile
    }

    suspend fun saveProfile(profile: Profile) = withContext(Dispatchers.IO) {
        userProfileDao.upsertProfile(profile.toEntity())
        userService.createUserProfile(profile)
    }

    suspend fun updateProfileFields(
        uid: String,
        name: String,
        semester: String,
        section: String,
        profileImageUrl: String,
        branch: String
    ) = withContext(Dispatchers.IO) {
        val previousEntity = userProfileDao.getProfile(uid)
        
        // 1. Optimistic Room update
        val updatedEntity = previousEntity?.copy(
            name = name,
            semester = semester,
            section = section,
            profileImageUrl = profileImageUrl,
            branch = branch,
            lastUpdatedMs = System.currentTimeMillis()
        ) ?: Profile(
            uid = uid,
            name = name,
            email = "",
            semester = semester,
            section = section,
            profileImageUrl = profileImageUrl,
            branch = branch
        ).toEntity()

        userProfileDao.upsertProfile(updatedEntity)

        // 2. Remote update to Firestore
        try {
            userService.updateProfileFields(uid, name, semester, section, profileImageUrl, branch)
        } catch (e: Exception) {
            // 3. Rollback on failure to prevent inconsistent Room state
            if (previousEntity != null) {
                userProfileDao.upsertProfile(previousEntity)
            } else {
                userProfileDao.deleteProfile(uid)
            }
            throw e
        }
    }

    suspend fun uploadProfileImage(uid: String, imageUriStr: String): String {
        return userService.uploadProfileImage(uid, imageUriStr)
    }

    fun stopObservingRemoteProfile() {
        activeObserverJob?.cancel()
        activeObserverJob = null
        currentObservedUid = null
    }
}

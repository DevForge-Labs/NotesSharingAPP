package com.pravor.notessharing.data.repository

import com.pravor.notessharing.core.firebase.FirestoreUserService
import com.pravor.notessharing.domain.model.Profile
import kotlinx.coroutines.flow.Flow

class ContributorStatsRepository {
    private val userService = FirestoreUserService()

    fun observeStats(uid: String): Flow<Profile?> {
        return userService.observeUserProfile(uid)
    }

    suspend fun getStats(uid: String): Profile? {
        return userService.getUserProfile(uid)
    }
}

package com.pravor.notessharing.profile

import com.pravor.notessharing.firebase.FirestoreUserService
import com.pravor.notessharing.model.Profile
import kotlinx.coroutines.flow.Flow

class ProfileRepository(private val userService: FirestoreUserService = FirestoreUserService()) {
    fun observeProfile(uid: String): Flow<Profile?> {
        return userService.observeUserProfile(uid)
    }

    suspend fun getProfile(uid: String): Profile? {
        return userService.getUserProfile(uid)
    }

    suspend fun saveProfile(profile: Profile) {
        userService.createUserProfile(profile)
    }

    suspend fun updateProfileFields(uid: String, name: String, semester: String, section: String, profileImageUrl: String, branch: String) {
        userService.updateProfileFields(uid, name, semester, section, profileImageUrl, branch)
    }

    suspend fun uploadProfileImage(uid: String, imageUriStr: String): String {
        return userService.uploadProfileImage(uid, imageUriStr)
    }
}

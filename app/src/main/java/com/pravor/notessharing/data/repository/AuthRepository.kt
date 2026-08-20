package com.pravor.notessharing.data.repository


import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.pravor.notessharing.core.firebase.FirestoreUserService
import com.pravor.notessharing.domain.model.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await


class AuthRepository(
    private val userService: FirestoreUserService = FirestoreUserService(),
    private val profileRepository: ProfileRepository = ProfileRepository()
) {
    private val firebaseAuth = FirebaseAuth.getInstance()

    val currentUser get() = firebaseAuth.currentUser

    fun emailSignUp(
        name: String,
        email: String,
        college: String,
        branch: String,
        semester: String,
        section: String,
        password: String
    ): Flow<Result<Profile>> = flow {
        try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw Exception("Sign up failed: User is null.")
            
            val profile = Profile(
                uid = user.uid,
                name = name,
                email = email,
                college = college,
                branch = branch,
                semester = semester,
                section = section,
                profileImageUrl = "",
                role = "user",
                totalUploads = 0,
                bookmarks = 0,
                upvotes = 0,
                contributorLevel = 1,
                createdAt = System.currentTimeMillis()
            )
            profileRepository.saveProfile(profile)
            emit(Result.success(profile))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun emailLogin(email: String, password: String): Flow<Result<Profile>> = flow {
        try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw Exception("Login failed: User is null.")
            val profile = profileRepository.getProfile(user.uid) ?: throw Exception("User profile does not exist in database.")
            
            if (profile.isDisabled) {
                firebaseAuth.signOut()
                throw Exception("Your account has been disabled by an administrator.")
            }

            emit(Result.success(profile))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun googleSignIn(idToken: String): Flow<Result<Profile>> = flow {
        Log.d("GOOGLE_AUTH", "Token length = ${idToken.length}")
        Log.d("GOOGLE_AUTH", "Token empty = ${idToken.isBlank()}")
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("Google Sign-In failed: User is null.")
            
            val existingProfile = profileRepository.getProfile(user.uid)
            if (existingProfile != null) {
                if (existingProfile.isDisabled) {
                    firebaseAuth.signOut()
                    throw Exception("Your account has been disabled by an administrator.")
                }
                emit(Result.success(existingProfile))
            } else {
                val tempProfile = Profile(
                    uid = user.uid,
                    name = user.displayName ?: "User",
                    email = user.email ?: "",
                    profileImageUrl = user.photoUrl?.toString() ?: "",
                    isOnboardingRequired = true
                )
                emit(Result.success(tempProfile))
            }
        } catch (e: Exception) {
            Log.e("GOOGLE_AUTH", "Google sign in failed", e)
            emit(Result.failure(e))
        }
    }

    suspend fun getUserProfile(uid: String): Profile? {
        return profileRepository.getProfile(uid)
    }

    suspend fun createUserProfile(profile: Profile) {
        profileRepository.saveProfile(profile)
    }

    fun logout() {
        profileRepository.stopObservingRemoteProfile()
        firebaseAuth.signOut()
    }
}

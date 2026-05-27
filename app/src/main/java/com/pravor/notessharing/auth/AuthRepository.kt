package com.pravor.notessharing.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.pravor.notessharing.firebase.FirestoreUserService
import com.pravor.notessharing.model.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class AuthRepository(private val userService: FirestoreUserService = FirestoreUserService()) {
    private val firebaseAuth = FirebaseAuth.getInstance()

    val currentUser get() = firebaseAuth.currentUser

    fun emailSignUp(
        name: String,
        email: String,
        semester: String,
        password: String
    ): Flow<Result<Profile>> = flow {
        try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw Exception("Sign up failed: User is null.")
            
            val profile = Profile(
                uid = user.uid,
                name = name,
                email = email,
                semester = semester,
                profileImageUrl = "",
                role = "user",
                uploads = 0,
                bookmarks = 0,
                upvotes = 0,
                notesUploaded = 0,
                contributorLevel = 1,
                branch = "Computer Science",
                createdAt = System.currentTimeMillis()
            )
            userService.createUserProfile(profile)
            emit(Result.success(profile))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun emailLogin(email: String, password: String): Flow<Result<Profile>> = flow {
        try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw Exception("Login failed: User is null.")
            val profile = userService.getUserProfile(user.uid) ?: throw Exception("User profile does not exist in database.")
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
            
            val profile = userService.createOrGetGoogleUser(
                uid = user.uid,
                name = user.displayName ?: "User",
                email = user.email ?: "",
                profileImageUrl = user.photoUrl?.toString() ?: ""
            )
            emit(Result.success(profile))
        } catch (e: Exception) {
            Log.e("GOOGLE_AUTH", "Google sign in failed", e)
            emit(Result.failure(e))
        }
    }

    fun logout() {
        firebaseAuth.signOut()
    }
}

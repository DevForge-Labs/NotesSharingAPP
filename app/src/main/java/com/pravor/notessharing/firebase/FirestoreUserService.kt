package com.pravor.notessharing.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.pravor.notessharing.model.Profile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreUserService {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    suspend fun getUserProfile(uid: String): Profile? {
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            if (snapshot.exists()) {
                val name = snapshot.getString("name") ?: ""
                val email = snapshot.getString("email") ?: ""
                val semester = snapshot.getString("semester") ?: "Not Set"
                val profileImageUrl = snapshot.getString("profileImageUrl") ?: ""
                val role = snapshot.getString("role") ?: "user"
                val uploads = snapshot.getLong("uploads")?.toInt() ?: 0
                val bookmarks = snapshot.getLong("bookmarks")?.toInt() ?: 0
                val upvotes = snapshot.getLong("upvotes")?.toInt() ?: 0
                val notesUploaded = snapshot.getLong("notesUploaded")?.toInt() ?: 0
                val contributorLevel = snapshot.getLong("contributorLevel")?.toInt() ?: 1
                val branch = snapshot.getString("branch") ?: "Computer Science"
                val createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis()

                Profile(
                    uid = uid,
                    name = name,
                    email = email,
                    semester = semester,
                    profileImageUrl = profileImageUrl,
                    role = role,
                    uploads = uploads,
                    bookmarks = bookmarks,
                    upvotes = upvotes,
                    notesUploaded = notesUploaded,
                    contributorLevel = contributorLevel,
                    branch = branch,
                    createdAt = createdAt
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun observeUserProfile(uid: String): Flow<Profile?> = callbackFlow {
        val listener = usersCollection.document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                try {
                    val name = snapshot.getString("name") ?: ""
                    val email = snapshot.getString("email") ?: ""
                    val semester = snapshot.getString("semester") ?: "Not Set"
                    val profileImageUrl = snapshot.getString("profileImageUrl") ?: ""
                    val role = snapshot.getString("role") ?: "user"
                    val uploads = snapshot.getLong("uploads")?.toInt() ?: 0
                    val bookmarks = snapshot.getLong("bookmarks")?.toInt() ?: 0
                    val upvotes = snapshot.getLong("upvotes")?.toInt() ?: 0
                    val notesUploaded = snapshot.getLong("notesUploaded")?.toInt() ?: 0
                    val contributorLevel = snapshot.getLong("contributorLevel")?.toInt() ?: 1
                    val branch = snapshot.getString("branch") ?: "Computer Science"
                    val createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis()

                    val profile = Profile(
                        uid = uid,
                        name = name,
                        email = email,
                        semester = semester,
                        profileImageUrl = profileImageUrl,
                        role = role,
                        uploads = uploads,
                        bookmarks = bookmarks,
                        upvotes = upvotes,
                        notesUploaded = notesUploaded,
                        contributorLevel = contributorLevel,
                        branch = branch,
                        createdAt = createdAt
                    )
                    trySend(profile)
                } catch (e: Exception) {
                    trySend(null)
                }
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun createUserProfile(profile: Profile) {
        usersCollection.document(profile.uid).set(profile).await()
    }

    suspend fun updateProfileFields(uid: String, name: String, semester: String, profileImageUrl: String) {
        val updates = mapOf(
            "name" to name,
            "semester" to semester,
            "profileImageUrl" to profileImageUrl
        )
        usersCollection.document(uid).update(updates).await()
    }

    suspend fun uploadProfileImage(uid: String, imageUriStr: String): String {
        val uri = android.net.Uri.parse(imageUriStr)
        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
            .child("profile_pictures/$uid.jpg")
        storageRef.putFile(uri).await()
        return storageRef.downloadUrl.await().toString()
    }

    suspend fun createOrGetGoogleUser(
        uid: String,
        name: String,
        email: String,
        profileImageUrl: String
    ): Profile {
        val existingProfile = getUserProfile(uid)
        if (existingProfile != null) {
            return existingProfile
        }
        val newProfile = Profile(
            uid = uid,
            name = name,
            email = email,
            profileImageUrl = profileImageUrl,
            semester = "Not Set",
            role = "user",
            uploads = 0,
            bookmarks = 0,
            upvotes = 0,
            notesUploaded = 0,
            contributorLevel = 1,
            branch = "Computer Science",
            createdAt = System.currentTimeMillis()
        )
        createUserProfile(newProfile)
        return newProfile
    }
}

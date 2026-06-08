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
                val branch = snapshot.getString("branch")?.let { com.pravor.notessharing.model.AcademicCatalog.getDisplayBranch(it) } ?: "CS"
                val college = snapshot.getString("college") ?: "kiit"
                val section = snapshot.getString("section") ?: ""
                val createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis()
                
                val pyqUploads = snapshot.getLong("pyqUploads")?.toInt() ?: 0
                val notesUploads = snapshot.getLong("notesUploads")?.toInt() ?: 0
                val assignmentUploads = snapshot.getLong("assignmentUploads")?.toInt() ?: 0
                val cheatSheetUploads = snapshot.getLong("cheatSheetUploads")?.toInt() ?: 0
                val youtubeUploads = snapshot.getLong("youtubeUploads")?.toInt() ?: 0

                Profile(
                    uid = uid,
                    name = name,
                    email = email,
                    semester = semester,
                    college = college,
                    section = section,
                    profileImageUrl = profileImageUrl,
                    role = role,
                    uploads = uploads,
                    bookmarks = bookmarks,
                    upvotes = upvotes,
                    notesUploaded = notesUploaded,
                    contributorLevel = contributorLevel,
                    branch = branch,
                    createdAt = createdAt,
                    pyqUploads = pyqUploads,
                    notesUploads = notesUploads,
                    assignmentUploads = assignmentUploads,
                    cheatSheetUploads = cheatSheetUploads,
                    youtubeUploads = youtubeUploads
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
                    val branch = snapshot.getString("branch")?.let { com.pravor.notessharing.model.AcademicCatalog.getDisplayBranch(it) } ?: "CS"
                    val college = snapshot.getString("college") ?: "kiit"
                    val section = snapshot.getString("section") ?: ""
                    val createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis()
                    
                    val pyqUploads = snapshot.getLong("pyqUploads")?.toInt() ?: 0
                    val notesUploads = snapshot.getLong("notesUploads")?.toInt() ?: 0
                    val assignmentUploads = snapshot.getLong("assignmentUploads")?.toInt() ?: 0
                    val cheatSheetUploads = snapshot.getLong("cheatSheetUploads")?.toInt() ?: 0
                    val youtubeUploads = snapshot.getLong("youtubeUploads")?.toInt() ?: 0

                    val profile = Profile(
                        uid = uid,
                        name = name,
                        email = email,
                        semester = semester,
                        college = college,
                        section = section,
                        profileImageUrl = profileImageUrl,
                        role = role,
                        uploads = uploads,
                        bookmarks = bookmarks,
                        upvotes = upvotes,
                        notesUploaded = notesUploaded,
                        contributorLevel = contributorLevel,
                        branch = branch,
                        createdAt = createdAt,
                        pyqUploads = pyqUploads,
                        notesUploads = notesUploads,
                        assignmentUploads = assignmentUploads,
                        cheatSheetUploads = cheatSheetUploads,
                        youtubeUploads = youtubeUploads
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
        val userMap = mapOf(
            "uid" to profile.uid,
            "name" to profile.name,
            "email" to profile.email,
            "semester" to profile.semester,
            "college" to com.pravor.notessharing.util.NormalizationUtil.normalizeCollege(profile.college),
            "branch" to com.pravor.notessharing.util.NormalizationUtil.normalizeBranch(profile.branch),
            "section" to com.pravor.notessharing.util.NormalizationUtil.normalizeSection(profile.section),
            "profileImageUrl" to profile.profileImageUrl,
            "role" to profile.role,
            "uploads" to profile.uploads,
            "bookmarks" to profile.bookmarks,
            "upvotes" to profile.upvotes,
            "notesUploaded" to profile.notesUploaded,
            "contributorLevel" to profile.contributorLevel,
            "createdAt" to profile.createdAt,
            "pyqUploads" to profile.pyqUploads,
            "notesUploads" to profile.notesUploads,
            "assignmentUploads" to profile.assignmentUploads,
            "cheatSheetUploads" to profile.cheatSheetUploads,
            "youtubeUploads" to profile.youtubeUploads
        )
        usersCollection.document(profile.uid).set(userMap).await()
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

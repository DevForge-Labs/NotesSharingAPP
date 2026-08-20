package com.pravor.notessharing.core.firebase

import com.pravor.notessharing.core.util.*

import com.google.firebase.firestore.FirebaseFirestore
import com.pravor.notessharing.domain.model.Profile
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
                val accountStatus = snapshot.getString("accountStatus")
                    ?: if (snapshot.getBoolean("isDisabled") == true) "DISABLED" else "ACTIVE"
                val totalUploads = snapshot.getLong("totalUploads")?.toInt() ?: 0
                val bookmarks = snapshot.getLong("bookmarks")?.toInt() ?: 0
                val upvotes = snapshot.getLong("upvotes")?.toInt() ?: 0
                val contributorLevel = snapshot.getLong("contributorLevel")?.toInt() ?: 1
                val branch = snapshot.getString("branch") ?: "cse"
                val college = snapshot.getString("college") ?: ""
                val section = snapshot.getString("section") ?: ""
                val createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis()
                
                val pyqUploads = snapshot.getLong("pyqUploads")?.toInt() ?: 0
                val notesUploads = snapshot.getLong("notesUploads")?.toInt() ?: 0
                val assignmentUploads = snapshot.getLong("assignmentUploads")?.toInt() ?: 0
                val cheatSheetUploads = snapshot.getLong("cheatSheetUploads")?.toInt() ?: 0
                val youtubeResourceUploads = snapshot.getLong("youtubeResourceUploads")?.toInt() ?: 0

                Profile(
                    uid = uid,
                    name = name,
                    email = email,
                    semester = semester,
                    college = college,
                    section = section,
                    profileImageUrl = profileImageUrl,
                    role = role,
                    accountStatus = accountStatus,
                    totalUploads = totalUploads,
                    bookmarks = bookmarks,
                    upvotes = upvotes,
                    contributorLevel = contributorLevel,
                    branch = branch,
                    createdAt = createdAt,
                    pyqUploads = pyqUploads,
                    notesUploads = notesUploads,
                    assignmentUploads = assignmentUploads,
                    cheatSheetUploads = cheatSheetUploads,
                    youtubeResourceUploads = youtubeResourceUploads
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
                    val accountStatus = snapshot.getString("accountStatus")
                        ?: if (snapshot.getBoolean("isDisabled") == true) "DISABLED" else "ACTIVE"
                    val totalUploads = snapshot.getLong("totalUploads")?.toInt() ?: 0
                    val bookmarks = snapshot.getLong("bookmarks")?.toInt() ?: 0
                    val upvotes = snapshot.getLong("upvotes")?.toInt() ?: 0
                    val contributorLevel = snapshot.getLong("contributorLevel")?.toInt() ?: 1
                    val branch = snapshot.getString("branch") ?: "cse"
                    val college = snapshot.getString("college") ?: ""
                    val section = snapshot.getString("section") ?: ""
                    val createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis()
                    
                    val pyqUploads = snapshot.getLong("pyqUploads")?.toInt() ?: 0
                    val notesUploads = snapshot.getLong("notesUploads")?.toInt() ?: 0
                    val assignmentUploads = snapshot.getLong("assignmentUploads")?.toInt() ?: 0
                    val cheatSheetUploads = snapshot.getLong("cheatSheetUploads")?.toInt() ?: 0
                    val youtubeResourceUploads = snapshot.getLong("youtubeResourceUploads")?.toInt() ?: 0

                    val profile = Profile(
                        uid = uid,
                        name = name,
                        email = email,
                        semester = semester,
                        college = college,
                        section = section,
                        profileImageUrl = profileImageUrl,
                        role = role,
                        accountStatus = accountStatus,
                        totalUploads = totalUploads,
                        bookmarks = bookmarks,
                        upvotes = upvotes,
                        contributorLevel = contributorLevel,
                        branch = branch,
                        createdAt = createdAt,
                        pyqUploads = pyqUploads,
                        notesUploads = notesUploads,
                        assignmentUploads = assignmentUploads,
                        cheatSheetUploads = cheatSheetUploads,
                        youtubeResourceUploads = youtubeResourceUploads
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
        val userMap = linkedMapOf(
            "uid" to profile.uid,
            "name" to profile.name,
            "email" to profile.email,
            "profileImageUrl" to profile.profileImageUrl,
            "role" to profile.role,
            "accountStatus" to profile.accountStatus,
            "contributorLevel" to profile.contributorLevel,
            "branch" to com.pravor.notessharing.core.util.NormalizationUtil.normalizeBranch(profile.branch),
            "semester" to profile.semester,
            "section" to com.pravor.notessharing.core.util.NormalizationUtil.normalizeSection(profile.section),
            "college" to com.pravor.notessharing.core.util.NormalizationUtil.normalizeCollege(profile.college),
            "totalUploads" to profile.totalUploads,
            "notesUploads" to profile.notesUploads,
            "assignmentUploads" to profile.assignmentUploads,
            "cheatSheetUploads" to profile.cheatSheetUploads,
            "pyqUploads" to profile.pyqUploads,
            "youtubeResourceUploads" to profile.youtubeResourceUploads,
            "bookmarks" to profile.bookmarks,
            "upvotes" to profile.upvotes,
            "createdAt" to profile.createdAt
        )
        usersCollection.document(profile.uid).set(userMap).await()
    }

    suspend fun updateProfileFields(uid: String, name: String, semester: String, section: String, profileImageUrl: String, branch: String) {
        val updates = mapOf(
            "name" to name,
            "semester" to semester,
            "section" to section,
            "profileImageUrl" to profileImageUrl,
            "branch" to com.pravor.notessharing.core.util.NormalizationUtil.normalizeBranch(branch)
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
            totalUploads = 0,
            bookmarks = 0,
            upvotes = 0,
            contributorLevel = 1,
            college = "kiit",
            branch = "cse",
            createdAt = System.currentTimeMillis()
        )
        createUserProfile(newProfile)
        return newProfile
    }
}

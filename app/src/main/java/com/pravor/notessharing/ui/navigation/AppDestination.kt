package com.pravor.notessharing.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : AppDestination("home", "Home", Icons.Filled.Home)
    data object Explore : AppDestination("explore", "Explore", Icons.Filled.Explore)
    data object TrendingNotes : AppDestination("explore/trending_notes", "Trending Notes", Icons.Filled.Explore)
    data object RecommendedVideos : AppDestination("explore/recommended_videos", "Recommended Videos", Icons.Filled.Explore)
    data object Discover : AppDestination("explore/discover", "Discover", Icons.Filled.Explore)
    data object ExamPrep : AppDestination("explore/exam_prep", "Exam Prep", Icons.Filled.Explore)
    data object Assignments : AppDestination("explore/assignments", "Assignments", Icons.Filled.Explore)
    data object Upload : AppDestination("upload", "Upload", Icons.Filled.UploadFile)
    data object UploadSuccess : AppDestination("upload/success", "Upload Success", Icons.Filled.UploadFile)
    data object MyFiles : AppDestination("my_files", "My Files", Icons.Filled.UploadFile)
    data object MyUploads : AppDestination("my_uploads", "My Uploads", Icons.Filled.UploadFile)
    data object MyBookmarks : AppDestination("my_bookmarks", "My Bookmarks", Icons.Filled.Bookmark)
    data object Profile : AppDestination("profile", "Profile", Icons.Filled.Person)
    data object EditProfile : AppDestination("profile/edit", "Edit Profile", Icons.Filled.Person)
    
    data object Welcome : AppDestination("welcome", "Welcome", Icons.Filled.Home)
    data object Login : AppDestination("login", "Login", Icons.Filled.Home)
    data object SignUp : AppDestination("signup", "Sign Up", Icons.Filled.Home)
    
    data object DocumentDetail : AppDestination("document_detail/{documentId}", "Document Detail", Icons.Filled.Explore) {
        fun createRoute(documentId: String) = "document_detail/$documentId"
    }

    data object VideoDetail : AppDestination("video_detail/{videoId}", "Video Detail", Icons.Filled.Explore) {
        fun createRoute(videoId: String) = "video_detail/$videoId"
    }

    data object PdfViewing : AppDestination("pdf_viewing/{documentId}/{fileUrl}/{title}", "PDF Viewer", Icons.Filled.Explore) {
        fun createRoute(documentId: String, fileUrl: String, title: String): String {
            val encodedUrl = android.net.Uri.encode(android.net.Uri.encode(fileUrl))
            val encodedTitle = android.net.Uri.encode(android.net.Uri.encode(title.ifBlank { "Document" }))
            return "pdf_viewing/$documentId/$encodedUrl/$encodedTitle"
        }
    }

    data object ImageViewing : AppDestination("image_viewing/{documentId}/{fileUrl}/{title}", "Image Viewer", Icons.Filled.Explore) {
        fun createRoute(documentId: String, fileUrl: String, title: String): String {
            val encodedUrl = android.net.Uri.encode(android.net.Uri.encode(fileUrl))
            val encodedTitle = android.net.Uri.encode(android.net.Uri.encode(title.ifBlank { "Image" }))
            return "image_viewing/$documentId/$encodedUrl/$encodedTitle"
        }
    }

    data object SubjectResources : AppDestination("explore/subject_resources/{subjectName}", "Subject Resources", Icons.Filled.Explore) {
        fun createRoute(subjectName: String) = "explore/subject_resources/${android.net.Uri.encode(subjectName)}"
    }
}

val bottomDestinations = listOf(
    AppDestination.Home,
    AppDestination.Explore,
    AppDestination.Upload,
    AppDestination.Profile
)

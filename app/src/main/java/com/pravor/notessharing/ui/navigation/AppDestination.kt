package com.pravor.notessharing.ui.navigation

import androidx.compose.material.icons.Icons
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
    data object Upload : AppDestination("upload", "Upload", Icons.Filled.UploadFile)
    data object MyFiles : AppDestination("my_files", "My Files", Icons.Filled.UploadFile)
    data object Profile : AppDestination("profile", "Profile", Icons.Filled.Person)
    data object EditProfile : AppDestination("profile/edit", "Edit Profile", Icons.Filled.Person)
    
    data object Splash : AppDestination("splash", "Splash", Icons.Filled.Home)
    data object Welcome : AppDestination("welcome", "Welcome", Icons.Filled.Home)
    data object Login : AppDestination("login", "Login", Icons.Filled.Home)
    data object SignUp : AppDestination("signup", "Sign Up", Icons.Filled.Home)
}

val bottomDestinations = listOf(
    AppDestination.Home,
    AppDestination.Explore,
    AppDestination.Upload,
    AppDestination.Profile
)

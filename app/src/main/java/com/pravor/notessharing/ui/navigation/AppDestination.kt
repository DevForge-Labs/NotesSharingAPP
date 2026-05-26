package com.pravor.notessharing.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Folder
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
    data object Upload : AppDestination("upload", "Upload", Icons.Filled.UploadFile)
    data object MyFiles : AppDestination("my_files", "My Files", Icons.Filled.Folder)
    data object Profile : AppDestination("profile", "Profile", Icons.Filled.Person)
}

val bottomDestinations = listOf(
    AppDestination.Home,
    AppDestination.Explore,
    AppDestination.Upload,
    AppDestination.MyFiles,
    AppDestination.Profile
)

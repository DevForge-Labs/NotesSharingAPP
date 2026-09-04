package com.pravor.notessharing.ui.features.home.components

import android.util.Log
import com.pravor.notessharing.ui.navigation.AppDestination

object InteractiveHubDestinationMapper {
    private const val TAG = "InteractiveHubDest"

    /**
     * Maps a remote destination identifier to a safe, valid AppDestination route.
     * Prevents arbitrary navigation route execution and guarantees crash-free fallback.
     */
    fun resolveRoute(destinationId: String?): String {
        if (destinationId.isNullOrBlank()) {
            Log.w(TAG, "Empty destination ID received, defaulting to Explore")
            return AppDestination.Explore.route
        }

        return when (destinationId.lowercase().trim()) {
            "exam_prep", "examprep", "exams", "exam" -> AppDestination.ExamPrep.route
            "notes", "trending_notes", "trendingnotes" -> AppDestination.TrendingNotes.route
            "assignments", "assignment" -> AppDestination.Assignments.route
            "videos", "recommended_videos", "youtube" -> AppDestination.RecommendedVideos.route
            "classroom", "google_classroom" -> AppDestination.Classroom.route
            "classroom_upcoming", "upcoming_assignments" -> AppDestination.ClassroomUpcoming.route
            "upload", "upload_resource" -> AppDestination.Upload.route
            "my_bookmarks", "bookmarks" -> AppDestination.MyBookmarks.route
            "my_files", "downloads", "my_downloads" -> AppDestination.MyFiles.route
            "profile", "account" -> AppDestination.Profile.route
            "search" -> AppDestination.Search.route
            "explore", "discover" -> AppDestination.Explore.route
            else -> {
                Log.w(TAG, "Unknown destination '$destinationId' requested. Safely defaulting to Explore route.")
                AppDestination.Explore.route
            }
        }
    }
}

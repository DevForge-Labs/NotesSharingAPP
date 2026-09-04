package com.pravor.notessharing.core.analytics

import com.pravor.notessharing.ui.navigation.AppDestination

/**
 * Maps Jetpack Compose routes to standardized, human-readable analytics screen names.
 * Ensures consistent snake_case screen identifiers and prevents leaking IDs or dynamic arguments
 * into GA4 screen names.
 */
object AnalyticsScreenMapper {

    fun mapRouteToScreenName(route: String?): String {
        if (route.isNullOrBlank()) return "screen_unknown"

        return when {
            route == "auth_gate" -> "screen_startup"
            route == "onboarding" -> "screen_onboarding"
            route == AppDestination.Welcome.route -> "screen_welcome"
            route == AppDestination.Login.route -> "screen_login"
            route == AppDestination.SignUp.route -> "screen_signup"
            route == "google_onboarding" -> "screen_google_onboarding"
            route == AppDestination.Home.route -> "screen_home"
            route == AppDestination.Explore.route -> "screen_explore"
            route == AppDestination.TrendingNotes.route -> "screen_trending_notes"
            route == AppDestination.RecommendedVideos.route -> "screen_recommended_videos"
            route == AppDestination.Discover.route -> "screen_discover"
            route == AppDestination.ExamPrep.route -> "screen_exam_prep"
            route == AppDestination.Assignments.route -> "screen_assignments"
            route.startsWith("explore/subject_resources") -> "screen_subject_resources"
            route == AppDestination.Search.route -> "screen_search"
            route.startsWith("document_detail") -> "screen_document_detail"
            route.startsWith("pdf_viewing") -> "screen_pdf_viewer"
            route.startsWith("image_viewing") -> "screen_image_viewer"
            route.startsWith("video_detail") -> "screen_video_detail"
            route == AppDestination.Upload.route -> "screen_upload"
            route == AppDestination.UploadSuccess.route -> "screen_upload_success"
            route == AppDestination.Classroom.route -> "screen_classroom"
            route == AppDestination.ClassroomUpcoming.route -> "screen_classroom_upcoming"
            route.startsWith("classroom/course") -> "screen_classroom_course"
            route == AppDestination.Profile.route -> "screen_profile"
            route == AppDestination.EditProfile.route -> "screen_edit_profile"
            route == AppDestination.NotificationPreferences.route -> "screen_notification_settings"
            route == AppDestination.About.route -> "screen_about"
            route == AppDestination.MyFiles.route -> "screen_my_downloads"
            route == AppDestination.MyUploads.route -> "screen_my_uploads"
            route == AppDestination.MyBookmarks.route -> "screen_my_bookmarks"
            else -> {
                // Normalize unrecognized routes: strip query/path params and dynamic braces
                val base = route.substringBefore("/").substringBefore("{").trim()
                if (base.isNotEmpty()) "screen_${base.lowercase()}" else "screen_unknown"
            }
        }
    }
}

package com.pravor.notessharing.ui.common.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class StudyResourceTheme(
    val docTypeStr: String,
    val accentColor: Color,
    val gradientColors: List<Color>
) {
    val cardBrush: Brush = Brush.verticalGradient(gradientColors)
    val thumbnailBrush: Brush = Brush.linearGradient(gradientColors)
}

object StudyResourceThemeCache {
    private val themes = ConcurrentHashMap<String, StudyResourceTheme>()

    fun getTheme(documentType: String?): StudyResourceTheme {
        val rawDocType = (documentType ?: "").lowercase(Locale.ROOT).trim()
        val isPlaylist = rawDocType.contains("playlist")
        val isVideo = rawDocType.contains("video") || rawDocType.contains("youtube")
        val isVideoOnly = isVideo && !isPlaylist

        val isPyq = rawDocType.contains("pyq")
        val isCheatSheet = rawDocType.contains("cheat") || rawDocType.contains("formula")
        val isAssignment = rawDocType.contains("assignment")
        val isNotes = rawDocType.contains("notes")

        val key = when {
            isPlaylist -> "Playlist"
            isVideoOnly -> "Video"
            isPyq -> "PYQ"
            isAssignment -> "Assignment"
            isCheatSheet -> "Cheat Sheet"
            isNotes -> "Notes"
            else -> "PDF"
        }

        return themes.getOrPut(key) {
            val accentColor = when (key) {
                "Video" -> Color(0xFFFF6B6B)
                "Playlist" -> Color(0xFFFF6B6B)
                "Notes" -> Color(0xFF58D6D1)
                "PYQ" -> Color(0xFFFFB45C)
                "Assignment" -> Color(0xFF7AD7FF)
                "Cheat Sheet" -> Color(0xFFC7A6FF)
                else -> Color(0xFFCFD8DC)
            }
            val gradientColors = when (key) {
                "Video" -> listOf(Color(0x9F2D191B), Color(0xFF1A0E10))
                "Playlist" -> listOf(Color(0x9F2D191B), Color(0xFF1A0E10))
                "Notes" -> listOf(Color(0xFF13201F), Color(0xFF0C1312))
                "PYQ" -> listOf(Color(0xFF241C15), Color(0xFF16110D))
                "Cheat Sheet" -> listOf(Color(0xFF1E1724), Color(0xFF120E16))
                "Assignment" -> listOf(Color(0xFF141F23), Color(0xFF0C1316))
                else -> listOf(Color(0xFF1D2124), Color(0xFF111315))
            }
            StudyResourceTheme(key, accentColor, gradientColors)
        }
    }
}

fun getStudyResourceTheme(documentType: String?): StudyResourceTheme {
    return StudyResourceThemeCache.getTheme(documentType)
}

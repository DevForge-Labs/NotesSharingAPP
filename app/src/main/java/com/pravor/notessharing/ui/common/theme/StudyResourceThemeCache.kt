package com.pravor.notessharing.ui.common.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

data class StudyResourceTheme(
    val docTypeStr: String,
    val accentColor: Color,
    val secondaryAccent: Color = accentColor,
    val gradientColors: List<Color>
) {
    val cardBrush: Brush = Brush.verticalGradient(gradientColors)
    val thumbnailBrush: Brush = Brush.linearGradient(gradientColors)
}

object StudyResourceThemeCache {
    private val themes = ConcurrentHashMap<String, StudyResourceTheme>()

    fun getTheme(documentType: String?, titleOrId: String? = null): StudyResourceTheme {
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

        val cacheKey = if (!titleOrId.isNullOrBlank()) "${key}_${abs(titleOrId.hashCode()) % 4}" else key

        return themes.getOrPut(cacheKey) {
            val (accentColor, secondaryAccent, gradientColors) = when (key) {
                "Video" -> Triple(
                    Color(0xFFFF6B6B),
                    Color(0xFFE11D48),
                    listOf(Color(0xFF281216).copy(alpha = 0.72f), Color(0xFF160D10).copy(alpha = 0.75f), Color(0xFF090A0E).copy(alpha = 0.78f))
                )
                "Playlist" -> Triple(
                    Color(0xFFFB7185),
                    Color(0xFFC026D3),
                    listOf(Color(0xFF26121C).copy(alpha = 0.72f), Color(0xFF140D14).copy(alpha = 0.75f), Color(0xFF090A0E).copy(alpha = 0.78f))
                )
                "Notes" -> Triple(
                    Color(0xFF58D6D1),
                    Color(0xFF0D9488),
                    listOf(Color(0xFF112222).copy(alpha = 0.72f), Color(0xFF0D1718).copy(alpha = 0.75f), Color(0xFF090A0E).copy(alpha = 0.78f))
                )
                "PYQ" -> Triple(
                    Color(0xFFFFB45C),
                    Color(0xFFD97706),
                    listOf(Color(0xFF261D12).copy(alpha = 0.72f), Color(0xFF17120E).copy(alpha = 0.75f), Color(0xFF090A0E).copy(alpha = 0.78f))
                )
                "Assignment" -> Triple(
                    Color(0xFF7AD7FF),
                    Color(0xFF2563EB),
                    listOf(Color(0xFF12202A).copy(alpha = 0.72f), Color(0xFF0D161F).copy(alpha = 0.75f), Color(0xFF090A0E).copy(alpha = 0.78f))
                )
                "Cheat Sheet" -> Triple(
                    Color(0xFFC7A6FF),
                    Color(0xFF9333EA),
                    listOf(Color(0xFF1F162A).copy(alpha = 0.72f), Color(0xFF130F1C).copy(alpha = 0.75f), Color(0xFF090A0E).copy(alpha = 0.78f))
                )
                else -> Triple(
                    Color(0xFF94A3B8),
                    Color(0xFF64748B),
                    listOf(Color(0xFF1A1F26).copy(alpha = 0.72f), Color(0xFF111419).copy(alpha = 0.75f), Color(0xFF090A0E).copy(alpha = 0.78f))
                )
            }
            StudyResourceTheme(key, accentColor, secondaryAccent, gradientColors)
        }
    }
}

fun getStudyResourceTheme(documentType: String?, titleOrId: String? = null): StudyResourceTheme {
    return StudyResourceThemeCache.getTheme(documentType, titleOrId)
}


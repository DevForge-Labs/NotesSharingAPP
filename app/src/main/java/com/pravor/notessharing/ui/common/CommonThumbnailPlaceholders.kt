package com.pravor.notessharing.ui.common

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.scale
import com.airbnb.lottie.compose.*
import com.pravor.notessharing.domain.model.FileType
import com.pravor.notessharing.domain.model.UploadType

// Centralized Fallback Mappings
val UploadType.thumbnailFallbackAnimation: String
    get() = when (this) {
        UploadType.Notes -> "App_animations/notes_buffer.json"
        UploadType.Assignment -> "App_animations/assignment_buffer.json"
        UploadType.CheatSheet -> "App_animations/cheatsheet_buffer.json"
        UploadType.Pyq -> "App_animations/pyq_buffer.json"
        UploadType.Youtube -> "App_animations/video_buffer.json"
    }

val FileType.thumbnailFallbackAnimation: String
    get() = when (this) {
        FileType.Notes -> "App_animations/notes_buffer.json"
        FileType.Pyq -> "App_animations/pyq_buffer.json"
        FileType.CheatSheet -> "App_animations/cheatsheet_buffer.json"
        FileType.Video -> "App_animations/video_buffer.json"
        FileType.Pdf -> "App_animations/notes_buffer.json"
        FileType.Book -> "App_animations/notes_buffer.json"
        FileType.LabManual -> "App_animations/notes_buffer.json"
        FileType.StudyGuide -> "App_animations/notes_buffer.json"
    }

fun String.getThumbnailFallbackAnimation(): String {
    val normalized = this.lowercase(java.util.Locale.ROOT).replace(" ", "").replace("_", "")
    return when {
        normalized.contains("notes") -> "App_animations/notes_buffer.json"
        normalized.contains("assignment") -> "App_animations/assignment_buffer.json"
        normalized.contains("pyq") -> "App_animations/pyq_buffer.json"
        normalized.contains("cheatsheet") -> "App_animations/cheatsheet_buffer.json"
        normalized.contains("video") || normalized.contains("youtube") || normalized.contains("playlist") -> "App_animations/video_buffer.json"
        else -> "App_animations/notes_buffer.json"
    }
}

@Composable
fun LottiePlaceholder(
    animationPath: String,
    modifier: Modifier = Modifier
) {
    val compositionResult = rememberLottieComposition(
        LottieCompositionSpec.Asset(animationPath)
    )
    val composition = compositionResult.value

    val progress: Float
    if (animationPath.contains("cheatsheet")) {
        val anim = rememberLottieAnimatable()
        LaunchedEffect(composition) {
            if (composition != null) {
                while (true) {
                    anim.animate(
                        composition = composition,
                        iterations = 1,
                        initialProgress = 0f
                    )
                    delay(3000) // 3 seconds gap
                }
            }
        }
        progress = anim.progress
    } else {
        val standardProgress by animateLottieCompositionAsState(
            composition = composition,
            iterations = LottieConstants.IterateForever
        )
        progress = standardProgress
    }

    val isNotes = animationPath.contains("notes")
    val animationModifier = if (isNotes) {
        Modifier.fillMaxSize().scale(1.3f)
    } else {
        Modifier.fillMaxSize()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (composition != null) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = animationModifier
            )
        }
    }
}

@Composable
fun NotesPlaceholder(modifier: Modifier = Modifier) {
    LottiePlaceholder("App_animations/notes_buffer.json", modifier)
}

@Composable
fun AssignmentPlaceholder(modifier: Modifier = Modifier) {
    LottiePlaceholder("App_animations/assignment_buffer.json", modifier)
}

@Composable
fun PyqPlaceholder(modifier: Modifier = Modifier) {
    LottiePlaceholder("App_animations/pyq_buffer.json", modifier)
}

@Composable
fun CheatSheetPlaceholder(modifier: Modifier = Modifier) {
    LottiePlaceholder("App_animations/cheatsheet_buffer.json", modifier)
}

@Composable
fun VideoPlaceholder(modifier: Modifier = Modifier) {
    LottiePlaceholder("App_animations/video_buffer.json", modifier)
}

@Composable
fun PlaylistPlaceholder(modifier: Modifier = Modifier) {
    LottiePlaceholder("App_animations/video_buffer.json", modifier)
}

@Composable
fun DocumentPlaceholder(
    documentType: String,
    modifier: Modifier = Modifier
) {
    LottiePlaceholder(
        animationPath = documentType.getThumbnailFallbackAnimation(),
        modifier = modifier
    )
}

@Composable
fun DocumentPlaceholder(
    fileType: FileType,
    modifier: Modifier = Modifier
) {
    LottiePlaceholder(
        animationPath = fileType.thumbnailFallbackAnimation,
        modifier = modifier
    )
}

package com.pravor.notessharing.ui.features.trending.components

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.pravor.notessharing.data.repository.DocumentDetailRepository
import com.pravor.notessharing.domain.model.DocumentDetail
import com.pravor.notessharing.domain.model.TrendingNote

@Composable
fun TrendingNoteDiscoveryCard(
    note: TrendingNote,
    detailRepository: DocumentDetailRepository,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onUpvoteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // If metadata fields are already pre-loaded/cached, we can display the content instantly!
    val hasCache = remember(note) { note.uploaderName.isNotBlank() }

    var documentDetail by remember { mutableStateOf<DocumentDetail?>(null) }
    var contributorLevel by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(!hasCache) }

    LaunchedEffect(note.id) {
        if (!hasCache) {
            isLoading = true
            val doc = detailRepository.getDocument(note.id)
            documentDetail = doc
            if (doc != null) {
                contributorLevel = detailRepository.getUploaderContributorLevel(doc.uploaderId)
            }
            isLoading = false
        }
    }

    if (isLoading) {
        TrendingNoteDiscoveryShimmerCard(modifier = modifier)
    } else {
        if (hasCache) {
            TrendingNoteDiscoveryCardContentFromNote(
                note = note,
                isBookmarked = note.isBookmarked,
                onBookmarkClick = onBookmarkClick,
                onClick = onClick,
                isUpvoted = note.isUpvoted,
                onUpvoteClick = onUpvoteClick,
                modifier = modifier
            )
        } else {
            val doc = documentDetail
            if (doc != null) {
                TrendingNoteDiscoveryCardContent(
                    doc = doc,
                    contributorLevel = contributorLevel ?: "Bronze Contributor",
                    isBookmarked = note.isBookmarked,
                    onBookmarkClick = onBookmarkClick,
                    onClick = onClick,
                    isUpvoted = note.isUpvoted,
                    onUpvoteClick = onUpvoteClick,
                    modifier = modifier
                )
            } else {
                TrendingNoteDiscoveryCardContentFallback(
                    note = note,
                    isBookmarked = note.isBookmarked,
                    onBookmarkClick = onBookmarkClick,
                    onClick = onClick,
                    isUpvoted = note.isUpvoted,
                    onUpvoteClick = onUpvoteClick,
                    modifier = modifier
                )
            }
        }
    }
}

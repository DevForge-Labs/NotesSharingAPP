package com.pravor.notessharing.ui.components.trending_components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TrendingNoteStats(
    upvotes: Int,
    bookmarks: Int,
    isBookmarked: Boolean,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDownloadClick: () -> Unit = {}
) {
    var isUpvoted by remember { mutableStateOf(false) }

    val upvoteCount = remember(upvotes, isUpvoted) {
        if (isUpvoted) upvotes + 1 else upvotes
    }
    
    val initialIsBookmarked = remember(bookmarks) { isBookmarked }
    val bookmarkCount = remember(bookmarks, isBookmarked) {
        if (isBookmarked && !initialIsBookmarked) {
            bookmarks + 1
        } else if (!isBookmarked && initialIsBookmarked) {
            (bookmarks - 1).coerceAtLeast(0)
        } else {
            bookmarks
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Downloads
        Row(
            modifier = Modifier.clickable { onDownloadClick() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Downloads",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Upvotes
        Row(
            modifier = Modifier.clickable { isUpvoted = !isUpvoted },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ThumbUp,
                contentDescription = "Upvotes",
                tint = if (isUpvoted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "$upvoteCount",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUpvoted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Bookmarks
        Row(
            modifier = Modifier.clickable { onBookmarkClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = "Bookmarks",
                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

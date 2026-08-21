package com.pravor.notessharing.ui.features.document.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pravor.notessharing.data.repository.UpvoteRepository

@Composable
fun UpvoteButtonSection(
    docId: String,
    initialUpvotes: Int,
    currentUid: String,
    onUpvoteClick: (String) -> Unit,
    onShowRemoveDialog: () -> Unit,
    enabled: Boolean = true
) {
    val upvotesMap by UpvoteRepository.upvotesFlow.collectAsStateWithLifecycle()
    val upvoteCountsMap by UpvoteRepository.upvoteCountsFlow.collectAsStateWithLifecycle()

    val isUpvoted = remember(upvotesMap, docId) {
        upvotesMap[docId] == true
    }
    val upvoteCount = remember(upvoteCountsMap, docId) {
        upvoteCountsMap[docId] ?: initialUpvotes
    }

    val context = LocalContext.current
    val upvoteColor = if (!enabled) {
        Color(0xFF94A3B8)
    } else if (isUpvoted) {
        Color(0xFFFFB74D)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) {
                if (currentUid.isEmpty()) {
                    Toast.makeText(context, "Please sign in to upvote", Toast.LENGTH_SHORT).show()
                    return@clickable
                }
                if (isUpvoted) {
                    onShowRemoveDialog()
                } else {
                    onUpvoteClick(docId)
                }
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ThumbUp,
                contentDescription = "Upvote",
                tint = upvoteColor,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "$upvoteCount",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = upvoteColor
                )
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isUpvoted) "Upvoted" else "Upvote",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = upvoteColor
            )
        )
    }
}

@Composable
fun ShareButtonSection(
    shareLoading: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val shareColor = if (!enabled) {
        Color(0xFF94A3B8)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled && !shareLoading) {
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.height(32.dp)
        ) {
            if (shareLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = shareColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Share",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = shareColor
            )
        )
    }
}

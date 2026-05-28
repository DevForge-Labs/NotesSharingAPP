package com.pravor.notessharing.ui.components.explore_components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pravor.notessharing.model.DiscoverFeedItem

@Composable
fun DiscoverFeedItem(item: DiscoverFeedItem, onClick: () -> Unit = {}) {
    PressScaleSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        onClick = onClick
    ) {
        when (item) {
            is DiscoverFeedItem.Note -> DiscoverRow(
                marker = "N",
                title = item.title,
                subtitle = "${item.subject} | ${item.downloads} downloads",
                icon = Icons.Default.BookmarkBorder
            )
            is DiscoverFeedItem.Video -> DiscoverRow(
                marker = "V",
                title = item.title,
                subtitle = "${item.channelName} | ${item.duration}",
                icon = Icons.Default.PlayArrow
            )
            is DiscoverFeedItem.Collection -> DiscoverRow(
                marker = "C",
                title = item.title,
                subtitle = "${item.resourceCount} resources in this collection",
                icon = Icons.Default.Bookmark
            )
            is DiscoverFeedItem.ContributorPost -> DiscoverRow(
                marker = item.initials,
                title = item.name,
                subtitle = item.message,
                icon = Icons.Default.Star
            )
        }
    }
}

@Composable
private fun DiscoverRow(
    marker: String,
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = marker.take(2),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

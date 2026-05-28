package com.pravor.notessharing.ui.screens.document

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pravor.notessharing.model.DocumentDetail
import com.pravor.notessharing.model.TrendingNote
import com.pravor.notessharing.ui.components.explore_components.TrendingNoteCard

@Composable
fun RelatedDocumentsSection(
    relatedDocuments: List<DocumentDetail>,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Related Documents",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(relatedDocuments) { relatedDoc ->
                val trendingNote = TrendingNote(
                    id = relatedDoc.id,
                    title = relatedDoc.title,
                    subject = relatedDoc.subject,
                    downloads = relatedDoc.downloads,
                    rating = 4.5,
                    upvotes = relatedDoc.upvotes,
                    isBookmarked = false,
                    thumbnailUrl = relatedDoc.thumbnailUrl,
                    thumbnailGenerated = relatedDoc.thumbnailGenerated,
                    thumbnailType = relatedDoc.thumbnailType
                )
                TrendingNoteCard(
                    note = trendingNote,
                    onClick = { onNavigateToDetail(relatedDoc.id) }
                )
            }
        }
    }
}

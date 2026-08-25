package com.pravor.notessharing.ui.features.classroom.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.pravor.notessharing.data.classroom.ClassroomAccount
import com.pravor.notessharing.ui.features.classroom.ClassroomSyncStatus
import com.pravor.notessharing.ui.theme.ElectricBlue
import com.pravor.notessharing.ui.theme.Mint

@Composable
fun ClassroomAccountCard(
    account: ClassroomAccount,
    syncStatus: ClassroomSyncStatus,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF131F2A),
                            Color(0xFF0C141B)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Google Avatar / Letter Avatar Fallback
                AccountAvatar(
                    photoUrl = account.photoUrl,
                    displayName = account.displayName,
                    email = account.email
                )

                // 2. Account Name & Email
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val name = account.displayName?.takeIf { it.isNotBlank() }
                        ?: account.email.substringBefore("@").replace(".", " ").capitalizeWords()

                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = account.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 3. Real Sync / Connected Status Tag
                SyncStatusBadge(syncStatus = syncStatus)
            }
        }
    }
}

@Composable
private fun AccountAvatar(
    photoUrl: String?,
    displayName: String?,
    email: String
) {
    val initial = when {
        !displayName.isNullOrBlank() -> displayName.trim().first().uppercaseChar().toString()
        email.isNotBlank() -> email.trim().first().uppercaseChar().toString()
        else -> "G"
    }

    Surface(
        shape = CircleShape,
        color = ElectricBlue.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.35f)),
        modifier = Modifier.size(44.dp)
    ) {
        if (!photoUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = photoUrl,
                contentDescription = "Google Account Profile",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                error = {
                    LetterAvatarFallback(initial)
                },
                loading = {
                    LetterAvatarFallback(initial)
                }
            )
        } else {
            LetterAvatarFallback(initial)
        }
    }
}

@Composable
private fun LetterAvatarFallback(initial: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ElectricBlue
        )
    }
}

@Composable
private fun SyncStatusBadge(syncStatus: ClassroomSyncStatus) {
    val (bgColor, borderColor, textColor, text) = when (syncStatus) {
        ClassroomSyncStatus.SYNCED -> Quadruple(
            Mint.copy(alpha = 0.14f),
            Mint.copy(alpha = 0.3f),
            Mint,
            "Connected · Synced"
        )
        ClassroomSyncStatus.SYNCING -> Quadruple(
            ElectricBlue.copy(alpha = 0.14f),
            ElectricBlue.copy(alpha = 0.3f),
            ElectricBlue,
            "Syncing..."
        )
        ClassroomSyncStatus.ERROR -> Quadruple(
            Color(0xFFFFA726).copy(alpha = 0.14f),
            Color(0xFFFFA726).copy(alpha = 0.3f),
            Color(0xFFFFA726),
            "Sync issue"
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(0.5.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = textColor,
                modifier = Modifier.size(6.dp)
            ) {}
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { word ->
    word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

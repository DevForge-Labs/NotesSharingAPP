package com.pravor.notessharing.ui.features.video.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pravor.notessharing.data.repository.ReportRepository
import com.pravor.notessharing.data.repository.UpvoteRepository
import com.pravor.notessharing.domain.model.VideoDetail
import com.pravor.notessharing.ui.common.components.Avatar
import com.pravor.notessharing.ui.navigation.LocalSnackbarHostState
import kotlinx.coroutines.launch

@Composable
fun YouTubeThumbnailPlayer(
    youtubeVideoId: String,
    youtubeUrl: String,
    title: String,
    context: Context,
    thumbnailUrl: String? = null,
    youtubeThumbnailUrl: String? = null,
    youtubeResourceType: String = "video",
    youtubePlaylistId: String = "",
    onPlayClick: () -> Unit
) {
    val finalImageUrl = if (!thumbnailUrl.isNullOrBlank()) {
        thumbnailUrl
    } else if (!youtubeThumbnailUrl.isNullOrBlank()) {
        youtubeThumbnailUrl
    } else {
        null
    }

    var hasError by remember { mutableStateOf(finalImageUrl.isNullOrBlank()) }

    LaunchedEffect(finalImageUrl) {
        hasError = finalImageUrl.isNullOrBlank()
    }

    if (hasError) {
        VideoFallbackUI(title = title)
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(22.dp))
                .background(Color.Black)
                .clickable {
                    onPlayClick()
                    launchYouTubeIntent(
                        resourceType = youtubeResourceType,
                        videoId = youtubeVideoId,
                        playlistId = youtubePlaylistId,
                        youtubeUrl = youtubeUrl,
                        context = context
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = finalImageUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = {
                    hasError = true
                }
            )

            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.size(68.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Video",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VideoFallbackUI(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Video unavailable",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun launchYouTubeIntent(
    resourceType: String,
    videoId: String,
    playlistId: String,
    youtubeUrl: String,
    context: Context
) {
    val finalLaunchUrl = if (resourceType == "playlist") {
        if (playlistId.isNotBlank()) "https://www.youtube.com/playlist?list=$playlistId" else youtubeUrl
    } else {
        if (youtubeUrl.isNotBlank()) youtubeUrl else "https://www.youtube.com/watch?v=$videoId"
    }

    if (resourceType == "playlist") {
        val appIntent = Intent(Intent.ACTION_VIEW, finalLaunchUrl.toUri()).apply {
            setPackage("com.google.android.youtube")
        }
        val webIntent = Intent(Intent.ACTION_VIEW, finalLaunchUrl.toUri())

        try {
            context.startActivity(appIntent)
        } catch (e: Exception) {
            try {
                context.startActivity(webIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "No app available to open this playlist link", Toast.LENGTH_SHORT).show()
            }
        }
    } else {
        val appUri = "vnd.youtube:$videoId".toUri()
        val webUri = finalLaunchUrl.toUri()
        val appIntent = Intent(Intent.ACTION_VIEW, appUri)
        val webIntent = Intent(Intent.ACTION_VIEW, webUri)

        try {
            context.startActivity(appIntent)
        } catch (e: Exception) {
            try {
                context.startActivity(webIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "No app available to open this video link", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun VideoInfoCard(
    video: VideoDetail,
    contributorLevel: String,
    currentUid: String,
    onUpvoteClick: (String) -> Unit,
    onShowRemoveUpvoteDialog: () -> Unit,
    onShareClick: () -> Unit,
    shareEnabled: Boolean,
    onReportClick: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "VIDEO",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }

                val reportedMap by ReportRepository.instance.reportedFlow.collectAsStateWithLifecycle()
                val isReported = remember(reportedMap, video.id) {
                    reportedMap[video.id] == true
                }

                IconButton(
                    onClick = {
                        if (isReported && currentUid.isNotEmpty()) {
                            scope.launch {
                                snackbarHostState.showSnackbar("You've already reported this resource.")
                            }
                        } else {
                            onReportClick()
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isReported) Icons.Filled.Flag else Icons.Outlined.Flag,
                        contentDescription = if (isReported) "Already Reported" else "Report Video",
                        tint = if (isReported) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = video.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${video.subject} | ${video.semester}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    VideoUpvoteButtonSection(
                        videoId = video.id,
                        initialUpvotes = video.upvotes,
                        currentUid = currentUid,
                        onUpvoteClick = onUpvoteClick,
                        onShowRemoveDialog = onShowRemoveUpvoteDialog
                    )
                }

                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .width(2.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    VideoShareButtonSection(
                        onClick = onShareClick,
                        enabled = shareEnabled
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                val initials = if (video.uploaderName.isNotBlank()) {
                    video.uploaderName.split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .map { it.first().uppercase() }
                        .joinToString("")
                        .ifBlank { "UN" }
                } else {
                    "UN"
                }

                Avatar(text = initials, modifier = Modifier.size(42.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Uploaded by: ${video.uploaderName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    val badgeColor = when (contributorLevel) {
                        "Gold Contributor" -> Color(0xFFFFD700)
                        "Silver Contributor" -> Color(0xFFC0C0C0)
                        "Bronze Contributor" -> Color(0xFFCD7F32)
                        "Platinum Contributor" -> Color(0xFF00E5FF)
                        else -> Color(0xFFD500F9)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = contributorLevel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RelatedVideoCard(video: VideoDetail, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(200.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                val finalImageUrl = if (!video.thumbnailUrl.isNullOrBlank()) {
                    video.thumbnailUrl
                } else if (!video.youtubeThumbnailUrl.isNullOrBlank()) {
                    video.youtubeThumbnailUrl
                } else {
                    null
                }
                if (!finalImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = finalImageUrl,
                        contentDescription = video.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayCircleFilled,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = video.uploaderName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

fun shareVideo(context: Context, videoUrl: String, videoTitle: String) {
    try {
        val shareText = "Check out this lecture on NoteShare!\n\n$videoUrl"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, videoTitle)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val chooser = Intent.createChooser(intent, "Share Video")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share video", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun VideoUpvoteButtonSection(
    videoId: String,
    initialUpvotes: Int,
    currentUid: String,
    onUpvoteClick: (String) -> Unit,
    onShowRemoveDialog: () -> Unit,
    enabled: Boolean = true
) {
    val upvotesMap by UpvoteRepository.upvotesFlow.collectAsStateWithLifecycle()
    val upvoteCountsMap by UpvoteRepository.upvoteCountsFlow.collectAsStateWithLifecycle()

    val isUpvoted = remember(upvotesMap, videoId) {
        upvotesMap[videoId] == true
    }
    val upvoteCount = remember(upvoteCountsMap, videoId) {
        upvoteCountsMap[videoId] ?: initialUpvotes
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
                    onUpvoteClick(videoId)
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
fun VideoShareButtonSection(
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
            .clickable(enabled = enabled) {
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.height(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share",
                tint = shareColor,
                modifier = Modifier.size(28.dp)
            )
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

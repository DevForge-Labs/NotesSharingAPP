package com.pravor.notessharing.ui.features.classroom.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.pravor.notessharing.data.classroom.ClassroomAccount
import com.pravor.notessharing.ui.features.classroom.ClassroomSyncStatus
import com.pravor.notessharing.ui.theme.ElectricBlue
import kotlin.math.sin

@Composable
fun ClassroomAccountCard(
    account: ClassroomAccount,
    syncStatus: ClassroomSyncStatus,
    onRefreshClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFF1E3245)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF12202C),
                            Color(0xFF0A131A)
                        )
                    )
                )
        ) {
            // 1. Top Section: Avatar, Account Info (Name, Email, Status Sub-row), Refresh Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Large Avatar with Google Profile or Bold Letter
                AccountAvatar(
                    photoUrl = account.photoUrl,
                    displayName = account.displayName,
                    email = account.email
                )

                // Name, Email & Status Sub-Row
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val rawName = account.displayName?.takeIf { it.isNotBlank() }
                        ?: account.email.substringBefore("@").replace(".", " ")
                    val displayName = rawName.uppercase()

                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = account.email,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Status Row: Pill Badge + Status Description Subtitle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SyncStatusPillBadge(syncStatus = syncStatus)

                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )

                        val statusDescription = when (syncStatus) {
                            ClassroomSyncStatus.SYNCED -> "Last synced just now"
                            ClassroomSyncStatus.SYNCING -> "Updating your classes..."
                            ClassroomSyncStatus.ERROR -> "Sync issue encountered"
                        }

                        Text(
                            text = statusDescription,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 2. Animated / Static Wavy Line Sync Indicator across the bottom
            WavySyncIndicator(
                syncStatus = syncStatus,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            )
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
        color = Color(0xFF1E293B),
        border = BorderStroke(1.5.dp, ElectricBlue.copy(alpha = 0.45f)),
        modifier = Modifier.size(54.dp)
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
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            ),
            color = ElectricBlue
        )
    }
}

@Composable
private fun SyncStatusPillBadge(syncStatus: ClassroomSyncStatus) {
    when (syncStatus) {
        ClassroomSyncStatus.SYNCED -> {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0F382A),
                border = BorderStroke(1.dp, Color(0xFF1E6047))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF34D399),
                        modifier = Modifier.size(12.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF0F382A),
                                modifier = Modifier.size(9.dp)
                            )
                        }
                    }
                    Text(
                        text = "Synced",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF34D399)
                    )
                }
            }
        }
        ClassroomSyncStatus.SYNCING -> {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0C383F),
                border = BorderStroke(1.dp, Color(0xFF156575))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "badge_spin")
                    val spinRotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "spinRotation"
                    )
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = Color(0xFF22D3EE),
                        modifier = Modifier
                            .size(12.dp)
                            .rotate(spinRotation)
                    )
                    Text(
                        text = "Syncing",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF22D3EE)
                    )
                }
            }
        }
        ClassroomSyncStatus.ERROR -> {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF381515),
                border = BorderStroke(1.dp, Color(0xFF7F1D1D))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = Color(0xFFF87171),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Sync issue",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFFF87171)
                    )
                }
            }
        }
    }
}

@Composable
private fun WavySyncIndicator(
    syncStatus: ClassroomSyncStatus,
    modifier: Modifier = Modifier
) {
    val isAnimated = syncStatus == ClassroomSyncStatus.SYNCING

    val infiniteTransition = rememberInfiniteTransition(label = "wave_animation")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val waveColor = when (syncStatus) {
        ClassroomSyncStatus.SYNCED -> Color(0xFF10B981)
        ClassroomSyncStatus.SYNCING -> Color(0xFF2DD4BF)
        ClassroomSyncStatus.ERROR -> Color(0xFFF87171)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
    ) {
        val width = size.width
        val height = size.height
        val centerY = height * 0.55f

        val currentPhase = if (isAnimated) phase else 0f

        // 1. Primary Sharp Wave
        val path1 = Path()
        val amp1 = if (isAnimated) 7.dp.toPx() else 3.dp.toPx()
        val freq1 = 2.2f // wave cycles across width

        for (x in 0..width.toInt() step 3) {
            val xF = x.toFloat()
            val angle = (xF / width) * (freq1 * 2 * Math.PI.toFloat()) + currentPhase
            val y = centerY + amp1 * sin(angle)
            if (x == 0) path1.moveTo(xF, y) else path1.lineTo(xF, y)
        }

        drawPath(
            path = path1,
            color = waveColor.copy(alpha = if (isAnimated) 0.85f else 0.45f),
            style = Stroke(width = 1.75.dp.toPx(), cap = StrokeCap.Round)
        )

        // 2. Harmonic Ambient Wave
        val path2 = Path()
        val amp2 = if (isAnimated) 10.dp.toPx() else 2.5.dp.toPx()
        val freq2 = 1.6f
        val phase2 = if (isAnimated) -currentPhase * 0.8f + 1.2f else 1.0f

        for (x in 0..width.toInt() step 3) {
            val xF = x.toFloat()
            val angle = (xF / width) * (freq2 * 2 * Math.PI.toFloat()) + phase2
            val y = centerY + amp2 * sin(angle)
            if (x == 0) path2.moveTo(xF, y) else path2.lineTo(xF, y)
        }

        drawPath(
            path = path2,
            color = waveColor.copy(alpha = if (isAnimated) 0.35f else 0.2f),
            style = Stroke(width = 1.25.dp.toPx(), cap = StrokeCap.Round)
        )

        // 3. Glowing Traveling Nodes / Particles (when animated)
        if (isAnimated) {
            val particleProgress = (phase / (2 * Math.PI.toFloat())) % 1f
            val particleX = particleProgress * width
            val angle = (particleX / width) * (freq1 * 2 * Math.PI.toFloat()) + currentPhase
            val particleY = centerY + amp1 * sin(angle)

            // Outer Radial Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        waveColor.copy(alpha = 0.9f),
                        waveColor.copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    center = Offset(particleX, particleY),
                    radius = 14.dp.toPx()
                ),
                radius = 14.dp.toPx(),
                center = Offset(particleX, particleY)
            )

            // Inner Core Particle
            drawCircle(
                color = Color.White,
                radius = 2.5.dp.toPx(),
                center = Offset(particleX, particleY)
            )

            // Second subtle trailing node on Wave 2
            val particle2Progress = ((particleProgress + 0.5f) % 1f)
            val particle2X = particle2Progress * width
            val angle2 = (particle2X / width) * (freq2 * 2 * Math.PI.toFloat()) + phase2
            val particle2Y = centerY + amp2 * sin(angle2)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        waveColor.copy(alpha = 0.6f),
                        Color.Transparent
                    ),
                    center = Offset(particle2X, particle2Y),
                    radius = 10.dp.toPx()
                ),
                radius = 10.dp.toPx(),
                center = Offset(particle2X, particle2Y)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = 2.dp.toPx(),
                center = Offset(particle2X, particle2Y)
            )
        }
    }
}


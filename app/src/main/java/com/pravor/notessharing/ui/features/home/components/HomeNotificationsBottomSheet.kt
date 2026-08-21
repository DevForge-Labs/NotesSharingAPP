package com.pravor.notessharing.ui.features.home.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pravor.notessharing.domain.model.Notification
import com.pravor.notessharing.domain.model.getRelativeTime
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeNotificationsBottomSheet(
    sheetState: SheetState,
    unreadNotificationsCount: Int,
    visibleNotifications: List<Notification>,
    highlightedNotificationId: String?,
    onDismissRequest: () -> Unit,
    onMarkAllNotificationsRead: () -> Unit,
    onMarkNotificationRead: (String) -> Unit,
    onDeleteNotification: (String) -> Unit,
    onClearAllClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    onVideoClick: (String) -> Unit
) {
    var animatedHighlightId by remember { mutableStateOf<String?>(null) }
    var dismissedNotificationIds by remember { mutableStateOf(setOf<String>()) }

    val filteredNotifications = remember(visibleNotifications, dismissedNotificationIds) {
        visibleNotifications.filter { it.id !in dismissedNotificationIds }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color(0xFF0F172A),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF334155)) }
    ) {
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.75f)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Notifications",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = Color.White
                    )

                    if (unreadNotificationsCount > 0) {
                        TextButton(onClick = onMarkAllNotificationsRead) {
                            Text(
                                text = "Mark all read",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredNotifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .navigationBarsPadding(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = Color(0xFF334155),
                                modifier = Modifier.size(64.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "No notifications yet",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "You'll see updates about notes,\nassignments, PYQs, videos and resources here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    val notificationsListState = rememberLazyListState()

                    LaunchedEffect(highlightedNotificationId, filteredNotifications) {
                        if (!highlightedNotificationId.isNullOrBlank() && filteredNotifications.isNotEmpty()) {
                            val index = filteredNotifications.indexOfFirst { it.id == highlightedNotificationId }
                            if (index >= 0) {
                                val viewportHeight = notificationsListState.layoutInfo.viewportEndOffset
                                val offset = if (viewportHeight > 0) -(viewportHeight / 3) else -300
                                notificationsListState.animateScrollToItem(index, offset)
                                animatedHighlightId = highlightedNotificationId
                                val targetNotification = filteredNotifications[index]
                                if (!targetNotification.read) {
                                    onMarkNotificationRead(targetNotification.id)
                                }
                            }
                        }
                    }

                    LazyColumn(
                        state = notificationsListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(filteredNotifications, key = { it.id }) { notification ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
                                        dismissedNotificationIds = dismissedNotificationIds + notification.id
                                        onDeleteNotification(notification.id)
                                        true
                                    } else {
                                        false
                                    }
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                modifier = Modifier.animateItem(),
                                backgroundContent = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                color = Color(0xFF334155).copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                    )
                                },
                                content = {
                                    NotificationItemRow(
                                        notification = notification,
                                        isHighlighted = notification.id == animatedHighlightId,
                                        onMarkRead = { onMarkNotificationRead(notification.id) },
                                        onNavigate = {
                                            onDismissRequest()
                                            val targetType = notification.type ?: ""
                                            if (targetType.contains("video", ignoreCase = true)) {
                                                onVideoClick(notification.targetId ?: "")
                                            } else {
                                                onDocumentClick(notification.targetId ?: "")
                                            }
                                        }
                                    )
                                }
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(top = 12.dp, bottom = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(onClick = onClearAllClick) {
                            Text(
                                text = "Clear All Notifications",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItemRow(
    notification: Notification,
    isHighlighted: Boolean = false,
    onMarkRead: () -> Unit,
    onNavigate: () -> Unit
) {
    val isLong = notification.message.contains("\n") || notification.message.length > 55
    var isExpanded by remember { mutableStateOf(false) }

    val highlightAlpha = remember { Animatable(if (isHighlighted) 1f else 0f) }

    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            highlightAlpha.snapTo(1f)
            delay(800)
            highlightAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 2200, easing = LinearOutSlowInEasing)
            )
        }
    }

    val highlightColor = MaterialTheme.colorScheme.primary
    val borderStroke = if (highlightAlpha.value > 0f) {
        BorderStroke(
            width = 2.dp,
            color = highlightColor.copy(alpha = highlightAlpha.value)
        )
    } else {
        if (notification.read) BorderStroke(1.dp, Color(0xFF1E293B)) else BorderStroke(1.dp, Color(0xFF334155))
    }

    val baseColor = if (notification.read) Color.Transparent else Color(0xFF1E293B)
    val finalContainerColor = if (highlightAlpha.value > 0f) {
        lerp(
            start = baseColor,
            stop = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
            fraction = highlightAlpha.value
        )
    } else {
        baseColor
    }

    Card(
        onClick = {
            if (isLong) {
                isExpanded = !isExpanded
            } else {
                onMarkRead()
                if (!notification.targetId.isNullOrBlank()) {
                    onNavigate()
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = finalContainerColor
        ),
        border = borderStroke
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (!notification.read) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                        .align(Alignment.CenterVertically)
                )
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                Spacer(modifier = Modifier.width(4.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (notification.read) FontWeight.Medium else FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = if (notification.read) Color(0xFF94A3B8) else Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp
                    ),
                    color = if (notification.read) Color(0xFF64748B) else Color(0xFFCBD5E1),
                    maxLines = if (isLong && !isExpanded) 1 else Int.MAX_VALUE,
                    overflow = if (isLong && !isExpanded) TextOverflow.Ellipsis else TextOverflow.Clip
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getRelativeTime(notification.createdAt),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp
                        ),
                        color = Color(0xFF64748B)
                    )

                    if (isLong && isExpanded) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!notification.read) {
                                TextButton(
                                    onClick = onMarkRead,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = "Mark as Read",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                            if (!notification.targetId.isNullOrBlank()) {
                                TextButton(
                                    onClick = {
                                        onMarkRead()
                                        onNavigate()
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = "Open Resource",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

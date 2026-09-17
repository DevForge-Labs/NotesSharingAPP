package com.pravor.notessharing.ui.features.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.pravor.notessharing.data.local.preferences.TimetablePreferences
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.pravor.notessharing.ui.common.components.SectionHeader
import com.pravor.notessharing.ui.features.home.timetable.TimetableRowItem
import com.pravor.notessharing.ui.features.home.timetable.TimetableSectionUiState

private val TimetableAccent = Color(0xFF818CF8) // Indigo-violet accent
private val ActiveClassGreen = Color(0xFF10B981) // Emerald-green for active class highlight
private val TimeHighlightColor = Color(0xFF38BDF8) // Crisp Sky-Blue to highlight time properly
private val RoomHighlightText = Color(0xFFFBBF24) // Warm Amber-400 text for room
private val RoomHighlightBg = Color(0xFFF59E0B).copy(alpha = 0.16f) // Amber container
private val RoomHighlightBorder = Color(0xFFF59E0B).copy(alpha = 0.38f) // Amber border

@Composable
fun HomeTimetableSection(
    state: TimetableSectionUiState,
    onConnectKayaClick: () -> Unit,
    onRetryClick: () -> Unit = {},
    onReconnectClick: () -> Unit = {},
    onDaySelected: (String) -> Unit = {},
    trailingContent: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionHeader(
            title = "Timetable",
            icon = Icons.Default.CalendarMonth,
            iconTint = TimetableAccent,
            accentColor = TimetableAccent,
            trailingContent = trailingContent
        )

        AnimatedContent(
            targetState = state::class,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
            label = "TimetableSectionStateTransition"
        ) { _ ->
            when (val currentState = state) {
                is TimetableSectionUiState.NotConnected -> {
                    TimetableConnectCard(
                        onConnectClick = onConnectKayaClick
                    )
                }

                is TimetableSectionUiState.Connecting -> {
                    TimetableConnectingCard()
                }

                is TimetableSectionUiState.Error -> {
                    TimetableErrorCard(
                        message = currentState.message,
                        onRetryClick = onRetryClick
                    )
                }

                is TimetableSectionUiState.Success -> {
                    TimetableSuccessCard(
                        state = currentState,
                        onDaySelected = onDaySelected,
                        onReconnectClick = onReconnectClick
                    )
                }
            }
        }
    }
}

/**
 * Empty / First-time state card prompting the user to connect KAYA.
 */
@Composable
private fun TimetableConnectCard(
    onConnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = remember { RoundedCornerShape(20.dp) }
    val isDark = MaterialTheme.colorScheme.surface.hashCode() != 0 // generic theme check

    val cardBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        )
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        border = BorderStroke(1.dp, TimetableAccent.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = TimetableAccent.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, TimetableAccent.copy(alpha = 0.35f)),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = TimetableAccent,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Connect your KAYA account",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "to see your personalized timetable.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = onConnectClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TimetableAccent,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Connect KAYA",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Subtle loading state when KAYA connection is being attempted.
 */
@Composable
private fun TimetableConnectingCard(
    modifier: Modifier = Modifier
) {
    val cardShape = remember { RoundedCornerShape(20.dp) }
    val cardBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        )
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        border = BorderStroke(1.dp, TimetableAccent.copy(alpha = 0.25f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = TimetableAccent,
                strokeWidth = 2.5.dp
            )

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Connecting to KAYA...",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Setting up your connection & timetable...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Error state card with retry action.
 */
@Composable
private fun TimetableErrorCard(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = remember { RoundedCornerShape(20.dp) }
    val errorColor = MaterialTheme.colorScheme.error

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        border = BorderStroke(1.dp, errorColor.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = errorColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Unable to connect to KAYA",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = onRetryClick,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, errorColor.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = errorColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Try Again",
                    style = MaterialTheme.typography.labelMedium,
                    color = errorColor
                )
            }
        }
    }
}

/**
 * Success state displaying glanceable timetable entries.
 * Caps visible height at ~4 rows and enables internal vertical scrolling if > 4 entries.
 */
@Composable
private fun TimetableSuccessCard(
    state: TimetableSectionUiState.Success,
    onDaySelected: (String) -> Unit,
    onReconnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val timetablePrefs = remember(context) { TimetablePreferences.getInstance(context) }
    var containerHeightDp by remember {
        mutableStateOf(timetablePrefs.getStored3RowHeight().dp)
    }
    val rowSpacingPx = with(density) { 6.dp.roundToPx() }
    val additionalSectionBufferPx = with(density) { 8.dp.roundToPx() }
    val measuredRowHeights = remember { mutableStateMapOf<Int, Int>() }

    val onRowHeightMeasured: (Int, Int) -> Unit = remember(density) {
        { index, heightPx ->
            if (heightPx > 0 && measuredRowHeights[index] != heightPx) {
                measuredRowHeights[index] = heightPx

                val calculatedHeightPx = if (measuredRowHeights.size >= 3) {
                    (measuredRowHeights[0] ?: heightPx) +
                    (measuredRowHeights[1] ?: heightPx) +
                    (measuredRowHeights[2] ?: heightPx) +
                    (2 * rowSpacingPx) +
                    additionalSectionBufferPx
                } else {
                    val avgHeightPx = measuredRowHeights.values.average().toInt()
                    (avgHeightPx * 3) + (2 * rowSpacingPx) + additionalSectionBufferPx
                }

                val newHeightDp = with(density) { calculatedHeightPx.toDp() }
                if (kotlin.math.abs(containerHeightDp.value - newHeightDp.value) > 1f) {
                    containerHeightDp = newHeightDp
                    timetablePrefs.store3RowHeight(newHeightDp.value)
                }
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    var is24Hour by remember {
        mutableStateOf(android.text.format.DateFormat.is24HourFormat(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                is24Hour = android.text.format.DateFormat.is24HourFormat(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val cardShape = remember { RoundedCornerShape(20.dp) }
    val cardBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        )
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        border = BorderStroke(1.dp, TimetableAccent.copy(alpha = 0.25f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush)
                .padding(top = 12.dp, bottom = 12.dp)
        ) {
            // Expired session banner if applicable
            if (state.isSessionExpired) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "KAYA needs you to sign in again.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(
                            onClick = onReconnectClick,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Sign in again",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = TimetableAccent
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            // Day Selector & Horizontal Pager State
            val initialPageIndex = remember(state.availableDays) {
                state.availableDays.indexOfFirst { it.equals(state.selectedDay, ignoreCase = true) }
                    .coerceAtLeast(0)
            }
            val pagerState = rememberPagerState(
                initialPage = initialPageIndex,
                pageCount = { state.availableDays.size }
            )
            val coroutineScope = rememberCoroutineScope()
            val dayListState = rememberLazyListState()
            val verticalDayListStates = remember { mutableMapOf<String, LazyListState>() }

            // Synchronize pager swipe -> selected day state
            LaunchedEffect(pagerState.currentPage) {
                val dayAtPage = state.availableDays.getOrNull(pagerState.currentPage)
                if (dayAtPage != null && !dayAtPage.equals(state.selectedDay, ignoreCase = true)) {
                    onDaySelected(dayAtPage)
                }
            }

            // Synchronize external selected day change -> pager
            LaunchedEffect(state.selectedDay) {
                val targetIndex = state.availableDays.indexOfFirst { it.equals(state.selectedDay, ignoreCase = true) }
                if (targetIndex >= 0 && targetIndex != pagerState.currentPage && !pagerState.isScrollInProgress) {
                    pagerState.scrollToPage(targetIndex)
                }
            }

            // Auto-scroll day chips when pager page changes
            LaunchedEffect(pagerState.currentPage) {
                dayListState.animateScrollToItem(pagerState.currentPage)
            }

            // Day Selector Row
            if (state.availableDays.size > 1) {
                LazyRow(
                    state = dayListState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(state.availableDays) { index, day ->
                        val isSelected = index == pagerState.currentPage
                        val shortLabel = day.take(3)
                        DayPillChip(
                            label = shortLabel,
                            isSelected = isSelected,
                            onClick = {
                                if (index != pagerState.currentPage) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                            }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                    thickness = 0.8.dp,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
                Spacer(Modifier.height(6.dp))
            }

            // Off-screen sample row to dynamically pre-measure 3-row height on initial fetch
            if (!timetablePrefs.is3RowHeightStored() && state.allEntries.isNotEmpty() && measuredRowHeights.isEmpty()) {
                Box(
                    modifier = Modifier
                        .size(0.dp)
                        .clipToBounds()
                        .alpha(0f)
                ) {
                    TimetableEntryRow(
                        item = state.allEntries.first(),
                        is24Hour = is24Hour,
                        onHeightMeasured = { h -> onRowHeightMeasured(0, h) }
                    )
                }
            }

            // Continuous Horizontal Pager container locked to dynamically measured 3-row height.
            // Adjacent days pre-positioned and visibly peek into view while dragging.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(containerHeightDp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    pageSpacing = 10.dp
                ) { pageIndex ->
                    val pageDay = state.availableDays.getOrElse(pageIndex) { state.selectedDay }
                    val pageEntries = remember(state.allEntries, pageDay) {
                        state.entriesForDay(pageDay)
                    }

                    val verticalListState = remember(pageDay) {
                        verticalDayListStates.getOrPut(pageDay) {
                            val calendar = java.util.Calendar.getInstance()
                            val currentDay = com.pravor.notessharing.ui.features.home.timetable.TimetableTimeUtils.getCurrentDayName(calendar)
                            val currentMinutes = com.pravor.notessharing.ui.features.home.timetable.TimetableTimeUtils.getCurrentMinutes(calendar)

                            val initialIndex = com.pravor.notessharing.ui.features.home.timetable.TimetableTimeUtils.calculateInitialListIndexForDay(
                                day = pageDay,
                                entries = pageEntries,
                                currentDay = currentDay,
                                currentMinutes = currentMinutes
                            )
                            LazyListState(firstVisibleItemIndex = initialIndex)
                        }
                    }

                    TimetableDayPage(
                        day = pageDay,
                        entries = pageEntries,
                        is24Hour = is24Hour,
                        listState = verticalListState,
                        onRowHeightMeasured = onRowHeightMeasured,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Single day page inside the HorizontalPager.
 * Renders the day's timetable entries as distinct individual cards,
 * with the active class already visible on the very first rendered frame.
 */
@Composable
private fun TimetableDayPage(
    day: String,
    entries: List<TimetableRowItem>,
    is24Hour: Boolean,
    listState: LazyListState,
    onRowHeightMeasured: ((Int, Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) {
        TimetableNoClassesEmptyState(
            day = day,
            modifier = modifier
        )
    } else {
        val coroutineScope = rememberCoroutineScope()
        val canScrollMore by remember {
            derivedStateOf { listState.canScrollForward }
        }

        Box(modifier = modifier) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 0.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(
                    items = entries,
                    key = { _, item -> item.id }
                ) { index, item ->
                    TimetableEntryRow(
                        item = item,
                        is24Hour = is24Hour,
                        onHeightMeasured = if (index < 3) { h ->
                            onRowHeightMeasured?.invoke(index, h)
                        } else null
                    )
                }
            }

            // High-visibility downward indicator with subtle gradient backdrop
            androidx.compose.animation.AnimatedVisibility(
                visible = canScrollMore,
                enter = fadeIn(animationSpec = tween(180)),
                exit = fadeOut(animationSpec = tween(180)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                                )
                            )
                        )
                        .padding(top = 14.dp, bottom = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        border = BorderStroke(1.2.dp, TimetableAccent.copy(alpha = 0.75f)),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable {
                                coroutineScope.launch {
                                    val target = (listState.firstVisibleItemIndex + 2).coerceAtMost(entries.size - 1)
                                    listState.animateScrollToItem(target)
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "More subjects below",
                                tint = TimetableAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Empty state displayed when there are no classes scheduled for the selected day.
 * Renders the custom animated resting baby panda as a background layer with
 * the restored title and subtitle cleanly displayed on top.
 */
@Composable
private fun TimetableNoClassesEmptyState(
    day: String,
    modifier: Modifier = Modifier
) {
    val compositionResult = rememberLottieComposition(
        LottieCompositionSpec.Asset("App_animations/panda_no_class.json")
    )
    val composition = compositionResult.value
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        if (composition != null) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (-14).dp)
                    .scale(2.02f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = "No classes on $day",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(3.dp))

            Text(
                text = "Enjoy your day off or catch up on your studies!",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}


/**
 * Compact day selector pill chip.
 */
@Composable
private fun DayPillChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val chipShape = RoundedCornerShape(10.dp)
    val bgColor = if (isSelected) TimetableAccent else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f)
    val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = chipShape,
        color = bgColor,
        border = if (isSelected) null else BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
            color = textColor
        )
    }
}

/**
 * Single timetable row: Subject on top, Time directly underneath, and Room on the right.
 * Highlights the active class with a compact left green accent bar and subtle background tint.
 */
@Composable
private fun TimetableEntryRow(
    item: TimetableRowItem,
    is24Hour: Boolean = false,
    onHeightMeasured: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val rowShape = RoundedCornerShape(10.dp)
    val rowBackground = if (item.isCurrentClass) {
        ActiveClassGreen.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f)
    }
    val rowBorder = if (item.isCurrentClass) {
        BorderStroke(1.dp, ActiveClassGreen.copy(alpha = 0.45f))
    } else {
        BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f))
    }
    val displayTime = remember(item, is24Hour) { item.formattedTime(is24Hour) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { size ->
                if (size.height > 0) {
                    onHeightMeasured?.invoke(size.height)
                }
            }
            .clip(rowShape)
            .background(rowBackground)
            .border(rowBorder, rowShape)
            .padding(
                start = if (item.isCurrentClass) 8.dp else 10.dp,
                end = 10.dp,
                top = 8.dp,
                bottom = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left accent indicator for currently active class
        if (item.isCurrentClass) {
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ActiveClassGreen)
            )
            Spacer(Modifier.width(8.dp))
        }

        // Left Column: Subject / course name at the top, Time directly underneath
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = item.subjectName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (item.isCurrentClass) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 14.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Distinct highlighted time with schedule clock icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = if (item.isCurrentClass) ActiveClassGreen else TimeHighlightColor,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = displayTime,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.5.sp
                    ),
                    color = if (item.isCurrentClass) ActiveClassGreen else TimeHighlightColor
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        // Right side: Room code, vertically centered with distinct highlight badge
        val roomText = item.room?.takeIf { it.isNotBlank() }?.let {
            it.replace(Regex("""^(?i)room\s*[-:]*\s*"""), "").trim()
        }

        if (!roomText.isNullOrBlank()) {
            val badgeBg = if (item.isCurrentClass) {
                ActiveClassGreen.copy(alpha = 0.16f)
            } else {
                RoomHighlightBg
            }
            val badgeBorder = if (item.isCurrentClass) {
                ActiveClassGreen.copy(alpha = 0.40f)
            } else {
                RoomHighlightBorder
            }
            val badgeTextColor = if (item.isCurrentClass) {
                Color(0xFF34D399)
            } else {
                RoomHighlightText
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = badgeBg,
                border = BorderStroke(0.8.dp, badgeBorder)
            ) {
                Text(
                    text = roomText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    ),
                    color = badgeTextColor,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }
    }
}

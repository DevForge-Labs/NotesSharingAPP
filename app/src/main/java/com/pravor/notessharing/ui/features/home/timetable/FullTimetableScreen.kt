@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.pravor.notessharing.ui.features.home.timetable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.pravor.notessharing.ui.theme.ElectricBlue
import kotlinx.coroutines.launch
import java.util.Calendar

private val TimetableAccent = Color(0xFF818CF8) // Indigo-violet accent
private val ActiveClassGreen = Color(0xFF10B981) // Emerald-green for active class highlight
private val TimeHighlightColor = Color(0xFF38BDF8) // Crisp Sky-Blue
private val RoomHighlightText = Color(0xFFFBBF24) // Warm Amber-400 text
private val RoomHighlightBg = Color(0xFFF59E0B).copy(alpha = 0.16f) // Amber container
private val RoomHighlightBorder = Color(0xFFF59E0B).copy(alpha = 0.38f) // Amber border

@Composable
fun FullTimetableRoute(
    initialDay: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: FullTimetableViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshTimeFormat()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    FullTimetableScreen(
        uiState = uiState,
        onDaySelected = viewModel::onDaySelected,
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun FullTimetableScreen(
    uiState: FullTimetableUiState,
    onDaySelected: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val availableDays = uiState.availableDays

    val initialPageIndex = remember(availableDays, uiState.selectedDay) {
        availableDays.indexOfFirst { it.equals(uiState.selectedDay, ignoreCase = true) }
            .coerceAtLeast(0)
    }

    val pagerState = rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = { availableDays.size }
    )

    val dayListState = rememberLazyListState()

    // Sync pager swipe to ViewModel selected day
    LaunchedEffect(pagerState.currentPage) {
        val dayAtPage = availableDays.getOrNull(pagerState.currentPage)
        if (dayAtPage != null && !dayAtPage.equals(uiState.selectedDay, ignoreCase = true)) {
            onDaySelected(dayAtPage)
        }
    }

    // Sync external selected day to pager
    LaunchedEffect(uiState.selectedDay) {
        val targetIndex = availableDays.indexOfFirst { it.equals(uiState.selectedDay, ignoreCase = true) }
        if (targetIndex >= 0 && targetIndex != pagerState.currentPage && !pagerState.isScrollInProgress) {
            pagerState.scrollToPage(targetIndex)
        }
    }

    // Auto-scroll day chips when pager page changes
    LaunchedEffect(pagerState.currentPage) {
        dayListState.animateScrollToItem(pagerState.currentPage)
    }

    val calendar = remember { Calendar.getInstance() }
    val todayName = remember { TimetableTimeUtils.getCurrentDayName(calendar) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Timetable",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Session expired warning banner
            if (uiState.isSessionExpired) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "KAYA session expired. Return to home to sign in again.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 7-Day Selector Row
            LazyRow(
                state = dayListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(availableDays) { index, day ->
                    val isSelected = index == pagerState.currentPage
                    val isToday = TimetableTimeUtils.isSameDay(day, todayName)

                    FullTimetableDayChip(
                        day = day,
                        isSelected = isSelected,
                        isToday = isToday,
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

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                thickness = 0.8.dp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Continuous HorizontalPager connecting all 7 days
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                pageSpacing = 16.dp
            ) { pageIndex ->
                val pageDay = availableDays.getOrElse(pageIndex) { uiState.selectedDay }
                val pageEntries = remember(uiState.allEntries, pageDay) {
                    uiState.entriesForDay(pageDay)
                }

                FullTimetableDayPage(
                    day = pageDay,
                    entries = pageEntries,
                    is24Hour = uiState.is24Hour,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Weekday Selector Chip supporting all 7 days with app-theme blue badge for Today.
 */
@Composable
private fun FullTimetableDayChip(
    day: String,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chipShape = RoundedCornerShape(14.dp)
    val shortLabel = day.take(3)

    val (bgColor, textColor, borderStroke) = when {
        isSelected && isToday -> Triple(
            ElectricBlue,
            Color(0xFF080A0F), // Ink text on vibrant ElectricBlue
            null
        )
        isSelected -> Triple(
            TimetableAccent,
            Color.White,
            null
        )
        isToday -> Triple(
            ElectricBlue.copy(alpha = 0.18f),
            ElectricBlue,
            BorderStroke(1.2.dp, ElectricBlue.copy(alpha = 0.75f))
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.40f),
            MaterialTheme.colorScheme.onSurfaceVariant,
            BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f))
        )
    }

    Surface(
        shape = chipShape,
        color = bgColor,
        border = borderStroke,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
        ) {
            Text(
                text = shortLabel,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 13.sp
                ),
                color = textColor
            )
        }
    }
}

/**
 * Single day page in the FullTimetable HorizontalPager.
 * Displays enlarged class cards or the enlarged resting panda empty state.
 */
@Composable
private fun FullTimetableDayPage(
    day: String,
    entries: List<TimetableRowItem>,
    is24Hour: Boolean,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) {
        FullTimetableNoClassesEmptyState(
            day = day,
            modifier = modifier
        )
    } else {
        val calendar = remember { Calendar.getInstance() }
        val currentDay = remember { TimetableTimeUtils.getCurrentDayName(calendar) }
        val currentMinutes = remember { TimetableTimeUtils.getCurrentMinutes(calendar) }

        val initialIndex = remember(day, entries) {
            TimetableTimeUtils.calculateInitialListIndexForDay(
                day = day,
                entries = entries,
                currentDay = currentDay,
                currentMinutes = currentMinutes
            )
        }

        val listState = remember(day) {
            LazyListState(firstVisibleItemIndex = initialIndex)
        }

        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(
                items = entries,
                key = { _, item -> item.id }
            ) { _, item ->
                FullTimetableClassCard(
                    item = item,
                    is24Hour = is24Hour
                )
            }
        }
    }
}

/**
 * Enlarged, clean class card for the dedicated full-screen timetable.
 * Displays subject title with time on start edge and room on end edge.
 * Ongoing class is distinguished purely via subtle emerald green color change.
 */
@Composable
private fun FullTimetableClassCard(
    item: TimetableRowItem,
    is24Hour: Boolean,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(18.dp)

    val cardBackground = if (item.isCurrentClass) {
        ActiveClassGreen.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    val cardBorder = if (item.isCurrentClass) {
        BorderStroke(1.5.dp, ActiveClassGreen.copy(alpha = 0.70f))
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
    }

    val displayTime = remember(item, is24Hour) { item.formattedTime(is24Hour) }

    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isCurrentClass) 2.dp else 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Subject Title
            Text(
                text = item.subjectName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    lineHeight = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))

            // Time and Room Number on opposite edges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Time Chip on the left edge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = TimeHighlightColor.copy(alpha = 0.12f),
                    border = BorderStroke(0.8.dp, TimeHighlightColor.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = TimeHighlightColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = displayTime,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp
                            ),
                            color = TimeHighlightColor
                        )
                    }
                }

                // Room Badge on the right edge
                if (!item.room.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = RoomHighlightBg,
                        border = BorderStroke(0.8.dp, RoomHighlightBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MeetingRoom,
                                contentDescription = null,
                                tint = RoomHighlightText,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = item.room,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp
                                ),
                                color = RoomHighlightText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Very large resting baby panda empty state for weekend or class-free days.
 * Centers the Lottie animation and clean typography.
 */
@Composable
private fun FullTimetableNoClassesEmptyState(
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
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (composition != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LottieAnimation(
                        composition = composition,
                        progress = { progress },
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(2.6f)
                    )
                }
            } else {
                Spacer(Modifier.height(260.dp))
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "No classes on $day",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Enjoy your day off or catch up on your studies!",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

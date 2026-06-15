package com.pravor.notessharing.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pravor.notessharing.core.config.DeveloperConfig
import com.pravor.notessharing.ui.components.navigation.WaterBottomBar
import com.pravor.notessharing.ui.navigation.AppDestination

@Composable
fun AppBottomBar(
    destinations: List<AppDestination>,
    currentRoute: String?,
    onDestinationClick: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    if (DeveloperConfig.USE_WATER_NAV) {
        WaterBottomBar(
            destinations = destinations,
            currentRoute = currentRoute,
            onDestinationClick = onDestinationClick,
            modifier = modifier
        )
    } else {
        BottomNavBar(
            destinations = destinations,
            currentRoute = currentRoute,
            onDestinationClick = onDestinationClick,
            modifier = modifier
        )
    }
}

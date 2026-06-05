package com.pravor.notessharing.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.TextAlign
import androidx.glance.GlanceTheme
import androidx.glance.ColorFilter
import android.util.Log
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.currentState
import com.pravor.notessharing.R

class QuickActionsWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Log.d("QuickActionsWidget", "provideGlance() executed")
        val repository = WidgetCountRepository(context)
        val initialBookmarks = repository.getBookmarksCount()
        val initialDownloads = repository.getDownloadsCount()
        
        try {
            updateAppWidgetState(context, id) { prefs ->
                if (prefs[intPreferencesKey("bookmarks_count")] == null) {
                    prefs[intPreferencesKey("bookmarks_count")] = initialBookmarks
                }
                if (prefs[intPreferencesKey("downloads_count")] == null) {
                    prefs[intPreferencesKey("downloads_count")] = initialDownloads
                }
            }
        } catch (e: Exception) {
            Log.e("QuickActionsWidget", "Failed to initialize state in provideGlance: ${e.message}", e)
        }

        provideContent {
            GlanceTheme {
                val prefs = currentState<Preferences>()
                val bookmarksCount = prefs[intPreferencesKey("bookmarks_count")] ?: initialBookmarks
                val downloadsCount = prefs[intPreferencesKey("downloads_count")] ?: initialDownloads
                Log.d(
                    "QuickActionsWidget",
                    "bookmarks=$bookmarksCount downloads=$downloadsCount"
                )
                WidgetContent(
                    context = context,
                    bookmarksCount = bookmarksCount,
                    downloadsCount = downloadsCount
                )
            }
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun WidgetContent(
    context: Context,
    bookmarksCount: Int,
    downloadsCount: Int
) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color(0xFF11151D))) // App Panel background (lighter black)
            .cornerRadius(16.dp)
            .padding(top = 8.dp, bottom = 8.dp, start = 12.dp, end = 12.dp),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.Top
    ) {
        // Branding Row (Logo + App Name)
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalAlignment = Alignment.Horizontal.Start,
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.app_logo_normal),
                contentDescription = "NotesSharing Logo",
                modifier = GlanceModifier.size(16.dp)
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = "NotesSharing",
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color(0xFFFFFFFF)),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        // Actions Row
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            // Bookmark Action
            ActionColumn(
                context = context,
                iconRes = R.drawable.ic_widget_bookmark,
                iconColor = 0xFFD186FF, // Purple/Violet
                label = "Saved",
                count = bookmarksCount.toString(),
                destination = WidgetDestinations.BOOKMARKS
            )

            // Vertical Divider 1
            VerticalDivider()

            // Upload Action
            ActionColumn(
                context = context,
                iconRes = R.drawable.ic_widget_upload,
                iconColor = 0xFF73E0B1, // Green/Mint
                label = "Upload",
                count = null,
                destination = WidgetDestinations.UPLOAD
            )

            // Vertical Divider 2
            VerticalDivider()

            // Download Action
            ActionColumn(
                context = context,
                iconRes = R.drawable.ic_widget_download,
                iconColor = 0xFF7CB7FF, // Blue/ElectricBlue
                label = "Files",
                count = downloadsCount.toString(),
                destination = WidgetDestinations.DOWNLOADS
            )
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun VerticalDivider() {
    Spacer(
        modifier = GlanceModifier
            .width(1.dp)
            .height(40.dp)
            .background(androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color(0xFF242B38))) // App PanelHighest divider color
    )
}

@SuppressLint("RestrictedApi")
@Composable
private fun RowScope.ActionColumn(
    context: Context,
    iconRes: Int,
    iconColor: Long,
    label: String,
    count: String?,
    destination: String
) {
    Column(
        modifier = GlanceModifier
            .defaultWeight()
            .clickable(actionStartActivity(WidgetActions.createClickIntent(context, destination))),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        // Icon directly on card background (no background container box)
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = label,
            modifier = GlanceModifier.size(24.dp),
            colorFilter = ColorFilter.tint(androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color(iconColor)))
        )
        
        Spacer(modifier = GlanceModifier.height(2.dp))
        
        // Label
        Text(
            text = label,
            style = TextStyle(
                color = androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color(0xFFA8B1C0)), // Muted grey
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        )
        
        if (count != null) {
            Spacer(modifier = GlanceModifier.height(1.dp))
            // Count underneath
            Text(
                text = count,
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color(iconColor)), // Same as icon color
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
        } else {
            // Empty placeholder spacer to keep all columns perfectly aligned
            Spacer(modifier = GlanceModifier.height(13.dp))
        }
    }
}

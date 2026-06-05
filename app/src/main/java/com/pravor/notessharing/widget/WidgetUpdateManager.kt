package com.pravor.notessharing.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WidgetUpdateManager {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun updateAllWidgets(context: Context) {
        val caller = Throwable().stackTrace.getOrNull(1)?.let { "${it.className}.${it.methodName}:${it.lineNumber}" } ?: "unknown"
        Log.d("WidgetUpdateManager", "updateAllWidgets() called by $caller at ${java.lang.System.currentTimeMillis()}")
        scope.launch {
            try {
                val appContext = context.applicationContext
                val repository = WidgetCountRepository(appContext)
                val bookmarks = repository.getBookmarksCount()
                val downloads = repository.getDownloadsCount()

                val manager = GlanceAppWidgetManager(appContext)
                val glanceIds = manager.getGlanceIds(QuickActionsWidget::class.java)
                glanceIds.forEach { glanceId ->
                    try {
                        updateAppWidgetState(appContext, glanceId) { prefs ->
                            prefs[intPreferencesKey("bookmarks_count")] = bookmarks
                            prefs[intPreferencesKey("downloads_count")] = downloads
                        }
                        QuickActionsWidget().update(appContext, glanceId)
                    } catch (e: Exception) {
                        Log.e("WidgetUpdateManager", "Failed to update state for widget ID $glanceId: ${e.message}", e)
                    }
                }
                Log.d("WidgetUpdateManager", "updateAllWidgets() executed successfully: bookmarks=$bookmarks downloads=$downloads")
            } catch (e: Exception) {
                Log.e("WidgetUpdateManager", "Failed to update widget: ${e.message}", e)
            }
        }
    }
}

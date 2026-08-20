package com.pravor.notessharing.core.widget

import com.pravor.notessharing.data.repository.WidgetCountRepository

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pravor.notessharing.data.repository.ContinueLearningRepository
import com.pravor.notessharing.domain.model.FileType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WidgetUpdateManager {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun updateAllWidgets(context: Context) {
        val startTime = System.currentTimeMillis()
        android.util.Log.d("PERF", "[PERF] Widget update START thread=${Thread.currentThread().name}")
        val caller = Throwable().stackTrace.getOrNull(1)?.let { "${it.className}.${it.methodName}:${it.lineNumber}" } ?: "unknown"
        Log.d("WidgetDebug", "WidgetUpdateManager.updateAllWidgets() called by $caller at ${java.lang.System.currentTimeMillis()}")
        scope.launch {
            try {
                val appContext = context.applicationContext
                val repository = WidgetCountRepository(appContext)
                val bookmarks = repository.getBookmarksCount()
                val downloads = repository.getDownloadsCount()

                val manager = GlanceAppWidgetManager(appContext)
                val glanceIds = manager.getGlanceIds(QuickActionsWidget::class.java)
                Log.d("WidgetDebug", "WidgetUpdateManager: Found ${glanceIds.size} instances of QuickActionsWidget")
                glanceIds.forEach { glanceId ->
                    try {
                        updateAppWidgetState(appContext, glanceId) { prefs ->
                            prefs[intPreferencesKey("bookmarks_count")] = bookmarks
                            prefs[intPreferencesKey("downloads_count")] = downloads
                        }
                        Log.d("WidgetDebug", "WidgetUpdateManager: Updating QuickActionsWidget instance $glanceId")
                        QuickActionsWidget().update(appContext, glanceId)
                    } catch (e: Exception) {
                        Log.e("WidgetDebug", "WidgetUpdateManager: Failed to update state for widget ID $glanceId: ${e.message}", e)
                    }
                }

                // Fetch the latest continue learning item from the repository
                val learningRepository = ContinueLearningRepository(appContext)
                val item = learningRepository.getLastOpened()
                Log.d("WidgetDebug", "WidgetUpdateManager: ContinueLearningRepository.getLastOpened() returned item: id=${item?.id}, title=${item?.title}, type=${item?.fileType}")

                val learningGlanceIds = manager.getGlanceIds(ContinueLearningWidget::class.java)
                Log.d("WidgetDebug", "WidgetUpdateManager: Found ${learningGlanceIds.size} instances of ContinueLearningWidget")
                learningGlanceIds.forEach { glanceId ->
                    try {
                        updateAppWidgetState(appContext, glanceId) { prefs ->
                            if (item != null) {
                                prefs[booleanPreferencesKey("learning_has_item")] = true
                                prefs[stringPreferencesKey("learning_id")] = item.id
                                prefs[stringPreferencesKey("learning_type")] = if (item.fileType == FileType.Video) "video" else "document"
                                prefs[stringPreferencesKey("learning_title")] = item.title
                                prefs[stringPreferencesKey("learning_subject")] = item.subject ?: "General"
                                prefs[stringPreferencesKey("learning_youtube_video_id")] = item.youtubeVideoId ?: ""
                                prefs[stringPreferencesKey("learning_timestamp")] = item.uploadDate
                                prefs[stringPreferencesKey("learning_uploader_name")] = item.uploaderName
                                prefs[stringPreferencesKey("learning_thumbnail_url")] = item.thumbnailUrl ?: ""
                                prefs[stringPreferencesKey("learning_youtube_thumbnail_url")] = item.youtubeThumbnailUrl ?: ""
                                prefs[stringPreferencesKey("learning_document_type")] = item.documentType ?: ""
                                prefs[stringPreferencesKey("learning_file_type")] = item.fileType.name
                            } else {
                                prefs[booleanPreferencesKey("learning_has_item")] = false
                            }
                        }
                        Log.d("WidgetDebug", "WidgetUpdateManager: Updating ContinueLearningWidget instance $glanceId")
                        ContinueLearningWidget().update(appContext, glanceId)
                        Log.d("WidgetDebug", "WidgetUpdateManager: Successfully called update() for ContinueLearningWidget instance $glanceId")
                    } catch (e: Exception) {
                        Log.e("WidgetDebug", "WidgetUpdateManager: Failed to update ContinueLearningWidget instance $glanceId: ${e.message}", e)
                    }
                }
                Log.d("WidgetDebug", "WidgetUpdateManager: updateAllWidgets() executed successfully: bookmarks=$bookmarks downloads=$downloads")
                val duration = System.currentTimeMillis() - startTime
                android.util.Log.d("PERF", "[PERF] Widget update END duration=${duration}ms thread=${Thread.currentThread().name}")
            } catch (e: Exception) {
                Log.e("WidgetDebug", "WidgetUpdateManager: Failed to update widgets: ${e.message}", e)
                val duration = System.currentTimeMillis() - startTime
                android.util.Log.d("PERF", "[PERF] Widget update END duration=${duration}ms thread=${Thread.currentThread().name}")
            }
        }
    }
}

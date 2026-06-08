package com.pravor.notessharing.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.Log
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
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
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
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.currentState
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.state.updateAppWidgetState
import com.pravor.notessharing.MainActivity
import com.pravor.notessharing.R
import com.pravor.notessharing.data.ContinueLearningRepository
import com.pravor.notessharing.model.FeedItem
import com.pravor.notessharing.model.FileType
import com.pravor.notessharing.ui.components.home_components.formatRelativeTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import androidx.glance.appwidget.SizeMode

class ContinueLearningWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val startTime = System.currentTimeMillis()
        android.util.Log.d("PERF", "[PERF] Widget update START thread=${Thread.currentThread().name}")
        Log.d("WidgetDebug", "ContinueLearningWidget: [TIMESTAMP STARTED] provideGlance() triggered at $startTime for ID $id")
        
        val repository = ContinueLearningRepository(context)
        val initialItem = repository.getLastOpened()
        
        try {
            updateAppWidgetState(context, id) { prefs ->
                if (prefs[KEY_HAS_ITEM] == null) {
                    Log.d("WidgetDebug", "ContinueLearningWidget: Initializing datastore state in provideGlance")
                    if (initialItem != null) {
                        prefs[KEY_HAS_ITEM] = true
                        prefs[KEY_ID] = initialItem.id
                        prefs[KEY_TYPE] = if (initialItem.fileType == FileType.Video) "video" else "document"
                        prefs[KEY_TITLE] = initialItem.title
                        prefs[KEY_SUBJECT] = initialItem.subject ?: "General"
                        prefs[KEY_YOUTUBE_VIDEO_ID] = initialItem.youtubeVideoId ?: ""
                        prefs[KEY_TIMESTAMP] = initialItem.uploadDate
                        prefs[KEY_UPLOADER_NAME] = initialItem.uploaderName
                        prefs[KEY_THUMBNAIL_URL] = initialItem.thumbnailUrl ?: ""
                        prefs[KEY_YOUTUBE_THUMBNAIL_URL] = initialItem.youtubeThumbnailUrl ?: ""
                        prefs[KEY_DOCUMENT_TYPE] = initialItem.documentType ?: ""
                        prefs[KEY_FILE_TYPE] = initialItem.fileType.name
                    } else {
                        prefs[KEY_HAS_ITEM] = false
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WidgetDebug", "ContinueLearningWidget: Failed to initialize state: ${e.message}", e)
        }

        provideContent {
            GlanceTheme {
                val prefs = currentState<Preferences>()
                val composeStartTime = System.currentTimeMillis()
                Log.d("WidgetDebug", "ContinueLearningWidget: [TIMESTAMP COMPOSE] provideContent block executed at $composeStartTime")
                
                val hasItem = prefs[KEY_HAS_ITEM] ?: false
                val item = if (hasItem) {
                    val fileTypeVal = try {
                        FileType.valueOf(prefs[KEY_FILE_TYPE] ?: "Pdf")
                    } catch (e: Exception) {
                        FileType.Pdf
                    }
                    FeedItem(
                        id = prefs[KEY_ID] ?: "",
                        uploaderName = prefs[KEY_UPLOADER_NAME] ?: "",
                        uploaderInitials = "AN",
                        uploadDate = prefs[KEY_TIMESTAMP] ?: "",
                        title = prefs[KEY_TITLE] ?: "",
                        description = prefs[KEY_SUBJECT] ?: "",
                        tags = emptyList(),
                        fileType = fileTypeVal,
                        upvotes = 0,
                        comments = 0,
                        downloads = 0,
                        isUpvoted = false,
                        isSaved = false,
                        bookmarksCount = 0,
                        youtubeVideoId = prefs[KEY_YOUTUBE_VIDEO_ID],
                        youtubeUrl = null,
                        thumbnailUrl = prefs[KEY_THUMBNAIL_URL],
                        thumbnailGenerated = null,
                        thumbnailType = null,
                        thumbnailUrls = emptyList(),
                        documentType = prefs[KEY_DOCUMENT_TYPE],
                        type = prefs[KEY_TYPE],
                        subject = prefs[KEY_SUBJECT],
                        examYear = null,
                        section = null,
                        sectionDisplay = null,
                        youtubeThumbnailUrl = prefs[KEY_YOUTUBE_THUMBNAIL_URL]
                    )
                } else {
                    null
                }

                // Reactive state-driven thumbnail loading inside the composition block
                val bitmapState = androidx.compose.runtime.produceState<Bitmap?>(initialValue = null, item) {
                    val widgetThumbStart = System.currentTimeMillis()
                    android.util.Log.d("PERF", "[PERF] MainThreadWork START operation=Widget thumbnail generation thread=${Thread.currentThread().name}")
                    value = getThumbnailBitmap(context, item)
                    val widgetThumbDuration = System.currentTimeMillis() - widgetThumbStart
                    android.util.Log.d("PERF", "[PERF] MainThreadWork END operation=Widget thumbnail generation duration=${widgetThumbDuration}ms thread=${Thread.currentThread().name}")
                }
                val bitmap = bitmapState.value ?: generateFallbackBitmap(context, item?.fileType?.name ?: "pdf", item?.subject ?: "General")

                WidgetContent(
                    context = context,
                    item = item,
                    thumbnailBitmap = bitmap
                )
            }
        }
        val duration = System.currentTimeMillis() - startTime
        android.util.Log.d("PERF", "[PERF] Widget update END duration=${duration}ms thread=${Thread.currentThread().name}")
    }

    private suspend fun getThumbnailBitmap(context: Context, item: FeedItem?): Bitmap {
        val startTime = System.currentTimeMillis()
        if (item == null) {
            return generateFallbackBitmap(context, "pdf", "General")
        }
        val isVideo = item.fileType == FileType.Video
        val rawDocType = (item.documentType ?: item.type)
            ?.lowercase(java.util.Locale.ROOT)?.trim() ?: ""

        val imageUrl = if (!item.thumbnailUrl.isNullOrBlank()) {
            item.thumbnailUrl
        } else if (!item.youtubeThumbnailUrl.isNullOrBlank()) {
            item.youtubeThumbnailUrl
        } else {
            null
        }

        Log.d("WidgetDebug", "getThumbnailBitmap: item=${item.id}, imageUrl=$imageUrl")

        if (imageUrl != null) {
            val cacheDir = File(context.cacheDir, "continue-learning")
            val localFile = File(cacheDir, "${item.id}.jpg")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            if (localFile.exists()) {
                try {
                    val decodeStartTime = System.currentTimeMillis()
                    android.util.Log.d("PERF", "[PERF] MainThreadWork START operation=Bitmap decoding thread=${Thread.currentThread().name}")
                    val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
                    val decodeDuration = System.currentTimeMillis() - decodeStartTime
                    android.util.Log.d("PERF", "[PERF] MainThreadWork END operation=Bitmap decoding duration=${decodeDuration}ms thread=${Thread.currentThread().name}")
                    if (bitmap != null) {
                        Log.d("WidgetDebug", "getThumbnailBitmap: Loaded from cache instantly. Size: ${bitmap.width}x${bitmap.height}, duration: ${System.currentTimeMillis() - startTime}ms")
                        return bitmap
                    }
                } catch (e: Exception) {
                    localFile.delete()
                }
            }

            // Cache miss: Launch background download to prevent widget refresh delays
            if (activeDownloads.add(item.id)) {
                Log.d("WidgetDebug", "getThumbnailBitmap: Cache miss. Launching background download for item ${item.id}")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        var connection: HttpURLConnection? = null
                        var downloadSuccess = false
                        try {
                            val url = URL(imageUrl)
                            connection = url.openConnection() as HttpURLConnection
                            connection.connectTimeout = 3000
                            connection.readTimeout = 3000
                            connection.requestMethod = "GET"
                            connection.connect()

                            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                                connection.inputStream.use { input ->
                                    FileOutputStream(localFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                downloadSuccess = true
                                Log.d("WidgetDebug", "getThumbnailBitmap: Background download completed successfully for item ${item.id}")
                            } else {
                                Log.w("WidgetDebug", "getThumbnailBitmap: Background download failed with code ${connection.responseCode}")
                            }
                        } catch (e: Exception) {
                            Log.e("WidgetDebug", "getThumbnailBitmap: Background download error: ${e.message}")
                        } finally {
                            connection?.disconnect()
                            activeDownloads.remove(item.id)
                        }

                        if (downloadSuccess && localFile.exists()) {
                            Log.d("WidgetDebug", "getThumbnailBitmap: Background download succeeded. Triggering widget refresh.")
                            WidgetUpdateManager.updateAllWidgets(context)
                        }
                    } catch (e: Exception) {
                        activeDownloads.remove(item.id)
                    }
                }
            } else {
                Log.d("WidgetDebug", "getThumbnailBitmap: Download already in progress for item ${item.id}")
            }
        }

        // Return fallback placeholder immediately to guarantee instant drawing while the image is loading
        val itemType = when {
            isVideo -> "video"
            rawDocType.contains("pyq") -> "pyq"
            rawDocType.contains("assignment") -> "assignment"
            rawDocType.contains("cheat") || rawDocType.contains("formula") -> "cheatsheet"
            rawDocType.contains("notes") -> "notes"
            else -> "pdf"
        }
        val fallback = generateFallbackBitmap(context, itemType, item.subject ?: "General")
        Log.d("WidgetDebug", "getThumbnailBitmap: Returning fallback for type $itemType. Size: ${fallback.width}x${fallback.height}, duration: ${System.currentTimeMillis() - startTime}ms")
        return fallback
    }

    private fun generateFallbackBitmap(context: Context, itemType: String, subject: String): Bitmap {
        val width = 200
        val height = 140
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Gradient color palettes mapped to content types
        val (startColor, endColor, typeLabel) = when (itemType.lowercase()) {
            "video" -> Triple(0xFF34495E.toInt(), 0xFF1A252F.toInt(), "VIDEO")
            "notes" -> Triple(0xFF13201F.toInt(), 0xFF0C1312.toInt(), "NOTES")
            "assignment" -> Triple(0xFF141F23.toInt(), 0xFF0C1316.toInt(), "ASSIGNMENT")
            "pyq" -> Triple(0xFF241C15.toInt(), 0xFF16110D.toInt(), "PYQ")
            "cheatsheet" -> Triple(0xFF1E1724.toInt(), 0xFF120E16.toInt(), "CHEAT SHEET")
            else -> Triple(0xFF1D2124.toInt(), 0xFF111315.toInt(), "PDF")
        }

        // Background paint with gradient
        val bgPaint = Paint().apply {
            isAntiAlias = true
            shader = LinearGradient(0f, 0f, 0f, height.toFloat(), startColor, endColor, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Type label text
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = 0xDEFFFFFF.toInt()
            textSize = 28f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(typeLabel, width / 2f, height / 2f + 10f, textPaint)

        // Subject subtext
        val subPaint = Paint().apply {
            isAntiAlias = true
            color = 0x99FFFFFF.toInt()
            textSize = 14f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val cleanSubj = if (subject.length > 20) subject.take(18) + "..." else subject
        canvas.drawText(cleanSubj, width / 2f, height / 2f + 34f, subPaint)

        return bitmap
    }

    companion object {
        val KEY_HAS_ITEM = booleanPreferencesKey("learning_has_item")
        val KEY_ID = stringPreferencesKey("learning_id")
        val KEY_TYPE = stringPreferencesKey("learning_type")
        val KEY_TITLE = stringPreferencesKey("learning_title")
        val KEY_SUBJECT = stringPreferencesKey("learning_subject")
        val KEY_YOUTUBE_VIDEO_ID = stringPreferencesKey("learning_youtube_video_id")
        val KEY_TIMESTAMP = stringPreferencesKey("learning_timestamp")
        val KEY_UPLOADER_NAME = stringPreferencesKey("learning_uploader_name")
        val KEY_THUMBNAIL_URL = stringPreferencesKey("learning_thumbnail_url")
        val KEY_YOUTUBE_THUMBNAIL_URL = stringPreferencesKey("learning_youtube_thumbnail_url")
        val KEY_DOCUMENT_TYPE = stringPreferencesKey("learning_document_type")
        val KEY_FILE_TYPE = stringPreferencesKey("learning_file_type")

        private val activeDownloads = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun WidgetContent(
    context: Context,
    item: FeedItem?,
    thumbnailBitmap: Bitmap
) {
    val localSize = androidx.glance.LocalSize.current
    val brandingHeight = 14.dp
    val titleHeight = 12.dp
    val dividerHeight = 1.dp
    val contentHeight = 56.dp
    val paddingHeight = 12.dp
    val spacersHeight = 7.dp
    val estimatedConsumedHeight = brandingHeight + titleHeight + dividerHeight + contentHeight + paddingHeight + spacersHeight

    Log.d("WidgetDebug", "ContinueLearningWidget: WidgetContent recomposed/drawn at ${System.currentTimeMillis()}")
    Log.d("WidgetDebug", "ContinueLearningWidget layout: LocalSize received = width=${localSize.width}, height=${localSize.height}")
    Log.d("WidgetDebug", "ContinueLearningWidget layout: Thumbnail bitmap dimensions = ${thumbnailBitmap.width}x${thumbnailBitmap.height}")
    Log.d("WidgetDebug", "ContinueLearningWidget layout: Estimated consumed layout height = $estimatedConsumedHeight, Available height = ${localSize.height}")

    val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
    val info = appWidgetManager.getInstalledProviders().find { 
        it.provider.packageName == context.packageName && it.provider.className == ContinueLearningWidgetReceiver::class.java.name 
    }
    val providerMinHeight = info?.minHeight ?: -1
    val providerMinResizeHeight = info?.minResizeHeight ?: -1
    val chosenSizeBucket = ContinueLearningWidget().sizeMode.toString()

    Log.d("WidgetDebug", "ContinueLearningWidget: LocalSize width = ${localSize.width}")
    Log.d("WidgetDebug", "ContinueLearningWidget: LocalSize height = ${localSize.height}")
    Log.d("WidgetDebug", "ContinueLearningWidget: Widget provider minHeight = ${providerMinHeight}dp")
    Log.d("WidgetDebug", "ContinueLearningWidget: Widget provider resize height = ${providerMinResizeHeight}dp")
    Log.d("WidgetDebug", "ContinueLearningWidget: Chosen Glance size bucket = $chosenSizeBucket")

    val isCompact = localSize.height < 90.dp
    val isMedium = localSize.height >= 90.dp && localSize.height < 140.dp
    val isFull = localSize.height >= 140.dp

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color(0xFF11151D))) // App Panel background
            .cornerRadius(16.dp)
            .padding(
                top = if (isCompact) 4.dp else 10.dp,
                bottom = if (isCompact) 4.dp else 10.dp,
                start = if (isCompact) 8.dp else 12.dp,
                end = if (isCompact) 8.dp else 12.dp
            ),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = if (isCompact) Alignment.Vertical.CenterVertically else Alignment.Vertical.Top
    ) {
        if (!isCompact) {
            // Branding Row (Logo + App Name)
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
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
                        color = androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color(0xFFA8B1C0)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            // Persistent Widget Title below branding
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Horizontal.Start,
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = "Continue Learning",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color(0xFFE2E8F0)),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            // Horizontal Divider
            Spacer(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color(0xFF242B38)))
            )

            Spacer(modifier = GlanceModifier.height(if (isFull) 12.dp else 6.dp))
        }

        if (item == null) {
            // Empty State
            val appIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .clickable(actionStartActivity(appIntent)),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = "No recently opened items",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color(0xFFE2E8F0)),
                        fontSize = if (isFull) 13.sp else 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                if (!isCompact) {
                    Spacer(modifier = GlanceModifier.height(if (isFull) 4.dp else 2.dp))
                    Text(
                        text = "Your recently viewed resources will appear here.",
                        style = TextStyle(
                            color = androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color(0xFFA8B1C0)),
                            fontSize = if (isFull) 11.sp else 9.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        } else {
            // Main Content Row / Column
            val isVideo = item.fileType == FileType.Video
            val clickIntent = Intent(context, MainActivity::class.java).apply {
                action = "com.pravor.notessharing.widget.ACTION_CONTINUE_LEARNING"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                if (isVideo) {
                    putExtra("video_id", item.id)
                } else {
                    putExtra("document_id", item.id)
                }
            }

            if (isFull) {
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                        .clickable(actionStartActivity(clickIntent)),
                    horizontalAlignment = Alignment.Horizontal.Start,
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    // Substantially Increased Thumbnail Size (180x140.dp) to visually anchor the left side
                    Box(
                        modifier = GlanceModifier
                            .size(width = 180.dp, height = 140.dp)
                            .cornerRadius(12.dp)
                    ) {
                        Image(
                            provider = ImageProvider(thumbnailBitmap),
                            contentDescription = item.title,
                            modifier = GlanceModifier.fillMaxSize(),
                            contentScale = androidx.glance.layout.ContentScale.Crop
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(16.dp))

                    // Title, badge, relative timestamp, and CTA on the right
                    Column(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .height(140.dp), // Match thumbnail height exactly
                        verticalAlignment = Alignment.Vertical.Top,
                        horizontalAlignment = Alignment.Horizontal.Start
                    ) {
                        // Badge Row
                        val rawDocType = (item.documentType ?: item.type)
                            ?.lowercase(java.util.Locale.ROOT)?.trim() ?: ""
                        val isPyq = rawDocType.contains("pyq")
                        val isAssignment = rawDocType.contains("assignment")
                        val isCheatSheet = rawDocType.contains("cheat") || rawDocType.contains("formula")
                        val isNotes = rawDocType.contains("notes")
                        val badgeText = when {
                            isVideo -> "YouTube Video"
                            isNotes -> "Notes"
                            isPyq -> "PYQ"
                            isAssignment -> "Assignment"
                            isCheatSheet -> "Cheat Sheet"
                            else -> "PDF"
                        }
                        val badgeColor = when {
                            isVideo -> 0xFFFF6B6B
                            isNotes -> 0xFF58D6D1
                            isPyq -> 0xFFFFB45C
                            isAssignment -> 0xFF7AD7FF
                            isCheatSheet -> 0xFFC7A6FF
                            else -> 0xFFCFD8DC
                        }

                        Row(
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            Text(
                                text = badgeText.uppercase(),
                                style = TextStyle(
                                    color = androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color(badgeColor)),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Text(
                                text = item.subject ?: "General",
                                style = TextStyle(
                                    color = androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color(0xFFA8B1C0)),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(6.dp))

                        // Title with increased prominence
                        Text(
                            text = item.title,
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color(0xFFFFFFFF)),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 3
                        )

                        Spacer(modifier = GlanceModifier.height(4.dp))

                        // Relative Time
                        val lastOpenedText = formatRelativeTime(item.uploadDate, isVideo)
                        Text(
                            text = lastOpenedText,
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color(0xFF808080)),
                                fontSize = 9.sp
                            )
                        )

                        // Pushes CTA to the bottom of the right column
                        Spacer(modifier = GlanceModifier.defaultWeight())

                        // Continue Learning CTA Button aligned at bottom-right
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Horizontal.End,
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            Box(
                                modifier = GlanceModifier
                                    .background(androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color(0xFF2563EB))) // Premium blue accent
                                    .cornerRadius(8.dp)
                                    .clickable(actionStartActivity(clickIntent))
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Continue Learning →",
                                    style = TextStyle(
                                        color = androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color(0xFFFFFFFF)),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .clickable(actionStartActivity(clickIntent)),
                    horizontalAlignment = Alignment.Horizontal.Start,
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(width = 80.dp, height = if (isCompact) 48.dp else 56.dp)
                            .cornerRadius(8.dp)
                    ) {
                        Image(
                            provider = ImageProvider(thumbnailBitmap),
                            contentDescription = item.title,
                            modifier = GlanceModifier.fillMaxSize(),
                            contentScale = androidx.glance.layout.ContentScale.Crop
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(8.dp))

                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                        verticalAlignment = Alignment.Vertical.CenterVertically,
                        horizontalAlignment = Alignment.Horizontal.Start
                    ) {
                        val rawDocType = (item.documentType ?: item.type)
                            ?.lowercase(java.util.Locale.ROOT)?.trim() ?: ""
                        val isPyq = rawDocType.contains("pyq")
                        val isAssignment = rawDocType.contains("assignment")
                        val isCheatSheet = rawDocType.contains("cheat") || rawDocType.contains("formula")
                        val isNotes = rawDocType.contains("notes")
                        val badgeText = when {
                            isVideo -> "YouTube Video"
                            isNotes -> "Notes"
                            isPyq -> "PYQ"
                            isAssignment -> "Assignment"
                            isCheatSheet -> "Cheat Sheet"
                            else -> "PDF"
                        }
                        val badgeColor = when {
                            isVideo -> 0xFFFF6B6B
                            isNotes -> 0xFF58D6D1
                            isPyq -> 0xFFFFB45C
                            isAssignment -> 0xFF7AD7FF
                            isCheatSheet -> 0xFFC7A6FF
                            else -> 0xFFCFD8DC
                        }

                        Row(
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            Text(
                                text = badgeText.uppercase(),
                                style = TextStyle(
                                    color = androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color(badgeColor)),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.width(4.dp))
                            Text(
                                text = item.subject ?: "General",
                                style = TextStyle(
                                    color = androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color(0xFFA8B1C0)),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(2.dp))

                        Text(
                            text = item.title,
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color(0xFFFFFFFF)),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = if (isCompact) 1 else 2
                        )

                        Spacer(modifier = GlanceModifier.height(2.dp))

                        val lastOpenedText = formatRelativeTime(item.uploadDate, isVideo)
                        Text(
                            text = lastOpenedText,
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color(0xFF808080)),
                                fontSize = 8.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

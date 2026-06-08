package com.pravor.notessharing.ui.components.pdfViewing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PdfViewerContent(
    pdfFile: File,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(pdfFile) {
        android.util.Log.d("PDF_DEBUG", "Rendering PDF")
        android.util.Log.d("PDF_DEBUG", "Source=${pdfFile.absolutePath}")
    }

    val pdfRenderer = remember(pdfFile) {
        try {
            val fileDescriptor = ParcelFileDescriptor.open(
                pdfFile,
                ParcelFileDescriptor.MODE_READ_ONLY
            )
            PdfRenderer(fileDescriptor)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    DisposableEffect(pdfRenderer) {
        onDispose {
            try {
                pdfRenderer?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val changes = event.changes
                        val isAnyConsumed = changes.any { it.isConsumed }
                        
                        if (!isAnyConsumed) {
                            if (changes.size >= 2) {
                                val zoomChange = event.calculateZoom()
                                val centroid = event.calculateCentroid(useCurrent = false)
                                if (zoomChange != 1f) {
                                    val oldScale = scale
                                    val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                                    if (newScale != oldScale) {
                                        val scaleFactor = newScale / oldScale
                                        val viewCenter = Offset(size.width / 2f, size.height / 2f)
                                        val newOffsetX = offset.x * scaleFactor + (centroid.x - viewCenter.x) * (1f - scaleFactor)
                                        val maxX = (size.width * (newScale - 1f)) / 2f
                                        
                                        scale = newScale
                                        offset = Offset(
                                            x = newOffsetX.coerceIn(-maxX, maxX),
                                            y = 0f
                                        )
                                    }
                                    changes.forEach { it.consume() }
                                }
                            } else if (changes.size == 1 && scale > 1f) {
                                val change = changes[0]
                                if (change.pressed) {
                                    val dragAmount = change.positionChange()
                                    if (kotlin.math.abs(dragAmount.x) > kotlin.math.abs(dragAmount.y)) {
                                        val maxX = (size.width * (scale - 1f)) / 2f
                                        offset = Offset(
                                            x = (offset.x + dragAmount.x).coerceIn(-maxX, maxX),
                                            y = 0f
                                        )
                                        change.consume()
                                    }
                                }
                            }
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            val targetScale = 2f
                            val viewCenter = Offset(size.width / 2f, size.height / 2f)
                            val targetOffsetX = (tapOffset.x - viewCenter.x) * (1f - targetScale)
                            val maxX = (size.width * (targetScale - 1f)) / 2f
                            
                            scale = targetScale
                            offset = Offset(
                                x = targetOffsetX.coerceIn(-maxX, maxX),
                                y = 0f
                            )
                        }
                    }
                )
            }
    ) {
        if (pdfRenderer != null) {
            val pageCount = pdfRenderer.pageCount
            val currentPage by remember {
                derivedStateOf {
                    (listState.firstVisibleItemIndex + 1).coerceAtMost(pageCount)
                }
            }

            var isIndicatorVisible by remember { mutableStateOf(false) }

            LaunchedEffect(listState.isScrollInProgress, listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
                isIndicatorVisible = true
                if (!listState.isScrollInProgress) {
                    kotlinx.coroutines.delay(1800)
                    isIndicatorVisible = false
                }
            }

            LazyColumn(
                state = listState,
                userScrollEnabled = true,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = 0f
                    },
                contentPadding = PaddingValues(vertical = 16.dp, horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(pageCount, key = { index -> "${pdfFile.name}_page_$index" }) { index ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        PdfPage(
                            pdfRenderer = pdfRenderer,
                            pageIndex = index,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = isIndicatorVisible,
                enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)),
                exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 6.dp,
                    shadowElevation = 6.dp
                ) {
                    Text(
                        text = "Page $currentPage / $pageCount",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Failed to render PDF document.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun PdfPage(
    pdfRenderer: PdfRenderer,
    pageIndex: Int,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pageIndex, pdfRenderer) {
        withContext(Dispatchers.IO) {
            val renderStartTime = System.currentTimeMillis()
            android.util.Log.d("PERF", "[PERF] MainThreadWork START operation=PDF thumbnail loading thread=${Thread.currentThread().name}")
            try {
                val page = pdfRenderer.openPage(pageIndex)
                val targetWidth = 1200
                val width = if (page.width > 0) targetWidth else 1080
                val height = if (page.width > 0) {
                    (page.height * (targetWidth.toFloat() / page.width.toFloat())).toInt()
                } else {
                    1600
                }

                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                canvas.drawColor(AndroidColor.WHITE)

                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmap = bmp
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                val renderDuration = System.currentTimeMillis() - renderStartTime
                android.util.Log.d("PERF", "[PERF] MainThreadWork END operation=PDF thumbnail loading duration=${renderDuration}ms thread=${Thread.currentThread().name}")
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(
                if (bitmap != null) {
                    bitmap!!.width.toFloat() / bitmap!!.height.toFloat()
                } else {
                    1f / 1.4142f
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

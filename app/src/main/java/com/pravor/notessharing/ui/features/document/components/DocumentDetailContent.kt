package com.pravor.notessharing.ui.features.document.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.FirebaseAuth
import com.pravor.notessharing.data.repository.ReportRepository
import com.pravor.notessharing.domain.model.DocumentDetail
import com.pravor.notessharing.ui.common.ReportBottomSheet
import com.pravor.notessharing.ui.features.document.AttachmentPreviewSection
import com.pravor.notessharing.ui.features.document.DownloadState
import com.pravor.notessharing.ui.features.document.RelatedDocumentsSection
import com.pravor.notessharing.ui.features.document.UploaderInfoCard
import com.pravor.notessharing.ui.navigation.LocalBottomBarPadding
import com.pravor.notessharing.ui.navigation.LocalSnackbarHostState
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailSuccessContent(
    doc: DocumentDetail,
    contributorLevel: String,
    relatedDocuments: List<DocumentDetail>,
    downloadState: DownloadState,
    shareLoading: Boolean,
    onNavigateToDetail: (String) -> Unit,
    onDownloadClick: () -> Unit,
    onShareClick: (String) -> Unit,
    onBottomShareClick: () -> Unit,
    onUpvoteClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    onShowRemoveUpvoteDialog: () -> Unit,
    onAttachmentClick: (String) -> Unit,
    isArchived: Boolean = false
) {
    val bottomPadding = LocalBottomBarPadding.current
    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var showReportBottomSheet by remember { mutableStateOf(false) }

    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (isArchived) {
            item(key = "archived-warning-banner") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF94A3B8).copy(alpha = 0.12f)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF94A3B8).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Archived",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(28.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Archived Download",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                )
                            )
                            Text(
                                text = "This resource has been removed from Campus Pages but remains available on your device.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFF64748B)
                                )
                            )
                        }
                    }
                }
            }
        }

        item(key = "preview-section") {
            AttachmentPreviewSection(
                doc = doc,
                onDownloadClick = { onDownloadClick() },
                onShareClick = onShareClick,
                onAttachmentClick = onAttachmentClick,
                modifier = Modifier
            )
        }

        item(key = "redesigned-metadata-card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val rawDocType = doc.documentType.lowercase(Locale.ROOT).trim()
                    val docTypeTag = when (rawDocType) {
                        "pyq" -> if (!doc.examYear.isNullOrBlank()) "PYQ | ${doc.examYear}" else "PYQ"
                        "assignment" -> {
                            buildList {
                                add("ASSIGNMENT")
                                val examType = doc.examType?.trim() ?: ""
                                if (examType.isNotBlank()) {
                                    add(examType.uppercase(Locale.ROOT))
                                }
                                val secDisp = doc.sectionDisplay?.trim() ?: ""
                                if (secDisp.isNotBlank()) {
                                    add(secDisp)
                                }
                            }.joinToString(" | ")
                        }
                        "cheatsheet", "cheat sheet" -> "CHEAT SHEET"
                        else -> doc.documentType.uppercase(Locale.ROOT)
                    }

                    val accentColor = when (rawDocType) {
                        "pyq" -> Color(0xFFE57373)
                        "assignment" -> Color(0xFF81C784)
                        "cheatsheet", "cheat sheet" -> Color(0xFFFFD54F)
                        else -> Color(0xFF64B5F6)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accentColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = docTypeTag,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        fontSize = 12.sp,
                                        color = accentColor
                                    )
                                )
                            }
                        }

                        val reportedMap by ReportRepository.instance.reportedFlow.collectAsStateWithLifecycle()
                        val isReported = remember(reportedMap, doc.id) {
                            reportedMap[doc.id] == true
                        }

                        IconButton(
                            onClick = {
                                if (currentUid.isEmpty()) {
                                    Toast.makeText(context, "Please sign in to report resources", Toast.LENGTH_SHORT).show()
                                } else if (isReported) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("You've already reported this resource.")
                                    }
                                } else {
                                    showReportBottomSheet = true
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isReported) Icons.Filled.Flag else Icons.Outlined.Flag,
                                contentDescription = if (isReported) "Already Reported" else "Report Resource",
                                tint = if (isReported) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val subjectSemesterText = remember(doc.subject, doc.semester) {
                        buildString {
                            append(doc.subject)
                            if (doc.semester.isNotBlank()) {
                                append(" | ")
                                append(doc.semester)
                            }
                        }
                    }
                    Text(
                        text = subjectSemesterText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }

        item(key = "redesigned-action-card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        NotesSharingDownloadIndicator(
                            downloadState = if (isArchived) DownloadState.Downloaded else downloadState,
                            onClick = {
                                if (!isArchived) {
                                    onDownloadClick()
                                }
                            }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .width(2.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    )

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        UpvoteButtonSection(
                            docId = doc.id,
                            initialUpvotes = doc.upvotes,
                            currentUid = currentUid,
                            onUpvoteClick = onUpvoteClick,
                            onShowRemoveDialog = onShowRemoveUpvoteDialog,
                            enabled = !isArchived
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .width(2.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    )

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        ShareButtonSection(
                            shareLoading = shareLoading,
                            onClick = onBottomShareClick,
                            enabled = !isArchived
                        )
                    }
                }
            }
        }

        if (doc.description.isNotBlank()) {
            item(key = "description-section") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = doc.description,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        item(key = "uploader-section") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(modifier = Modifier.padding(18.dp)) {
                    UploaderInfoCard(
                        uploaderName = doc.uploaderName,
                        uploaderPhotoUrl = doc.uploaderPhotoUrl,
                        contributorLevel = contributorLevel,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (!isArchived && relatedDocuments.isNotEmpty()) {
            item(key = "related-section") {
                RelatedDocumentsSection(
                    relatedDocuments = relatedDocuments,
                    onNavigateToDetail = onNavigateToDetail,
                    onBookmarkClick = onBookmarkClick,
                    onUpvoteClick = onUpvoteClick
                )
            }
        }
    }

    if (showReportBottomSheet) {
        ReportBottomSheet(
            resourceId = doc.id,
            resourceType = doc.collection,
            resourceTitle = doc.title,
            resourceThumbnail = doc.thumbnailUrl ?: doc.thumbnailUrls.firstOrNull(),
            uploaderUid = doc.uploaderId,
            uploaderName = doc.uploaderName,
            onDismissRequest = { showReportBottomSheet = false }
        )
    }
}

package com.pravor.notessharing.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pravor.notessharing.data.ReportRepository
import com.pravor.notessharing.ui.navigation.LocalSnackbarHostState
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportBottomSheet(
    resourceId: String,
    resourceType: String, // Explicit collection name: notes, assignments, pyqs, cheatsheets, videos
    resourceTitle: String,
    resourceThumbnail: String?,
    uploaderUid: String,
    uploaderName: String,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackbarHostState.current
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }

    val reasons = listOf(
        "Spam",
        "Duplicate",
        "Wrong Subject",
        "Misinformation / Incorrect Content",
        "Copyright Violation",
        "Offensive Content",
        "Broken File",
        "Other"
    )

    var selectedReason by remember { mutableStateOf<String?>(null) }
    var customMessage by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    var reporterName by remember { mutableStateOf(currentUser?.displayName ?: "User") }
    var reporterEmail by remember { mutableStateOf(currentUser?.email ?: "") }

    LaunchedEffect(currentUser) {
        val uid = currentUser?.uid
        if (uid != null) {
            try {
                val userSnap = FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
                if (userSnap.exists()) {
                    reporterName = userSnap.getString("name") ?: reporterName
                    reporterEmail = userSnap.getString("email") ?: reporterEmail
                }
            } catch (e: Exception) {
                // Ignore, use fallback details
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Report Resource",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Help us understand what's wrong with this resource. Your report will be reviewed by our moderation team.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // List of Reasons
            reasons.forEach { reason ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isSubmitting) { selectedReason = reason }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (selectedReason == reason),
                        onClick = { selectedReason = reason },
                        enabled = !isSubmitting,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Custom message for "Other"
            if (selectedReason == "Other") {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customMessage,
                    onValueChange = { customMessage = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    label = { Text("Describe the issue") },
                    placeholder = { Text("Please explain the reason for reporting this resource...") },
                    minLines = 3,
                    maxLines = 5,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(18.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f),
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Cancel")
                }

                val isSubmitEnabled = selectedReason != null && 
                        (!isSubmitting) && 
                        (selectedReason != "Other" || customMessage.trim().isNotEmpty())

                Button(
                    onClick = {
                        val uid = currentUser?.uid
                        if (uid == null) {
                            Toast.makeText(context, "Please sign in to submit a report", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSubmitting = true
                        scope.launch {
                            val repo = ReportRepository.instance
                            val result = repo.submitReport(
                                resourceId = resourceId,
                                resourceType = resourceType,
                                resourceTitle = resourceTitle,
                                resourceThumbnail = resourceThumbnail,
                                uploaderUid = uploaderUid,
                                uploaderName = uploaderName,
                                reporterUid = uid,
                                reporterName = reporterName,
                                reporterEmail = reporterEmail,
                                reason = selectedReason!!,
                                customMessage = if (selectedReason == "Other") customMessage.trim() else ""
                            )
                            isSubmitting = false
                            result.onSuccess {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Report submitted successfully.")
                                }
                                onDismissRequest()
                            }.onFailure { exception ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(exception.message ?: "Failed to submit report")
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = isSubmitEnabled,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Submit Report")
                    }
                }
            }
        }
    }
}

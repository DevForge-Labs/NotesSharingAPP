package com.pravor.notessharing.ui.features.upload.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.pravor.notessharing.domain.model.UploadType
import com.pravor.notessharing.ui.common.components.SectionHeader
import com.pravor.notessharing.ui.features.upload.CatalogSubject
import com.pravor.notessharing.ui.features.upload.UploadUiState
import java.util.Locale

fun Modifier.clearFocusOnOutsideTap(onClearFocus: () -> Unit): Modifier =
    pointerInput(onClearFocus) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            onClearFocus()
        }
    }

@Composable
fun MetadataSection(
    uiState: UploadUiState,
    onBranchChange: (String) -> Unit,
    onSemesterChange: (String) -> Unit,
    onGroupChange: (String) -> Unit,
    onSubjectChange: (String) -> Unit,
    onSubjectSelected: (CatalogSubject) -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSectionChange: (String) -> Unit,
    onYoutubeResourceTypeChange: (String) -> Unit,
    onTypeSelected: (UploadType) -> Unit,
    onExamYearChange: (String) -> Unit,
    onExamTypeChange: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader("Study Metadata")
            
            DropdownField(
                label = "Branch",
                value = uiState.selectedBranch,
                options = uiState.branches,
                onValueChange = onBranchChange
            )
            
            DropdownField(
                label = "Semester",
                value = uiState.selectedSemester,
                options = uiState.semesters,
                onValueChange = onSemesterChange
            )

            val isFirstYear = uiState.selectedSemester == "Semester 1" || uiState.selectedSemester == "Semester 2"
            if (isFirstYear) {
                DropdownField(
                    label = "Group",
                    value = uiState.selectedGroup,
                    options = uiState.groups,
                    onValueChange = onGroupChange
                )
            }
            
            if (uiState.useCatalogDropdown) {
                DropdownFieldSubject(
                    label = "Subject",
                    value = uiState.subject,
                    options = uiState.catalogSubjects,
                    onValueChange = onSubjectSelected
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = uiState.subject,
                        onValueChange = onSubjectChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Subject") },
                        placeholder = { Text("DBMS, Operating Systems, DSA...") },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    val selectionDone = if (uiState.selectedSemester == "Semester 1" || uiState.selectedSemester == "Semester 2") {
                        uiState.selectedSemester.isNotBlank() && uiState.selectedGroup.isNotBlank()
                    } else {
                        uiState.selectedBranch.isNotBlank() && uiState.selectedSemester.isNotBlank()
                    }
                    if (selectionDone && !uiState.subjectCatalogKeyExists) {
                        Text(
                            text = "Subject catalog unavailable for this branch/semester. Enter subject manually.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    val showSubjectError = uiState.subject.isBlank() && (uiState.selectedBranch.isNotBlank() || uiState.selectedSemester.isNotBlank())
                    if (showSubjectError) {
                        Text(
                            text = "Subject is required",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            DropdownFieldUploadType(
                label = "Document Type",
                value = uiState.selectedType,
                options = UploadType.values().toList(),
                onValueChange = onTypeSelected
            )

            if (uiState.selectedType == UploadType.Youtube) {
                DropdownField(
                    label = "Type",
                    value = if (uiState.youtubeResourceType == "playlist") "Playlist" else "Video",
                    options = listOf("Video", "Playlist"),
                    onValueChange = { selected ->
                        onYoutubeResourceTypeChange(selected.lowercase(Locale.ROOT))
                    }
                )
            }

            if (uiState.selectedType == UploadType.Assignment) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = uiState.section,
                        onValueChange = onSectionChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Section") },
                        placeholder = { Text("Enter section (e.g. CSE-32, ECE 2)...") },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    val showSectionError = uiState.section.isBlank() && uiState.selectedType == UploadType.Assignment
                    if (showSectionError) {
                        Text(
                            text = "Section is required",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            if (uiState.selectedType in listOf(UploadType.Notes, UploadType.CheatSheet, UploadType.Assignment)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = onTitleChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Title") },
                        placeholder = { Text("Enter a title for this upload...") },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    val showTitleError = uiState.title.isBlank() && uiState.selectedType != null
                    if (showTitleError) {
                        Text(
                            text = "Title is required",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            if (uiState.selectedType == UploadType.Pyq) {
                DropdownField(
                    label = "Exam Year",
                    value = uiState.selectedExamYear,
                    options = uiState.examYears,
                    onValueChange = onExamYearChange
                )
                if (uiState.selectedExamYear.isBlank() && uiState.selectedExamType.isNotBlank()) {
                    Text(
                        text = "Exam Year is required",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                DropdownField(
                    label = "Exam Type",
                    value = uiState.selectedExamType,
                    options = uiState.examTypes,
                    onValueChange = onExamTypeChange
                )
                if (uiState.selectedExamType.isBlank() && uiState.selectedExamYear.isNotBlank()) {
                    Text(
                        text = "Exam Type is required",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            val descriptionPlaceholder = when (uiState.selectedType) {
                UploadType.Notes -> "Mention teacher name or section name..."
                UploadType.Assignment -> "Mention teacher name or section name..."
                UploadType.CheatSheet -> "How it helps..."
                else -> "Optional description..."
            }

            if (uiState.selectedType != UploadType.Pyq && uiState.selectedType != UploadType.Youtube) {
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = onDescriptionChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Description") },
                    placeholder = { Text(descriptionPlaceholder, maxLines = 2) },
                    singleLine = false,
                    minLines = 1,
                    maxLines = 10,
                    shape = RoundedCornerShape(18.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            label = { Text(label) },
            placeholder = { Text("Select $label") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(18.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 200.dp)
        ) {
            val scrollState = rememberScrollState()
            val primaryColor = MaterialTheme.colorScheme.primary

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .verticalScroll(scrollState)
                    .drawWithContent {
                        drawContent()
                        val maxScroll = scrollState.maxValue
                        if (maxScroll > 0) {
                            val viewportHeight = size.height
                            val totalContentHeight = maxScroll + viewportHeight
                            
                            val trackWidth = 3.dp.toPx()
                            val trackRightPadding = 4.dp.toPx()
                            val trackTopPadding = 8.dp.toPx()
                            val trackX = size.width - trackWidth - trackRightPadding
                            
                            val usableHeight = viewportHeight - trackTopPadding * 2
                            val thumbFraction = (viewportHeight / totalContentHeight).coerceIn(0.08f, 0.18f)
                            val thumbHeight = usableHeight * thumbFraction

                            val scrollFraction = scrollState.value.toFloat() / maxScroll.toFloat()
                            val maxOffset = usableHeight - thumbHeight
                            val thumbOffset = maxOffset * scrollFraction

                            drawRoundRect(
                                color = primaryColor.copy(alpha = 0.08f),
                                topLeft = Offset(trackX, trackTopPadding),
                                size = Size(trackWidth, usableHeight),
                                cornerRadius = CornerRadius(x = trackWidth / 2f, y = trackWidth / 2f)
                            )

                            drawRoundRect(
                                color = primaryColor.copy(alpha = 0.85f),
                                topLeft = Offset(trackX, thumbOffset + trackTopPadding),
                                size = Size(trackWidth, thumbHeight.coerceAtLeast(trackWidth)),
                                cornerRadius = CornerRadius(x = trackWidth / 2f, y = trackWidth / 2f)
                            )
                        }
                    }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownFieldUploadType(
    label: String,
    value: UploadType?,
    options: List<UploadType>,
    onValueChange: (UploadType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value?.label ?: "",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            label = { Text(label) },
            placeholder = { Text("Select $label") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(18.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 200.dp)
        ) {
            val scrollState = rememberScrollState()
            val primaryColor = MaterialTheme.colorScheme.primary

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .verticalScroll(scrollState)
                    .drawWithContent {
                        drawContent()
                        val maxScroll = scrollState.maxValue
                        if (maxScroll > 0) {
                            val viewportHeight = size.height
                            val totalContentHeight = maxScroll + viewportHeight
                            
                            val trackWidth = 3.dp.toPx()
                            val trackRightPadding = 4.dp.toPx()
                            val trackTopPadding = 8.dp.toPx()
                            val trackX = size.width - trackWidth - trackRightPadding
                            
                            val usableHeight = viewportHeight - trackTopPadding * 2
                            val thumbFraction = (viewportHeight / totalContentHeight).coerceIn(0.08f, 0.18f)
                            val thumbHeight = usableHeight * thumbFraction

                            val scrollFraction = scrollState.value.toFloat() / maxScroll.toFloat()
                            val maxOffset = usableHeight - thumbHeight
                            val thumbOffset = maxOffset * scrollFraction

                            drawRoundRect(
                                color = primaryColor.copy(alpha = 0.08f),
                                topLeft = Offset(trackX, trackTopPadding),
                                size = Size(trackWidth, usableHeight),
                                cornerRadius = CornerRadius(x = trackWidth / 2f, y = trackWidth / 2f)
                            )

                            drawRoundRect(
                                color = primaryColor.copy(alpha = 0.85f),
                                topLeft = Offset(trackX, thumbOffset + trackTopPadding),
                                size = Size(trackWidth, thumbHeight.coerceAtLeast(trackWidth)),
                                cornerRadius = CornerRadius(x = trackWidth / 2f, y = trackWidth / 2f)
                            )
                        }
                    }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownFieldSubject(
    label: String,
    value: String,
    options: List<CatalogSubject>,
    onValueChange: (CatalogSubject) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            label = { Text(label) },
            placeholder = { Text("Select $label") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(18.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 200.dp)
        ) {
            val scrollState = rememberScrollState()
            val primaryColor = MaterialTheme.colorScheme.primary

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .verticalScroll(scrollState)
                    .drawWithContent {
                        drawContent()
                        val maxScroll = scrollState.maxValue
                        if (maxScroll > 0) {
                            val viewportHeight = size.height
                            val totalContentHeight = maxScroll + viewportHeight
                            
                            val trackWidth = 3.dp.toPx()
                            val trackRightPadding = 4.dp.toPx()
                            val trackTopPadding = 8.dp.toPx()
                            val trackX = size.width - trackWidth - trackRightPadding
                            
                            val usableHeight = viewportHeight - trackTopPadding * 2
                            val thumbFraction = (viewportHeight / totalContentHeight).coerceIn(0.08f, 0.18f)
                            val thumbHeight = usableHeight * thumbFraction

                            val scrollFraction = scrollState.value.toFloat() / maxScroll.toFloat()
                            val maxOffset = usableHeight - thumbHeight
                            val thumbOffset = maxOffset * scrollFraction

                            drawRoundRect(
                                color = primaryColor.copy(alpha = 0.08f),
                                topLeft = Offset(trackX, trackTopPadding),
                                size = Size(trackWidth, usableHeight),
                                cornerRadius = CornerRadius(x = trackWidth / 2f, y = trackWidth / 2f)
                            )

                            drawRoundRect(
                                color = primaryColor.copy(alpha = 0.85f),
                                topLeft = Offset(trackX, thumbOffset + trackTopPadding),
                                size = Size(trackWidth, thumbHeight.coerceAtLeast(trackWidth)),
                                cornerRadius = CornerRadius(x = trackWidth / 2f, y = trackWidth / 2f)
                            )
                        }
                    }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.name) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

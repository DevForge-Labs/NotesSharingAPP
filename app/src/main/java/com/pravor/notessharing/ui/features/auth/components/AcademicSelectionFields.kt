package com.pravor.notessharing.ui.features.auth.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pravor.notessharing.data.repository.BranchMetadata
import com.pravor.notessharing.data.repository.CollegeMetadata

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollegeDropdownField(
    selectedCollegeName: String,
    collegeExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    colleges: List<CollegeMetadata>,
    isCollegesLoading: Boolean,
    collegesError: String?,
    onCollegeSelected: (CollegeMetadata) -> Unit,
    modifier: Modifier = Modifier
) {
    ExposedDropdownMenuBox(
        expanded = collegeExpanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedCollegeName,
            onValueChange = {},
            readOnly = true,
            label = { Text("College / University") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.School, contentDescription = null)
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = collegeExpanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
        ExposedDropdownMenu(
            expanded = collegeExpanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            if (isCollegesLoading) {
                DropdownMenuItem(
                    text = { Text("Loading colleges...") },
                    onClick = {},
                    enabled = false
                )
            } else if (collegesError != null) {
                DropdownMenuItem(
                    text = { Text(collegesError) },
                    onClick = {},
                    enabled = false
                )
            } else if (colleges.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No colleges available") },
                    onClick = {},
                    enabled = false
                )
            } else {
                colleges.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.name) },
                        onClick = { onCollegeSelected(option) },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchDropdownField(
    selectedBranchName: String,
    branchExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    branches: List<BranchMetadata>,
    isBranchesLoading: Boolean,
    branchesError: String?,
    onBranchSelected: (BranchMetadata) -> Unit,
    modifier: Modifier = Modifier
) {
    ExposedDropdownMenuBox(
        expanded = branchExpanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedBranchName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Branch") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Class, contentDescription = null)
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = branchExpanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
        ExposedDropdownMenu(
            expanded = branchExpanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            if (isBranchesLoading) {
                DropdownMenuItem(
                    text = { Text("Loading branches...") },
                    onClick = {},
                    enabled = false
                )
            } else if (branchesError != null) {
                DropdownMenuItem(
                    text = { Text(branchesError) },
                    onClick = {},
                    enabled = false
                )
            } else if (branches.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No branches available") },
                    onClick = {},
                    enabled = false
                )
            } else {
                branches.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.name) },
                        onClick = { onBranchSelected(option) },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemesterDropdownField(
    semester: String,
    semesterExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    semesterOptions: List<String>,
    onSemesterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ExposedDropdownMenuBox(
        expanded = semesterExpanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = semester,
            onValueChange = {},
            readOnly = true,
            label = { Text("Semester") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Class, contentDescription = null)
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = semesterExpanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
        ExposedDropdownMenu(
            expanded = semesterExpanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            semesterOptions.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption) },
                    onClick = { onSemesterSelected(selectionOption) },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
fun SectionInputField(
    section: String,
    onSectionChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = section,
        onValueChange = onSectionChange,
        label = { Text("Section (e.g. CSE-52, IT 7, A1)") },
        leadingIcon = {
            Icon(imageVector = Icons.Default.Group, contentDescription = null)
        },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        )
    )
}

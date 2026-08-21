package com.pravor.notessharing.ui.features.profile

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pravor.notessharing.domain.model.Profile
import com.pravor.notessharing.ui.common.EditProfileState
import com.pravor.notessharing.ui.common.ProfileUiState
import com.pravor.notessharing.ui.features.profile.components.EditProfileAvatarCard
import com.pravor.notessharing.ui.navigation.LocalBottomBarPadding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileRoute(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val profileUiState by viewModel.uiState.collectAsState()
    val editState by viewModel.editState.collectAsState()

    when (val state = profileUiState) {
        is ProfileUiState.Success -> {
            EditProfileScreen(
                profile = state.profile,
                resolvedCollegeName = state.resolvedCollegeName,
                resolvedBranchName = state.resolvedBranchName,
                viewModel = viewModel,
                editState = editState,
                onSaveChanges = { name, semester, section, branch, localUri, isRemoved, onSuccess ->
                    viewModel.updateProfile(name, semester, section, branch, localUri, isRemoved, onSuccess)
                },
                clearEditState = { viewModel.clearEditState() },
                onNavigateBack = onNavigateBack,
                onNavigateToProfile = onNavigateToProfile
            )
        }
        else -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    profile: Profile,
    resolvedCollegeName: String,
    resolvedBranchName: String,
    viewModel: ProfileViewModel,
    editState: EditProfileState,
    onSaveChanges: (String, String, String, String, String?, Boolean, () -> Unit) -> Unit,
    clearEditState: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var fullName by remember { mutableStateOf(profile.name) }
    var semester by remember { mutableStateOf(profile.semester) }
    var section by remember { mutableStateOf(profile.section) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isImageRemoved by remember { mutableStateOf(false) }

    val branches by viewModel.branches.collectAsState()
    val isBranchesLoading by viewModel.isBranchesLoading.collectAsState()
    val branchesError by viewModel.branchesError.collectAsState()

    var selectedBranchId by remember { mutableStateOf(profile.branch) }
    var selectedBranchName by remember { mutableStateOf(resolvedBranchName) }
    var branchExpanded by remember { mutableStateOf(false) }

    var semesterExpanded by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var semesterError by remember { mutableStateOf<String?>(null) }
    var sectionError by remember { mutableStateOf<String?>(null) }
    var branchError by remember { mutableStateOf<String?>(null) }

    val hasChanges = fullName != profile.name ||
            semester != profile.semester ||
            section != profile.section ||
            selectedBranchId != profile.branch ||
            selectedImageUri != null ||
            isImageRemoved

    LaunchedEffect(profile.college) {
        viewModel.loadBranchesForCollege(profile.college)
    }

    BackHandler(enabled = hasChanges) {
        showDiscardDialog = true
    }

    LaunchedEffect(editState) {
        if (editState is EditProfileState.Success) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Profile Updated Successfully")
            }
            clearEditState()
            onNavigateToProfile()
        } else if (editState is EditProfileState.Error) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(editState.message)
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            isImageRemoved = false
        }
    }

    val bottomPadding = LocalBottomBarPadding.current

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (hasChanges) {
                            showDiscardDialog = true
                        } else {
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "Edit Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 12.dp + bottomPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EditProfileAvatarCard(
                profile = profile,
                selectedImageUri = selectedImageUri,
                isImageRemoved = isImageRemoved,
                onChangePhotoClick = { galleryLauncher.launch("image/*") },
                onRemovePhotoClick = {
                    selectedImageUri = null
                    isImageRemoved = true
                }
            )

            Card(
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Personal Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = {
                                fullName = it
                                nameError = if (it.trim().length < 2) "Name must be at least 2 characters." else null
                            },
                            label = { Text("Full Name") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            isError = nameError != null,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        nameError?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }

                    ReadOnlyRow(label = "College", value = resolvedCollegeName, icon = Icons.Default.School)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ExposedDropdownMenuBox(
                            expanded = branchExpanded,
                            onExpandedChange = { branchExpanded = !branchExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedBranchName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Branch") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Class, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = branchExpanded) },
                                isError = branchError != null,
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = branchExpanded,
                                onDismissRequest = { branchExpanded = false }
                            ) {
                                if (isBranchesLoading) {
                                    DropdownMenuItem(
                                        text = { Text("Loading branches...") },
                                        onClick = {},
                                        enabled = false
                                    )
                                } else if (branchesError != null) {
                                    DropdownMenuItem(
                                        text = { Text(branchesError ?: "Error loading branches") },
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
                                            onClick = {
                                                selectedBranchId = option.id
                                                selectedBranchName = option.name
                                                branchExpanded = false
                                                branchError = null
                                            },
                                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                        )
                                    }
                                }
                            }
                        }
                        branchError?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val semesterOptions = listOf(
                            "Semester 1", "Semester 2", "Semester 3", "Semester 4",
                            "Semester 5", "Semester 6", "Semester 7", "Semester 8"
                        )
                        ExposedDropdownMenuBox(
                            expanded = semesterExpanded,
                            onExpandedChange = { semesterExpanded = !semesterExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = semester,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Semester") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Class, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = semesterExpanded) },
                                isError = semesterError != null,
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = semesterExpanded,
                                onDismissRequest = { semesterExpanded = false }
                            ) {
                                semesterOptions.forEach { selectionOption ->
                                    DropdownMenuItem(
                                        text = { Text(selectionOption) },
                                        onClick = {
                                            semester = selectionOption
                                            semesterExpanded = false
                                            semesterError = null
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                    )
                                }
                            }
                        }
                        semesterError?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = section,
                            onValueChange = {
                                section = it
                                sectionError = if (it.trim().isBlank()) "Section is mandatory." else null
                            },
                            label = { Text("Section (e.g. CSE-52, IT 7, A1)") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            isError = sectionError != null,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        sectionError?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Account Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    ReadOnlyRow(label = "Email Address", value = profile.email, icon = Icons.Default.Email)
                    
                    val formattedDate = remember(profile.createdAt) {
                        try {
                            val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                            sdf.format(Date(profile.createdAt))
                        } catch (e: Exception) {
                            "Not Available"
                        }
                    }
                    ReadOnlyRow(label = "Member Since", value = formattedDate, icon = Icons.Default.Info)
                }
            }

            val isSaving = editState is EditProfileState.Loading
            Button(
                onClick = {
                    nameError = null
                    semesterError = null
                    sectionError = null
                    branchError = null
                    if (fullName.trim().isBlank() || fullName.trim().length < 2) {
                        nameError = "Name cannot be blank and must be at least 2 characters."
                        return@Button
                    }
                    if (selectedBranchId.trim().isBlank()) {
                        branchError = "Branch is mandatory."
                        return@Button
                    }
                    if (semester.trim().isBlank()) {
                        semesterError = "Semester selection is mandatory."
                        return@Button
                    }
                    if (section.trim().isBlank()) {
                        sectionError = "Section is mandatory."
                        return@Button
                    }

                    onSaveChanges(
                        fullName.trim(),
                        semester.trim(),
                        section.trim(),
                        selectedBranchId.trim(),
                        selectedImageUri?.toString(),
                        isImageRemoved
                    ) {
                        // handled in LaunchedEffect
                    }
                },
                enabled = hasChanges && !isSaving,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "Save Changes",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = {
                Text(
                    text = "Discard Changes?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("You have unsaved profile changes.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        clearEditState()
                        onNavigateBack()
                    }
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}

@Composable
fun ReadOnlyRow(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

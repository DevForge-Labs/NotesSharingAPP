package com.pravor.notessharing.ui.features.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pravor.notessharing.ui.features.auth.components.BranchDropdownField
import com.pravor.notessharing.ui.features.auth.components.CollegeDropdownField
import com.pravor.notessharing.ui.features.auth.components.SectionInputField
import com.pravor.notessharing.ui.features.auth.components.SemesterDropdownField
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleOnboardingScreen(
    viewModel: AuthViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val tempProfile = viewModel.tempGoogleProfile
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var fullName by remember { mutableStateOf(tempProfile?.name ?: "") }
    var email by remember { mutableStateOf(tempProfile?.email ?: "") }
    var selectedCollegeId by remember { mutableStateOf("") }
    var selectedCollegeName by remember { mutableStateOf("") }
    var selectedBranchId by remember { mutableStateOf("") }
    var selectedBranchName by remember { mutableStateOf("") }
    var semester by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("") }

    var collegeExpanded by remember { mutableStateOf(false) }
    var branchExpanded by remember { mutableStateOf(false) }
    var semesterExpanded by remember { mutableStateOf(false) }

    val colleges by viewModel.colleges.collectAsState()
    val branches by viewModel.branches.collectAsState()
    val isCollegesLoading by viewModel.isCollegesLoading.collectAsState()
    val collegesError by viewModel.collegesError.collectAsState()
    val isBranchesLoading by viewModel.isBranchesLoading.collectAsState()
    val branchesError by viewModel.branchesError.collectAsState()

    val semesterOptions = listOf(
        "Semester 1", "Semester 2", "Semester 3", "Semester 4",
        "Semester 5", "Semester 6", "Semester 7", "Semester 8"
    )

    LaunchedEffect(Unit) {
        viewModel.loadColleges()
    }

    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(600))
    }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            viewModel.clearState()
        } else if (uiState is AuthUiState.Error) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar((uiState as AuthUiState.Error).message)
            }
        }
    }

    val isFormValid = fullName.isNotBlank() &&
            selectedCollegeId.isNotBlank() &&
            selectedBranchId.isNotBlank() &&
            semester.isNotBlank() &&
            section.isNotBlank()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceContainerLowest
                    )
                )
            )
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
                    .alpha(alpha.value),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
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
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Complete Your Profile",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Select your academic details to personalize your experience",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Email Address") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    enabled = false
                )

                Spacer(modifier = Modifier.height(14.dp))

                CollegeDropdownField(
                    selectedCollegeName = selectedCollegeName,
                    collegeExpanded = collegeExpanded,
                    onExpandedChange = { collegeExpanded = it },
                    colleges = colleges,
                    isCollegesLoading = isCollegesLoading,
                    collegesError = collegesError,
                    onCollegeSelected = { item ->
                        selectedCollegeId = item.id
                        selectedCollegeName = item.name
                        collegeExpanded = false
                        selectedBranchId = ""
                        selectedBranchName = ""
                        viewModel.loadBranchesForCollege(item.id)
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                BranchDropdownField(
                    selectedBranchName = selectedBranchName,
                    branchExpanded = branchExpanded,
                    onExpandedChange = { branchExpanded = it },
                    branches = branches,
                    isBranchesLoading = isBranchesLoading,
                    branchesError = branchesError,
                    onBranchSelected = { item ->
                        selectedBranchId = item.id
                        selectedBranchName = item.name
                        branchExpanded = false
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                SemesterDropdownField(
                    semester = semester,
                    semesterExpanded = semesterExpanded,
                    onExpandedChange = { semesterExpanded = it },
                    semesterOptions = semesterOptions,
                    onSemesterSelected = { item ->
                        semester = item
                        semesterExpanded = false
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                SectionInputField(
                    section = section,
                    onSectionChange = { section = it }
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = {
                        viewModel.completeGoogleOnboarding(
                            name = fullName.trim(),
                            college = selectedCollegeId.trim(),
                            branch = selectedBranchId.trim(),
                            semester = semester.trim(),
                            section = section.trim()
                        )
                    },
                    enabled = isFormValid && uiState !is AuthUiState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (uiState is AuthUiState.Loading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "Complete Registration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

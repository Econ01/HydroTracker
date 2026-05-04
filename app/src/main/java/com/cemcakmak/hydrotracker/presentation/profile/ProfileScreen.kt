package com.cemcakmak.hydrotracker.presentation.profile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.cemcakmak.hydrotracker.data.models.UserProfile
import com.cemcakmak.hydrotracker.data.repository.UserRepository
import com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository
import com.cemcakmak.hydrotracker.data.database.repository.TodayStatistics
import com.cemcakmak.hydrotracker.utils.WaterCalculator
import com.cemcakmak.hydrotracker.data.models.ActivityLevel
import com.cemcakmak.hydrotracker.data.models.AgeGroup
import com.cemcakmak.hydrotracker.data.models.Gender
import com.cemcakmak.hydrotracker.data.models.ReminderStyle
import com.cemcakmak.hydrotracker.presentation.common.HydroSnackbarHost
import com.cemcakmak.hydrotracker.presentation.common.showSuccessSnackbar
import com.cemcakmak.hydrotracker.utils.ImageUtils
import com.cemcakmak.hydrotracker.notifications.HydroNotificationScheduler
import java.io.File

/**
 * Main Profile Screen
 * Modular architecture with separate components
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    userRepository: UserRepository,
    waterIntakeRepository: WaterIntakeRepository,
    onNavigateBack: () -> Unit = {}
) {
    // Collect statistics data
    val todayStatistics by waterIntakeRepository.getTodayStatistics().collectAsState(
        initial = TodayStatistics(0.0, 0f, 0, 0.0, 0.0, null, null, false, 0.0)
    )

    val last30DaysEntries by waterIntakeRepository.getLast30DaysEntries().collectAsState(
        initial = emptyList()
    )

    // State management
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Bottom sheet states
    var showGoalBottomSheet by remember { mutableStateOf(false) }
    var showActivityBottomSheet by remember { mutableStateOf(false) }
    var showScheduleBottomSheet by remember { mutableStateOf(false) }
    var showGenderBottomSheet by remember { mutableStateOf(false) }
    var showAgeGroupBottomSheet by remember { mutableStateOf(false) }
    var showWeightBottomSheet by remember { mutableStateOf(false) }
    var showUsernameDialog by remember { mutableStateOf(false) }
    var showProfilePictureBottomSheet by remember { mutableStateOf(false) }

    // Animation state
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    fun updateUserProfile(updatedProfile: UserProfile) {
        coroutineScope.launch {
            // Check if notification-affecting fields changed
            val needsNotificationReschedule =
                userProfile.wakeUpTime != updatedProfile.wakeUpTime ||
                userProfile.sleepTime != updatedProfile.sleepTime ||
                userProfile.reminderInterval != updatedProfile.reminderInterval

            userRepository.saveUserProfile(updatedProfile)

            // Reschedule notifications if needed and user has completed onboarding
            if (needsNotificationReschedule && updatedProfile.isOnboardingCompleted) {
                Log.d("ProfileScreen", "Profile changes affect notifications, rescheduling...")
                HydroNotificationScheduler.rescheduleNotifications(context, updatedProfile)
            }

            snackbarHostState.showSuccessSnackbar(
                message = "Profile updated successfully!"
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile",
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { HydroSnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 6.dp)
                .padding(bottom = 80.dp), // Add bottom padding for navigation bar
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // Profile Header Section
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { -it / 3 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(animationSpec = tween(600))
            ) {
                ProfileHeaderCard(
                    userProfile = userProfile,
                    todayStatistics = todayStatistics,
                    totalDaysTracked = last30DaysEntries.groupBy { it.date }.size,
                    onEditProfilePicture = { showProfilePictureBottomSheet = true },
                    onEditUsername = { showUsernameDialog = true }
                )
            }

            // Profile Details Section
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(animationSpec = tween(600, delayMillis = 200))
            ) {
                ProfileDetailsCard(
                    userProfile = userProfile,
                    onEditGender = { showGenderBottomSheet = true },
                    onEditAgeGroup = { showAgeGroupBottomSheet = true },
                    onEditWeight = { showWeightBottomSheet = true }
                )
            }

            // Daily Goals Section
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(animationSpec = tween(600, delayMillis = 300))
            ) {
                DailyGoalsCard(
                    userProfile = userProfile,
                    onEditGoal = { showGoalBottomSheet = true },
                    onEditActivity = { showActivityBottomSheet = true }
                )
            }

            // Active Schedule Section
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(animationSpec = tween(600, delayMillis = 400))
            ) {
                ActiveScheduleCard(
                    userProfile = userProfile,
                    onEditSchedule = { showScheduleBottomSheet = true }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Bottom Sheets
    GoalEditBottomSheet(
        showBottomSheet = showGoalBottomSheet,
        currentGoal = userProfile.dailyWaterGoal,
        onDismiss = { showGoalBottomSheet = false },
        onConfirm = { newGoal ->
            updateUserProfile(userProfile.copy(dailyWaterGoal = newGoal))
            showGoalBottomSheet = false
        }
    )

    ActivityLevelBottomSheet(
        showBottomSheet = showActivityBottomSheet,
        currentLevel = userProfile.activityLevel,
        onDismiss = { showActivityBottomSheet = false },
        onConfirm = { newLevel ->
            // Recalculate goal with new activity level
            val newGoal = WaterCalculator.calculateDailyWaterGoal(
                gender = userProfile.gender,
                ageGroup = userProfile.ageGroup,
                activityLevel = newLevel,
                weight = userProfile.weight,
                hydrationStandard = userProfile.hydrationStandard
            )
            updateUserProfile(
                userProfile.copy(
                    activityLevel = newLevel,
                    dailyWaterGoal = newGoal
                )
            )
            showActivityBottomSheet = false
        }
    )

    ScheduleEditBottomSheet(
        showBottomSheet = showScheduleBottomSheet,
        currentWakeUpTime = userProfile.wakeUpTime,
        currentSleepTime = userProfile.sleepTime,
        onDismiss = { showScheduleBottomSheet = false },
        onConfirm = { wakeUp, sleep ->
            // Recalculate reminder interval
            val newInterval = WaterCalculator.calculateReminderInterval(
                wakeUpTime = wakeUp,
                sleepTime = sleep,
                dailyGoal = userProfile.dailyWaterGoal
            )

            Log.d("Interval", "New interval: $newInterval")

            updateUserProfile(
                userProfile.copy(
                    wakeUpTime = wakeUp,
                    sleepTime = sleep,
                    reminderInterval = newInterval
                )
            )
            showScheduleBottomSheet = false
        }
    )

    GenderEditBottomSheet(
        showBottomSheet = showGenderBottomSheet,
        currentGender = userProfile.gender,
        onDismiss = { showGenderBottomSheet = false },
        onConfirm = { newGender ->
            // Recalculate goal with new gender
            val newGoal = WaterCalculator.calculateDailyWaterGoal(
                gender = newGender,
                ageGroup = userProfile.ageGroup,
                activityLevel = userProfile.activityLevel,
                weight = userProfile.weight,
                hydrationStandard = userProfile.hydrationStandard
            )
            updateUserProfile(
                userProfile.copy(
                    gender = newGender,
                    dailyWaterGoal = newGoal
                )
            )
            showGenderBottomSheet = false
        }
    )

    AgeGroupEditBottomSheet(
        showBottomSheet = showAgeGroupBottomSheet,
        currentAgeGroup = userProfile.ageGroup,
        onDismiss = { showAgeGroupBottomSheet = false },
        onConfirm = { newAgeGroup ->
            // Recalculate goal with new age group
            val newGoal = WaterCalculator.calculateDailyWaterGoal(
                gender = userProfile.gender,
                ageGroup = newAgeGroup,
                activityLevel = userProfile.activityLevel,
                weight = userProfile.weight,
                hydrationStandard = userProfile.hydrationStandard
            )
            updateUserProfile(
                userProfile.copy(
                    ageGroup = newAgeGroup,
                    dailyWaterGoal = newGoal
                )
            )
            showAgeGroupBottomSheet = false
        }
    )

    WeightEditBottomSheet(
        showBottomSheet = showWeightBottomSheet,
        currentWeight = userProfile.weight,
        onDismiss = { showWeightBottomSheet = false },
        onConfirm = { newWeight ->
            // Recalculate goal with new weight
            val newGoal = WaterCalculator.calculateDailyWaterGoal(
                gender = userProfile.gender,
                ageGroup = userProfile.ageGroup,
                activityLevel = userProfile.activityLevel,
                weight = newWeight,
                hydrationStandard = userProfile.hydrationStandard
            )
            updateUserProfile(
                userProfile.copy(
                    weight = newWeight,
                    dailyWaterGoal = newGoal
                )
            )
            showWeightBottomSheet = false
        }
    )
    
    // Profile Picture Bottom Sheet and Username Dialog
    ProfilePictureBottomSheet(
        showBottomSheet = showProfilePictureBottomSheet,
        onDismiss = { showProfilePictureBottomSheet = false },
        onImageSelected = { uri ->
            updateUserProfile(userProfile.copy(profileImagePath = uri?.toString()))
            showProfilePictureBottomSheet = false
        }
    )
    
    if (showUsernameDialog) {
        UsernameEditDialog(
            currentName = userProfile.name,
            onDismiss = { showUsernameDialog = false },
            onConfirm = { newName ->
                updateUserProfile(userProfile.copy(name = newName))
                showUsernameDialog = false
            }
        )
    }
}

@Preview
@Composable
fun ProfileScreenPreview() {
    val userProfile = UserProfile(
        name = "Preview User",
        gender = Gender.MALE,
        ageGroup = AgeGroup.YOUNG_ADULT_18_30,
        activityLevel = ActivityLevel.MODERATE,
        wakeUpTime = "07:00",
        sleepTime = "23:00",
        dailyWaterGoal = 2500.0,
        reminderInterval = 60,
        reminderStyle = ReminderStyle.GENTLE
    )
    val userRepository = UserRepository(LocalContext.current)
    val waterIntakeRepository = WaterIntakeRepository(
        waterIntakeDao = object : com.cemcakmak.hydrotracker.data.database.dao.WaterIntakeDao {
            override suspend fun insertEntry(entry: com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry): Long = 0
            override suspend fun insertEntries(entries: List<com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry>) {}
            override fun getEntriesForDate(date: String): kotlinx.coroutines.flow.Flow<List<com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry>> = kotlinx.coroutines.flow.flowOf(emptyList())
            override suspend fun getEntriesForDateSync(date: String): List<com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry> = emptyList()
            override suspend fun getAllEntriesForDateSync(date: String): List<com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry> = emptyList()
            override fun getEntriesForDateRange(startDate: String, endDate: String): kotlinx.coroutines.flow.Flow<List<com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry>> = kotlinx.coroutines.flow.flowOf(emptyList())
            override fun getTotalIntakeForDate(date: String): kotlinx.coroutines.flow.Flow<Double> = kotlinx.coroutines.flow.flowOf(0.0)
            override suspend fun getEntryCountForDate(date: String): Int = 0
            override suspend fun getEntryCount(): Int = 0
            override fun getLast30DaysEntries(): kotlinx.coroutines.flow.Flow<List<com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry>> = kotlinx.coroutines.flow.flowOf(emptyList())
            override suspend fun updateEntry(entry: com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry) {}
            override suspend fun deleteEntry(entry: com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry) {}
            override suspend fun deleteEntryById(entryId: Long) {}
            override suspend fun deleteAllEntries() {}
            override suspend fun hideEntry(entryId: Long) {}
            override suspend fun unhideEntry(entryId: Long) {}
            override fun getHiddenEntries(): kotlinx.coroutines.flow.Flow<List<com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry>> = kotlinx.coroutines.flow.flowOf(emptyList())
            override suspend fun getDailyTotals(startDate: String, endDate: String): List<com.cemcakmak.hydrotracker.data.database.dao.DailyTotal> = emptyList()
        },
        dailySummaryDao = object : com.cemcakmak.hydrotracker.data.database.dao.DailySummaryDao {
            override suspend fun insertSummary(summary: com.cemcakmak.hydrotracker.data.database.entities.DailySummary) {}
            override suspend fun insertSummaries(summaries: List<com.cemcakmak.hydrotracker.data.database.entities.DailySummary>) {}
            override fun getSummaryForDate(date: String): kotlinx.coroutines.flow.Flow<com.cemcakmak.hydrotracker.data.database.entities.DailySummary?> = kotlinx.coroutines.flow.flowOf(null)
            override fun getSummariesForRange(startDate: String, endDate: String): kotlinx.coroutines.flow.Flow<List<com.cemcakmak.hydrotracker.data.database.entities.DailySummary>> = kotlinx.coroutines.flow.flowOf(emptyList())
            override fun getLast30DaysSummaries(): kotlinx.coroutines.flow.Flow<List<com.cemcakmak.hydrotracker.data.database.entities.DailySummary>> = kotlinx.coroutines.flow.flowOf(emptyList())
            override suspend fun updateSummary(summary: com.cemcakmak.hydrotracker.data.database.entities.DailySummary) {}
            override suspend fun deleteSummaryForDate(date: String) {}
            override suspend fun deleteAllSummaries() {}
        },
        userRepository = userRepository,
        context = LocalContext.current
    )
    ProfileScreen(userProfile = userProfile, userRepository = userRepository, waterIntakeRepository = waterIntakeRepository)
}

/**
 * Profile Picture Bottom Sheet Component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePictureBottomSheet(
    showBottomSheet: Boolean,
    onDismiss: () -> Unit,
    onImageSelected: (Uri?) -> Unit
) {
    val context = LocalContext.current
    val bottomSheetState = rememberModalBottomSheetState()

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // The camera image was saved to a temporary file, now save it properly
            val tempFile = File(context.cacheDir, "temp_profile_photo.jpg")
            if (tempFile.exists()) {
                val savedPath = ImageUtils.saveProfileImage(context, Uri.fromFile(tempFile))
                if (savedPath != null) {
                    onImageSelected(savedPath.toUri())
                }
                tempFile.delete() // Clean up temp file
            }
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, launch camera immediately
            val photoFile = File(context.cacheDir, "temp_profile_photo.jpg")
            val photoUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            cameraLauncher.launch(photoUri)
        }
    }
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            // Save the image to local storage
            val savedPath = ImageUtils.saveProfileImage(context, selectedUri)
            if (savedPath != null) {
                onImageSelected(savedPath.toUri())
            }
        }
    }
    
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = bottomSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Update Profile Photo",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                
                // Gallery option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { imagePickerLauncher.launch("image/*") },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Choose from Gallery",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Select an existing photo",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Camera option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Check camera permission
                            when (PackageManager.PERMISSION_GRANTED) {
                                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                                    // Create a temporary file for the camera image
                                    val photoFile = File(context.cacheDir, "temp_profile_photo.jpg")
                                    val photoUri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        photoFile
                                    )
                                    cameraLauncher.launch(photoUri)
                                }
                                else -> {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Take Photo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Use your camera to take a new photo",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Remove photo option (if user has a profile picture)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            ImageUtils.deleteProfileImage(context)
                            onImageSelected(null)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Remove Photo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Use default avatar",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Username Edit Dialog Component
 */
@Composable
fun UsernameEditDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    val isValidName = name.isNotBlank() && name.length <= 15
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Edit Name")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { newName ->
                        if (newName.length <= 15) {
                            name = newName
                        }
                    },
                    label = { Text("Your name") },
                    supportingText = { 
                        Text("${name.length}/15 characters")
                    },
                    isError = !isValidName,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    singleLine = true
                )
                
                if (!isValidName) {
                    Text(
                        text = if (name.isBlank()) "Name cannot be empty" else "Name is too long",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = isValidName
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
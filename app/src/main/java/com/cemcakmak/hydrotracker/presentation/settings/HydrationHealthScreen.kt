package com.cemcakmak.hydrotracker.presentation.settings

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry
import com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository
import com.cemcakmak.hydrotracker.data.models.ActivityLevel
import com.cemcakmak.hydrotracker.data.models.AgeGroup
import com.cemcakmak.hydrotracker.data.models.Gender
import com.cemcakmak.hydrotracker.data.models.HydrationStandard
import com.cemcakmak.hydrotracker.data.models.UserProfile
import com.cemcakmak.hydrotracker.data.repository.UserRepository
import com.cemcakmak.hydrotracker.health.HealthConnectManager
import com.cemcakmak.hydrotracker.health.HealthConnectSyncManager
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HydrationHealthScreen(
    userProfile: UserProfile? = null,
    userRepository: UserRepository? = null,
    waterIntakeRepository: WaterIntakeRepository? = null,
    snackbarHostState: SnackbarHostState? = null,
    healthConnectPermissionLauncher: ActivityResultLauncher<Set<String>>? = null,
    onHydrationStandardChange: (HydrationStandard) -> Unit = {},
    onHealthConnectSyncChange: (Boolean) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    paddingValues: PaddingValues = PaddingValues()
) {
    SettingsDetailScaffold(
        title = "Hydration & Health",
        onNavigateBack = onNavigateBack,
        paddingValues = paddingValues
    ) {
        CalculationStandardSection(
            userProfile = userProfile,
            onHydrationStandardChange = onHydrationStandardChange
        )

        if (HealthConnectManager.isVersionSupported()) {
            HealthConnectSection(
                healthConnectPermissionLauncher = healthConnectPermissionLauncher,
                userProfile = userProfile,
                userRepository = userRepository,
                waterIntakeRepository = waterIntakeRepository,
                snackbarHostState = snackbarHostState,
                onHealthConnectSyncChange = onHealthConnectSyncChange
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CalculationStandardSection(
    userProfile: UserProfile?,
    onHydrationStandardChange: (HydrationStandard) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Column(
        modifier = Modifier.padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        userProfile?.let { profile ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(30.dp),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BlurMorph(targetState = profile.hydrationStandard, label = "standardInfo") { standard, blurModifier ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(blurModifier),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Hydration standard name
                            Text(
                                text = standard.getDisplayName(),
                                style = MaterialTheme.typography.headlineSmall
                            )

                            // Hydration standard description
                            Text(
                                text = "(" + standard.getDescription() + ")",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    HorizontalDivider()

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Daily baseline intake",
                            style = MaterialTheme.typography.bodyMedium
                        )
 
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            HydrationStatChip(
                                label = "Male",
                                value = profile.hydrationStandard.getMaleIntake().toInt() / 1000.0
                            )
                            HydrationStatChip(
                                label = "Female",
                                value = profile.hydrationStandard.getFemaleIntake().toInt() / 1000.0
                            )
                        }
                    }
                }
            }
        }

        SettingsSectionHeader("Calculation standard")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HydrationStandard.entries.forEach { standard ->
                val isSelected = userProfile?.hydrationStandard == standard
                ToggleButton(
                    checked = isSelected,
                    onCheckedChange = {
                        onHydrationStandardChange(standard)
                        haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = when (standard) {
                                HydrationStandard.EFSA -> standard.getDisplayName() + " (European)"
                                HydrationStandard.IOM -> standard.getDisplayName() + " (US)"
                            },
                            style = if (isSelected) MaterialTheme.typography.labelLargeEmphasized else MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = when (standard) {
                                HydrationStandard.EFSA -> "Conservative"
                                HydrationStandard.IOM -> "Higher intake"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> BlurMorph(
    targetState: T,
    label: String,
    content: @Composable (state: T, blurModifier: Modifier) -> Unit
) {
    val fadeSpec = tween<Float>(durationMillis = 450)
    val blurSpec = tween<Dp>(durationMillis = 450)
    AnimatedContent(
        targetState = targetState,
        transitionSpec = {
            (fadeIn(fadeSpec) togetherWith fadeOut(fadeSpec)) using SizeTransform(clip = false)
        },
        label = label
    ) { state ->
        val blur by transition.animateDp(
            transitionSpec = { blurSpec },
            label = "${label}Blur"
        ) { enterExit ->
            if (enterExit == EnterExitState.Visible) 0.dp else 12.dp
        }
        content(state, Modifier.blur(blur, BlurredEdgeTreatment.Unbounded))
    }
}

@Composable
private fun HydrationStatChip(label: String, value: Double) {
    Column(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BlurMorph(targetState = value, label = "statValue") { v, blurModifier ->
            Text(
                text = "$v L",
                style = MaterialTheme.typography.headlineMediumEmphasized,
                color = MaterialTheme.colorScheme.tertiary,
                maxLines = 1,
                modifier = blurModifier
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HealthConnectSection(
    healthConnectPermissionLauncher: ActivityResultLauncher<Set<String>>?,
    userProfile: UserProfile?,
    userRepository: UserRepository?,
    waterIntakeRepository: WaterIntakeRepository?,
    snackbarHostState: SnackbarHostState?,
    onHealthConnectSyncChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val manager = HealthConnectManager
    val inPreview = LocalInspectionMode.current

    var isHealthConnectEnabled by remember { mutableStateOf(false) }
    var healthConnectStatus by remember { mutableStateOf("Checking...") }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    var showHistorySheet by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreTimeRange by remember { mutableStateOf("all") }
    var isRestoring by remember { mutableStateOf(false) }

    // Check Health Connect status on mount / when refresh is triggered
    LaunchedEffect(refreshTrigger) {
        if (inPreview) return@LaunchedEffect
        try {
            val status = manager.getStatusMessage(context)
            healthConnectStatus = status
            isHealthConnectEnabled = status == "Health Connect is ready"
        } catch (e: Exception) {
            healthConnectStatus = "Error: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // Refresh permissions when the app regains focus (returning from Health Connect)
    DisposableEffect(Unit) {
        if (inPreview) return@DisposableEffect onDispose { }
        val activity = context as? ComponentActivity
        val listener = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                if (activity == context) refreshTrigger++
            }
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        }
        activity?.application?.registerActivityLifecycleCallbacks(listener)
        onDispose { activity?.application?.unregisterActivityLifecycleCallbacks(listener) }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsSectionHeader("Health Connect")
        val haptics = LocalHapticFeedback.current

        if (isHealthConnectEnabled || inPreview) {
            Column {
                // Sync toggle
                SettingsGroupCard(index = 0, size = 2) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Crossfade(
                            targetState = userProfile?.healthConnectSyncEnabled,
                            animationSpec = tween(400),
                            label = "autoHideIcon"
                        ) { on ->
                            Icon(
                                imageVector = if (on == true) ImageVector.vectorResource(R.drawable.health_and_safety_filled) else ImageVector.vectorResource(R.drawable.health_and_safety),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sync with Health Connect",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Share hydration data with other health apps",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = userProfile?.healthConnectSyncEnabled == true,
                            onCheckedChange = { enabled ->
                                onHealthConnectSyncChange(enabled)

                                val hapticType = if (enabled) {
                                    HapticFeedbackType.ToggleOn
                                } else {
                                    HapticFeedbackType.ToggleOff
                                }
                                haptics.performHapticFeedback(hapticType)
                            },
                            thumbContent = {
                                val iconType = if (userProfile?.healthConnectSyncEnabled == true) {
                                    ImageVector.vectorResource(R.drawable.sync_filled)
                                } else {
                                    ImageVector.vectorResource(R.drawable.sync)
                                }
                                Icon(
                                    imageVector = iconType,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    }
                }

                // Health Connect data history -> expandable bottom sheet
                SettingsGroupCard(index = 1, size = 2, onClick = { showHistorySheet = true }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.health_connect),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Health Connect Data",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "View your synced intake history",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Restore as a large full-width button
            Button(
                onClick = {
                    if (!isRestoring) showRestoreDialog = true
                    haptics.performHapticFeedback(HapticFeedbackType.ToggleOn) },
                enabled = !isRestoring,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    disabledContainerColor = ButtonDefaults.buttonColors().disabledContainerColor,
                    disabledContentColor = ButtonDefaults.buttonColors().disabledContentColor
                ),
                contentPadding = PaddingValues(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                if (isRestoring) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "Restore from Health Connect",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        } else if (!isLoading &&
            (healthConnectStatus.contains("Permissions") || healthConnectStatus.contains("Missing permissions"))
        ) {
            Button(
                onClick = {
                    if (healthConnectPermissionLauncher != null) {
                        coroutineScope.launch {
                            manager.checkPermissionsAndRun(context, healthConnectPermissionLauncher) {
                                healthConnectStatus = "Health Connect is ready"
                                isHealthConnectEnabled = true
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (healthConnectStatus.contains("Missing")) "Try Again" else "Grant Permissions")
            }
        }
    }

    if (showHistorySheet) {
        HealthConnectHistorySheet(
            waterIntakeRepository = waterIntakeRepository,
            onDismiss = { showHistorySheet = false }
        )
    }

    if (showRestoreDialog) {
        RestoreHealthConnectDialog(
            restoreTimeRange = restoreTimeRange,
            onTimeRangeChange = { restoreTimeRange = it },
            isRestoring = isRestoring,
            canRestore = userRepository != null && waterIntakeRepository != null,
            onConfirm = {
                isRestoring = true
                val since = if (restoreTimeRange == "all") {
                    java.time.Instant.EPOCH
                } else {
                    java.time.Instant.now().minus(90, java.time.temporal.ChronoUnit.DAYS)
                }
                HealthConnectSyncManager.restoreHydroTrackerHistory(
                    context,
                    userRepository!!,
                    waterIntakeRepository!!,
                    since
                ) { imported, skipped ->
                    coroutineScope.launch {
                        isRestoring = false
                        showRestoreDialog = false
                        snackbarHostState?.showSnackbar(
                            "Restored $imported entries, skipped $skipped duplicates"
                        )
                    }
                }
            },
            onDismiss = { showRestoreDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RestoreHealthConnectDialog(
    restoreTimeRange: String,
    onTimeRangeChange: (String) -> Unit,
    isRestoring: Boolean,
    canRestore: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    AlertDialog(
        onDismissRequest = { if (!isRestoring) onDismiss() },
        title = { Text("Restore from Health Connect") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Import your past HydroTracker entries from Health Connect. Choose a time range:")
                val options = listOf(
                    "all" to "All history",
                    "90days" to "Last 90 days"
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    options.forEachIndexed { index, (value, label) ->
                        SelectableOptionCard(
                            index = index,
                            size = options.size,
                            selected = restoreTimeRange == value,
                            selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                            unselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedContentColor = MaterialTheme.colorScheme.onTertiary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            tonalElevation = 0.dp,
                            onClick = {
                                if (!isRestoring) {
                                    haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                                    onTimeRangeChange(value)
                                }
                            }
                        ) { contentColor ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = contentColor
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onConfirm()
                },
                enabled = !isRestoring && canRestore
            ) {
                if (isRestoring) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Restore")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (!isRestoring) {
                        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onDismiss()
                    }
                },
                enabled = !isRestoring
            ) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HealthConnectHistorySheet(
    waterIntakeRepository: WaterIntakeRepository?,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var entries by remember { mutableStateOf<List<WaterIntakeEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(waterIntakeRepository) {
        if (waterIntakeRepository != null) {
            combine(
                waterIntakeRepository.getAllEntries(),
                waterIntakeRepository.getHiddenEntries()
            ) { visible, hidden -> visible + hidden }
                .collect { all ->
                    entries = all
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        HealthConnectHistoryContent(
            entries = entries,
            isLoading = isLoading,
            modifier = Modifier.fillMaxHeight(0.9f)
        )
    }
}

@Composable
private fun HealthConnectHistoryContent(
    entries: List<WaterIntakeEntry>,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Health Connect Data",
            style = MaterialTheme.typography.titleMediumEmphasized,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )
        when {
            isLoading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            entries.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No entries yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(entries.sortedByDescending { it.timestamp }) { entry ->
                    HistoryEntryRow(entry)
                }
            }
        }
    }
}

@Composable
private fun HistoryEntryRow(entry: WaterIntakeEntry) {
    val isExternal = entry.isExternalEntry()
    val isHidden = entry.isHidden
    val formatted = remember(entry.timestamp) {
        java.time.Instant.ofEpochMilli(entry.timestamp)
            .atZone(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.WaterDrop,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${entry.getEffectiveHydrationAmount().toInt()} ml",
                    style = MaterialTheme.typography.titleSmallEmphasized,
                )
                Text(
                    text = formatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isHidden) {
                AssistChip(
                    onClick = {},
                    label = { Text("Hidden", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(24.dp)
                )
            }
            if (isExternal) {
                AssistChip(
                    onClick = {},
                    label = { Text("External", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(24.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HydrationHealthScreenPreview() {
    var previewProfile by remember {
        mutableStateOf(
            UserProfile(
                name = "Preview",
                gender = Gender.MALE,
                ageGroup = AgeGroup.ADULT_31_50,
                activityLevel = ActivityLevel.MODERATE,
                wakeUpTime = "07:00",
                sleepTime = "23:00",
                dailyWaterGoal = 2500.0,
                reminderInterval = 60,
                healthConnectSyncEnabled = true
            )
        )
    }
    HydroTrackerTheme {
        HydrationHealthScreen(
            userProfile = previewProfile,
            onHydrationStandardChange = { previewProfile = previewProfile.copy(hydrationStandard = it) },
            onHealthConnectSyncChange = { previewProfile = previewProfile.copy(healthConnectSyncEnabled = it) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, heightDp = 720)
@Composable
fun HealthConnectHistorySheetPreview() {
    val now = System.currentTimeMillis()
    val sampleEntries = listOf(
        WaterIntakeEntry(
            id = 1,
            amount = 500.0,
            timestamp = now,
            date = "2026-05-31",
            containerType = "Glass",
            containerVolume = 500.0
        ),
        WaterIntakeEntry(
            id = 2,
            amount = 250.0,
            timestamp = now - 2 * 3_600_000L,
            date = "2026-05-31",
            containerType = "Cup",
            containerVolume = 250.0,
            note = "Imported from Samsung Health"
        ),
        WaterIntakeEntry(
            id = 3,
            amount = 750.0,
            timestamp = now - 5 * 3_600_000L,
            date = "2026-05-31",
            containerType = "Bottle",
            containerVolume = 750.0,
            isHidden = true
        ),
        WaterIntakeEntry(
            id = 4,
            amount = 1000.0,
            timestamp = now - 26 * 3_600_000L,
            date = "2026-05-30",
            containerType = "Bottle",
            containerVolume = 1000.0
        ),
        WaterIntakeEntry(
            id = 5,
            amount = 300.0,
            timestamp = now - 30 * 3_600_000L,
            date = "2026-05-30",
            containerType = "Cup",
            containerVolume = 300.0,
            note = "Imported from Fitbit"
        )
    )
    HydroTrackerTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BottomSheetDefaults.DragHandle()
                HealthConnectHistoryContent(
                    entries = sampleEntries,
                    isLoading = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

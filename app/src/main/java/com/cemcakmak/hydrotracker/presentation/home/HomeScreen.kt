package com.cemcakmak.hydrotracker.presentation.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.UserProfile
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.data.models.ContainerPreset
import com.cemcakmak.hydrotracker.data.models.BeverageType
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository
import com.cemcakmak.hydrotracker.data.database.repository.ContainerPresetRepository
import com.cemcakmak.hydrotracker.data.database.repository.WaterProgress
import com.cemcakmak.hydrotracker.data.database.repository.TodayStatistics
import com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry
import com.cemcakmak.hydrotracker.utils.DateTimeFormatters
import com.cemcakmak.hydrotracker.utils.VolumeUnitConverter
import com.cemcakmak.hydrotracker.utils.WaterCalculator
import com.cemcakmak.hydrotracker.presentation.common.showSuccessSnackbar
import com.cemcakmak.hydrotracker.presentation.common.showErrorSnackbar
import com.cemcakmak.hydrotracker.presentation.common.AddContainerPresetBottomSheet
import com.cemcakmak.hydrotracker.presentation.common.EditContainerPresetBottomSheet
import com.cemcakmak.hydrotracker.presentation.common.BeverageOption
import com.cemcakmak.hydrotracker.presentation.common.toOption
import com.cemcakmak.hydrotracker.presentation.common.DailyEntriesSection
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    userProfile: UserProfile,
    themePreferences: ThemePreferences,
    waterIntakeRepository: WaterIntakeRepository,
    containerPresetRepository: ContainerPresetRepository,
    activeBeverages: List<BeverageOption> = BeverageType.getAllSorted().map { it.toOption() },
    paddingValues: PaddingValues,
    snackbarHostState: SnackbarHostState,
    showCustomDialog: Boolean = false,
    onCustomDialogChange: (Boolean) -> Unit = {},
    onFabExpandedChange: (Boolean) -> Unit = {}
) {
    // Check for new user day when HomeScreen is displayed
    LaunchedEffect(Unit) {
        waterIntakeRepository.checkAndHandleNewUserDay()
    }
    // Collect real-time water intake data from database
    val todayProgress by waterIntakeRepository.getTodayProgress().collectAsState(
        initial = WaterProgress(
            currentIntake = 0.0,
            dailyGoal = userProfile.dailyWaterGoal,
            progress = 0f,
            isGoalAchieved = false,
            remainingAmount = userProfile.dailyWaterGoal
        )
    )

    val todayEntries by waterIntakeRepository.getTodayEntries().collectAsState(initial = emptyList())

    val todayStatistics by waterIntakeRepository.getTodayStatistics().collectAsState(
        initial = TodayStatistics(
            totalIntake = 0.0,
            goalProgress = 0f,
            entryCount = 0,
            averageIntake = 0.0,
            largestIntake = 0.0,
            firstIntakeTime = null,
            lastIntakeTime = null,
            isGoalAchieved = false,
            remainingAmount = userProfile.dailyWaterGoal
        )
    )

    // Vibration and haptics
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current

    // Coroutine scope for database operations
    val coroutineScope = rememberCoroutineScope()
    // Custom entry dialogue state managed by parent for FAB hoisting

    // Edit entry dialogue state
    var showEditDialog by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<WaterIntakeEntry?>(null) }

    // Beverage selection state
    var selectedBeverage by remember { mutableStateOf(BeverageType.WATER.toOption()) }

    // Container preset management state
    val presets by containerPresetRepository.getAllPresets().collectAsState(initial = emptyList())
    var showAddPresetSheet by remember { mutableStateOf(false) }
    var showEditPresetSheet by remember { mutableStateOf(false) }
    var presetToEdit by remember { mutableStateOf<ContainerPreset?>(null) }

    // Pull-to-refresh state
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    // Resolved message templates for snackbars launched from coroutines.
    val selectedBeverageLabel = if (selectedBeverage.hasLabelRes) {
        stringResource(selectedBeverage.labelResId)
    } else {
        selectedBeverage.displayName
    }
    val addedMessageTemplate = stringResource(R.string.home_snackbar_added)
    val addFailedMessageTemplate = stringResource(R.string.home_snackbar_add_failed)
    val deletedMessageTemplate = stringResource(R.string.home_snackbar_deleted)
    val deleteFailedMessageTemplate = stringResource(R.string.home_snackbar_delete_failed)
    val updatedMessageTemplate = stringResource(R.string.home_snackbar_updated)
    val updateFailedMessageTemplate = stringResource(R.string.home_snackbar_update_failed)
    val syncedMessageTemplate = stringResource(R.string.home_snackbar_synced)
    val syncErrorsMessageTemplate = stringResource(R.string.home_snackbar_sync_errors)
    val upToDateMessage = stringResource(R.string.home_snackbar_up_to_date)
    val syncFailedMessageTemplate = stringResource(R.string.home_snackbar_sync_failed)
    val containerAddedTemplate = stringResource(R.string.home_snackbar_container_added)
    val containerUpdatedTemplate = stringResource(R.string.home_snackbar_container_updated)
    val containerDeletedTemplate = stringResource(R.string.home_snackbar_container_deleted)

    // Function to add water intake to database
    fun addWaterIntake(amount: Double, containerName: String) {
        coroutineScope.launch {
            val containerPreset = ContainerPreset.getDefaultPresets()
                .find { it.name == containerName }
                ?: ContainerPreset(name = containerName, volume = amount)

            val result = waterIntakeRepository.addWaterIntake(
                amount = amount,
                containerPreset = containerPreset,
                beverageKey = selectedBeverage.storageKey,
                beverageMultiplier = selectedBeverage.storedMultiplier
            )

            result.onSuccess {
                val beverageInfo = if (!selectedBeverage.isWater) {
                    " $selectedBeverageLabel"
                } else {
                    ""
                }
                snackbarHostState.showSuccessSnackbar(
                    message = addedMessageTemplate.format(
                        WaterCalculator.formatWaterAmount(context, amount, userProfile.volumeUnit),
                        beverageInfo
                    )
                )
            }.onFailure { error ->
                snackbarHostState.showErrorSnackbar(
                    message = addFailedMessageTemplate.format(error.message ?: "")
                )
            }
        }
    }

    // Function to delete water intake entry
    fun deleteWaterIntake(entry: WaterIntakeEntry) {
        coroutineScope.launch {
            val result = waterIntakeRepository.deleteWaterIntake(entry)
            
            result.onSuccess {
                snackbarHostState.showSuccessSnackbar(
                    message = deletedMessageTemplate.format(
                        entry.getFormattedAmount(context, userProfile.volumeUnit)
                    )
                )
            }.onFailure { error ->
                snackbarHostState.showErrorSnackbar(
                    message = deleteFailedMessageTemplate.format(error.message ?: "")
                )
            }
        }
    }

    // Function to update water intake entry
    fun updateWaterIntake(oldEntry: WaterIntakeEntry, newEntry: WaterIntakeEntry) {
        coroutineScope.launch {
            val result = waterIntakeRepository.updateWaterIntake(oldEntry, newEntry)

            result.onSuccess {
                snackbarHostState.showSuccessSnackbar(
                    message = updatedMessageTemplate.format(
                        newEntry.getFormattedAmount(context, userProfile.volumeUnit)
                    )
                )
            }.onFailure { error ->
                snackbarHostState.showErrorSnackbar(
                    message = updateFailedMessageTemplate.format(error.message ?: "")
                )
            }
        }
    }

    // Function to perform manual sync with Health Connect
    fun performManualSync() {
        coroutineScope.launch {
            if (userProfile.healthConnectSyncEnabled) {
                isRefreshing = true
                try {
                    // Import external hydration data from the last 30 days
                    val since = java.time.Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS)

                    waterIntakeRepository.getSyncManager().importExternalHydrationData(context, waterIntakeRepository.getUserRepository(), waterIntakeRepository, since) { imported, errors ->
                        coroutineScope.launch {
                            // Always show loading for at least 1.5 seconds for better UX
                            kotlinx.coroutines.delay(1500.milliseconds)

                            when {
                                imported > 0 -> {
                                    snackbarHostState.showSuccessSnackbar(
                                        message = syncedMessageTemplate.format(imported)
                                    )
                                }
                                errors > 0 -> {
                                    snackbarHostState.showErrorSnackbar(
                                        message = syncErrorsMessageTemplate.format(errors)
                                    )
                                }
                                else -> {
                                    snackbarHostState.showSuccessSnackbar(
                                        message = upToDateMessage
                                    )
                                }
                            }
                            isRefreshing = false
                        }
                    }
                } catch (e: Exception) {
                    // Show loading for at least 1.5 seconds even on error
                    kotlinx.coroutines.delay(1500.milliseconds)
                    snackbarHostState.showErrorSnackbar(
                        message = syncFailedMessageTemplate.format(e.message ?: "")
                    )
                    isRefreshing = false
                }
            }
        }
    }

    // Animate the progress value
    val animatedProgress by animateFloatAsState(
        targetValue = todayProgress.progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "progress_animation"
    )

    val scrollState = rememberScrollState()

    // Track scroll direction for FAB collapse/expand
    var lastScrollValue by remember { mutableIntStateOf(0) }
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.value }
            .collect { currentScroll ->
                val isScrollingUp = currentScroll < lastScrollValue
                val isAtTop = currentScroll <= 0
                onFabExpandedChange(isScrollingUp || isAtTop)
                lastScrollValue = currentScroll
            }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = ::performManualSync,
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        state = pullToRefreshState
    ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

            // Daily Progress Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_label_daily_progress),
                        style = MaterialTheme.typography.headlineLargeEmphasized,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Progress amount display
                    Text(
                        text = stringResource(
                            R.string.progress_current_of_goal_format,
                            VolumeUnitConverter.format(context, todayProgress.currentIntake, userProfile.volumeUnit),
                            VolumeUnitConverter.format(context, todayProgress.dailyGoal, userProfile.volumeUnit)
                        ),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Wavy Progress Indicator
                    LinearWavyProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        stroke = WavyProgressIndicatorDefaults.linearIndicatorStroke,
                        trackStroke = WavyProgressIndicatorDefaults.linearTrackStroke,
                        amplitude = WavyProgressIndicatorDefaults.indicatorAmplitude,
                        wavelength = WavyProgressIndicatorDefaults.LinearDeterminateWavelength,
                        waveSpeed = WavyProgressIndicatorDefaults.LinearDeterminateWavelength
                    )

                    // Motivational message
                    Text(
                        text = getMotivationalMessage(todayProgress.progress, userProfile, todayProgress.isGoalAchieved),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Additional stats row
                    if (todayStatistics.entryCount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatChip(
                                label = stringResource(R.string.home_label_entries),
                                value = "${todayStatistics.entryCount}"
                            )
                            todayStatistics.firstIntakeTime?.let { timestamp ->
                                StatChip(
                                    label = stringResource(R.string.home_label_first_intake),
                                    value = DateTimeFormatters.formatTime(
                                        context,
                                        java.time.Instant.ofEpochMilli(timestamp)
                                            .atZone(java.time.ZoneId.systemDefault())
                                            .toLocalTime(),
                                        themePreferences.timeFormat
                                    )
                                )
                            }
                            if (todayStatistics.entryCount > 1) {
                                todayStatistics.lastIntakeTime?.let { timestamp ->
                                    StatChip(
                                        label = stringResource(R.string.home_label_latest_intake),
                                        value = DateTimeFormatters.formatTime(
                                            context,
                                            java.time.Instant.ofEpochMilli(timestamp)
                                                .atZone(java.time.ZoneId.systemDefault())
                                                .toLocalTime(),
                                            themePreferences.timeFormat
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Card (
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = MaterialTheme.shapes.extraLargeIncreased
            ){
                // +1 for the "Add" button at the end
                val carouselItemCount = presets.size + 1
                val carouselState = rememberCarouselState { carouselItemCount }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 12.dp)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_section_quick_select),
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    HorizontalMultiBrowseCarousel(
                        state = carouselState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(135.dp),
                        preferredItemWidth = 130.dp,
                        itemSpacing = 8.dp,
                    ) { index ->
                        if (index < presets.size) {
                            val preset = presets[index]
                            CarouselWaterCard(
                                preset = preset,
                                userProfile = userProfile,
                                onClick = {
                                    addWaterIntake(preset.volume, preset.name)
                                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                },
                                onLongPress = {
                                    presetToEdit = preset
                                    showEditPresetSheet = true
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                modifier = Modifier
                                    .height(130.dp)
                                    .maskClip(MaterialTheme.shapes.extraLarge)
                            )
                        } else {
                            // Add button at the end
                            AddContainerCard(
                                onClick = {
                                    showAddPresetSheet = true
                                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                },
                                modifier = Modifier
                                    .height(130.dp)
                                    .maskClip(MaterialTheme.shapes.extraLarge)
                            )
                        }
                    }

                    // Beverage Selection Section
                    BeverageSelectionSection(
                        selectedBeverage = selectedBeverage,
                        onBeverageChange = { beverage ->
                            selectedBeverage = beverage
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        },
                        beverages = activeBeverages,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Recent Entries Section
                DailyEntriesSection(
                    entries = todayEntries,
                    userProfile = userProfile,
                    themePreferences = themePreferences,
                    onEdit = { entry ->
                        entryToEdit = entry
                        showEditDialog = true
                    },
                    onDelete = { entryToDelete ->
                        deleteWaterIntake(entryToDelete)
                    }
                )
            }

            // Bottom spacing for FAB
            Spacer(modifier = Modifier.height(20.dp))
            }
        }

    // Custom Water Entry Dialogue
    if (showCustomDialog) {
        CustomWaterDialog(
            onDismiss = { onCustomDialogChange(false) },
            onConfirm = { amount ->
                addWaterIntake(amount, "Custom")
                onCustomDialogChange(false)
            },
            selectedBeverage = selectedBeverage,
            onBeverageChange = { newBeverage ->
                selectedBeverage = newBeverage
            },
            volumeUnit = userProfile.volumeUnit,
            beverages = activeBeverages
        )
    }

    // Edit Water Entry Dialogue
    if (showEditDialog && entryToEdit != null) {
        EditWaterDialog(
            entry = entryToEdit!!,
            themePreferences = themePreferences,
            volumeUnit = userProfile.volumeUnit,
            onDismiss = {
                showEditDialog = false
                entryToEdit = null
            },
            onConfirm = { updatedEntry ->
                updateWaterIntake(entryToEdit!!, updatedEntry)
                showEditDialog = false
                entryToEdit = null
            },
            beverages = activeBeverages
        )
    }

    // Add Container Preset Bottom Sheet
    if (showAddPresetSheet) {
        AddContainerPresetBottomSheet(
            volumeUnit = userProfile.volumeUnit,
            onDismiss = { showAddPresetSheet = false },
            onAdd = { name, volume ->
                coroutineScope.launch {
                    containerPresetRepository.addPreset(name, volume)
                    showAddPresetSheet = false
                    snackbarHostState.showSuccessSnackbar(
                        message = containerAddedTemplate.format(name)
                    )
                }
            }
        )
    }

    // Edit Container Preset Bottom Sheet
    if (showEditPresetSheet && presetToEdit != null) {
        EditContainerPresetBottomSheet(
            preset = presetToEdit!!,
            volumeUnit = userProfile.volumeUnit,
            onDismiss = {
                showEditPresetSheet = false
                presetToEdit = null
            },
            onSave = { name, volume ->
                coroutineScope.launch {
                    containerPresetRepository.updatePreset(presetToEdit!!.id, name, volume)
                    showEditPresetSheet = false
                    presetToEdit = null
                    snackbarHostState.showSuccessSnackbar(
                        message = containerUpdatedTemplate.format(name)
                    )
                }
            },
            onDelete = {
                coroutineScope.launch {
                    val deletedName = presetToEdit!!.name
                    containerPresetRepository.deletePreset(presetToEdit!!.id)
                    showEditPresetSheet = false
                    presetToEdit = null
                    snackbarHostState.showSuccessSnackbar(
                        message = containerDeletedTemplate.format(deletedName)
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CarouselWaterCard(
    modifier: Modifier = Modifier,
    preset: ContainerPreset,
    userProfile: UserProfile,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        contentAlignment = Alignment.Center,
) {
        val presetLabel = if (preset.labelResId != 0) {
            stringResource(preset.labelResId)
        } else {
            preset.name
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp, bottom = 10.dp)
        ) {
            when {
                preset.iconRes != null -> {
                    Icon(
                        painter = painterResource(preset.iconRes),
                        contentDescription = presetLabel,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
                preset.icon != null -> {
                    Icon(
                        imageVector = preset.icon,
                        contentDescription = presetLabel,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = presetLabel,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Text(
                text = presetLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Text(
                text = preset.getFormattedVolume(context, userProfile.volumeUnit),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun AddContainerCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.cd_add_container),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.home_add_container_label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomWaterDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
    selectedBeverage: BeverageOption,
    onBeverageChange: (BeverageOption) -> Unit,
    volumeUnit: VolumeUnit,
    beverages: List<BeverageOption> = BeverageType.getAllSorted().map { it.toOption() }
) {
    var amountText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val minAmountMl = 1.0
    val maxAmountMl = 5000.0
    val minAmountDisplay = VolumeUnitConverter.formatValue(minAmountMl, volumeUnit)
    val maxAmountDisplay = VolumeUnitConverter.formatValue(maxAmountMl, volumeUnit)
    val unitShortLabel = stringResource(volumeUnit.shortLabelResId)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLargeIncreased,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_dialog_add_custom_amount),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Beverage Type Selection Dropdown
                var beverageExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = beverageExpanded,
                    onExpandedChange = { beverageExpanded = !beverageExpanded }
                ) {
                    OutlinedTextField(
                        value = if (selectedBeverage.hasLabelRes) {
                            stringResource(selectedBeverage.labelResId)
                        } else {
                            selectedBeverage.displayName
                        },
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.home_label_beverage_type)) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(selectedBeverage.iconRes),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = beverageExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = beverageExpanded,
                        onDismissRequest = { beverageExpanded = false }
                    ) {
                        beverages.forEach { beverage ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(beverage.iconRes),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = if (beverage.hasLabelRes) {
                                                    stringResource(beverage.labelResId)
                                                } else {
                                                    beverage.displayName
                                                },
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                text = stringResource(
                                                    R.string.home_label_hydration_percentage,
                                                    (beverage.hydrationMultiplier * 100).toInt()
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onBeverageChange(beverage)
                                    beverageExpanded = false
                                }
                            )
                        }
                    }
                }

                // Show selected beverage info
                if (!selectedBeverage.isWater) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.home_label_selected_beverage,
                                    if (selectedBeverage.hasLabelRes) {
                                        stringResource(selectedBeverage.labelResId)
                                    } else {
                                        selectedBeverage.displayName
                                    }
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            selectedBeverage.description?.let { description ->
                                val resolvedDescription = if (selectedBeverage.hasDescriptionRes) {
                                    stringResource(selectedBeverage.descriptionResId)
                                } else {
                                    description
                                }
                                Text(
                                    text = resolvedDescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = stringResource(
                                    R.string.home_label_hydration_effectiveness,
                                    (selectedBeverage.hydrationMultiplier * 100).toInt()
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        isError = false
                    },
                    label = { Text(stringResource(R.string.home_label_amount_ml, unitShortLabel)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (volumeUnit == VolumeUnit.MILLILITRES) KeyboardType.Number else KeyboardType.Decimal
                    ),
                    isError = isError,
                    supportingText = if (isError) {
                        { Text(stringResource(R.string.home_error_amount_invalid, minAmountDisplay, maxAmountDisplay)) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_cancel))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        shapes = ButtonDefaults.shapes(),
                        onClick = {
                            val amountInUserUnit = amountText.toDoubleOrNull()
                            if (amountInUserUnit != null && amountInUserUnit > 0) {
                                val amountInMl = VolumeUnitConverter.toMillilitres(amountInUserUnit, volumeUnit)
                                if (amountInMl in minAmountMl..maxAmountMl) {
                                    onConfirm(amountInMl)
                                } else {
                                    isError = true
                                }
                            } else {
                                isError = true
                            }
                        }
                    ) {
                        Text(stringResource(R.string.action_add))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditWaterDialog(
    entry: WaterIntakeEntry,
    themePreferences: ThemePreferences,
    volumeUnit: VolumeUnit,
    onDismiss: () -> Unit,
    onConfirm: (WaterIntakeEntry) -> Unit,
    beverages: List<BeverageOption> = BeverageType.getAllSorted().map { it.toOption() }
) {
    val context = LocalContext.current

    val minAmountMl = 1.0
    val maxAmountMl = 5000.0
    val minAmountDisplay = VolumeUnitConverter.formatValue(minAmountMl, volumeUnit)
    val maxAmountDisplay = VolumeUnitConverter.formatValue(maxAmountMl, volumeUnit)
    val unitShortLabel = stringResource(volumeUnit.shortLabelResId)

    var amountText by remember {
        mutableStateOf(VolumeUnitConverter.formatValue(entry.amount, volumeUnit))
    }
    var containerType by remember { mutableStateOf(entry.containerType) }
    var selectedBeverage by remember {
        mutableStateOf(
            beverages.find { it.storageKey == entry.beverageType }
                ?: beverages.firstOrNull { it.isWater }
                ?: BeverageType.WATER.toOption()
        )
    }
    var isError by remember { mutableStateOf(false) }

    // Time picker state
    var showTimePicker by remember { mutableStateOf(false) }
    val calendar = remember {
        java.util.Calendar.getInstance().apply {
            timeInMillis = entry.timestamp
        }
    }
    var selectedHour by remember { mutableIntStateOf(calendar.get(java.util.Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(calendar.get(java.util.Calendar.MINUTE)) }

    val timePickerState = rememberTimePickerState(
        initialHour = selectedHour,
        initialMinute = selectedMinute,
        is24Hour = true
    )

    val presets = remember { ContainerPreset.getDefaultPresets() }
    val isExternalEntry = entry.isExternalEntry()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLargeIncreased,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(
                        if (isExternalEntry) R.string.home_dialog_external_entry_title
                        else R.string.home_dialog_edit_entry_title
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Warning message for external entries
                if (isExternalEntry) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.home_external_entry_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = stringResource(R.string.home_external_entry_message),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // Container type dropdown (disabled for external entries)
                var expanded by remember { mutableStateOf(false) }

                val customContainerKey = "Custom"
                val displayContainerType = if (containerType == customContainerKey) {
                    stringResource(R.string.home_option_custom)
                } else {
                    containerType
                }

                ExposedDropdownMenuBox(
                    expanded = expanded && !isExternalEntry,
                    onExpandedChange = { if (!isExternalEntry) expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = displayContainerType,
                        onValueChange = { },
                        readOnly = true,
                        enabled = !isExternalEntry,
                        label = { Text(stringResource(R.string.home_label_container_type)) },
                        trailingIcon = {
                            if (!isExternalEntry) {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            }
                        },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )

                    if (!isExternalEntry) {
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            presets.forEach { preset ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (preset.labelResId != 0) {
                                                stringResource(preset.labelResId)
                                            } else {
                                                preset.name
                                            }
                                        )
                                    },
                                    onClick = {
                                        containerType = preset.name
                                        amountText = VolumeUnitConverter.formatValue(preset.volume, volumeUnit)
                                        expanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.home_option_custom)) },
                                onClick = {
                                    containerType = customContainerKey
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Beverage Type Selection Dropdown (disabled for external entries)
                if (!isExternalEntry) {
                    var beverageExpanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = beverageExpanded,
                        onExpandedChange = { beverageExpanded = !beverageExpanded }
                    ) {
                        OutlinedTextField(
                            value = if (selectedBeverage.hasLabelRes) {
                                stringResource(selectedBeverage.labelResId)
                            } else {
                                selectedBeverage.displayName
                            },
                            onValueChange = { },
                            readOnly = true,
                            label = { Text(stringResource(R.string.home_label_beverage_type)) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(selectedBeverage.iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = beverageExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = beverageExpanded,
                            onDismissRequest = { beverageExpanded = false }
                        ) {
                            beverages.forEach { beverage ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(beverage.iconRes),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = if (beverage.hasLabelRes) {
                                                        stringResource(beverage.labelResId)
                                                    } else {
                                                        beverage.displayName
                                                    },
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                                Text(
                                                    text = stringResource(
                                                        R.string.home_label_hydration_percentage,
                                                        (beverage.hydrationMultiplier * 100).toInt()
                                                    ),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedBeverage = beverage
                                        beverageExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Time picker field (disabled for external entries)
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = DateTimeFormatters.formatTime(
                            context,
                            java.time.LocalTime.of(selectedHour, selectedMinute),
                            themePreferences.timeFormat
                        ),
                        onValueChange = { },
                        readOnly = true,
                        enabled = !isExternalEntry,
                        label = { Text(stringResource(R.string.home_label_time)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Invisible clickable overlay to capture clicks
                    if (!isExternalEntry) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showTimePicker = true }
                        )
                    }
                }

                // Amount field (disabled for external entries)
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        if (!isExternalEntry) {
                            amountText = it
                            isError = false
                        }
                    },
                    enabled = !isExternalEntry,
                    label = { Text(stringResource(R.string.home_label_amount_ml, unitShortLabel)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (volumeUnit == VolumeUnit.MILLILITRES) KeyboardType.Number else KeyboardType.Decimal
                    ),
                    isError = isError && !isExternalEntry,
                    supportingText = if (isError && !isExternalEntry) {
                        { Text(stringResource(R.string.home_error_amount_invalid, minAmountDisplay, maxAmountDisplay)) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            stringResource(
                                if (isExternalEntry) R.string.action_close else R.string.action_cancel
                            )
                        )
                    }

                    if (!isExternalEntry) {
                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            shapes = ButtonDefaults.shapes(),
                            onClick = {
                                val amountInUserUnit = amountText.toDoubleOrNull()
                                if (amountInUserUnit != null && amountInUserUnit > 0) {
                                    val amountInMl = VolumeUnitConverter.toMillilitres(amountInUserUnit, volumeUnit)
                                    if (amountInMl in minAmountMl..maxAmountMl) {
                                        // Calculate new timestamp with selected time
                                        val newCalendar = java.util.Calendar.getInstance().apply {
                                            timeInMillis = entry.timestamp
                                            set(java.util.Calendar.HOUR_OF_DAY, selectedHour)
                                            set(java.util.Calendar.MINUTE, selectedMinute)
                                            set(java.util.Calendar.SECOND, 0)
                                            set(java.util.Calendar.MILLISECOND, 0)
                                        }

                                        val updatedEntry = entry.copy(
                                            amount = amountInMl,
                                            containerType = containerType,
                                            beverageType = selectedBeverage.storageKey,
                                            beverageMultiplier = selectedBeverage.storedMultiplier,
                                            timestamp = newCalendar.timeInMillis
                                        )
                                        onConfirm(updatedEntry)
                                    } else {
                                        isError = true
                                    }
                                } else {
                                    isError = true
                                }
                            }
                        ) {
                            Text(stringResource(R.string.action_save))
                        }
                    }
                }
            }
        }
    }

    // Time Picker Dialogue
    if (showTimePicker) {
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLargeIncreased,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_dialog_select_time),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    TimePicker(
                        state = timePickerState,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text(stringResource(R.string.action_cancel))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            shapes = ButtonDefaults.shapes(),
                            onClick = {
                                selectedHour = timePickerState.hour
                                selectedMinute = timePickerState.minute
                                showTimePicker = false
                            }
                        ) {
                            Text(stringResource(R.string.action_confirm))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun RecentEntryItem(
    entry: WaterIntakeEntry,
    userProfile: UserProfile,
    themePreferences: ThemePreferences,
    onEdit: (WaterIntakeEntry) -> Unit = {},
    onDelete: (WaterIntakeEntry) -> Unit = {}
) {
    val context = LocalContext.current
    // Find a matching preset to fetch its icon (res or vector)
    val preset = remember(entry.containerType) {
        ContainerPreset.getDefaultPresets()
            .firstOrNull { it.name == entry.containerType }
    }
    val containerLabel = when {
        preset?.labelResId != 0 && preset?.labelResId != null -> stringResource(preset.labelResId)
        entry.containerType == "Custom" -> stringResource(R.string.home_option_custom)
        else -> entry.containerType
    }
    val beverageEnum = remember(entry.beverageType) {
        BeverageType.entries.find { it.name == entry.beverageType }
    }
    val beverageLabel = if (beverageEnum != null) {
        stringResource(beverageEnum.labelResId)
    } else {
        entry.beverageType
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current

    val dismissState = rememberSwipeToDismissBoxState(
        initialValue = SwipeToDismissBoxValue.Settled,
        positionalThreshold = { distance -> distance * 0.5f }
    )

    // Handle state changes and actions
    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                // Right swipe - Edit
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onEdit(entry)
                // Reset to centre after action
                kotlinx.coroutines.delay(100.milliseconds)
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
            SwipeToDismissBoxValue.EndToStart -> {
                // Left swipe - Show delete confirmation
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                showDeleteDialog = true
                // Reset to centre after showing dialogue
                kotlinx.coroutines.delay(100.milliseconds)
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
            SwipeToDismissBoxValue.Settled -> {
                // No action needed
            }
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier.fillMaxWidth(),
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                SwipeToDismissBoxValue.Settled -> Alignment.Center
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = when (direction) {
                            SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                            SwipeToDismissBoxValue.Settled -> MaterialTheme.colorScheme.surface
                        },
                        shape = MaterialTheme.shapes.extraLargeIncreased
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        // Edit action
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.cd_edit_entry),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(R.string.action_edit),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    SwipeToDismissBoxValue.EndToStart -> {
                        // Delete action
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.action_delete),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.cd_delete_entry),
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    SwipeToDismissBoxValue.Settled -> {
                        // No action
                    }
                }
            }
        }
    ) {
        // Main list item content
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLargeIncreased,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = ListItemDefaults.colors(
                    MaterialTheme.colorScheme.surfaceContainer
                ),
                leadingContent = {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 2.dp,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                preset?.iconRes != null -> {
                                    Icon(
                                        painter = painterResource(preset.iconRes),
                                        contentDescription = containerLabel,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                preset?.icon != null -> {
                                    Icon(
                                        imageVector = preset.icon,
                                        contentDescription = containerLabel,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                else -> {
                                    Icon(
                                        imageVector = Icons.Default.WaterDrop,
                                        contentDescription = containerLabel,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                headlineContent = {
                    Text(
                        text = containerLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                supportingContent = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = entry.getFormattedTime(context, themePreferences.timeFormat),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (entry.beverageType != BeverageType.WATER.name) {
                            Text(
                                text = stringResource(
                                    R.string.home_beverage_effective_format,
                                    beverageLabel,
                                    entry.getFormattedEffectiveAmount(context, userProfile.volumeUnit)
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                trailingContent = {
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = entry.getFormattedAmount(context, userProfile.volumeUnit),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.home_swipe_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            )
        }
    }

    // Delete confirmation dialogue
    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            entry = entry,
            userProfile = userProfile,
            onConfirm = {
                onDelete(entry)
                showDeleteDialog = false
            },
            onDismiss = {
                showDeleteDialog = false
            }
        )
    }
}

@Composable
private fun DeleteConfirmationDialog(
    entry: WaterIntakeEntry,
    userProfile: UserProfile,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val preset = remember(entry.containerType) {
        ContainerPreset.getDefaultPresets()
            .firstOrNull { it.name == entry.containerType }
    }
    val containerLabel = when {
        preset?.labelResId != 0 && preset?.labelResId != null -> stringResource(preset.labelResId)
        entry.containerType == "Custom" -> stringResource(R.string.home_option_custom)
        else -> entry.containerType
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLargeIncreased,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = stringResource(R.string.home_dialog_delete_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = stringResource(
                        R.string.home_dialog_delete_message,
                        entry.getFormattedAmount(context, userProfile.volumeUnit),
                        containerLabel
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = stringResource(R.string.home_dialog_delete_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_cancel))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Text(
                            text = stringResource(R.string.action_delete),
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun getMotivationalMessage(progress: Float, userProfile: UserProfile, isGoalAchieved: Boolean): String {
    return when {
        isGoalAchieved -> stringResource(R.string.home_motivation_goal_achieved)
        progress >= 0.75f -> stringResource(R.string.home_motivation_almost_there)
        progress >= 0.5f -> stringResource(R.string.home_motivation_halfway)
        progress >= 0.25f -> stringResource(R.string.home_motivation_good_start)
        else -> stringResource(userProfile.activityLevel.hydrationTipResId)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BeverageSelectionSection(
    modifier: Modifier = Modifier,
    selectedBeverage: BeverageOption,
    onBeverageChange: (BeverageOption) -> Unit,
    beverages: List<BeverageOption> = BeverageType.getAllSorted().map { it.toOption() },
) {
    val haptics = LocalHapticFeedback.current

    val safeSelected = if (selectedBeverage in beverages) selectedBeverage else beverages.first()
    LaunchedEffect(beverages) {
        if (selectedBeverage !in beverages) onBeverageChange(beverages.firstOrNull { it.isWater } ?: beverages.first())
    }

    Column(
        modifier = modifier.padding(horizontal = 5.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Horizontally scrollable chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(beverages) { beverage ->
                val isSelected = safeSelected == beverage

                FilterChip(
                    shape = if (isSelected){
                        MaterialTheme.shapes.medium
                    } else {
                        MaterialTheme.shapes.extraLarge
                    },
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        onBeverageChange(beverage)
                    },
                    label = {
                        val labelText = if (beverage.hasLabelRes) {
                            stringResource(beverage.labelResId)
                        } else {
                            beverage.displayName
                        }
                        if (isSelected){
                            Text(
                                text = labelText,
                                style = MaterialTheme.typography.labelMediumEmphasized,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = labelText,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    leadingIcon = {
                        if (isSelected){
                            Icon(
                                painter = painterResource(beverage.iconResFilled),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                painter = painterResource(beverage.iconRes),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    selected = isSelected,
                    modifier = Modifier.animateItem()
                )
            }
        }

        // Show selected beverage info with animation
        AnimatedVisibility(
            visible = !safeSelected.isWater,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                expandFrom = Alignment.Top
            ) + scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                initialScale = 0.8f,
                transformOrigin = TransformOrigin(0.5f, 0f)
            ),
            exit = shrinkVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                shrinkTowards = Alignment.Top
            ) + scaleOut(
                animationSpec = tween(150),
                targetScale = 0.9f,
                transformOrigin = TransformOrigin(0.5f, 0f)
            )
        ) {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
                modifier = Modifier
                    .padding(horizontal = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedContent(
                            targetState = (safeSelected.hydrationMultiplier * 100).toInt(),
                            transitionSpec = {
                                slideInVertically { height -> height } + fadeIn() togetherWith
                                slideOutVertically { height -> -height } + fadeOut()
                            },
                            label = "hydration_percentage"
                        ) { percentage ->
                            Text(
                                text = stringResource(
                                    R.string.home_label_effective_hydration,
                                    percentage
                                ),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                    }
                }
            }
        }
    }
}
package com.cemcakmak.hydrotracker.presentation.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.tooling.preview.Preview
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.database.dao.ContainerPresetDao
import com.cemcakmak.hydrotracker.data.database.dao.DailySummaryDao
import com.cemcakmak.hydrotracker.data.database.dao.DailyTotal
import com.cemcakmak.hydrotracker.data.database.dao.WaterIntakeDao
import com.cemcakmak.hydrotracker.data.database.entities.ContainerPresetEntity
import com.cemcakmak.hydrotracker.data.database.entities.DailySummary
import com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry
import com.cemcakmak.hydrotracker.data.database.repository.ContainerPresetRepository
import com.cemcakmak.hydrotracker.data.database.repository.TodayStatistics
import com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository
import com.cemcakmak.hydrotracker.data.database.repository.WaterProgress
import com.cemcakmak.hydrotracker.data.models.ActivityLevel
import com.cemcakmak.hydrotracker.data.models.AgeGroup
import com.cemcakmak.hydrotracker.data.models.BeverageType
import com.cemcakmak.hydrotracker.data.models.ContainerPreset
import com.cemcakmak.hydrotracker.data.models.DayEndMode
import com.cemcakmak.hydrotracker.data.models.Gender
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.models.UserProfile
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.data.repository.UserRepository
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.utils.DateTimeFormatters
import com.cemcakmak.hydrotracker.utils.UserDayCalculator
import com.cemcakmak.hydrotracker.utils.VolumeUnitConverter
import com.cemcakmak.hydrotracker.utils.WaterCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import com.cemcakmak.hydrotracker.presentation.common.showSuccessSnackbar
import com.cemcakmak.hydrotracker.presentation.common.showErrorSnackbar
import com.cemcakmak.hydrotracker.presentation.common.sheets.AddContainerPresetBottomSheet
import com.cemcakmak.hydrotracker.presentation.common.sheets.EditContainerPresetBottomSheet
import com.cemcakmak.hydrotracker.presentation.common.BeverageOption
import com.cemcakmak.hydrotracker.presentation.common.toOption
import com.cemcakmak.hydrotracker.presentation.common.DailyEntriesSection
import com.cemcakmak.hydrotracker.presentation.common.dialogs.EditWaterDialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.cemcakmak.hydrotracker.data.models.EdgeEffect
import com.cemcakmak.hydrotracker.presentation.common.effect.BackdropBlurState
import com.cemcakmak.hydrotracker.presentation.common.effect.BackdropBlurStyle
import com.cemcakmak.hydrotracker.presentation.common.effect.BackdropProgressive
import com.cemcakmak.hydrotracker.presentation.common.effect.backdropBlur
import com.cemcakmak.hydrotracker.presentation.common.effect.backdropSource
import com.cemcakmak.hydrotracker.presentation.common.effect.rememberBackdropBlurState
import com.cemcakmak.hydrotracker.presentation.common.rememberAnimatedDouble
import com.cemcakmak.hydrotracker.presentation.common.timeBasedGreeting
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
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

    val scrollState = rememberScrollState()

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
            val containerPreset = presets.find { it.name == containerName }
                ?: ContainerPreset.getDefaultPresets().find { it.name == containerName }
                ?: ContainerPreset(
                    name = containerName,
                    volume = amount,
                    iconType = "DRAWABLE",
                    iconName = "water_filled"
                )

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
                    val since = Instant.now().minus(30, ChronoUnit.DAYS)

                    waterIntakeRepository.getSyncManager().importExternalHydrationData(context, waterIntakeRepository.getUserRepository(), waterIntakeRepository, since) { imported, errors ->
                        coroutineScope.launch {
                            // Always show loading for at least 1.5 seconds for better UX
                            delay(1500.milliseconds)

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
                    delay(1500.milliseconds)
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

    val edgeEffectStyle = themePreferences.edgeEffect.let {
        if (it == EdgeEffect.BLURRED && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            EdgeEffect.TRANSPARENT
        } else {
            it
        }
    }

    // Backdrop captured for the frosted top band
    val backdropState = rememberBackdropBlurState()

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

    val scaleFraction = {
        if (isRefreshing) 1f
        else LinearOutSlowInEasing.transform(pullToRefreshState.distanceFraction).coerceIn(0f, 1f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = ::performManualSync
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (edgeEffectStyle == EdgeEffect.BLURRED) {
                        Modifier
                            .backdropSource(backdropState)
                            .background(MaterialTheme.colorScheme.background)
                    } else {
                        Modifier
                    }
                )
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(paddingValues.calculateTopPadding()))

            // Daily Progress Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Personal greeting
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = timeBasedGreeting() + " " + userProfile.name,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Hero title
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.home_label_daily_progress),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.displaySmallEmphasized,
                    )
                }

                // Progress amount display
                val animatedCurrentIntake = rememberAnimatedDouble(
                    targetValue = todayProgress.currentIntake / 1000,
                    hapticsEnabled = true
                )

                Text(
                    text = stringResource(
                        R.string.progress_current_of_goal_format,
                        VolumeUnitConverter.format(context, (animatedCurrentIntake * 1000).toDouble(), userProfile.volumeUnit),
                        VolumeUnitConverter.format(context, todayProgress.dailyGoal, userProfile.volumeUnit)
                    ),
                    style = MaterialTheme.typography.headlineMedium
                )

                // Wavy Progress Indicator
                LinearWavyProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .scale(scaleX = 2f, scaleY = 2f)
                        .padding(vertical = 16.dp),
                    progress = { animatedProgress },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                    stroke = WavyProgressIndicatorDefaults.linearIndicatorStroke,
                    trackStroke = WavyProgressIndicatorDefaults.linearTrackStroke,
                    gapSize = WavyProgressIndicatorDefaults.LinearIndicatorTrackGapSize,
                    amplitude = WavyProgressIndicatorDefaults.indicatorAmplitude,
                    wavelength = WavyProgressIndicatorDefaults.LinearIndeterminateWavelength,
                    waveSpeed = WavyProgressIndicatorDefaults.LinearIndeterminateWavelength
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
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedStatItem(
                            label = stringResource(R.string.home_label_entries),
                            targetValue = todayStatistics.entryCount.toDouble(),
                            formatValue = { it.toInt().toString() }
                        )
                        todayStatistics.firstIntakeTime?.let { timestamp ->
                            val firstIntakeTime = Instant.ofEpochMilli(timestamp)
                                .atZone(ZoneId.systemDefault())
                                .toLocalTime()
                            AnimatedStatItem(
                                label = stringResource(R.string.home_label_first_intake),
                                targetValue = (firstIntakeTime.hour * 60 + firstIntakeTime.minute).toDouble(),
                                formatValue = { minutes ->
                                    DateTimeFormatters.formatTime(
                                        context,
                                        LocalTime.of(
                                            (minutes.toInt() / 60).coerceIn(0, 23),
                                            (minutes.toInt() % 60).coerceIn(0, 59)
                                        ),
                                        themePreferences.timeFormat
                                    )
                                }
                            )
                        }
                        if (todayStatistics.entryCount > 1) {
                            todayStatistics.lastIntakeTime?.let { timestamp ->
                                val latestIntakeTime = Instant.ofEpochMilli(timestamp)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalTime()
                                AnimatedStatItem(
                                    label = stringResource(R.string.home_label_latest_intake),
                                    targetValue = (latestIntakeTime.hour * 60 + latestIntakeTime.minute).toDouble(),
                                    formatValue = { minutes ->
                                        DateTimeFormatters.formatTime(
                                            context,
                                            LocalTime.of(
                                                (minutes.toInt() / 60).coerceIn(0, 23),
                                                (minutes.toInt() % 60).coerceIn(0, 59)
                                            ),
                                            themePreferences.timeFormat
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Card (
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(top = 24.dp),
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
                        .padding(top = 16.dp)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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

                    HorizontalUncontainedCarousel(
                        state = carouselState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(155.dp),
                        itemWidth = 150.dp,
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
                                useTertiaryColors = !selectedBeverage.isWater,
                                modifier = Modifier
                                    .height(150.dp)
                                    .maskClip(MaterialTheme.shapes.extraLargeIncreased)
                            )
                        } else {
                            // Add button at the end
                            AddContainerCard(
                                onClick = {
                                    showAddPresetSheet = true
                                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                },
                                useTertiaryColors = !selectedBeverage.isWater,
                                modifier = Modifier
                                    .height(150.dp)
                                    .maskClip(MaterialTheme.shapes.extraLargeIncreased)
                            )
                        }
                    }

                    // Effective hydration info for non-water beverages
                    EffectiveHydrationInfoCard(
                        selectedBeverage = selectedBeverage,
                        beverages = activeBeverages,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Recent Entries Section
                DailyEntriesSection(
                    entries = todayEntries,
                    userProfile = userProfile,
                    themePreferences = themePreferences,
                    tonalElevation = 0.dp,
                    onEdit = { entry ->
                        entryToEdit = entry
                        showEditDialog = true
                    },
                    onDelete = { entryToDelete ->
                        deleteWaterIntake(entryToDelete)
                    }
                )
            }

            Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
        }

        TopEdgeEffect(
            style = edgeEffectStyle,
            backdropState = backdropState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        Box(
            modifier = Modifier.align(Alignment.TopCenter).graphicsLayer {
                scaleX = scaleFraction()
                scaleY = scaleFraction()
            }
        ) {
            PullToRefreshDefaults.LoadingIndicator(state= pullToRefreshState, isRefreshing = isRefreshing)
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
            onAdd = { name, volume, iconType, iconName ->
                coroutineScope.launch {
                    containerPresetRepository.addPreset(name, volume, iconType, iconName)
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
            onSave = { name, volume, iconType, iconName ->
                coroutineScope.launch {
                    containerPresetRepository.updatePreset(presetToEdit!!.id, name, volume, iconType, iconName)
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

/**
 * Top-edge treatment for the Home screen, chosen by the user's [EdgeEffect] setting:
 *  - [EdgeEffect.TRANSPARENT]: nothing (content runs edge-to-edge under the status bar).
 *  - [EdgeEffect.SCRIM]: a plain surface→transparent gradient fade.
 *  - [EdgeEffect.BLURRED]: a variable-radius backdrop blur of the captured content.
 *
 * The band is fixed height (status bar + app bar area) and sits above the content but below the app bar title.
 */
@Composable
private fun TopEdgeEffect(
    style: EdgeEffect,
    backdropState: BackdropBlurState,
    modifier: Modifier = Modifier
) {
    val bandHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 40.dp

    when (style) {
        EdgeEffect.TRANSPARENT -> Unit
        EdgeEffect.SCRIM -> {
            val scrimColor = MaterialTheme.colorScheme.surface
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(bandHeight)
                    .drawBehind {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(scrimColor, Color.Transparent)
                            )
                        )
                    }
            )
        }
        EdgeEffect.BLURRED -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .height(bandHeight)
                        .backdropBlur(
                            state = backdropState,
                            style = BackdropBlurStyle(
                                blurRadius = 10.dp,
                                progressive = BackdropProgressive(
                                    startFraction = 0f,
                                    endFraction = 1f
                                ),
                                tint = MaterialTheme.colorScheme.surface.copy(0.3f)
                            )
                        )
                )
            }
        }
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
    useTertiaryColors: Boolean = false
) {
    val context = LocalContext.current
    val containerColor by animateColorAsState(
        targetValue = if (useTertiaryColors) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.primary
        },
        animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
        label = "carousel_card_container_color"
    )
    val contentColor by animateColorAsState(
        targetValue = if (useTertiaryColors) {
            MaterialTheme.colorScheme.onTertiary
        } else {
            MaterialTheme.colorScheme.onPrimary
        },
        animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
        label = "carousel_card_content_color"
    )

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraLargeIncreased)
            .background(containerColor)
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
            verticalArrangement = Arrangement.spacedBy(12.dp, alignment = Alignment.CenterVertically),
            modifier = Modifier
                .fillMaxSize()
        ) {
            when {
                preset.iconRes != null -> {
                    Icon(
                        painter = painterResource(preset.iconRes),
                        contentDescription = presetLabel,
                        tint = contentColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = presetLabel,
                        tint = contentColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = presetLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    maxLines = 2
                )

                Text(
                    text = preset.getFormattedVolume(context, userProfile.volumeUnit),
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun AddContainerCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    useTertiaryColors: Boolean = false
) {
    val containerColor by animateColorAsState(
        targetValue = if (useTertiaryColors) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
        label = "carousel_card_container_color"
    )
    val contentColor by animateColorAsState(
        targetValue = if (useTertiaryColors) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        },
        animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
        label = "carousel_card_content_color"
    )

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraExtraLarge)
            .background(containerColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, alignment = Alignment.CenterVertically),
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.add_filled),
                contentDescription = stringResource(R.string.cd_add_container),
                tint = contentColor,
                modifier = Modifier.size(32.dp)
            )

            Text(
                text = stringResource(R.string.home_add_container_label),
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                textAlign = TextAlign.Center
            )
        }
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

    // Horizontally scrollable connected toggle-button group
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        beverages.forEach { beverage ->
            val isSelected = safeSelected == beverage

            val labelText = if (beverage.hasLabelRes) {
                stringResource(beverage.labelResId)
            } else {
                beverage.displayName
            }

            val useTertiaryColors = isSelected && !safeSelected.isWater
            val checkedContainerColor by animateColorAsState(
                targetValue = if (useTertiaryColors) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.primary
                },
                animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
                label = "beverage_toggle_container_color"
            )
            val checkedContentColor by animateColorAsState(
                targetValue = if (useTertiaryColors) {
                    MaterialTheme.colorScheme.onTertiary
                } else {
                    MaterialTheme.colorScheme.onPrimary
                },
                animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
                label = "beverage_toggle_content_color"
            )

            ToggleButton(
                modifier = Modifier
                    .semantics { role = Role.RadioButton },
                checked = isSelected,
                onCheckedChange = {
                    haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    onBeverageChange(beverage)
                },
                colors = ToggleButtonDefaults.toggleButtonColors(
                    checkedContainerColor = checkedContainerColor,
                    checkedContentColor = checkedContentColor,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Crossfade(
                        targetState = isSelected,
                        animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
                        label = "containerSelectionToggleIcon"
                    ) { selected ->
                        Icon(
                            painter = painterResource(
                                if (selected) beverage.iconResFilled else beverage.iconRes
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = labelText,
                        style = if (isSelected) {
                            MaterialTheme.typography.labelMediumEmphasized
                        } else {
                            MaterialTheme.typography.labelMedium
                        }
                    )
                }
            }
        }
    }
}

/**
 * Displays the effective hydration percentage for the selected non-water beverage.
 * Intended to appear below the preset carousel.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EffectiveHydrationInfoCard(
    modifier: Modifier = Modifier,
    selectedBeverage: BeverageOption,
    beverages: List<BeverageOption> = BeverageType.getAllSorted().map { it.toOption() }
) {
    val safeSelected = if (selectedBeverage in beverages) selectedBeverage else beverages.first()

    AnimatedVisibility(
        modifier = modifier,
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
            initialScale = 0.3f,
            transformOrigin = TransformOrigin(0.5f, 0f)
        ),
        exit = shrinkVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            shrinkTowards = Alignment.Top
        ) + scaleOut(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            targetScale = 0.3f,
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

@Composable
private fun AnimatedStatItem(
    label: String,
    targetValue: Double,
    hapticsEnabled: Boolean = false,
    formatValue: @Composable (Float) -> String
) {
    val animatedValue = rememberAnimatedDouble(
        targetValue = targetValue,
        hapticsEnabled = hapticsEnabled
    )
    ChartStatItem(
        label = label,
        value = formatValue(animatedValue)
    )
}

@Composable
private fun ChartStatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
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
//region Preview

/** Static user profile used by the HomeScreen preview. */
private val previewUserProfile = UserProfile(
    name = "Preview User",
    gender = Gender.MALE,
    ageGroup = AgeGroup.ADULT_31_50,
    activityLevel = ActivityLevel.MODERATE,
    wakeUpTime = "07:00",
    sleepTime = "23:00",
    dailyWaterGoal = 2700.0,
    reminderInterval = 60,
    volumeUnit = VolumeUnit.MILLILITRES,
    healthConnectSyncEnabled = false
)

/** Fixed sample entries shown in the preview. */
private val previewEntries: List<WaterIntakeEntry>
    get() {
        val now = System.currentTimeMillis()
        val date = UserDayCalculator.getCurrentUserDayString(
            wakeUpTime = "07:00",
            dayEndMode = DayEndMode.SLEEP_TIME
        )
        return listOf(
            WaterIntakeEntry(
                id = 1,
                amount = 300.0,
                timestamp = now - 3_600_000 * 4,
                date = date,
                containerType = "Medium Glass",
                containerVolume = 200.0,
                beverageType = BeverageType.WATER.name,
                iconType = "DRAWABLE",
                iconName = "water_medium"
            ),
            WaterIntakeEntry(
                id = 2,
                amount = 500.0,
                timestamp = now - 3_600_000 * 2,
                date = date,
                containerType = "Water Bottle",
                containerVolume = 500.0,
                beverageType = BeverageType.WATER.name,
                iconType = "DRAWABLE",
                iconName = "water_bottle"
            ),
            WaterIntakeEntry(
                id = 3,
                amount = 550.0,
                timestamp = now - 3_600_000,
                date = date,
                containerType = "Coffee Cup",
                containerVolume = 100.0,
                beverageType = BeverageType.COFFEE.name,
                beverageMultiplier = BeverageType.COFFEE.hydrationMultiplier,
                iconType = "DRAWABLE",
                iconName = "local_cafe"
            )
        )
    }

/** Preview-time DAO that returns static water intake data. */
private class PreviewWaterIntakeDao : WaterIntakeDao {
    override fun getEntriesForDate(date: String): Flow<List<WaterIntakeEntry>> = flowOf(previewEntries)
    override suspend fun getEntriesForDateSync(date: String): List<WaterIntakeEntry> = previewEntries
    override suspend fun getAllEntriesForDateSync(date: String): List<WaterIntakeEntry> = previewEntries
    override fun getEntriesForDateRange(startDate: String, endDate: String): Flow<List<WaterIntakeEntry>> =
        flowOf(previewEntries)
    override fun getTotalIntakeForDate(date: String): Flow<Double> = flowOf(1350.0)
    override suspend fun getEntryCountForDate(date: String): Int = previewEntries.size
    override suspend fun getEntryCount(): Int = previewEntries.size
    override fun getLast30DaysEntries(): Flow<List<WaterIntakeEntry>> = flowOf(previewEntries)
    override fun getAllEntries(): Flow<List<WaterIntakeEntry>> = flowOf(previewEntries)
    override suspend fun updateEntry(entry: WaterIntakeEntry) {}
    override suspend fun deleteEntry(entry: WaterIntakeEntry) {}
    override suspend fun deleteEntryById(entryId: Long) {}
    override suspend fun deleteAllEntries() {}
    override suspend fun hideEntry(entryId: Long) {}
    override suspend fun unhideEntry(entryId: Long) {}
    override fun getHiddenEntries(): Flow<List<WaterIntakeEntry>> = flowOf(emptyList())
    override suspend fun getDailyTotals(startDate: String, endDate: String): List<DailyTotal> = emptyList()
    override suspend fun insertEntry(entry: WaterIntakeEntry): Long = 1L
    override suspend fun insertEntries(entries: List<WaterIntakeEntry>) {}
}

/** Preview-time DAO that returns empty summaries. */
private class PreviewDailySummaryDao : DailySummaryDao {
    override suspend fun insertSummary(summary: DailySummary) {}
    override suspend fun insertSummaries(summaries: List<DailySummary>) {}
    override fun getSummaryForDate(date: String): Flow<DailySummary?> = flowOf(null)
    override fun getSummariesForRange(startDate: String, endDate: String): Flow<List<DailySummary>> =
        flowOf(emptyList())
    override fun getLast30DaysSummaries(): Flow<List<DailySummary>> = flowOf(emptyList())
    override fun getAllSummaries(): Flow<List<DailySummary>> = flowOf(emptyList())
    override suspend fun updateSummary(summary: DailySummary) {}
    override suspend fun deleteSummaryForDate(date: String) {}
    override suspend fun deleteAllSummaries() {}
}

/** Preview-time DAO that seeds the default container presets. */
private class PreviewContainerPresetDao : ContainerPresetDao {
    private val defaultEntities = ContainerPresetRepository.getDefaultPresetEntities()

    override fun getAllPresets(): Flow<List<ContainerPresetEntity>> = flowOf(defaultEntities)
    override suspend fun getAllPresetsSync(): List<ContainerPresetEntity> = defaultEntities
    override suspend fun getPresetById(id: Long): ContainerPresetEntity? = defaultEntities.find { it.id == id }
    override suspend fun insertPreset(preset: ContainerPresetEntity): Long = 1L
    override suspend fun insertPresets(presets: List<ContainerPresetEntity>) {}
    override suspend fun updatePreset(preset: ContainerPresetEntity) {}
    override suspend fun deletePresetById(id: Long) {}
    override suspend fun deleteAllPresets() {}
    override suspend fun getPresetCount(): Int = defaultEntities.size
    override suspend fun getMaxDisplayOrder(): Int = defaultEntities.size
    override suspend fun updateDisplayOrder(id: Long, displayOrder: Int) {}
    override suspend fun reorderPresets(orderedIds: List<Long>) {}
}

@Preview(showBackground = true, name = "Home Screen")
@Composable
private fun HomeScreenPreview() {
    val context = LocalContext.current
    val waterRepository = WaterIntakeRepository(
        waterIntakeDao = PreviewWaterIntakeDao(),
        dailySummaryDao = PreviewDailySummaryDao(),
        userRepository = UserRepository(context),
        context = context
    )
    val containerRepository = ContainerPresetRepository(PreviewContainerPresetDao())

    HydroTrackerTheme {
        HomeScreen(
            userProfile = previewUserProfile,
            themePreferences = ThemePreferences(),
            waterIntakeRepository = waterRepository,
            containerPresetRepository = containerRepository,
            paddingValues = PaddingValues(0.dp),
            snackbarHostState = SnackbarHostState()
        )
    }
}

//endregion

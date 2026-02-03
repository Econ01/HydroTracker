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
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import com.cemcakmak.hydrotracker.data.models.UserProfile
import com.cemcakmak.hydrotracker.data.models.ContainerPreset
import com.cemcakmak.hydrotracker.data.models.BeverageType
import com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository
import com.cemcakmak.hydrotracker.data.database.repository.ContainerPresetRepository
import com.cemcakmak.hydrotracker.health.HealthConnectManager
import com.cemcakmak.hydrotracker.health.HealthConnectSyncManager
import com.cemcakmak.hydrotracker.data.database.repository.WaterProgress
import com.cemcakmak.hydrotracker.data.database.repository.TodayStatistics
import com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry
import com.cemcakmak.hydrotracker.utils.WaterCalculator
import com.cemcakmak.hydrotracker.presentation.common.HydroSnackbarHost
import com.cemcakmak.hydrotracker.presentation.common.showSuccessSnackbar
import com.cemcakmak.hydrotracker.presentation.common.showErrorSnackbar
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    userProfile: UserProfile,
    waterIntakeRepository: WaterIntakeRepository,
    containerPresetRepository: ContainerPresetRepository,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
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
    val snackbarHostState = remember { SnackbarHostState() }

    // Custom entry dialog state
    var showCustomDialog by remember { mutableStateOf(false) }

    // Edit entry dialog state
    var showEditDialog by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<WaterIntakeEntry?>(null) }

    // Beverage selection state
    var selectedBeverageType by remember { mutableStateOf(BeverageType.WATER) }

    // Container preset management state
    val presets by containerPresetRepository.getAllPresets().collectAsState(initial = emptyList())
    var showAddPresetSheet by remember { mutableStateOf(false) }
    var showEditPresetSheet by remember { mutableStateOf(false) }
    var presetToEdit by remember { mutableStateOf<ContainerPreset?>(null) }

    // Pull-to-refresh state
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    // Function to add water intake to database
    fun addWaterIntake(amount: Double, containerName: String) {
        coroutineScope.launch {
            val containerPreset = ContainerPreset.getDefaultPresets()
                .find { it.name == containerName }
                ?: ContainerPreset(name = "Custom", volume = amount)

            val result = waterIntakeRepository.addWaterIntake(
                amount = amount,
                containerPreset = containerPreset,
                beverageType = selectedBeverageType
            )

            result.onSuccess {
                val beverageInfo = if (selectedBeverageType != BeverageType.WATER) {
                    " ${selectedBeverageType.displayName}"
                } else {
                    ""
                }
                snackbarHostState.showSuccessSnackbar(
                    message = "Added ${WaterCalculator.formatWaterAmount(amount)}$beverageInfo!"
                )
            }.onFailure { error ->
                snackbarHostState.showErrorSnackbar(
                    message = "Failed to add water: ${error.message}"
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
                    message = "Deleted ${entry.getFormattedAmount()} entry"
                )
            }.onFailure { error ->
                snackbarHostState.showErrorSnackbar(
                    message = "Failed to delete entry: ${error.message}"
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
                    message = "Updated entry to ${newEntry.getFormattedAmount()}"
                )
            }.onFailure { error ->
                snackbarHostState.showErrorSnackbar(
                    message = "Failed to update entry: ${error.message}"
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
                            kotlinx.coroutines.delay(1500)

                            when {
                                imported > 0 -> {
                                    snackbarHostState.showSuccessSnackbar(
                                        message = "Synced $imported entries from Health Connect"
                                    )
                                }
                                errors > 0 -> {
                                    snackbarHostState.showErrorSnackbar(
                                        message = "Sync completed with $errors errors"
                                    )
                                }
                                else -> {
                                    snackbarHostState.showSuccessSnackbar(
                                        message = "Data is up to date"
                                    )
                                }
                            }
                            isRefreshing = false
                        }
                    }
                } catch (e: Exception) {
                    // Show loading for at least 1.5 seconds even on error
                    kotlinx.coroutines.delay(1500)
                    snackbarHostState.showErrorSnackbar(
                        message = "Sync failed: ${e.message}"
                    )
                    isRefreshing = false
                }
            } else {
                // Show loading animation even when disabled for consistency
                kotlinx.coroutines.delay(1500)
                snackbarHostState.showSnackbar(
                    message = "Health Connect sync is disabled",
                    actionLabel = "Enable"
                ).let { result ->
                    if (result == SnackbarResult.ActionPerformed) {
                        onNavigateToSettings()
                    }
                }
                isRefreshing = false
            }
        }
    }

    // Animation states
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Animate the progress value
    val animatedProgress by animateFloatAsState(
        targetValue = todayProgress.progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "progress_animation"
    )

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollState = rememberScrollState()

    val elevated by remember {
        derivedStateOf { scrollBehavior.state.collapsedFraction > 0f }
    }
    val animatedElevation by animateDpAsState(
        targetValue = if (elevated) 6.dp else 0.dp,
        label = "AppBarElevation"
    )

    // Track scroll direction for FAB collapse/expand
    var lastScrollValue by remember { mutableIntStateOf(0) }
    val fabExpanded by remember {
        derivedStateOf { 
            val currentScroll = scrollState.value
            val isScrollingUp = currentScroll < lastScrollValue
            val isAtTop = currentScroll <= 0
            
            // Update last scroll value for next comparison
            lastScrollValue = currentScroll
            
            // Expand when scrolling up or at the top, collapse when scrolling down
            isScrollingUp || isAtTop
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Surface(
                tonalElevation = animatedElevation,
                shadowElevation = animatedElevation
            ) {
                TopAppBar(
                    scrollBehavior = scrollBehavior,
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "HydroTracker",
                                style = MaterialTheme.typography.headlineLargeEmphasized,
                                color = MaterialTheme.colorScheme.onSurface,
                            )

                            // Health Connect Sync Status Icon
                            HealthConnectSyncIcon(
                                userProfile = userProfile,
                                waterIntakeRepository = waterIntakeRepository,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = true,
                    alwaysShowLabel = true,
                    onClick = { }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "History") },
                    label = { Text("History") },
                    selected = false,
                    alwaysShowLabel = true,
                    onClick = onNavigateToHistory
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = false,
                    alwaysShowLabel = true,
                    onClick = onNavigateToProfile
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCustomDialog = true
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick) },
                expanded = fabExpanded,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Add Custom Amount"
                    )
                },
                text = {
                    Text(
                        text = "Add Custom",
                        style = MaterialTheme.typography.labelLargeEmphasized
                    )
                }
            )
        },
        snackbarHost = { HydroSnackbarHost(snackbarHostState) }
    ) { paddingValues ->
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
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    initialOffsetY = { it / 3 }
                ) + fadeIn(animationSpec = tween(600, delayMillis = 200))
            ) {
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
                            text = "Daily Progress",
                            style = MaterialTheme.typography.headlineLargeEmphasized,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Progress amount display
                        Text(
                            text = "${todayProgress.getFormattedCurrent()} / ${todayProgress.getFormattedGoal()}",
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
                                    label = "Entries",
                                    value = "${todayStatistics.entryCount}"
                                )
                                if (todayStatistics.firstIntakeTime != null) {
                                    StatChip(
                                        label = "First",
                                        value = todayStatistics.firstIntakeTime!!
                                    )
                                }
                                if (todayStatistics.lastIntakeTime != null && todayStatistics.entryCount > 1) {
                                    StatChip(
                                        label = "Latest",
                                        value = todayStatistics.lastIntakeTime!!
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
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        initialOffsetY = { it / 2 }
                    ) + fadeIn(animationSpec = tween(600, delayMillis = 400))
                ) {
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
                            text = "Quick Select",
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
                            selectedBeverageType = selectedBeverageType,
                            onBeverageTypeChange = { beverageType ->
                                selectedBeverageType = beverageType
                                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Recent Entries Section
                if (todayEntries.isNotEmpty()) {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            initialOffsetY = { it / 2 }
                        ) + fadeIn(animationSpec = tween(600, delayMillis = 500))
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Recent Entries",
                                    style = MaterialTheme.typography.titleLargeEmphasized,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )

                                todayEntries.forEach { entry ->
                                    key(entry.id) {
                                        RecentEntryItem(
                                            entry = entry,
                                            onEdit = { entry ->
                                                entryToEdit = entry
                                                showEditDialog = true
                                            },
                                            onDelete = { entryToDelete ->
                                                deleteWaterIntake(entryToDelete)
                                            }
                                        )
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom spacing for FAB
            Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Custom Water Entry Dialog
    if (showCustomDialog) {
        CustomWaterDialog(
            onDismiss = { showCustomDialog = false },
            onConfirm = { amount ->
                addWaterIntake(amount, "Custom")
                showCustomDialog = false
            },
            selectedBeverageType = selectedBeverageType,
            onBeverageTypeChange = { newType ->
                selectedBeverageType = newType
            }
        )
    }

    // Edit Water Entry Dialog
    if (showEditDialog && entryToEdit != null) {
        EditWaterDialog(
            entry = entryToEdit!!,
            onDismiss = {
                showEditDialog = false
                entryToEdit = null
            },
            onConfirm = { updatedEntry ->
                updateWaterIntake(entryToEdit!!, updatedEntry)
                showEditDialog = false
                entryToEdit = null
            }
        )
    }

    // Add Container Preset Bottom Sheet
    if (showAddPresetSheet) {
        AddContainerPresetBottomSheet(
            onDismiss = { showAddPresetSheet = false },
            onAdd = { name, volume ->
                coroutineScope.launch {
                    containerPresetRepository.addPreset(name, volume)
                    showAddPresetSheet = false
                    snackbarHostState.showSuccessSnackbar(
                        message = "Added \"$name\" container"
                    )
                }
            }
        )
    }

    // Edit Container Preset Bottom Sheet
    if (showEditPresetSheet && presetToEdit != null) {
        EditContainerPresetBottomSheet(
            preset = presetToEdit!!,
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
                        message = "Updated \"$name\" container"
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
                        message = "Deleted \"$deletedName\" container"
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CarouselWaterCard(
    preset: ContainerPreset,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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
                        contentDescription = preset.name,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
                preset.icon != null -> {
                    Icon(
                        imageVector = preset.icon,
                        contentDescription = preset.name,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = preset.name,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Text(
                text = preset.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Text(
                text = preset.getFormattedVolume(),
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
                contentDescription = "Add container",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Add",
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
    selectedBeverageType: BeverageType,
    onBeverageTypeChange: (BeverageType) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

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
                    text = "Add Custom Amount",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Beverage Type Selection Dropdown
                val beverageTypes = BeverageType.getAllSorted()
                var beverageExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = beverageExpanded,
                    onExpandedChange = { beverageExpanded = !beverageExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedBeverageType.displayName,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Beverage Type") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(selectedBeverageType.iconRes),
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
                        beverageTypes.forEach { beverageType ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(beverageType.iconRes),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = beverageType.displayName,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                text = "${(beverageType.hydrationMultiplier * 100).toInt()}% hydration",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onBeverageTypeChange(beverageType)
                                    beverageExpanded = false
                                }
                            )
                        }
                    }
                }

                // Show selected beverage info
                if (selectedBeverageType != BeverageType.WATER) {
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
                                text = "Selected: ${selectedBeverageType.displayName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = selectedBeverageType.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Hydration effectiveness: ${(selectedBeverageType.hydrationMultiplier * 100).toInt()}%",
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
                    label = { Text("Amount (ml)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("Please enter a valid amount (1-5000 ml)") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        shapes = ButtonDefaults.shapes(),
                        onClick = {
                            val amount = amountText.toDoubleOrNull()
                            if (amount != null && amount > 0 && amount <= 5000) {
                                onConfirm(amount)
                            } else {
                                isError = true
                            }
                        }
                    ) {
                        Text("Add")
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
    onDismiss: () -> Unit,
    onConfirm: (WaterIntakeEntry) -> Unit
) {
    var amountText by remember { mutableStateOf(entry.amount.toString()) }
    var containerType by remember { mutableStateOf(entry.containerType) }
    var selectedBeverageType by remember { mutableStateOf(entry.getBeverageType()) }
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
                    text = if (isExternalEntry) "External Water Entry" else "Edit Water Entry",
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
                                    text = "Entry from another app",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "This entry was imported from another health app and cannot be edited. You can only view its details.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // Container type dropdown (disabled for external entries)
                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded && !isExternalEntry,
                    onExpandedChange = { if (!isExternalEntry) expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = containerType,
                        onValueChange = { },
                        readOnly = true,
                        enabled = !isExternalEntry,
                        label = { Text("Container Type") },
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
                                    text = { Text(preset.name) },
                                    onClick = {
                                        containerType = preset.name
                                        amountText = preset.volume.toString()
                                        expanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Custom") },
                                onClick = {
                                    containerType = "Custom"
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Beverage Type Selection Dropdown (disabled for external entries)
                if (!isExternalEntry) {
                    val beverageTypes = BeverageType.getAllSorted()
                    var beverageExpanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = beverageExpanded,
                        onExpandedChange = { beverageExpanded = !beverageExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedBeverageType.displayName,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Beverage Type") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(selectedBeverageType.iconRes),
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
                            beverageTypes.forEach { beverageType ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(beverageType.iconRes),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = beverageType.displayName,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                                Text(
                                                    text = "${(beverageType.hydrationMultiplier * 100).toInt()}% hydration",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedBeverageType = beverageType
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
                        value = String.format(java.util.Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute),
                        onValueChange = { },
                        readOnly = true,
                        enabled = !isExternalEntry,
                        label = { Text("Time") },
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
                    label = { Text("Amount (ml)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isError && !isExternalEntry,
                    supportingText = if (isError && !isExternalEntry) {
                        { Text("Please enter a valid amount (1-5000 ml)") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (isExternalEntry) "Close" else "Cancel")
                    }

                    if (!isExternalEntry) {
                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            shapes = ButtonDefaults.shapes(),
                            onClick = {
                                val amount = amountText.toDoubleOrNull()
                                if (amount != null && amount > 0 && amount <= 5000) {
                                    // Calculate new timestamp with selected time
                                    val newCalendar = java.util.Calendar.getInstance().apply {
                                        timeInMillis = entry.timestamp
                                        set(java.util.Calendar.HOUR_OF_DAY, selectedHour)
                                        set(java.util.Calendar.MINUTE, selectedMinute)
                                        set(java.util.Calendar.SECOND, 0)
                                        set(java.util.Calendar.MILLISECOND, 0)
                                    }

                                    val updatedEntry = entry.copy(
                                        amount = amount,
                                        containerType = containerType,
                                        beverageType = selectedBeverageType.name,
                                        timestamp = newCalendar.timeInMillis
                                    )
                                    onConfirm(updatedEntry)
                                } else {
                                    isError = true
                                }
                            }
                        ) {
                            Text("Update")
                        }
                    }
                }
            }
        }
    }

    // Time Picker Dialog
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
                        text = "Select Time",
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
                            Text("Cancel")
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
                            Text("OK")
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
    onEdit: (WaterIntakeEntry) -> Unit = {},
    onDelete: (WaterIntakeEntry) -> Unit = {}
) {
    // Find a matching preset to fetch its icon (res or vector)
    val preset = remember(entry.containerType) {
        ContainerPreset.getDefaultPresets()
            .firstOrNull { it.name == entry.containerType }
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
                // Reset to center after action
                kotlinx.coroutines.delay(100)
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
            SwipeToDismissBoxValue.EndToStart -> {
                // Left swipe - Show delete confirmation
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                showDeleteDialog = true
                // Reset to center after showing dialog
                kotlinx.coroutines.delay(100)
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
                                contentDescription = "Edit entry",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Edit",
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
                                text = "Delete",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete entry",
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
                                        contentDescription = preset.name,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                preset?.icon != null -> {
                                    Icon(
                                        imageVector = preset.icon,
                                        contentDescription = preset.name,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                else -> {
                                    Icon(
                                        imageVector = Icons.Default.WaterDrop,
                                        contentDescription = entry.containerType,
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
                        text = entry.containerType,
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
                            text = entry.getFormattedTime(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (entry.getBeverageType() != BeverageType.WATER) {
                            Text(
                                text = "${entry.getBeverageType().displayName} • ${entry.getFormattedEffectiveAmount()} effective",
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
                            text = entry.getFormattedAmount(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Edit → • ← Delete",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            )
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            entry = entry,
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
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
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
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Delete Entry",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Are you sure you want to delete this ${entry.getFormattedAmount()} ${entry.containerType} entry?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
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
                            text = "Delete",
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }
        }
    }
}

private fun getMotivationalMessage(progress: Float, userProfile: UserProfile, isGoalAchieved: Boolean): String {
    return when {
        isGoalAchieved -> "🎉 Amazing! You've reached your daily goal!"
        progress >= 0.75f -> "💪 You're doing great! Almost there!"
        progress >= 0.5f -> "🌟 Halfway there! Keep up the good work!"
        progress >= 0.25f -> "👍 Good start! Stay consistent!"
        else -> userProfile.activityLevel.getHydrationTip()
    }
}

@Composable
private fun HealthConnectSyncIcon(
    userProfile: UserProfile,
    waterIntakeRepository: WaterIntakeRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var syncStatus by remember { mutableStateOf(HealthConnectSyncManager.SyncStatus.DISABLED) }

    // Get the HealthConnectSyncManager from the waterIntakeRepository
    val syncManager = remember { waterIntakeRepository.getSyncManager() }

    // Monitor sync state from the actual sync manager
    val isSyncing by syncManager.isSyncing.collectAsState()

    // Check sync status
    LaunchedEffect(userProfile.healthConnectSyncEnabled) {
        if (!userProfile.healthConnectSyncEnabled) {
            syncStatus = HealthConnectSyncManager.SyncStatus.DISABLED
            return@LaunchedEffect
        }

        syncStatus = try {
            when {
                !HealthConnectManager.isAvailable(context) -> HealthConnectSyncManager.SyncStatus.UNAVAILABLE
                !HealthConnectManager.hasPermissions(context) -> HealthConnectSyncManager.SyncStatus.NO_PERMISSIONS
                else -> HealthConnectSyncManager.SyncStatus.READY
            }
        } catch (_: Exception) {
            HealthConnectSyncManager.SyncStatus.ERROR
        }
    }

    // Animated sync indicator with smooth transitions
    AnimatedContent(
        targetState = Pair(syncStatus, isSyncing),
        transitionSpec = {
            fadeIn(animationSpec = tween(500)) togetherWith
            fadeOut(animationSpec = tween(500))
        },
        modifier = modifier,
        label = "sync_icon_transition"
    ) { (status, syncing) ->
        when (status) {
            HealthConnectSyncManager.SyncStatus.READY -> {
                if (syncing) {
                    // Outlined primary colored cloud for active syncing
                    Icon(
                        imageVector = Icons.Outlined.Cloud,
                        contentDescription = "Health Connect syncing",
                        modifier = Modifier.fillMaxSize(),
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    // Filled rounded onSurface colored cloud for synced state
                    Icon(
                        imageVector = Icons.Rounded.Cloud,
                        contentDescription = "Health Connect synced",
                        modifier = Modifier.fillMaxSize(),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            HealthConnectSyncManager.SyncStatus.DISABLED -> {
                // No icon when disabled
            }
            HealthConnectSyncManager.SyncStatus.UNAVAILABLE,
            HealthConnectSyncManager.SyncStatus.NO_PERMISSIONS -> {
                // Warning icon for issues
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Health Connect not available",
                    modifier = Modifier.fillMaxSize(),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HealthConnectSyncManager.SyncStatus.ERROR -> {
                // Error icon
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Health Connect error",
                    modifier = Modifier.fillMaxSize(),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BeverageSelectionSection(
    selectedBeverageType: BeverageType,
    onBeverageTypeChange: (BeverageType) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = modifier.padding(horizontal = 5.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Horizontally scrollable chips
        val beverageTypes = BeverageType.getAllSorted()

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(beverageTypes) { beverageType ->
                val isSelected = selectedBeverageType == beverageType

                FilterChip(
                    shape = if (isSelected){
                        MaterialTheme.shapes.medium
                    } else {
                        MaterialTheme.shapes.extraLarge
                    },
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        onBeverageTypeChange(beverageType)
                    },
                    label = {
                        if (isSelected){
                            Text(
                                text = beverageType.displayName,
                                style = MaterialTheme.typography.labelMediumEmphasized,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = beverageType.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    leadingIcon = {
                        if (isSelected){
                            Icon(
                                painter = painterResource(beverageType.iconResFilled),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                painter = painterResource(beverageType.iconRes),
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
            visible = selectedBeverageType != BeverageType.WATER,
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
                        Text(
                            text = "Effective Hydration: ",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        AnimatedContent(
                            targetState = (selectedBeverageType.hydrationMultiplier * 100).toInt(),
                            transitionSpec = {
                                slideInVertically { height -> height } + fadeIn() togetherWith
                                slideOutVertically { height -> -height } + fadeOut()
                            },
                            label = "hydration_percentage"
                        ) { percentage ->
                            Text(
                                text = "$percentage%",
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                    }
                }
            }
        }
    }
}
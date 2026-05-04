package com.cemcakmak.hydrotracker.presentation.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.carousel.CarouselDefaults
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry
import com.cemcakmak.hydrotracker.data.database.repository.ContainerPresetRepository
import com.cemcakmak.hydrotracker.data.database.repository.TodayStatistics
import com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository
import com.cemcakmak.hydrotracker.data.database.repository.WaterProgress
import com.cemcakmak.hydrotracker.data.models.BeverageType
import com.cemcakmak.hydrotracker.data.models.ContainerPreset
import com.cemcakmak.hydrotracker.data.models.UserProfile
import com.cemcakmak.hydrotracker.health.HealthConnectManager
import com.cemcakmak.hydrotracker.health.HealthConnectSyncManager
import com.cemcakmak.hydrotracker.presentation.common.HydroSnackbarHost
import com.cemcakmak.hydrotracker.presentation.common.SnackbarQueue
import com.cemcakmak.hydrotracker.presentation.common.showErrorSnackbar
import com.cemcakmak.hydrotracker.presentation.common.showSuccessSnackbar
import com.cemcakmak.hydrotracker.utils.WaterCalculator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    userProfile: UserProfile,
    waterIntakeRepository: WaterIntakeRepository,
    containerPresetRepository: ContainerPresetRepository,
    activeBeverageTypes: List<BeverageType> = BeverageType.getAllSorted(),
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

   // val todayCompletionPercentage = todayStatistics.entryCount / todayStatistics.

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
                SnackbarQueue.clearAll()
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
                    val since = Instant.now().minus(30, ChronoUnit.DAYS)

                    waterIntakeRepository.getSyncManager().importExternalHydrationData(context, waterIntakeRepository.getUserRepository(), waterIntakeRepository, since) { imported, errors ->
                        coroutineScope.launch {
                            // Always show loading for at least 1.5 seconds for better UX
                            delay(1500)

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
                    delay(1500)
                    snackbarHostState.showErrorSnackbar(
                        message = "Sync failed: ${e.message}"
                    )
                    isRefreshing = false
                }
            } else {
                // Show loading animation even when disabled for consistency
                delay(1500)
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
                    navigationIcon = {
                        Image(
                            modifier = Modifier.size(42.dp),
                            painter = painterResource(

                                id = com.cemcakmak.hydrotracker.R.drawable.ic_launcher_foreground,
                            ),
                            colorFilter = ColorFilter.tint(
                                color = MaterialTheme.colorScheme.primary
                            ),
                            contentDescription = ""
                        )
                    },
                    scrollBehavior = scrollBehavior,
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "HydroTracker",
                                fontWeight = FontWeight.SemiBold
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                   Column(
                       horizontalAlignment = Alignment.Start,
                      modifier =  Modifier.weight(1f)
                   ) {
                       Text(
                           text = "Today",
                           style = MaterialTheme.typography.labelMedium.copy(
                               fontWeight = FontWeight.Light
                           )
                       )

                       Text(
                           text = "${todayProgress.currentIntake .toInt()} ml",
                           style = MaterialTheme.typography.headlineMedium.copy(
                               color = MaterialTheme.colorScheme.primary
                           )
                       )
                       Text(
                           text = "Goal ${todayProgress.dailyGoal.toInt()} ml",
                           style = MaterialTheme.typography.titleLarge.copy(
                               color = MaterialTheme.colorScheme.onSurface
                           )
                       )

                       if (todayStatistics.entryCount > 0){
                           Spacer(modifier = Modifier.height(8.dp))
                           Text(
                               text = buildAnnotatedString {
                                   withStyle(
                                       style = SpanStyle(
                                           fontWeight = FontWeight.Medium,
                                           fontSize = MaterialTheme.typography.labelLarge.fontSize
                                       )
                                   ) { append("${todayStatistics.entryCount}") }

                                   withStyle(
                                       style = SpanStyle(
                                           fontWeight = FontWeight.Thin,
                                           color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                           fontSize = MaterialTheme.typography.labelSmall.fontSize
                                       )
                                   ) {
                                       append(" Entries")
                                   }
                               }
                           )
                           Text(
                               text = buildAnnotatedString {
                                   withStyle(
                                       style = SpanStyle(
                                           fontWeight = FontWeight.Medium,
                                           fontSize = MaterialTheme.typography.labelLarge.fontSize
                                       )
                                   ) { append(todayStatistics.firstIntakeTime!!) }

                                   withStyle(
                                       style = SpanStyle(
                                           fontWeight = FontWeight.Thin,
                                           color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                           fontSize = MaterialTheme.typography.labelSmall.fontSize
                                       )
                                   ) {
                                       append(" First Intake")
                                   }
                               },
                           )

                           Text(
                               text = buildAnnotatedString {
                                   withStyle(
                                       style = SpanStyle(
                                           fontWeight = FontWeight.Medium,
                                           fontSize = MaterialTheme.typography.labelLarge.fontSize
                                       )
                                   ) { append(todayStatistics.lastIntakeTime!!) }

                                   withStyle(
                                       style = SpanStyle(
                                           fontWeight = FontWeight.Thin,
                                           color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                           fontSize = MaterialTheme.typography.labelSmall.fontSize
                                       )
                                   ) {
                                       append(" Last Intake")
                                   }
                               },
                           )


                       }
                   }
                    Box(
                      contentAlignment = Alignment.Center
                   ){
                       Text(
                           textAlign = TextAlign.Center,
                           text = buildAnnotatedString {
                               withStyle(style = SpanStyle(
                                   fontWeight = FontWeight.Bold,
                                   fontSize =  MaterialTheme.typography.headlineMedium  .fontSize,
                                   fontStyle = MaterialTheme.typography.titleLarge.fontStyle
                               )){
                                   append("${(todayStatistics.goalProgress * 100).toInt()}%")
                               }
                               withStyle(style = SpanStyle(
                                   fontWeight = FontWeight.Normal,
                                   fontSize =  MaterialTheme.typography.labelSmall  .fontSize,
                                   fontStyle = MaterialTheme.typography.labelSmall.fontStyle,
                                   color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                               )){
                                   append("\nCompleted")
                               }


                           }
                       )
                        // representaion of the progress of water
                       CircularWavyProgressIndicator(
                           amplitude = { 16f },
                           wavelength = 50.dp,
                           stroke = Stroke(
                               cap = StrokeCap.Round,
                               join = StrokeJoin.Round,
                               width = 38f
                           ),
                           trackStroke = Stroke(
                               width = 38f,
                               cap = StrokeCap.Round,
                               join = StrokeJoin.Round
                           ),
                           modifier = Modifier
                               .size(200.dp)
                               .align(Alignment.CenterEnd),
                           progress = { animatedProgress }
                       )
                   }
                }

            }


            Card (
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
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
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Quick Select",
                            style = MaterialTheme.typography.titleLargeEmphasized,
                            color = MaterialTheme.colorScheme.onSurface,
                        )


                        // this is the ui here u can select the amount of the hydration to add
                        val scrollState = rememberLazyListState()
                        LazyRow (
                            state = scrollState,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                           itemsIndexed(presets) { index, currentPreset ->
                                if (index + 1 < presets.size) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillParentMaxWidth(0.3f)
                                            .clip(
                                                RoundedCornerShape(16.dp)
                                            )
                                            .background(
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .combinedClickable(
                                                onLongClick = {
                                                    presetToEdit = currentPreset
                                                    showEditPresetSheet = true
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                                onClick = {
                                                    addWaterIntake(
                                                        currentPreset.volume,
                                                        currentPreset.name
                                                    )
                                                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                                }
                                            )
                                            .padding(16.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            when {
                                                currentPreset.iconRes != null -> {
                                                    Icon(
                                                        painter = painterResource(currentPreset.iconRes),
                                                        contentDescription = currentPreset.name,
                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }

                                                currentPreset.icon != null -> {
                                                    Icon(
                                                        imageVector = currentPreset.icon,
                                                        contentDescription = currentPreset.name,
                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }

                                                else -> {
                                                    Icon(
                                                        imageVector = Icons.Default.WaterDrop,
                                                        contentDescription = currentPreset.name,
                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${currentPreset.volume.toInt()} ml"
                                            )
                                        }
                                    }
                                }else{
                                    Box(
                                        contentAlignment = Alignment.Center,
                                       modifier =  Modifier
                                           .fillParentMaxWidth(0.4f)
                                           .clip(
                                               RoundedCornerShape(16.dp)
                                           )
                                           .background(
                                               color = MaterialTheme.colorScheme.surface,
                                               shape = RoundedCornerShape(16.dp)
                                           )
                                           .border(
                                               shape = RoundedCornerShape(16.dp),
                                               width = 2.dp,
                                               color = MaterialTheme.colorScheme.primary
                                           ).clickable(
                                               onClick = {
                                                   showAddPresetSheet = true
                                                   haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                               })
                                           .padding(16.dp)
                                    ){
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add container",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(32.dp)
                                            )


                                            Text(
                                                text = "Add",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }







                        // this the ui where u can select what type of hydration (it's s toggle button material expressive one) like water coffe energy drink ok
                        BeverageSelectionSection(
                            selectedBeverageType = selectedBeverageType,
                            onBeverageTypeChange = { beverageType ->
                                selectedBeverageType = beverageType
                                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            },
                            beverageTypes = activeBeverageTypes,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                 //Recent Entries Section
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Text(
                                modifier = Modifier.padding(start = 16.dp, top = 16.dp),
                                text = "Recent Entries",
                                style = MaterialTheme.typography.titleLargeEmphasized,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {


                                todayEntries.forEachIndexed { index, entry ->
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
                                        if (index +1 != todayEntries.size){
                                            HorizontalDivider()
                                        }
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
            },
            beverageTypes = activeBeverageTypes
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
            },
            beverageTypes = activeBeverageTypes
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
            .background(MaterialTheme.colorScheme.surface)
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

           Row(
               modifier = Modifier.fillMaxHeight()
           ) {
               VerticalDivider(color = Color.Red, modifier = Modifier.fillMaxHeight())
               Text(
                  // modifier = Modifier.weight(1f),
                   text = preset.volume.toString(),
                   style = MaterialTheme.typography.titleLarge,
                   color = MaterialTheme.colorScheme.primary
               )
               VerticalDivider()
           }
            Text("ml",color = MaterialTheme.colorScheme.onSurface)
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
    onBeverageTypeChange: (BeverageType) -> Unit,
    beverageTypes: List<BeverageType> = BeverageType.getAllSorted()
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
    onConfirm: (WaterIntakeEntry) -> Unit,
    beverageTypes: List<BeverageType> = BeverageType.getAllSorted()
) {
    var amountText by remember { mutableStateOf(entry.amount.toString()) }
    var containerType by remember { mutableStateOf(entry.containerType) }
    var selectedBeverageType by remember {
        mutableStateOf(
            entry.getBeverageType().let { t -> if (t in beverageTypes) t else BeverageType.WATER }
        )
    }
    var isError by remember { mutableStateOf(false) }

    // Time picker state
    var showTimePicker by remember { mutableStateOf(false) }
    val calendar = remember {
        Calendar.getInstance().apply {
            timeInMillis = entry.timestamp
        }
    }
    var selectedHour by remember { mutableIntStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(calendar.get(Calendar.MINUTE)) }

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
                        value = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute),
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
                                    val newCalendar = Calendar.getInstance().apply {
                                        timeInMillis = entry.timestamp
                                        set(Calendar.HOUR_OF_DAY, selectedHour)
                                        set(Calendar.MINUTE, selectedMinute)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
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
    value: String,
    alignment: Alignment.Horizontal = Alignment.Start
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        shape = MaterialTheme.shapes.small,

    ) {
        Column(
            modifier = Modifier.padding( vertical = 4.dp),
            horizontalAlignment = alignment
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraLight),
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
                delay(100)
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
            SwipeToDismissBoxValue.EndToStart -> {
                // Left swipe - Show delete confirmation
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                showDeleteDialog = true
                // Reset to center after showing dialog
                delay(100)
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
                            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                            SwipeToDismissBoxValue.Settled -> MaterialTheme.colorScheme.surface
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 12.dp),
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
                                color = MaterialTheme.colorScheme.onError,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete entry",
                                tint = MaterialTheme.colorScheme.onError,
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
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 0.dp),
                colors = ListItemDefaults.colors(
                    MaterialTheme.colorScheme.surfaceContainer
                ),
                leadingContent = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        tonalElevation = 2.dp,
                        modifier = Modifier.size(48.dp)
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
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                preset?.icon != null -> {
                                    Icon(
                                        imageVector = preset.icon,
                                        contentDescription = preset.name,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                else -> {
                                    Icon(
                                        imageVector = Icons.Default.WaterDrop,
                                        contentDescription = entry.containerType,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
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
                        fontWeight = FontWeight.SemiBold,
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


/**
 * Selction of what kinda drink like water ui
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BeverageSelectionSection(
    selectedBeverageType: BeverageType,
    onBeverageTypeChange: (BeverageType) -> Unit,
    beverageTypes: List<BeverageType> = BeverageType.getAllSorted(),
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    val safeSelected = if (selectedBeverageType in beverageTypes) selectedBeverageType else beverageTypes.first()
    LaunchedEffect(beverageTypes) {
        if (selectedBeverageType !in beverageTypes) onBeverageTypeChange(BeverageType.WATER)
    }

    val rowScrollState = rememberLazyListState()
    val scope = rememberCoroutineScope ()

    Column(
        modifier = modifier.padding( vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Horizontally scrollable chips
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            LazyRow(
                state = rowScrollState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                itemsIndexed(beverageTypes) {index,  beverageType ->
                    val isSelected = safeSelected == beverageType

                    ToggleButton(
                        checked = isSelected,
                        onCheckedChange = {
                            scope.launch {
                                rowScrollState.animateScrollToItem(index,)
                            }
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            onBeverageTypeChange(beverageType)
                        }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                            //modifier = Modifier.padding(8.dp),
                        ) {
                            if (isSelected){
                                Icon(
                                    painter = painterResource(beverageType.iconResFilled),
                                    contentDescription = null,
                                )}else{
                                Icon(
                                    painter = painterResource(beverageType.iconRes),
                                    contentDescription = null,)
                            }

                            Text(
                                text = beverageType.displayName,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

//                FilterChip(
//                    shape = if (isSelected){
//                        MaterialTheme.shapes.medium
//                    } else {
//                        MaterialTheme.shapes.extraLarge
//                    },
//                    onClick = {
//                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
//                        onBeverageTypeChange(beverageType)
//                    },
//                    label = {
//                        if (isSelected){
//                            Text(
//                                text = beverageType.displayName,
//                                style = MaterialTheme.typography.titleMedium,
//                                textAlign = TextAlign.Center
//                            )
//                        } else {
//                            Text(
//                                text = beverageType.displayName,
//                                style = MaterialTheme.typography.titleMedium,
//                                textAlign = TextAlign.Center
//                            )
//                        }
//                    },
//                    leadingIcon = {
//                        if (isSelected){
//                            Icon(
//                                painter = painterResource(beverageType.iconResFilled),
//                                contentDescription = null,
//                                modifier = Modifier.size(34.dp)
//                            )
//                        } else {
//                            Icon(
//                                painter = painterResource(beverageType.iconRes),
//                                contentDescription = null,
//                                modifier = Modifier.size(34.dp)
//                            )
//                        }
//                    },
//                    selected = isSelected,
//                    modifier = Modifier.animateItem()
//                )
                }
            }
        }

        // Show selected beverage info with animation
        AnimatedVisibility(
            visible = safeSelected != BeverageType.WATER,
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
                            targetState = (safeSelected.hydrationMultiplier * 100).toInt(),
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
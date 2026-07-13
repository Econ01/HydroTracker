package com.cemcakmak.hydrotracker

import android.app.Application
import android.app.UiModeManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.text.ExperimentalTextApi
import com.cemcakmak.hydrotracker.utils.AppLocale
import com.cemcakmak.hydrotracker.utils.SmartComposeHapticFeedback
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import com.cemcakmak.hydrotracker.data.repository.*
import com.cemcakmak.hydrotracker.data.update.UpdateRepository
import com.cemcakmak.hydrotracker.data.update.UpdateStatus
import com.cemcakmak.hydrotracker.data.database.DatabaseInitializer
import com.cemcakmak.hydrotracker.data.database.DatabaseMigrationHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.lifecycle.lifecycleScope
import com.cemcakmak.hydrotracker.data.models.BeveragePreferences
import com.cemcakmak.hydrotracker.data.models.ContainerPreset
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import androidx.navigationevent.NavigationEvent
import com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository
import com.cemcakmak.hydrotracker.data.database.repository.ContainerPresetRepository
import com.cemcakmak.hydrotracker.data.database.repository.CustomBeverageRepository
import com.cemcakmak.hydrotracker.presentation.common.*
import com.cemcakmak.hydrotracker.presentation.home.HomeScreen
import com.cemcakmak.hydrotracker.presentation.history.HistoryScreen
import com.cemcakmak.hydrotracker.presentation.history.HistoryViewModel
import com.cemcakmak.hydrotracker.presentation.history.HistoryViewModelFactory
import com.cemcakmak.hydrotracker.presentation.statistics.StatisticsScreen
import com.cemcakmak.hydrotracker.presentation.statistics.StatisticsViewModel
import com.cemcakmak.hydrotracker.presentation.statistics.StatisticsViewModelFactory
import com.cemcakmak.hydrotracker.presentation.settings.SettingsHubScreen
import com.cemcakmak.hydrotracker.presentation.settings.AboutScreen
import com.cemcakmak.hydrotracker.presentation.settings.UpdatesScreen
import com.cemcakmak.hydrotracker.presentation.settings.WhatsNewBottomSheet
import com.cemcakmak.hydrotracker.presentation.settings.UpdateAvailableDialog
import com.cemcakmak.hydrotracker.presentation.settings.AppearanceScreen
import com.cemcakmak.hydrotracker.presentation.settings.DisplayLocaleScreen
import com.cemcakmak.hydrotracker.presentation.settings.DataManagementScreen
import com.cemcakmak.hydrotracker.presentation.settings.HydrationHealthScreen
import com.cemcakmak.hydrotracker.presentation.settings.QuickAddCustomizationScreen
import com.cemcakmak.hydrotracker.presentation.settings.ContainerPresetsScreen
import com.cemcakmak.hydrotracker.presentation.settings.BeverageTypesEditScreen
import com.cemcakmak.hydrotracker.presentation.settings.NotificationsScreen
import com.cemcakmak.hydrotracker.presentation.settings.ReminderIntervalScreen
import com.cemcakmak.hydrotracker.presentation.settings.DeveloperOptionsScreen
import com.cemcakmak.hydrotracker.presentation.settings.HapticsLabScreen
import com.cemcakmak.hydrotracker.presentation.settings.HapticsTestScreen
import com.cemcakmak.hydrotracker.presentation.settings.LicensesScreen
import com.cemcakmak.hydrotracker.presentation.settings.SupportDevelopmentScreen
import com.cemcakmak.hydrotracker.presentation.settings.profile.ProfileSettingsScreen
import com.cemcakmak.hydrotracker.presentation.settings.profile.crop.CropProfileImageScreen
import com.cemcakmak.hydrotracker.presentation.common.dialogs.CustomWaterDialog
import com.cemcakmak.hydrotracker.presentation.common.sheets.AddCustomBeverageBottomSheet
import com.cemcakmak.hydrotracker.presentation.common.sheets.AddContainerPresetBottomSheet
import com.cemcakmak.hydrotracker.presentation.onboarding.*
import com.cemcakmak.hydrotracker.notifications.*
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.health.HealthConnectManager
import androidx.core.net.toUri

// Navigation animation tuning
internal const val TAB_SWITCH_DURATION = 400

private val TOP_LEVEL_TAB_KEYS: Set<NavigationRoutes> = setOf(
    NavigationRoutes.Home,
    NavigationRoutes.History,
    NavigationRoutes.Statistics,
    NavigationRoutes.Settings,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
class MainActivity : ComponentActivity() {
    private lateinit var userRepository: UserRepository
    private lateinit var waterIntakeRepository: WaterIntakeRepository
    private lateinit var containerPresetRepository: ContainerPresetRepository
    private lateinit var customBeverageRepository: CustomBeverageRepository
    private lateinit var updateRepository: UpdateRepository

    // Modern permission launcher
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            lifecycleScope.launch {
                val userProfile = userRepository.userProfile.first()
                userProfile?.takeIf { it.isOnboardingCompleted }?.let {
                    HydroNotificationScheduler.startNotifications(this@MainActivity, it)
                }
            }
        }
        // Handle denied case if needed - currently no-op as per original logic
    }

    // Health Connect permission launcher - using proper Activity context
    private lateinit var healthConnectPermissionLauncher: ActivityResultLauncher<Set<String>>

    // Listener so a system contrast change (Android 14+) re-applies the dynamic colours live
    private var contrastChangeListener: UiModeManager.ContrastChangeListener? = null

    override fun attachBaseContext(newBase: Context) {
        // Apply the user-selected per-app language before any resources are resolved.
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Set navigation bar contrast enforcement - only available on Android 10+ (API 29)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        // Follow the system contrast setting (Android 14+). The platform bakes contrast into the
        // dynamic colour palette, so recreate on change to reload it immediately while the app is open.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val uiModeManager = getSystemService(UiModeManager::class.java)
            val listener = UiModeManager.ContrastChangeListener { recreate() }
            uiModeManager.addContrastChangeListener(mainExecutor, listener)
            contrastChangeListener = listener
        }

        // Init
        userRepository = UserRepository(applicationContext)
        updateRepository = UpdateRepository(applicationContext)

        // Perform comprehensive database health check (critical for Room 2.8.1 compatibility)
        lifecycleScope.launch {
            val healthResult = DatabaseMigrationHelper.performStartupDatabaseCheck(applicationContext)
            val healthMessage = DatabaseMigrationHelper.getHealthMessage(healthResult)

            android.util.Log.i("MainActivity", "Database health: $healthMessage")

            // Clear notification cache if database was recovered or reset
            when (healthResult) {
                is com.cemcakmak.hydrotracker.data.database.DatabaseHealthResult.RecoveredWithDataLoss -> {
                    android.util.Log.i("MainActivity", "Database was recovered - clearing notification cache")
                    HydroNotificationScheduler.clearCacheOnSchemaChange(applicationContext)
                }
                is com.cemcakmak.hydrotracker.data.database.DatabaseHealthResult.CriticalFailure -> {
                    android.util.Log.w("MainActivity", "Database critical failure - clearing notification cache")
                    HydroNotificationScheduler.clearCacheOnSchemaChange(applicationContext)
                }
                else -> {
                    // For healthy or fresh install, validate notification state
                    val userProfile = userRepository.userProfile.first()
                    if (userProfile != null) {
                        val notificationStateValid = HydroNotificationScheduler.validateAndRepairNotificationState(
                            applicationContext, userProfile
                        )
                        android.util.Log.d("MainActivity", "Notification state validation: $notificationStateValid")
                    }
                }
            }

            // Notify user if there were issues
            if (DatabaseMigrationHelper.shouldNotifyUser(healthResult)) {
                android.util.Log.w("MainActivity", "Database migration issue: $healthMessage")
                // You can add a toast or dialogue here if needed for critical failures
            }
        }

        waterIntakeRepository = DatabaseInitializer.getWaterIntakeRepository(
            applicationContext, userRepository
        )

        containerPresetRepository = DatabaseInitializer.getContainerPresetRepository(
            applicationContext
        )

        customBeverageRepository = DatabaseInitializer.getCustomBeverageRepository(
            applicationContext
        )

        // Seed default container presets if needed
        lifecycleScope.launch {
            containerPresetRepository.seedDefaultsIfNeeded()
        }

        // Create Health Connect permission launcher using the proper method from the manager
        healthConnectPermissionLauncher = HealthConnectManager.createPermissionRequestLauncher(this) { grantedPermissions ->
            // This callback will be called when user responds to permission request
            android.util.Log.d("MainActivity", "Health Connect permission result: $grantedPermissions")
        }

        setContent {
            CompositionLocalProvider(
                LocalHapticFeedback provides SmartComposeHapticFeedback(this)
            ) {
                HydroTrackerApp(
                    userRepository,
                    waterIntakeRepository,
                    containerPresetRepository,
                    customBeverageRepository,
                    updateRepository,
                    notificationPermissionLauncher,
                    healthConnectPermissionLauncher
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            contrastChangeListener?.let {
                getSystemService(UiModeManager::class.java).removeContrastChangeListener(it)
            }
        }
    }

}

@Composable
fun HydroTrackerApp(
    userRepository: UserRepository,
    waterIntakeRepository: WaterIntakeRepository,
    containerPresetRepository: ContainerPresetRepository,
    customBeverageRepository: CustomBeverageRepository,
    updateRepository: UpdateRepository,
    notificationPermissionLauncher: ActivityResultLauncher<String>,
    healthConnectPermissionLauncher: ActivityResultLauncher<Set<String>>
) {
    val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModelFactory(userRepository))
    val themePreferences by themeViewModel.themePreferences.collectAsState()

    val historyViewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModelFactory(waterIntakeRepository, userRepository)
    )
    val historyUiState by historyViewModel.uiState.collectAsState()

    val statisticsViewModel: StatisticsViewModel = viewModel(
        factory = StatisticsViewModelFactory(waterIntakeRepository, userRepository, containerPresetRepository)
    )
    val statisticsUiState by statisticsViewModel.uiState.collectAsState()

    val appPreferences by userRepository.appPreferences.collectAsState(initial = null)
    val userProfile = appPreferences?.profile
    val isOnboardingCompleted = appPreferences?.onboardingCompleted ?: false
    val beveragePreferences = appPreferences?.beverages ?: BeveragePreferences.default()
    val customBeverages by customBeverageRepository.getAll().collectAsState(initial = emptyList())
    val activeBeverages = remember(beveragePreferences, customBeverages) {
        buildActiveBeverages(beveragePreferences, customBeverages)
    }
    // Loading until the first persisted snapshot arrives, so existing users are never briefly routed
    // to onboarding while DataStore performs its first read (and its one-time SharedPreferences import).
    val isLoading = appPreferences == null
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var wasPop by remember { mutableStateOf(false) }
    var quickAddWasPop by remember { mutableStateOf(false) }
    var notificationsWasPop by remember { mutableStateOf(false) }
    var developerOptionsWasPop by remember { mutableStateOf(false) }
    var aboutWasPop by remember { mutableStateOf(false) }

    LaunchedEffect(isOnboardingCompleted, userProfile) {
        // Check for new user day when app starts
        if (isOnboardingCompleted && userProfile != null) {
            waterIntakeRepository.checkAndHandleNewUserDay()

            // Perform app launch sync to import any missed external Health Connect data
            waterIntakeRepository.getSyncManager().performAppLaunchSync(context, userRepository, waterIntakeRepository)
        }
    }

    val systemHaptics = LocalHapticFeedback.current
    val isHapticsEnabled = appPreferences?.hapticsEnabled ?: true

    com.cemcakmak.hydrotracker.utils.SmartHaptics.isGloballyEnabled = isHapticsEnabled
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            view.isHapticFeedbackEnabled = isHapticsEnabled
        }
    }
    val proxyHaptics = remember(systemHaptics, isHapticsEnabled) {
        object : androidx.compose.ui.hapticfeedback.HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: androidx.compose.ui.hapticfeedback.HapticFeedbackType) {
                if (isHapticsEnabled) {
                    systemHaptics.performHapticFeedback(hapticFeedbackType)
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalHapticFeedback provides proxyHaptics
    ) {
        HydroTrackerTheme(themePreferences = themePreferences) {
        if (isLoading) {
            LoadingScreen()
        } else {
            val startKey: NavigationRoutes = if (isOnboardingCompleted && userProfile != null)
                NavigationRoutes.Home else NavigationRoutes.Onboarding
            val backStack = rememberNavBackStack(startKey)
            val currentKey = backStack.lastOrNull() as? NavigationRoutes ?: startKey

            // Shared onboarding ViewModel so the cropper can update it.
            val onboardingViewModel: OnboardingViewModel = viewModel(
                factory = OnboardingViewModelFactory(
                    LocalContext.current.applicationContext as Application,
                    userRepository
                )
            )
            var homeShowCustomDialog by remember { mutableStateOf(false) }
            var showAddBeverageSheet by remember { mutableStateOf(false) }
            var showAddContainerSheet by remember { mutableStateOf(false) }
            var showPastEntryDialog by remember { mutableStateOf(false) }
            var pastEntryDate by remember { mutableStateOf<String?>(null) }
            var pastEntrySelectedBeverage by remember(activeBeverages) {
                mutableStateOf(activeBeverages.firstOrNull { it.isWater } ?: activeBeverages.first())
            }

            // Auto-check for updates on launch (throttled to once per 24h inside the repository).
            LaunchedEffect(Unit) {
                updateRepository.maybeAutoCheck()
            }

            // Home-screen update dialogue — only shown when on Home and an update is available.
            var showUpdateDialog by remember { mutableStateOf(false) }
            val updateStatus by updateRepository.updateStatus.collectAsState()
            LaunchedEffect(currentKey, updateStatus) {
                showUpdateDialog = currentKey == NavigationRoutes.Home && updateStatus is UpdateStatus.Available
            }
            if (showUpdateDialog && updateStatus is UpdateStatus.Available) {
                val availableStatus = updateStatus as UpdateStatus.Available
                UpdateAvailableDialog(
                    status = availableStatus,
                    installSource = updateRepository.installSource,
                    updateRepository = updateRepository,
                    themePreferences = themePreferences,
                    onDismiss = { showUpdateDialog = false }
                )
            }

            // Show "What's New?" once after the user updates and onboarding is complete.
            val whatsNewAvailable by updateRepository.whatsNewAvailable.collectAsState()
            var showWhatsNew by remember { mutableStateOf(false) }
            LaunchedEffect(isOnboardingCompleted, whatsNewAvailable) {
                if (isOnboardingCompleted && whatsNewAvailable) {
                    showWhatsNew = true
                }
            }
            if (showWhatsNew) {
                val whatsNewContent = remember { updateRepository.loadWhatsNewContent() }
                if (whatsNewContent.isNotBlank()) {
                    WhatsNewBottomSheet(
                        versionName = BuildConfig.VERSION_NAME,
                        content = whatsNewContent,
                        onDismiss = {
                            showWhatsNew = false
                            updateRepository.markWhatsNewSeen()
                        }
                    )
                } else {
                    updateRepository.markWhatsNewSeen()
                    showWhatsNew = false
                }
            }

            MainNavigationScaffold(
                backStack = backStack,
                currentKey = currentKey,
                isHistoryDaySelected = historyUiState.selectedDate != null,
                onAddCustomClick = {
                    when (currentKey) {
                        NavigationRoutes.Home -> homeShowCustomDialog = true
                        NavigationRoutes.History -> {
                            pastEntryDate = historyUiState.selectedDate
                            showPastEntryDialog = true
                        }
                        else -> {}
                    }
                },
                onAddBeverageClick = { showAddBeverageSheet = true },
                onAddContainerClick = { showAddContainerSheet = true },
                onTabSwitch = { wasPop = false },
                autoHideNavBar = themePreferences.autoHideNavBar,
                navBarLabelMode = themePreferences.navBarLabelMode
            ) { paddingValues ->
                val popBackStack = {
                    wasPop = true
                    if (backStack.size > 1) backStack.removeLastOrNull()
                }

                SharedTransitionLayout {
                    NavDisplay(
                        backStack = backStack,
                    onBack = popBackStack,
                    transitionSpec = {
                        // We have to get explicitly string. Nav3 converts NavigationRoutes data objects into
                        // plain text strings when managing the backstack. Therefore, we have to compare str to str
                        // otherwise isTabSwitch logic will return null
                        val rawInit = initialState.entries.firstOrNull()?.contentKey?.toString()
                        val rawTarget = targetState.entries.firstOrNull()?.contentKey?.toString()

                        // Reverse map the strings back to actual NavigationRoutes objects
                        val initialRoute = TOP_LEVEL_TAB_KEYS.find { it.toString() == rawInit }
                        val targetRoute = TOP_LEVEL_TAB_KEYS.find { it.toString() == rawTarget }

                        // If both routes were successfully found in TOP_LEVEL_TAB_KEYS, it is a valid tab switch
                        val isTabSwitch = initialState.entries.size == 1 && targetState.entries.size == 1 &&
                                initialRoute != null && targetRoute != null

                        if (isTabSwitch) {
                            // Determine tab orders for the target and destination tabs
                            val fromIndex = NavigationItem.entries.indexOfFirst { it.key == initialRoute }
                            val toIndex = NavigationItem.entries.indexOfFirst { it.key == targetRoute }
                            val goingForward = toIndex > fromIndex

                            ContentTransform(
                                targetContentEnter = slideInHorizontally(tween(TAB_SWITCH_DURATION)) {
                                    if (goingForward) it else -it
                                } + fadeIn(),
                                initialContentExit = slideOutHorizontally(tween(TAB_SWITCH_DURATION)) {
                                    if (goingForward) -it else it
                                } + fadeOut(),
                            )
                        } else {
                            ContentTransform(
                                targetContentEnter = slideInHorizontally(tween(TAB_SWITCH_DURATION)) { it } + fadeIn(),
                                initialContentExit = slideOutHorizontally(tween(TAB_SWITCH_DURATION)) { -it } + fadeOut(),
                            )
                        }
                    },
                    popTransitionSpec = {
                        ContentTransform(
                            targetContentEnter = fadeIn(tween(TAB_SWITCH_DURATION)) +
                                scaleIn(tween(TAB_SWITCH_DURATION), initialScale = 0.95f),
                            initialContentExit = fadeOut(tween(TAB_SWITCH_DURATION)) +
                                scaleOut(tween(TAB_SWITCH_DURATION), targetScale = 0.90f),
                        )
                    },
                    predictivePopTransitionSpec = { swipeEdge ->
                        val slideDirection = if (swipeEdge == NavigationEvent.EDGE_LEFT) 1 else -1

                        ContentTransform(
                            targetContentEnter = scaleIn(tween(400), initialScale = 0.8f),
                            initialContentExit = scaleOut(
                                targetScale = 0.8f,
                                transformOrigin = TransformOrigin(
                                    pivotFractionX = if (slideDirection == 1) 0f else 1f,
                                    pivotFractionY = 0.5f
                                )
                            ) + slideOutHorizontally(animationSpec = tween(400)) { it * slideDirection }
                        )
                    },
                    entryProvider = entryProvider {
                        entry<NavigationRoutes.Onboarding> {
                            OnboardingScreen(
                                onNavigateToHome = {
                                    backStack.apply {
                                        clear()
                                        add(NavigationRoutes.Home)
                                    }
                                },
                                onNavigateToCrop = { uri ->
                                    wasPop = true
                                    backStack.add(
                                        NavigationRoutes.CropProfileImage(
                                            sourceUri = uri.toString(),
                                            caller = NavigationRoutes.CropCaller.ONBOARDING
                                        )
                                    )
                                },
                                themePreferences = themePreferences,
                                viewModel = onboardingViewModel
                            )
                        }

                        entry<NavigationRoutes.Home> {
                            userProfile?.let { profile ->
                                HomeScreen(
                                    userProfile = profile,
                                    themePreferences = themePreferences,
                                    waterIntakeRepository = waterIntakeRepository,
                                    containerPresetRepository = containerPresetRepository,
                                    activeBeverages = activeBeverages,
                                    paddingValues = paddingValues,
                                    showCustomDialog = homeShowCustomDialog,
                                    onCustomDialogChange = { homeShowCustomDialog = it }
                                )
                            } ?: LoadingScreen()
                        }

                        entry<NavigationRoutes.History> {
                            HistoryScreen(
                                uiState = historyUiState,
                                themePreferences = themePreferences,
                                userProfile = userProfile,
                                activeBeverages = activeBeverages,
                                paddingValues = paddingValues,
                                onPeriodSelected = historyViewModel::selectPeriod,
                                onPreviousPeriod = historyViewModel::previousPeriod,
                                onNextPeriod = historyViewModel::nextPeriod,
                                onDaySelected = historyViewModel::selectDate,
                                onUpdateEntry = { oldEntry, updatedEntry ->
                                    coroutineScope.launch {
                                        waterIntakeRepository.updateWaterIntake(oldEntry, updatedEntry)
                                    }
                                },
                                onDeleteEntry = { entry ->
                                    coroutineScope.launch {
                                        waterIntakeRepository.deleteWaterIntake(entry)
                                    }
                                }
                            )
                        }

                        entry<NavigationRoutes.Statistics> {
                            StatisticsScreen(
                                uiState = statisticsUiState,
                                themePreferences = themePreferences,
                                userProfile = userProfile,
                                activeBeverages = activeBeverages,
                                paddingValues = paddingValues
                            )
                        }

                        entry<NavigationRoutes.Settings> {
                            CompositionLocalProvider(
                                LocalSharedTransitionScope provides this@SharedTransitionLayout,
                                LocalNavAnimatedVisibilityScope provides LocalNavAnimatedContentScope.current
                            ) {
                                SettingsHubScreen(
                                    userProfile = userProfile,
                                    themePreferences = themePreferences,
                                    wasPop = wasPop,
                                    developerOptionsEnabled = BuildConfig.DEBUG,
                                    paddingValues = paddingValues,
                                    updateStatus = updateStatus,
                                    onNavigateTo = { key ->
                                        wasPop = true
                                        quickAddWasPop = false
                                        backStack.add(key)
                                    }
                                )
                            }
                        }

                        entry<NavigationRoutes.SettingsAppearance> {
                            AppearanceScreen(
                                themePreferences = themePreferences,
                                isHapticsEnabled = isHapticsEnabled,
                                onHapticsEnabledChange = { enabled ->
                                    coroutineScope.launch {
                                        userRepository.updateHapticsEnabled(enabled)
                                    }
                                },
                                isDynamicColorAvailable = themeViewModel.isDynamicColorAvailable(),
                                onColorSourceChange = themeViewModel::setColorSource,
                                onDarkModeChange = themeViewModel::updateDarkModePreference,
                                onPureBlackChange = themeViewModel::updatePureBlackPreference,
                                onAppFontChange = themeViewModel::setAppFont,
                                onAutoHideNavBarChange = themeViewModel::setAutoHideNavBar,
                                onNavBarLabelModeChange = themeViewModel::setNavBarLabelMode,
                                isBlurSupported = themeViewModel.isBlurSupported(),
                                onEdgeEffectChange = themeViewModel::setEdgeEffect,
                                onNavigateBack = popBackStack
                            )
                        }
                        entry<NavigationRoutes.SettingsDisplay> {
                            DisplayLocaleScreen(
                                themePreferences = themePreferences,
                                userProfile = userProfile,
                                onWeekStartDayChange = themeViewModel::updateWeekStartDay,
                                onTimeFormatChange = themeViewModel::updateTimeFormat,
                                onDateFormatChange = themeViewModel::updateDateFormat,
                                onVolumeUnitChange = { unit ->
                                    userProfile?.let { profile ->
                                        coroutineScope.launch {
                                            userRepository.saveUserProfile(profile.copy(volumeUnit = unit))
                                        }
                                    }
                                },
                                onNavigateBack = popBackStack
                            )
                        }
                        entry<NavigationRoutes.SettingsHydration> {
                            HydrationHealthScreen(
                                userProfile = userProfile,
                                userRepository = userRepository,
                                waterIntakeRepository = waterIntakeRepository,
                                healthConnectPermissionLauncher = healthConnectPermissionLauncher,
                                themePreferences = themePreferences,
                                onHydrationStandardChange = { newStandard ->
                                    userProfile?.let { profile ->
                                        val newGoal = com.cemcakmak.hydrotracker.utils.WaterCalculator.calculateDailyWaterGoal(
                                            gender = profile.gender,
                                            activityLevel = profile.activityLevel,
                                            weight = profile.weight,
                                            hydrationStandard = newStandard
                                        )
                                        val updatedProfile = profile.copy(
                                            hydrationStandard = newStandard,
                                            dailyWaterGoal = newGoal
                                        )
                                        coroutineScope.launch { userRepository.saveUserProfile(updatedProfile) }
                                    }
                                },
                                onHealthConnectSyncChange = { enabled ->
                                    userProfile?.let { profile ->
                                        coroutineScope.launch { userRepository.saveUserProfile(profile.copy(healthConnectSyncEnabled = enabled)) }
                                    }
                                },
                                onNavigateBack = popBackStack
                            )
                        }
                        entry<NavigationRoutes.SettingsContainers> {
                            QuickAddCustomizationScreen(
                                themePreferences = themePreferences,
                                wasPop = quickAddWasPop,
                                onNavigateToContainerPresets = {
                                    quickAddWasPop = true
                                    backStack.add(NavigationRoutes.SettingsContainerPresets)
                                },
                                onNavigateToBeverageTypes = {
                                    quickAddWasPop = true
                                    backStack.add(NavigationRoutes.SettingsBeverageTypes)
                                },
                                onNavigateBack = popBackStack
                            )
                        }
                        entry<NavigationRoutes.SettingsContainerPresets> {
                            ContainerPresetsScreen(
                                themePreferences = themePreferences,
                                containerPresetRepository = containerPresetRepository,
                                userProfile = userProfile,
                                onNavigateBack = popBackStack
                            )
                        }
                        entry<NavigationRoutes.SettingsBeverageTypes> {
                            BeverageTypesEditScreen(
                                themePreferences = themePreferences,
                                userRepository = userRepository,
                                customBeverageRepository = customBeverageRepository,
                                onNavigateBack = popBackStack
                            )
                        }
                        entry<NavigationRoutes.SettingsNotifications> {
                            NotificationsScreen(
                                wasPop = notificationsWasPop,
                                onNavigateToReminderInterval = {
                                    notificationsWasPop = true
                                    backStack.add(NavigationRoutes.SettingsReminderInterval)
                                },
                                themePreferences = themePreferences,
                                userProfile = userProfile,
                                onNavigateBack = popBackStack,
                                onRequestNotificationPermission = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                onUserProfileUpdate = { updatedProfile ->
                                    coroutineScope.launch {
                                        userRepository.saveUserProfile(updatedProfile)
                                        HydroNotificationScheduler.rescheduleNotifications(context, updatedProfile)
                                    }
                                }
                            )
                        }
                        entry<NavigationRoutes.SettingsReminderInterval> {
                            ReminderIntervalScreen(
                                userProfile = userProfile,
                                themePreferences = themePreferences,
                                onNavigateBack = popBackStack,
                                onUserProfileUpdate = { updatedProfile ->
                                    coroutineScope.launch {
                                        userRepository.saveUserProfile(updatedProfile)
                                        HydroNotificationScheduler.rescheduleNotifications(context, updatedProfile)
                                    }
                                }
                            )
                        }
                        entry<NavigationRoutes.SettingsSupport> {
                            SupportDevelopmentScreen(
                                themePreferences = themePreferences,
                                onNavigateBack = popBackStack
                            )
                        }
                        entry<NavigationRoutes.SettingsAbout> {
                            AboutScreen(
                                themePreferences = themePreferences,
                                wasPop = aboutWasPop,
                                updateStatus = updateStatus,
                                onNavigateToUpdates = {
                                    aboutWasPop = true
                                    backStack.add(NavigationRoutes.SettingsUpdates)
                                },
                                onNavigateBack = popBackStack,
                                onNavigateToLicenses = {
                                    aboutWasPop = true
                                    backStack.add(NavigationRoutes.SettingsLicenses)
                                }
                            )
                        }
                        entry<NavigationRoutes.SettingsUpdates> {
                            UpdatesScreen(
                                updateRepository = updateRepository,
                                themePreferences = themePreferences,
                                onNavigateBack = popBackStack
                            )
                        }
                        entry<NavigationRoutes.SettingsLicenses> {
                            LicensesScreen(
                                themePreferences = themePreferences,
                                onNavigateBack = popBackStack
                            )
                        }
                        entry<NavigationRoutes.SettingsDeveloper> {
                            DeveloperOptionsScreen(
                                themePreferences = themePreferences,
                                wasPop = developerOptionsWasPop,
                                userProfile = userProfile,
                                userRepository = userRepository,
                                waterIntakeRepository = waterIntakeRepository,
                                containerPresetRepository = containerPresetRepository,
                                customBeverageRepository = customBeverageRepository,
                                updateRepository = updateRepository,
                                onNavigateBack = popBackStack,
                                onNavigateToOnboarding = {
                                    backStack.apply {
                                        clear()
                                        add(NavigationRoutes.Onboarding)
                                    }
                                },
                                onNavigateToHapticsTest = {
                                    developerOptionsWasPop = true
                                    backStack.add(NavigationRoutes.SettingsDeveloperHaptics)
                                },
                                onNavigateToHapticsLab = {
                                    developerOptionsWasPop = true
                                    backStack.add(NavigationRoutes.SettingsDeveloperHapticsLab)
                                }
                            )
                        }
                        entry<NavigationRoutes.SettingsDeveloperHaptics> {
                            HapticsTestScreen(
                                themePreferences = themePreferences,
                                onNavigateBack = popBackStack
                            )
                        }
                        entry<NavigationRoutes.SettingsDeveloperHapticsLab> {
                            HapticsLabScreen(
                                themePreferences = themePreferences,
                                onNavigateBack = popBackStack
                            )
                        }

                        entry<NavigationRoutes.SettingsDataManagement> {
                            DataManagementScreen(
                                waterIntakeRepository = waterIntakeRepository,
                                containerPresetRepository = containerPresetRepository,
                                customBeverageRepository = customBeverageRepository,
                                userRepository = userRepository,
                                themePreferences = themePreferences,
                                onNavigateBack = popBackStack
                            )
                        }

                        entry<NavigationRoutes.SettingsProfile> {
                            CompositionLocalProvider(
                                LocalSharedTransitionScope provides this@SharedTransitionLayout,
                                LocalNavAnimatedVisibilityScope provides LocalNavAnimatedContentScope.current
                            ) {
                                userProfile?.let { profile ->
                                    val todayStatistics by waterIntakeRepository.getTodayStatistics().collectAsState(
                                        initial = com.cemcakmak.hydrotracker.data.database.repository.TodayStatistics(
                                            0.0, 0f, 0, 0.0, 0.0, null, null, false, 0.0
                                        )
                                    )
                                    val last30DaysEntries by waterIntakeRepository.getLast30DaysEntries().collectAsState(
                                        initial = emptyList()
                                    )

                                    ProfileSettingsScreen(
                                        userProfile = profile,
                                        themePreferences = themePreferences,
                                        userRepository = userRepository,
                                        todayEntryCount = todayStatistics.entryCount,
                                        daysTracked = last30DaysEntries.groupBy { it.date }.size,
                                        todayGoalProgress = todayStatistics.goalProgress,
                                        onNavigateToCrop = { uri ->
                                            wasPop = true
                                            backStack.add(
                                                NavigationRoutes.CropProfileImage(
                                                    sourceUri = uri.toString(),
                                                    caller = NavigationRoutes.CropCaller.PROFILE_SETTINGS
                                                )
                                            )
                                        },
                                        onNavigateBack = popBackStack
                                    )
                                } ?: LoadingScreen()
                            }
                        }

                        entry<NavigationRoutes.CropProfileImage> { route ->
                            val scope = rememberCoroutineScope()

                            CropProfileImageScreen(
                                sourceUri = route.sourceUri.toUri(),
                                onCropCompleted = { croppedUri ->
                                    when (route.caller) {
                                        NavigationRoutes.CropCaller.PROFILE_SETTINGS -> {
                                            userProfile?.let { profile ->
                                                scope.launch {
                                                    userRepository.saveUserProfile(
                                                        profile.copy(profileImagePath = croppedUri?.toString())
                                                    )
                                                }
                                            }
                                        }
                                        NavigationRoutes.CropCaller.ONBOARDING -> {
                                            onboardingViewModel.updateProfileImage(croppedUri)
                                        }
                                    }
                                    popBackStack()
                                },
                                onNavigateBack = popBackStack
                            )
                        }

                        
                    }
                )
                }

                // FAB sheets / dialogues hosted at the scaffold level so they work from any tab.
                if (showAddBeverageSheet) {
                    AddCustomBeverageBottomSheet(
                        onDismiss = { showAddBeverageSheet = false },
                        onAdd = { name, hydrationMultiplier, iconKey ->
                            coroutineScope.launch {
                                customBeverageRepository.addBeverage(name, hydrationMultiplier, iconKey)
                                showAddBeverageSheet = false
                            }
                        }
                    )
                }

                if (showAddContainerSheet) {
                    AddContainerPresetBottomSheet(
                        volumeUnit = userProfile?.volumeUnit ?: VolumeUnit.MILLILITRES,
                        onDismiss = { showAddContainerSheet = false },
                        onAdd = { name, volume, iconType, iconName ->
                            coroutineScope.launch {
                                containerPresetRepository.addPreset(name, volume, iconType, iconName)
                                showAddContainerSheet = false
                            }
                        }
                    )
                }

                if (showPastEntryDialog && pastEntryDate != null && userProfile != null) {
                    CustomWaterDialog(
                        onDismiss = {
                            showPastEntryDialog = false
                            pastEntryDate = null
                        },
                        onConfirm = { amount ->
                            coroutineScope.launch {
                                val containerPreset = ContainerPreset(
                                    name = "Custom",
                                    volume = amount,
                                    iconType = "DRAWABLE",
                                    iconName = "water_filled"
                                )
                                waterIntakeRepository.addWaterIntake(
                                    amount = amount,
                                    containerPreset = containerPreset,
                                    beverageKey = pastEntrySelectedBeverage.storageKey,
                                    beverageMultiplier = pastEntrySelectedBeverage.storedMultiplier,
                                    date = pastEntryDate
                                )
                                showPastEntryDialog = false
                                pastEntryDate = null
                            }
                        },
                        selectedBeverage = pastEntrySelectedBeverage,
                        onBeverageChange = { pastEntrySelectedBeverage = it },
                        volumeUnit = userProfile.volumeUnit,
                        beverages = activeBeverages
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Loading HydroTracker...",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Checking your hydration data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

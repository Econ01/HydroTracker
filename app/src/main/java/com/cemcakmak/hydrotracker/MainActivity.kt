package com.cemcakmak.hydrotracker

import android.app.Application
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent
import com.cemcakmak.hydrotracker.data.repository.*
import com.cemcakmak.hydrotracker.data.database.DatabaseInitializer
import com.cemcakmak.hydrotracker.data.database.DatabaseMigrationHelper
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository
import com.cemcakmak.hydrotracker.data.database.repository.ContainerPresetRepository
import com.cemcakmak.hydrotracker.presentation.common.*
import com.cemcakmak.hydrotracker.presentation.home.HomeScreen
import com.cemcakmak.hydrotracker.presentation.history.HistoryScreen
import com.cemcakmak.hydrotracker.presentation.profile.ProfileScreen
import com.cemcakmak.hydrotracker.presentation.settings.SettingsScreen
import com.cemcakmak.hydrotracker.presentation.settings.SettingsHubScreen
import com.cemcakmak.hydrotracker.presentation.settings.AppearanceScreen
import com.cemcakmak.hydrotracker.presentation.settings.PlaceholderScreen
import com.cemcakmak.hydrotracker.presentation.settings.HealthConnectDataScreen
import com.cemcakmak.hydrotracker.presentation.settings.BeverageTypesScreen
import com.cemcakmak.hydrotracker.presentation.onboarding.*
import com.cemcakmak.hydrotracker.notifications.*
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.health.HealthConnectManager

// Predictive-back gesture animation tuning
private const val PB_OUTGOING_SCALE = 0.85f // how much the current screen shrinks (1.0 = no shrink, 0.7 = aggressive)
private const val PB_OUTGOING_SLIDE_FRACTION = 0.15f    // lateral drift toward the swipe edge, as a fraction of screen width
private const val PB_INCOMING_INITIAL_SCALE = 0.95f // previous screen starts at this scale, grows to 1.0 (1.0 = no growth)
private const val PB_SPRING_DAMPING = 1.0f  // 1.0 = critically damped (no bounce); <1.0 adds bounce
private const val PB_SPRING_STIFFNESS = 1600f   // higher = snappier; Material 3's default effects spec is 1600

private val TOP_LEVEL_TAB_KEYS: Set<NavKey> = setOf(
    NavigationRoutes.Home,
    NavigationRoutes.History,
    NavigationRoutes.Profile,
    NavigationRoutes.Settings,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
class MainActivity : ComponentActivity() {
    private lateinit var userRepository: UserRepository
    private lateinit var waterIntakeRepository: WaterIntakeRepository
    private lateinit var containerPresetRepository: ContainerPresetRepository

    // Modern permission launcher
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val userProfile = userRepository.userProfile.value
            userProfile?.takeIf { it.isOnboardingCompleted }?.let {
                HydroNotificationScheduler.startNotifications(this, it)
            }
        }
        // Handle denied case if needed - currently no-op as per original logic
    }

    // Health Connect permission launcher - using proper Activity context
    private lateinit var healthConnectPermissionLauncher: ActivityResultLauncher<Set<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Set navigation bar contrast enforcement - only available on Android 10+ (API 29)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        // Init
        userRepository = UserRepository(applicationContext)

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
                    val userProfile = userRepository.userProfile.value
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
                // You can add a toast or dialog here if needed for critical failures
            }
        }

        waterIntakeRepository = DatabaseInitializer.getWaterIntakeRepository(
            applicationContext, userRepository
        )

        containerPresetRepository = DatabaseInitializer.getContainerPresetRepository(
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
            HydroTrackerApp(
                userRepository,
                waterIntakeRepository,
                containerPresetRepository,
                notificationPermissionLauncher,
                healthConnectPermissionLauncher
            )
        }
    }

}

@Composable
fun HydroTrackerApp(
    userRepository: UserRepository,
    waterIntakeRepository: WaterIntakeRepository,
    containerPresetRepository: ContainerPresetRepository,
    notificationPermissionLauncher: ActivityResultLauncher<String>,
    healthConnectPermissionLauncher: ActivityResultLauncher<Set<String>>
) {
    val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModelFactory(userRepository))
    val themePreferences by themeViewModel.themePreferences.collectAsState()
    val userProfile by userRepository.userProfile.collectAsState()
    val isOnboardingCompleted by userRepository.isOnboardingCompleted.collectAsState()
    val beveragePreferences by userRepository.beveragePreferences.collectAsState()
    val activeBeverageTypes = remember(beveragePreferences) { beveragePreferences.toDisplayList() }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    LaunchedEffect(isOnboardingCompleted, userProfile) {
        isLoading = false

        // Check for new user day when app starts
        if (isOnboardingCompleted && userProfile != null) {
            waterIntakeRepository.checkAndHandleNewUserDay()

            // Perform app launch sync to import any missed external Health Connect data
            waterIntakeRepository.getSyncManager().performAppLaunchSync(context, userRepository, waterIntakeRepository)
        }
    }

    HydroTrackerTheme(themePreferences = themePreferences) {
        if (isLoading) {
            LoadingScreen()
        } else {
            val startKey: NavigationRoutes = if (isOnboardingCompleted && userProfile != null)
                NavigationRoutes.Home else NavigationRoutes.Onboarding
            val backStack = rememberNavBackStack(startKey)
            val currentKey = backStack.lastOrNull() as? NavigationRoutes ?: startKey

            MainNavigationScaffold(
                backStack = backStack,
                currentKey = currentKey,
                userProfileImagePath = userProfile?.profileImagePath
            ) { padding ->
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.padding(padding),
                    predictivePopTransitionSpec = { swipeEdge ->
                        val floatSpring = spring<Float>(
                            dampingRatio = PB_SPRING_DAMPING,
                            stiffness = PB_SPRING_STIFFNESS,
                        )
                        val outgoingKey = backStack.lastOrNull()
                        if (outgoingKey in TOP_LEVEL_TAB_KEYS) {
                            // No animation when popping a top-level tab back to Home.
                            ContentTransform(
                                targetContentEnter = EnterTransition.None,
                                initialContentExit = ExitTransition.None,
                            )
                        } else {
                            val slideDirection = if (swipeEdge == NavigationEvent.EDGE_LEFT) 1 else -1
                            val offsetSpring = spring<IntOffset>(
                                dampingRatio = PB_SPRING_DAMPING,
                                stiffness = PB_SPRING_STIFFNESS,
                            )
                            ContentTransform(
                                targetContentEnter = fadeIn(animationSpec = floatSpring) +
                                    scaleIn(
                                        initialScale = PB_INCOMING_INITIAL_SCALE,
                                        animationSpec = floatSpring,
                                    ),
                                initialContentExit = scaleOut(
                                    targetScale = PB_OUTGOING_SCALE,
                                    animationSpec = floatSpring,
                                ) + slideOutHorizontally(animationSpec = offsetSpring) { width ->
                                    (slideDirection * width * PB_OUTGOING_SLIDE_FRACTION).toInt()
                                },
                            )
                        }
                    },
                    entryProvider = entryProvider {
                        entry<NavigationRoutes.Onboarding> {
                            val ctx = LocalContext.current
                            val onboardingVM: OnboardingViewModel = viewModel(
                                factory = OnboardingViewModelFactory(ctx.applicationContext as Application, userRepository)
                            )

                            OnboardingScreen(
                                onNavigateToHome = {
                                    backStack.apply {
                                        clear()
                                        add(NavigationRoutes.Home)
                                    }
                                },
                                viewModel = onboardingVM
                            )
                        }

                        entry<NavigationRoutes.Home> {
                            userProfile?.let {
                                HomeScreen(
                                    userProfile = it,
                                    waterIntakeRepository = waterIntakeRepository,
                                    containerPresetRepository = containerPresetRepository,
                                    activeBeverageTypes = activeBeverageTypes,
                                    onNavigateToSettings = { backStack.add(NavigationRoutes.SettingsOld) }
                                )
                            } ?: LoadingScreen()
                        }

                        entry<NavigationRoutes.History> {
                            HistoryScreen(
                                waterIntakeRepository = waterIntakeRepository,
                                themePreferences = themePreferences
                            ) {
                                backStack.removeLastOrNull()
                            }
                        }

                        entry<NavigationRoutes.Profile> {
                            userProfile?.let {
                                ProfileScreen(
                                    userProfile = it,
                                    userRepository = userRepository,
                                    waterIntakeRepository = waterIntakeRepository,
                                    onNavigateBack = { backStack.removeLastOrNull() }
                                )
                            } ?: LoadingScreen()
                        }

                        entry<NavigationRoutes.SettingsOld> {
                            SettingsScreen(
                                themePreferences = themePreferences,
                                userProfile = userProfile,
                                userRepository = userRepository,
                                waterIntakeRepository = waterIntakeRepository,
                                containerPresetRepository = containerPresetRepository,
                                onColorSourceChange = themeViewModel::setColorSource,
                                onDarkModeChange = themeViewModel::updateDarkModePreference,
                                onPureBlackChange = themeViewModel::updatePureBlackPreference,
                                onWeekStartDayChange = themeViewModel::updateWeekStartDay,
                                onHydrationStandardChange = { newStandard ->
                                    userProfile?.let { profile ->
                                        val newGoal = com.cemcakmak.hydrotracker.utils.WaterCalculator.calculateDailyWaterGoal(
                                            gender = profile.gender,
                                            ageGroup = profile.ageGroup,
                                            activityLevel = profile.activityLevel,
                                            weight = profile.weight,
                                            hydrationStandard = newStandard
                                        )

                                        val updatedProfile = profile.copy(
                                            hydrationStandard = newStandard,
                                            dailyWaterGoal = newGoal
                                        )
                                        userRepository.saveUserProfile(updatedProfile)
                                    }
                                },
                                isDynamicColorAvailable = themeViewModel.isDynamicColorAvailable(),
                                onRequestNotificationPermission = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                healthConnectPermissionLauncher = healthConnectPermissionLauncher,
                                onNavigateBack = { backStack.removeLastOrNull() },
                                onNavigateToOnboarding = {
                                    backStack.apply {
                                        clear()
                                        add(NavigationRoutes.Onboarding)
                                    }
                                },
                                onNavigateToBeverageTypes = {
                                    backStack.add(NavigationRoutes.BeverageTypes)
                                },
                                onNavigateToHealthConnectData = {
                                    backStack.add(NavigationRoutes.HealthConnectData)
                                }
                            )
                        }

                        entry<NavigationRoutes.Settings> {
                            SettingsHubScreen(
                                developerOptionsEnabled = BuildConfig.DEBUG,
                                onNavigateTo = { key -> backStack.add(key) }
                            )
                        }

                        entry<NavigationRoutes.SettingsAppearance> {
                            AppearanceScreen(
                                themePreferences = themePreferences,
                                isDynamicColorAvailable = themeViewModel.isDynamicColorAvailable(),
                                onColorSourceChange = themeViewModel::setColorSource,
                                onDarkModeChange = themeViewModel::updateDarkModePreference,
                                onPureBlackChange = themeViewModel::updatePureBlackPreference,
                                onNavigateBack = { backStack.removeLastOrNull() }
                            )
                        }
                        entry<NavigationRoutes.SettingsDisplay> {
                            PlaceholderScreen(title = "Display & Locale", onNavigateBack = { backStack.removeLastOrNull() })
                        }
                        entry<NavigationRoutes.SettingsHydration> {
                            PlaceholderScreen(title = "Hydration & Health", onNavigateBack = { backStack.removeLastOrNull() })
                        }
                        entry<NavigationRoutes.SettingsContainers> {
                            PlaceholderScreen(title = "Quick Add Customization", onNavigateBack = { backStack.removeLastOrNull() })
                        }
                        entry<NavigationRoutes.SettingsNotifications> {
                            PlaceholderScreen(title = "Notifications", onNavigateBack = { backStack.removeLastOrNull() })
                        }
                        entry<NavigationRoutes.SettingsSupport> {
                            PlaceholderScreen(title = "Support Development", onNavigateBack = { backStack.removeLastOrNull() })
                        }
                        entry<NavigationRoutes.SettingsAbout> {
                            PlaceholderScreen(title = "About", onNavigateBack = { backStack.removeLastOrNull() })
                        }
                        entry<NavigationRoutes.SettingsDeveloper> {
                            PlaceholderScreen(title = "Developer Options", onNavigateBack = { backStack.removeLastOrNull() })
                        }

                        entry<NavigationRoutes.HealthConnectData> {
                            HealthConnectDataScreen(
                                waterIntakeRepository = waterIntakeRepository,
                                onNavigateBack = { backStack.removeLastOrNull() }
                            )
                        }

                        entry<NavigationRoutes.BeverageTypes> {
                            BeverageTypesScreen(
                                userRepository = userRepository,
                                onNavigateBack = { backStack.removeLastOrNull() }
                            )
                        }
                    }
                )
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

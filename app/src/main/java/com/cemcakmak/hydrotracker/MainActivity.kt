package com.cemcakmak.hydrotracker

import android.app.Application
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import androidx.compose.animation.*
import androidx.compose.ui.graphics.TransformOrigin
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
import com.cemcakmak.hydrotracker.presentation.settings.HealthConnectDataScreen
import com.cemcakmak.hydrotracker.presentation.onboarding.*
import com.cemcakmak.hydrotracker.notifications.*
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.health.HealthConnectManager

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
    val navController = rememberNavController()
    val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModelFactory(userRepository))
    val themePreferences by themeViewModel.themePreferences.collectAsState()
    val userProfile by userRepository.userProfile.collectAsState()
    val isOnboardingCompleted by userRepository.isOnboardingCompleted.collectAsState()
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

    val startDestination = if (isOnboardingCompleted && userProfile != null)
        NavigationRoutes.HOME else NavigationRoutes.ONBOARDING

    HydroTrackerTheme(themePreferences = themePreferences) {
        if (isLoading) {
            LoadingScreen()
        } else {
            val currentRoute by navController.currentBackStackEntryAsState()
            val route = currentRoute?.destination?.route ?: startDestination

            MainNavigationScaffold(
                navController = navController, 
                currentRoute = route, 
                userProfileImagePath = userProfile?.profileImagePath
            ) { padding ->
                NavHost(
                    navController = navController, 
                    startDestination = startDestination,
                    // Predictive back gesture animations
                    popExitTransition = {
                        scaleOut(
                            targetScale = 0.7f,
                            transformOrigin = TransformOrigin(
                                pivotFractionX = 1f,
                                pivotFractionY = 0.5f
                            )
                        ) + fadeOut()
                    },
                    popEnterTransition = {
                        scaleIn(
                            initialScale = 1.1f,
                            transformOrigin = TransformOrigin(
                                pivotFractionX = 0.4f,
                                pivotFractionY = 0.5f
                            )
                        )
                    }
                ) {

                    composable(NavigationRoutes.ONBOARDING) {
                        val context = LocalContext.current
                        val onboardingVM: OnboardingViewModel = viewModel(
                            factory = OnboardingViewModelFactory(context.applicationContext as Application, userRepository)
                        )

                        OnboardingScreen(
                            onNavigateToHome = {
                                navController.navigate(NavigationRoutes.HOME) {
                                    popUpTo(NavigationRoutes.ONBOARDING) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            viewModel = onboardingVM
                        )
                    }

                    composable(NavigationRoutes.HOME) {
                        userProfile?.let {
                            HomeScreen(
                                userProfile = it,
                                waterIntakeRepository = waterIntakeRepository,
                                containerPresetRepository = containerPresetRepository,
                                onNavigateToHistory = { navController.navigate(NavigationRoutes.HISTORY) },
                                onNavigateToSettings = { navController.navigate(NavigationRoutes.SETTINGS) },
                                onNavigateToProfile = { navController.navigate(NavigationRoutes.PROFILE) }
                            )
                        } ?: LoadingScreen()
                    }

                    composable(NavigationRoutes.HISTORY) {
                        HistoryScreen(
                            waterIntakeRepository = waterIntakeRepository,
                            themePreferences = themePreferences
                        ) {
                            navController.popBackStack()
                        }
                    }

                    composable(NavigationRoutes.PROFILE) {
                        userProfile?.let {
                            ProfileScreen(
                                userProfile = it,
                                userRepository = userRepository,
                                waterIntakeRepository = waterIntakeRepository,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        } ?: LoadingScreen()
                    }

                    composable(NavigationRoutes.SETTINGS) {
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
                                    // Recalculate daily water goal with new standard
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
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToOnboarding = {
                                navController.navigate(NavigationRoutes.ONBOARDING) {
                                    popUpTo(NavigationRoutes.HOME) { inclusive = true }
                                }
                            },
                            onNavigateToHealthConnectData = {
                                navController.navigate(NavigationRoutes.HEALTH_CONNECT_DATA)
                            }
                        )
                    }

                    composable(NavigationRoutes.HEALTH_CONNECT_DATA) {
                        HealthConnectDataScreen(
                            waterIntakeRepository = waterIntakeRepository,
                            onNavigateBack = { navController.popBackStack() }
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

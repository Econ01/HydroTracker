package com.cemcakmak.hydrotracker.presentation.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.models.DarkModePreference
import com.cemcakmak.hydrotracker.data.models.ColorSource
import com.cemcakmak.hydrotracker.data.models.WeekStartDay
import com.cemcakmak.hydrotracker.data.models.UserProfile
import com.cemcakmak.hydrotracker.data.models.HydrationStandard
import com.cemcakmak.hydrotracker.data.repository.UserRepository
import com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository
import com.cemcakmak.hydrotracker.data.database.repository.ContainerPresetRepository
import com.cemcakmak.hydrotracker.presentation.common.HydroSnackbarHost
import com.cemcakmak.hydrotracker.presentation.common.showSuccessSnackbar
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import android.content.Intent
import androidx.compose.ui.res.painterResource
import com.cemcakmak.hydrotracker.R
import androidx.compose.ui.graphics.Color
import androidx.core.net.toUri
import com.cemcakmak.hydrotracker.BuildConfig
import com.cemcakmak.hydrotracker.health.HealthConnectManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    themePreferences: ThemePreferences = ThemePreferences(),
    userProfile: UserProfile? = null,
    userRepository: UserRepository? = null,
    waterIntakeRepository: WaterIntakeRepository? = null,
    containerPresetRepository: ContainerPresetRepository? = null,
    onDarkModeChange: (DarkModePreference) -> Unit = {},
    onColorSourceChange: (ColorSource) -> Unit = {},
    onPureBlackChange: (Boolean) -> Unit = {},
    onWeekStartDayChange: (WeekStartDay) -> Unit = {},
    onHydrationStandardChange: (HydrationStandard) -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    healthConnectPermissionLauncher: ActivityResultLauncher<Set<String>>? = null,
    onNavigateBack: () -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
    onNavigateToHealthConnectData: () -> Unit = {},
    isDynamicColorAvailable: Boolean = true
) {
    // Animation states
    var isVisible by remember { mutableStateOf(false) }

    // Developer options state
    var developerOptionsEnabled by remember {
        mutableStateOf(
            userRepository?.loadDeveloperOptionsEnabled() ?: false
        )
    }
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    // Snackbar state for Material 3 Expressive feedback
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Theme Customization Section
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    initialOffsetY = { -it / 3 }
                ) + fadeIn(animationSpec = tween(600))
            ) {
                ThemeSection(
                    themePreferences = themePreferences,
                    onColorSourceChange = onColorSourceChange,
                    onDarkModeChange = onDarkModeChange,
                    onPureBlackChange = onPureBlackChange,
                    isDynamicColorAvailable = isDynamicColorAvailable,
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Display Section
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    initialOffsetY = { it / 2 }
                ) + fadeIn(animationSpec = tween(600, delayMillis = 200))
            ) {
                DisplaySection(
                    themePreferences = themePreferences,
                    onWeekStartDayChange = onWeekStartDayChange
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Hydration Calculation Section
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    initialOffsetY = { it / 2 }
                ) + fadeIn(animationSpec = tween(600, delayMillis = 250))
            ) {
                HydrationSection(
                    userProfile = userProfile,
                    onHydrationStandardChange = onHydrationStandardChange
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Container Presets Section
            if (containerPresetRepository != null) {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        initialOffsetY = { it / 2 }
                    ) + fadeIn(animationSpec = tween(600, delayMillis = 260))
                ) {
                    ContainerPresetsSection(
                        containerPresetRepository = containerPresetRepository,
                        snackbarHostState = snackbarHostState
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            // Health Connect Section - only show if supported on this Android version
            if (HealthConnectManager.isVersionSupported()) {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        initialOffsetY = { it / 2 }
                    ) + fadeIn(animationSpec = tween(600, delayMillis = 275))
                ) {
                    HealthConnectSection(
                        healthConnectPermissionLauncher = healthConnectPermissionLauncher,
                        userProfile = userProfile,
                        onHealthConnectSyncChange = { enabled ->
                            userProfile?.let { profile ->
                                val updatedProfile = profile.copy(healthConnectSyncEnabled = enabled)
                                userRepository?.saveUserProfile(updatedProfile)
                            }
                        },
                        onNavigateToHealthConnectData = onNavigateToHealthConnectData
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Notification Settings Section
            NotificationSettingsSection(
                userProfile = userProfile,
                onRequestPermission = onRequestNotificationPermission,
                isVisible = isVisible
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Developer Options Section (only show if enabled and repositories available)
            if (developerOptionsEnabled && userRepository != null && waterIntakeRepository != null) {
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
                    DeveloperOptionsSection(
                        userRepository = userRepository,
                        waterIntakeRepository = waterIntakeRepository,
                        snackbarHostState = snackbarHostState,
                        onNavigateToOnboarding = onNavigateToOnboarding,
                        onDisableDeveloperOptions = {
                            developerOptionsEnabled = false
                            userRepository.saveDeveloperOptionsEnabled(false)
                        },
                        userProfile = userProfile
                    )
                }
            }

            // Support Section
            SupportSection(
                isVisible = isVisible
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // About Section
            AboutSection(
                isVisible = isVisible
            )

            // Footer with app info
            FooterSection(
                onVersionTap = {
                    val currentTime = System.currentTimeMillis()

                    // Reset counter if more than 3 seconds have passed
                    if (currentTime - lastTapTime > 3000) {
                        tapCount = 1
                    } else {
                        tapCount++
                    }

                    lastTapTime = currentTime

                    // Activate developer options after 10 taps
                    if (tapCount >= 10 && !developerOptionsEnabled) {
                        developerOptionsEnabled = true
                        userRepository?.saveDeveloperOptionsEnabled(true)

                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Developer options activated",
                                duration = SnackbarDuration.Short
                            )
                        }

                        tapCount = 0
                    }
                },
                isVisible = isVisible
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThemeSection(
    themePreferences: ThemePreferences,
    onColorSourceChange: (ColorSource) -> Unit,
    onDarkModeChange: (DarkModePreference) -> Unit,
    onPureBlackChange: (Boolean) -> Unit,
    isDynamicColorAvailable: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(5.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Theme Mode Section
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Theme Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Connected Button Groups for Theme Mode
                val haptics = LocalHapticFeedback.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DarkModePreference.entries.forEach { preference ->
                        val isSelected = themePreferences.darkMode == preference

                        ToggleButton(
                            checked = isSelected,
                            onCheckedChange = {
                                onDarkModeChange(preference)
                                haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (preference) {
                                        DarkModePreference.SYSTEM -> if (isSelected) Icons.Filled.Settings else Icons.Outlined.Settings
                                        DarkModePreference.LIGHT -> if (isSelected) Icons.Filled.LightMode else Icons.Outlined.LightMode
                                        DarkModePreference.DARK -> if (isSelected) Icons.Filled.DarkMode else Icons.Outlined.DarkMode
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = when (preference) {
                                        DarkModePreference.SYSTEM -> "System"
                                        DarkModePreference.LIGHT -> "Light"
                                        DarkModePreference.DARK -> "Dark"
                                    },
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }

            // Color Theme Section
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dynamic Colors Toggle with Icon - only show if available
                if (isDynamicColorAvailable) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Dynamic Colors",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Colors from your wallpaper",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = themePreferences.colorSource == ColorSource.DYNAMIC_COLOR,
                            onCheckedChange = { enabled ->
                                onColorSourceChange(
                                    if (enabled) ColorSource.DYNAMIC_COLOR else ColorSource.HYDRO_THEME
                                )
                            },
                            thumbContent = if (themePreferences.colorSource == ColorSource.DYNAMIC_COLOR) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                }
                            } else {
                                null
                            }
                        )
                    }
                }

                // Pure Black Toggle
                val haptics = LocalHapticFeedback.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "AMOLED Mode",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "True black backgrounds in dark mode",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = themePreferences.usePureBlack,
                        onCheckedChange = { enabled ->
                            onPureBlackChange(enabled)
                            haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                        },
                        thumbContent = if (themePreferences.usePureBlack) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            }
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }
}


@Composable
private fun DisplaySection(
    themePreferences: ThemePreferences,
    onWeekStartDayChange: (WeekStartDay) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(5.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Week Start",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Connected Button Groups for Week Start Day
            val haptics = LocalHapticFeedback.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WeekStartDay.entries.forEach { weekStartDay ->
                    val isSelected = themePreferences.weekStartDay == weekStartDay

                    ToggleButton(
                        checked = isSelected,
                        onCheckedChange = {
                            onWeekStartDayChange(weekStartDay)
                            haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (weekStartDay) {
                                    WeekStartDay.SUNDAY -> if (isSelected) Icons.Filled.Weekend else Icons.Outlined.Weekend
                                    WeekStartDay.MONDAY -> if (isSelected) Icons.Filled.Today else Icons.Outlined.Today
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = when (weekStartDay) {
                                    WeekStartDay.SUNDAY -> "Sunday"
                                    WeekStartDay.MONDAY -> "Monday"
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}






@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AsyncDebugActionButton(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    snackbarHostState: SnackbarHostState,
    onClick: suspend () -> Unit,
    confirmationMessage: String
) {
    var isPressed by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "debug_button_press"
    )

    Card(
        onClick = {
            if (!isLoading) {
                isPressed = true
                isLoading = true
                coroutineScope.launch {
                    try {
                        onClick()
                        snackbarHostState.showSnackbar(
                            message = confirmationMessage,
                            duration = SnackbarDuration.Long
                        )
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(
                            message = "Error: ${e.message}",
                            duration = SnackbarDuration.Long
                        )
                    } finally {
                        isLoading = false
                        kotlinx.coroutines.delay(150)
                        isPressed = false
                    }
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SupportSection(
    isVisible: Boolean
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            initialOffsetY = { it / 2 }
        ) + fadeIn(animationSpec = tween(600, delayMillis = 450))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Support Development",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "If you like to support my work, you can donate me :)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // PayPal Button
                    FilledTonalButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, "https://www.paypal.com/donate/?hosted_button_id=CQUZLNRM79CAU".toUri())
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF003087)
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.paypal),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (isDarkTheme) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.inverseOnSurface
                            )
                            Text(
                                text = "PayPal",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isDarkTheme) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.inverseOnSurface
                            )
                        }
                    }

                    // Buy Me a Coffee Button
                    FilledTonalButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, "https://buymeacoffee.com/thegadgetgeek".toUri())
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFFFDD00)
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.coffee),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (isDarkTheme) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Buy Me Coffee",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isDarkTheme) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FooterSection(
    onVersionTap: () -> Unit,
    isVisible: Boolean
) {
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
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "HydroTracker",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable { onVersionTap() }
                )
                
                Text(
                    text = "Developed by Ali Cem Çakmak",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DeveloperOptionsSection(
    userRepository: UserRepository,
    waterIntakeRepository: WaterIntakeRepository,
    snackbarHostState: SnackbarHostState,
    onNavigateToOnboarding: () -> Unit,
    onDisableDeveloperOptions: () -> Unit,
    userProfile: UserProfile?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(5.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeveloperMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Developer Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Text(
                text = "These options are for development and testing purposes only.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )
            
            // Disable Developer Options Toggle
            Card(
                onClick = onDisableDeveloperOptions,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Disable Developer Options",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Hide developer options from settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.3f)
            )

            // Reset Onboarding Button
            ResetOnboardingButton(
                snackbarHostState = snackbarHostState,
                onClick = {
                    userRepository.resetOnboarding()
                    onNavigateToOnboarding()
                }
            )

            // Clear All Data Button
            AsyncDebugActionButton(
                title = "Clear All Data",
                description = "Remove all stored user preferences and water data",
                icon = Icons.Default.DeleteForever,
                snackbarHostState = snackbarHostState,
                onClick = {
                    userRepository.clearUserProfile()
                    waterIntakeRepository.clearAllData()
                },
                confirmationMessage = "All data cleared!"
            )

            AsyncDebugActionButton(
                title = "Inject 30-Day Data",
                description = "Add realistic water intake data for past 30 days",
                icon = Icons.Default.DataObject,
                snackbarHostState = snackbarHostState,
                onClick = {
                    waterIntakeRepository.injectDebugData()
                },
                confirmationMessage = "30 days of realistic data injected! Check History screen."
            )

            // Health Connect Debug Section - only show if Health Connect is supported and enabled
            if (HealthConnectManager.isVersionSupported() && userProfile?.healthConnectSyncEnabled == true) {
                val context = LocalContext.current // Capture context in Composable scope

                Text(
                    text = "Health Connect Testing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                // Test Health Connect Write
                AsyncDebugActionButton(
                    title = "Test Health Connect Write",
                    description = "Write a 250ml test entry to Health Connect",
                    icon = Icons.Default.CloudUpload,
                    snackbarHostState = snackbarHostState,
                    onClick = {
                        try {
                            val healthConnectManager = HealthConnectManager
                            val testEntry = com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry(
                                amount = 250.0,
                                timestamp = System.currentTimeMillis(),
                                date = java.time.LocalDate.now().toString(),
                                containerType = "Debug Test",
                                containerVolume = 250.0,
                                note = "Health Connect Debug Test Entry"
                            )
                            val result = healthConnectManager.writeHydrationRecord(context,testEntry)
                            Log.i("HealthConnectDebug", "Test write result: ${result.getOrNull()}")
                        } catch (e: Exception) {
                            Log.e("HealthConnectDebug", "Test write failed", e)
                        }
                    },
                    confirmationMessage = "Test entry sent to Health Connect! Check logs for results."
                )

                // Test Health Connect Read
                AsyncDebugActionButton(
                    title = "Test Health Connect Read",
                    description = "Read recent hydration records from Health Connect",
                    icon = Icons.Default.CloudDownload,
                    snackbarHostState = snackbarHostState,
                    onClick = {
                        try {
                            val healthConnectManager = HealthConnectManager
                            val yesterday = java.time.Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS)
                            val result = healthConnectManager.readHydrationRecords(context,yesterday)
                            Log.i("HealthConnectDebug", "Found ${result.getOrNull()?.size ?: 0} records since yesterday")
                            result.getOrNull()?.forEach { record ->
                                Log.d("HealthConnectDebug", "Record: ${record.volume.inMilliliters}ml at ${record.startTime}")
                            }
                        } catch (e: Exception) {
                            Log.e("HealthConnectDebug", "Test read failed", e)
                        }
                    },
                    confirmationMessage = "Health Connect read test completed! Check logs for results."
                )

                // Test Health Connect Import (External Data)
                AsyncDebugActionButton(
                    title = "Test Health Connect Import",
                    description = "Import external hydration data from Health Connect",
                    icon = Icons.Default.Download,
                    snackbarHostState = snackbarHostState,
                    onClick = {
                        try {
                            val healthConnectSyncManager = com.cemcakmak.hydrotracker.health.HealthConnectSyncManager
                            val since = java.time.Instant.now().minus(7, java.time.temporal.ChronoUnit.DAYS)
                            Log.i("HealthConnectDebug", "🔄 Starting import test for last 7 days...")
                            healthConnectSyncManager.importExternalHydrationData(context, userRepository, waterIntakeRepository, since) { imported, errors ->
                                Log.i("HealthConnectDebug", "📊 Import test result: $imported entries imported, $errors errors")
                            }
                        } catch (e: Exception) {
                            Log.e("HealthConnectDebug", "Import test failed", e)
                        }
                    },
                    confirmationMessage = "Health Connect import test started! Check logs for detailed results."
                )

                // Health Connect Status Check
                AsyncDebugActionButton(
                    title = "Check Health Connect Status",
                    description = "Verify Health Connect availability and permissions",
                    icon = Icons.Default.HealthAndSafety,
                    snackbarHostState = snackbarHostState,
                    onClick = {
                        try {
                            val healthConnectManager = HealthConnectManager
                            Log.i("HealthConnectDebug", "=== Health Connect Status Check ===")
                            Log.i("HealthConnectDebug", "Available: ${healthConnectManager.isAvailable(context)}")
                            Log.i("HealthConnectDebug", "Has Permissions: ${healthConnectManager.hasPermissions(context)}")
                            Log.i("HealthConnectDebug", "Status: ${healthConnectManager.getStatusMessage(context)}")
                            Log.i("HealthConnectDebug", "Sync Enabled: ${userProfile.healthConnectSyncEnabled}")
                            HealthConnectManager.debugPermissions()
                        } catch (e: Exception) {
                            Log.e("HealthConnectDebug", "Status check failed", e)
                        }
                    },
                    confirmationMessage = "Health Connect status logged! Check: adb logcat | grep HealthConnectDebug"
                )

                Text(
                    text = "💡 View logs with: adb logcat | grep -E \"(HealthConnect|HealthConnectDebug|HealthConnectTest)\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Show Current Status
            val isOnboardingCompleted by userRepository.isOnboardingCompleted.collectAsState()
            val currentUserProfile by userRepository.userProfile.collectAsState()

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Current Status",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Onboarding Completed: $isOnboardingCompleted",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "User Profile Exists: ${currentUserProfile != null}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (currentUserProfile != null) {
                        Text(
                            text = "Daily Goal: ${currentUserProfile!!.dailyWaterGoal.toInt()} ml",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            // Debug Notification Section
            DebugNotificationSection(
                userProfile = userProfile,
                waterIntakeRepository = waterIntakeRepository,
                snackbarHostState = snackbarHostState,
                isVisible = true
            )
        }
    }
}

@Composable
private fun ResetOnboardingButton(
    snackbarHostState: SnackbarHostState,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "debug_button_press"
    )

    Card(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.RestartAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Reset Onboarding",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Clear user data and restart onboarding",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Show confirmation snackbar
    LaunchedEffect(isPressed) {
        if (isPressed) {
            snackbarHostState.showSnackbar(
                message = "Onboarding reset! Redirecting...",
                duration = SnackbarDuration.Short
            )
            kotlinx.coroutines.delay(150)
            isPressed = false
        }
    }
}

@Composable
private fun AboutSection(
    isVisible: Boolean
) {
    val context = LocalContext.current
    var showLicenseBottomSheet by remember { mutableStateOf(false) }
    var showPrivacyPolicyBottomSheet by remember { mutableStateOf(false) }
    var showSourcesBottomSheet by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            initialOffsetY = { it / 2 }
        ) + fadeIn(animationSpec = tween(600, delayMillis = 475))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(5.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                // Sources
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSourcesBottomSheet = true },
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sources & Research",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "View scientific sources and research",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Privacy Policy
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPrivacyPolicyBottomSheet = true },
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Privacy Policy",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "View our privacy policy",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Open Source License
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLicenseBottomSheet = true },
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Open Source License",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "View the software license",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }

    // Sources Bottom Sheet
    if (showSourcesBottomSheet) {
        SourcesBottomSheet(
            onDismiss = { showSourcesBottomSheet = false },
            context = context
        )
    }

    // Privacy Policy Bottom Sheet
    if (showPrivacyPolicyBottomSheet) {
        PrivacyPolicyBottomSheet(
            onDismiss = { showPrivacyPolicyBottomSheet = false },
            context = context
        )
    }

    // License Bottom Sheet
    if (showLicenseBottomSheet) {
        LicenseBottomSheet(
            onDismiss = { showLicenseBottomSheet = false },
            context = context
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LicenseBottomSheet(
    onDismiss: () -> Unit,
    context: android.content.Context
) {
    val sheetState = rememberModalBottomSheetState()
    var licenseText by remember { mutableStateOf("Loading...") }

    LaunchedEffect(Unit) {
        licenseText = try {
            loadLicenseText(context)
        } catch (e: Exception) {
            "Error loading license: ${e.message}"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Open Source License",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // License Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ParsedMarkdownText(
                    text = licenseText,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ParsedMarkdownText(
    text: String,
    modifier: Modifier = Modifier
) {
    val lines = text.split("\n")

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (line in lines) {
            when {
                line.startsWith("# ") -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = line.substring(2),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                line.startsWith("## ") -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = line.substring(3),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                line.startsWith("### ") -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = line.substring(4),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                line.startsWith("- ") -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Text(
                            text = parseInlineMarkdown(line.substring(2)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                line.trim().startsWith("**") && line.trim().endsWith("**") -> {
                    Text(
                        text = line.trim().removeSurrounding("**"),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                line.isBlank() -> {
                    Spacer(modifier = Modifier.height(2.dp))
                }
                line.trim() == "---" -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                else -> {
                    Text(
                        text = parseInlineMarkdown(line),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2
                    )
                }
            }
        }
    }
}

@Composable
private fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        val boldRegex = "\\*\\*(.*?)\\*\\*".toRegex()

        val matches = boldRegex.findAll(text).toList()

        for (match in matches) {
            // Add text before the match
            append(text.substring(currentIndex, match.range.first))

            // Add bold text
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(match.groupValues[1])
            }

            currentIndex = match.range.last + 1
        }

        // Add remaining text
        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }
}

private fun loadLicenseText(context: android.content.Context): String {
    return context.assets.open("LICENSE.md").bufferedReader().use { it.readText() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrivacyPolicyBottomSheet(
    onDismiss: () -> Unit,
    context: android.content.Context
) {
    val sheetState = rememberModalBottomSheetState()
    var privacyPolicyText by remember { mutableStateOf("Loading...") }

    LaunchedEffect(Unit) {
        privacyPolicyText = try {
            loadPrivacyPolicyText(context)
        } catch (e: Exception) {
            "Error loading privacy policy: ${e.message}"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Privacy Policy Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ParsedMarkdownText(
                    text = privacyPolicyText,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun loadPrivacyPolicyText(context: android.content.Context): String {
    return context.assets.open("privacy-policy.md").bufferedReader().use { it.readText() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourcesBottomSheet(
    onDismiss: () -> Unit,
    context: android.content.Context
) {
    val sheetState = rememberModalBottomSheetState()
    var sourcesText by remember { mutableStateOf("Loading...") }

    LaunchedEffect(Unit) {
        sourcesText = try {
            loadSourcesText(context)
        } catch (e: Exception) {
            "Error loading sources: ${e.message}"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sources & Research",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sources Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ParsedMarkdownText(
                    text = sourcesText,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun loadSourcesText(context: android.content.Context): String {
    return context.assets.open("sources.md").bufferedReader().use { it.readText() }
}

@Composable
private fun HydrationSection(
    userProfile: UserProfile?,
    onHydrationStandardChange: (HydrationStandard) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(5.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Science,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Calculation Standard",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Hydration Standard Toggle
            val haptics = LocalHapticFeedback.current
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
                                text = standard.getDisplayName(),
                                style = if (isSelected) MaterialTheme.typography.labelLargeEmphasized else MaterialTheme.typography.labelLarge,
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

            // Current values display
            userProfile?.let { profile ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(5.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Current Standards:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Male baseline:",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${profile.hydrationStandard.getMaleIntake().toInt() / 1000.0} L",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Female baseline:",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${profile.hydrationStandard.getFemaleIntake().toInt() / 1000.0} L",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContainerPresetsSection(
    containerPresetRepository: ContainerPresetRepository,
    snackbarHostState: SnackbarHostState
) {
    val coroutineScope = rememberCoroutineScope()
    var showResetConfirmation by remember { mutableStateOf(false) }
    val presetCount by containerPresetRepository.getAllPresets().collectAsState(initial = emptyList())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(5.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalDrink,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Container Presets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = "Customize the quick select containers on the home screen. Long-press any container to edit or delete it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Current preset count
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current containers:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${presetCount.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Reset to defaults button
            OutlinedButton(
                onClick = { showResetConfirmation = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset to Defaults")
            }
        }
    }

    // Reset confirmation dialog
    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Reset Container Presets?") },
            text = {
                Text("This will remove all custom containers and restore the 7 default presets. This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmation = false
                        coroutineScope.launch {
                            containerPresetRepository.resetToDefaults()
                            snackbarHostState.showSuccessSnackbar(
                                message = "Container presets reset to defaults"
                            )
                        }
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HealthConnectSection(
    healthConnectPermissionLauncher: ActivityResultLauncher<Set<String>>? = null,
    userProfile: UserProfile? = null,
    onHealthConnectSyncChange: (Boolean) -> Unit = {},
    onNavigateToHealthConnectData: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isHealthConnectEnabled by remember { mutableStateOf(false) }
    var healthConnectStatus by remember { mutableStateOf("Checking...") }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val manager = HealthConnectManager

    // Check Health Connect status on component mount and when refresh is triggered
    LaunchedEffect(refreshTrigger) {
        try {

            val status = manager.getStatusMessage(context)
            healthConnectStatus = status
            val newIsEnabled = status == "Health Connect is ready"

            isHealthConnectEnabled = newIsEnabled
        } catch (e: Exception) {
            healthConnectStatus = "Error: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // Listen for when app regains focus to refresh permissions
    androidx.compose.runtime.DisposableEffect(Unit) {
        val activity = context as? ComponentActivity
        val listener = object : android.app.Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: android.app.Activity) {
                if (activity == context) {
                    // Refresh permissions when returning to this screen
                    refreshTrigger++
                }
            }
            override fun onActivityPaused(activity: android.app.Activity) {}
            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
            override fun onActivityStarted(activity: android.app.Activity) {}
            override fun onActivityStopped(activity: android.app.Activity) {}
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: android.app.Activity) {}
        }

        activity?.application?.registerActivityLifecycleCallbacks(listener)
        onDispose {
            activity?.application?.unregisterActivityLifecycleCallbacks(listener)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(5.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.HealthAndSafety,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Health Connect",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Settings Toggle (only show when ready)
            if (isHealthConnectEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sync with Health Connect",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Share hydration data with other health apps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = userProfile?.healthConnectSyncEnabled == true,
                        enabled = isHealthConnectEnabled, // Only enable if permissions are granted
                        onCheckedChange = { enabled ->
                            onHealthConnectSyncChange(enabled)
                        },
                        thumbContent = {
                            Icon(
                                imageVector = Icons.Filled.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    )
                }

                // Health Connect Data button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToHealthConnectData() },
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.health_connect),
                            modifier = Modifier.size(20.dp),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Health Connect Data",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "View and manage Health Connect data",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Request permissions button (when not ready)
            if (!isHealthConnectEnabled && !isLoading &&
                (healthConnectStatus.contains("Permissions") || healthConnectStatus.contains("Missing permissions"))) {

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (healthConnectPermissionLauncher != null) {
                                // Use the proper Activity-based permission launcher
                                coroutineScope.launch {
                                    manager.checkPermissionsAndRun(context, healthConnectPermissionLauncher) {
                                        // Permissions granted callback
                                        healthConnectStatus = "Health Connect is ready"
                                        isHealthConnectEnabled = true
                                    }
                                }
                            } else {
                                Log.w("HealthConnect", "Permission launcher not available")
                                healthConnectStatus = "Error: Permission launcher not available"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (healthConnectStatus.contains("Missing")) "Try Again" else "Grant Permissions")
                    }

                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    HydroTrackerTheme {
        SettingsScreen()
    }
}
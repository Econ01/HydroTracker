package com.cemcakmak.hydrotracker.presentation.settings

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.EnterExitState
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import android.content.Context
import android.os.Build
import android.view.RoundedCorner
import android.view.WindowManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.ColorSource
import com.cemcakmak.hydrotracker.data.models.DarkModePreference
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppearanceScreen(
    themePreferences: ThemePreferences = ThemePreferences(),
    isDynamicColorAvailable: Boolean = true,
    onColorSourceChange: (ColorSource) -> Unit = {},
    onDarkModeChange: (DarkModePreference) -> Unit = {},
    onPureBlackChange: (Boolean) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    paddingValues: PaddingValues = PaddingValues()
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val haptics = LocalHapticFeedback.current

    val isPreview = LocalInspectionMode.current
    val animatedContentScope = if (isPreview) null else LocalNavAnimatedContentScope.current
    val isExiting = animatedContentScope?.transition?.targetState == EnterExitState.PostExit

    val context = LocalContext.current
    val density = LocalDensity.current
    val deviceCornerRadius = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val insets = windowManager.currentWindowMetrics.windowInsets
            val corner = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)
            corner?.let { with(density) { it.radius.toDp() } }
        } else null
    } ?: 24.dp

    val cornerRadius = if (isExiting) deviceCornerRadius else 0.dp

    // Scroll haptic logic
    var wasExpanded by remember { mutableStateOf(true) }
    var wasCollapsed by remember { mutableStateOf(false) }
    
    LaunchedEffect(scrollBehavior.state) {
        snapshotFlow { scrollBehavior.state.collapsedFraction }
            .collect { fraction ->
                val isExpanded = fraction == 0f
                val isCollapsed = fraction == 1f

                if (isExpanded && !wasExpanded) {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                }
                if (isCollapsed && !wasCollapsed) {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                }

                wasExpanded = isExpanded
                wasCollapsed = isCollapsed
            }
    }

    Scaffold(
        modifier = Modifier
            .padding(paddingValues)
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .clip(RoundedCornerShape(cornerRadius)),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Appearance") },
                navigationIcon = {
                    val collapsedFraction = scrollBehavior.state.collapsedFraction
                    val buttonWidth = (40 - collapsedFraction * 8).dp
                    FilledIconButton(
                        onClick = {
                            onNavigateBack()
                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        },
                        shapes = IconButtonDefaults.shapes(),
                        colors = IconButtonDefaults.filledIconButtonColors(),
                        modifier = Modifier.size(width = buttonWidth, height = 40.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.arrow_back_filled),
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ThemePreviewCard(themePreferences = themePreferences)

            ThemeSection(
                themePreferences = themePreferences,
                onColorSourceChange = onColorSourceChange,
                onDarkModeChange = onDarkModeChange,
                onPureBlackChange = onPureBlackChange,
                isDynamicColorAvailable = isDynamicColorAvailable
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThemePreviewCard(themePreferences: ThemePreferences) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animatedProgress.animateTo(
            targetValue = 0.5f,
            animationSpec = tween(durationMillis = 1200)
        )
    }

    val isDark = when (themePreferences.darkMode) {
        DarkModePreference.DARK -> true
        DarkModePreference.LIGHT -> false
        DarkModePreference.SYSTEM -> isSystemInDarkTheme()
    }

    val border = if (themePreferences.usePureBlack && isDark) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    } else {
        null
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        shape = RoundedCornerShape(size = 30.dp),
        tonalElevation = 2.dp,
        border = border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "1,250 / 2,500 ml",
                style = MaterialTheme.typography.headlineMedium
            )

            LinearWavyProgressIndicator(
                progress = { animatedProgress.value },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                stroke = WavyProgressIndicatorDefaults.linearIndicatorStroke,
                trackStroke = WavyProgressIndicatorDefaults.linearTrackStroke,
                amplitude = WavyProgressIndicatorDefaults.indicatorAmplitude,
                wavelength = WavyProgressIndicatorDefaults.LinearDeterminateWavelength,
                waveSpeed = WavyProgressIndicatorDefaults.LinearDeterminateWavelength
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PreviewStatChip(label = "Entries", value = "5")
                PreviewStatChip(label = "First", value = "08:30")
                PreviewStatChip(label = "Latest", value = "14:45")
            }
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
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Dark Mode Section
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Dark mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

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
                            Crossfade(
                                targetState = isSelected,
                                animationSpec = tween(300),
                                label = "darkModeToggleIcon_${preference.name}"
                            ) { selected ->
                                Icon(
                                    imageVector = when (preference) {
                                        DarkModePreference.SYSTEM -> if (selected) ImageVector.vectorResource(R.drawable.settings_filled) else ImageVector.vectorResource(R.drawable.settings)
                                        DarkModePreference.LIGHT -> if (selected) ImageVector.vectorResource(R.drawable.light_mode_filled) else ImageVector.vectorResource(R.drawable.light_mode)
                                        DarkModePreference.DARK -> if (selected) ImageVector.vectorResource(R.drawable.dark_mode_filled) else ImageVector.vectorResource(R.drawable.dark_mode)
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
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

        // Color Section
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Color",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (isDynamicColorAvailable) {
                val haptics = LocalHapticFeedback.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Crossfade(
                        targetState = themePreferences.colorSource == ColorSource.DYNAMIC_COLOR,
                        animationSpec = tween(400),
                        label = "paletteIcon"
                    ) { isDynamic ->
                        Icon(
                            imageVector = if (isDynamic) ImageVector.vectorResource(R.drawable.palette_filled) else ImageVector.vectorResource(R.drawable.palette),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
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
                            haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                            onColorSourceChange(
                                if (enabled) ColorSource.DYNAMIC_COLOR else ColorSource.HYDRO_THEME,
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

            val haptics = LocalHapticFeedback.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Crossfade(
                    targetState = themePreferences.usePureBlack,
                    animationSpec = tween(400),
                    label = "darkModeIcon"
                ) { isPureBlack ->
                    Icon(
                        imageVector = if (isPureBlack) ImageVector.vectorResource(R.drawable.dark_mode_filled) else ImageVector.vectorResource(R.drawable.dark_mode),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
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

@Composable
private fun PreviewStatChip(
    label: String,
    value: String
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

@Preview(showBackground = true)
@Composable
fun ThemePreviewCardPreview() {
    HydroTrackerTheme {
        ThemePreviewCard(themePreferences = ThemePreferences())
    }
}

@Preview(showBackground = true)
@Composable
fun AppearanceScreenWithAppBarPreview() {
    var previewPreferences by remember {
        mutableStateOf(ThemePreferences())
    }

    HydroTrackerTheme(themePreferences = previewPreferences) {
        AppearanceScreen(
            themePreferences = previewPreferences,
            isDynamicColorAvailable = true,
            onColorSourceChange = { source ->
                previewPreferences = previewPreferences.copy(colorSource = source)
            },
            onDarkModeChange = { mode ->
                previewPreferences = previewPreferences.copy(darkMode = mode)
            },
            onPureBlackChange = { enabled ->
                previewPreferences = previewPreferences.copy(usePureBlack = enabled)
            }
        )
    }
}

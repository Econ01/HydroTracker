package com.cemcakmak.hydrotracker.presentation.settings

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    paddingValues: PaddingValues = PaddingValues()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
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
                text = "Daily Progress",
                style = MaterialTheme.typography.headlineLargeEmphasized,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "1,250 / 2,500 ml",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
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

            Text(
                text = "Halfway there! Keep drinking.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.tabler_palette_filled),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
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
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.dark_mode),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
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

@Preview(showBackground = true)
@Composable
fun ThemePreviewCardPreview() {
    HydroTrackerTheme {
        ThemePreviewCard(themePreferences = ThemePreferences())
    }
}

@Preview(showBackground = true)
@Composable
fun AppearanceScreenPreview() {
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
            },
            paddingValues = PaddingValues()
        )
    }
}

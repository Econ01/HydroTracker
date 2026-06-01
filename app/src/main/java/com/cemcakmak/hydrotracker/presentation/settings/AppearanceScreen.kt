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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.Surface
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.AppFont
import com.cemcakmak.hydrotracker.data.models.ColorSource
import com.cemcakmak.hydrotracker.data.models.DarkModePreference
import com.cemcakmak.hydrotracker.data.models.NavBarLabelMode
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.ui.theme.fontFamilyFor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppearanceScreen(
    themePreferences: ThemePreferences = ThemePreferences(),
    isDynamicColorAvailable: Boolean = true,
    onColorSourceChange: (ColorSource) -> Unit = {},
    onDarkModeChange: (DarkModePreference) -> Unit = {},
    onPureBlackChange: (Boolean) -> Unit = {},
    onAppFontChange: (AppFont) -> Unit = {},
    onAutoHideNavBarChange: (Boolean) -> Unit = {},
    onNavBarLabelModeChange: (NavBarLabelMode) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    paddingValues: PaddingValues = PaddingValues()
) {
    var showFontSheet by remember { mutableStateOf(false) }
    var showLabelSheet by remember { mutableStateOf(false) }

    SettingsDetailScaffold(
        title = "Appearance",
        onNavigateBack = onNavigateBack,
        paddingValues = paddingValues
    ) {
        ThemePreviewCard(themePreferences = themePreferences)

        DarkModeSection(
            darkMode = themePreferences.darkMode,
            onDarkModeChange = onDarkModeChange
        )

        ColorSection(
            themePreferences = themePreferences,
            isDynamicColorAvailable = isDynamicColorAvailable,
            onColorSourceChange = onColorSourceChange,
            onPureBlackChange = onPureBlackChange
        )

        NavigationBarSection(
            autoHide = themePreferences.autoHideNavBar,
            onAutoHideChange = onAutoHideNavBarChange,
            labelMode = themePreferences.navBarLabelMode,
            onOpenLabelSheet = { showLabelSheet = true }
        )

        FontSection(
            selectedFont = themePreferences.appFont,
            onOpenFontSheet = { showFontSheet = true }
        )
    }

    if (showFontSheet) {
        FontBottomSheet(
            selectedFont = themePreferences.appFont,
            onAppFontChange = onAppFontChange,
            onDismiss = { showFontSheet = false }
        )
    }

    if (showLabelSheet) {
        NavLabelBottomSheet(
            selected = themePreferences.navBarLabelMode,
            onSelect = onNavBarLabelModeChange,
            onDismiss = { showLabelSheet = false }
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
private fun DarkModeSection(
    darkMode: DarkModePreference,
    onDarkModeChange: (DarkModePreference) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionHeader("Theme")
        val haptics = LocalHapticFeedback.current
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DarkModePreference.entries.forEach { preference ->
                val isSelected = darkMode == preference

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
                            animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
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
}

@Composable
private fun ColorSection(
    themePreferences: ThemePreferences,
    isDynamicColorAvailable: Boolean,
    onColorSourceChange: (ColorSource) -> Unit,
    onPureBlackChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionHeader("Color")
        Column {
            // Build the rows for this section; add another entry here to grow the list.
            val rows = buildList<@Composable () -> Unit> {
                if (isDynamicColorAvailable) {
                    add {
                        DynamicColorsRow(
                            themePreferences = themePreferences,
                            onColorSourceChange = onColorSourceChange
                        )
                    }
                }
                add {
                    AmoledRow(
                        themePreferences = themePreferences,
                        onPureBlackChange = onPureBlackChange
                    )
                }
            }
            rows.forEachIndexed { index, row ->
                SettingsGroupCard(index = index, size = rows.size) {
                    row()
                }
            }
        }
    }
}

@Composable
private fun DynamicColorsRow(
    themePreferences: ThemePreferences,
    onColorSourceChange: (ColorSource) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
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

@Composable
private fun AmoledRow(
    themePreferences: ThemePreferences,
    onPureBlackChange: (Boolean) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
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

@Composable
private fun FontSection(
    selectedFont: AppFont,
    onOpenFontSheet: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionHeader("Font")
        SettingsGroupCard(
            index = 0,
            size = 1,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                onOpenFontSheet()
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.font_filled),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = selectedFont.getDisplayName(),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = fontFamilyFor(selectedFont),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FontBottomSheet(
    selectedFont: AppFont,
    onAppFontChange: (AppFont) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val haptics = LocalHapticFeedback.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val fonts = AppFont.entries
            fonts.forEachIndexed { index, font ->
                SelectableOptionCard(
                    index = index,
                    size = fonts.size,
                    selected = font == selectedFont,
                    onClick = {
                        onAppFontChange(font)
                        haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    }
                ) { contentColor ->
                    Text(
                        text = font.getDisplayName(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = fontFamilyFor(font),
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationBarSection(
    autoHide: Boolean,
    onAutoHideChange: (Boolean) -> Unit,
    labelMode: NavBarLabelMode,
    onOpenLabelSheet: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionHeader("Navigation bar")
        Column {
            // Auto-hide on scroll (switch row)
            SettingsGroupCard(index = 0, size = 2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Crossfade(
                        targetState = autoHide,
                        animationSpec = tween(400),
                        label = "autoHideIcon"
                    ) { on ->
                        Icon(
                            imageVector = if (on) ImageVector.vectorResource(R.drawable.bottom_panel_close_filled) else ImageVector.vectorResource(R.drawable.bottom_panel_close),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-hide on scroll",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Hide the bar when scrolling down",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoHide,
                        onCheckedChange = { enabled ->
                            onAutoHideChange(enabled)
                            haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                        },
                        thumbContent = if (autoHide) {
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
            // Labels (opens a bottom sheet)
            SettingsGroupCard(
                index = 1,
                size = 2,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onOpenLabelSheet()
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.bottom_navigation_filled),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Labels",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = labelMode.getDisplayName(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavLabelBottomSheet(
    selected: NavBarLabelMode,
    onSelect: (NavBarLabelMode) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val haptics = LocalHapticFeedback.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val modes = NavBarLabelMode.entries
            modes.forEachIndexed { index, mode ->
                SelectableOptionCard(
                    index = index,
                    size = modes.size,
                    selected = mode == selected,
                    onClick = {
                        onSelect(mode)
                        haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    }
                ) { contentColor ->
                    Text(
                        text = mode.getDisplayName(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentColor
                    )
                }
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

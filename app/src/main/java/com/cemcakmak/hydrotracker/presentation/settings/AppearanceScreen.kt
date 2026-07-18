/*
 *
 *  * HydroTracker - A modern and private water intake tracking application
 *  * Copyright (c) 2026 Ali Cem Çakmak
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.cemcakmak.hydrotracker.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.AppFont
import com.cemcakmak.hydrotracker.data.models.ColorSource
import com.cemcakmak.hydrotracker.data.models.DarkModePreference
import com.cemcakmak.hydrotracker.data.models.NavBarLabelMode
import com.cemcakmak.hydrotracker.data.models.EdgeEffect
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.ui.theme.extendedColorScheme
import com.cemcakmak.hydrotracker.ui.theme.fontFamilyFor
import com.cemcakmak.hydrotracker.utils.OemFontWarning

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppearanceScreen(
    themePreferences: ThemePreferences = ThemePreferences(),
    wasPop: Boolean = false,
    isHapticsEnabled: Boolean = true,
    onHapticsEnabledChange: (Boolean) -> Unit = {},
    isDynamicColorAvailable: Boolean = true,
    onColorSourceChange: (ColorSource) -> Unit = {},
    onDarkModeChange: (DarkModePreference) -> Unit = {},
    onPureBlackChange: (Boolean) -> Unit = {},
    onAmoledBordersChange: (Boolean) -> Unit = {},
    onAppFontChange: (AppFont) -> Unit = {},
    onAutoHideNavBarChange: (Boolean) -> Unit = {},
    onNavBarLabelModeChange: (NavBarLabelMode) -> Unit = {},
    isBlurSupported: Boolean = true,
    onEdgeEffectChange: (EdgeEffect) -> Unit = {},
    onUseBeverageColorsChange: (Boolean) -> Unit = {},
    onNavigateToWidget: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    var showFontSheet by remember { mutableStateOf(false) }
    var showLabelSheet by remember { mutableStateOf(false) }
    var showEdgeEffectSheet by remember { mutableStateOf(false) }

    val isPreview = LocalInspectionMode.current
    val shouldApplyDepth = !isPreview && wasPop

    val blur by if (shouldApplyDepth) {
        val animatedContentScope = LocalNavAnimatedContentScope.current
        animatedContentScope.transition.animateDp(
            transitionSpec = { tween(400) },
            label = "quickAddEnterBlur"
        ) { state ->
            if (state == EnterExitState.PreEnter) 8.dp else 0.dp
        }
    } else {
        remember { mutableStateOf(0.dp) }
    }

    // Depth scrim that clears in sync with the blur as the page comes forward.
    val scrimAlpha by if (shouldApplyDepth) {
        val animatedContentScope = LocalNavAnimatedContentScope.current
        animatedContentScope.transition.animateFloat(
            transitionSpec = { tween(400) },
            label = "quickAddEnterScrim"
        ) { state ->
            if (state == EnterExitState.PreEnter) 0.4f else 0f
        }
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val scrimColor = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
        Color.White
    } else {
        Color.Black
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().blur(blur)) {
            SettingsDetailScaffold(
                title = stringResource(R.string.screen_appearance_title),
                onNavigateBack = onNavigateBack,
                themePreferences = themePreferences
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
                    onPureBlackChange = onPureBlackChange,
                    onAmoledBordersChange = onAmoledBordersChange
                )

                NavigationBarSection(
                    autoHide = themePreferences.autoHideNavBar,
                    onAutoHideChange = onAutoHideNavBarChange,
                    labelMode = themePreferences.navBarLabelMode,
                    onOpenLabelSheet = { showLabelSheet = true }
                )

                HomeScreenSection(
                    edgeEffect = themePreferences.edgeEffect,
                    isBlurSupported = isBlurSupported,
                    useBeverageColors = themePreferences.useBeverageColors,
                    onOpenEdgeEffectSheet = { showEdgeEffectSheet = true },
                    onUseBeverageColorsChange = onUseBeverageColorsChange
                )

                FontSection(
                    selectedFont = themePreferences.appFont,
                    onOpenFontSheet = { showFontSheet = true }
                )

                WidgetSection(onNavigateToWidget = onNavigateToWidget)

                FeedbackSection(
                    isHapticsEnabled = isHapticsEnabled,
                    onHapticsEnabledChange = onHapticsEnabledChange
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

            if (showEdgeEffectSheet) {
                EdgeEffectBottomSheet(
                    selected = themePreferences.edgeEffect,
                    isBlurSupported = isBlurSupported,
                    onSelect = onEdgeEffectChange,
                    onDismiss = { showEdgeEffectSheet = false }
                )
            }
        }

        if (scrimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.3f)
                    .background(scrimColor.copy(alpha = scrimAlpha))
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
                text = stringResource(R.string.appearance_preview_amount),
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
                PreviewStatChip(label = stringResource(R.string.appearance_preview_entries), value = "5")
                PreviewStatChip(label = stringResource(R.string.appearance_preview_first), value = "08:30")
                PreviewStatChip(label = stringResource(R.string.appearance_preview_latest), value = "14:45")
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
        SettingsSectionHeader(stringResource(R.string.appearance_theme_header))
        val haptics = LocalHapticFeedback.current
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DarkModePreference.entries.forEach { preference ->
                val isSelected = darkMode == preference

                ToggleButton(
                    checked = isSelected,
                    onCheckedChange = { enabled ->
                        onDarkModeChange(preference)

                        val hapticType = if (enabled) {
                            HapticFeedbackType.ToggleOn
                        } else {
                            HapticFeedbackType.ToggleOff
                        }
                        haptics.performHapticFeedback(hapticType)
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
                                DarkModePreference.SYSTEM -> stringResource(R.string.appearance_theme_system)
                                DarkModePreference.LIGHT -> stringResource(R.string.appearance_theme_light)
                                DarkModePreference.DARK -> stringResource(R.string.appearance_theme_dark)
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ColorSection(
    themePreferences: ThemePreferences,
    isDynamicColorAvailable: Boolean,
    onColorSourceChange: (ColorSource) -> Unit,
    onPureBlackChange: (Boolean) -> Unit,
    onAmoledBordersChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionHeader(stringResource(R.string.appearance_color_header))
        Column {
            val showBordersRow = themePreferences.usePureBlack
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
            val totalSize = rows.size + if (showBordersRow) 1 else 0
            rows.forEachIndexed { index, row ->
                SettingsGroupCard(index = index, size = totalSize) {
                    row()
                }
            }
            AnimatedVisibility(
                visible = showBordersRow,
                enter = fadeIn(animationSpec = MaterialTheme.motionScheme.slowSpatialSpec()) +
                        expandVertically(animationSpec = MaterialTheme.motionScheme.slowSpatialSpec()),
                exit = fadeOut(animationSpec = MaterialTheme.motionScheme.slowSpatialSpec()) +
                        shrinkVertically(animationSpec = MaterialTheme.motionScheme.slowSpatialSpec())
            ) {
                SettingsGroupCard(index = totalSize - 1, size = totalSize) {
                    AmoledBordersRow(
                        themePreferences = themePreferences,
                        onAmoledBordersChange = onAmoledBordersChange
                    )
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
                text = stringResource(R.string.color_source_dynamic),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.color_source_dynamic_desc),
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
                        imageVector = ImageVector.vectorResource(R.drawable.check_filled),
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
                text = stringResource(R.string.appearance_amoled_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.appearance_amoled_desc),
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
                        imageVector = ImageVector.vectorResource(R.drawable.check_filled),
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
private fun AmoledBordersRow(
    themePreferences: ThemePreferences,
    onAmoledBordersChange: (Boolean) -> Unit
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
            targetState = themePreferences.showAmoledBorders,
            animationSpec = tween(400),
            label = "amoledBordersIcon"
        ) { showBorders ->
            Icon(
                imageVector = if (showBorders) ImageVector.vectorResource(R.drawable.border_outer_fill) else ImageVector.vectorResource(R.drawable.border_outer),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(R.string.appearance_amoled_borders_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.appearance_amoled_borders_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = themePreferences.showAmoledBorders,
            onCheckedChange = { enabled ->
                onAmoledBordersChange(enabled)

                val hapticType = if (enabled) {
                    HapticFeedbackType.ToggleOn
                } else {
                    HapticFeedbackType.ToggleOff
                }
                haptics.performHapticFeedback(hapticType)
            },
            thumbContent = if (themePreferences.showAmoledBorders) {
                {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.check_filled),
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
private fun NavigationBarSection(
    autoHide: Boolean,
    onAutoHideChange: (Boolean) -> Unit,
    labelMode: NavBarLabelMode,
    onOpenLabelSheet: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionHeader(stringResource(R.string.appearance_navbar_header))
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
                            text = stringResource(R.string.appearance_autohide_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.appearance_autohide_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoHide,
                        onCheckedChange = { enabled ->
                            onAutoHideChange(enabled)

                            val hapticType = if (enabled) {
                                HapticFeedbackType.ToggleOn
                            } else {
                                HapticFeedbackType.ToggleOff
                            }
                            haptics.performHapticFeedback(hapticType)
                        },
                        thumbContent = if (autoHide) {
                            {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.check_filled),
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
                            text = stringResource(R.string.appearance_labels_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(labelMode.labelResId),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.keyboard_arrow_up_filled),
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
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
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
                        text = stringResource(mode.labelResId),
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreenSection(
    edgeEffect: EdgeEffect,
    isBlurSupported: Boolean,
    useBeverageColors: Boolean,
    onOpenEdgeEffectSheet: () -> Unit,
    onUseBeverageColorsChange: (Boolean) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    // Devices that can't blur never show the Blurred option, so a stored BLURRED value is
    // surfaced (and rendered) as Transparent here.
    val effective = if (edgeEffect == EdgeEffect.BLURRED && !isBlurSupported) {
        EdgeEffect.TRANSPARENT
    } else {
        edgeEffect
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionHeader(stringResource(R.string.appearance_home_header))

        Column {
            SettingsGroupCard(
                index = 0,
                size = 2
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Crossfade(
                        targetState = useBeverageColors,
                        animationSpec = tween(400),
                        label = "beverageColorsIcon"
                    ) { on ->
                        Icon(
                            imageVector = if (on) {
                                ImageVector.vectorResource(R.drawable.colors_filled)
                            } else {
                                ImageVector.vectorResource(R.drawable.colors)
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.appearance_beverage_colors_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.appearance_beverage_colors_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = useBeverageColors,
                        onCheckedChange = { enabled ->
                            onUseBeverageColorsChange(enabled)
                            val hapticType = if (enabled) {
                                HapticFeedbackType.ToggleOn
                            } else {
                                HapticFeedbackType.ToggleOff
                            }
                            haptics.performHapticFeedback(hapticType)
                        },
                        thumbContent = if (useBeverageColors) {
                            {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.check_filled),
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

            SettingsGroupCard(
                index = 1,
                size = 2,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onOpenEdgeEffectSheet()
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
                        imageVector = ImageVector.vectorResource(R.drawable.blur_on_filled),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.appearance_edge_effect_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(effective.labelResId),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.keyboard_arrow_up_filled),
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
private fun EdgeEffectBottomSheet(
    selected: EdgeEffect,
    isBlurSupported: Boolean,
    onSelect: (EdgeEffect) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    val haptics = LocalHapticFeedback.current
    // Hide Blurred where the device can't render it; treat a stored BLURRED as Transparent.
    val options = EdgeEffect.entries.filter { isBlurSupported || it != EdgeEffect.BLURRED }
    val effectiveSelected = if (selected == EdgeEffect.BLURRED && !isBlurSupported) {
        EdgeEffect.TRANSPARENT
    } else {
        selected
    }
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
            options.forEachIndexed { index, mode ->
                SelectableOptionCard(
                    index = index,
                    size = options.size,
                    selected = mode == effectiveSelected,
                    onClick = {
                        onSelect(mode)
                        haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    }
                ) { contentColor ->
                    Text(
                        text = stringResource(mode.labelResId),
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedbackSection(
    isHapticsEnabled: Boolean,
    onHapticsEnabledChange: (Boolean) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionHeader(stringResource(R.string.appearance_feedback_header))
        SettingsGroupCard(index = 0, size = 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Crossfade(
                    targetState = isHapticsEnabled,
                    animationSpec = tween(400),
                    label = "hapticsIcon"
                ) { on ->
                    Icon(
                        imageVector = if (on) ImageVector.vectorResource(R.drawable.mobile_vibrate_filled) else ImageVector.vectorResource(R.drawable.mobile_vibrate),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.appearance_haptics_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.appearance_haptics_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isHapticsEnabled,
                    onCheckedChange = { enabled ->
                        onHapticsEnabledChange(enabled)

                        val hapticType = if (enabled) {
                            HapticFeedbackType.ToggleOn
                        } else {
                            HapticFeedbackType.ToggleOff
                        }
                        haptics.performHapticFeedback(hapticType)
                    },
                    thumbContent = if (isHapticsEnabled) {
                        {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.check_filled),
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
private fun WidgetSection(onNavigateToWidget: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionHeader(stringResource(R.string.appearance_widget_section_title))
        SettingsGroupCard(
            index = 0,
            size = 1,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                onNavigateToWidget()
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
                    imageVector = ImageVector.vectorResource(R.drawable.widgets_filled),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.appearance_widget_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.appearance_widget_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.keyboard_arrow_right_filled),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FontSection(
    selectedFont: AppFont,
    onOpenFontSheet: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionHeader(stringResource(R.string.appearance_font_header))
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
                    text = stringResource(selectedFont.labelResId),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = fontFamilyFor(selectedFont),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.keyboard_arrow_up_filled),
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
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
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
            if (OemFontWarning.isAffectedDevice) {
                Text(
                    text = stringResource(R.string.font_xiaomi_warning),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.extendedColorScheme.warning,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

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
                        text = stringResource(font.labelResId),
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
            style = MaterialTheme.typography.labelLargeEmphasized,
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
            },
            onAmoledBordersChange = { enabled ->
                previewPreferences = previewPreferences.copy(showAmoledBorders = enabled)
            },
            onEdgeEffectChange = { style ->
                previewPreferences = previewPreferences.copy(edgeEffect = style)
            },
            onUseBeverageColorsChange = { enabled ->
                previewPreferences = previewPreferences.copy(useBeverageColors = enabled)
            }
        )
    }
}

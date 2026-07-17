package com.cemcakmak.hydrotracker.presentation.settings

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.models.WidgetPreferences
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme

@Composable
fun WidgetSettingsScreen(
    themePreferences: ThemePreferences = ThemePreferences(),
    wasPop: Boolean = false,
    widgetPreferences: WidgetPreferences = WidgetPreferences(),
    isDynamicColorAvailable: Boolean = true,
    onWidgetPreferencesChange: (WidgetPreferences) -> Unit = {},
    onNavigateToQuickAdd: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
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
                title = stringResource(R.string.appearance_widget_title),
                onNavigateBack = onNavigateBack,
                themePreferences = themePreferences
            ) {
                Column(
                    modifier = Modifier.padding(top = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (isDynamicColorAvailable) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SettingsSectionHeader(stringResource(R.string.widget_settings_colours_header))
                            SettingsGroupCard(index = 0, size = 1) {
                                WidgetSwitchRow(
                                    iconResOn = R.drawable.palette_filled,
                                    iconResOff = R.drawable.palette,
                                    title = stringResource(R.string.widget_settings_dynamic_title),
                                    description = stringResource(R.string.widget_settings_dynamic_desc),
                                    checked = widgetPreferences.useDynamicColors,
                                    onCheckedChange = {
                                        onWidgetPreferencesChange(widgetPreferences.copy(useDynamicColors = it))
                                    }
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingsSectionHeader(stringResource(R.string.widget_settings_background_header))
                        Column {
                            val transparent = widgetPreferences.useTransparentBackground
                            val visibleRows = if (transparent) 1 else 3
                            SettingsGroupCard(index = 0, size = visibleRows) {
                                WidgetSwitchRow(
                                    iconResOn = R.drawable.blur_on_filled,
                                    iconResOff = R.drawable.blur_on,
                                    title = stringResource(R.string.widget_settings_transparent_title),
                                    description = stringResource(R.string.widget_settings_transparent_desc),
                                    checked = transparent,
                                    onCheckedChange = {
                                        onWidgetPreferencesChange(widgetPreferences.copy(useTransparentBackground = it))
                                    }
                                )
                            }
                            // Transparent overrides the per-mode surfaces: hide both while it is on.
                            AnimatedVisibility(
                                visible = !transparent,
                                enter = fadeIn(animationSpec = MaterialTheme.motionScheme.slowSpatialSpec()) +
                                        expandVertically(animationSpec = MaterialTheme.motionScheme.slowSpatialSpec()),
                                exit = fadeOut(animationSpec = MaterialTheme.motionScheme.slowSpatialSpec()) +
                                        shrinkVertically(animationSpec = MaterialTheme.motionScheme.slowSpatialSpec())
                            ) {
                                Column {
                                    SettingsGroupCard(index = 1, size = visibleRows) {
                                        WidgetSwitchRow(
                                            iconResOn = R.drawable.dark_mode_filled,
                                            iconResOff = R.drawable.dark_mode,
                                            title = stringResource(R.string.widget_settings_pure_black_title),
                                            description = stringResource(R.string.widget_settings_pure_black_desc),
                                            checked = widgetPreferences.usePureBlack,
                                            onCheckedChange = {
                                                onWidgetPreferencesChange(widgetPreferences.copy(usePureBlack = it))
                                            }
                                        )
                                    }
                                    SettingsGroupCard(index = 2, size = visibleRows) {
                                        WidgetSwitchRow(
                                            iconResOn = R.drawable.light_mode_filled,
                                            iconResOff = R.drawable.light_mode,
                                            title = stringResource(R.string.widget_settings_pure_white_title),
                                            description = stringResource(R.string.widget_settings_pure_white_desc),
                                            checked = widgetPreferences.usePureWhite,
                                            onCheckedChange = {
                                                onWidgetPreferencesChange(widgetPreferences.copy(usePureWhite = it))
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingsSectionHeader(stringResource(R.string.widget_settings_quickadd_title))
                        val haptics = LocalHapticFeedback.current
                        SettingsGroupCard(
                            index = 0,
                            size = 1,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                onNavigateToQuickAdd()
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
                                    imageVector = ImageVector.vectorResource(R.drawable.tune_filled),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.widget_settings_quickadd_title),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = stringResource(R.string.widget_settings_quickadd_desc),
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

@Composable
private fun WidgetSwitchRow(
    @DrawableRes iconResOn: Int,
    @DrawableRes iconResOff: Int,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
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
            targetState = checked,
            animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
            label = "paletteIcon"
        ) { isChecked ->
            Icon(
                imageVector = if (isChecked) ImageVector.vectorResource(iconResOn) else ImageVector.vectorResource(iconResOff),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = { enabled ->
                onCheckedChange(enabled)
                haptics.performHapticFeedback(
                    if (enabled) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
                )
            },
            thumbContent = if (checked) {
                {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.check_filled),
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                }
            } else null
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WidgetSettingsScreenPreview() {
    HydroTrackerTheme {
        WidgetSettingsScreen()
    }
}

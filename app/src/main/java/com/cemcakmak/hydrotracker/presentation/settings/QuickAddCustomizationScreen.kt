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

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.EmojiFoodBeverage
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddCustomizationScreen(
    themePreferences: ThemePreferences = ThemePreferences(),
    wasPop: Boolean = false,
    onNavigateToContainerPresets: () -> Unit = {},
    onNavigateToBeverageTypes: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
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
                title = stringResource(R.string.screen_quickadd_title),
                onNavigateBack = onNavigateBack,
                themePreferences = themePreferences
            ) {
                Column(
                    modifier = Modifier.padding(top = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SettingsSectionHeader(stringResource(R.string.quickadd_containers_header))
                        Text(
                            text = stringResource(R.string.quickadd_containers_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    QuickAddNavRow(
                        icon = Icons.Default.LocalDrink,
                        label = stringResource(R.string.quickadd_edit_containers),
                        onClick = onNavigateToContainerPresets
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SettingsSectionHeader(stringResource(R.string.quickadd_beverages_header))
                        Text(
                            text = stringResource(R.string.quickadd_beverages_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    QuickAddNavRow(
                        icon = Icons.Default.EmojiFoodBeverage,
                        label = stringResource(R.string.quickadd_edit_beverages),
                        onClick = onNavigateToBeverageTypes
                    )
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
private fun QuickAddNavRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    SettingsGroupCard(
        index = 0,
        size = 1,
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
            onClick()
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
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
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

@Preview(showBackground = true)
@Composable
fun QuickAddCustomizationScreenPreview() {
    HydroTrackerTheme {
        QuickAddCustomizationScreen()
    }
}

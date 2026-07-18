/*
 *
 *  * HydroTracker - A modern and private water intake tracking application
 *  * Copyright (c) 2026 Ali Cem Çakmak
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.cemcakmak.hydrotracker.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.animation.core.Spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.presentation.common.BlurMorph
import com.cemcakmak.hydrotracker.presentation.common.RollDirection
import com.cemcakmak.hydrotracker.presentation.common.RollingEffects
import com.cemcakmak.hydrotracker.presentation.common.RollingNumberConfig
import com.cemcakmak.hydrotracker.presentation.common.RollingNumberText
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import java.util.Locale

/**
 * Developer screen for testing custom motion components in isolation.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AnimationMotionLabScreen(
    themePreferences: ThemePreferences = ThemePreferences(),
    onNavigateBack: () -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current

    var rollingValue by remember { mutableDoubleStateOf(1250.0) }
    var rollingDirection by remember { mutableStateOf(RollDirection.Auto) }
    var rollingEffects by remember { mutableStateOf(RollingEffects.Full) }
    var rollingHaptics by remember { mutableStateOf(false) }
    var animationConfig by remember { mutableStateOf(RollingNumberConfig()) }

    val blurPhrases = remember {
        listOf(
            "Every 30 minutes",
            "Every 2 hours",
            "Only when behind goal",
            "Every 45 minutes",
            "Goal reached!"
        )
    }
    var blurIndex by remember { mutableIntStateOf(0) }

    fun triggerHaptic() {
        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
    }

    SettingsDetailScaffold(
        title = "Animation Motion Lab",
        onNavigateBack = onNavigateBack,
        themePreferences = themePreferences
    ) {
        // Rolling Number section
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsSectionHeader("Rolling Number")

            Column {
                SettingsGroupCard(index = 0, size = 3) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RollingNumberText(
                            targetValue = rollingValue,
                            formatValue = { value -> "${value.toInt()} of 2000 ml" },
                            style = MaterialTheme.typography.displaySmallEmphasized,
                            direction = rollingDirection,
                            effects = rollingEffects,
                            hapticsEnabled = rollingHaptics,
                            animationConfig = animationConfig
                        )
                    }
                }

                SettingsGroupCard(index = 1, size = 3) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Value triggers",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(-100, -10, -1).forEach { step ->
                                    FilledTonalButton(
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            triggerHaptic()
                                            rollingValue = (rollingValue + step).coerceAtLeast(0.0)
                                        }
                                    ) {
                                        Text(
                                            text = if (step > 0) "+$step" else "$step",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(+1, +10, +100).forEach { step ->
                                    FilledTonalButton(
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            triggerHaptic()
                                            rollingValue = (rollingValue + step).coerceAtLeast(0.0)
                                        }
                                    ) {
                                        Text(
                                            text = if (step > 0) "+$step" else "$step",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(9.0, 10.0, 99.0).forEach { value ->
                                    FilledTonalButton(
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            triggerHaptic()
                                            rollingValue = value
                                        }
                                    ) {
                                        Text(
                                            text = value.toInt().toString(),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(100.0, 999.0, 1000.0).forEach { value ->
                                    FilledTonalButton(
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            triggerHaptic()
                                            rollingValue = value
                                        }
                                    ) {
                                        Text(
                                            text = value.toInt().toString(),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider()

                        Text(
                            text = "Animation tuning",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        TuningSlider(
                            label = "Rapid-change threshold",
                            value = animationConfig.rapidChangeThresholdMs.toFloat(),
                            valueRange = 0f..1000f,
                            steps = 9,
                            onValueChange = {
                                animationConfig = animationConfig.copy(rapidChangeThresholdMs = it.toInt())
                            },
                            valueFormatter = { "${it.toInt()} ms" },
                            onReset = {
                                animationConfig = animationConfig.copy(rapidChangeThresholdMs = 0)
                            }
                        )

                        TuningSlider(
                            label = "Transition scale",
                            value = animationConfig.transitionScale,
                            valueRange = 0f..1.5f,
                            steps = 30,
                            onValueChange = {
                                animationConfig = animationConfig.copy(transitionScale = it)
                            },
                            valueFormatter = { String.format(Locale.ROOT, "%.2f", it) },
                            onReset = {
                                animationConfig = animationConfig.copy(transitionScale = 0.4f)
                            }
                        )

                        TuningSlider(
                            label = "Blur radius",
                            value = animationConfig.blurRadius.value,
                            valueRange = 0f..20f,
                            steps = 19,
                            onValueChange = {
                                animationConfig = animationConfig.copy(blurRadius = it.dp)
                            },
                            valueFormatter = { "${it.toInt()} dp" },
                            onReset = {
                                animationConfig = animationConfig.copy(blurRadius = 6.dp)
                            }
                        )

                        SpringSlider(
                            label = "Slide spring",
                            dampingRatio = animationConfig.slideDampingRatio,
                            stiffness = animationConfig.slideStiffness,
                            onDampingChange = {
                                animationConfig = animationConfig.copy(slideDampingRatio = it)
                            },
                            onStiffnessChange = {
                                animationConfig = animationConfig.copy(slideStiffness = it)
                            },
                            onReset = {
                                animationConfig = animationConfig.copy(
                                    slideDampingRatio = 0.7f,
                                    slideStiffness = Spring.StiffnessMediumLow
                                )
                            }
                        )

                        SpringSlider(
                            label = "Scale spring",
                            dampingRatio = animationConfig.scaleDampingRatio,
                            stiffness = animationConfig.scaleStiffness,
                            onDampingChange = {
                                animationConfig = animationConfig.copy(scaleDampingRatio = it)
                            },
                            onStiffnessChange = {
                                animationConfig = animationConfig.copy(scaleStiffness = it)
                            },
                            onReset = {
                                animationConfig = animationConfig.copy(
                                    scaleDampingRatio = 0.7f,
                                    scaleStiffness = Spring.StiffnessMedium
                                )
                            }
                        )

                        SpringSlider(
                            label = "Blur spring",
                            dampingRatio = animationConfig.blurDampingRatio,
                            stiffness = animationConfig.blurStiffness,
                            stiffnessRange = 0f..5000f,
                            stiffnessSteps = 50,
                            onDampingChange = {
                                animationConfig = animationConfig.copy(blurDampingRatio = it)
                            },
                            onStiffnessChange = {
                                animationConfig = animationConfig.copy(blurStiffness = it.coerceAtLeast(100f))
                            },
                            onReset = {
                                animationConfig = animationConfig.copy(
                                    blurDampingRatio = Spring.DampingRatioNoBouncy,
                                    blurStiffness = Spring.StiffnessHigh
                                )
                            }
                        )

                        SpringSlider(
                            label = "Fade spring",
                            dampingRatio = animationConfig.fadeDampingRatio,
                            stiffness = animationConfig.fadeStiffness,
                            onDampingChange = {
                                animationConfig = animationConfig.copy(fadeDampingRatio = it)
                            },
                            onStiffnessChange = {
                                animationConfig = animationConfig.copy(fadeStiffness = it)
                            },
                            onReset = {
                                animationConfig = animationConfig.copy(
                                    fadeDampingRatio = Spring.DampingRatioNoBouncy,
                                    fadeStiffness = Spring.StiffnessMedium
                                )
                            }
                        )

                        TuningSlider(
                            label = "Rotation angle",
                            value = animationConfig.rotationAngle,
                            valueRange = 0f..90f,
                            steps = 89,
                            onValueChange = {
                                animationConfig = animationConfig.copy(rotationAngle = it)
                            },
                            valueFormatter = { "${it.toInt()}°" },
                            onReset = {
                                animationConfig = animationConfig.copy(rotationAngle = 0f)
                            }
                        )

                        TuningSlider(
                            label = "Camera distance",
                            value = animationConfig.cameraDistance,
                            valueRange = 1f..64f,
                            steps = 63,
                            onValueChange = {
                                animationConfig = animationConfig.copy(cameraDistance = it)
                            },
                            valueFormatter = { String.format(Locale.ROOT, "%.1f", it) },
                            onReset = {
                                animationConfig = animationConfig.copy(cameraDistance = 8f)
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    triggerHaptic()
                                    rollingValue = (0..2000).random().toDouble()
                                }
                            ) {
                                Text(text = "Random", style = MaterialTheme.typography.labelMedium)
                            }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    triggerHaptic()
                                    rollingValue = 0.0
                                }
                            ) {
                                Text(text = "Reset", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        FilledTonalButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                triggerHaptic()
                                animationConfig = RollingNumberConfig()
                            }
                        ) {
                            Text(text = "Reset tuning", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                SettingsGroupCard(index = 2, size = 3) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Direction",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RollDirection.entries.forEach { direction ->
                                    val selected = rollingDirection == direction
                                    ToggleButton(
                                        checked = selected,
                                        onCheckedChange = {
                                            triggerHaptic()
                                            rollingDirection = direction
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = direction.name,
                                            style = if (selected) {
                                                MaterialTheme.typography.labelLargeEmphasized
                                            } else {
                                                MaterialTheme.typography.labelLarge
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Effects",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RollingEffects.entries.forEach { effect ->
                                    val selected = rollingEffects == effect
                                    ToggleButton(
                                        checked = selected,
                                        onCheckedChange = {
                                            triggerHaptic()
                                            rollingEffects = effect
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = effect.name,
                                            style = if (selected) {
                                                MaterialTheme.typography.labelLargeEmphasized
                                            } else {
                                                MaterialTheme.typography.labelLarge
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Haptics",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Switch(
                                checked = rollingHaptics,
                                onCheckedChange = {
                                    triggerHaptic()
                                    rollingHaptics = it
                                }
                            )
                        }
            }
                }
            }
        }

        // Blur Morph section
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsSectionHeader("Blur Morph")

            Column {
                SettingsGroupCard(index = 0, size = 2) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BlurMorph(targetState = blurPhrases[blurIndex]) { text, blurModifier ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = text,
                                    textAlign = TextAlign.Center,
                                    modifier = blurModifier,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            }
                        }
                    }
                }

                SettingsGroupCard(index = 1, size = 2) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {

                        Text(
                            text = "Preset phrases",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    triggerHaptic()
                                    blurIndex = (blurIndex - 1).mod(blurPhrases.size)
                                }
                            ) {
                                Text(text = "Previous", style = MaterialTheme.typography.labelMedium)
                            }
                            FilledTonalButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    triggerHaptic()
                                    blurIndex = (blurIndex + 1).mod(blurPhrases.size)
                                }
                            ) {
                                Text(text = "Next", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        blurPhrases.forEachIndexed { index, phrase ->
                            FilledTonalButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    triggerHaptic()
                                    blurIndex = index
                                }
                            ) {
                                Text(text = phrase, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TuningSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueFormatter: (Float) -> String,
    steps: Int = 0,
    onReset: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = valueFormatter(value),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (onReset != null) {
                    TextButton(
                        onClick = onReset,
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Text(
                            text = "Reset",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
        Slider(
            value = value,
            valueRange = valueRange,
            steps = steps,
            onValueChange = onValueChange
        )
    }
}

@Composable
private fun SpringSlider(
    label: String,
    dampingRatio: Float,
    stiffness: Float,
    stiffnessRange: ClosedFloatingPointRange<Float> = 100f..1500f,
    dampingSteps: Int = 14,
    stiffnessSteps: Int = 14,
    onDampingChange: (Float) -> Unit,
    onStiffnessChange: (Float) -> Unit,
    onReset: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            if (onReset != null) {
                TextButton(
                    onClick = onReset,
                    modifier = Modifier.padding(0.dp)
                ) {
                    Text(
                        text = "Reset",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Damping",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = dampingFlag(dampingRatio) ?: String.format(Locale.ROOT, "%.2f", dampingRatio),
                style = MaterialTheme.typography.bodySmall,
                color = if (dampingFlag(dampingRatio) != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        Slider(
            value = dampingRatio,
            valueRange = 0.1f..1.5f,
            steps = dampingSteps,
            onValueChange = onDampingChange
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Stiffness",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stiffnessFlag(stiffness) ?: String.format(Locale.ROOT, "%.0f", stiffness),
                style = MaterialTheme.typography.bodySmall,
                color = if (stiffnessFlag(stiffness) != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        Slider(
            value = stiffness,
            valueRange = stiffnessRange,
            steps = stiffnessSteps,
            onValueChange = onStiffnessChange
        )
    }
}

private fun stiffnessFlag(value: Float): String? = when {
    kotlin.math.abs(value - Spring.StiffnessVeryLow) < 10f -> "VeryLow"
    kotlin.math.abs(value - Spring.StiffnessLow) < 10f -> "Low"
    kotlin.math.abs(value - Spring.StiffnessMediumLow) < 10f -> "MediumLow"
    kotlin.math.abs(value - Spring.StiffnessMedium) < 10f -> "Medium"
    kotlin.math.abs(value - Spring.StiffnessHigh) < 10f -> "High"
    else -> null
}

private fun dampingFlag(value: Float): String? = when {
    kotlin.math.abs(value - Spring.DampingRatioNoBouncy) < 0.02f -> "NoBouncy"
    kotlin.math.abs(value - Spring.DampingRatioLowBouncy) < 0.02f -> "LowBouncy"
    kotlin.math.abs(value - Spring.DampingRatioMediumBouncy) < 0.02f -> "MediumBouncy"
    kotlin.math.abs(value - Spring.DampingRatioHighBouncy) < 0.02f -> "HighBouncy"
    else -> null
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview(showBackground = true, name = "Animation Motion Lab")
@Composable
private fun AnimationMotionLabScreenPreview() {
    HydroTrackerTheme {
        AnimationMotionLabScreen()
    }
}

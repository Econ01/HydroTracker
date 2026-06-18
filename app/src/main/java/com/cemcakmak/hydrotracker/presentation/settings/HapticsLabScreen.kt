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

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.VibratorManager
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.data.models.DarkModePreference
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.utils.PrimitiveStep
import com.cemcakmak.hydrotracker.utils.SmartHaptics
import kotlin.math.roundToInt

/**
 * Developer-only lab for designing primitive compositions live on-device. Lets you add any primitive,
 * adjust its intensity (scale) and the delay before it, chain several together, play the whole chain
 * (or a single step), and copy the result as Kotlin so a dialed-in recipe can be pasted straight into
 * [SmartHaptics.toPrimitiveComposition]. Requires API 31+ and a vibrator with composition support.
 */
@SuppressLint("NewApi")
@Composable
fun HapticsLabScreen(
    themePreferences: ThemePreferences = ThemePreferences(),
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            null
        }
    }

    val supported: Map<LabPrimitive, Boolean> = remember {
        if (vibrator != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            LabPrimitive.entries.associateWith { p ->
                vibrator.arePrimitivesSupported(p.id).firstOrNull() == true
            }
        } else {
            LabPrimitive.entries.associateWith { false }
        }
    }

    val durations: Map<LabPrimitive, Int> = remember {
        if (vibrator != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            LabPrimitive.entries.associateWith { p ->
                vibrator.getPrimitiveDurations(p.id).firstOrNull() ?: 0
            }
        } else {
            LabPrimitive.entries.associateWith { 0 }
        }
    }

    val anySupported = supported.values.any { it }
    val steps = remember { mutableStateListOf<LabStep>() }

    fun playAll() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && steps.isNotEmpty()) {
            SmartHaptics.playPrimitiveSteps(
                context,
                steps.map { PrimitiveStep(it.primitive.id, it.scale, it.delayMs) }
            )
        }
    }

    fun playOne(step: LabStep) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SmartHaptics.playPrimitiveSteps(
                context,
                listOf(PrimitiveStep(step.primitive.id, step.scale, 0))
            )
        }
    }

    fun copyAsKotlin() {
        val code = buildString {
            append("VibrationEffect.startComposition()")
            steps.forEach { s ->
                val scaleStr = "%.2f".format(s.scale) + "f"
                append("\n    .addPrimitive(VibrationEffect.Composition.${s.primitive.constName}, $scaleStr")
                if (s.delayMs > 0) append(", ${s.delayMs}")
                append(")")
            }
            append("\n    .compose()")
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("haptic composition", code))
        Log.i("HapticsLab", code)
        Toast.makeText(context, "Copied composition to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun moveUp(i: Int) {
        if (i > 0) {
            val tmp = steps[i - 1]
            steps[i - 1] = steps[i]
            steps[i] = tmp
        }
    }

    fun moveDown(i: Int) {
        if (i < steps.lastIndex) {
            val tmp = steps[i + 1]
            steps[i + 1] = steps[i]
            steps[i] = tmp
        }
    }

    SettingsDetailScaffold(
        title = "Haptics Primitive Lab",
        onNavigateBack = onNavigateBack
    ) {
        LabDeviceInfoCard(themePreferences = themePreferences, anySupported = anySupported)

        if (!anySupported) {
            SettingsGroupCard(index = 0, size = 1) {
                Text(
                    text = "Primitive compositions need Android 12+ and a vibrator with composition " +
                        "support. This device (API ${Build.VERSION.SDK_INT}) can't preview them.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
            return@SettingsDetailScaffold
        }

        // Palette: tap a primitive to append it as a step. Two per row, unsupported ones disabled.
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsSectionHeader("Add primitive")
            LabPrimitive.entries.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { p ->
                        val isSupported = supported[p] == true
                        FilledTonalButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                steps.add(LabStep(p))
                            },
                            enabled = isSupported,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isSupported) p.label else "${p.label} (n/a)")
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Composition: the ordered chain of steps.
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsSectionHeader("Composition (${steps.size})")
            if (steps.isEmpty()) {
                SettingsGroupCard(index = 0, size = 1) {
                    Text(
                        text = "No steps yet. Tap a primitive above to add it, then tune intensity " +
                            "and delay.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            } else {
                steps.forEachIndexed { index, step ->
                    SettingsGroupCard(index = index, size = steps.size) {
                        StepCardContent(
                            position = index + 1,
                            step = step,
                            durationMs = durations[step.primitive] ?: 0,
                            canMoveUp = index > 0,
                            canMoveDown = index < steps.lastIndex,
                            onPlay = { playOne(step) },
                            onRemove = { steps.removeAt(index) },
                            onMoveUp = { moveUp(index) },
                            onMoveDown = { moveDown(index) },
                            onScaleChange = { newScale ->
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                step.scale = newScale
                            },
                            onDelayChange = { newDelay ->
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                step.delayMs = newDelay
                            }
                        )
                    }
                }
            }
        }

        // Actions
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsSectionHeader("Actions")
            val totalMs = steps.sumOf { (durations[it.primitive] ?: 0) + it.delayMs }
            Text(
                text = "Estimated duration: $totalMs ms",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        playAll()
                    },
                    enabled = steps.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Play")
                }
                OutlinedButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        steps.clear()
                    },
                    enabled = steps.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear")
                }
            }
            TextButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    copyAsKotlin()
                },
                enabled = steps.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Copy as Kotlin")
            }
        }
    }
}

@Composable
private fun LabDeviceInfoCard(
    themePreferences: ThemePreferences,
    anySupported: Boolean
) {
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
        shape = RoundedCornerShape(30.dp),
        tonalElevation = 2.dp,
        border = border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            InfoRow("API level", Build.VERSION.SDK_INT.toString())
            HorizontalDivider()
            InfoRow("Primitive support", if (anySupported) "Yes" else "No")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StepCardContent(
    position: Int,
    step: LabStep,
    durationMs: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onScaleChange: (Float) -> Unit,
    onDelayChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$position. ${step.primitive.label}",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Hardware duration: $durationMs ms",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Intensity: ${"%.2f".format(step.scale)}",
            style = MaterialTheme.typography.bodySmall
        )
        Slider(
            value = step.scale,
            onValueChange = { raw ->
                val snapped = ((raw * 20f).roundToInt() / 20f).coerceIn(0f, 1f)
                if (snapped != step.scale) onScaleChange(snapped)
            },
            valueRange = 0f..1f
        )

        Text(
            text = "Delay before: ${step.delayMs} ms",
            style = MaterialTheme.typography.bodySmall
        )
        Slider(
            value = step.delayMs.toFloat(),
            onValueChange = { raw ->
                val snapped = ((raw / 5f).roundToInt() * 5).coerceIn(0, 200)
                if (snapped != step.delayMs) onDelayChange(snapped)
            },
            valueRange = 0f..200f
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactTextButton(label = "↑", enabled = canMoveUp, onClick = onMoveUp)
            CompactTextButton(label = "↓", enabled = canMoveDown, onClick = onMoveDown)
            CompactTextButton(label = "Play", onClick = onPlay)
            CompactTextButton(label = "Remove", onClick = onRemove)
        }
    }
}

@Composable
private fun CompactTextButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label)
    }
}

/** A primitive the lab can place into a composition. */
@SuppressLint("NewApi")
private enum class LabPrimitive(val id: Int, val label: String, val constName: String) {
    CLICK(VibrationEffect.Composition.PRIMITIVE_CLICK, "Click", "PRIMITIVE_CLICK"),
    TICK(VibrationEffect.Composition.PRIMITIVE_TICK, "Tick", "PRIMITIVE_TICK"),
    LOW_TICK(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, "Low Tick", "PRIMITIVE_LOW_TICK"),
    QUICK_RISE(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, "Quick Rise", "PRIMITIVE_QUICK_RISE"),
    SLOW_RISE(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, "Slow Rise", "PRIMITIVE_SLOW_RISE"),
    QUICK_FALL(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, "Quick Fall", "PRIMITIVE_QUICK_FALL"),
    SPIN(VibrationEffect.Composition.PRIMITIVE_SPIN, "Spin", "PRIMITIVE_SPIN"),
    THUD(VibrationEffect.Composition.PRIMITIVE_THUD, "Thud", "PRIMITIVE_THUD")
}

/** A single mutable step in the lab's composition (intensity + delay are tweaked via sliders). */
private class LabStep(val primitive: LabPrimitive, scale: Float = 1f, delayMs: Int = 0) {
    var scale by mutableFloatStateOf(scale)
    var delayMs by mutableIntStateOf(delayMs)
}

@Preview(showBackground = true)
@Composable
fun HapticsLabScreenPreview() {
    HydroTrackerTheme {
        HapticsLabScreen()
    }
}

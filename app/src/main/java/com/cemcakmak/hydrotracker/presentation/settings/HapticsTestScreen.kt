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
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.data.models.DarkModePreference
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.utils.ForcedTier
import com.cemcakmak.hydrotracker.utils.SmartHaptics
import com.cemcakmak.hydrotracker.utils.SmartHapticToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HapticsTestScreen(
    themePreferences: ThemePreferences = ThemePreferences(),
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var playMode by remember { mutableStateOf(PlayMode.INDIVIDUAL) }
    var selectedTier by remember { mutableStateOf(ForcedTier.PRIMITIVES) }

    val buttonHighlights = remember { mutableStateMapOf<String, Boolean>() }

    fun highlightButton(key: String) {
        buttonHighlights[key] = true
        scope.launch {
            delay(300.milliseconds)
            buttonHighlights[key] = false
        }
    }

    fun playSystem(token: SmartHapticToken) {
        val constant = token.toHapticFeedbackConstant()
        if (constant != null) {
            view.performHapticFeedback(constant)
        }
        highlightButton("${token.name}_system")
    }

    fun playCustom(token: SmartHapticToken) {
        SmartHaptics.performForced(context, token, selectedTier)
        highlightButton("${token.name}_custom")
    }

    fun playAbSequence(token: SmartHapticToken) {
        scope.launch {
            playSystem(token)
            delay(200.milliseconds)
            playCustom(token)
        }
    }

    fun handlePlay(token: SmartHapticToken, side: HapticSide) {
        when (playMode) {
            PlayMode.INDIVIDUAL -> when (side) {
                HapticSide.SYSTEM -> playSystem(token)
                HapticSide.CUSTOM -> playCustom(token)
            }
            PlayMode.AB_SEQUENCE -> playAbSequence(token)
        }
    }

    SettingsDetailScaffold(
        title = "Haptic Feedback Test",
        onNavigateBack = onNavigateBack
    ) {

        DeviceInfoSection(themePreferences = themePreferences)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsSectionHeader("Play mode")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlayMode.entries.forEach { mode ->
                    val isSelected = playMode == mode
                    ToggleButton(
                        checked = isSelected,
                        onCheckedChange = {
                            haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                            playMode = mode
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = mode.label,
                            style = if (isSelected) {
                                MaterialTheme.typography.labelLargeEmphasized
                            } else {
                                MaterialTheme.typography.labelLarge
                            }
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsSectionHeader("Custom tier (forced)")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tiers = ForcedTier.entries
                tiers.forEachIndexed { index, tier ->
                    SelectableOptionCard(
                        index = index,
                        size = tiers.size,
                        selected = selectedTier == tier,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                            selectedTier = tier
                        }
                    ) { contentColor ->
                        Text(
                            text = tier.displayName(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = contentColor
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsSectionHeader("Tokens")
            Column {
                val tokens = SmartHapticToken.entries
                tokens.forEachIndexed { index, token ->
                    SettingsGroupCard(index = index, size = tokens.size) {
                        HapticTokenRowContent(
                            token = token,
                            systemHighlighted = buttonHighlights["${token.name}_system"] == true,
                            customHighlighted = buttonHighlights["${token.name}_custom"] == true,
                            onSystemClick = { handlePlay(token, HapticSide.SYSTEM) },
                            onCustomClick = { handlePlay(token, HapticSide.CUSTOM) }
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsSectionHeader("Stress tests")
            SettingsGroupCard(index = 0, size = 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "10× Rapid Ticks",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Fires SEGMENT_FREQUENT_TICK rapidly",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            scope.launch {
                                repeat(10) {
                                    SmartHaptics.performForced(
                                        context,
                                        SmartHapticToken.SegmentFrequentTick,
                                        selectedTier
                                    )
                                    delay(50.milliseconds)
                                }
                            }
                        },
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Text("Fire")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceInfoSection(
    themePreferences: ThemePreferences
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

    val context = LocalContext.current
    val primitivesSupported = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibrator = (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            vibrator?.arePrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)?.firstOrNull() == true
        } else {
            false
        }
    }

    // Which tier SmartHaptics will actually route to on this device — straight from the engine,
    // so the readout always matches real behavior. Uses a representative token (Confirm).
    val resolvedTier = remember { SmartHaptics.resolveTierLabel(context) }

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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoRow("Manufacturer", Build.MANUFACTURER)
            HorizontalDivider()
            InfoRow("Model", Build.MODEL)
            HorizontalDivider()
            InfoRow("API Level", Build.VERSION.SDK_INT.toString())
            HorizontalDivider()
            InfoRow("Broken OEM", if (SmartHaptics.isBrokenOem) "Yes" else "No")
            HorizontalDivider()
            InfoRow("Primitives", if (primitivesSupported) "Supported" else "Not supported")
            HorizontalDivider()
            InfoRow("Haptics Tier", resolvedTier)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmallEmphasized
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun HapticTokenRowContent(
    token: SmartHapticToken,
    systemHighlighted: Boolean,
    customHighlighted: Boolean,
    onSystemClick: () -> Unit,
    onCustomClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text(
                text = token.displayName(),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = token.description(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HapticPlayButton(
                label = "System",
                highlighted = systemHighlighted,
                onClick = onSystemClick,
                modifier = Modifier.weight(1f)
            )
            HapticPlayButton(
                label = "Custom",
                highlighted = customHighlighted,
                onClick = onCustomClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HapticPlayButton(
    label: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (highlighted) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
        label = "playButtonColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (highlighted) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        },
        animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
        label = "playButtonColor"
    )

    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        shapes = ButtonDefaults.shapes(),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Text(label)
    }
}

private enum class PlayMode(val label: String) {
    INDIVIDUAL("Individual"),
    AB_SEQUENCE("A/B Sequence")
}

private enum class HapticSide {
    SYSTEM,
    CUSTOM
}

private fun ForcedTier.displayName(): String = when (this) {
    ForcedTier.AUTO -> "Auto"
    ForcedTier.CONSTANTS -> "Constants"
    ForcedTier.PRIMITIVES -> "Primitives"
    ForcedTier.PREDEFINED -> "Predefined"
    ForcedTier.LEGACY -> "Legacy"
}

private fun SmartHapticToken.displayName(): String = when (this) {
    SmartHapticToken.ClockTick -> "Clock Tick"
    SmartHapticToken.Confirm -> "Confirm"
    SmartHapticToken.ContextClick -> "Context Click"
    SmartHapticToken.DragStart -> "Drag Start"
    SmartHapticToken.GestureEnd -> "Gesture End"
    SmartHapticToken.GestureStart -> "Gesture Start"
    SmartHapticToken.GestureThresholdActive -> "Threshold Active"
    SmartHapticToken.GestureThresholdDeactive -> "Threshold Deactive"
    SmartHapticToken.LongPress -> "Long Press"
    SmartHapticToken.Reject -> "Reject"
    SmartHapticToken.SegmentFrequentTick -> "Frequent Tick"
    SmartHapticToken.SegmentTick -> "Segment Tick"
    SmartHapticToken.TextHandleMove -> "Text Handle Move"
    SmartHapticToken.ToggleOff -> "Toggle Off"
    SmartHapticToken.ToggleOn -> "Toggle On"
    SmartHapticToken.VirtualKey -> "Virtual Key"
    SmartHapticToken.VirtualKeyRelease -> "Key Release"
}

private fun SmartHapticToken.description(): String = when (this) {
    SmartHapticToken.ClockTick -> "The user has pressed either an hour or minute tick of a Clock."
    SmartHapticToken.Confirm -> "A haptic effect to signal the confirmation or successful completion of a user interaction."
    SmartHapticToken.ContextClick -> "The user has performed a context click on an object."
    SmartHapticToken.DragStart -> "The user has started a drag-and-drop gesture."
    SmartHapticToken.GestureEnd -> "The user has finished a gesture (e.g. on the soft keyboard)."
    SmartHapticToken.GestureStart -> "The user has started a gesture (e.g. on the soft keyboard)."
    SmartHapticToken.GestureThresholdActive -> "The user is executing a swipe/drag-style gesture, such as pull-to-refresh, where the gesture action is \"eligible\" at a certain threshold of movement, and can be cancelled by moving back past the threshold."
    SmartHapticToken.GestureThresholdDeactive -> "The user is executing a swipe/drag-style gesture, such as pull-to-refresh, where the gesture action is \"eligible\" at a certain threshold of movement, and can be cancelled by moving back past the threshold."
    SmartHapticToken.LongPress -> "The user has performed a long press on an object that is resulting in an action being performed."
    SmartHapticToken.Reject -> "A haptic effect to signal the rejection or failure of a user interaction."
    SmartHapticToken.SegmentFrequentTick -> "The user is switching between a series of many potential choices, for example minutes on a clock face, or individual percentages."
    SmartHapticToken.SegmentTick -> "The user is switching between a series of potential choices, for example items in a list or discrete points on a slider."
    SmartHapticToken.TextHandleMove -> "The user has performed a selection/insertion handle move on text field."
    SmartHapticToken.ToggleOff -> "The user has toggled a switch or button into the off position."
    SmartHapticToken.ToggleOn -> "The user has toggled a switch or button into the on position."
    SmartHapticToken.VirtualKey -> "The user has pressed on a virtual on-screen key."
    SmartHapticToken.VirtualKeyRelease -> "The user has released a virtual key"
}

@Preview(showBackground = true)
@Composable
fun HapticsTestScreenPreview() {
    HydroTrackerTheme {
        HapticsTestScreen()
    }
}

@SuppressLint("NewApi")
private fun SmartHapticToken.toHapticFeedbackConstant(): Int? = when (this) {
    SmartHapticToken.ClockTick -> HapticFeedbackConstants.CLOCK_TICK
    SmartHapticToken.Confirm -> HapticFeedbackConstants.CONFIRM
    SmartHapticToken.ContextClick -> HapticFeedbackConstants.CONTEXT_CLICK
    SmartHapticToken.DragStart -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        HapticFeedbackConstants.DRAG_START
    } else null
    SmartHapticToken.GestureEnd -> HapticFeedbackConstants.GESTURE_END
    SmartHapticToken.GestureStart -> HapticFeedbackConstants.GESTURE_START
    SmartHapticToken.GestureThresholdActive -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
    } else null
    SmartHapticToken.GestureThresholdDeactive -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        HapticFeedbackConstants.GESTURE_THRESHOLD_DEACTIVATE
    } else null
    SmartHapticToken.LongPress -> HapticFeedbackConstants.LONG_PRESS
    SmartHapticToken.Reject -> HapticFeedbackConstants.REJECT
    SmartHapticToken.SegmentFrequentTick -> HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
    SmartHapticToken.SegmentTick -> HapticFeedbackConstants.SEGMENT_TICK
    SmartHapticToken.TextHandleMove -> HapticFeedbackConstants.TEXT_HANDLE_MOVE
    SmartHapticToken.ToggleOff -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        HapticFeedbackConstants.TOGGLE_OFF
    } else null
    SmartHapticToken.ToggleOn -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        HapticFeedbackConstants.TOGGLE_ON
    } else null
    SmartHapticToken.VirtualKey -> HapticFeedbackConstants.VIRTUAL_KEY
    SmartHapticToken.VirtualKeyRelease -> HapticFeedbackConstants.VIRTUAL_KEY_RELEASE
}

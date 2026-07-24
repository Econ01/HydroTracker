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

package com.cemcakmak.hydrotracker.presentation.common.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry
import com.cemcakmak.hydrotracker.data.models.BeverageType
import com.cemcakmak.hydrotracker.data.models.ContainerPreset
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.presentation.common.BeverageOption
import com.cemcakmak.hydrotracker.presentation.common.toOption
import com.cemcakmak.hydrotracker.utils.DateTimeFormatters
import com.cemcakmak.hydrotracker.utils.VolumeUnitConverter
import java.time.LocalTime
import java.util.Calendar

/**
 * Dialogue for editing an existing water intake entry.
 *
 * Presents the container type, beverage type, timestamp, and amount. External entries are shown
 * in a read-only state with an explanation instead of editable fields.
 *
 * @param entry The entry to edit.
 * @param themePreferences Theme preferences used for time formatting.
 * @param volumeUnit The user's preferred volume unit for amount display.
 * @param onDismiss Called when the user cancels or closes the dialogue.
 * @param onConfirm Called with the updated entry when the user saves.
 * @param beverages The list of beverage options available for selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWaterDialog(
    entry: WaterIntakeEntry,
    themePreferences: ThemePreferences,
    volumeUnit: VolumeUnit,
    onDismiss: () -> Unit,
    onConfirm: (WaterIntakeEntry) -> Unit,
    beverages: List<BeverageOption> = BeverageType.getAllSorted().map { it.toOption() }
) {
    val context = LocalContext.current

    val minAmountMl = 1.0
    val maxAmountMl = 5000.0
    val minAmountDisplay = VolumeUnitConverter.formatValue(minAmountMl, volumeUnit)
    val maxAmountDisplay = VolumeUnitConverter.formatValue(maxAmountMl, volumeUnit)
    val unitShortLabel = stringResource(volumeUnit.shortLabelResId)

    var amountText by remember {
        mutableStateOf(VolumeUnitConverter.formatValue(entry.amount, volumeUnit))
    }
    var containerType by remember { mutableStateOf(entry.containerType) }
    var selectedBeverage by remember {
        mutableStateOf(
            beverages.find { it.storageKey == entry.beverageType }
                ?: beverages.firstOrNull { it.isWater }
                ?: BeverageType.WATER.toOption()
        )
    }
    var isError by remember { mutableStateOf(false) }

    // Time picker state
    var showTimePicker by remember { mutableStateOf(false) }
    val calendar = remember {
        Calendar.getInstance().apply {
            timeInMillis = entry.timestamp
        }
    }
    var selectedHour by remember { mutableIntStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(calendar.get(Calendar.MINUTE)) }

    val timePickerState = rememberTimePickerState(
        initialHour = selectedHour,
        initialMinute = selectedMinute,
        is24Hour = true
    )

    val presets = remember { ContainerPreset.getDefaultPresets() }
    val isExternalEntry = entry.isExternalEntry()

    val handleSave = {
        val amountInUserUnit = amountText.replace(",", "").toDouble()
        val amountInMl = VolumeUnitConverter.toMillilitres(amountInUserUnit, volumeUnit)

        val newCalendar = Calendar.getInstance().apply {
            timeInMillis = entry.timestamp
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val updatedEntry = entry.copy(
            amount = amountInMl,
            containerType = containerType,
            beverageType = selectedBeverage.storageKey,
            beverageMultiplier = selectedBeverage.storedMultiplier,
            timestamp = newCalendar.timeInMillis
        )
        onConfirm(updatedEntry)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLargeIncreased
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(
                        if (isExternalEntry) R.string.home_dialog_external_entry_title
                        else R.string.home_dialog_edit_entry_title
                    ),
                    style = MaterialTheme.typography.headlineSmallEmphasized
                )

                // Warning message for external entries
                if (isExternalEntry) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.home_external_entry_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = stringResource(R.string.home_external_entry_message),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // Container type dropdown
                var expanded by remember { mutableStateOf(false) }

                val customContainerKey = "Custom"
                val displayContainerType = if (containerType == customContainerKey) {
                    stringResource(R.string.home_option_custom)
                } else {
                    containerType
                }

                ExposedDropdownMenuBox(
                    expanded = expanded && !isExternalEntry,
                    onExpandedChange = { if (!isExternalEntry) expanded = !expanded }
                ) {
                    OutlinedTextField(
                        shape = MaterialTheme.shapes.extraExtraLarge,
                        value = displayContainerType,
                        onValueChange = { },
                        readOnly = true,
                        enabled = !isExternalEntry,
                        label = { Text(stringResource(R.string.home_label_container_type)) },
                        leadingIcon = {
                            val matchedPreset = presets.find { it.name == containerType }
                            if (matchedPreset?.iconRes != null) {
                                Icon(
                                    painter = painterResource(matchedPreset.iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        trailingIcon = {
                            if (!isExternalEntry) {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            }
                        },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )

                    if (!isExternalEntry) {
                        // "Custom" is appended as a virtual item at the end
                        val allItems = presets + null
                        val itemCount = allItems.size

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            containerColor = MenuDefaults.groupStandardContainerColor,
                            shape = MenuDefaults.standaloneGroupShape,
                        ) {
                            allItems.forEachIndexed { index, preset ->
                                val isCustomItem = preset == null
                                val itemLabel = if (isCustomItem) {
                                    stringResource(R.string.home_option_custom)
                                } else if (preset.labelResId != 0) {
                                    stringResource(preset.labelResId)
                                } else {
                                    preset.name
                                }
                                val isSelected = if (isCustomItem) {
                                    containerType == customContainerKey
                                } else {
                                    containerType == preset.name
                                }

                                DropdownMenuItem(
                                    selected = isSelected,
                                    onClick = {
                                        if (isCustomItem) {
                                            containerType = customContainerKey
                                        } else {
                                            containerType = preset.name
                                            amountText = VolumeUnitConverter.formatValue(
                                                preset.volume, volumeUnit
                                            )
                                        }
                                        expanded = false
                                    },
                                    text = { Text(itemLabel) },
                                    shapes = MenuDefaults.itemShape(index, itemCount),
                                    leadingIcon = if (!isCustomItem && preset.iconRes != null) {
                                        {
                                            Icon(
                                                painter = painterResource(preset.iconRes),
                                                contentDescription = null,
                                                modifier = Modifier.size(MenuDefaults.LeadingIconSize)
                                            )
                                        }
                                    } else null,
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                )
                            }
                        }
                    }
                }

                // Beverage Type Selection Dropdown
                if (!isExternalEntry) {
                    var beverageExpanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = beverageExpanded,
                        onExpandedChange = { beverageExpanded = !beverageExpanded }
                    ) {
                        OutlinedTextField(
                            shape = MaterialTheme.shapes.extraExtraLarge,
                            value = if (selectedBeverage.hasLabelRes) {
                                stringResource(selectedBeverage.labelResId)
                            } else {
                                selectedBeverage.displayName
                            },
                            onValueChange = { },
                            readOnly = true,
                            label = { Text(stringResource(R.string.home_label_beverage_type)) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(selectedBeverage.iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = beverageExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = beverageExpanded,
                            onDismissRequest = { beverageExpanded = false },
                            containerColor = MenuDefaults.groupStandardContainerColor,
                            shape = MenuDefaults.standaloneGroupShape,
                        ) {
                            val beverageCount = beverages.size
                            beverages.forEachIndexed { index, beverage ->
                                val itemLabel = if (beverage.hasLabelRes) {
                                    stringResource(beverage.labelResId)
                                } else {
                                    beverage.displayName
                                }

                                DropdownMenuItem(
                                    selected = selectedBeverage.displayName == beverage.displayName,
                                    onClick = {
                                        selectedBeverage = beverage
                                        beverageExpanded = false
                                    },
                                    text = { Text(itemLabel) },
                                    shapes = MenuDefaults.itemShape(index, beverageCount),
                                    supportingText = {
                                        Text(
                                            stringResource(
                                                R.string.home_label_hydration_percentage,
                                                (beverage.hydrationMultiplier * 100).toInt()
                                            )
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(beverage.iconRes),
                                            contentDescription = null,
                                            modifier = Modifier.size(MenuDefaults.LeadingIconSize)
                                        )
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                )
                            }
                        }
                    }
                }

                // Time picker field
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        shape = MaterialTheme.shapes.extraLargeIncreased,
                        value = DateTimeFormatters.formatTime(
                            context,
                            LocalTime.of(selectedHour, selectedMinute),
                            themePreferences.timeFormat
                        ),
                        onValueChange = { },
                        readOnly = true,
                        enabled = !isExternalEntry,
                        label = { Text(stringResource(R.string.home_label_time)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Invisible clickable overlay to capture clicks
                    if (!isExternalEntry) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showTimePicker = true }
                        )
                    }
                }

                // Amount field
                OutlinedTextField(
                    shape = MaterialTheme.shapes.extraLargeIncreased,
                    value = amountText,
                    onValueChange = {
                        if (!isExternalEntry) {
                            amountText = it
                            isError = false
                        }
                    },
                    enabled = !isExternalEntry,
                    label = { Text(stringResource(R.string.home_label_amount_ml, unitShortLabel)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (volumeUnit == VolumeUnit.MILLILITRES) KeyboardType.Number else KeyboardType.Decimal
                    ),
                    isError = isError && !isExternalEntry,
                    supportingText = if (isError && !isExternalEntry) {
                        { Text(stringResource(R.string.home_error_amount_invalid, minAmountDisplay, maxAmountDisplay)) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                ActionButtons(
                    onDismiss = onDismiss,
                    onSave = handleSave,
                    primaryLabel = stringResource(
                        if (isExternalEntry) R.string.action_close else R.string.action_save
                    ),
                    secondaryLabel = stringResource(
                        if (isExternalEntry) R.string.action_close else R.string.action_cancel
                    ),
                    amountText = amountText,
                    volumeUnit = volumeUnit,
                    isError = { isError = true },
                    showPrimaryButton = !isExternalEntry
                )
            }
        }
    }

    // Time Picker Dialogue
    if (showTimePicker) {
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLargeIncreased,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_dialog_select_time),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    TimePicker(
                        state = timePickerState,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text(stringResource(R.string.action_cancel))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            shapes = ButtonDefaults.shapes(),
                            onClick = {
                                selectedHour = timePickerState.hour
                                selectedMinute = timePickerState.minute
                                showTimePicker = false
                            }
                        ) {
                            Text(stringResource(R.string.action_confirm))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionButtons(
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    primaryLabel: String,
    secondaryLabel: String,
    amountText: String,
    volumeUnit: VolumeUnit,
    isError: () -> Unit,
    showPrimaryButton: Boolean = true
) {
    val haptics = LocalHapticFeedback.current
    val cancelInteractionSource = remember { MutableInteractionSource() }
    val addInteractionSource = remember { MutableInteractionSource() }

    val minAmountMl = 1.0
    val maxAmountMl = 5000.0

    var isInvalid by remember { mutableStateOf(false) }

    LaunchedEffect(cancelInteractionSource) {
        cancelInteractionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                is PressInteraction.Release -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                else -> {  }
            }
        }
    }

    LaunchedEffect(addInteractionSource) {
        addInteractionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                is PressInteraction.Release -> if (isInvalid) haptics.performHapticFeedback(HapticFeedbackType.Reject) else haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                else -> {  }
            }
        }
    }

    ButtonGroup(
        modifier = Modifier.fillMaxWidth(),
        overflowIndicator = {}
    ) {
        val scope = this
        customItem(
            buttonGroupContent = {
                FilledTonalButton(
                    onClick = onDismiss,
                    shapes = ButtonDefaults.shapes(),
                    interactionSource = cancelInteractionSource,
                    modifier = with(scope) {
                        Modifier
                            .weight(1f)
                            .height(46.dp)
                            .animateWidth(interactionSource = cancelInteractionSource)
                    }
                ) {
                    Text(
                        text = secondaryLabel,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            },
            menuContent = {}
        )

        if (showPrimaryButton) {
            customItem(
                buttonGroupContent = {
                    Button(
                        onClick = {
                            val amountInUserUnit = amountText.replace(",", "").toDoubleOrNull()
                            if (amountInUserUnit != null && amountInUserUnit > 0) {
                                val amountInMl = VolumeUnitConverter.toMillilitres(amountInUserUnit, volumeUnit)
                                if (amountInMl in minAmountMl..maxAmountMl) {
                                    onSave()
                                } else {
                                    isError()
                                    isInvalid = true
                                }
                            } else {
                                isError()
                                isInvalid = true
                            }
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shapes = ButtonDefaults.shapes(),
                        interactionSource = addInteractionSource,
                        modifier = with(scope) {
                            Modifier
                                .weight(1f)
                                .height(46.dp)
                                .animateWidth(interactionSource = addInteractionSource)
                        }
                    ) {
                        Text(
                            text = primaryLabel,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                },
                menuContent = {}
            )
        }
    }
}
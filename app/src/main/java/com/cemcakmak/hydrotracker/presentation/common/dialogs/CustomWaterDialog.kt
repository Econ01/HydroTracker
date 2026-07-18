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

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.BeverageType
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.presentation.common.BeverageOption
import com.cemcakmak.hydrotracker.presentation.common.toOption
import com.cemcakmak.hydrotracker.ui.theme.rememberBeverageColorRoles
import com.cemcakmak.hydrotracker.utils.VolumeUnitConverter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomWaterDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
    selectedBeverage: BeverageOption,
    onBeverageChange: (BeverageOption) -> Unit,
    volumeUnit: VolumeUnit,
    beverages: List<BeverageOption> = BeverageType.getAllSorted().map { it.toOption() },
    useBeverageColors: Boolean = false,
) {
    var amountText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val minAmountMl = 1.0
    val maxAmountMl = 5000.0
    val minAmountDisplay = VolumeUnitConverter.formatValue(minAmountMl, volumeUnit)
    val maxAmountDisplay = VolumeUnitConverter.formatValue(maxAmountMl, volumeUnit)
    val unitShortLabel = stringResource(volumeUnit.shortLabelResId)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLargeIncreased
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_dialog_add_custom_amount),
                    style = MaterialTheme.typography.headlineSmallEmphasized
                )

                // Beverage Type Selection Dropdown
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
                                    onBeverageChange(beverage)
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

                // Show selected beverage info
                if (!selectedBeverage.isWater) {
                    val beverageColors = rememberBeverageColorRoles(
                        beverageKey = selectedBeverage.storageKey,
                        useBeverageColors = useBeverageColors
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = beverageColors.containerColor,
                        contentColor = beverageColors.onContainerColor,
                        shape = MaterialTheme.shapes.extraLargeIncreased
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            selectedBeverage.description?.let { description ->
                                val resolvedDescription = if (selectedBeverage.hasDescriptionRes) {
                                    stringResource(selectedBeverage.descriptionResId)
                                } else {
                                    description
                                }
                                Text(
                                    text = resolvedDescription,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Text(
                                text = stringResource(
                                    R.string.home_label_hydration_effectiveness,
                                    (selectedBeverage.hydrationMultiplier * 100).toInt()
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = beverageColors.color
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    shape = MaterialTheme.shapes.extraLargeIncreased,
                    onValueChange = {
                        amountText = it
                        isError = false
                    },
                    label = { Text(stringResource(R.string.home_label_amount_ml, unitShortLabel)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (volumeUnit == VolumeUnit.MILLILITRES) KeyboardType.Number else KeyboardType.Decimal
                    ),
                    isError = isError,
                    supportingText = if (isError) {
                        { Text(stringResource(R.string.home_error_amount_invalid, minAmountDisplay, maxAmountDisplay)) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                ActionButtons(
                    onDismiss = onDismiss,
                    onConfirm = onConfirm,
                    onValidationError = { isError = true },
                    amountText = amountText,
                    volumeUnit = volumeUnit
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionButtons(
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
    onValidationError: () -> Unit,
    amountText: String,
    volumeUnit: VolumeUnit
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
                        text = stringResource(R.string.action_cancel),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            },
            menuContent = {}
        )

        customItem(
            buttonGroupContent = {
                Button(
                    onClick = {
                        val amountInUserUnit = amountText.toDoubleOrNull()
                        if (amountInUserUnit != null && amountInUserUnit > 0) {
                            val amountInMl = VolumeUnitConverter.toMillilitres(amountInUserUnit, volumeUnit)
                            if (amountInMl in minAmountMl..maxAmountMl) {
                                onConfirm(amountInMl)
                            } else {
                                isInvalid = true
                                onValidationError()
                            }
                        } else {
                            onValidationError()
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
                        text = stringResource(R.string.action_add),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            },
            menuContent = {}
        )
    }
}

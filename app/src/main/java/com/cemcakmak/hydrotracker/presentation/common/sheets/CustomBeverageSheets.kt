package com.cemcakmak.hydrotracker.presentation.common.sheets

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.BeverageType
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme

/**
 * Sheets for the beverage management page: add / edit custom beverages, and a read-only
 * sheet for preset beverages (hide / show only). Styling mirrors ContainerPresetSheets.
 */

/** Curated icon set users can pick for a custom beverage (reuses the preset beverage drawables). */
data class BeverageIconOption(val key: String, @param:DrawableRes val res: Int)

object BeverageIcons {
    const val DEFAULT_KEY = "water"

    val options = listOf(
        BeverageIconOption("water", R.drawable.water_filled),
        BeverageIconOption("coffee", R.drawable.coffee_filled),
        BeverageIconOption("tea", R.drawable.tea_filled),
        BeverageIconOption("soft_drink", R.drawable.soft_drink_filled),
        BeverageIconOption("energy_drink", R.drawable.energy_drink_filled),
        BeverageIconOption("sports_drink", R.drawable.sports_drink_filled),
        BeverageIconOption("oral_rehydration_solution", R.drawable.oral_rehydration_solution_filled),
        BeverageIconOption("milk", R.drawable.milk_filled),
        BeverageIconOption("fruit_juice", R.drawable.fruit_juice_filled)
    )

    @DrawableRes
    fun resFor(key: String): Int = options.find { it.key == key }?.res ?: R.drawable.water_filled
}

private const val MIN_PERCENT = 1
private const val MAX_PERCENT = 500

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AddCustomBeverageSheetContent(
    onAdd: (name: String, hydrationMultiplier: Double, iconKey: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var percentText by remember { mutableStateOf("100") }
    var iconKey by remember { mutableStateOf(BeverageIcons.DEFAULT_KEY) }
    var nameError by remember { mutableStateOf(false) }
    var percentError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        BeverageSheetHeader(title = stringResource(R.string.beverage_add), iconKey = iconKey)

        BeverageFormFields(
            name = name,
            onNameChange = { name = it; nameError = false },
            percentText = percentText,
            onPercentChange = { percentText = it; percentError = false },
            iconKey = iconKey,
            onIconKeyChange = { iconKey = it },
            nameError = nameError,
            percentError = percentError
        )

        val haptics = LocalHapticFeedback.current
        val addInteractionSource = remember { MutableInteractionSource() }

        LaunchedEffect(addInteractionSource) {
            addInteractionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    is PressInteraction.Release -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    else -> {  }
                }
            }
        }

        Button(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                val trimmedName = name.trim()
                val percent = percentText.toIntOrNull()
                nameError = trimmedName.isEmpty()
                percentError = percent == null || percent < MIN_PERCENT || percent > MAX_PERCENT
                if (!nameError && !percentError && percent != null) {
                    onAdd(trimmedName, percent / 100.0, iconKey)
                }
            },
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shapes = ButtonDefaults.shapes(),
            interactionSource = addInteractionSource,
            modifier = Modifier
                .fillMaxWidth(1f)
                .height(56.dp)
        ) {
            Text(stringResource(R.string.beverage_add))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AddCustomBeverageBottomSheet(
    onDismiss: () -> Unit,
    onAdd: (name: String, hydrationMultiplier: Double, iconKey: String) -> Unit
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        AddCustomBeverageSheetContent(onAdd = onAdd)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EditCustomBeverageSheetContent(
    initialName: String,
    initialMultiplier: Double,
    initialIconKey: String,
    onSave: (name: String, hydrationMultiplier: Double, iconKey: String) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var percentText by remember { mutableStateOf((initialMultiplier * 100).toInt().toString()) }
    var iconKey by remember { mutableStateOf(initialIconKey) }
    var nameError by remember { mutableStateOf(false) }
    var percentError by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        BeverageSheetHeader(title = stringResource(R.string.beverage_edit_title), iconKey = iconKey)

        BeverageFormFields(
            name = name,
            onNameChange = { name = it; nameError = false },
            percentText = percentText,
            onPercentChange = { percentText = it; percentError = false },
            iconKey = iconKey,
            onIconKeyChange = { iconKey = it },
            nameError = nameError,
            percentError = percentError
        )

        val haptics = LocalHapticFeedback.current
        val deleteInteractionSource = remember { MutableInteractionSource() }
        val saveInteractionSource = remember { MutableInteractionSource() }

        LaunchedEffect(deleteInteractionSource) {
            deleteInteractionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    is PressInteraction.Release -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    else -> {  }
                }
            }
        }

        LaunchedEffect(saveInteractionSource) {
            saveInteractionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    is PressInteraction.Release -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
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
                        onClick = {
                            showDeleteConfirmation = true
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shapes = ButtonDefaults.shapes(),
                        interactionSource = deleteInteractionSource,
                        modifier = with(scope) {
                            Modifier
                                .weight(1f)
                                .height(56.dp)
                                .animateWidth(interactionSource = deleteInteractionSource)
                        }
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.delete_fill),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_delete),
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
                            val trimmedName = name.trim()
                            val percent = percentText.toIntOrNull()

                            nameError = trimmedName.isEmpty()
                            percentError = percent == null || percent < MIN_PERCENT || percent > MAX_PERCENT

                            if (!nameError && !percentError && percent != null) {
                                onSave(trimmedName, percent / 100.0, iconKey)
                            }
                        },
                        shapes = ButtonDefaults.shapes(),
                        interactionSource = saveInteractionSource,
                        modifier = with(scope) {
                            Modifier
                                .weight(1f)
                                .height(56.dp)
                                .animateWidth(interactionSource = saveInteractionSource)
                        }
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.save_fill),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_save),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                },
                menuContent = {}
            )
        }
    }

    if (showDeleteConfirmation) {
        val dialogHaptics = LocalHapticFeedback.current
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(R.string.beverage_delete_title)) },
            text = { Text(stringResource(R.string.delete_confirm_message, name.trim())) },
            confirmButton = {
                Button(
                    onClick = {
                        dialogHaptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        showDeleteConfirmation = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditCustomBeverageBottomSheet(
    initialName: String,
    initialMultiplier: Double,
    initialIconKey: String,
    onDismiss: () -> Unit,
    onSave: (name: String, hydrationMultiplier: Double, iconKey: String) -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        EditCustomBeverageSheetContent(
            initialName = initialName,
            initialMultiplier = initialMultiplier,
            initialIconKey = initialIconKey,
            onSave = onSave,
            onDelete = onDelete
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PresetBeverageSheetContent(
    type: BeverageType,
    isHidden: Boolean,
    onToggleHidden: () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(type.labelResId),
                    style = MaterialTheme.typography.titleLargeEmphasized
                )
                Text(
                    text = stringResource(
                        R.string.beverage_hydration_effectiveness,
                        (type.hydrationMultiplier * 100).toInt()
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(type.iconResFilled),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Text(
            text = stringResource(type.descriptionResId),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val hideInteractionSource = remember { MutableInteractionSource() }

        LaunchedEffect(hideInteractionSource) {
            hideInteractionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    is PressInteraction.Release -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    else -> {  }
                }
            }
        }

        Button(
            onClick = {
                onToggleHidden()
            },
            shapes = ButtonDefaults.shapes(),
            interactionSource = hideInteractionSource,
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = if (isHidden) ImageVector.vectorResource(R.drawable.visibility_filled) else ImageVector.vectorResource(R.drawable.visibility_off_filled),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isHidden) stringResource(R.string.beverage_show_in_quick_add) else stringResource(R.string.beverage_hide_from_quick_add))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PresetBeverageBottomSheet(
    type: BeverageType,
    isHidden: Boolean,
    onToggleHidden: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        PresetBeverageSheetContent(
            type = type,
            isHidden = isHidden,
            onToggleHidden = onToggleHidden
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BeverageSheetHeader(title: String, iconKey: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLargeEmphasized)
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(56.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(BeverageIcons.resFor(iconKey)),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BeverageFormFields(
    name: String,
    onNameChange: (String) -> Unit,
    percentText: String,
    onPercentChange: (String) -> Unit,
    iconKey: String,
    onIconKeyChange: (String) -> Unit,
    nameError: Boolean,
    percentError: Boolean
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(stringResource(R.string.beverage_name_label)) },
        placeholder = { Text(stringResource(R.string.beverage_name_placeholder)) },
        isError = nameError,
        supportingText = if (nameError) {
            { Text(stringResource(R.string.error_name_required)) }
        } else null,
        singleLine = true,
        shape = RoundedCornerShape(50.dp),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
    )

    OutlinedTextField(
        value = percentText,
        onValueChange = onPercentChange,
        label = { Text(stringResource(R.string.beverage_effectiveness_label)) },
        placeholder = { Text(stringResource(R.string.beverage_effectiveness_placeholder)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = percentError,
        supportingText = if (percentError) {
            { Text(stringResource(R.string.beverage_effectiveness_error, MIN_PERCENT, MAX_PERCENT)) }
        } else {
            { Text(stringResource(R.string.beverage_effectiveness_hint)) }
        },
        singleLine = true,
        shape = RoundedCornerShape(50.dp),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
    )

    Text(
        text = stringResource(R.string.label_icon),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp)
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BeverageIcons.options.forEach { option ->
            val selected = option.key == iconKey
            Surface(
                onClick = { onIconKeyChange(option.key) },
                shape = CircleShape,
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier.size(48.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(option.res),
                        contentDescription = option.key,
                        tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun AddCustomBeverageBottomSheetPreview() {
    HydroTrackerTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BottomSheetDefaults.DragHandle()
                AddCustomBeverageSheetContent(onAdd = { _, _, _ -> })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun EditCustomBeverageBottomSheetPreview() {
    HydroTrackerTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BottomSheetDefaults.DragHandle()
                EditCustomBeverageSheetContent(
                    initialName = "Green Smoothie",
                    initialMultiplier = 0.85,
                    initialIconKey = "fruit_juice",
                    onSave = { _, _, _ -> },
                    onDelete = {}
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun PresetBeverageBottomSheetPreview() {
    HydroTrackerTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BottomSheetDefaults.DragHandle()
                PresetBeverageSheetContent(
                    type = BeverageType.SPORTS_DRINK,
                    isHidden = false,
                    onToggleHidden = {}
                )
            }
        }
    }
}

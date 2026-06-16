package com.cemcakmak.hydrotracker.presentation.common

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
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
import com.cemcakmak.hydrotracker.data.models.ContainerPreset
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.utils.ContainerIconMapper
import com.cemcakmak.hydrotracker.utils.VolumeUnitConverter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EditContainerPresetSheetContent(
    preset: ContainerPreset,
    volumeUnit: VolumeUnit,
    onSave: (name: String, volume: Double) -> Unit,
    onDelete: () -> Unit
) {

    val minVolumeMl = 1.0
    val maxVolumeMl = 5000.0
    val minVolumeDisplay = VolumeUnitConverter.formatValue(minVolumeMl, volumeUnit)
    val maxVolumeDisplay = VolumeUnitConverter.formatValue(maxVolumeMl, volumeUnit)
    val unitShortLabel = stringResource(volumeUnit.shortLabelResId)

    var name by remember { mutableStateOf(preset.name) }
    var volumeText by remember {
        mutableStateOf(VolumeUnitConverter.formatValue(preset.volume, volumeUnit))
    }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }
    var volumeError by remember { mutableStateOf(false) }

    // Calculate preview icon based on current volume (converted back to millilitres)
    val previewIcon = remember(volumeText, volumeUnit) {
        val volumeInUserUnit = volumeText.toDoubleOrNull() ?: 0.0
        val volumeInMl = VolumeUnitConverter.toMillilitres(volumeInUserUnit, volumeUnit)
        ContainerIconMapper.getIconForVolume(if (volumeInMl > 0) volumeInMl else preset.volume)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.container_edit_title),
                style = MaterialTheme.typography.titleLargeEmphasized
            )

            // Icon preview
            Surface(
                shape = MaterialShapes.Cookie12Sided.toShape(),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(66.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        previewIcon.drawableRes != null -> {
                            Icon(
                                painter = painterResource(previewIcon.drawableRes),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        previewIcon.vectorIcon != null -> {
                            Icon(
                                imageVector = previewIcon.vectorIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }

        // Name field
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                nameError = false
            },
            label = { Text(stringResource(R.string.container_name_label)) },
            placeholder = { Text(stringResource(R.string.container_name_placeholder)) },
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

        // Volume field
        OutlinedTextField(
            value = volumeText,
            onValueChange = {
                volumeText = it
                volumeError = false
            },
            label = { Text(stringResource(R.string.container_volume_label, unitShortLabel)) },
            placeholder = { Text(stringResource(R.string.container_volume_placeholder)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (volumeUnit == VolumeUnit.MILLILITRES) KeyboardType.Number else KeyboardType.Decimal
            ),
            isError = volumeError,
            shape = RoundedCornerShape(50.dp),
            supportingText = if (volumeError) {
                { Text(stringResource(R.string.container_volume_error, minVolumeDisplay, maxVolumeDisplay)) }
            } else {
                { Text(stringResource(R.string.container_icon_auto)) }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 60.dp)
        )

        // Action buttons - Standard button group with press animations
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
                            val volumeInUserUnit = volumeText.toDoubleOrNull()

                            nameError = trimmedName.isEmpty()
                            val volumeInMl = volumeInUserUnit?.let {
                                VolumeUnitConverter.toMillilitres(it, volumeUnit)
                            }
                            volumeError = volumeInMl == null || volumeInMl <= 0 || volumeInMl > maxVolumeMl

                            if (!nameError && !volumeError && volumeInMl != null) {
                                onSave(trimmedName, volumeInMl)
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

    // Delete confirmation dialogue
    if (showDeleteConfirmation) {
        AlertDialog(
            modifier = Modifier.fillMaxWidth(0.9f),
            onDismissRequest = { showDeleteConfirmation = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(R.string.container_delete_title)) },
            text = {
                Text(stringResource(R.string.delete_confirm_message, preset.name))
            },
            confirmButton = {
                val haptics = LocalHapticFeedback.current
                val cancelInteractionSource = remember { MutableInteractionSource() }
                val confirmDeleteInteractionSource = remember { MutableInteractionSource() }

                LaunchedEffect(cancelInteractionSource) {
                    cancelInteractionSource.interactions.collect { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                            is PressInteraction.Release -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            else -> {  }
                        }
                    }
                }

                LaunchedEffect(confirmDeleteInteractionSource) {
                    confirmDeleteInteractionSource.interactions.collect { interaction ->
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
                                    showDeleteConfirmation = false
                                },
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
                                    showDeleteConfirmation = false
                                    onDelete()
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                shapes = ButtonDefaults.shapes(),
                                interactionSource = confirmDeleteInteractionSource,
                                modifier = with(scope) {
                                    Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .animateWidth(interactionSource = confirmDeleteInteractionSource)
                                }
                            ) {
                                Text(
                                    text = stringResource(R.string.action_delete),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        },
                        menuContent = {}
                    )
                }
            }
        )
    }
}

/**
 * Bottom sheet for editing an existing container preset
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditContainerPresetBottomSheet(
    preset: ContainerPreset,
    volumeUnit: VolumeUnit,
    onDismiss: () -> Unit,
    onSave: (name: String, volume: Double) -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        EditContainerPresetSheetContent(
            preset = preset,
            volumeUnit = volumeUnit,
            onSave = onSave,
            onDelete = onDelete
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AddContainerPresetSheetContent(
    volumeUnit: VolumeUnit,
    onAdd: (name: String, volume: Double) -> Unit
) {

    val minVolumeMl = 1.0
    val maxVolumeMl = 5000.0
    val minVolumeDisplay = VolumeUnitConverter.formatValue(minVolumeMl, volumeUnit)
    val maxVolumeDisplay = VolumeUnitConverter.formatValue(maxVolumeMl, volumeUnit)
    val unitShortLabel = stringResource(volumeUnit.shortLabelResId)

    var name by remember { mutableStateOf("") }
    var volumeText by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var volumeError by remember { mutableStateOf(false) }

    // Calculate preview icon based on current volume (converted back to millilitres)
    val previewIcon = remember(volumeText, volumeUnit) {
        val volumeInUserUnit = volumeText.toDoubleOrNull() ?: 250.0
        val volumeInMl = VolumeUnitConverter.toMillilitres(volumeInUserUnit, volumeUnit)
        ContainerIconMapper.getIconForVolume(volumeInMl)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.container_add_title),
                style = MaterialTheme.typography.titleLargeEmphasized
            )

            // Icon preview
            Surface(
                shape = MaterialShapes.Cookie12Sided.toShape(),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        previewIcon.drawableRes != null -> {
                            Icon(
                                painter = painterResource(previewIcon.drawableRes),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        previewIcon.vectorIcon != null -> {
                            Icon(
                                imageVector = previewIcon.vectorIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }

        // Name field
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                nameError = false
            },
            label = { Text(stringResource(R.string.container_name_label)) },
            placeholder = { Text(stringResource(R.string.container_name_placeholder)) },
            isError = nameError,
            shape = RoundedCornerShape(50.dp),
            supportingText = if (nameError) {
                { Text(stringResource(R.string.error_name_required)) }
            } else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Volume field
        OutlinedTextField(
            value = volumeText,
            onValueChange = {
                volumeText = it
                volumeError = false
            },
            label = { Text(stringResource(R.string.container_volume_label, unitShortLabel)) },
            placeholder = { Text(stringResource(R.string.container_volume_placeholder)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (volumeUnit == VolumeUnit.MILLILITRES) KeyboardType.Number else KeyboardType.Decimal
            ),
            isError = volumeError,
            shape = RoundedCornerShape(50.dp),
            supportingText = if (volumeError) {
                { Text(stringResource(R.string.container_volume_error, minVolumeDisplay, maxVolumeDisplay)) }
            } else {
                { Text(stringResource(R.string.container_icon_auto)) }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        val haptics = LocalHapticFeedback.current
        val addInteractionSource = remember { MutableInteractionSource() }

        Button(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                val trimmedName = name.trim()
                val volumeInUserUnit = volumeText.toDoubleOrNull()

                nameError = trimmedName.isEmpty()
                val volumeInMl = volumeInUserUnit?.let {
                    VolumeUnitConverter.toMillilitres(it, volumeUnit)
                }
                volumeError = volumeInMl == null || volumeInMl <= 0 || volumeInMl > maxVolumeMl

                if (!nameError && !volumeError && volumeInMl != null) {
                    onAdd(trimmedName, volumeInMl)
                }
            },
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shapes = ButtonDefaults.shapes(),
            interactionSource = addInteractionSource,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(stringResource(R.string.container_add_title))
        }
    }
}

/**
 * Bottom sheet for adding a new container preset
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AddContainerPresetBottomSheet(
    volumeUnit: VolumeUnit,
    onDismiss: () -> Unit,
    onAdd: (name: String, volume: Double) -> Unit
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        AddContainerPresetSheetContent(volumeUnit = volumeUnit, onAdd = onAdd)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun EditContainerPresetBottomSheetPreview() {
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
                EditContainerPresetSheetContent(
                    preset = ContainerPreset.getDefaultPresets().first(),
                    volumeUnit = VolumeUnit.MILLILITRES,
                    onSave = { _, _ -> },
                    onDelete = {}
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun AddContainerPresetBottomSheetPreview() {
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
                AddContainerPresetSheetContent(
                    volumeUnit = VolumeUnit.MILLILITRES,
                    onAdd = { _, _ -> }
                )
            }
        }
    }
}

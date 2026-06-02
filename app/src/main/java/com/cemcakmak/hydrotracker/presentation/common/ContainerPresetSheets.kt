package com.cemcakmak.hydrotracker.presentation.common

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.ContainerPreset
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.utils.ContainerIconMapper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EditContainerPresetSheetContent(
    preset: ContainerPreset,
    onSave: (name: String, volume: Double) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(preset.name) }
    var volumeText by remember { mutableStateOf(preset.volume.toInt().toString()) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }
    var volumeError by remember { mutableStateOf(false) }

    // Calculate preview icon based on current volume
    val previewIcon = remember(volumeText) {
        val volume = volumeText.toDoubleOrNull() ?: preset.volume
        ContainerIconMapper.getIconForVolume(volume)
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
                text = "Edit Container",
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
            label = { Text("Container Name") },
            placeholder = { Text("e.g., Coffee Mug") },
            isError = nameError,
            supportingText = if (nameError) {
                { Text("Name is required") }
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
            label = { Text("Volume (ml)") },
            placeholder = { Text("e.g., 250") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = volumeError,
            shape = RoundedCornerShape(50.dp),
            supportingText = if (volumeError) {
                { Text("Enter a valid volume (1-5000 ml)") }
            } else {
                { Text("Icon updates automatically based on volume") }
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

        // Track pressed states for shape animation
        val isDeletePressed by deleteInteractionSource.collectIsPressedAsState()
        val isSavePressed by saveInteractionSource.collectIsPressedAsState()

        // Animate corner radius: pill (50.dp) -> rounded rectangle (16.dp) when pressed
        val deleteCornerRadius by animateDpAsState(
            targetValue = if (isDeletePressed) 16.dp else 50.dp,
            animationSpec = spring(),
            label = "deleteCornerRadius"
        )
        val saveCornerRadius by animateDpAsState(
            targetValue = if (isSavePressed) 16.dp else 50.dp,
            animationSpec = spring(),
            label = "saveCornerRadius"
        )

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
                        shape = RoundedCornerShape(deleteCornerRadius),
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
                            text = "Delete",
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
                            val volume = volumeText.toDoubleOrNull()

                            nameError = trimmedName.isEmpty()
                            volumeError = volume == null || volume <= 0 || volume > 5000

                            if (!nameError && !volumeError && volume != null) {
                                onSave(trimmedName, volume)
                            }
                        },
                        shape = RoundedCornerShape(saveCornerRadius),
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
                            text = "Save",
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                },
                menuContent = {}
            )
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete Container?") },
            text = {
                Text("Are you sure you want to delete \"${preset.name}\"? This action cannot be undone.")
            },
            confirmButton = {
                val haptics = LocalHapticFeedback.current
                val cancelInteractionSource = remember { MutableInteractionSource() }
                val confirmDeleteInteractionSource = remember { MutableInteractionSource() }
                val isCancelPressed by cancelInteractionSource.collectIsPressedAsState()
                val isConfirmDeletePressed by confirmDeleteInteractionSource.collectIsPressedAsState()

                val cancelCornerRadius by animateDpAsState(
                    targetValue = if (isCancelPressed) 16.dp else 50.dp,
                    animationSpec = spring(),
                    label = "cancelCornerRadius"
                )
                val confirmDeleteCornerRadius by animateDpAsState(
                    targetValue = if (isConfirmDeletePressed) 16.dp else 50.dp,
                    animationSpec = spring(),
                    label = "confirmDeleteCornerRadius"
                )

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
                                shape = RoundedCornerShape(cancelCornerRadius),
                                interactionSource = cancelInteractionSource,
                                modifier = with(scope) {
                                    Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .animateWidth(interactionSource = cancelInteractionSource)
                                }
                            ) {
                                Text(
                                    text = "Cancel",
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
                                shape = RoundedCornerShape(confirmDeleteCornerRadius),
                                interactionSource = confirmDeleteInteractionSource,
                                modifier = with(scope) {
                                    Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .animateWidth(interactionSource = confirmDeleteInteractionSource)
                                }
                            ) {
                                Text(
                                    text = "Delete",
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
    onDismiss: () -> Unit,
    onSave: (name: String, volume: Double) -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        EditContainerPresetSheetContent(
            preset = preset,
            onSave = onSave,
            onDelete = onDelete
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AddContainerPresetSheetContent(
    onAdd: (name: String, volume: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var volumeText by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var volumeError by remember { mutableStateOf(false) }

    // Calculate preview icon based on current volume
    val previewIcon = remember(volumeText) {
        val volume = volumeText.toDoubleOrNull() ?: 250.0
        ContainerIconMapper.getIconForVolume(volume)
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
                text = "Add Container",
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
            label = { Text("Container Name") },
            placeholder = { Text("e.g., Coffee Mug") },
            isError = nameError,
            shape = RoundedCornerShape(50.dp),
            supportingText = if (nameError) {
                { Text("Name is required") }
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
            label = { Text("Volume (ml)") },
            placeholder = { Text("e.g., 250") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = volumeError,
            shape = RoundedCornerShape(50.dp),
            supportingText = if (volumeError) {
                { Text("Enter a valid volume (1-5000 ml)") }
            } else {
                { Text("Icon is assigned automatically based on volume") }
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
                val volume = volumeText.toDoubleOrNull()

                nameError = trimmedName.isEmpty()
                volumeError = volume == null || volume <= 0 || volume > 5000

                if (!nameError && !volumeError && volume != null) {
                    onAdd(trimmedName, volume)
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
            Text("Add Container")
        }
    }
}

/**
 * Bottom sheet for adding a new container preset
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AddContainerPresetBottomSheet(
    onDismiss: () -> Unit,
    onAdd: (name: String, volume: Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        AddContainerPresetSheetContent(onAdd = onAdd)
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
                AddContainerPresetSheetContent(onAdd = { _, _ -> })
            }
        }
    }
}

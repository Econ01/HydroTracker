package com.cemcakmak.hydrotracker.presentation.home

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.data.models.ContainerPreset
import com.cemcakmak.hydrotracker.utils.ContainerIconMapper

/**
 * Bottom sheet for editing an existing container preset
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditContainerPresetBottomSheet(
    preset: ContainerPreset,
    onDismiss: () -> Unit,
    onSave: (name: String, volume: Double) -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
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
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )

                // Icon preview
                Surface(
                    shape = MaterialTheme.shapes.large,
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
                supportingText = if (volumeError) {
                    { Text("Enter a valid volume (1-5000 ml)") }
                } else {
                    { Text("Icon updates automatically based on volume") }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Delete button
                OutlinedButton(
                    onClick = { showDeleteConfirmation = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete")
                }

                // Save button
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
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save")
                }
            }
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
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Bottom sheet for adding a new container preset
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContainerPresetBottomSheet(
    onDismiss: () -> Unit,
    onAdd: (name: String, volume: Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var name by remember { mutableStateOf("") }
    var volumeText by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var volumeError by remember { mutableStateOf(false) }

    // Calculate preview icon based on current volume
    val previewIcon = remember(volumeText) {
        val volume = volumeText.toDoubleOrNull() ?: 250.0
        ContainerIconMapper.getIconForVolume(volume)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
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
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )

                // Icon preview
                Surface(
                    shape = MaterialTheme.shapes.large,
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
                supportingText = if (volumeError) {
                    { Text("Enter a valid volume (1-5000 ml)") }
                } else {
                    { Text("Icon is assigned automatically based on volume") }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Add button
            Button(
                onClick = {
                    val trimmedName = name.trim()
                    val volume = volumeText.toDoubleOrNull()

                    nameError = trimmedName.isEmpty()
                    volumeError = volume == null || volume <= 0 || volume > 5000

                    if (!nameError && !volumeError && volume != null) {
                        onAdd(trimmedName, volume)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Container")
            }
        }
    }
}

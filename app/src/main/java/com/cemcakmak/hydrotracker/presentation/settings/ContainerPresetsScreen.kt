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

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.database.repository.ContainerPresetRepository
import com.cemcakmak.hydrotracker.data.models.ContainerPreset
import com.cemcakmak.hydrotracker.data.models.UserProfile
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.presentation.common.AddContainerPresetBottomSheet
import com.cemcakmak.hydrotracker.presentation.common.EditContainerPresetBottomSheet
import com.cemcakmak.hydrotracker.presentation.common.ReorderableGroupedColumn
import com.cemcakmak.hydrotracker.presentation.common.showSuccessSnackbar
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ContainerPresetsScreen(
    containerPresetRepository: ContainerPresetRepository? = null,
    snackbarHostState: SnackbarHostState? = null,
    userProfile: UserProfile? = null,
    onNavigateBack: () -> Unit = {},
    paddingValues: PaddingValues = PaddingValues()
) {
    val context = LocalContext.current
    val volumeUnit = userProfile?.volumeUnit ?: VolumeUnit.MILLILITRES
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val containerAddedTemplate = stringResource(R.string.container_added)
    val containerUpdatedTemplate = stringResource(R.string.container_updated)
    val containerDeletedTemplate = stringResource(R.string.container_deleted)
    val containerResetDone = stringResource(R.string.container_reset_done)

    val presets by remember(containerPresetRepository) {
        containerPresetRepository?.getAllPresets() ?: flowOf(ContainerPreset.getDefaultPresets())
    }.collectAsState(initial = emptyList())

    var showAddSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var presetToEdit by remember { mutableStateOf<ContainerPreset?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    SettingsDetailScaffold(
        title = stringResource(R.string.container_presets_screen_title),
        onNavigateBack = onNavigateBack,
        paddingValues = paddingValues,
        scrollable = false
    ) {
        ReorderableGroupedColumn(
            items = presets,
            key = { it.id },
            onReorder = { newOrder ->
                containerPresetRepository?.let { repo ->
                    coroutineScope.launch { repo.reorderPresets(newOrder.map { it.id }) }
                }
            },
            onClick = { preset ->
                presetToEdit = preset
                showEditSheet = true
            },
            contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp),
            modifier = Modifier.fillMaxSize(),
            header = {
                item(key = "container_helper") {
                    Text(
                        text = stringResource(R.string.quickadd_containers_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                    )
                }
            },
            footer = {
                item(key = "container_buttons") {
                    ContainerActionButtons(
                        onReset = { showResetDialog = true },
                        onAdd = { showAddSheet = true },
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        ) { preset ->
            ContainerLeadingIcon(preset)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = preset.getFormattedVolume(context, volumeUnit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showAddSheet) {
        AddContainerPresetBottomSheet(
            volumeUnit = volumeUnit,
            onDismiss = { showAddSheet = false },
            onAdd = { name, volume ->
                showAddSheet = false
                containerPresetRepository?.let { repo ->
                    coroutineScope.launch {
                        repo.addPreset(name, volume)
                        snackbarHostState?.showSuccessSnackbar(message = containerAddedTemplate.format(name))
                    }
                }
            }
        )
    }

    if (showEditSheet) {
        presetToEdit?.let { target ->
            EditContainerPresetBottomSheet(
                preset = target,
                volumeUnit = volumeUnit,
                onDismiss = {
                    showEditSheet = false
                    presetToEdit = null
                },
                onSave = { name, volume ->
                    showEditSheet = false
                    presetToEdit = null
                    containerPresetRepository?.let { repo ->
                        coroutineScope.launch {
                            repo.updatePreset(target.id, name, volume)
                            snackbarHostState?.showSuccessSnackbar(message = containerUpdatedTemplate.format(name))
                        }
                    }
                },
                onDelete = {
                    showEditSheet = false
                    presetToEdit = null
                    containerPresetRepository?.let { repo ->
                        coroutineScope.launch {
                            repo.deletePreset(target.id)
                            snackbarHostState?.showSuccessSnackbar(message = containerDeletedTemplate.format(target.name))
                        }
                    }
                }
            )
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text(stringResource(R.string.container_reset_title)) },
            text = { Text(stringResource(R.string.container_reset_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        containerPresetRepository?.let { repo ->
                            coroutineScope.launch {
                                repo.resetToDefaults()
                                snackbarHostState?.showSuccessSnackbar(message = containerResetDone)
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.action_reset))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ContainerActionButtons(
    onReset: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val resetInteractionSource = remember { MutableInteractionSource() }
    val addInteractionSource = remember { MutableInteractionSource() }

    LaunchedEffect(resetInteractionSource) {
        resetInteractionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                is PressInteraction.Release -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                else -> {}
            }
        }
    }
    LaunchedEffect(addInteractionSource) {
        addInteractionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                is PressInteraction.Release -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                else -> {}
            }
        }
    }

    ButtonGroup(
        modifier = modifier.fillMaxWidth(),
        overflowIndicator = {}
    ) {
        val scope = this
        customItem(
            buttonGroupContent = {
                FilledTonalButton(
                    onClick = onReset,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shapes = ButtonDefaults.shapes(),
                    interactionSource = resetInteractionSource,
                    modifier = with(scope) {
                        Modifier
                            .weight(1f)
                            .height(56.dp)
                            .animateWidth(interactionSource = resetInteractionSource)
                    }
                ) {
                    Text(text = stringResource(R.string.container_reset_defaults_button), maxLines = 1, softWrap = false)
                }
            },
            menuContent = {}
        )
        customItem(
            buttonGroupContent = {
                Button(
                    onClick = onAdd,
                    shapes = ButtonDefaults.shapes(),
                    interactionSource = addInteractionSource,
                    modifier = with(scope) {
                        Modifier
                            .weight(1f)
                            .height(56.dp)
                            .animateWidth(interactionSource = addInteractionSource)
                    }
                ) {
                    Text(text = stringResource(R.string.container_add_title), maxLines = 1, softWrap = false)
                }
            },
            menuContent = {}
        )
    }
}

@Composable
private fun ContainerLeadingIcon(preset: ContainerPreset) {
    when {
        preset.iconRes != null -> Icon(
            painter = painterResource(preset.iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        preset.icon != null -> Icon(
            imageVector = preset.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        else -> Icon(
            imageVector = Icons.Default.WaterDrop,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ContainerPresetsScreenPreview() {
    HydroTrackerTheme {
        ContainerPresetsScreen()
    }
}

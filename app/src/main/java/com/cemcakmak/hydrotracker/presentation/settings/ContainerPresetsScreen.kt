package com.cemcakmak.hydrotracker.presentation.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.database.repository.ContainerPresetRepository
import com.cemcakmak.hydrotracker.data.models.ContainerPreset
import com.cemcakmak.hydrotracker.presentation.common.AddContainerPresetBottomSheet
import com.cemcakmak.hydrotracker.presentation.common.EditContainerPresetBottomSheet
import com.cemcakmak.hydrotracker.presentation.common.showSuccessSnackbar
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableColumn

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ContainerPresetsScreen(
    containerPresetRepository: ContainerPresetRepository? = null,
    snackbarHostState: SnackbarHostState? = null,
    onNavigateBack: () -> Unit = {},
    paddingValues: PaddingValues = PaddingValues()
) {
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val presets by remember(containerPresetRepository) {
        containerPresetRepository?.getAllPresets() ?: flowOf(ContainerPreset.getDefaultPresets())
    }.collectAsState(initial = emptyList())

    // Local copy so drag reordering stays smooth; re-synced whenever the source list emits.
    var items by remember(presets) { mutableStateOf(presets) }

    var showAddSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var presetToEdit by remember { mutableStateOf<ContainerPreset?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    SettingsDetailScaffold(
        title = "Container Presets",
        onNavigateBack = onNavigateBack,
        paddingValues = paddingValues
    ) {
        Column(
            modifier = Modifier.padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Quick-select containers on the home screen. Tap to edit, drag to reorder.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )

            ReorderableColumn(
                list = items,
                onSettle = { fromIndex, toIndex ->
                    val updated = items.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
                    items = updated
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    containerPresetRepository?.let { repo ->
                        coroutineScope.launch { repo.reorderPresets(updated.map { it.id }) }
                    }
                },
                onMove = { haptics.performHapticFeedback(HapticFeedbackType.ContextClick) },
                modifier = Modifier.fillMaxWidth()
            ) { index, preset, isDragging ->
                val handleModifier = Modifier.draggableHandle(
                    onDragStarted = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
                )
                ContainerPresetRow(
                    preset = preset,
                    index = index,
                    size = items.size,
                    isDragging = isDragging,
                    onClick = {
                        presetToEdit = preset
                        showEditSheet = true
                    },
                    handleModifier = handleModifier
                )
            }

            val resetInteractionSource = remember { MutableInteractionSource() }
            val addInteractionSource = remember { MutableInteractionSource() }

            // Track pressed states for shape animation
            val isResetPressed by resetInteractionSource.collectIsPressedAsState()
            val isAddPressed by addInteractionSource.collectIsPressedAsState()

            // Animate corner radius: pill (50.dp) -> rounded rectangle (16.dp) when pressed
            val resetCornerRadius by animateDpAsState(
                targetValue = if (isResetPressed) 16.dp else 50.dp,
                animationSpec = spring(),
                label = "deleteCornerRadius"
            )
            val addCornerRadius by animateDpAsState(
                targetValue = if (isAddPressed) 16.dp else 50.dp,
                animationSpec = spring(),
                label = "saveCornerRadius"
            )

            LaunchedEffect(resetInteractionSource) {
                resetInteractionSource.interactions.collect { interaction ->
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
                                showResetDialog = true
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(resetCornerRadius),
                            interactionSource = resetInteractionSource,
                            modifier = with(scope) {
                                Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .animateWidth(interactionSource = resetInteractionSource)
                            }
                        ) {
                            Text(
                                text = "Reset Defaults",
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
                                showAddSheet = true
                            },
                            shape = RoundedCornerShape(addCornerRadius),
                            interactionSource = addInteractionSource,
                            modifier = with(scope) {
                                Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .animateWidth(interactionSource = addInteractionSource)
                            }
                        ) {
                            Text(
                                text = "Add Container",
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

    if (showAddSheet) {
        AddContainerPresetBottomSheet(
            onDismiss = { showAddSheet = false },
            onAdd = { name, volume ->
                showAddSheet = false
                containerPresetRepository?.let { repo ->
                    coroutineScope.launch {
                        repo.addPreset(name, volume)
                        snackbarHostState?.showSuccessSnackbar(message = "Added \"$name\" container")
                    }
                }
            }
        )
    }

    if (showEditSheet) {
        presetToEdit?.let { target ->
            EditContainerPresetBottomSheet(
                preset = target,
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
                            snackbarHostState?.showSuccessSnackbar(message = "Updated \"$name\" container")
                        }
                    }
                },
                onDelete = {
                    showEditSheet = false
                    presetToEdit = null
                    containerPresetRepository?.let { repo ->
                        coroutineScope.launch {
                            repo.deletePreset(target.id)
                            snackbarHostState?.showSuccessSnackbar(message = "Deleted \"${target.name}\" container")
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
            title = { Text("Reset Container Presets?") },
            text = { Text("This will remove all custom containers and restore the default presets. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        containerPresetRepository?.let { repo ->
                            coroutineScope.launch {
                                repo.resetToDefaults()
                                snackbarHostState?.showSuccessSnackbar(message = "Container presets reset to defaults")
                            }
                        }
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContainerPresetRow(
    preset: ContainerPreset,
    index: Int,
    size: Int,
    isDragging: Boolean,
    onClick: () -> Unit,
    handleModifier: Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.03f else 1f,
        label = "containerDragScale"
    )

    val tonalElevation by animateDpAsState(
        targetValue = if (isDragging) 6.dp else 2.dp,
        label = "tonalElevation"
    )

    val shadowElevation by animateDpAsState(
        targetValue = if (isDragging) 6.dp else 0.dp,
        label = "shadowElevation"
    )

    val targetCorners = groupCorners(index, size)
    val topStart by animateDpAsState(targetCorners.topStart, label = "topStart")
    val topEnd by animateDpAsState(targetCorners.topEnd, label = "topEnd")
    val bottomStart by animateDpAsState(targetCorners.bottomStart, label = "bottomStart")
    val bottomEnd by animateDpAsState(targetCorners.bottomEnd, label = "bottomEnd")

    Surface(
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        onClick = onClick,
        shape = RoundedCornerShape(topStart, topEnd, bottomEnd , bottomStart),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .padding(bottom = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContainerLeadingIcon(preset)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = preset.getFormattedVolume(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.drag_handle_filled),
                contentDescription = "Reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = handleModifier.size(24.dp)
            )
        }
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

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
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.data.database.entities.CustomBeverageEntity
import com.cemcakmak.hydrotracker.data.database.repository.CustomBeverageRepository
import com.cemcakmak.hydrotracker.data.models.BeveragePreferences
import com.cemcakmak.hydrotracker.data.models.BeverageType
import com.cemcakmak.hydrotracker.data.repository.UserRepository
import com.cemcakmak.hydrotracker.presentation.common.AddCustomBeverageBottomSheet
import com.cemcakmak.hydrotracker.presentation.common.BeverageIcons
import com.cemcakmak.hydrotracker.presentation.common.EditCustomBeverageBottomSheet
import com.cemcakmak.hydrotracker.presentation.common.PresetBeverageBottomSheet
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableColumn

private sealed interface BeverageItem {
    data class Preset(val type: BeverageType) : BeverageItem
    data class Custom(val entity: CustomBeverageEntity) : BeverageItem
}

private fun BeverageItem.token(): String = when (this) {
    is BeverageItem.Preset -> type.name
    is BeverageItem.Custom -> BeveragePreferences.customToken(entity.id)
}

private fun buildVisibleItems(
    prefs: BeveragePreferences,
    customs: List<CustomBeverageEntity>
): List<BeverageItem> {
    val customById = customs.associateBy { it.id }
    val used = mutableSetOf<Long>()
    val items = mutableListOf<BeverageItem>()
    prefs.orderedVisible.forEach { token ->
        val customId = BeveragePreferences.customIdOrNull(token)
        if (customId != null) {
            customById[customId]?.let { items.add(BeverageItem.Custom(it)); used.add(customId) }
        } else {
            BeverageType.entries.find { it.name == token }
                ?.takeIf { it != BeverageType.WATER }
                ?.let { items.add(BeverageItem.Preset(it)) }
        }
    }
    customs.filter { it.id !in used }.forEach { items.add(BeverageItem.Custom(it)) }
    return items
}

private fun buildHiddenPresets(prefs: BeveragePreferences): List<BeverageType> =
    prefs.hidden.mapNotNull { name -> BeverageType.entries.find { it.name == name } }
        .filter { it != BeverageType.WATER }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BeverageTypesEditScreen(
    userRepository: UserRepository? = null,
    customBeverageRepository: CustomBeverageRepository? = null,
    onNavigateBack: () -> Unit = {},
    paddingValues: PaddingValues = PaddingValues()
) {
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val beveragePrefs by remember(userRepository) {
        userRepository?.beveragePreferences ?: flowOf(BeveragePreferences.default())
    }.collectAsState(initial = BeveragePreferences.default())

    val customs by remember(customBeverageRepository) {
        customBeverageRepository?.getAll() ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    // Local copy of the visible (reorderable) items; re-synced whenever prefs/customs emit.
    var visibleItems by remember(beveragePrefs, customs) {
        mutableStateOf(buildVisibleItems(beveragePrefs, customs))
    }
    val hiddenPresets = remember(beveragePrefs) { buildHiddenPresets(beveragePrefs) }

    var showAddSheet by remember { mutableStateOf(false) }
    var editingCustom by remember { mutableStateOf<CustomBeverageEntity?>(null) }
    var presetForSheet by remember { mutableStateOf<BeverageType?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    fun persist(newVisible: List<BeverageItem>, newHidden: Set<String>) {
        userRepository?.saveBeveragePreferences(
            BeveragePreferences(
                orderedVisible = newVisible.map { it.token() },
                hidden = newHidden
            )
        )
    }

    SettingsDetailScaffold(
        title = "Beverage Types",
        onNavigateBack = onNavigateBack,
        paddingValues = paddingValues
    ) {
        Column(
            modifier = Modifier.padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Beverages offered in quick add. Tap to edit, drag to reorder.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )

            // WATER pinned + the reorderable visible items form one continuous group.
            val visibleGroupSize = 1 + visibleItems.size
            Column {
                BeverageRow(
                    iconRes = BeverageType.WATER.iconResFilled,
                    name = BeverageType.WATER.displayName,
                    subtitle = "100% hydration",
                    shape = getGroupShape(0, visibleGroupSize)
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                ReorderableColumn(
                    list = visibleItems,
                    onSettle = { fromIndex, toIndex ->
                        val updated = visibleItems.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
                        visibleItems = updated
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        persist(updated, beveragePrefs.hidden)
                    },
                    onMove = { haptics.performHapticFeedback(HapticFeedbackType.ContextClick) },
                    modifier = Modifier.fillMaxWidth()
                ) { index, item, isDragging ->
                    val handleModifier = Modifier.draggableHandle(
                        onDragStarted = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
                    )
                    val iconRes = when (item) {
                        is BeverageItem.Preset -> item.type.iconResFilled
                        is BeverageItem.Custom -> BeverageIcons.resFor(item.entity.iconKey)
                    }
                    val name = when (item) {
                        is BeverageItem.Preset -> item.type.displayName
                        is BeverageItem.Custom -> item.entity.name
                    }
                    val multiplier = when (item) {
                        is BeverageItem.Preset -> item.type.hydrationMultiplier
                        is BeverageItem.Custom -> item.entity.hydrationMultiplier
                    }
                    BeverageRow(
                        iconRes = iconRes,
                        name = name,
                        subtitle = "${(multiplier * 100).toInt()}% hydration",
                        shape = getGroupShape(index + 1, visibleGroupSize),
                        isDragging = isDragging,
                        onClick = {
                            when (item) {
                                is BeverageItem.Preset -> presetForSheet = item.type
                                is BeverageItem.Custom -> editingCustom = item.entity
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Reorder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = handleModifier.size(24.dp)
                        )
                    }
                }
            }

            // Hidden presets
            if (hiddenPresets.isNotEmpty()) {
                Text(
                    text = "Hidden",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
                Column {
                    hiddenPresets.forEachIndexed { index, type ->
                        BeverageRow(
                            iconRes = type.iconResFilled,
                            name = type.displayName,
                            subtitle = "${(type.hydrationMultiplier * 100).toInt()}% hydration",
                            shape = getGroupShape(index, hiddenPresets.size),
                            dimmed = true,
                            onClick = { presetForSheet = type }
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = "Hidden",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
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
                                text = "Add Beverage",
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
        AddCustomBeverageBottomSheet(
            onDismiss = { showAddSheet = false },
            onAdd = { name, multiplier, iconKey ->
                showAddSheet = false
                customBeverageRepository?.let { repo ->
                    coroutineScope.launch {
                        val id = repo.addBeverage(name, multiplier, iconKey)
                        val newEntity = CustomBeverageEntity(id, name, multiplier, iconKey)
                        persist(visibleItems + BeverageItem.Custom(newEntity), beveragePrefs.hidden)
                    }
                }
            }
        )
    }

    editingCustom?.let { target ->
        EditCustomBeverageBottomSheet(
            initialName = target.name,
            initialMultiplier = target.hydrationMultiplier,
            initialIconKey = target.iconKey,
            onDismiss = { editingCustom = null },
            onSave = { name, multiplier, iconKey ->
                editingCustom = null
                customBeverageRepository?.let { repo ->
                    coroutineScope.launch { repo.updateBeverage(target.id, name, multiplier, iconKey) }
                }
            },
            onDelete = {
                editingCustom = null
                customBeverageRepository?.let { repo ->
                    coroutineScope.launch {
                        repo.deleteBeverage(target.id)
                        persist(
                            visibleItems.filterNot { it is BeverageItem.Custom && it.entity.id == target.id },
                            beveragePrefs.hidden
                        )
                    }
                }
            }
        )
    }

    presetForSheet?.let { type ->
        val isHidden = beveragePrefs.hidden.contains(type.name)
        PresetBeverageBottomSheet(
            type = type,
            isHidden = isHidden,
            onToggleHidden = {
                presetForSheet = null
                if (isHidden) {
                    persist(visibleItems + BeverageItem.Preset(type), beveragePrefs.hidden - type.name)
                } else {
                    persist(
                        visibleItems.filterNot { it is BeverageItem.Preset && it.type == type },
                        beveragePrefs.hidden + type.name
                    )
                }
            },
            onDismiss = { presetForSheet = null }
        )
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
            title = { Text("Reset Beverages?") },
            text = { Text("This restores the default beverage order, unhides all presets, and removes all custom beverages. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        coroutineScope.launch {
                            customBeverageRepository?.deleteAll()
                            userRepository?.saveBeveragePreferences(BeveragePreferences.default())
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
private fun BeverageRow(
    iconRes: Int,
    name: String,
    subtitle: String,
    shape: Shape,
    dimmed: Boolean = false,
    isDragging: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.03f else 1f,
        label = "beverageDragScale"
    )

    val tonalElevation by animateDpAsState(
        targetValue = if (isDragging) 6.dp else 2.dp,
        label = "tonalElevation"
    )

    val modifier = Modifier
        .fillMaxWidth()
        .scale(scale)
        .padding(bottom = 2.dp)
        .alpha(if (dimmed) 0.6f else 1f)

    val inner: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            trailing()
        }
    }

    if (onClick != null) {
        Surface(onClick = onClick, shape = shape, tonalElevation = tonalElevation, modifier = modifier) { inner() }
    } else {
        Surface(shape = shape, tonalElevation = tonalElevation, modifier = modifier) { inner() }
    }
}

@Preview(showBackground = true)
@Composable
fun BeverageTypesEditScreenPreview() {
    HydroTrackerTheme {
        BeverageTypesEditScreen()
    }
}

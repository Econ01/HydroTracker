package com.cemcakmak.hydrotracker.presentation.settings

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
import com.cemcakmak.hydrotracker.presentation.common.ReorderableGroupedColumn
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

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

    val visibleItems = remember(beveragePrefs, customs) { buildVisibleItems(beveragePrefs, customs) }
    val hiddenPresets = remember(beveragePrefs) { buildHiddenPresets(beveragePrefs) }
    val allItems = remember(visibleItems, hiddenPresets) {
        buildList {
            add(BeverageItem.Preset(BeverageType.WATER))   // pinned
            addAll(visibleItems)
            addAll(hiddenPresets.map { BeverageItem.Preset(it) })
        }
    }

    var showAddSheet by remember { mutableStateOf(false) }
    var editingCustom by remember { mutableStateOf<CustomBeverageEntity?>(null) }
    var presetForSheet by remember { mutableStateOf<BeverageType?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    fun persist(newVisible: List<BeverageItem>, newHidden: Set<String>) {
        coroutineScope.launch {
            userRepository?.saveBeveragePreferences(
                BeveragePreferences(
                    orderedVisible = newVisible.map { it.token() },
                    hidden = newHidden
                )
            )
        }
    }

    SettingsDetailScaffold(
        title = "Beverage Types",
        onNavigateBack = onNavigateBack,
        paddingValues = paddingValues,
        scrollable = false
    ) {
        ReorderableGroupedColumn(
            items = allItems,
            key = { it.token() },
            isPinned = { it is BeverageItem.Preset && it.type == BeverageType.WATER },
            isHidden = { it is BeverageItem.Preset && it.type.name in beveragePrefs.hidden },
            onReorder = { newVisible -> persist(newVisible, beveragePrefs.hidden) },
            onClick = { item ->
                when (item) {
                    is BeverageItem.Preset -> presetForSheet = item.type
                    is BeverageItem.Custom -> editingCustom = item.entity
                }
            },
            contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp),
            modifier = Modifier.fillMaxSize(),
            header = {
                item(key = "beverage_helper") {
                    Text(
                        text = "Beverages offered in quick add. Tap to edit, drag to reorder.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                    )
                }
            },
            footer = {
                item(key = "beverage_buttons") {
                    BeverageActionButtons(
                        onReset = { showResetDialog = true },
                        onAdd = { showAddSheet = true },
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        ) { item ->
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
                    text = "${(multiplier * 100).toInt()}% hydration",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BeverageActionButtons(
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
                    Text(text = "Reset Defaults", maxLines = 1, softWrap = false)
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
                    Text(text = "Add Beverage", maxLines = 1, softWrap = false)
                }
            },
            menuContent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BeverageTypesEditScreenPreview() {
    HydroTrackerTheme {
        BeverageTypesEditScreen()
    }
}

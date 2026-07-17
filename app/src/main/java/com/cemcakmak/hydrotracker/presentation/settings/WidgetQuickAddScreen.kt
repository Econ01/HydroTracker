package com.cemcakmak.hydrotracker.presentation.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.database.dao.MostUsedQuickAddCombo
import com.cemcakmak.hydrotracker.data.database.entities.CustomBeverageEntity
import com.cemcakmak.hydrotracker.data.database.repository.ContainerPresetRepository
import com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository
import com.cemcakmak.hydrotracker.data.models.BeverageType
import com.cemcakmak.hydrotracker.data.models.ContainerPreset
import com.cemcakmak.hydrotracker.data.models.PinnedQuickAddSlot
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.data.models.WidgetPreferences
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.utils.ContainerIconMapper
import com.cemcakmak.hydrotracker.utils.VolumeUnitConverter
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetQuickAddScreen(
    themePreferences: ThemePreferences = ThemePreferences(),
    widgetPreferences: WidgetPreferences = WidgetPreferences(),
    containerPresetRepository: ContainerPresetRepository? = null,
    customBeverages: List<CustomBeverageEntity> = emptyList(),
    waterIntakeRepository: WaterIntakeRepository? = null,
    volumeUnit: VolumeUnit = VolumeUnit.MILLILITRES,
    onWidgetPreferencesChange: (WidgetPreferences) -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var pickerSlot by remember { mutableStateOf<Int?>(null) }

    val containerPresets by remember(containerPresetRepository) {
        containerPresetRepository?.getAllPresets() ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    val topCombos by produceState(initialValue = emptyList(), waterIntakeRepository) {
        // Headroom beyond 3 so auto slots stay filled after de-duplicating against pins.
        value = waterIntakeRepository?.getTopQuickAddCombos(AUTO_FETCH_LIMIT) ?: emptyList()
    }

    fun savePin(slot: Int, pin: PinnedQuickAddSlot?) {
        val others = widgetPreferences.pinnedQuickAddSlots.filterNot { it.slot == slot }
        onWidgetPreferencesChange(
            widgetPreferences.copy(pinnedQuickAddSlots = if (pin != null) others + pin else others)
        )
    }

    SettingsDetailScaffold(
        title = stringResource(R.string.screen_widget_quickadd_title),
        onNavigateBack = onNavigateBack,
        themePreferences = themePreferences
    ) {
        Column(
            modifier = Modifier.padding(top = 24.dp)
        ) {
            // Mirror the widget's resolution: pins hold their position; auto slots take the
            // ranked combos (skipping pinned identities) in order.
            val pinnedIdentities = widgetPreferences.pinnedQuickAddSlots
                .map { Triple(it.containerName, it.volume, it.beverageName) }
                .toSet()
            val autoCombos = topCombos.filter {
                Triple(it.containerName, it.volume, it.beverage) !in pinnedIdentities
            }
            var nextAuto = 0
            (0 until SLOT_COUNT).forEach { slot ->
                val pin = widgetPreferences.pinnedQuickAddSlots.firstOrNull { it.slot == slot }
                val summary = when {
                    pin != null -> pinnedSummary(context, pin, volumeUnit)
                    else -> autoCombos.getOrNull(nextAuto)?.let { combo ->
                        nextAuto++
                        stringResource(R.string.widget_quickadd_auto_summary, comboLabel(context, combo))
                    } ?: stringResource(R.string.widget_quickadd_auto)
                }
                SettingsGroupCard(
                    index = slot,
                    size = SLOT_COUNT,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        pickerSlot = slot
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.glass_cup_filled),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.widget_quickadd_slot_title, slot + 1),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    pickerSlot?.let { slot ->
        ModalBottomSheet(onDismissRequest = { pickerSlot = null }) {
            QuickAddPicker(
                slot = slot,
                currentPin = widgetPreferences.pinnedQuickAddSlots.firstOrNull { it.slot == slot },
                containerPresets = containerPresets,
                customBeverages = customBeverages,
                volumeUnit = volumeUnit,
                onAuto = {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    savePin(slot, null)
                    pickerSlot = null
                },
                onPick = { container, beverageName ->
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    savePin(slot, PinnedQuickAddSlot(slot, container.name, container.volume, beverageName))
                    pickerSlot = null
                }
            )
        }
    }
}

private const val SLOT_COUNT = 3
private const val AUTO_FETCH_LIMIT = 10

/**
 * Bottom-sheet picker: Auto, then every container preset; choosing a container reveals the
 * beverage chips (Water first, then standard types, then customs) to finish the pin.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickAddPicker(
    slot: Int,
    currentPin: PinnedQuickAddSlot?,
    containerPresets: List<ContainerPreset>,
    customBeverages: List<CustomBeverageEntity>,
    volumeUnit: VolumeUnit,
    onAuto: () -> Unit,
    onPick: (ContainerPreset, String) -> Unit,
) {
    val context = LocalContext.current
    var pickedContainer by remember(slot) { mutableStateOf<ContainerPreset?>(null) }
    val scrollState = rememberScrollState()
    // Bring the beverage step into view once a container is picked.
    LaunchedEffect(pickedContainer) {
        if (pickedContainer != null) scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.widget_quickadd_picker_title, slot + 1),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        PickerRow(
            iconRes = R.drawable.tune_filled,
            label = stringResource(R.string.widget_quickadd_auto),
            selected = currentPin == null,
            onClick = onAuto
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        containerPresets.forEach { preset ->
            PickerRow(
                iconRes = preset.iconRes
                    ?: ContainerIconMapper.getIconByName(preset.iconType, preset.iconName)?.checkedRes
                    ?: R.drawable.glass_cup_filled,
                label = "${containerLabel(context, preset.name)} · " +
                    VolumeUnitConverter.format(context, preset.volume, volumeUnit),
                selected = pickedContainer?.name == preset.name || currentPin?.containerName == preset.name,
                onClick = { pickedContainer = preset }
            )
        }
        pickedContainer?.let { container ->
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = stringResource(R.string.widget_quickadd_beverage_header),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val beverages = BeverageType.getAllSorted().map { it.name to it.labelResId } +
                    customBeverages.map { it.name to 0 }
                beverages.forEach { (key, labelRes) ->
                    FilterChip(
                        selected = currentPin?.beverageName == key,
                        onClick = { onPick(container, key) },
                        label = {
                            Text(text = if (labelRes != 0) stringResource(labelRes) else key)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PickerRow(
    iconRes: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    SettingsGroupCard(
        index = 0,
        size = 1,
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
            onClick()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(iconRes),
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Localized container name (default presets carry a string resource; customs use raw text). */
private fun containerLabel(context: Context, name: String): String {
    val preset = ContainerPreset.getDefaultPresets().firstOrNull { it.name == name }
    return when {
        preset == null -> name
        preset.labelResId != 0 -> context.getString(preset.labelResId)
        else -> preset.name
    }
}

/** Localized beverage name, or the raw custom beverage name. */
private fun beverageLabel(context: Context, name: String): String {
    val type = BeverageType.entries.firstOrNull {
        it.name == name || it.displayName.equals(name, ignoreCase = true)
    }
    return type?.let { context.getString(it.labelResId) } ?: name
}

/** "Container · Beverage · volume" for a pinned slot. */
private fun pinnedSummary(context: Context, pin: PinnedQuickAddSlot, volumeUnit: VolumeUnit): String =
    listOf(
        containerLabel(context, pin.containerName),
        beverageLabel(context, pin.beverageName),
        VolumeUnitConverter.format(context, pin.volume, volumeUnit),
    ).joinToString(" · ")

/** Smart label for an auto combo, mirroring the widget: beverages by name, water by container. */
private fun comboLabel(context: Context, combo: MostUsedQuickAddCombo): String {
    val beverage = BeverageType.fromStringOrDefault(combo.beverage)
    return if (beverage != BeverageType.WATER) {
        context.getString(beverage.labelResId)
    } else {
        containerLabel(context, combo.containerName)
    }
}

@Preview(showBackground = true)
@Composable
private fun WidgetQuickAddScreenPreview() {
    HydroTrackerTheme {
        WidgetQuickAddScreen()
    }
}

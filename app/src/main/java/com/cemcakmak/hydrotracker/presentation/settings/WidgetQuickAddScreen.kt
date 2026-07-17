package com.cemcakmak.hydrotracker.presentation.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.database.entities.CustomBeverageEntity
import com.cemcakmak.hydrotracker.data.database.repository.ContainerPresetRepository
import com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository
import com.cemcakmak.hydrotracker.data.models.BeverageType
import com.cemcakmak.hydrotracker.data.models.ContainerPreset
import com.cemcakmak.hydrotracker.data.models.PinnedQuickAddSlot
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.data.models.WidgetPreferences
import com.cemcakmak.hydrotracker.presentation.common.BlurMorph
import com.cemcakmak.hydrotracker.presentation.common.shapes.SquircleShape
import com.cemcakmak.hydrotracker.presentation.common.sheets.BeverageIcons
import com.cemcakmak.hydrotracker.ui.theme.ExtendedColorScheme
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.ui.theme.extendedColorScheme
import com.cemcakmak.hydrotracker.utils.VolumeUnitConverter
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("UNUSED_PARAMETER")
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
    val haptics = LocalHapticFeedback.current

    val containerPresets by remember(containerPresetRepository) {
        containerPresetRepository?.getAllPresets() ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    var pickerSlot by remember { mutableStateOf<Int?>(null) }
    var pickerMode by remember { mutableStateOf<PickerMode?>(null) }

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
            modifier = Modifier.padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsSectionHeader(stringResource(R.string.widget_quickadd_slot_section_title))

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Surface(
                            shape = SquircleShape(
                                topStart = CornerSize(24.dp),
                                topEnd = CornerSize(10.dp),
                                bottomEnd = CornerSize(10.dp),
                                bottomStart = CornerSize(10.dp)
                            ),
                            tonalElevation = 2.dp,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .weight(3f)
                                .padding(bottom = 3.dp)
                        ) {
                            Text(
                                modifier = Modifier
                                    .weight(2f)
                                    .padding(horizontal = 8.dp, vertical = 16.dp),
                                text = stringResource(R.string.statistics_container_header_container),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMediumEmphasized
                            )
                        }

                        Surface(
                            shape = SquircleShape(
                                topStart = CornerSize(10.dp),
                                topEnd = CornerSize(24.dp),
                                bottomEnd = CornerSize(10.dp),
                                bottomStart = CornerSize(10.dp)
                            ),
                            tonalElevation = 2.dp,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier
                                .weight(2f)
                                .padding(bottom = 3.dp)
                        ) {
                            Text(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp, vertical = 16.dp),
                                text = stringResource(R.string.widget_quickadd_beverage_header),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMediumEmphasized
                            )
                        }
                    }

                    (0 until SLOT_COUNT).forEach { slot ->
                        val pin = widgetPreferences.pinnedQuickAddSlots.firstOrNull { it.slot == slot }
                        QuickAddSlotRow(
                            pin = pin,
                            containerPresets = containerPresets,
                            customBeverages = customBeverages,
                            isLast = slot == SLOT_COUNT - 1,
                            onContainerClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                pickerSlot = slot
                                pickerMode = PickerMode.CONTAINER
                            },
                            onBeverageClick = if (pin != null) {
                                {
                                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                    pickerSlot = slot
                                    pickerMode = PickerMode.BEVERAGE
                                }
                            } else null
                        )

                    }
                }
            }

            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    onWidgetPreferencesChange(widgetPreferences.copy(pinnedQuickAddSlots = emptyList()))
                },
                shapes = ButtonDefaults.shapes(),
                colors = ButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    disabledContainerColor = ButtonDefaults.buttonColors().disabledContainerColor,
                    disabledContentColor = ButtonDefaults.buttonColors().disabledContentColor
                ),
                contentPadding = PaddingValues(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.widget_quickadd_reset_all),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }

    pickerSlot?.let { slot ->
        when (pickerMode) {
            PickerMode.CONTAINER -> {
                val currentPin = widgetPreferences.pinnedQuickAddSlots.firstOrNull { it.slot == slot }
                ContainerPickerSheet(
                    currentContainerName = currentPin?.containerName,
                    containerPresets = containerPresets,
                    volumeUnit = volumeUnit,
                    onAuto = {
                        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        savePin(slot, null)
                        pickerSlot = null
                    },
                    onPick = { preset ->
                        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        val beverageName = currentPin?.beverageName ?: BeverageType.WATER.name
                        savePin(slot, PinnedQuickAddSlot(slot, preset.name, preset.volume, beverageName))
                        pickerSlot = null
                    },
                    onDismiss = { pickerSlot = null }
                )
            }

            PickerMode.BEVERAGE -> {
                val currentPin = widgetPreferences.pinnedQuickAddSlots.firstOrNull { it.slot == slot }
                BeveragePickerSheet(
                    currentBeverageName = currentPin?.beverageName,
                    customBeverages = customBeverages,
                    onPick = { beverageName ->
                        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        currentPin?.let { pin ->
                            savePin(slot, pin.copy(beverageName = beverageName))
                        }
                        pickerSlot = null
                    },
                    onDismiss = { pickerSlot = null }
                )
            }

            else -> {}
        }
    }
}

private const val SLOT_COUNT = 3
private const val AUTO_KEY = "__auto__"

private enum class PickerMode {
    CONTAINER,
    BEVERAGE
}

@Composable
private fun QuickAddSlotRow(
    pin: PinnedQuickAddSlot?,
    containerPresets: List<ContainerPreset>,
    customBeverages: List<CustomBeverageEntity>,
    isLast: Boolean,
    onContainerClick: () -> Unit,
    onBeverageClick: (() -> Unit)?,
) {
    val containerKey = pin?.containerName ?: AUTO_KEY
    val beverageKey = pin?.beverageName ?: AUTO_KEY

    val beverage = pin?.let { BeverageType.fromStringOrDefault(it.beverageName) }
    val (targetContainerColor, targetContentColor) = if (beverage != null) {
        MaterialTheme.extendedColorScheme.colorsFor(beverage)
    } else {
        MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }
    val beverageContainerColor by animateColorAsState(
        targetValue = targetContainerColor,
        label = "beverageContainerColor"
    )
    val beverageContentColor by animateColorAsState(
        targetValue = targetContentColor,
        label = "beverageContentColor"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // Container half
        Surface(
            onClick = onContainerClick,
            shape = slotRowShape(isLast, isLeft = true),
            color = MaterialTheme.colorScheme.secondaryContainer,
            tonalElevation = 2.dp,
            modifier = Modifier
                .weight(3f)
                .padding(bottom = 3.dp)
        ) {
            BlurMorph(targetState = containerKey) { key, blurModifier ->
                val preset = containerPresets.firstOrNull { it.name == key }
                val iconRes = preset?.iconRes ?: R.drawable.glass_cup_filled
                val label = preset?.labelResId?.takeIf { it != 0 }?.let { stringResource(it) }
                    ?: if (key == AUTO_KEY) stringResource(R.string.widget_quickadd_auto) else key

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(blurModifier)
                        .padding(horizontal = 12.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Beverage half
        Surface(
            onClick = onBeverageClick ?: {},
            shape = slotRowShape(isLast, isLeft = false),
            color = beverageContainerColor,
            tonalElevation = 2.dp,
            modifier = Modifier
                .weight(2f)
                .padding(bottom = 3.dp)
        ) {
            BlurMorph(targetState = beverageKey) { key, blurModifier ->
                val isAuto = key == AUTO_KEY
                val custom = customBeverages.firstOrNull { it.name == key }
                val label = when {
                    isAuto -> stringResource(R.string.widget_quickadd_auto)
                    custom != null -> custom.name
                    else -> stringResource(BeverageType.fromStringOrDefault(key).labelResId)
                }

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(blurModifier)
                        .padding(horizontal = 8.dp, vertical = 16.dp),
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = beverageContentColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContainerPickerSheet(
    currentContainerName: String?,
    containerPresets: List<ContainerPreset>,
    volumeUnit: VolumeUnit,
    onAuto: () -> Unit,
    onPick: (ContainerPreset) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.widget_quickadd_container_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val itemCount = containerPresets.size + 1
            SelectableOptionCard(
                index = 0,
                size = itemCount,
                selected = currentContainerName == null,
                onClick = onAuto
            ) { contentColor ->
                Text(
                    text = stringResource(R.string.widget_quickadd_auto),
                    color = contentColor
                )
            }

            containerPresets.forEachIndexed { index, preset ->
                SelectableOptionCard(
                    index = index + 1,
                    size = itemCount,
                    selected = currentContainerName == preset.name,
                    onClick = { onPick(preset) }
                ) { contentColor ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(
                                preset.iconRes ?: R.drawable.glass_cup_filled
                            ),
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        val presetLabel = preset.labelResId.takeIf { it != 0 }
                            ?.let { stringResource(it) }
                            ?: preset.name
                        Text(
                            text = "$presetLabel (${formatVolumeText(preset.volume, volumeUnit)})",
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BeveragePickerSheet(
    currentBeverageName: String?,
    customBeverages: List<CustomBeverageEntity>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    val beverages = remember(customBeverages) {
        BeverageType.getAllSorted().map { Triple(it.name, it.labelResId, it.iconResFilled) } +
            customBeverages.map { Triple(it.name, 0, BeverageIcons.resFor(it.iconKey)) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.widget_quickadd_beverage_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            beverages.forEachIndexed { index, (name, labelRes, iconRes) ->
                SelectableOptionCard(
                    index = index,
                    size = beverages.size,
                    selected = currentBeverageName == name,
                    onClick = { onPick(name) }
                ) { contentColor ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(iconRes),
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (labelRes != 0) stringResource(labelRes) else name,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun formatVolumeText(millilitres: Double, volumeUnit: VolumeUnit): String {
    val displayUnit = remember(millilitres, volumeUnit) {
        VolumeUnitConverter.selectDisplayUnit(millilitres, volumeUnit)
    }
    val value = remember(millilitres, displayUnit) {
        VolumeUnitConverter.formatValue(millilitres, displayUnit)
    }
    val unitLabel = stringResource(displayUnit.shortLabelResId)
    return "$value $unitLabel"
}

private fun ExtendedColorScheme.colorsFor(beverage: BeverageType): Pair<Color, Color> = when (beverage) {
    BeverageType.WATER -> waterContainer to onWaterContainer
    BeverageType.COFFEE -> coffeeContainer to onCoffeeContainer
    BeverageType.TEA -> teaContainer to onTeaContainer
    BeverageType.SOFT_DRINK -> softDrinkContainer to onSoftDrinkContainer
    BeverageType.ENERGY_DRINK -> energyDrinkContainer to onEnergyDrinkContainer
    BeverageType.SPORTS_DRINK -> sportsDrinkContainer to onSportsDrinkContainer
    BeverageType.ORAL_REHYDRATION_SOLUTION -> oralRehydrationSolutionContainer to onOralRehydrationSolutionContainer
    BeverageType.MILK -> milkContainer to onMilkContainer
    BeverageType.FRUIT_JUICE -> fruitJuiceContainer to onFruitJuiceContainer
}

private fun slotRowShape(isLast: Boolean, isLeft: Boolean): Shape {
    val outer = 24.dp
    val inner = 10.dp
    return SquircleShape(
        topStart = CornerSize(inner),
        topEnd = CornerSize(inner),
        bottomStart = CornerSize(if (isLast && isLeft) outer else inner),
        bottomEnd = CornerSize(if (isLast && !isLeft) outer else inner),
    )
}

@Preview(showBackground = true)
@Composable
private fun WidgetQuickAddScreenPreview() {
    HydroTrackerTheme {
        WidgetQuickAddScreen()
    }
}

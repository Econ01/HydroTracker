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

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.cemcakmak.hydrotracker.data.models.DateFormatPattern
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.models.TimeFormat
import com.cemcakmak.hydrotracker.data.models.UserProfile
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.data.models.WeekStartDay
import com.cemcakmak.hydrotracker.presentation.common.BlurMorph
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.utils.AppLocale
import com.cemcakmak.hydrotracker.utils.VolumeUnitConverter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DisplayLocaleScreen(
    themePreferences: ThemePreferences = ThemePreferences(),
    userProfile: UserProfile? = null,
    onWeekStartDayChange: (WeekStartDay) -> Unit = {},
    onTimeFormatChange: (TimeFormat) -> Unit = {},
    onDateFormatChange: (DateFormatPattern) -> Unit = {},
    onVolumeUnitChange: (VolumeUnit) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    SettingsDetailScaffold(
        title = stringResource(R.string.screen_display_locale_title),
        onNavigateBack = onNavigateBack,
        themePreferences = themePreferences
    ) {
        TimeFormatSection(
            timeFormat = themePreferences.timeFormat,
            onTimeFormatChange = onTimeFormatChange
        )

        DateAndTimeSection(
            dateFormat = themePreferences.dateFormat,
            onDateFormatChange = onDateFormatChange,
            weekStartDay = themePreferences.weekStartDay,
            onWeekStartDayChange = onWeekStartDayChange
        )

        VolumeUnitSection(
            volumeUnit = userProfile?.volumeUnit ?: VolumeUnit.MILLILITRES,
            onVolumeUnitChange = onVolumeUnitChange
        )

        LanguageSection()
    }
}

@Composable
private fun DateAndTimeSection(
    dateFormat: DateFormatPattern,
    onDateFormatChange: (DateFormatPattern) -> Unit,
    weekStartDay: WeekStartDay,
    onWeekStartDayChange: (WeekStartDay) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var showDateFormatSheet by remember { mutableStateOf(false) }
    var showWeekStartSheet by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsSectionHeader(stringResource(R.string.display_date_and_time_format_header))

        Column {
            // Date format
            SettingsGroupCard(
                index = 0,
                size = 2,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    showDateFormatSheet = true
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
                        imageVector = ImageVector.vectorResource(R.drawable.edit_calendar_filled),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.date_format_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        BlurMorph(targetState = dateFormat) { state, blurModifier ->
                            Text(
                                modifier = blurModifier,
                                text = stringResource(state.labelResId),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (showDateFormatSheet) {
                        DateFormatBottomSheet(
                            selected = dateFormat,
                            onSelect = onDateFormatChange,
                            onDismiss = { showDateFormatSheet = false }
                        )
                    }
                }
            }

            // Week start
            SettingsGroupCard(
                index = 1,
                size = 2,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    showWeekStartSheet = true
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
                        imageVector = ImageVector.vectorResource(R.drawable.view_week_filled),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.week_start_format_title),
                            style = MaterialTheme.typography.titleMedium
                        )

                        BlurMorph(targetState = weekStartDay) { state, blurModifier ->
                            Text(
                                modifier = blurModifier,
                                text = stringResource(state.labelResId),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (showWeekStartSheet) {
                        WeekStartBottomSheet(
                            selected = weekStartDay,
                            onSelect = onWeekStartDayChange,
                            onDismiss = { showWeekStartSheet = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageSection() {
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    var showLanguageSheet by remember { mutableStateOf(false) }

    // Cache the persisted tag so SharedPreferences is not read on every recomposition.
    val currentTag = remember(context) { AppLocale.currentTag(context) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionHeader(stringResource(R.string.display_language_header))
        SettingsGroupCard(
            index = 0,
            size = 1,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                showLanguageSheet = true
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
                    imageVector = ImageVector.vectorResource(R.drawable.language_filled),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                BlurMorph(
                    modifier = Modifier.weight(1f),
                    targetState = currentTag
                ) { state, blurModifier ->
                    Text(
                        modifier = blurModifier,
                        text = languageDisplayName(state),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showLanguageSheet) {
        LanguageBottomSheet(
            currentTag = currentTag,
            onDismiss = { showLanguageSheet = false }
        )
    }
}

@Composable
private fun languageDisplayName(tag: String?): String {
    return if (tag == null) {
        stringResource(R.string.language_system_default)
    } else {
        AppLocale.displayName(tag)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageBottomSheet(
    currentTag: String?,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    val haptics = LocalHapticFeedback.current
    val activity = LocalActivity.current
    val context = LocalContext.current

    // null represents "System default"; the rest are the shipped translation tags.
    val options: List<String?> = remember { listOf(null) + AppLocale.SUPPORTED_TAGS }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEachIndexed { index, tag ->
                SelectableOptionCard(
                    index = index,
                    size = options.size,
                    selected = currentTag == tag,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                        AppLocale.apply(context, tag)
                        onDismiss()
                        // Re-create so stringResource lookups pick up the new locale immediately
                        // across all API levels (deterministic regardless of configChanges).
                        activity?.recreate()
                    }
                ) { contentColor ->
                    Text(
                        text = languageDisplayName(tag),
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeekStartBottomSheet(
    selected: WeekStartDay,
    onSelect: (WeekStartDay) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    val haptics = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            WeekStartDay.entries.forEachIndexed { index, day ->
                SelectableOptionCard(
                    index = index,
                    size = WeekStartDay.entries.size,
                    selected = day == selected,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                        onSelect(day)
                        onDismiss()
                    }
                ) { contentColor ->
                    Text(
                        text = stringResource(day.labelResId),
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TimeFormatSection(
    timeFormat: TimeFormat,
    onTimeFormatChange: (TimeFormat) -> Unit
) {
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = Modifier.padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsSectionHeader(stringResource(R.string.display_time_format_header))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimeFormat.entries.forEach { format ->
                val isSelected = timeFormat == format
                ToggleButton(
                    checked = isSelected,
                    onCheckedChange = { enabled ->
                        onTimeFormatChange(format)

                        val hapticType = if (enabled) {
                            HapticFeedbackType.ToggleOn
                        } else {
                            HapticFeedbackType.ToggleOff
                        }
                        haptics.performHapticFeedback(hapticType)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(format.labelResId),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateFormatBottomSheet(
    selected: DateFormatPattern,
    onSelect: (DateFormatPattern) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    val haptics = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DateFormatPattern.entries.forEachIndexed { index, pattern ->
                SelectableOptionCard(
                    index = index,
                    size = DateFormatPattern.entries.size,
                    selected = pattern == selected,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                        onSelect(pattern)
                        onDismiss()
                    }
                ) { contentColor ->
                    Text(
                        text = stringResource(pattern.labelResId),
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun VolumeUnitSection(
    volumeUnit: VolumeUnit,
    onVolumeUnitChange: (VolumeUnit) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    var showVolumeUnitSheet by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsSectionHeader(stringResource(R.string.display_volume_unit_header))
        SettingsGroupCard(
            index = 0,
            size = 1,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                showVolumeUnitSheet = true
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
                    imageVector = ImageVector.vectorResource(R.drawable.water_drop_filled),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                BlurMorph(
                    modifier = Modifier.weight(1f),
                    targetState = volumeUnit
                ) { state, blurModifier ->
                    Column(
                        modifier = blurModifier
                    ) {
                        Text(
                            text = stringResource(state.labelResId),
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = VolumeUnitConverter.format(context, 250.0, volumeUnit),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showVolumeUnitSheet) {
        VolumeUnitBottomSheet(
            selected = volumeUnit,
            onSelect = onVolumeUnitChange,
            onDismiss = { showVolumeUnitSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VolumeUnitBottomSheet(
    selected: VolumeUnit,
    onSelect: (VolumeUnit) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    val haptics = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            VolumeUnit.entries.forEachIndexed { index, unit ->
                SelectableOptionCard(
                    index = index,
                    size = VolumeUnit.entries.size,
                    selected = unit == selected,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                        onSelect(unit)
                        onDismiss()
                    }
                ) { contentColor ->
                    Text(
                        text = stringResource(unit.labelResId),
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DisplayLocaleScreenPreview() {
    var previewPreferences by remember { mutableStateOf(ThemePreferences()) }
    var previewProfile by remember { mutableStateOf(UserProfile(
        name = "Preview",
        gender = com.cemcakmak.hydrotracker.data.models.Gender.OTHER,
        ageGroup = com.cemcakmak.hydrotracker.data.models.AgeGroup.YOUNG_ADULT_18_30,
        activityLevel = com.cemcakmak.hydrotracker.data.models.ActivityLevel.MODERATE,
        wakeUpTime = "07:00",
        sleepTime = "23:00",
        dailyWaterGoal = 2500.0,
        reminderInterval = 120
    )) }

    HydroTrackerTheme {
        DisplayLocaleScreen(
            themePreferences = previewPreferences,
            userProfile = previewProfile,
            onWeekStartDayChange = { day ->
                previewPreferences = previewPreferences.copy(weekStartDay = day)
            },
            onTimeFormatChange = { format ->
                previewPreferences = previewPreferences.copy(timeFormat = format)
            },
            onDateFormatChange = { pattern ->
                previewPreferences = previewPreferences.copy(dateFormat = pattern)
            },
            onVolumeUnitChange = { unit ->
                previewProfile = previewProfile.copy(volumeUnit = unit)
            }
        )
    }
}

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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.ActivityLevel
import com.cemcakmak.hydrotracker.data.models.AgeGroup
import com.cemcakmak.hydrotracker.data.models.DarkModePreference
import com.cemcakmak.hydrotracker.data.models.DayEndMode
import com.cemcakmak.hydrotracker.data.models.Gender
import com.cemcakmak.hydrotracker.data.models.ReminderIntervalMode
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.models.UserProfile
import com.cemcakmak.hydrotracker.presentation.common.BlurMorph
import com.cemcakmak.hydrotracker.presentation.common.rememberAnimatedDouble
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.utils.DateTimeFormatters
import com.cemcakmak.hydrotracker.utils.VolumeUnitConverter
import com.cemcakmak.hydrotracker.utils.WaterCalculator

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReminderIntervalScreen(
    userProfile: UserProfile? = null,
    themePreferences: ThemePreferences = ThemePreferences(),
    onNavigateBack: () -> Unit = {},
    onUserProfileUpdate: (UserProfile) -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current

    SettingsDetailScaffold(
        title = stringResource(R.string.screen_reminder_schedule_title),
        onNavigateBack = onNavigateBack,
        themePreferences = themePreferences
    ) {
        if (userProfile != null) {
            val previewText = buildPreviewText(userProfile, themePreferences)

            // Preview card
            ReminderSchedulePreviewCard(
                themePreferences = themePreferences,
                previewText = previewText,
                reminderIntervalMode = userProfile.reminderIntervalMode,
                dayEndMode = userProfile.dayEndMode
            )

            DayEndSection(
                currentMode = userProfile.dayEndMode,
                onModeChange = { newMode ->
                    haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    onUserProfileUpdate(userProfile.copy(dayEndMode = newMode))
                }
            )

            // Mode selector
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsSectionHeader(stringResource(R.string.reminder_mode_header))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReminderIntervalMode.entries.forEach { mode ->
                        val isSelected = userProfile.reminderIntervalMode == mode

                        ToggleButton(
                            checked = isSelected,
                            onCheckedChange = {
                                haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                                val newInterval = WaterCalculator.calculateReminderInterval(
                                    wakeUpTime = userProfile.wakeUpTime,
                                    sleepTime = userProfile.sleepTime,
                                    dailyGoal = userProfile.dailyWaterGoal,
                                    reminderIntervalMode = mode,
                                    customReminderInterval = userProfile.customReminderInterval
                                )
                                onUserProfileUpdate(
                                    userProfile.copy(
                                        reminderIntervalMode = mode,
                                        reminderInterval = newInterval
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Crossfade(
                                    targetState = isSelected,
                                    animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
                                    label = "modeIcon_${mode.name}"
                                ) { selected ->
                                    Icon(
                                        imageVector = when (mode) {
                                            ReminderIntervalMode.AUTOMATIC -> if (selected) ImageVector.vectorResource(
                                                R.drawable.automation_filled
                                            ) else ImageVector.vectorResource(R.drawable.automation)

                                            ReminderIntervalMode.CUSTOM -> if (selected) ImageVector.vectorResource(
                                                R.drawable.tune_filled
                                            ) else ImageVector.vectorResource(R.drawable.tune)
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Text(
                                    text = stringResource(mode.labelResId),
                                    style = if (isSelected) {
                                        MaterialTheme.typography.labelLargeEmphasized
                                    } else {
                                        MaterialTheme.typography.labelLarge
                                    }
                                )
                            }
                        }
                    }
                }

                // Custom interval picker
                AnimatedVisibility(
                    visible = userProfile.reminderIntervalMode == ReminderIntervalMode.CUSTOM,
                    enter = fadeIn(animationSpec = MaterialTheme.motionScheme.slowSpatialSpec()) +
                            expandVertically(animationSpec = MaterialTheme.motionScheme.slowSpatialSpec()),
                    exit = fadeOut(animationSpec = MaterialTheme.motionScheme.slowSpatialSpec()) +
                           shrinkVertically(animationSpec = MaterialTheme.motionScheme.slowSpatialSpec())
                ) {
                    CustomIntervalPicker(
                        currentInterval = userProfile.customReminderInterval,
                        onIntervalChange = { newInterval ->
                            onUserProfileUpdate(
                                userProfile.copy(
                                    customReminderInterval = newInterval,
                                    reminderInterval = newInterval
                                )
                            )
                        }
                    )
                }
            }


        }
    }
}

@Composable
private fun ReminderSchedulePreviewCard(
    themePreferences: ThemePreferences,
    previewText: String,
    reminderIntervalMode: ReminderIntervalMode,
    dayEndMode: DayEndMode
) {
    val isDark = when (themePreferences.darkMode) {
        DarkModePreference.DARK -> true
        DarkModePreference.LIGHT -> false
        DarkModePreference.SYSTEM -> isSystemInDarkTheme()
    }

    val border = if (themePreferences.usePureBlack && isDark) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    } else {
        null
    }

    val modeExplanation = when (reminderIntervalMode) {
        ReminderIntervalMode.AUTOMATIC -> stringResource(R.string.reminder_mode_auto_explain)
        ReminderIntervalMode.CUSTOM -> stringResource(R.string.reminder_mode_custom_explain)
    }

    val dayEndExplanation = when (dayEndMode) {
        DayEndMode.SLEEP_TIME -> stringResource(R.string.reminder_dayend_sleep_explain)
        DayEndMode.MIDNIGHT -> stringResource(R.string.reminder_dayend_midnight_explain)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        shape = RoundedCornerShape(size = 30.dp),
        tonalElevation = 2.dp,
        border = border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BlurMorph(targetState = previewText) { text, blurModifier ->
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(blurModifier),
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))

                Text(
                    text = stringResource(R.string.reminder_mode_header),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.labelMediumEmphasized,
                    color = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            BlurMorph(targetState = modeExplanation) { text, blurModifier ->
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(blurModifier),
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))

                Text(
                    text = stringResource(R.string.reminder_day_end_divider),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.labelMediumEmphasized,
                    color = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            BlurMorph(targetState = dayEndExplanation) { text, blurModifier ->
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(blurModifier),
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DayEndSection(
    currentMode: DayEndMode,
    onModeChange: (DayEndMode) -> Unit
) {
    val haptics = LocalHapticFeedback.current

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsSectionHeader(stringResource(R.string.reminder_day_end_header))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DayEndMode.entries.forEach { mode ->
                val isSelected = currentMode == mode

                ToggleButton(
                    checked = isSelected,
                    onCheckedChange = {
                        onModeChange(mode)
                        haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Crossfade(
                            targetState = isSelected,
                            animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
                            label = "modeIcon_${mode.getDisplayName()}"
                        ) { selected ->
                            Icon(
                                imageVector = when (mode) {
                                    DayEndMode.SLEEP_TIME -> if (selected) ImageVector.vectorResource(
                                        R.drawable.bed_filled
                                    ) else ImageVector.vectorResource(R.drawable.bed)

                                    DayEndMode.MIDNIGHT -> if (selected) ImageVector.vectorResource(
                                        R.drawable.bedtime_filled
                                    ) else ImageVector.vectorResource(R.drawable.bedtime)
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = stringResource(mode.labelResId),
                            style = if (isSelected) {
                                MaterialTheme.typography.labelLargeEmphasized
                            } else {
                                MaterialTheme.typography.labelLarge
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomIntervalPicker(
    currentInterval: Int,
    onIntervalChange: (Int) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var sliderPosition by remember { mutableIntStateOf(currentInterval) }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val animatedMinutes = rememberAnimatedDouble(
            targetValue = sliderPosition.toDouble(),
            hapticsEnabled = false,
            animationSpec = tween(durationMillis = 150, easing = EaseInOut)
        )

        Text(
            text = formatInterval(animatedMinutes.toInt()),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        val startIcon = rememberVectorPainter(ImageVector.vectorResource(R.drawable.sunny_filled))
        val endIcon = rememberVectorPainter(ImageVector.vectorResource(R.drawable.bedtime_filled))
        Slider(
            value = sliderPosition.toFloat(),
            onValueChange = {
                val stepSize = 10f
                val rawValue = it.coerceIn(1f, 480f)
                val snappedValue = kotlin.math.round(rawValue / stepSize) * stepSize
                val newValue = snappedValue.toInt().coerceIn(1, 480)
                if (newValue != sliderPosition) {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    sliderPosition = newValue
                }
            },
            onValueChangeFinished = {
                onIntervalChange(sliderPosition)
            },
            valueRange = 1f..480f,
            steps = 0,
            track = { sliderState ->
                val iconSize = DpSize(25.dp, 25.dp)
                val iconPadding = 10.dp
                val thumbTrackGapSize = 6.dp
                val activeIconColor = SliderDefaults.colors().activeTickColor
                val inactiveIconColor = SliderDefaults.colors().inactiveTickColor
                val trackIconStart: DrawScope.(Offset, Color) -> Unit = { offset, color ->
                    translate(offset.x + iconPadding.toPx(), offset.y) {
                        with(startIcon) {
                            draw(iconSize.toSize(), colorFilter = ColorFilter.tint(color))
                        }
                    }
                }
                val trackIconEnd: DrawScope.(Offset, Color) -> Unit = { offset, color ->
                    translate(offset.x - iconPadding.toPx() - iconSize.toSize().width, offset.y) {
                        with(endIcon) {
                            draw(iconSize.toSize(), colorFilter = ColorFilter.tint(color))
                        }
                    }
                }
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(36.dp).drawWithContent {
                        drawContent()

                        val yOffset = size.height / 2 - iconSize.toSize().height / 2
                        val activeTrackStart = 0f
                        val activeTrackEnd = size.width * sliderState.coercedValueAsFraction - thumbTrackGapSize.toPx()
                        val inactiveTrackStart = activeTrackEnd + thumbTrackGapSize.toPx() * 2
                        val inactiveTrackEnd = size.width

                        val activeTrackWidth = activeTrackEnd - activeTrackStart
                        val inactiveTrackWith = inactiveTrackEnd - inactiveTrackStart
                        if (
                            iconSize.toSize().width * 2 < activeTrackWidth - iconPadding.toPx() * 2
                        ) {
                            trackIconStart(Offset(activeTrackStart, yOffset), activeIconColor)
                            trackIconEnd(Offset(activeTrackEnd, yOffset), activeIconColor)
                        }
                        if (
                            iconSize.toSize().width * 2 < inactiveTrackWith - iconPadding.toPx() * 2
                        ) {
                            trackIconStart(Offset(inactiveTrackStart, yOffset), inactiveIconColor)
                            trackIconEnd(Offset(inactiveTrackEnd, yOffset), inactiveIconColor)
                        }
                    },
                    trackCornerSize = 12.dp,
                    drawStopIndicator = null,
                    thumbTrackGapSize = thumbTrackGapSize
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.reminder_slider_min),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.reminder_slider_max),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun buildPreviewText(userProfile: UserProfile, themePreferences: ThemePreferences): String {
    val context = LocalContext.current
    return when (userProfile.reminderIntervalMode) {
        ReminderIntervalMode.AUTOMATIC -> {
            val awakeHours = WaterCalculator.calculateAwakeHours(
                userProfile.wakeUpTime,
                userProfile.sleepTime
            )
            val glasses = (userProfile.dailyWaterGoal / 300.0).toInt()
            stringResource(
                R.string.reminder_preview_auto,
                VolumeUnitConverter.format(context, userProfile.dailyWaterGoal, userProfile.volumeUnit),
                glasses,
                awakeHours.toInt(),
                userProfile.reminderInterval
            )
        }
        ReminderIntervalMode.CUSTOM -> stringResource(
            R.string.reminder_preview_custom,
            formatInterval(userProfile.customReminderInterval),
            DateTimeFormatters.formatTimeString(context, userProfile.wakeUpTime, themePreferences.timeFormat),
            DateTimeFormatters.formatTimeString(context, userProfile.sleepTime, themePreferences.timeFormat)
        )
    }
}

@Composable
private fun formatInterval(minutes: Int): String {
    return when {
        minutes < 60 -> pluralStringResource(R.plurals.interval_every_minutes, minutes, minutes)
        minutes == 60 -> stringResource(R.string.interval_every_hour)
        minutes % 60 == 0 -> pluralStringResource(R.plurals.interval_every_hours, minutes / 60, minutes / 60)
        else -> stringResource(R.string.interval_every_h_m, minutes / 60, minutes % 60)
    }
}

@Preview(showBackground = true)
@Composable
fun ReminderIntervalScreenPreview() {
    var previewProfile by remember {
        mutableStateOf(
            UserProfile(
                name = "Preview",
                gender = Gender.MALE,
                ageGroup = AgeGroup.ADULT_31_50,
                activityLevel = ActivityLevel.MODERATE,
                wakeUpTime = "07:00",
                sleepTime = "23:00",
                dailyWaterGoal = 2500.0,
                reminderInterval = 60,
                isOnboardingCompleted = true
            )
        )
    }

    HydroTrackerTheme {
        ReminderIntervalScreen(
            userProfile = previewProfile,
            onUserProfileUpdate = { previewProfile = it }
        )
    }

}

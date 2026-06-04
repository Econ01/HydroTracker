package com.cemcakmak.hydrotracker.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.data.models.ActivityLevel
import com.cemcakmak.hydrotracker.data.models.AgeGroup
import com.cemcakmak.hydrotracker.data.models.DarkModePreference
import com.cemcakmak.hydrotracker.data.models.DayEndMode
import com.cemcakmak.hydrotracker.data.models.Gender
import com.cemcakmak.hydrotracker.data.models.ReminderIntervalMode
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.models.UserProfile
import com.cemcakmak.hydrotracker.presentation.common.BlurMorph
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
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
        title = "Reminder Schedule",
        onNavigateBack = onNavigateBack
    ) {
        if (userProfile != null) {
            val previewText = buildPreviewText(userProfile)

            // Preview card
            ReminderSchedulePreviewCard(
                themePreferences = themePreferences,
                previewText = previewText
            )

            // Mode selector
            SettingsSectionHeader("Mode")
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
                        Text(
                            text = mode.getDisplayName(),
                            style = if (isSelected) {
                                MaterialTheme.typography.labelLargeEmphasized
                            } else {
                                MaterialTheme.typography.labelLarge
                            }
                        )
                    }
                }
            }

            // Custom interval picker
            if (userProfile.reminderIntervalMode == ReminderIntervalMode.CUSTOM) {
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

            DayEndSection(
                currentMode = userProfile.dayEndMode,
                onModeChange = { newMode ->
                    haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    onUserProfileUpdate(userProfile.copy(dayEndMode = newMode))
                }
            )
        }
    }
}

@Composable
private fun ReminderSchedulePreviewCard(
    themePreferences: ThemePreferences,
    previewText: String
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
            BlurMorph(targetState = previewText) { previewText, blurModifier ->
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(blurModifier),
                    text = previewText,
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
                    text = "Mode",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.labelMediumEmphasized,
                    color = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            BlurMorph(targetState = previewText) { previewText, blurModifier ->
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(blurModifier),
                    text = previewText,
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
                    text = "Day End",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.labelMediumEmphasized,
                    color = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            BlurMorph(targetState = previewText) { previewText, blurModifier ->
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(blurModifier),
                    text = previewText,
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
        modifier = Modifier.padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsSectionHeader("Day end")
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = mode.getDisplayName(),
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

@Composable
private fun CustomIntervalPicker(
    currentInterval: Int,
    onIntervalChange: (Int) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var sliderPosition by remember { mutableIntStateOf(currentInterval) }

    Column(
        modifier = Modifier.padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsSectionHeader("Custom interval")

        Text(
            text = formatInterval(sliderPosition),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Slider(
            value = sliderPosition.toFloat(),
            onValueChange = {
                val newValue = it.toInt()
                if (newValue != sliderPosition) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    sliderPosition = newValue
                }
            },
            onValueChangeFinished = {
                onIntervalChange(sliderPosition)
            },
            valueRange = 1f..480f,
            steps = 478,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "1 min",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "8 hours",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun buildPreviewText(userProfile: UserProfile): String {
    return when (userProfile.reminderIntervalMode) {
        ReminderIntervalMode.AUTOMATIC -> {
            val awakeHours = WaterCalculator.calculateAwakeHours(
                userProfile.wakeUpTime,
                userProfile.sleepTime
            )
            val glasses = (userProfile.dailyWaterGoal / 300.0).toInt()
            val interval = userProfile.reminderInterval
            "Based on your ${WaterCalculator.formatWaterAmount(userProfile.dailyWaterGoal)} goal, " +
                "you'll get about $glasses reminders (one per glass) spread across your " +
                "${awakeHours.toInt()} active hours. Roughly every $interval minutes."
        }
        ReminderIntervalMode.CUSTOM -> {
            "You'll receive a reminder ${formatInterval(userProfile.customReminderInterval)} " +
                "between ${userProfile.wakeUpTime} and ${userProfile.sleepTime}."
        }
    }
}

private fun formatInterval(minutes: Int): String {
    return when {
        minutes < 60 -> "every $minutes minutes"
        minutes == 60 -> "every hour"
        minutes % 60 == 0 -> "every ${minutes / 60} hours"
        else -> {
            val h = minutes / 60
            val m = minutes % 60
            "every ${h}h ${m}m"
        }
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

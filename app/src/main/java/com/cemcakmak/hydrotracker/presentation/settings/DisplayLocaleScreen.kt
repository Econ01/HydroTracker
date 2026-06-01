package com.cemcakmak.hydrotracker.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.Crossfade
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.models.WeekStartDay
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DisplayLocaleScreen(
    themePreferences: ThemePreferences = ThemePreferences(),
    onWeekStartDayChange: (WeekStartDay) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    paddingValues: PaddingValues = PaddingValues()
) {
    SettingsDetailScaffold(
        title = "Display & Locale",
        onNavigateBack = onNavigateBack,
        paddingValues = paddingValues
    ) {
        WeekStartSection(
            weekStartDay = themePreferences.weekStartDay,
            onWeekStartDayChange = onWeekStartDayChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WeekStartSection(
    weekStartDay: WeekStartDay,
    onWeekStartDayChange: (WeekStartDay) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Column(
        modifier = Modifier.padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsSectionHeader("Week start")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WeekStartDay.entries.forEach { day ->
                val isSelected = weekStartDay == day
                ToggleButton(
                    checked = isSelected,
                    onCheckedChange = {
                        onWeekStartDayChange(day)
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
                            label = "weekStartIcon_${day.name}"
                        ) { selected ->
                            Icon(
                                imageVector = when (day) {
                                    WeekStartDay.SUNDAY -> if (selected) ImageVector.vectorResource(R.drawable.weekend_filled) else ImageVector.vectorResource(R.drawable.weekend)
                                    WeekStartDay.MONDAY -> if (selected) ImageVector.vectorResource(R.drawable.calendar_today_filled) else ImageVector.vectorResource(R.drawable.calendar_today)
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = when (day) {
                                WeekStartDay.SUNDAY -> "Sunday"
                                WeekStartDay.MONDAY -> "Monday"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DisplayLocaleScreenPreview() {
    var previewPreferences by remember { mutableStateOf(ThemePreferences()) }

    HydroTrackerTheme {
        DisplayLocaleScreen(
            themePreferences = previewPreferences,
            onWeekStartDayChange = { day ->
                previewPreferences = previewPreferences.copy(weekStartDay = day)
            }
        )
    }
}

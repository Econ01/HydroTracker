package com.cemcakmak.hydrotracker.presentation.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.models.WeekStartDay
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.utils.AppLocale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DisplayLocaleScreen(
    themePreferences: ThemePreferences = ThemePreferences(),
    onWeekStartDayChange: (WeekStartDay) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    SettingsDetailScaffold(
        title = stringResource(R.string.screen_display_locale_title),
        onNavigateBack = onNavigateBack
    ) {
        LanguageSection()
        WeekStartSection(
            weekStartDay = themePreferences.weekStartDay,
            onWeekStartDayChange = onWeekStartDayChange
        )
    }
}

/**
 * In-app language picker. The option list grows automatically as languages are added to [AppLocale.SUPPORTED_TAGS].
 */
@Composable
private fun LanguageSection() {
    val haptics = LocalHapticFeedback.current
    val activity = LocalActivity.current
    val context = LocalContext.current
    val currentTag = AppLocale.currentTag(context)

    // null represents "System default"; the rest are the shipped translation tags.
    val options: List<String?> = remember { listOf(null) + AppLocale.SUPPORTED_TAGS }

    Column(
        modifier = Modifier.padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsSectionHeader(stringResource(R.string.display_language_header))
        Text(
            text = stringResource(R.string.display_language_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
        Column {
            options.forEachIndexed { index, tag ->
                val label = if (tag == null) {
                    stringResource(R.string.language_system_default)
                } else {
                    AppLocale.displayName(tag)
                }
                SelectableOptionCard(
                    index = index,
                    size = options.size,
                    selected = currentTag == tag,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                        AppLocale.apply(context, tag)
                        // Re-create so stringResource lookups pick up the new locale immediately
                        // across all API levels (deterministic regardless of configChanges).
                        activity?.recreate()
                    }
                ) { contentColor ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor
                    )
                }
            }
        }
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
        SettingsSectionHeader(stringResource(R.string.display_week_start_header))
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
                            text = stringResource(day.labelResId),
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

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
        WeekStartSection(
            weekStartDay = themePreferences.weekStartDay,
            onWeekStartDayChange = onWeekStartDayChange
        )
        LanguageSection()
    }
}

/**
 * In-app language picker styled as a single summary row that opens a bottom sheet.
 * The option list grows automatically as languages are added to [AppLocale.SUPPORTED_TAGS].
 */
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
                Text(
                    text = languageDisplayName(currentTag),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
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

@Composable
private fun WeekStartSection(
    weekStartDay: WeekStartDay,
    onWeekStartDayChange: (WeekStartDay) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var showWeekStartSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsSectionHeader(stringResource(R.string.display_week_start_header))
        SettingsGroupCard(
            index = 0,
            size = 1,
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
                    imageVector = ImageVector.vectorResource(R.drawable.calendar_today_filled),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = stringResource(weekStartDay.labelResId),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showWeekStartSheet) {
        WeekStartBottomSheet(
            selected = weekStartDay,
            onSelect = onWeekStartDayChange,
            onDismiss = { showWeekStartSheet = false }
        )
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

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

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import com.cemcakmak.hydrotracker.presentation.common.BlurMorph
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.utils.AppLocale
import org.json.JSONObject
import androidx.core.net.toUri

/** Per-language translation and approval percentages. */
private data class LanguageProgress(
    val translated: Int,
    val approved: Int
)

/**
 * Immutable snapshot of per-language translation progress loaded from
 * `assets/translation_progress.json`.
 */
private data class TranslationProgress(
    val languages: Map<String, LanguageProgress>
)

/**
 * Reads `translation_progress.json` from the assets directory and returns a
 * [TranslationProgress] instance. Returns `null` if the file is missing or malformed.
 */
private fun loadTranslationProgress(context: android.content.Context): TranslationProgress? {
    return try {
        val json = context.assets.open("translation_progress.json")
            .bufferedReader()
            .use { it.readText() }
        val root = JSONObject(json)
        val languages = root.getJSONObject("languages")
        val map = mutableMapOf<String, LanguageProgress>()
        for (key in languages.keys()) {
            val entry = languages.getJSONObject(key)
            map[key] = LanguageProgress(
                translated = entry.getInt("translated"),
                approved = entry.getInt("approved")
            )
        }
        TranslationProgress(languages = map)
    } catch (_: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(
    themePreferences: ThemePreferences = ThemePreferences(),
    onNavigateBack: () -> Unit = {}
) {
    SettingsDetailScaffold(
        title = stringResource(R.string.screen_language_title),
        onNavigateBack = onNavigateBack,
        themePreferences = themePreferences
    ) {
        LanguageSection()

        CrowdinSection()

        TranslationProgressSection()
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
                    imageVector = ImageVector.vectorResource(R.drawable.keyboard_arrow_up_filled),
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
private fun CrowdinSection() {
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionHeader(stringResource(R.string.language_contribute_title))
        SettingsGroupCard(
            index = 0,
            size = 1,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    "https://crowdin.com/project/hydrotracker".toUri()
                )
                context.startActivity(intent)
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
                    imageVector = ImageVector.vectorResource(R.drawable.crowdin_mono),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.language_crowdin_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.language_crowdin_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.open_in_new_filled),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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

@Composable
private fun TranslationProgressSection() {
    val context = LocalContext.current
    val progress = remember(context) { loadTranslationProgress(context) } ?: return

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionHeader(stringResource(R.string.language_progress_header))



        val sortedLanguages = remember(progress) {
            progress.languages.entries.sortedByDescending { it.value.translated }
        }

        Column {
            sortedLanguages.forEachIndexed { index, (tag, langProgress) ->
                SettingsGroupCard(
                    index = index,
                    size = sortedLanguages.size
                ) {
                    LanguageProgressRow(
                        tag = tag,
                        translatedPercent = langProgress.translated,
                        approvedPercent = langProgress.approved
                    )
                }
            }
        }

        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, alignment = Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(
                colour = MaterialTheme.colorScheme.primary,
                label = stringResource(R.string.language_progress_approved)
            )
            LegendItem(
                colour = MaterialTheme.colorScheme.primaryContainer,
                label = stringResource(R.string.language_progress_translated)
            )
        }
    }
}

@Composable
private fun LegendItem(colour: Color, label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(colour)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LanguageProgressRow(
    tag: String,
    translatedPercent: Int,
    approvedPercent: Int
) {
    val translatedFraction = translatedPercent / 100f
    val approvedFraction = approvedPercent / 100f

    // Animate progress bars on first appearance.
    var targetTranslated by remember { mutableFloatStateOf(0f) }
    var targetApproved by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(translatedFraction) { targetTranslated = translatedFraction }
    LaunchedEffect(approvedFraction) { targetApproved = approvedFraction }

    val animatedTranslated by animateFloatAsState(
        targetValue = targetTranslated,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "translated_$tag"
    )
    val animatedApproved by animateFloatAsState(
        targetValue = targetApproved,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "approved_$tag"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = AppLocale.displayName(tag),
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "$translatedPercent%",
                style = MaterialTheme.typography.bodyLargeEmphasized,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }


        Box(modifier = Modifier.fillMaxWidth()) {
            // Bottom layer: translated progress
            LinearProgressIndicator(
                progress = { animatedTranslated },
                modifier = Modifier
                    .fillMaxWidth()
                    .size(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
            // Top layer: approved progress
            LinearProgressIndicator(
                progress = { animatedApproved },
                modifier = Modifier
                    .fillMaxWidth()
                    .size(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent,
                strokeCap = StrokeCap.Round
            )
        }
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

@Preview(showBackground = true)
@Composable
private fun LanguageScreenPreview() {
    HydroTrackerTheme {
        LanguageScreen()
    }
}

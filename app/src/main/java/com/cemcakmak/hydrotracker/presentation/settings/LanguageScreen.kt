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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.presentation.common.BlurMorph
import com.cemcakmak.hydrotracker.presentation.common.shapes.PillShape
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.ui.theme.extendedColorScheme
import com.cemcakmak.hydrotracker.utils.AppLocale
import org.json.JSONObject
import java.text.NumberFormat

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

/** A language reported against a Crowdin contributor's activity. */
private data class CrowdinLanguage(
    val id: String,
    val name: String
)

/** A Crowdin contributor and their all-time translation and proofreading activity. */
private data class CrowdinContributor(
    val username: String,
    val languages: List<CrowdinLanguage>,
    val translatedWords: Int,
    val approvedWords: Int
)

private enum class CrowdinContributionType {
    TRANSLATION,
    PROOFREADING,
    OTHER
}

private fun parseContributorLanguages(entry: JSONObject): List<CrowdinLanguage> {
    val languages = entry.optJSONArray("languages") ?: return emptyList()
    return buildList {
        for (index in 0 until languages.length()) {
            val language = languages.getJSONObject(index)
            val id = language.optString("id")
            if (id.isNotBlank()) {
                add(
                    CrowdinLanguage(
                        id = id,
                        name = language.optString("name").ifBlank { id }
                    )
                )
            }
        }
    }.distinctBy { it.id }
}

/**
 * Reads every contributor from `assets/translation_contributors.json`. Newer fields are optional
 * so the previous asset remains usable while Crowdin's next export is pending.
 */
private fun loadCrowdinContributors(
    context: android.content.Context
): List<CrowdinContributor> {
    return try {
        val json = context.assets.open("translation_contributors.json")
            .bufferedReader()
            .use { it.readText() }
        val contributors = JSONObject(json).getJSONArray("contributors")
        buildList {
            for (index in 0 until contributors.length()) {
                val entry = contributors.getJSONObject(index)
                add(
                    CrowdinContributor(
                        username = entry.optString("username").ifBlank {
                            entry.getString("name")
                        },
                        languages = parseContributorLanguages(entry),
                        translatedWords = entry.optInt("translatedWords"),
                        approvedWords = entry.optInt("approvedWords")
                    )
                )
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}

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

        CrowdinContributorSections()
    }
}

@Composable
private fun LanguageSection() {
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    var showLanguageSheet by remember { mutableStateOf(false) }
    val isPreview = LocalInspectionMode.current
    val progress = remember(context) { loadTranslationProgress(context) }

    // Cache the persisted tag so SharedPreferences is not read on every recomposition.
    val currentTag = remember(context) { AppLocale.currentTag(context) }
    val effectiveTag = remember(currentTag) { resolveEffectiveTag(currentTag) }

    val showDisclaimer = remember(isPreview, effectiveTag, progress) {
        if (isPreview) return@remember true

        val approved = effectiveTag?.let { progress?.languages?.get(it)?.approved }
        effectiveTag != null &&
                effectiveTag != "en" &&
                approved != null &&
                approved <= 70
    }

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

        AnimatedVisibility(visible = showDisclaimer) {
            Text(
                text = stringResource(R.string.language_translation_disclaimer),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.extendedColorScheme.warning
            )
        }
    }

    if (showLanguageSheet) {
        LanguageBottomSheet(
            currentTag = currentTag,
            onDismiss = { showLanguageSheet = false }
        )
    }
}

/**
 * Returns the supported language tag that best matches the effective locale.
 * For an explicit per-app selection this is just [currentTag]. For "System default"
 * it is derived from [Locale.getDefault()].
 */
private fun resolveEffectiveTag(currentTag: String?): String? {
    if (currentTag != null) return currentTag

    val systemLocale = java.util.Locale.getDefault()
    if (systemLocale.language == "en") return "en"

    return AppLocale.SUPPORTED_TAGS.firstOrNull { tag ->
        java.util.Locale.forLanguageTag(tag).language == systemLocale.language
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
private fun CrowdinContributorSections() {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val contributors = remember(context, isPreview) {
        if (isPreview) previewCrowdinContributors else loadCrowdinContributors(context)
    }
    if (contributors.isEmpty()) return

    CrowdinContributorSectionsContent(contributors = contributors)
}

@Composable
private fun CrowdinContributorSectionsContent(
    contributors: List<CrowdinContributor>
) {
    val translators = remember(contributors) {
        contributors
            .filter { it.translatedWords > 0 }
            .sortedWith(
                compareByDescending<CrowdinContributor> { it.translatedWords }
                    .thenBy { it.username.lowercase() }
            )
    }
    val proofreaders = remember(contributors) {
        contributors
            .filter { it.approvedWords > 0 }
            .sortedWith(
                compareByDescending<CrowdinContributor> { it.approvedWords }
                    .thenBy { it.username.lowercase() }
            )
    }
    val otherContributors = remember(contributors) {
        contributors
            .filter { it.translatedWords == 0 && it.approvedWords == 0 }
            .sortedBy { it.username.lowercase() }
    }

    if (translators.isNotEmpty()) {
        CrowdinContributorActivitySection(
            title = stringResource(R.string.language_translators_header),
            description = stringResource(R.string.language_translators_description),
            contributors = translators,
            contributionType = CrowdinContributionType.TRANSLATION
        )
    }
    if (proofreaders.isNotEmpty()) {
        CrowdinContributorActivitySection(
            title = stringResource(R.string.language_proofreaders_header),
            description = stringResource(R.string.language_proofreaders_description),
            contributors = proofreaders,
            contributionType = CrowdinContributionType.PROOFREADING
        )
    }
    if (otherContributors.isNotEmpty()) {
        CrowdinContributorActivitySection(
            title = stringResource(R.string.language_other_contributors_header),
            description = null,
            contributors = otherContributors,
            contributionType = CrowdinContributionType.OTHER
        )
    }
}

@Composable
private fun CrowdinContributorActivitySection(
    title: String,
    description: String?,
    contributors: List<CrowdinContributor>,
    contributionType: CrowdinContributionType,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsSectionHeader(title)
        description?.let {
            Text(
                text = it,
                modifier = Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            contributors.forEach { contributor ->
                CrowdinContributorPill(
                    contributor = contributor,
                    contributionType = contributionType
                )
            }
        }
    }
}

@Composable
private fun CrowdinContributorPill(
    contributor: CrowdinContributor,
    contributionType: CrowdinContributionType,
    modifier: Modifier = Modifier
) {
    val wordCount = when (contributionType) {
        CrowdinContributionType.TRANSLATION -> contributor.translatedWords
        CrowdinContributionType.PROOFREADING -> contributor.approvedWords
        CrowdinContributionType.OTHER -> null
    }
    val locale = LocalConfiguration.current.locales[0]
    val formattedWordCount = remember(locale, wordCount) {
        wordCount?.let { NumberFormat.getIntegerInstance(locale).format(it) }.orEmpty()
    }
    val activityLabel = when (contributionType) {
        CrowdinContributionType.TRANSLATION -> pluralStringResource(
            R.plurals.language_translator_words,
            contributor.translatedWords,
            contributor.username,
            formattedWordCount
        )
        CrowdinContributionType.PROOFREADING -> pluralStringResource(
            R.plurals.language_proofreader_words,
            contributor.approvedWords,
            contributor.username,
            formattedWordCount
        )
        CrowdinContributionType.OTHER -> contributor.username
    }
    val containerColor = when (contributionType) {
        CrowdinContributionType.TRANSLATION -> MaterialTheme.colorScheme.primaryContainer
        CrowdinContributionType.PROOFREADING -> MaterialTheme.colorScheme.secondaryContainer
        CrowdinContributionType.OTHER -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val flags = remember(contributor.languages) {
        // Keep one marker per language; distinct unknown languages may each need a globe.
        contributor.languages.map { crowdinLanguageFlag(it.id) }
    }
    val languageNames = remember(contributor.languages) {
        contributor.languages.map { language ->
            resolveAppLanguageTag(language.id)
                ?.let { AppLocale.displayName(it) }
                ?: language.name
        }
    }
    val accessibilityLabel = if (languageNames.isEmpty()) {
        activityLabel
    } else {
        stringResource(
            R.string.language_contributor_languages,
            activityLabel,
            languageNames.joinToString(", ")
        )
    }

    Surface(
        modifier = modifier.clearAndSetSemantics {
            text = AnnotatedString(accessibilityLabel)
        },
        shape = PillShape,
        color = containerColor
    ) {
        // Let content wrap below unusually long names instead of clipping either value.
        FlowRow(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            if (flags.isNotEmpty()) {
                Text(
                    text = flags.joinToString(" "),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2
                )
            }
            Text(
                text = contributor.username,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (contributionType != CrowdinContributionType.OTHER) {
                Text(
                    text = formattedWordCount,
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    maxLines = 1
                )
            }
        }
    }
}

private fun resolveAppLanguageTag(languageId: String): String? {
    val normalizedId = languageId.replace('_', '-')
    AppLocale.SUPPORTED_TAGS.firstOrNull { it.equals(normalizedId, ignoreCase = true) }?.let {
        return it
    }
    if ('-' in normalizedId) return null

    return AppLocale.SUPPORTED_TAGS.singleOrNull {
        it.substringBefore('-').equals(normalizedId, ignoreCase = true)
    }
}

private fun crowdinLanguageFlag(languageId: String): String {
    val tag = resolveAppLanguageTag(languageId) ?: return "🌐"
    val country = java.util.Locale.forLanguageTag(tag).country.uppercase(java.util.Locale.ROOT)
    if (country.length != 2 || country.any { it !in 'A'..'Z' }) return "🌐"

    return buildString {
        country.forEach { letter ->
            val regionalIndicator = 0x1F1E6 + (letter - 'A')
            append(String(Character.toChars(regionalIndicator)))
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

private val previewCrowdinContributors = listOf(
    CrowdinContributor(
        username = "avkurach",
        languages = listOf(
            CrowdinLanguage("ru", "Russian"),
            CrowdinLanguage("de", "German")
        ),
        translatedWords = 2940,
        approvedWords = 350
    ),
    CrowdinContributor(
        username = "cccClyde",
        languages = listOf(CrowdinLanguage("zh-CN", "Chinese Simplified")),
        translatedWords = 2867,
        approvedWords = 0
    ),
    CrowdinContributor(
        username = "TytoAlbaGuttata",
        languages = listOf(
            CrowdinLanguage("tr", "Turkish"),
            CrowdinLanguage("de", "German")
        ),
        translatedWords = 2840,
        approvedWords = 120
    ),
    CrowdinContributor(
        username = "zhx000",
        languages = listOf(CrowdinLanguage("zh-CN", "Chinese Simplified")),
        translatedWords = 1137,
        approvedWords = 0
    ),
    CrowdinContributor(
        username = "proofPilot",
        languages = listOf(CrowdinLanguage("fr", "French")),
        translatedWords = 0,
        approvedWords = 640
    ),
    CrowdinContributor(
        username = "singleWord",
        languages = listOf(CrowdinLanguage("es", "Spanish")),
        translatedWords = 1,
        approvedWords = 0
    ),
    CrowdinContributor(
        username = "quietVoter",
        languages = listOf(CrowdinLanguage("pt", "Portuguese")),
        translatedWords = 0,
        approvedWords = 0
    )
)

@Preview(showBackground = true)
@Composable
private fun LanguageScreenPreview() {
    HydroTrackerTheme {
        LanguageScreen()
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun CrowdinContributorSectionsPreview() {
    HydroTrackerTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            CrowdinContributorSectionsContent(contributors = previewCrowdinContributors)
        }
    }
}

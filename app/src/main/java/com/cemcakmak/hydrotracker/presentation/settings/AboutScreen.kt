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

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import com.cemcakmak.hydrotracker.data.update.UpdateStatus
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.cemcakmak.hydrotracker.BuildConfig
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.DarkModePreference
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme

private const val URL_GITHUB_PROFILE = "https://github.com/Econ01"

@Composable
fun AboutScreen(
    themePreferences: ThemePreferences = ThemePreferences(),
    wasPop: Boolean = false,
    updateStatus: UpdateStatus = UpdateStatus.Idle,
    onNavigateToUpdates: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onNavigateToLicenses: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    val isPreview = LocalInspectionMode.current
    val shouldApplyDepth = !isPreview && wasPop

    val blur by if (shouldApplyDepth) {
        val animatedContentScope = LocalNavAnimatedContentScope.current
        animatedContentScope.transition.animateDp(
            transitionSpec = { tween(400) },
            label = "aboutEnterBlur"
        ) { state -> if (state == EnterExitState.PreEnter) 8.dp else 0.dp }
    } else {
        remember { mutableStateOf(0.dp) }
    }

    val scrimAlpha by if (shouldApplyDepth) {
        val animatedContentScope = LocalNavAnimatedContentScope.current
        animatedContentScope.transition.animateFloat(
            transitionSpec = { tween(400) },
            label = "aboutEnterScrim"
        ) { state -> if (state == EnterExitState.PreEnter) 0.4f else 0f }
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val scrimColor = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color.White else Color.Black

    // Which document sheet is open (null = none).
    var openDoc by remember { mutableStateOf<DocSheet?>(null) }

    fun openSheet(doc: DocSheet) {
        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
        openDoc = doc
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().blur(blur)) {
            SettingsDetailScaffold(
                title = stringResource(R.string.screen_about_title),
                onNavigateBack = onNavigateBack,
                themePreferences = themePreferences
            ) {
                VersionHero(themePreferences = themePreferences)

                // Contributors
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingsSectionHeader(stringResource(R.string.about_section_contributors))
                    SettingsGroupCard(index = 0, size = 1) {
                        ContributorRow(
                            avatar = R.drawable.econ01,
                            name = stringResource(R.string.about_contributor_name),
                            role = stringResource(R.string.about_contributor_role),
                            bio = stringResource(R.string.about_contributor_bio),
                            onOpenGitHub = {
                                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                openUrl(context, URL_GITHUB_PROFILE)
                            }
                        )
                    }
                }

                // Updates
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingsSectionHeader(stringResource(R.string.updates_bug_report))
                    Column {
                        val isUpdateAvailable = updateStatus is UpdateStatus.Available
                        AboutRow(
                            index = 0,
                            size = 1,
                            icon = ImageVector.vectorResource(R.drawable.update_filled),
                            title = if (isUpdateAvailable) stringResource(R.string.updates_available_title) else stringResource(R.string.screen_updates_title),
                            titleColor = if (isUpdateAvailable) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                            description = stringResource(R.string.updates_status_check_now),
                            showChevron = true,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                onNavigateToUpdates()
                            }
                        )
                    }
                }

                // Information
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingsSectionHeader(stringResource(R.string.about_section_information))
                    Column {
                        AboutRow(
                            index = 0,
                            size = 5,
                            icon = ImageVector.vectorResource(R.drawable.article_filled),
                            title = stringResource(R.string.about_changelog),
                            description = stringResource(R.string.about_changelog_desc),
                            onClick = { openSheet(DocSheet(R.string.about_changelog, "CHANGELOG.md")) }
                        )
                        AboutRow(
                            index = 1,
                            size = 5,
                            icon = ImageVector.vectorResource(R.drawable.science_filled),
                            title = stringResource(R.string.about_sources),
                            description = stringResource(R.string.about_sources_desc),
                            onClick = { openSheet(DocSheet(R.string.about_sources, "sources.md")) }
                        )
                        AboutRow(
                            index = 2,
                            size = 5,
                            icon = ImageVector.vectorResource(R.drawable.security_filled),
                            title = stringResource(R.string.about_privacy_policy),
                            description = stringResource(R.string.about_privacy_policy_desc),
                            onClick = { openSheet(DocSheet(R.string.about_privacy_policy, "privacy-policy.md")) }
                        )
                        AboutRow(
                            index = 3,
                            size = 5,
                            icon = ImageVector.vectorResource(R.drawable.license_filled),
                            title = stringResource(R.string.about_license),
                            description = stringResource(R.string.about_license_desc),
                            onClick = { openSheet(DocSheet(R.string.about_license, "LICENSE.md")) }
                        )
                        AboutRow(
                            index = 4,
                            size = 5,
                            icon = ImageVector.vectorResource(R.drawable.signature_filled),
                            title = stringResource(R.string.about_third_party_licenses),
                            description = stringResource(R.string.about_third_party_licenses_desc),
                            showChevron = true,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                onNavigateToLicenses()
                            }
                        )
                    }
                }
            }
        }

        if (scrimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.3f)
                    .background(scrimColor.copy(alpha = scrimAlpha))
            )
        }
    }

    openDoc?.let { doc ->
        MarkdownBottomSheet(
            title = stringResource(doc.titleResId),
            assetFileName = doc.asset,
            onDismiss = { openDoc = null }
        )
    }
}

@Composable
private fun VersionHero(themePreferences: ThemePreferences) {
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
        shape = RoundedCornerShape(30.dp),
        tonalElevation = 2.dp,
        border = border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.padding(start = 6.dp)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLargeEmphasized
                )

                Text(
                    text = stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space = 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(30.dp),
                    color = MaterialTheme.colorScheme.tertiary
                ) {
                    Text(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                        text = stringResource(R.string.label_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodyMediumEmphasized,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(30.dp),
                    color = MaterialTheme.colorScheme.secondary
                ) {
                    Text(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                        text = stringResource(R.string.label_build_number, BuildConfig.VERSION_CODE),
                        style = MaterialTheme.typography.bodyMediumEmphasized,
                    )
                }
            }
        }
    }
}

@Suppress("SameParameterValue")
@Composable
private fun ContributorRow(
    avatar: Int,
    name: String,
    role: String,
    bio: String,
    onOpenGitHub: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(avatar),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.titleLargeEmphasized)
            Text(
                text = role,
                style = MaterialTheme.typography.bodyMediumEmphasized,
                color = MaterialTheme.colorScheme.tertiary
            )
            Text(
                text = bio,
                style = MaterialTheme.typography.bodySmall
            )
        }
        IconButton(
            modifier = Modifier.size(34.dp),
            onClick = onOpenGitHub
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.github),
                contentDescription = stringResource(R.string.cd_open_github_profile),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AboutRow(
    index: Int,
    size: Int,
    icon: ImageVector,
    title: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    description: String,
    showChevron: Boolean = false,
    onClick: () -> Unit
) {
    SettingsGroupCard(index = index, size = size, onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class DocSheet(@param:StringRes val titleResId: Int, val asset: String)

@Suppress("SameParameterValue")
private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.toast_no_app_to_open_link), Toast.LENGTH_SHORT).show()
    }
}

@Preview(showBackground = true)
@Composable
fun AboutScreenPreview() {
    HydroTrackerTheme {
        AboutScreen()
    }
}

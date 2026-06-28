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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.DarkModePreference
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.google.android.play.core.review.ReviewManagerFactory

private const val URL_GITHUB_SPONSORS = "https://github.com/sponsors/Econ01"
private const val URL_PAYPAL = "https://www.paypal.com/donate/?hosted_button_id=CQUZLNRM79CAU"
private const val URL_BUYMEACOFFEE = "https://buymeacoffee.com/thegadgetgeek"
private const val URL_GITHUB_REPO = "https://github.com/Econ01/HydroTracker"

/**
 * "Support Development" settings sub-page.
 */
@Composable
fun SupportDevelopmentScreen(
    themePreferences: ThemePreferences = ThemePreferences(),
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    SettingsDetailScaffold(
        title = stringResource(R.string.screen_support_title),
        onNavigateBack = onNavigateBack,
        themePreferences = themePreferences
    ) {
        SupportHeroCard(themePreferences = themePreferences)

        // Donation links — brand-coloured icons, order GitHub → PayPal → Buy Me a Coffee.
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsSectionHeader(stringResource(R.string.support_section_support))
            val links = listOf(
                SupportLink(
                    iconRes = R.drawable.github_sponsors,
                    tint = if (isDark) Color(0xFFDB61A2) else Color(0xFFBF3989),
                    title = stringResource(R.string.support_github_sponsors),
                    blurb = stringResource(R.string.support_github_sponsors_blurb),
                    url = URL_GITHUB_SPONSORS
                ),
                SupportLink(
                    iconRes = R.drawable.paypal,
                    tint = if (isDark) Color(0xFF009CDE) else Color(0xFF003087),
                    title = stringResource(R.string.support_paypal),
                    blurb = stringResource(R.string.support_paypal_blurb),
                    url = URL_PAYPAL
                ),
                SupportLink(
                    iconRes = R.drawable.coffee_filled,
                    tint = if (isDark) Color(0xFFFFDD00) else Color(0xFF8D6E00),
                    title = stringResource(R.string.support_buymeacoffee),
                    blurb = stringResource(R.string.support_buymeacoffee_blurb),
                    url = URL_BUYMEACOFFEE
                )
            )
            Column {
                links.forEachIndexed { index, link ->
                    SettingsGroupCard(
                        index = index,
                        size = links.size,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                            openUrl(context, link.url)
                        }
                    ) {
                        SupportRowContent(
                            icon = ImageVector.vectorResource(link.iconRes),
                            iconTint = link.tint,
                            title = link.title,
                            blurb = link.blurb,
                            trailing = ImageVector.vectorResource(R.drawable.open_in_new_filled)
                        )
                    }
                }
            }
        }

        // Free, non-monetary actions — standard primary-coloured icons.
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsSectionHeader(stringResource(R.string.support_section_other_ways))
            val helpSize = 3
            Column {
                SettingsGroupCard(
                    index = 0,
                    size = helpSize,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        launchInAppReview(context)
                    }
                ) {
                    SupportRowContent(
                        icon = ImageVector.vectorResource(R.drawable.star_rate_filled),
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = stringResource(R.string.support_rate_play_store),
                        blurb = stringResource(R.string.support_rate_play_store_desc),
                        trailing = null
                    )
                }
                SettingsGroupCard(
                    index = 1,
                    size = helpSize,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        shareApp(context)
                    }
                ) {
                    SupportRowContent(
                        icon = ImageVector.vectorResource(R.drawable.share_filled),
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = stringResource(R.string.support_share_app),
                        blurb = stringResource(R.string.support_share_app_desc),
                        trailing = null
                    )
                }
                SettingsGroupCard(
                    index = 2,
                    size = helpSize,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        openUrl(context, URL_GITHUB_REPO)
                    }
                ) {
                    SupportRowContent(
                        icon = ImageVector.vectorResource(R.drawable.github),
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = stringResource(R.string.support_star_github),
                        blurb = stringResource(R.string.support_star_github_desc),
                        trailing = ImageVector.vectorResource(R.drawable.open_in_new_filled)
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportHeroCard(themePreferences: ThemePreferences) {
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.heart_smile_filled),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = stringResource(R.string.support_development_hero),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SupportRowContent(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    blurb: String,
    trailing: ImageVector?
) {
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
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = blurb,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (trailing != null) {
            Icon(
                imageVector = trailing,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class SupportLink(
    val iconRes: Int,
    val tint: Color,
    val title: String,
    val blurb: String,
    val url: String
)

fun launchInAppReview(context: Context) {
    val manager = ReviewManagerFactory.create(context)
    val request = manager.requestReviewFlow()

    request.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            // Success Message
            Toast.makeText(context, context.getString(R.string.toast_review_successful), Toast.LENGTH_SHORT).show()

            val reviewInfo = task.result
            // The reviewInfo object is only valid for a limited amount of time
            val flow = manager.launchReviewFlow(context as android.app.Activity, reviewInfo)
            flow.addOnCompleteListener {
                // Review flow finished. Continue app flow.
                // Note: The API does not inform you if the user actually reviewed or not.
            }
        } else {
            // Fail Message
            Toast.makeText(context, context.getString(R.string.toast_review_failed), Toast.LENGTH_SHORT).show()
        }
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.toast_no_app_to_open_link), Toast.LENGTH_SHORT).show()
    }
}

private fun shareApp(context: Context) {
    val packageName = context.packageName
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(
            Intent.EXTRA_TEXT,
            "https://play.google.com/store/apps/details?id=$packageName"
        )
    }
    try {
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_chooser_title)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.toast_no_app_to_share), Toast.LENGTH_SHORT).show()
    }
}

@Preview(showBackground = true)
@Composable
fun SupportDevelopmentScreenPreview() {
    HydroTrackerTheme {
        SupportDevelopmentScreen()
    }
}

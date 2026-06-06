package com.cemcakmak.hydrotracker.presentation.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.cemcakmak.hydrotracker.R
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
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    SettingsDetailScaffold(
        title = "Support Development",
        onNavigateBack = onNavigateBack
    ) {
        SupportHeroCard()

        // Donation links — brand-colored icons, order GitHub → PayPal → Buy Me a Coffee.
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsSectionHeader("Support development")
            val links = listOf(
                SupportLink(
                    iconRes = R.drawable.github_sponsors,
                    tint = if (isDark) Color(0xFFDB61A2) else Color(0xFFBF3989),
                    title = "GitHub Sponsors",
                    blurb = "Support ongoing development",
                    url = URL_GITHUB_SPONSORS
                ),
                SupportLink(
                    iconRes = R.drawable.paypal,
                    tint = if (isDark) Color(0xFF009CDE) else Color(0xFF003087),
                    title = "PayPal",
                    blurb = "Send a quick donation",
                    url = URL_PAYPAL
                ),
                SupportLink(
                    iconRes = R.drawable.coffee_filled,
                    tint = if (isDark) Color(0xFFFFDD00) else Color(0xFF8D6E00),
                    title = "Buy Me a Coffee",
                    blurb = "Treat me to a coffee",
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

        // Free, non-monetary actions — standard primary-colored icons.
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsSectionHeader("Other ways to help")
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
                        title = "Rate on Google Play",
                        blurb = "Leave a rating on the Play Store",
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
                        title = "Share the app",
                        blurb = "Tell a friend about HydroTracker",
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
                        title = "Star on GitHub",
                        blurb = "Star the project on GitHub",
                        trailing = ImageVector.vectorResource(R.drawable.open_in_new_filled)
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportHeroCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        shape = RoundedCornerShape(30.dp),
        tonalElevation = 2.dp
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
                text = "HydroTracker is built and maintained by one person, with no ads and no " +
                    "tracking. If it has helped you drink more water, anything you chip in keeps it " +
                    "growing.",
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
            Toast.makeText(context, "Review is successful", Toast.LENGTH_SHORT).show()

            val reviewInfo = task.result
            // The reviewInfo object is only valid for a limited amount of time
            val flow = manager.launchReviewFlow(context as android.app.Activity, reviewInfo)
            flow.addOnCompleteListener {
                // Review flow finished. Continue app flow.
                // Note: The API does not inform you if the user actually reviewed or not.
            }
        } else {
            // Fail Message
            Toast.makeText(context, "Review failed", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No app found to open this link", Toast.LENGTH_SHORT).show()
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
        context.startActivity(Intent.createChooser(intent, "Share HydroTracker"))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No app found to share", Toast.LENGTH_SHORT).show()
    }
}

@Preview(showBackground = true)
@Composable
fun SupportDevelopmentScreenPreview() {
    HydroTrackerTheme {
        SupportDevelopmentScreen()
    }
}

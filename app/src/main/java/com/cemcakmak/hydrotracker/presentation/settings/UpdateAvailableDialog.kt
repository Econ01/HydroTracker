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
import android.os.Build
import android.view.RoundedCorner
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import com.cemcakmak.hydrotracker.BuildConfig
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.DarkModePreference
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.update.InstallSource
import com.cemcakmak.hydrotracker.data.update.UpdateRepository
import com.cemcakmak.hydrotracker.data.update.UpdateStatus
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.google.android.play.core.appupdate.AppUpdateOptions

/**
 * A custom modal dialoug shown on the Home screen when a new app update is available.
 * Displays version info, install source, and triggers the update flow directly.
 */
@Composable
fun UpdateAvailableDialog(
    status: UpdateStatus.Available,
    installSource: InstallSource,
    updateRepository: UpdateRepository,
    themePreferences: ThemePreferences = ThemePreferences(),
    onDismiss: () -> Unit = {}
) {
    Dialog(onDismissRequest = onDismiss) {
        UpdateAvailableDialogContent(
            status = status,
            installSource = installSource,
            updateRepository = updateRepository,
            themePreferences = themePreferences,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun UpdateAvailableDialogContent(
    status: UpdateStatus.Available,
    installSource: InstallSource,
    updateRepository: UpdateRepository,
    themePreferences: ThemePreferences = ThemePreferences(),
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current

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

    val deviceCornerRadius = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val insets = windowManager.currentWindowMetrics.windowInsets
            val corner = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)
            corner?.let { with(density) { it.radius.toDp() } }
        } else null
    } ?: 30.dp

    val actionContext = when (installSource) {
        InstallSource.PLAY_STORE -> stringResource(R.string.update_dialog_action_play)
        InstallSource.F_DROID -> stringResource(R.string.update_dialog_action_f_droid)
        InstallSource.OTHER -> stringResource(R.string.update_dialog_action_github)
    }

    val couldNotStartUpdate = stringResource(R.string.toast_could_not_start_update)
    val noAppToOpenLink = stringResource(R.string.toast_no_app_to_open_link)

    val currentVersionLabel = if (installSource == InstallSource.PLAY_STORE) {
        BuildConfig.VERSION_CODE.toString()
    } else {
        BuildConfig.VERSION_NAME
    }

    val updateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { /* Result handled by Play's install-state listener */ }

    fun performUpdate() {
        when {
            status.playUpdateInfo != null && status.playUpdateType != null -> {
                try {
                    updateRepository.appUpdateManager?.startUpdateFlowForResult(
                        status.playUpdateInfo,
                        updateLauncher,
                        AppUpdateOptions.defaultOptions(status.playUpdateType)
                    )
                } catch (_: Exception) {
                    Toast.makeText(context, couldNotStartUpdate, Toast.LENGTH_SHORT).show()
                }
            }
            status.downloadUrl != null -> {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, status.downloadUrl.toUri()))
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, noAppToOpenLink, Toast.LENGTH_SHORT).show()
                }
            }
        }
        onDismiss()
    }

    Surface(
        shape = RoundedCornerShape(deviceCornerRadius),
        tonalElevation = 2.dp,
        border = border,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.update_filled),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = stringResource(R.string.update_dialog_title),
                    style = MaterialTheme.typography.headlineMediumEmphasized
                )
            }

            HorizontalDivider()

            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Version info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Current version
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.update_dialog_current_version),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = currentVersionLabel,
                            style = MaterialTheme.typography.titleLargeEmphasized,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(100.dp)
                    ) {
                        LoadingIndicator(
                            modifier = Modifier.matchParentSize(),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.arrow_forward_filled),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiary
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.update_dialog_new_version),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = status.versionName,
                            style = MaterialTheme.typography.titleLargeEmphasized,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = actionContext,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Action buttons - Standard button group with press animations
                val haptics = LocalHapticFeedback.current
                val cancelInteractionSource = remember { MutableInteractionSource() }
                val updateInteractionSource = remember { MutableInteractionSource() }

                LaunchedEffect(cancelInteractionSource) {
                    cancelInteractionSource.interactions.collect { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                            is PressInteraction.Release -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            else -> {  }
                        }
                    }
                }

                LaunchedEffect(updateInteractionSource) {
                    updateInteractionSource.interactions.collect { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                            is PressInteraction.Release -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            else -> {  }
                        }
                    }
                }

                // Buttons
                ButtonGroup(
                    modifier = Modifier.fillMaxWidth(),
                    overflowIndicator = {}
                ) {
                    val scope = this
                    customItem(
                        buttonGroupContent = {
                            FilledTonalButton(
                                onClick = onDismiss,
                                shapes = ButtonDefaults.shapes(),
                                interactionSource = cancelInteractionSource,
                                modifier = with(scope) {
                                    Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .animateWidth(interactionSource = cancelInteractionSource)
                                }
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.cancel_filled),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.update_button_later), maxLines = 1, softWrap = false)
                            }
                        },
                        menuContent = {}
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = ::performUpdate,
                                shapes = ButtonDefaults.shapes(),
                                interactionSource = updateInteractionSource,
                                modifier = with(scope) {
                                    Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .animateWidth(interactionSource = updateInteractionSource)
                                }
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.update_filled),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.update_button_update), maxLines = 1, softWrap = false)
                            }
                        },
                        menuContent = {}
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UpdateAvailableDialogGitHubPreview() {
    HydroTrackerTheme {
        UpdateAvailableDialogContent(
            status = UpdateStatus.Available(
                versionName = "1.0.7",
                downloadUrl = "https://github.com/Econ01/HydroTracker/releases"
            ),
            installSource = InstallSource.OTHER,
            updateRepository = UpdateRepository(LocalContext.current)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UpdateAvailableDialogPlayPreview() {
    HydroTrackerTheme {
        UpdateAvailableDialogContent(
            status = UpdateStatus.Available(
                versionName = "27",
                playUpdateInfo = null,
                playUpdateType = null
            ),
            installSource = InstallSource.PLAY_STORE,
            updateRepository = UpdateRepository(LocalContext.current)
        )
    }
}

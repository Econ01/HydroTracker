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

import android.content.Context
import android.os.Build
import android.view.RoundedCorner
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cemcakmak.hydrotracker.BuildConfig
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.DarkModePreference
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.update.InstallSource
import com.cemcakmak.hydrotracker.data.update.UpdateError
import com.cemcakmak.hydrotracker.data.update.UpdateMechanism
import com.cemcakmak.hydrotracker.data.update.UpdateRepository
import com.cemcakmak.hydrotracker.data.update.UpdateStatus
import com.cemcakmak.hydrotracker.data.update.UpdateUiVisibility
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.ui.theme.extendedColorScheme

/**
 * A modal dialogue that follows an update from availability through installation.
 * Dismissing the dialogue only hides it; repository-owned update work continues.
 */
@Composable
fun UpdateAvailableDialog(
    status: UpdateStatus,
    installSource: InstallSource,
    updateRepository: UpdateRepository,
    themePreferences: ThemePreferences = ThemePreferences(),
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val activeUpdate by updateRepository.activeUpdate.collectAsState()
    val updateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result -> updateRepository.onUpdateFlowResult(result.resultCode) }

    DisposableEffect(Unit) {
        UpdateUiVisibility.isUpdateModalVisible = true
        onDispose {
            UpdateUiVisibility.isUpdateModalVisible = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        UpdateAvailableDialogContent(
            status = status,
            activeUpdate = activeUpdate,
            installSource = installSource,
            themePreferences = themePreferences,
            onDismiss = onDismiss,
            onStartUpdate = { available ->
                updateRepository.startUpdate(available, updateLauncher) { error ->
                    Toast.makeText(context, resources.getString(error.messageRes()), Toast.LENGTH_LONG).show()
                }
            },
            onInstallUpdate = updateRepository::installUpdate,
            onResumeUpdate = { updateRepository.resumeUpdate() },
            onCancelDownload = updateRepository::cancelUpdate
        )
    }
}

/** State-hoisted update dialogue content, kept independent of repository and activity-result APIs. */
@Composable
internal fun UpdateAvailableDialogContent(
    status: UpdateStatus,
    activeUpdate: UpdateStatus.Available?,
    installSource: InstallSource,
    themePreferences: ThemePreferences = ThemePreferences(),
    onDismiss: () -> Unit = {},
    onStartUpdate: (UpdateStatus.Available) -> Unit = {},
    onInstallUpdate: () -> Unit = {},
    onResumeUpdate: () -> Unit = {},
    onCancelDownload: () -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current

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
        } else {
            null
        }
    } ?: 30.dp

    val displayedUpdate = activeUpdate ?: when (status) {
        is UpdateStatus.Available -> status
        is UpdateStatus.ReadyToInstall -> status.available
        is UpdateStatus.PermissionRequired -> status.available
        else -> null
    }

    val actionContext = when (installSource) {
        InstallSource.PLAY_STORE -> stringResource(R.string.update_dialog_action_play)
        InstallSource.F_DROID -> stringResource(R.string.update_dialog_action_f_droid)
        InstallSource.OTHER -> stringResource(R.string.update_dialog_action_github)
    }

    val currentVersionLabel = if (installSource == InstallSource.PLAY_STORE) {
        BuildConfig.VERSION_CODE.toString()
    } else {
        BuildConfig.VERSION_NAME
    }

    val statusText = when (status) {
        is UpdateStatus.Available -> actionContext
        is UpdateStatus.Checking -> stringResource(R.string.updates_status_checking)
        is UpdateStatus.Downloading -> stringResource(R.string.updates_status_downloading)
        is UpdateStatus.Verifying -> stringResource(R.string.updates_status_verifying)
        is UpdateStatus.ReadyToInstall -> stringResource(R.string.updates_status_ready_to_install)
        is UpdateStatus.PermissionRequired -> stringResource(R.string.updates_status_install_permission_required)
        is UpdateStatus.Installing -> stringResource(R.string.updates_status_installing)
        is UpdateStatus.Failed -> stringResource(status.error.messageRes())
        is UpdateStatus.UpToDate -> stringResource(R.string.updates_status_up_to_date)
        is UpdateStatus.Unsupported -> stringResource(R.string.updates_status_f_droid_desc)
        is UpdateStatus.Idle -> stringResource(R.string.updates_status_check_now)
    }

    val defaultStatusColor = MaterialTheme.colorScheme.onSurface
    val errorStatusColor = MaterialTheme.colorScheme.error
    val successColor = MaterialTheme.extendedColorScheme.success
    val warningColor = MaterialTheme.extendedColorScheme.warning

    val statusColor = when (status) {
        is UpdateStatus.Available -> defaultStatusColor
        is UpdateStatus.Checking -> defaultStatusColor
        is UpdateStatus.Downloading -> defaultStatusColor
        is UpdateStatus.Verifying -> defaultStatusColor
        is UpdateStatus.ReadyToInstall -> successColor
        is UpdateStatus.PermissionRequired -> warningColor
        is UpdateStatus.Installing -> defaultStatusColor
        is UpdateStatus.Failed -> errorStatusColor
        is UpdateStatus.UpToDate -> defaultStatusColor
        is UpdateStatus.Unsupported -> errorStatusColor
        is UpdateStatus.Idle -> defaultStatusColor
    }

    val primaryLabel = when (status) {
        is UpdateStatus.Available -> stringResource(R.string.update_button_update)
        is UpdateStatus.ReadyToInstall -> stringResource(R.string.update_button_install)
        is UpdateStatus.PermissionRequired -> stringResource(R.string.update_button_allow_install)
        UpdateStatus.Installing -> stringResource(R.string.updates_button_continue_install)
        is UpdateStatus.Failed -> if (activeUpdate != null) stringResource(R.string.update_button_failed_retry)
        else stringResource(R.string.update_button_failed_dismiss)
        else -> null
    }
    val primaryAction: (() -> Unit)? = when (status) {
        is UpdateStatus.Available -> {
            { onStartUpdate(status) }
        }
        is UpdateStatus.ReadyToInstall -> onInstallUpdate
        is UpdateStatus.PermissionRequired -> onInstallUpdate
        UpdateStatus.Installing -> onResumeUpdate
        is UpdateStatus.Failed -> if (activeUpdate != null) {
            { onStartUpdate(activeUpdate) }
        } else {
            onDismiss
        }
        else -> null
    }
    val canCancelDownload = installSource == InstallSource.OTHER &&
        (status is UpdateStatus.Downloading || status == UpdateStatus.Verifying)

    Surface(
        shape = RoundedCornerShape(deviceCornerRadius),
        tonalElevation = 2.dp,
        border = border,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("update_flow_dialog")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = stringResource(R.string.update_dialog_title),
                style = MaterialTheme.typography.headlineMediumEmphasized
            )

            HorizontalDivider()

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
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
                        modifier = Modifier.size(100.dp)
                    ) {
                        LoadingIndicator(
                            modifier = Modifier.matchParentSize(),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.arrow_downward_filled),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiary
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.update_dialog_new_version),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = displayedUpdate?.versionName ?: "—",
                            style = MaterialTheme.typography.titleLargeEmphasized,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = statusColor,
                            modifier = Modifier.testTag("update_status_text")
                        )

                        when (status) {
                            is UpdateStatus.Downloading -> {
                                val progress = status.progress
                                if (progress != null) {
                                    Text(
                                        text = stringResource(R.string.updates_status_downloading_progress, progress),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.testTag("update_status_percentage_text")
                                    )
                                }
                            }
                            else -> Unit
                        }
                    }

                    when (status) {
                        is UpdateStatus.Downloading -> {
                            val progress = status.progress
                            val progressModifier = Modifier
                                .fillMaxWidth()
                                .testTag("update_progress")
                            if (progress != null) {
                                LinearWavyProgressIndicator(
                                    progress = { progress.coerceIn(0, 100) / 100f },
                                    modifier = progressModifier
                                )
                            } else {
                                LinearWavyProgressIndicator(modifier = progressModifier)
                            }
                        }
                        UpdateStatus.Verifying, UpdateStatus.Installing -> LinearWavyProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("update_progress")
                        )
                        else -> Unit
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (primaryLabel != null && primaryAction != null) {
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                primaryAction()
                            },
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("update_primary_action")
                        ) {
                            Text(primaryLabel, maxLines = 2)
                        }
                    }
                    FilledTonalButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            if (canCancelDownload) onCancelDownload()
                            onDismiss()
                        },
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag(if (canCancelDownload) "update_cancel_action" else "update_secondary_action")
                    ) {
                        Text(
                            text = stringResource(
                                if (canCancelDownload) R.string.update_cancel_download
                                else R.string.update_button_later
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private val previewGitHubUpdate = UpdateStatus.Available(
    versionName = "1.0.7",
    mechanism = UpdateMechanism.GITHUB_APK,
    downloadUrl = "https://github.com/Econ01/HydroTracker/releases"
)

@Composable
private fun UpdateAvailableDialogPreview(
    status: UpdateStatus,
    activeUpdate: UpdateStatus.Available? = null,
    installSource: InstallSource = InstallSource.OTHER
) {
    HydroTrackerTheme {
        UpdateAvailableDialogContent(
            status = status,
            activeUpdate = activeUpdate,
            installSource = installSource
        )
    }
}

@Preview(name = "Available - GitHub", showBackground = true)
@Composable
private fun UpdateAvailableDialogGitHubPreview() {
    UpdateAvailableDialogPreview(
        status = previewGitHubUpdate,
        activeUpdate = previewGitHubUpdate
    )
}

@Preview(name = "Checking", showBackground = true)
@Composable
private fun UpdateAvailableDialogCheckingPreview() {
    UpdateAvailableDialogPreview(status = UpdateStatus.Checking)
}

@Preview(name = "Downloading - Indeterminate", showBackground = true)
@Composable
private fun UpdateAvailableDialogDownloadingIndeterminatePreview() {
    UpdateAvailableDialogPreview(
        status = UpdateStatus.Downloading(),
        activeUpdate = previewGitHubUpdate
    )
}

@Preview(name = "Downloading - Progress", showBackground = true)
@Composable
private fun UpdateAvailableDialogDownloadingProgressPreview() {
    UpdateAvailableDialogPreview(
        status = UpdateStatus.Downloading(progress = 65),
        activeUpdate = previewGitHubUpdate
    )
}

@Preview(name = "Verifying", showBackground = true)
@Composable
private fun UpdateAvailableDialogVerifyingPreview() {
    UpdateAvailableDialogPreview(
        status = UpdateStatus.Verifying,
        activeUpdate = previewGitHubUpdate
    )
}

@Preview(name = "Ready to Install", showBackground = true)
@Composable
private fun UpdateAvailableDialogReadyToInstallPreview() {
    UpdateAvailableDialogPreview(
        status = UpdateStatus.ReadyToInstall(previewGitHubUpdate),
        activeUpdate = previewGitHubUpdate
    )
}

@Preview(name = "Permission Required", showBackground = true)
@Composable
private fun UpdateAvailableDialogPermissionRequiredPreview() {
    UpdateAvailableDialogPreview(
        status = UpdateStatus.PermissionRequired(previewGitHubUpdate),
        activeUpdate = previewGitHubUpdate
    )
}

@Preview(name = "Installing", showBackground = true)
@Composable
private fun UpdateAvailableDialogInstallingPreview() {
    UpdateAvailableDialogPreview(
        status = UpdateStatus.Installing,
        activeUpdate = previewGitHubUpdate
    )
}

@Preview(name = "Failed - Retry", showBackground = true)
@Composable
private fun UpdateAvailableDialogFailedRetryPreview() {
    UpdateAvailableDialogPreview(
        status = UpdateStatus.Failed(
            UpdateError.VERIFICATION_FAILED,
            "The downloaded update could not be verified."
        ),
        activeUpdate = previewGitHubUpdate
    )
}

@Preview(name = "Failed - Dismiss", showBackground = true)
@Composable
private fun UpdateAvailableDialogFailedDismissPreview() {
    UpdateAvailableDialogPreview(
        status = UpdateStatus.Failed(
            UpdateError.CHECK_FAILED,
            "The update check could not be completed."
        )
    )
}

@Preview(name = "Up to Date", showBackground = true)
@Composable
private fun UpdateAvailableDialogUpToDatePreview() {
    UpdateAvailableDialogPreview(status = UpdateStatus.UpToDate)
}

@Preview(name = "Unsupported - F-Droid", showBackground = true)
@Composable
private fun UpdateAvailableDialogUnsupportedPreview() {
    UpdateAvailableDialogPreview(
        status = UpdateStatus.Unsupported,
        installSource = InstallSource.F_DROID
    )
}

@Preview(name = "Idle", showBackground = true)
@Composable
private fun UpdateAvailableDialogIdlePreview() {
    UpdateAvailableDialogPreview(status = UpdateStatus.Idle)
}

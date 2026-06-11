package com.cemcakmak.hydrotracker.presentation.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.core.net.toUri
import com.cemcakmak.hydrotracker.BuildConfig
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.ColorSource
import com.cemcakmak.hydrotracker.data.models.DarkModePreference
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.update.InstallSource
import com.cemcakmak.hydrotracker.data.update.UpdateRepository
import com.cemcakmak.hydrotracker.data.update.UpdateStatus
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.InstallStatus
import kotlinx.coroutines.launch

/**
 * The Updates settings page.
 */
@Composable
fun UpdatesScreen(
    updateRepository: UpdateRepository,
    themePreferences: ThemePreferences = ThemePreferences(),
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val couldNotStartUpdate = stringResource(R.string.toast_could_not_start_update)

    val status by updateRepository.updateStatus.collectAsState()
    val autoCheckEnabled by updateRepository.autoCheckEnabled.collectAsState()
    val lastCheckTime by updateRepository.lastCheckTime.collectAsState()
    val simulatedDownloaded by updateRepository.simulatedDownloaded.collectAsState()

    var downloaded by remember { mutableStateOf(false) }

    val appUpdateManager = updateRepository.appUpdateManager
    val updateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { /* The install-state listener and the next check reflect the outcome. */ }

    // Surface Play's "downloaded → restart" state as an inline action.
    DisposableEffect(appUpdateManager) {
        if (appUpdateManager == null) {
            onDispose { }
        } else {
            val listener = InstallStateUpdatedListener { state ->
                if (state.installStatus() == InstallStatus.DOWNLOADED) downloaded = true
            }
            appUpdateManager.registerListener(listener)
            // Reflect an update that finished downloading while we were away.
            appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
                if (info.installStatus() == InstallStatus.DOWNLOADED) downloaded = true
            }
            onDispose { appUpdateManager.unregisterListener(listener) }
        }
    }

    UpdatesContent(
        themePreferences = themePreferences,
        installSource = updateRepository.installSource,
        status = status,
        autoCheckEnabled = autoCheckEnabled,
        lastCheckTime = lastCheckTime,
        downloaded = downloaded || simulatedDownloaded,
        onNavigateBack = onNavigateBack,
        onCheckNow = {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            scope.launch { updateRepository.checkForUpdates() }
        },
        onToggleAuto = updateRepository::setAutoCheckEnabled,
        onCompleteInstall = { appUpdateManager?.completeUpdate() },
        onPrimaryAction = { available ->
            if (available.playUpdateInfo != null && available.playUpdateType != null) {
                try {
                    appUpdateManager?.startUpdateFlowForResult(
                        available.playUpdateInfo,
                        updateLauncher,
                        AppUpdateOptions.defaultOptions(available.playUpdateType)
                    )
                } catch (_: Exception) {
                    Toast.makeText(context, couldNotStartUpdate, Toast.LENGTH_SHORT).show()
                }
            } else if (available.downloadUrl != null) {
                openUrl(context, available.downloadUrl)
            }
        }
    )
}

@Composable
private fun UpdatesContent(
    themePreferences: ThemePreferences,
    installSource: InstallSource,
    status: UpdateStatus,
    autoCheckEnabled: Boolean,
    lastCheckTime: Long,
    downloaded: Boolean,
    onNavigateBack: () -> Unit,
    onCheckNow: () -> Unit,
    onToggleAuto: (Boolean) -> Unit,
    onCompleteInstall: () -> Unit,
    onPrimaryAction: (UpdateStatus.Available) -> Unit
) {
    SettingsDetailScaffold(title = stringResource(R.string.screen_updates_title), onNavigateBack = onNavigateBack) {
        VersionInfo(
            themePreferences = themePreferences,
            installSource = installSource
        )

        val haptics = LocalHapticFeedback.current

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsSectionHeader(stringResource(R.string.updates_section_auto_update))
            SettingsGroupCard(index = 0, size = 1) {
                val haptics = LocalHapticFeedback.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.update),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.updates_toggle_auto_check),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.updates_toggle_auto_check_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoCheckEnabled,
                        onCheckedChange = { enabled ->
                            onToggleAuto(enabled)
                            haptics.performHapticFeedback(
                                if (enabled) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
                            )
                        },
                        thumbContent = if (themePreferences.colorSource == ColorSource.DYNAMIC_COLOR) {
                            {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.update_filled),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            }
                        } else {
                            null
                        }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsSectionHeader(stringResource(R.string.updates_section_status))
            UpdateStatusCard(
                status = status,
                lastCheckTime = lastCheckTime,
                downloaded = downloaded,
                onPrimaryAction = onPrimaryAction,
                onCompleteInstall = onCompleteInstall
            )

            val checking = status is UpdateStatus.Checking
            if (status !is UpdateStatus.Available) {
                Button(
                    onClick = {
                        onCheckNow()
                        haptics.performHapticFeedback(HapticFeedbackType.ToggleOn) },
                    enabled = !checking,
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                        disabledContainerColor = ButtonDefaults.buttonColors().disabledContainerColor,
                        disabledContentColor = ButtonDefaults.buttonColors().disabledContentColor
                    ),
                    contentPadding = PaddingValues(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(if (checking) stringResource(R.string.updates_button_checking) else stringResource(R.string.updates_button_check_now))
                }
            }
        }
    }
}

@Composable
private fun VersionInfo(
    themePreferences: ThemePreferences,
    installSource: InstallSource
) {
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

    val sourceLabel = when (installSource) {
        InstallSource.PLAY_STORE -> stringResource(R.string.install_source_google_play)
        InstallSource.F_DROID -> stringResource(R.string.install_source_f_droid)
        InstallSource.OTHER -> stringResource(R.string.install_source_manual)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        shape = RoundedCornerShape(size = 30.dp),
        tonalElevation = 2.dp,
        border = border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoRow(stringResource(R.string.label_version_simple), BuildConfig.VERSION_NAME)
            HorizontalDivider()
            InfoRow(stringResource(R.string.label_build_simple), BuildConfig.VERSION_CODE.toString())
            HorizontalDivider()
            InfoRow(stringResource(R.string.label_install_method), sourceLabel)
            if (installSource == InstallSource.OTHER) {
                HorizontalDivider()
                val sourceValue = if (BuildConfig.DEBUG) stringResource(R.string.install_source_debug) else stringResource(R.string.install_source_github)
                InfoRow(stringResource(R.string.label_source), sourceValue)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMediumEmphasized,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun UpdateStatusCard(
    status: UpdateStatus,
    lastCheckTime: Long,
    downloaded: Boolean,
    onPrimaryAction: (UpdateStatus.Available) -> Unit,
    onCompleteInstall: () -> Unit
) {
    SettingsGroupCard(index = 0, size = 1) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (status) {
                is UpdateStatus.Available -> AvailableContent(status, onPrimaryAction)
                is UpdateStatus.Checking -> StatusLine(
                    icon = null,
                    title = stringResource(R.string.updates_status_checking),
                    subtitle = null,
                    showSpinner = true
                )
                is UpdateStatus.Failed -> StatusLine(
                    icon = R.drawable.cancel_filled,
                    title = stringResource(R.string.updates_status_failed),
                    subtitle = status.message
                )
                UpdateStatus.Unsupported -> StatusLine(
                    icon = R.drawable.info_filled,
                    title = stringResource(R.string.updates_status_f_droid),
                    subtitle = stringResource(R.string.updates_status_f_droid_desc)
                )
                UpdateStatus.UpToDate -> StatusLine(
                    icon = R.drawable.check_filled,
                    title = stringResource(R.string.updates_status_up_to_date),
                    subtitle = lastCheckedText(lastCheckTime)
                )
                UpdateStatus.Idle -> StatusLine(
                    icon = R.drawable.info_filled,
                    title = stringResource(R.string.updates_status_check_now),
                    subtitle = lastCheckedText(lastCheckTime)
                )
            }

            if (downloaded) {
                Button(onClick = onCompleteInstall, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.updates_button_restart_install))
                }
            }
        }
    }
}

@Composable
private fun AvailableContent(
    status: UpdateStatus.Available,
    onPrimaryAction: (UpdateStatus.Available) -> Unit
) {
    val isPlay = status.playUpdateInfo != null
    val haptics = LocalHapticFeedback.current

    Column (
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.updates_available_title), style = MaterialTheme.typography.titleLarge)
        Text(
            text = if (isPlay) stringResource(R.string.updates_available_play_desc)
            else stringResource(R.string.updates_available_version, status.versionName),
            style = MaterialTheme.typography.bodyMediumEmphasized,
            color = MaterialTheme.colorScheme.primary
        )
    }

    // Update button
    Button(
        onClick = {
            onPrimaryAction(status)
            haptics.performHapticFeedback(HapticFeedbackType.ToggleOn) },
        shapes = ButtonDefaults.shapes(),
        colors = ButtonColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary,
            disabledContainerColor = ButtonDefaults.buttonColors().disabledContainerColor,
            disabledContentColor = ButtonDefaults.buttonColors().disabledContentColor
        ),
        contentPadding = PaddingValues(20.dp),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(if (isPlay) stringResource(R.string.updates_button_update) else stringResource(R.string.updates_button_download))
    }
}

@Composable
private fun StatusLine(
    icon: Int?,
    title: String,
    subtitle: String?,
    showSpinner: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showSpinner) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
        } else if (icon != null) {
            Icon(
                imageVector = ImageVector.vectorResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun lastCheckedText(time: Long): String {
    if (time <= 0L) return stringResource(R.string.updates_last_checked_never)
    val relative = DateUtils.getRelativeTimeSpanString(
        time,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    )
    return stringResource(R.string.updates_last_checked, relative.toString())
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.toast_no_app_to_open_link), Toast.LENGTH_SHORT).show()
    }
}

@Preview(showBackground = true)
@Composable
private fun UpdatesAvailablePreview() {
    HydroTrackerTheme {
        UpdatesContent(
            themePreferences = ThemePreferences(),
            installSource = InstallSource.OTHER,
            status = UpdateStatus.Available(
                versionName = "1.0.7",
                downloadUrl = "https://example.com/app.apk"
            ),
            autoCheckEnabled = true,
            lastCheckTime = System.currentTimeMillis() - 3_600_000,
            downloaded = false,
            onNavigateBack = {},
            onCheckNow = {},
            onToggleAuto = {},
            onCompleteInstall = {},
            onPrimaryAction = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UpdatesUpToDatePreview() {
    HydroTrackerTheme {
        UpdatesContent(
            themePreferences = ThemePreferences(),
            installSource = InstallSource.PLAY_STORE,
            status = UpdateStatus.UpToDate,
            autoCheckEnabled = false,
            lastCheckTime = System.currentTimeMillis() - 86_400_000,
            downloaded = false,
            onNavigateBack = {},
            onCheckNow = {},
            onToggleAuto = {},
            onCompleteInstall = {},
            onPrimaryAction = {}
        )
    }
}

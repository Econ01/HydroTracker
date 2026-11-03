package com.cemcakmak.hydrotracker.presentation.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.cemcakmak.hydrotracker.BuildConfig
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.update.DebugUpdateRepository
import com.cemcakmak.hydrotracker.data.update.InstallSource
import com.cemcakmak.hydrotracker.data.update.UpdateError
import com.cemcakmak.hydrotracker.data.update.UpdateMechanism
import com.cemcakmak.hydrotracker.data.update.UpdateRepository
import com.cemcakmak.hydrotracker.data.update.UpdateStatus
import com.cemcakmak.hydrotracker.data.update.UpdateTestActions
import kotlinx.coroutines.launch

/** Comprehensive debug-only controls for update UI, server scenarios and installer outcomes. */
@Composable
fun UpdateTestingScreen(
    updateRepository: UpdateRepository,
    themePreferences: ThemePreferences = ThemePreferences(),
    onNavigateBack: () -> Unit = {}
) {
    val debugRepository = updateRepository as? DebugUpdateRepository ?: return
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var localNetworkPermissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 37 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_LOCAL_NETWORK) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        localNetworkPermissionGranted = granted
        if (granted) {
            scope.launch { updateRepository.checkForUpdates() }
        } else {
            Toast.makeText(
                context,
                "Local network permission is required for the update test server",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    val status by updateRepository.updateStatus.collectAsState()
    val simulatedDownloaded by updateRepository.simulatedDownloaded.collectAsState()

    var channel by  remember { mutableStateOf(UpdateTestActions.channel(context)) }
    var baseUrl by remember { mutableStateOf(UpdateTestActions.baseUrl(context)) }
    var scenario by remember { mutableStateOf(UpdateTestActions.scenario(context)) }

    fun action(block: () -> Unit) {
        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
        block()
    }

    fun saveAndCheckForUpdates() {
        UpdateTestActions.saveServerConfiguration(context, baseUrl, scenario)
        if (Build.VERSION.SDK_INT >= 37 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_LOCAL_NETWORK) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
        } else {
            scope.launch { updateRepository.checkForUpdates() }
        }
    }

    SettingsDetailScaffold(
        title = "Update testing",
        onNavigateBack = onNavigateBack,
        themePreferences = themePreferences
    ) {
        if (UpdateTestActions.supportsChannelSelection) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsSectionHeader("Update channel")
                UpdateTestCard {
                    OutlinedButton(
                        onClick = {
                            action {
                                val channels = InstallSource.entries
                                channel = channels[(channels.indexOf(channel) + 1) % channels.size]
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Channel: ${channelLabel(channel)}")
                    }
                    Text(
                        text = "This changes the active implementation throughout the normal UI. F-Droid mode also hides Play-only review controls.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = {
                            action {
                                UpdateTestActions.saveChannel(context, channel)
                                debugRepository.resetSimulation()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply channel")
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsSectionHeader("Current state")
            UpdateTestCard {
                Text(
                    text = statusDescription(status),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Start and observe the real flow through the Home update modal or Settings → Updates.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (UpdateTestActions.supportsServerTesting) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsSectionHeader("Local server")
                UpdateTestCard {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("Server base URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Emulator: http://10.0.2.2:8765\nPhysical device: http://<computer-LAN-IP>:8765",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(
                        onClick = {
                            action {
                                val scenarios = UpdateTestActions.scenarios
                                val next = (scenarios.indexOf(scenario) + 1) % scenarios.size
                                scenario = scenarios[next]
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Scenario: $scenario")
                    }
                    Text(
                        text = scenarioDescription(scenario),
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (Build.VERSION.SDK_INT >= 37 && !localNetworkPermissionGranted) {
                        Text(
                            text = "Android 17 requires local network permission for this test server. You will be asked to grant it before checking.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Button(
                        onClick = { action(::saveAndCheckForUpdates) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save and check for updates")
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsSectionHeader("State simulations")
            UpdateTestCard {
                SimulationButton("Idle") { action { debugRepository.simulate(UpdateStatus.Idle) } }
                SimulationButton("Checking") { action { debugRepository.simulate(UpdateStatus.Checking) } }
                SimulationButton("Available") {
                    action {
                        debugRepository.simulate(testAvailable(updateRepository.installSource))
                    }
                }
                SimulationButton("Up to date") { action { debugRepository.simulate(UpdateStatus.UpToDate) } }
                SimulationButton("Failed") {
                    action {
                        debugRepository.simulate(
                            UpdateStatus.Failed(UpdateError.UNKNOWN, "Simulated update failure")
                        )
                    }
                }
                SimulationButton("Downloading — indeterminate") {
                    action { debugRepository.simulate(UpdateStatus.Downloading()) }
                }
                SimulationButton("Downloading — 50%") {
                    action { debugRepository.simulate(UpdateStatus.Downloading(50)) }
                }
                SimulationButton("Verifying") { action { debugRepository.simulate(UpdateStatus.Verifying) } }
                SimulationButton("Ready to install") {
                    action { debugRepository.simulate(UpdateStatus.ReadyToInstall(testAvailable(updateRepository.installSource))) }
                }
                SimulationButton("Permission required") {
                    action { debugRepository.simulate(UpdateStatus.PermissionRequired(testAvailable(updateRepository.installSource))) }
                }
                SimulationButton("Installing") { action { debugRepository.simulate(UpdateStatus.Installing) } }
                if (BuildConfig.HAS_PLAY_STORE) {
                    SimulationButton(
                        if (simulatedDownloaded) "Play downloaded — ON" else "Play downloaded — OFF"
                    ) {
                        action { updateRepository.setSimulatedDownloaded(!simulatedDownloaded) }
                    }
                }
            }
        }

        if (UpdateTestActions.supportsInstallResultSimulation) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsSectionHeader("Installer result boundary")
                UpdateTestCard {
                    Text(
                        text = "These actions abandon the active system session and simulate its final callback; no APK is installed.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    SimulationButton("Simulate installation success") {
                        action { UpdateTestActions.simulateInstallationResult(true) }
                    }
                    SimulationButton("Simulate installation failure") {
                        action { UpdateTestActions.simulateInstallationResult(false) }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsSectionHeader("Reset and documentation")
            UpdateTestCard {
                SimulationButton("Reset simulated state") {
                    action { debugRepository.resetSimulation() }
                }
                SimulationButton("Reset What's New") {
                    action {
                        updateRepository.resetWhatsNew()
                        Toast.makeText(context, "What's New reset", Toast.LENGTH_SHORT).show()
                    }
                }
                if (BuildConfig.HAS_PLAY_STORE) {
                    Text(
                        text = "For real Play updates, use internal app sharing. Actual review submission requires the internal test track. See docs/update-testing.md.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (UpdateTestActions.supportsServerTesting) {
                    Text(
                        text = "For background testing, select slow_download, begin the update and leave the app before it finishes. Use Continue installation if notifications are unavailable.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateTestCard(content: @Composable ColumnScope.() -> Unit) {
    SettingsGroupCard(index = 0, size = 1) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun SimulationButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label)
    }
}

private fun statusDescription(status: UpdateStatus): String = when (status) {
    UpdateStatus.Idle -> "Idle"
    UpdateStatus.Checking -> "Checking"
    UpdateStatus.UpToDate -> "Up to date"
    UpdateStatus.Unsupported -> "Managed by F-Droid (debug flavour)"
    is UpdateStatus.Failed -> status.technicalDetails?.let { details ->
        "Failed: ${status.error}: $details"
    } ?: "Failed: ${status.error}"
    is UpdateStatus.Downloading -> "Downloading: ${status.progress?.let { "$it%" } ?: "indeterminate"}"
    UpdateStatus.Verifying -> "Verifying"
    is UpdateStatus.ReadyToInstall -> "Ready to install: ${status.available.versionName}"
    is UpdateStatus.PermissionRequired -> "Installation permission required"
    UpdateStatus.Installing -> "Installing"
    is UpdateStatus.Available -> "Available: ${status.versionName} (${status.mechanism})"
}

private fun testAvailable(source: InstallSource) = UpdateStatus.Available(
    versionName = "99.99.99",
    mechanism = if (source == InstallSource.PLAY_STORE) {
        UpdateMechanism.PLAY_IN_APP
    } else {
        UpdateMechanism.GITHUB_APK
    },
    downloadUrl = "https://example.com/HydroTracker-99.99.99.apk",
    downloadSha256 = "0".repeat(64)
)

private fun channelLabel(source: InstallSource): String = when (source) {
    InstallSource.PLAY_STORE -> "Google Play"
    InstallSource.F_DROID -> "F-Droid simulation"
    InstallSource.OTHER -> "GitHub"
}

private fun scenarioDescription(scenario: String): String = when (scenario) {
    "success" -> "Valid newer APK; the full installation flow should succeed."
    "up_to_date" -> "Release matches this version; no update should be offered."
    "http_error" -> "The release endpoint returns HTTP 500."
    "invalid_json" -> "The release endpoint returns malformed JSON."
    "missing_asset" -> "No GitHub-flavour APK is present in the release."
    "bad_digest" -> "The APK digest does not match the release metadata."
    "wrong_package" -> "The APK has a different application ID."
    "old_version" -> "The APK version code is older than the installed build."
    "bad_signature" -> "The APK is signed with an untrusted test key."
    "slow_download" -> "A valid APK downloads slowly for background and progress testing."
    else -> "Unknown scenario"
}

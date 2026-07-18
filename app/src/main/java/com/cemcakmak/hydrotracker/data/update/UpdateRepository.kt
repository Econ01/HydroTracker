package com.cemcakmak.hydrotracker.data.update

import android.content.Context
import androidx.core.content.edit
import com.cemcakmak.hydrotracker.BuildConfig
import com.google.android.play.core.appupdate.AppUpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owns update-check state and preferences. Picks the right [UpdateChecker] for the installation source
 * and exposes the latest [UpdateStatus] plus the auto-check toggle.
 */
class UpdateRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("hydrotracker_prefs", Context.MODE_PRIVATE)
    private val checker: UpdateChecker = UpdateCheckerFactory.create(appContext)

    val installSource: InstallSource get() = checker.source

    /** Non-null only for Play installs; used by the UI to launch/complete the in-app update flow. */
    val appUpdateManager: AppUpdateManager? = (checker as? PlayUpdateChecker)?.appUpdateManager

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    private val _autoCheckEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_CHECK, true))
    val autoCheckEnabled: StateFlow<Boolean> = _autoCheckEnabled.asStateFlow()

    private val _lastCheckTime = MutableStateFlow(prefs.getLong(KEY_LAST_CHECK, 0L))
    val lastCheckTime: StateFlow<Long> = _lastCheckTime.asStateFlow()

    private val _whatsNewAvailable = MutableStateFlow(false)
    val whatsNewAvailable: StateFlow<Boolean> = _whatsNewAvailable.asStateFlow()

    private val _simulatedDownloaded = MutableStateFlow(false)
    val simulatedDownloaded: StateFlow<Boolean> = _simulatedDownloaded.asStateFlow()

    init {
        val lastSeen = prefs.getString(KEY_WHATS_NEW_VERSION, null)
        val current = BuildConfig.VERSION_NAME
        if (lastSeen == null) {
            // Fresh install — mark current version as seen so the sheet doesn't show.
            prefs.edit { putString(KEY_WHATS_NEW_VERSION, current) }
        } else if (lastSeen != current) {
            _whatsNewAvailable.value = true
        }
        // Clean up orphaned skipped-version pref from internal builds.
        prefs.edit { remove("update_skipped_version") }
    }

    /**
     * Runs a check. Always records the check timestamp.
     */
    suspend fun checkForUpdates() {
        _updateStatus.value = UpdateStatus.Checking
        val result = try {
            checker.check()
        } catch (e: Exception) {
            UpdateStatus.Failed(e.message ?: "Unknown error")
        }
        val now = System.currentTimeMillis()
        prefs.edit { putLong(KEY_LAST_CHECK, now) }
        _lastCheckTime.value = now
        _updateStatus.value = result
    }

    /** Auto-check on launch: only when enabled, supported, and not checked in the last 24h. */
    suspend fun maybeAutoCheck() {
        if (!_autoCheckEnabled.value) return
        if (installSource == InstallSource.F_DROID) return
        if (System.currentTimeMillis() - _lastCheckTime.value < AUTO_CHECK_INTERVAL_MS) return
        checkForUpdates()
    }

    fun setAutoCheckEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AUTO_CHECK, enabled) }
        _autoCheckEnabled.value = enabled
    }

    fun markWhatsNewSeen() {
        prefs.edit { putString(KEY_WHATS_NEW_VERSION, BuildConfig.VERSION_NAME) }
        _whatsNewAvailable.value = false
    }

    /** Debug: forces the update status to Available so the UI can be tested. */
    fun simulateUpdateAvailable(versionName: String = "99.99.99") {
        _updateStatus.value = UpdateStatus.Available(
            versionName = versionName,
            downloadUrl = "https://github.com/Econ01/HydroTracker/releases"
        )
    }

    /** Debug: clears the last-seen version so the What's New sheet shows again. */
    fun resetWhatsNew() {
        prefs.edit { remove(KEY_WHATS_NEW_VERSION) }
        _whatsNewAvailable.value = true
    }

    /** Debug: toggles the simulated "downloaded → restart" state for Play flexible updates. */
    fun setSimulatedDownloaded(enabled: Boolean) {
        _simulatedDownloaded.value = enabled
    }

    /** Loads and extracts the changelog entry for the current app version from assets. */
    fun loadWhatsNewContent(): String {
        val current = BuildConfig.VERSION_NAME
        return try {
            val text = appContext.assets.open("CHANGELOG.md").bufferedReader().use { it.readText() }
            extractVersionChangelog(text, current)
        } catch (_: Exception) {
            ""
        }
    }

    companion object {
        private const val KEY_AUTO_CHECK = "update_auto_check_enabled"
        private const val KEY_LAST_CHECK = "update_last_check"
        private const val KEY_WHATS_NEW_VERSION = "whats_new_last_seen_version"
        private const val AUTO_CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000
    }
}

/** Extracts the Markdown block under `[versionName]` from the full changelog text. */
@Suppress("SameParameterValue")
private fun extractVersionChangelog(fullText: String, versionName: String): String {
    val normalized = versionName.removePrefix("v").removePrefix("V")
    val lines = fullText.lines()
    val startRegex = Regex("^\\[${Regex.escape(normalized)}]$")
    val startIndex = lines.indexOfFirst { it.trim().matches(startRegex) }
    if (startIndex == -1) return ""
    val remaining = lines.drop(startIndex + 1)
    val endIndex = remaining.indexOfFirst { it.trim().matches(Regex("^\\[.+]$")) }
    val section = if (endIndex == -1) remaining else remaining.take(endIndex)
    return section.joinToString("\n").trim()
}

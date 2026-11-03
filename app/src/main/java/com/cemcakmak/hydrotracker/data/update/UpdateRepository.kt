package com.cemcakmak.hydrotracker.data.update

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.content.edit
import com.cemcakmak.hydrotracker.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Owns update-check state and preferences. Uses the [UpdateChecker] for this distribution flavour
 * and exposes the latest [UpdateStatus] plus the auto-check toggle.
 */
open class UpdateRepository(
    context: Context,
    private val checker: UpdateChecker = UpdateCheckerFactory.create(context.applicationContext)
) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("hydrotracker_prefs", Context.MODE_PRIVATE)
    // Update execution must outlive the dialogue or screen that initiated it.
    private val updateScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var updateInProgress = false

    val installSource: InstallSource get() = checker.source

    private val restoredStatus = checker.restoreState()
    private val _updateStatus = MutableStateFlow(
        restoredStatus ?: if (checker.source == InstallSource.F_DROID) {
            UpdateStatus.Unsupported
        } else {
            UpdateStatus.Idle
        }
    )
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    protected fun setUpdateStatus(status: UpdateStatus) {
        _updateStatus.value = status
    }

    private val _activeUpdate = MutableStateFlow(
        when (restoredStatus) {
            is UpdateStatus.ReadyToInstall -> restoredStatus.available
            is UpdateStatus.PermissionRequired -> restoredStatus.available
            else -> null
        }
    )
    val activeUpdate: StateFlow<UpdateStatus.Available?> = _activeUpdate.asStateFlow()

    private val _autoCheckEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_CHECK, true))
    val autoCheckEnabled: StateFlow<Boolean> = _autoCheckEnabled.asStateFlow()

    private val _lastCheckTime = MutableStateFlow(prefs.getLong(KEY_LAST_CHECK, 0L))
    val lastCheckTime: StateFlow<Long> = _lastCheckTime.asStateFlow()

    private val _whatsNewAvailable = MutableStateFlow(false)
    val whatsNewAvailable: StateFlow<Boolean> = _whatsNewAvailable.asStateFlow()

    private val _simulatedDownloaded = MutableStateFlow(false)
    val simulatedDownloaded: StateFlow<Boolean> = _simulatedDownloaded.asStateFlow()

    init {
        (checker as? UpdateActionHandler)?.restoreActionListener { newStatus ->
            _updateStatus.value = newStatus
            if (newStatus is UpdateStatus.UpToDate) _activeUpdate.value = null
        }
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
        if (_activeUpdate.value != null && _updateStatus.value !is UpdateStatus.Failed) return
        _activeUpdate.value = null
        _updateStatus.value = UpdateStatus.Checking
        val result = try {
            checker.check()
        } catch (e: Exception) {
            e.toUpdateFailure(UpdateError.CHECK_FAILED)
        }
        val now = System.currentTimeMillis()
        prefs.edit { putLong(KEY_LAST_CHECK, now) }
        _lastCheckTime.value = now
        _updateStatus.value = result
    }

    /** Starts the current flavour's user-confirmed update flow independently of Compose lifecycles. */
    fun startUpdate(
        status: UpdateStatus.Available,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        onError: (UpdateError) -> Unit = {}
    ) {
        val handler = checker as? UpdateActionHandler
        if (handler == null) {
            onError(UpdateError.MANAGED_EXTERNALLY)
            return
        }

        synchronized(this) {
            if (updateInProgress) return
            updateInProgress = true
        }
        if (status.mechanism == UpdateMechanism.GITHUB_APK) {
            _activeUpdate.value = status
            _updateStatus.value = UpdateStatus.Downloading(0)
        } else {
            _activeUpdate.value = null
        }

        updateScope.launch {
            try {
                handler.startUpdate(status, launcher) { newStatus ->
                    _updateStatus.value = newStatus
                    if (newStatus is UpdateStatus.UpToDate) _activeUpdate.value = null
                }
            } catch (e: Exception) {
                val failure = e.toUpdateFailure(UpdateError.START_FAILED)
                _updateStatus.value = failure
                onError(failure.error)
            } finally {
                updateInProgress = false
            }
        }
    }

    /** Starts Android's installer for an APK that has already been downloaded and verified. */
    fun installUpdate() {
        val available = _activeUpdate.value
            ?: (_updateStatus.value as? UpdateStatus.ReadyToInstall)?.available
            ?: return
        val handler = checker as? UpdateActionHandler ?: return

        synchronized(this) {
            if (updateInProgress) return
            updateInProgress = true
        }
        _activeUpdate.value = available
        _updateStatus.value = UpdateStatus.Installing

        updateScope.launch {
            try {
                handler.installUpdate { newStatus ->
                    _updateStatus.value = newStatus
                    if (newStatus is UpdateStatus.UpToDate) _activeUpdate.value = null
                }
            } catch (e: Exception) {
                _updateStatus.value = e.toUpdateFailure(UpdateError.INSTALL_FAILED)
            } finally {
                updateInProgress = false
            }
        }
    }

    /** Records Play's activity result so its flow can be retried after cancellation. */
    fun onUpdateFlowResult(resultCode: Int) {
        (checker as? UpdateActionHandler)?.onUpdateFlowResult(resultCode)
    }

    /** Completes a flexible Play update after its download has finished. */
    fun completeUpdate() {
        (checker as? UpdateActionHandler)?.completeUpdate()
    }

    /** Resumes Android's installation confirmation after returning to the app. */
    fun resumeUpdate(): Boolean {
        return (checker as? UpdateActionHandler)?.resumeUpdate() ?: false
    }

    /** Observes Play's downloaded state; other flavours return a no-op clean-up action. */
    fun observeDownloaded(onDownloaded: () -> Unit): () -> Unit {
        return (checker as? UpdateActionHandler)?.observeDownloaded(onDownloaded) ?: {}
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

    /** Clears session metadata without cancelling platform work, used by debug channel switching. */
    fun clearUpdateSession() {
        _activeUpdate.value = null
    }

    fun markWhatsNewSeen() {
        prefs.edit { putString(KEY_WHATS_NEW_VERSION, BuildConfig.VERSION_NAME) }
        _whatsNewAvailable.value = false
    }

    /** Debug: forces the update status to Available so the UI can be tested. */
    fun simulateUpdateAvailable(versionName: String = "99.99.99") {
        _updateStatus.value = UpdateStatus.Available(
            versionName = versionName,
            mechanism = if (installSource == InstallSource.PLAY_STORE) {
                UpdateMechanism.PLAY_IN_APP
            } else {
                UpdateMechanism.GITHUB_APK
            },
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

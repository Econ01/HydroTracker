package com.cemcakmak.hydrotracker.data.update

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.ktx.isFlexibleUpdateAllowed
import com.google.android.play.core.ktx.isImmediateUpdateAllowed
import com.google.android.play.core.ktx.requestAppUpdateInfo

/** Uses Google Play In-App Updates for the Play distribution flavour. */
class PlayUpdateChecker(context: Context) : UpdateChecker, UpdateActionHandler {

    override val source: InstallSource = InstallSource.PLAY_STORE

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context)
    private var pendingUpdateInfo: AppUpdateInfo? = null
    private var pendingUpdateType: Int? = null
    private var updateFlowInProgress = false
    private var inFlightUpdateType: Int? = null

    override suspend fun check(): UpdateStatus {
        pendingUpdateInfo = null
        pendingUpdateType = null
        updateFlowInProgress = false
        inFlightUpdateType = null
        val info = try {
            appUpdateManager.requestAppUpdateInfo()
        } catch (e: Exception) {
            return e.toUpdateFailure(UpdateError.CHECK_FAILED)
        }

        val supportsFlexible = info.isFlexibleUpdateAllowed
        val supportsImmediate = info.isImmediateUpdateAllowed
        val updateType = when {
            supportsFlexible -> AppUpdateType.FLEXIBLE
            supportsImmediate -> AppUpdateType.IMMEDIATE
            else -> null
        }

        pendingUpdateInfo = info.takeIf { updateType != null }
        pendingUpdateType = updateType

        return if (updateType == null) {
            UpdateStatus.UpToDate
        } else {
            UpdateStatus.Available(
                versionName = info.availableVersionCode().toString(),
                mechanism = UpdateMechanism.PLAY_IN_APP
            )
        }
    }

    override suspend fun startUpdate(
        status: UpdateStatus.Available,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        onStatusChanged: (UpdateStatus) -> Unit
    ) {
        require(status.mechanism == UpdateMechanism.PLAY_IN_APP)
        if (updateFlowInProgress) return
        val info = pendingUpdateInfo ?: throw UpdateException(
            UpdateError.PLAY_RECHECK_REQUIRED,
            "Check Google Play again before updating"
        )
        val updateType = pendingUpdateType ?: throw UpdateException(
            UpdateError.PLAY_UPDATE_UNAVAILABLE,
            "No Play update type is available"
        )
        val started = appUpdateManager.startUpdateFlowForResult(
            info,
            launcher,
            AppUpdateOptions.defaultOptions(updateType)
        )
        if (!started) {
            throw UpdateException(UpdateError.START_FAILED, "Google Play couldn't start the update")
        }
        updateFlowInProgress = true
        inFlightUpdateType = updateType
        pendingUpdateInfo = null
        pendingUpdateType = null
    }

    override fun onUpdateFlowResult(resultCode: Int) {
        if (resultCode != Activity.RESULT_OK || inFlightUpdateType == AppUpdateType.IMMEDIATE) {
            updateFlowInProgress = false
            inFlightUpdateType = null
        }
    }

    override fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }

    override fun observeDownloaded(onDownloaded: () -> Unit): () -> Unit {
        val listener = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                updateFlowInProgress = false
                inFlightUpdateType = null
                onDownloaded()
            }
        }
        appUpdateManager.registerListener(listener)
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                updateFlowInProgress = false
                inFlightUpdateType = null
                onDownloaded()
            }
        }
        return { appUpdateManager.unregisterListener(listener) }
    }
}

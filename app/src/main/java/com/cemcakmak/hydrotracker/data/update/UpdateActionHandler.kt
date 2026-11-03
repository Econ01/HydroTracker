package com.cemcakmak.hydrotracker.data.update

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest

/**
 * Channel-neutral operations exposed by a flavour's update implementation.
 *
 * Google types never cross this boundary, which keeps the GitHub and F-Droid artefacts free of
 * proprietary Play dependencies.
 */
interface UpdateActionHandler {
    suspend fun startUpdate(
        status: UpdateStatus.Available,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        onStatusChanged: (UpdateStatus) -> Unit
    )

    /** Starts Android's installer after a GitHub APK has been downloaded and verified. */
    suspend fun installUpdate(onStatusChanged: (UpdateStatus) -> Unit) {
        throw UpdateException(UpdateError.NO_DOWNLOADED_UPDATE, "No downloaded update is ready")
    }

    /** Reattaches an existing operation to a newly created repository after recreation. */
    fun restoreActionListener(onStatusChanged: (UpdateStatus) -> Unit) = Unit

    /** Records that Play's update activity returned, allowing a later retry. */
    fun onUpdateFlowResult(resultCode: Int) = Unit

    fun completeUpdate() = Unit

    /** Resumes a pending system installation confirmation, if one exists. */
    fun resumeUpdate(): Boolean = false

    /** Observes Play's flexible-update downloaded state. Returns the corresponding clean-up action. */
    fun observeDownloaded(onDownloaded: () -> Unit): () -> Unit = {}
}

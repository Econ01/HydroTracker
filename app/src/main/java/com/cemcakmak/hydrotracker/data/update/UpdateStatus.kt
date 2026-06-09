package com.cemcakmak.hydrotracker.data.update

import com.google.android.play.core.appupdate.AppUpdateInfo

/** The result of an update check, rendered by the Updates screen. */
sealed interface UpdateStatus {
    /** No check has run yet. */
    data object Idle : UpdateStatus

    /** A check is currently in progress. */
    data object Checking : UpdateStatus

    /** The installed version is the latest. */
    data object UpToDate : UpdateStatus

    /** Update checking isn't applicable on this channel (e.g. F-Droid handles its own updates). */
    data object Unsupported : UpdateStatus

    /** The check failed (no network, API error, …). */
    data class Failed(val message: String) : UpdateStatus

    /** A newer version is available. */
    data class Available(
        val versionName: String,
        /** Non-null for Play installs; holds the update info needed to launch the in-app flow. */
        val playUpdateInfo: AppUpdateInfo? = null,
        /** The allowed Play update type ([AppUpdateType.FLEXIBLE] or [AppUpdateType.IMMEDIATE]); null for non-Play. */
        val playUpdateType: Int? = null,
        /** Non-null for GitHub/sideload installs; the URL to the APK or release page. */
        val downloadUrl: String? = null
    ) : UpdateStatus
}

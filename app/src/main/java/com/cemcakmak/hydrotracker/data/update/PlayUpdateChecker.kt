package com.cemcakmak.hydrotracker.data.update

import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.ktx.isFlexibleUpdateAllowed
import com.google.android.play.core.ktx.isImmediateUpdateAllowed
import com.google.android.play.core.ktx.requestAppUpdateInfo

/**
 * Uses Google Play In-App Updates to report whether Play has a newer version.
 */
class PlayUpdateChecker(context: Context) : UpdateChecker {

    override val source: InstallSource = InstallSource.PLAY_STORE

    val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context)

    override suspend fun check(): UpdateStatus {
        val info = try {
            appUpdateManager.requestAppUpdateInfo()
        } catch (e: Exception) {
            return UpdateStatus.Failed(e.message ?: "Couldn't reach Google Play")
        }

        val supportsFlexible = info.isFlexibleUpdateAllowed
        val supportsImmediate = info.isImmediateUpdateAllowed

        return when {
            supportsFlexible -> UpdateStatus.Available(
                versionName = info.availableVersionCode().toString(),
                playUpdateInfo = info,
                playUpdateType = AppUpdateType.FLEXIBLE
            )
            supportsImmediate -> UpdateStatus.Available(
                versionName = info.availableVersionCode().toString(),
                playUpdateInfo = info,
                playUpdateType = AppUpdateType.IMMEDIATE
            )
            else -> UpdateStatus.UpToDate
        }
    }
}

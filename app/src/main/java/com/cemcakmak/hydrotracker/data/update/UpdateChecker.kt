package com.cemcakmak.hydrotracker.data.update

import android.content.Context
import android.os.Build

// Where this APK was installed from
enum class InstallSource {
    // Google Play Store (`com.android.vending`)
    PLAY_STORE,

    // F-Droid client (`org.fdroid.fdroid` / `org.fdroid.basic`)
    F_DROID,

    // Sideloaded APK, unknown installer, or anything else.
    OTHER
}

/**
 * Detects the installer package and maps it to an [InstallSource]. Uses
 * [android.content.pm.PackageManager.getInstallSourceInfo] on API 30+, falling back to the
 * deprecated `getInstallerPackageName()` below that. Any failure is treated as [InstallSource.OTHER].
 */
private fun detectInstallSource(context: Context): InstallSource {
    val packageName = context.packageName
    val installer: String? = try {
        val pm = context.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pm.getInstallSourceInfo(packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            pm.getInstallerPackageName(packageName)
        }
    } catch (_: Exception) {
        null
    }

    return when (installer) {
        "com.android.vending" -> InstallSource.PLAY_STORE
        "org.fdroid.fdroid", "org.fdroid.basic" -> InstallSource.F_DROID
        else -> InstallSource.OTHER
    }
}

interface UpdateChecker {
    val source: InstallSource
    suspend fun check(): UpdateStatus
}

/** Used on channels that manage their own updates (e.g. F-Droid). Always reports [UpdateStatus.Unsupported]. */
private object FdroidUpdateChecker : UpdateChecker {
    override val source = InstallSource.F_DROID
    override suspend fun check() = UpdateStatus.Unsupported
}

/** Builds the right [UpdateChecker] for the channel this build was installed from. */
object UpdateCheckerFactory {
    fun create(context: Context): UpdateChecker {
        return when (detectInstallSource(context)) {
            InstallSource.PLAY_STORE -> PlayUpdateChecker(context.applicationContext)
            InstallSource.F_DROID -> FdroidUpdateChecker
            InstallSource.OTHER -> GitHubUpdateChecker()
        }
    }
}

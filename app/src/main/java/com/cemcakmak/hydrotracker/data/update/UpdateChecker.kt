package com.cemcakmak.hydrotracker.data.update

/** The distribution channel for which this APK was built. */
enum class InstallSource {
    PLAY_STORE,
    F_DROID,
    OTHER
}

interface UpdateChecker {
    val source: InstallSource
    suspend fun check(): UpdateStatus

    /** Restores an unfinished update after activity or process recreation, when available. */
    fun restoreState(): UpdateStatus? = null
}

/** Used on channels that manage their own updates. Always reports [UpdateStatus.Unsupported]. */
internal object FdroidUpdateChecker : UpdateChecker {
    override val source = InstallSource.F_DROID
    override suspend fun check() = UpdateStatus.Unsupported
}

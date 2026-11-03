package com.cemcakmak.hydrotracker.data.update

import android.content.Context

/** Debug-only state controls used by the Update Testing screen. */
class DebugUpdateRepository(context: Context) : UpdateRepository(context) {
    fun simulate(status: UpdateStatus) {
        setUpdateStatus(status)
    }

    fun resetSimulation() {
        clearUpdateSession()
        setUpdateStatus(
            if (installSource == InstallSource.F_DROID) {
                UpdateStatus.Unsupported
            } else {
                UpdateStatus.Idle
            }
        )
    }
}

/** Selects the debug repository with state-simulation controls. */
object UpdateRepositoryFactory {
    fun create(context: Context): UpdateRepository = DebugUpdateRepository(context.applicationContext)
}

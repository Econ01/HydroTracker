package com.cemcakmak.hydrotracker.data.update

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest

/** Selects a real Play, GitHub or F-Droid implementation at runtime in the unified debug app. */
class DebugChannelUpdateChecker(context: Context) : UpdateChecker, UpdateActionHandler {
    private val appContext = context.applicationContext
    private val play = PlayUpdateChecker(appContext)
    private val github = GitHubUpdateChecker(
        context = appContext,
        feedUrlProvider = { GitHubUpdateTestConfig.feedUrl(appContext) },
        allowCleartext = true
    )
    private val fdroid = FdroidUpdateChecker

    private val delegate: UpdateChecker
        get() = when (DebugChannelConfig.channel(appContext)) {
            InstallSource.PLAY_STORE -> play
            InstallSource.F_DROID -> fdroid
            InstallSource.OTHER -> github
        }

    override val source: InstallSource get() = delegate.source

    override suspend fun check(): UpdateStatus = delegate.check()

    override fun restoreState(): UpdateStatus? = delegate.restoreState()

    override suspend fun startUpdate(
        status: UpdateStatus.Available,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        onStatusChanged: (UpdateStatus) -> Unit
    ) = delegateAsHandler().startUpdate(status, launcher, onStatusChanged)

    override suspend fun installUpdate(onStatusChanged: (UpdateStatus) -> Unit) =
        delegateAsHandler().installUpdate(onStatusChanged)

    override fun cancelUpdate(status: UpdateStatus.Available) = delegateAsHandler().cancelUpdate(status)

    override fun restoreActionListener(onStatusChanged: (UpdateStatus) -> Unit) {
        (delegate as? UpdateActionHandler)?.restoreActionListener(onStatusChanged)
    }

    override fun onUpdateFlowResult(resultCode: Int) = delegateAsHandler().onUpdateFlowResult(resultCode)

    override fun completeUpdate() = delegateAsHandler().completeUpdate()

    override fun resumeUpdate(): Boolean = delegateAsHandler().resumeUpdate()

    override fun observeDownloaded(onDownloaded: () -> Unit): () -> Unit {
        return (delegate as? UpdateActionHandler)?.observeDownloaded(onDownloaded) ?: {}
    }

    private fun delegateAsHandler(): UpdateActionHandler {
        return delegate as? UpdateActionHandler
            ?: throw UpdateException(UpdateError.MANAGED_EXTERNALLY, "Updates are managed by F-Droid")
    }
}

/** Creates the unified debug checker with runtime channel selection. */
object UpdateCheckerFactory {
    fun create(context: Context): UpdateChecker = DebugChannelUpdateChecker(context.applicationContext)
}

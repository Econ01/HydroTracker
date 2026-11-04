package com.cemcakmak.hydrotracker.data.update

import android.app.Application
import android.os.Looper
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class UpdateRepositoryTest {

    @Test
    fun `typed update exception preserves error and diagnostic details`() {
        val exception = UpdateException(UpdateError.INVALID_UPDATE_METADATA, "Missing APK digest")

        val failure = exception.toUpdateFailure(UpdateError.START_FAILED)

        assertEquals(UpdateError.INVALID_UPDATE_METADATA, failure.error)
        assertEquals("Missing APK digest", failure.technicalDetails)
    }

    @Test
    fun `unexpected exception uses fallback error and retains diagnostic details`() {
        val failure = IllegalStateException("Installer unavailable")
            .toUpdateFailure(UpdateError.INSTALL_FAILED)

        assertEquals(UpdateError.INSTALL_FAILED, failure.error)
        assertEquals("Installer unavailable", failure.technicalDetails)
    }

    @Test
    fun `cancelling restores the same update for retry`() {
        val checker = CancellableUpdateChecker()
        val repository = UpdateRepository(
            ApplicationProvider.getApplicationContext(),
            checker
        )
        val status = UpdateStatus.Available(
            versionName = "1.8.1",
            mechanism = UpdateMechanism.GITHUB_APK,
            downloadUrl = "https://example.com/update.apk"
        )

        repository.startUpdate(status, NoopLauncher)
        repository.cancelUpdate()

        assertEquals(status, repository.updateStatus.value)
        assertEquals(status, repository.activeUpdate.value)
        assertEquals(status, checker.cancelledUpdate)
    }

    @Test
    fun `permission required resumes installation only when permission is granted`() {
        val checker = CancellableUpdateChecker()
        val repository = UpdateRepository(
            ApplicationProvider.getApplicationContext(),
            checker
        )
        val status = UpdateStatus.Available(
            versionName = "1.8.1",
            mechanism = UpdateMechanism.GITHUB_APK,
            downloadUrl = "https://example.com/update.apk"
        )
        checker.installPermissionGranted = false
        checker.startStatus = UpdateStatus.PermissionRequired(status)
        repository.startUpdate(status, NoopLauncher)

        assertEquals(false, repository.resumeUpdate())
        assertEquals(0, checker.installCalls)

        checker.installPermissionGranted = true
        assertEquals(true, repository.resumeUpdate())
        assertEquals(1, checker.installCalls)
    }

    @Test
    fun `repeated starts are single-flight until the active operation completes`() = runBlocking {
        val checker = BlockingUpdateChecker()
        val repository = UpdateRepository(
            ApplicationProvider.getApplicationContext(),
            checker
        )
        val status = UpdateStatus.Available(
            versionName = "1.8.1",
            mechanism = UpdateMechanism.GITHUB_APK,
            downloadUrl = "https://example.com/update.apk"
        )

        repository.startUpdate(status, NoopLauncher)
        repository.startUpdate(status, NoopLauncher)

        assertEquals(1, checker.startCalls)

        checker.completeCurrent()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        repository.startUpdate(status, NoopLauncher)
        assertEquals(2, checker.startCalls)
        checker.completeCurrent()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    private class CancellableUpdateChecker : UpdateChecker, UpdateActionHandler {
        var cancelledUpdate: UpdateStatus.Available? = null
            private set
        var installPermissionGranted = true
        var installCalls = 0
            private set
        var startStatus: UpdateStatus? = null

        override val source = InstallSource.OTHER
        override suspend fun check(): UpdateStatus = UpdateStatus.UpToDate

        override suspend fun startUpdate(
            status: UpdateStatus.Available,
            launcher: ActivityResultLauncher<IntentSenderRequest>,
            onStatusChanged: (UpdateStatus) -> Unit
        ) {
            startStatus?.let(onStatusChanged)
        }

        override suspend fun installUpdate(onStatusChanged: (UpdateStatus) -> Unit) {
            installCalls++
        }

        override fun hasInstallPermission(): Boolean = installPermissionGranted

        override fun cancelUpdate(status: UpdateStatus.Available) {
            cancelledUpdate = status
        }
    }

    private class BlockingUpdateChecker : UpdateChecker, UpdateActionHandler {
        var startCalls = 0
            private set
        private var gate = CompletableDeferred<Unit>()

        override val source = InstallSource.OTHER
        override suspend fun check(): UpdateStatus = UpdateStatus.UpToDate

        override suspend fun startUpdate(
            status: UpdateStatus.Available,
            launcher: ActivityResultLauncher<IntentSenderRequest>,
            onStatusChanged: (UpdateStatus) -> Unit
        ) {
            startCalls++
            gate.await()
        }

        fun completeCurrent() {
            gate.complete(Unit)
            gate = CompletableDeferred()
        }
    }

    private object NoopLauncher : ActivityResultLauncher<IntentSenderRequest>() {
        override val contract = ActivityResultContracts.StartIntentSenderForResult()
        override fun launch(input: IntentSenderRequest, options: ActivityOptionsCompat?) = Unit
        override fun unregister() = Unit
    }
}

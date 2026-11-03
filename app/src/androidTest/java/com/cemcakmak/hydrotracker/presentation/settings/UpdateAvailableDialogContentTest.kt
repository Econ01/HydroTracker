package com.cemcakmak.hydrotracker.presentation.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.cemcakmak.hydrotracker.data.update.InstallSource
import com.cemcakmak.hydrotracker.data.update.UpdateError
import com.cemcakmak.hydrotracker.data.update.UpdateMechanism
import com.cemcakmak.hydrotracker.data.update.UpdateStatus
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UpdateAvailableDialogContentTest {
    @get:Rule
    val compose = createComposeRule()

    private val available = UpdateStatus.Available(
        versionName = "1.8.1",
        mechanism = UpdateMechanism.GITHUB_APK,
        downloadUrl = "https://example.com/HydroTracker-1.8.1.apk",
        downloadSha256 = "0".repeat(64)
    )

    @Test
    fun completeModalFlowRetainsSessionAndUsesCorrectActions() {
        var status by mutableStateOf<UpdateStatus>(available)
        var starts = 0
        var installs = 0
        var resumes = 0

        compose.setContent {
            HydroTrackerTheme {
                UpdateAvailableDialogContent(
                    status = status,
                    activeUpdate = available,
                    installSource = InstallSource.OTHER,
                    onStartUpdate = { starts++ },
                    onInstallUpdate = { installs++ },
                    onResumeUpdate = { resumes++ }
                )
            }
        }

        compose.onNodeWithTag("update_primary_action").performClick()
        assertEquals(1, starts)

        compose.runOnIdle { status = UpdateStatus.Downloading(50) }
        compose.onNodeWithTag("update_progress").assertIsDisplayed()
        compose.onNodeWithTag("update_status_text").assertIsDisplayed()

        compose.runOnIdle { status = UpdateStatus.Verifying }
        compose.onNodeWithTag("update_status_text").assertIsDisplayed()

        compose.runOnIdle { status = UpdateStatus.ReadyToInstall(available) }
        compose.onNodeWithTag("update_primary_action").performClick()
        assertEquals(1, installs)

        compose.runOnIdle { status = UpdateStatus.Installing }
        compose.onNodeWithTag("update_primary_action").performClick()
        assertEquals(1, resumes)

        compose.onNodeWithTag("update_flow_dialog").assertIsDisplayed()
    }

    @Test
    fun availableLaterDismissesWithoutStartingUpdate() {
        var dismissed = false
        var starts = 0

        compose.setContent {
            HydroTrackerTheme {
                UpdateAvailableDialogContent(
                    status = available,
                    activeUpdate = available,
                    installSource = InstallSource.OTHER,
                    onDismiss = { dismissed = true },
                    onStartUpdate = { starts++ }
                )
            }
        }

        compose.onNodeWithTag("update_secondary_action").performClick()
        assertTrue(dismissed)
        assertEquals(0, starts)
    }

    @Test
    fun permissionRequiredRetriesSameUpdate() {
        var retried: UpdateStatus.Available? = null

        compose.setContent {
            HydroTrackerTheme {
                UpdateAvailableDialogContent(
                    status = UpdateStatus.PermissionRequired(available),
                    activeUpdate = available,
                    installSource = InstallSource.OTHER,
                    onStartUpdate = { retried = it }
                )
            }
        }

        compose.onNodeWithTag("update_primary_action").performClick()
        assertEquals(available, retried)
    }

    @Test
    fun failureRetriesSameUpdateWithoutDismissal() {
        var retried: UpdateStatus.Available? = null
        var dismissed = false

        compose.setContent {
            HydroTrackerTheme {
                UpdateAvailableDialogContent(
                    status = UpdateStatus.Failed(UpdateError.VERIFICATION_FAILED, "Digest mismatch"),
                    activeUpdate = available,
                    installSource = InstallSource.OTHER,
                    onDismiss = { dismissed = true },
                    onStartUpdate = { retried = it }
                )
            }
        }

        compose.onNodeWithTag("update_primary_action").performClick()
        assertEquals(available, retried)
        compose.onNodeWithTag("update_flow_dialog").assertIsDisplayed()
        assertEquals(false, dismissed)
    }
}

package com.cemcakmak.hydrotracker.data.update

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class GitHubUpdateNotificationTest {

    @Test
    fun `determinate progress uses percentage in progress style`() {
        val style = updateProgressStyle(65)

        assertEquals(65, style.progress)
        assertFalse(style.isProgressIndeterminate)
    }

    @Test
    fun `missing progress uses indeterminate progress style`() {
        val style = updateProgressStyle(null)

        assertTrue(style.isProgressIndeterminate)
    }
}

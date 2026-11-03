package com.cemcakmak.hydrotracker.data.update

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class GitHubUpdateTestConfigTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `saved endpoint and scenario are used to build the debug feed URL`() {
        GitHubUpdateTestConfig.save(context, "http://192.168.1.20:8765/", "bad_digest")

        assertEquals("http://192.168.1.20:8765", GitHubUpdateTestConfig.baseUrl(context))
        assertEquals("bad_digest", GitHubUpdateTestConfig.scenario(context))
        assertEquals(
            "http://192.168.1.20:8765/release.json?scenario=bad_digest",
            GitHubUpdateTestConfig.feedUrl(context)
        )
    }
}

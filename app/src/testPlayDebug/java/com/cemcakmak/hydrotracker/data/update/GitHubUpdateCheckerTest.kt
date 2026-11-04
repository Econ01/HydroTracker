package com.cemcakmak.hydrotracker.data.update

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class GitHubUpdateCheckerTest {

    @Test
    fun `version comparison handles prefixes lengths and suffixes`() {
        assertTrue(isNewerVersion("v1.8", "1.7"))
        assertTrue(isNewerVersion("V1.8.1", "1.8"))
        assertTrue(isNewerVersion("1.8.0.1", "1.8"))
        assertTrue(isNewerVersion("1.9-rc1", "1.8"))

        assertFalse(isNewerVersion("1.8", "1.8"))
        assertFalse(isNewerVersion("v1.7", "1.8"))
        assertFalse(isNewerVersion("1.8", "1.8.0.1"))
    }

    @Test
    fun `asset selection chooses GitHub APK instead of F-Droid APK`() {
        val release = releaseWithAssets(
            "HydroTracker-1.8-fdroid.apk",
            "HydroTracker-1.8.apk",
            "HydroTracker-1.8.aab"
        )

        val selected = findGitHubApkAsset(release, "v1.8")

        assertEquals("HydroTracker-1.8.apk", selected?.optString("name"))
        assertEquals(
            "https://example.com/HydroTracker-1.8.apk",
            selected?.optString("browser_download_url")
        )
    }

    @Test
    fun `asset selection rejects missing or differently versioned APK`() {
        val release = releaseWithAssets("HydroTracker-1.8-fdroid.apk")

        assertNull(findGitHubApkAsset(release, "v1.8"))
        assertNull(findGitHubApkAsset(release, "v1.9"))
    }

    @Test
    fun `URL policy requires HTTPS outside the debug harness`() {
        requireUpdateUrlAllowed(java.net.URL("https://example.com/release.json"), false)
        requireUpdateUrlAllowed(java.net.URL("http://10.0.2.2:8765/release.json"), true)

        val insecureHttp = org.junit.Assert.assertThrows(UpdateException::class.java) {
            requireUpdateUrlAllowed(java.net.URL("http://example.com/release.json"), false)
        }
        assertEquals(UpdateError.INSECURE_URL, insecureHttp.error)
        org.junit.Assert.assertThrows(UpdateException::class.java) {
            requireUpdateUrlAllowed(java.net.URL("ftp://example.com/release.json"), true)
        }
    }

    @Test
    fun `local endpoint detection distinguishes test server from public releases`() {
        assertTrue(isLocalUpdateEndpoint(java.net.URL("http://10.0.2.2:8765/release.json")))
        assertTrue(isLocalUpdateEndpoint(java.net.URL("http://127.0.0.1:8765/release.json")))
        assertTrue(isLocalUpdateEndpoint(java.net.URL("http://192.168.178.26:8765/release.json")))
        assertTrue(isLocalUpdateEndpoint(java.net.URL("http://172.20.0.1:8765/release.json")))
        assertTrue(isLocalUpdateEndpoint(java.net.URL("http://printer.local:8765/release.json")))

        assertFalse(isLocalUpdateEndpoint(java.net.URL("https://api.github.com/repos/Econ01/HydroTracker/releases/latest")))
        assertFalse(isLocalUpdateEndpoint(java.net.URL("https://172.32.0.1:8765/release.json")))
    }

    private fun releaseWithAssets(vararg names: String): JSONObject {
        val assets = names.joinToString(separator = ",") { name ->
            """{"name":"$name","browser_download_url":"https://example.com/$name"}"""
        }
        return JSONObject("""{"tag_name":"v1.8","assets":[$assets]}""")
    }
}

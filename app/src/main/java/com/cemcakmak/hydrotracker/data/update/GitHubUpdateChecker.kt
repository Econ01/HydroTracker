package com.cemcakmak.hydrotracker.data.update

import com.cemcakmak.hydrotracker.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Checks the latest GitHub release for a newer version. It is used for sideloaded installations.
 */
class GitHubUpdateChecker(
    private val owner: String = "Econ01",
    private val repo: String = "HydroTracker"
) : UpdateChecker {

    override val source: InstallSource = InstallSource.OTHER

    override suspend fun check(): UpdateStatus = withContext(Dispatchers.IO) {
        try {
            val release = fetchLatestRelease()
            val tag = release.optString("tag_name")
            if (tag.isBlank()) return@withContext UpdateStatus.Failed("No release found")

            if (!isNewerVersion(tag, BuildConfig.VERSION_NAME)) {
                return@withContext UpdateStatus.UpToDate
            }

            val downloadUrl = pickApkUrl(release) ?: release.optString("html_url")
            if (downloadUrl.isNullOrBlank()) {
                return@withContext UpdateStatus.Failed("No download available")
            }

            UpdateStatus.Available(
                versionName = tag.removePrefix("v").removePrefix("V"),
                downloadUrl = downloadUrl
            )
        } catch (e: Exception) {
            UpdateStatus.Failed(e.message ?: "Couldn't check for updates")
        }
    }

    private fun fetchLatestRelease(): JSONObject {
        val url = URL("https://api.github.com/repos/$owner/$repo/releases/latest")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "HydroTracker")
        }
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("GitHub API returned ${conn.responseCode}")
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(body)
        } finally {
            conn.disconnect()
        }
    }

    // First release asset whose name ends in `.apk`
    private fun pickApkUrl(release: JSONObject): String? {
        val assets = release.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                return asset.optString("browser_download_url").ifBlank { null }
            }
        }
        return null
    }
}

/**
 * Compares dotted numeric versions (e.g. "1.0.6.1"). A leading "v"/"V" is stripped and any
 * non-numeric suffix on a component (e.g. "-dev", "-rc1") is ignored. Returns true when
 * [remoteTag] is strictly newer than [current].
 */
internal fun isNewerVersion(remoteTag: String, current: String): Boolean {
    val remote = parseVersion(remoteTag)
    val local = parseVersion(current)
    val size = maxOf(remote.size, local.size)
    for (i in 0 until size) {
        val r = remote.getOrElse(i) { 0 }
        val l = local.getOrElse(i) { 0 }
        if (r != l) return r > l
    }
    return false
}

private fun parseVersion(raw: String): List<Int> {
    return raw.trim()
        .removePrefix("v")
        .removePrefix("V")
        .split(".")
        .map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
}

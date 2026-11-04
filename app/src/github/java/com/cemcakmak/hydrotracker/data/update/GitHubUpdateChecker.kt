package com.cemcakmak.hydrotracker.data.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.content.ContextCompat
import com.cemcakmak.hydrotracker.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Checks GitHub releases and performs user-initiated APK updates for the GitHub flavour. */
class GitHubUpdateChecker(
    context: Context,
    private val feedUrlProvider: () -> String = {
        "https://api.github.com/repos/Econ01/HydroTracker/releases/latest"
    },
    private val allowCleartext: Boolean = false
) : UpdateChecker, UpdateActionHandler {

    private val appContext = context.applicationContext
    private val installer = GitHubApkInstaller(appContext, allowCleartext)

    override val source: InstallSource = InstallSource.OTHER

    override fun restoreState(): UpdateStatus? {
        if (installer.hasPendingConfirmation()) return UpdateStatus.Installing
        return installer.readyUpdate()?.let(UpdateStatus::ReadyToInstall)
    }

    override suspend fun check(): UpdateStatus = withContext(Dispatchers.IO) {
        restoreState()?.let { restored ->
            if (restored is UpdateStatus.Installing) installer.resumePendingInstallation()
            return@withContext restored
        }

        val endpoint = URL(feedUrlProvider())
        if (isLocalUpdateEndpoint(endpoint) && !hasLocalNetworkPermission()) {
            return@withContext UpdateStatus.Failed(UpdateError.LOCAL_NETWORK_PERMISSION_REQUIRED)
        }

        try {
            val release = fetchLatestRelease(endpoint)
            val tag = release.optString("tag_name")
            if (tag.isBlank()) {
                return@withContext UpdateStatus.Failed(UpdateError.UPDATE_NOT_AVAILABLE)
            }

            if (!isNewerVersion(tag, BuildConfig.VERSION_NAME)) {
                return@withContext UpdateStatus.UpToDate
            }

            val asset = findGitHubApkAsset(release, tag)
                ?: return@withContext UpdateStatus.Failed(UpdateError.UPDATE_NOT_AVAILABLE)
            val downloadUrl = asset.optString("browser_download_url")
            if (downloadUrl.isBlank()) {
                return@withContext UpdateStatus.Failed(UpdateError.INVALID_UPDATE_METADATA)
            }

            UpdateStatus.Available(
                versionName = tag.removePrefix("v").removePrefix("V"),
                mechanism = UpdateMechanism.GITHUB_APK,
                downloadUrl = downloadUrl,
                downloadSize = asset.optLong("size").takeIf { it > 0L },
                downloadSha256 = asset.optString("digest")
                    .removePrefix("sha256:")
                    .ifBlank { null }
            )
        } catch (e: Exception) {
            e.toUpdateFailure(UpdateError.CHECK_FAILED)
        }
    }

    override suspend fun startUpdate(
        status: UpdateStatus.Available,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        onStatusChanged: (UpdateStatus) -> Unit
    ) {
        require(status.mechanism == UpdateMechanism.GITHUB_APK)
        GitHubApkInstaller.registerStatusChanged(appContext, onStatusChanged)
        GitHubUpdateDownloadService.start(appContext, status, allowCleartext)
    }

    override suspend fun installUpdate(onStatusChanged: (UpdateStatus) -> Unit) {
        installer.installDownloadedUpdate(onStatusChanged)
    }

    override fun cancelUpdate(status: UpdateStatus.Available) {
        GitHubUpdateDownloadService.cancel(
            appContext,
            status.downloadSha256 ?: status.versionName
        )
    }

    override fun restoreActionListener(onStatusChanged: (UpdateStatus) -> Unit) {
        GitHubApkInstaller.registerStatusChanged(appContext, onStatusChanged)
    }

    override fun hasInstallPermission(): Boolean = appContext.packageManager.canRequestPackageInstalls()

    override fun resumeUpdate(): Boolean = installer.resumePendingInstallation()

    private fun hasLocalNetworkPermission(): Boolean {
        return Build.VERSION.SDK_INT < 37 ||
            ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.ACCESS_LOCAL_NETWORK) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun fetchLatestRelease(url: URL): JSONObject {
        requireUpdateUrlAllowed(url, allowCleartext)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "HydroTracker")
        }
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                throw UpdateException(UpdateError.CHECK_FAILED, "GitHub API returned ${conn.responseCode}")
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(body)
        } finally {
            conn.disconnect()
        }
    }
}

/** Returns true for loopback, emulator, link-local, private-LAN and mDNS endpoints. */
internal fun isLocalUpdateEndpoint(url: URL): Boolean {
    val host = url.host.lowercase().removePrefix("[").removeSuffix("]")
    if (host == "localhost" || host == "::1" || host == "10.0.2.2" || host.endsWith(".local")) {
        return true
    }
    if (host.startsWith("127.") || host.startsWith("192.168.") || host.startsWith("10.") ||
        host.startsWith("169.254.") || host.startsWith("fe80:") || host.startsWith("fc") || host.startsWith("fd")
    ) {
        return true
    }
    if (host.startsWith("172.")) {
        val secondOctet = host.substringAfter("172.").substringBefore(".").toIntOrNull()
        return secondOctet in 16..31
    }
    return false
}

/** Production accepts HTTPS only; the debug-only local harness may explicitly allow HTTP. */
internal fun requireUpdateUrlAllowed(url: URL, allowCleartext: Boolean) {
    val isHttps = url.protocol.equals("https", ignoreCase = true)
    val isAllowedHttp = allowCleartext && url.protocol.equals("http", ignoreCase = true)
    if (!isHttps && !isAllowedHttp) {
        throw UpdateException(UpdateError.INSECURE_URL, "Update URLs must use HTTPS")
    }
}

/** Selects only the public GitHub artefact, never the separate F-Droid verification APK. */
internal fun findGitHubApkAsset(release: JSONObject, tag: String): JSONObject? {
    val versionName = tag.removePrefix("v").removePrefix("V")
    val expectedName = "HydroTracker-$versionName.apk"
    val assets = release.optJSONArray("assets") ?: return null
    for (index in 0 until assets.length()) {
        val asset = assets.optJSONObject(index) ?: continue
        if (asset.optString("name") == expectedName) return asset
    }
    return null
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
    for (index in 0 until size) {
        val remotePart = remote.getOrElse(index) { 0 }
        val localPart = local.getOrElse(index) { 0 }
        if (remotePart != localPart) return remotePart > localPart
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

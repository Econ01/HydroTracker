package com.cemcakmak.hydrotracker.data.update

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.edit
import androidx.core.net.toUri
import com.cemcakmak.hydrotracker.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Downloads, verifies and stages a GitHub APK using Android's confirmed installation flow. */
internal class GitHubApkInstaller(
    private val context: Context,
    private val allowCleartext: Boolean = false
) {
    init {
        restoreReadyUpdate()
        restorePendingConfirmation()
    }

    fun hasPendingConfirmation(): Boolean = pendingConfirmation() != null

    fun readyUpdate(): UpdateStatus.Available? {
        if (!readyPreferences().getBoolean(KEY_READY, false)) return null
        restoreReadyUpdate()
        val available = activeAvailable ?: return null
        val apk = activeApk ?: return null
        return try {
            verifyApk(apk)
            available
        } catch (_: Exception) {
            clearActiveUpdate(context)
            null
        }
    }

    fun resumePendingInstallation() = resumePendingInstallation(context)

    suspend fun downloadAndVerify(
        status: UpdateStatus.Available,
        onStatusChanged: (UpdateStatus) -> Unit
    ) = withContext(Dispatchers.IO) {
        check(status.mechanism == UpdateMechanism.GITHUB_APK)

        cancelledDownloadId = null
        clearReadyUpdate()
        activeContext = context.applicationContext
        activeAvailable = status
        val updateDir = File(context.filesDir, "updates").apply { mkdirs() }
        val apk = File(updateDir, "HydroTracker-update.apk")
        activeApk = apk

        try {
            val downloaded = downloadApk(status, apk, onStatusChanged)
            ensureDownloadNotCancelled(status)
            onStatusChanged(UpdateStatus.Verifying)
            verifyApk(downloaded)
            ensureDownloadNotCancelled(status)
            saveReadyUpdate(status, downloaded)
            ensureDownloadNotCancelled(status)
            onStatusChanged(UpdateStatus.ReadyToInstall(status))
        } catch (e: Exception) {
            if (isDownloadCancelled(status)) {
                onStatusChanged(status)
                clearActiveUpdate(context)
                return@withContext
            }
            onStatusChanged(e.toUpdateFailure(UpdateError.DOWNLOAD_FAILED))
            clearActiveUpdate(context)
            throw e
        }
    }

    suspend fun installDownloadedUpdate(
        onStatusChanged: (UpdateStatus) -> Unit
    ) = withContext(Dispatchers.IO) {
        activeStatusChanged = onStatusChanged
        activeContext = context.applicationContext
        GitHubUpdateDownloadService.cancelReadyNotification(context)
        if (pendingConfirmationIntent != null) {
            onStatusChanged(UpdateStatus.Installing)
            resumePendingInstallation()
            return@withContext
        }
        val apk = activeApk ?: throw UpdateException(
            UpdateError.NO_DOWNLOADED_UPDATE,
            "The downloaded update is no longer available"
        )
        if (!apk.isFile) {
            throw UpdateException(
                UpdateError.NO_DOWNLOADED_UPDATE,
                "The downloaded update is no longer available"
            )
        }

        val available = activeAvailable ?: throw UpdateException(
            UpdateError.NO_DOWNLOADED_UPDATE,
            "The downloaded update is no longer available"
        )
        if (!ensureInstallPermission()) {
            onStatusChanged(UpdateStatus.PermissionRequired(available))
            return@withContext
        }

        try {
            onStatusChanged(UpdateStatus.Installing)
            stageApk(apk)
        } catch (e: Exception) {
            onStatusChanged(e.toUpdateFailure(UpdateError.INSTALL_FAILED))
            clearActiveUpdate(context)
            throw e
        }
    }

    private fun ensureInstallPermission(): Boolean {
        if (context.packageManager.canRequestPackageInstalls()) return true

        val settings = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            "package:${context.packageName}".toUri()
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(settings)
        return false
    }

    private fun downloadApk(
        status: UpdateStatus.Available,
        apk: File,
        onStatusChanged: (UpdateStatus) -> Unit
    ): File {
        val downloadUrl = status.downloadUrl ?: throw UpdateException(
            UpdateError.INVALID_UPDATE_METADATA,
            "The update has no download URL"
        )
        val expectedDigest = status.downloadSha256?.takeIf { it.matches(HEX_SHA256) }
            ?: throw UpdateException(
                UpdateError.INVALID_UPDATE_METADATA,
                "GitHub didn't provide a valid SHA-256 digest for this APK"
            )
        val partial = File(apk.parentFile, "${apk.name}.part")
        partial.delete()
        onStatusChanged(UpdateStatus.Downloading(0))

        val url = URL(downloadUrl)
        requireUpdateUrlAllowed(url, allowCleartext)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/vnd.android.package-archive")
            setRequestProperty("User-Agent", "HydroTracker")
        }

        activeConnection = connection
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw UpdateException(
                    UpdateError.DOWNLOAD_FAILED,
                    "GitHub returned HTTP ${connection.responseCode} while downloading"
                )
            }

            val digest = MessageDigest.getInstance("SHA-256")
            val expectedSize = status.downloadSize ?: connection.contentLengthLong.takeIf { it >= 0L }
            var downloadedSize = 0L
            var lastProgress = -1

            connection.inputStream.use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        ensureDownloadNotCancelled(status)
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        downloadedSize += count

                        val progress = expectedSize?.takeIf { it > 0L }
                            ?.let { ((downloadedSize * 100L) / it).toInt().coerceIn(0, 99) }
                        if (progress != null && progress != lastProgress) {
                            onStatusChanged(UpdateStatus.Downloading(progress))
                            lastProgress = progress
                        }
                    }
                }
            }

            if (expectedSize != null && downloadedSize != expectedSize) {
                throw UpdateException(UpdateError.DOWNLOAD_FAILED, "The downloaded APK is incomplete")
            }
            val actualDigest = digest.digest().toHexString()
            if (!actualDigest.equals(expectedDigest, ignoreCase = true)) {
                throw UpdateException(
                    UpdateError.VERIFICATION_FAILED,
                    "The downloaded APK failed its SHA-256 check"
                )
            }

            ensureDownloadNotCancelled(status)
            apk.delete()
            if (!partial.renameTo(apk)) {
                throw UpdateException(UpdateError.DOWNLOAD_FAILED, "Couldn't store the downloaded APK")
            }
            onStatusChanged(UpdateStatus.Downloading(100))
            return apk
        } catch (e: Exception) {
            partial.delete()
            throw e
        } finally {
            activeConnection = null
            connection.disconnect()
        }
    }

    private fun ensureDownloadNotCancelled(status: UpdateStatus.Available) {
        if (isDownloadCancelled(status)) {
            throw UpdateException(UpdateError.UNKNOWN, "Download cancelled")
        }
    }

    private fun verifyApk(apk: File) {
        val packageManager = context.packageManager
        val archive = packageManager.getArchiveInfo(apk.absolutePath)
            ?: throw UpdateException(UpdateError.VERIFICATION_FAILED, "The downloaded file isn't a valid APK")

        if (archive.packageName != context.packageName) {
            throw UpdateException(
                UpdateError.VERIFICATION_FAILED,
                "The downloaded APK has the wrong application ID"
            )
        }
        if (archive.versionCodeLong() <= BuildConfig.VERSION_CODE.toLong()) {
            throw UpdateException(
                UpdateError.VERIFICATION_FAILED,
                "The downloaded APK isn't newer than the installed version"
            )
        }

        val installed = packageManager.getInstalledInfo(context.packageName)
        if (installed.signerFingerprints() != archive.signerFingerprints()) {
            throw UpdateException(
                UpdateError.VERIFICATION_FAILED,
                "The downloaded APK has an unexpected signing certificate"
            )
        }
    }

    @SuppressLint("RequestInstallPackagesPolicy")
    private fun stageApk(apk: File) {
        val packageInstaller = context.packageManager.packageInstaller
        activeSessionId?.let { oldSessionId ->
            runCatching { activePackageInstaller?.abandonSession(oldSessionId) }
            activeSessionId = null
        }
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apk.length())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
            }
        }

        val sessionId = packageInstaller.createSession(params)
        var session: PackageInstaller.Session? = null
        try {
            session = packageInstaller.openSession(sessionId)
            session.openWrite("HydroTracker-update.apk", 0L, apk.length()).use { output ->
                apk.inputStream().use { input -> input.copyTo(output) }
                session.fsync(output)
            }

            val callback = Intent(context, GitHubUpdateInstallReceiver::class.java).apply {
                action = INSTALL_STATUS_ACTION
            }
            val mutableFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                callback,
                PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag
            )
            activePackageInstaller = packageInstaller
            activeSessionId = sessionId
            session.commit(pendingIntent.intentSender)
        } catch (e: Exception) {
            packageInstaller.abandonSession(sessionId)
            throw e
        } finally {
            session?.close()
        }
    }

    private fun saveReadyUpdate(status: UpdateStatus.Available, apk: File) {
        activeAvailable = status
        activeApk = apk
        readyPreferences().edit {
            putBoolean(KEY_READY, true)
            putString(KEY_VERSION, status.versionName)
            putString(KEY_URL, status.downloadUrl)
            putLong(KEY_SIZE, status.downloadSize ?: -1L)
            putString(KEY_DIGEST, status.downloadSha256)
            putString(KEY_PATH, apk.absolutePath)
        }
    }

    private fun restoreReadyUpdate() {
        if (activeAvailable != null && activeApk?.isFile == true) return
        val prefs = readyPreferences()
        val version = prefs.getString(KEY_VERSION, null) ?: return
        val path = prefs.getString(KEY_PATH, null) ?: return
        val file = File(path)
        if (!file.isFile) {
            prefs.edit { clear() }
            return
        }

        activeAvailable = UpdateStatus.Available(
            versionName = version,
            mechanism = UpdateMechanism.GITHUB_APK,
            downloadUrl = prefs.getString(KEY_URL, null),
            downloadSize = prefs.getLong(KEY_SIZE, -1L).takeIf { it > 0L },
            downloadSha256 = prefs.getString(KEY_DIGEST, null)
        )
        activeApk = file
        activeContext = context.applicationContext
    }

    private fun clearReadyUpdate() {
        activeAvailable = null
        readyPreferences().edit { clear() }
    }

    private fun restorePendingConfirmation() {
        val prefs = pendingPreferences(context)
        val uri = prefs.getString(KEY_CONFIRMATION_URI, null) ?: return
        pendingConfirmationIntent = runCatching {
            Intent.parseUri(uri, Intent.URI_INTENT_SCHEME)
        }.getOrNull() ?: return
        activeContext = context.applicationContext
        activePackageInstaller = context.packageManager.packageInstaller
        activeSessionId = prefs.getInt(KEY_SESSION_ID, -1).takeIf { it >= 0 }
    }

    private fun readyPreferences() = context.applicationContext.getSharedPreferences(
        READY_PREFERENCES,
        Context.MODE_PRIVATE
    )

    private fun PackageManager.getArchiveInfo(path: String): PackageInfo? {
        val flags = signingFlags
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getPackageArchiveInfo(path, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            getPackageArchiveInfo(path, flags)
        }
    }

    private fun PackageManager.getInstalledInfo(packageName: String): PackageInfo {
        val flags = signingFlags
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            getPackageInfo(packageName, flags)
        }
    }

    private val signingFlags: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

    private fun PackageInfo.versionCodeLong(): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            longVersionCode
        } else {
            @Suppress("DEPRECATION")
            versionCode.toLong()
        }
    }

    private fun PackageInfo.signerFingerprints(): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = signingInfo ?: throw UpdateException(
                UpdateError.VERIFICATION_FAILED,
                "The APK signing information is unavailable"
            )
            if (info.hasMultipleSigners()) info.apkContentsSigners else info.signingCertificateHistory
        } else {
            @Suppress("DEPRECATION")
            signatures
        }
        if (signatures.isNullOrEmpty()) {
            throw UpdateException(UpdateError.VERIFICATION_FAILED, "The APK signing information is unavailable")
        }
        return signatures.map { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).toHexString()
        }.toSet()
    }

    private fun ByteArray.toHexString(): String {
        val digits = "0123456789abcdef"
        return buildString(size * 2) {
            this@toHexString.forEach { byte ->
                val value = byte.toInt() and 0xff
                append(digits[value ushr 4])
                append(digits[value and 0x0f])
            }
        }
    }

    companion object {
        private const val INSTALL_STATUS_ACTION =
            "com.cemcakmak.hydrotracker.action.GITHUB_UPDATE_INSTALL_STATUS"
        private const val READY_PREFERENCES = "github_update_ready"
        private const val PENDING_PREFERENCES = "github_update_pending"
        private const val KEY_CONFIRMATION_URI = "confirmation_uri"
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_READY = "ready"
        private const val KEY_VERSION = "version"
        private const val KEY_URL = "url"
        private const val KEY_SIZE = "size"
        private const val KEY_DIGEST = "digest"
        private const val KEY_PATH = "path"
        private val HEX_SHA256 = Regex("^[0-9a-fA-F]{64}$")

        @Volatile
        private var activeApk: File? = null

        @Volatile
        private var activeAvailable: UpdateStatus.Available? = null

        @Volatile
        private var activeConnection: HttpURLConnection? = null

        @Volatile
        private var cancelledDownloadId: String? = null

        @Volatile
        private var activeStatusChanged: ((UpdateStatus) -> Unit)? = null

        @Volatile
        private var pendingConfirmationIntent: Intent? = null

        @Volatile
        private var activeContext: Context? = null

        @Volatile
        private var activePackageInstaller: PackageInstaller? = null

        @Volatile
        private var activeSessionId: Int? = null

        private fun UpdateStatus.Available.downloadId(): String = downloadSha256 ?: versionName

        private fun isDownloadCancelled(status: UpdateStatus.Available): Boolean {
            return cancelledDownloadId == status.downloadId()
        }

        /** Disconnects the active socket, removes partial files and restores the update for retry. */
        fun cancelActiveDownload(downloadId: String): UpdateStatus.Available? {
            val available = activeAvailable
            if (available != null && available.downloadId() != downloadId) return null

            cancelledDownloadId = downloadId
            activeConnection?.disconnect()
            activeConnection = null
            activeApk?.let { apk ->
                File(apk.parentFile, "${apk.name}.part").delete()
            }

            val callback = activeStatusChanged
            clearActiveUpdate(activeContext)
            available?.let { availableUpdate -> callback?.invoke(availableUpdate) }
            return available
        }

        fun registerStatusChanged(context: Context, listener: (UpdateStatus) -> Unit) {
            activeContext = context.applicationContext
            activeStatusChanged = listener
        }

        fun emitStatus(status: UpdateStatus) {
            activeStatusChanged?.invoke(status)
        }

        /** Abandons the active system session before reporting a simulated result. */
        fun cancelActiveInstallation(errorMessage: String?) {
            activeSessionId?.let { sessionId ->
                runCatching { activePackageInstaller?.abandonSession(sessionId) }
            }
            activeContext?.let(GitHubUpdateInstallReceiver::dismissConfirmationNotification)
            activeContext?.let { finishInstallation(it, errorMessage) }
        }

        private fun pendingPreferences(context: Context) = context.applicationContext.getSharedPreferences(
            PENDING_PREFERENCES,
            Context.MODE_PRIVATE
        )

        fun savePendingConfirmation(context: Context, intent: Intent) {
            pendingConfirmationIntent = intent
            pendingPreferences(context).edit {
                putString(KEY_CONFIRMATION_URI, intent.toUri(Intent.URI_INTENT_SCHEME))
                activeSessionId?.let { putInt(KEY_SESSION_ID, it) }
            }
        }

        fun pendingConfirmation(): Intent? = pendingConfirmationIntent

        /** Returns true only when the confirmation activity was launched successfully. */
        fun resumePendingInstallation(context: Context): Boolean {
            val confirmation = pendingConfirmationIntent ?: return false
            return try {
                confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(confirmation)
                true
            } catch (_: Exception) {
                false
            }
        }

        /** Called by the manifest receiver with null for success or diagnostic details for failure. */
        fun finishInstallation(
            context: Context,
            errorMessage: String?,
            error: UpdateError = UpdateError.INSTALL_FAILED
        ) {
            val callback = activeStatusChanged
            clearActiveUpdate(context)
            callback?.invoke(
                if (errorMessage == null) UpdateStatus.UpToDate
                else UpdateStatus.Failed(error, errorMessage)
            )
        }

        private fun clearActiveUpdate(context: Context?) {
            val readyContext = activeContext ?: context
            val preferences = readyContext?.getSharedPreferences(READY_PREFERENCES, Context.MODE_PRIVATE)
            val readyPath = preferences?.getString(KEY_PATH, null)
            activeApk?.delete() ?: readyPath?.let { File(it).delete() }
            preferences?.edit { clear() }
            readyContext?.getSharedPreferences(PENDING_PREFERENCES, Context.MODE_PRIVATE)
                ?.edit { clear() }
            activeApk = null
            activeAvailable = null
            activeStatusChanged = null
            pendingConfirmationIntent = null
            activeContext = null
            activePackageInstaller = null
            activeSessionId = null
        }
    }
}

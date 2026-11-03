package com.cemcakmak.hydrotracker.data.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cemcakmak.hydrotracker.MainActivity
import com.cemcakmak.hydrotracker.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** User-initiated foreground download with progress and ready-to-install notifications. */
class GitHubUpdateDownloadService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val available = intent?.toAvailableUpdate()
        if (available == null) {
            stopSelfResult(startId)
            return START_NOT_STICKY
        }

        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                progressNotification(UpdateStatus.Downloading(0)),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(
                NOTIFICATION_ID,
                progressNotification(UpdateStatus.Downloading(0))
            )
        }

        val allowCleartext = intent.getBooleanExtra(EXTRA_ALLOW_CLEARTEXT, false)
        serviceScope.launch {
            val installer = GitHubApkInstaller(this@GitHubUpdateDownloadService, allowCleartext)
            try {
                installer.downloadAndVerify(available) { status ->
                    when (status) {
                        is UpdateStatus.Downloading -> updateProgressNotification(status)
                        is UpdateStatus.ReadyToInstall -> showReadyNotificationIfNeeded()
                        else -> Unit
                    }
                    GitHubApkInstaller.emitStatus(status)

                    when (status) {
                        is UpdateStatus.ReadyToInstall,
                        is UpdateStatus.PermissionRequired,
                        is UpdateStatus.Failed -> stopForegroundAndSelf(startId)
                        else -> Unit
                    }
                }
            } catch (_: Exception) {
                // The installer has already reported the failure through the shared status flow.
                stopForegroundAndSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        running = false
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        stopSelf()
    }

    private fun stopForegroundAndSelf(startId: Int) {
        running = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelfResult(startId)
    }

    private fun updateProgressNotification(status: UpdateStatus.Downloading) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        runCatching {
            NotificationManagerCompat.from(this).notify(
                NOTIFICATION_ID,
                progressNotification(status)
            )
        }
    }

    private fun progressNotification(status: UpdateStatus.Downloading) = NotificationCompat
        .Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.cloud_download_filled)
        .setContentTitle(getString(R.string.updates_notification_downloading_title))
        .setContentText(
            status.progress?.let { getString(R.string.updates_status_downloading_progress, it) }
                ?: getString(R.string.updates_notification_downloading_text)
        )
        .setProgress(100, status.progress ?: 0, status.progress == null)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setContentIntent(updatePendingIntent(NOTIFICATION_ID))
        .build()

    private fun showReadyNotificationIfNeeded() {
        if (UpdateUiVisibility.isUpdateModalVisible && UpdateUiVisibility.isAppInForeground) return
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.update_filled)
            .setContentTitle(getString(R.string.updates_status_ready_title))
            .setContentText(getString(R.string.updates_notification_ready_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(updatePendingIntent(READY_NOTIFICATION_ID))
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(this).notify(READY_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // The in-app Install update action remains available without notification permission.
        }
    }

    private fun updatePendingIntent(requestCode: Int): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_SHOW_UPDATE
            putExtra(EXTRA_SHOW_UPDATE, true)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        }
        return PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        NotificationManagerCompat.from(this).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.screen_updates_title),
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    private fun Intent.toAvailableUpdate(): UpdateStatus.Available? {
        val version = getStringExtra(EXTRA_VERSION) ?: return null
        val url = getStringExtra(EXTRA_URL) ?: return null
        val digest = getStringExtra(EXTRA_DIGEST) ?: return null
        return UpdateStatus.Available(
            versionName = version,
            mechanism = UpdateMechanism.GITHUB_APK,
            downloadUrl = url,
            downloadSize = getLongExtra(EXTRA_SIZE, -1L).takeIf { it > 0L },
            downloadSha256 = digest
        )
    }

    companion object {
        fun cancelReadyNotification(context: Context) {
            NotificationManagerCompat.from(context).cancel(READY_NOTIFICATION_ID)
        }

        private const val CHANNEL_ID = "app_updates"
        private const val NOTIFICATION_ID = 7308
        private const val READY_NOTIFICATION_ID = 7311
        private const val EXTRA_VERSION = "version"
        private const val EXTRA_URL = "url"
        private const val EXTRA_SIZE = "size"
        private const val EXTRA_DIGEST = "digest"
        private const val EXTRA_ALLOW_CLEARTEXT = "allow_cleartext"

        @Volatile
        private var running = false

        @Synchronized
        fun start(context: Context, status: UpdateStatus.Available, allowCleartext: Boolean) {
            if (running) return
            val url = status.downloadUrl ?: throw UpdateException(
                UpdateError.INVALID_UPDATE_METADATA,
                "Missing APK URL"
            )
            val digest = status.downloadSha256 ?: throw UpdateException(
                UpdateError.INVALID_UPDATE_METADATA,
                "Missing APK digest"
            )
            running = true
            cancelReadyNotification(context)
            val intent = Intent(context, GitHubUpdateDownloadService::class.java).apply {
                putExtra(EXTRA_VERSION, status.versionName)
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_SIZE, status.downloadSize ?: -1L)
                putExtra(EXTRA_DIGEST, digest)
                putExtra(EXTRA_ALLOW_CLEARTEXT, allowCleartext)
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                running = false
                throw e
            }
        }
    }
}

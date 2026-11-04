package com.cemcakmak.hydrotracker.data.update

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cemcakmak.hydrotracker.R

/** Receives user-action requests and final results from Android's package installer. */
class GitHubUpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }

                if (confirmation == null) {
                    GitHubApkInstaller.finishInstallation(
                        context,
                        "Android didn't provide an installation confirmation",
                        UpdateError.INSTALL_CONFIRMATION_UNAVAILABLE
                    )
                } else {
                    GitHubApkInstaller.savePendingConfirmation(context, confirmation)
                    val resumed = isForeground(context) &&
                        GitHubApkInstaller.resumePendingInstallation(context)
                    if (!resumed) {
                        // If notifications are unavailable, the pending confirmation remains
                        // available through the Updates screen's Continue installation action.
                        showConfirmationNotification(context)
                    }
                }
            }

            PackageInstaller.STATUS_SUCCESS -> {
                GitHubUpdateDownloadService.cancelReadyNotification(context)
                dismissConfirmationNotification(context)
                GitHubApkInstaller.finishInstallation(context, null)
            }

            else -> {
                GitHubUpdateDownloadService.cancelReadyNotification(context)
                dismissConfirmationNotification(context)
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    ?: "Android couldn't install the update"
                GitHubApkInstaller.finishInstallation(context, message)
            }
        }
    }

    private fun isForeground(context: Context): Boolean {
        val state = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(state)
        return state.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }

    private fun showConfirmationNotification(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        val confirmation = GitHubApkInstaller.pendingConfirmation() ?: return false
        val contentIntent = PendingIntent.getActivity(
            context,
            CONFIRMATION_REQUEST_CODE,
            confirmation,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.screen_updates_title),
                NotificationManager.IMPORTANCE_HIGH
            )
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.update_filled)
            .setContentTitle(context.getString(R.string.updates_status_installing))
            .setContentText(context.getString(R.string.updates_status_installing_desc))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()

        return try {
            manager.notify(NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    companion object {
        private const val CHANNEL_ID = "app_updates"
        private const val NOTIFICATION_ID = 7309
        private const val CONFIRMATION_REQUEST_CODE = 7310

        fun dismissConfirmationNotification(context: Context) {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        }
    }
}

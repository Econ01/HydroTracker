// HydroNotificationService.kt
// Location: app/src/main/java/com/cemcakmak/hydrotracker/notifications/HydroNotificationService.kt

package com.dev.hydrotracker.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dev.hydrotracker.MainActivity
import com.dev.hydrotracker.R
import com.dev.hydrotracker.data.models.UserProfile
import com.dev.hydrotracker.data.database.repository.WaterProgress

/**
 * Service for creating and managing hydration reminder notifications
 * Integrates with HydroTracker's Material 3 Expressive design system
 */
class HydroNotificationService(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    companion object {
        const val CHANNEL_ID = "hydro_reminders"
        const val CHANNEL_NAME = "Hydration Reminders"
        const val CHANNEL_DESCRIPTION = "Notifications to remind you to stay hydrated"
        const val NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannel()
    }

    /**
     * Create notification channel for hydration reminders
     * Required for Android 8.0+ (API 26+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300) // Gentle vibration pattern
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Show hydration reminder notification
     */
    fun showHydrationReminder(
        userProfile: UserProfile,
        waterProgress: WaterProgress
    ) {
        if (!NotificationPermissionManager.hasNotificationPermission(context)) {
            return
        }

        val content = NotificationContentProvider.getNotificationContent(
            reminderStyle = userProfile.reminderStyle,
            userName = null, // Can be added to UserProfile if needed
            currentProgress = waterProgress.progress,
            dailyGoal = userProfile.dailyWaterGoal
        )

        val notification = buildNotification(content, waterProgress)

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Handle case where permission was revoked
            println("Notification permission denied: ${e.message}")
        }
    }

    /**
     * Show test notification for debug purposes
     * Now sends an actual hydration reminder to test the content generation
     */
    fun showTestNotification(userProfile: UserProfile, waterProgress: WaterProgress) {
        if (!NotificationPermissionManager.hasNotificationPermission(context)) {
            return
        }

        // Generate actual notification content using the real system
        val content = NotificationContentProvider.getNotificationContent(
            reminderStyle = userProfile.reminderStyle,
            userName = null,
            currentProgress = waterProgress.progress,
            dailyGoal = userProfile.dailyWaterGoal
        )

        val notification = buildNotification(content, waterProgress)

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            println("Test notification permission denied: ${e.message}")
        }
    }

    /**
     * Build notification with Material 3 styling
     */
    private fun buildNotification(
        content: NotificationContent,
        waterProgress: WaterProgress
    ): android.app.Notification {
        // Create intent to open app when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name) // Use your custom notification icon
            .setContentTitle(content.title)
            .setContentText(content.message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(content.message)
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setProgress(
                100,
                (content.progress * 100).toInt(),
                false
            )
            .setSubText("${waterProgress.getFormattedCurrent()} / ${waterProgress.getFormattedGoal()}")
            .setColorized(true)
            .setColor(0xFF0077BE.toInt()) // HydroTracker primary color
            .build()
    }

    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }

    /**
     * Cancel specific notification
     */
    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
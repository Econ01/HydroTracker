// HydroNotificationService.kt
// Location: app/src/main/java/com/cemcakmak/hydrotracker/notifications/HydroNotificationService.kt

package com.cemcakmak.hydrotracker.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.cemcakmak.hydrotracker.MainActivity
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.ContainerPreset
import com.cemcakmak.hydrotracker.data.models.UserProfile
import com.cemcakmak.hydrotracker.data.database.repository.WaterProgress
import com.cemcakmak.hydrotracker.utils.VolumeUnitConverter

/**
 * Service for creating and managing hydration reminder notifications
 * Integrates with HydroTracker's Material 3 Expressive design system
 */
class HydroNotificationService(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    companion object {
        const val REMINDER_CHANNEL_ID = "hydro_reminders"
        const val FUN_FACTS_CHANNEL_ID = "hydro_fun_facts"
        const val NOTIFICATION_ID = 1001
        const val FUN_FACT_NOTIFICATION_ID = 1002
    }

    init {
        createNotificationChannels()
    }

    /**
     * Create notification channels for hydration reminders and daily fun facts.
     * Required for Android 8.0+ (API 26+).
     */
    private fun createNotificationChannels() {
        val reminderChannel = NotificationChannel(
            REMINDER_CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            enableVibration(true)
            setShowBadge(true)
        }

        val funFactsChannel = NotificationChannel(
            FUN_FACTS_CHANNEL_ID,
            context.getString(R.string.notification_channel_fun_facts_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_fun_facts_description)
            enableVibration(true)
            setShowBadge(true)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannels(listOf(reminderChannel, funFactsChannel))
    }

    /**
     * Show hydration reminder notification with optional quick-add actions.
     */
    fun showHydrationReminder(
        userProfile: UserProfile,
        waterProgress: WaterProgress,
        quickAddPresets: List<ContainerPreset> = emptyList()
    ) {
        if (!NotificationPermissionManager.hasNotificationPermission(context)) {
            return
        }

        val content = NotificationContentProvider.getNotificationContent(
            context = context,
            reminderStyle = userProfile.reminderStyle,
            currentProgress = waterProgress.progress,
            dailyGoal = userProfile.dailyWaterGoal,
            volumeUnit = userProfile.volumeUnit
        )

        val notification = buildNotification(content, waterProgress, userProfile, quickAddPresets)

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            println("Notification permission denied: ${e.message}")
        }
    }

    /**
     * Show test notification for debug purposes.
     */
    fun showTestNotification(
        userProfile: UserProfile,
        waterProgress: WaterProgress,
        quickAddPresets: List<ContainerPreset> = emptyList()
    ) {
        if (!NotificationPermissionManager.hasNotificationPermission(context)) {
            return
        }

        val content = NotificationContentProvider.getNotificationContent(
            context = context,
            reminderStyle = userProfile.reminderStyle,
            currentProgress = waterProgress.progress,
            dailyGoal = userProfile.dailyWaterGoal,
            volumeUnit = userProfile.volumeUnit
        )

        val notification = buildNotification(content, waterProgress, userProfile, quickAddPresets)

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            println("Test notification permission denied: ${e.message}")
        }
    }

    /**
     * Show a brief post-quick-add feedback notification.
     *
     * The quick-add buttons are removed, the title shows the amount that was added,
     * and the message shows the updated intake versus the daily goal. The notification
     * auto-dismisses after [timeoutAfterMs].
     */
    fun showQuickAddFeedbackNotification(
        userProfile: UserProfile,
        waterProgress: WaterProgress,
        addedAmount: Double,
        timeoutAfterMs: Long = 3000L
    ) {
        if (!NotificationPermissionManager.hasNotificationPermission(context)) {
            return
        }

        val notification = buildQuickAddFeedbackNotification(
            userProfile,
            waterProgress,
            addedAmount,
            timeoutAfterMs
        )

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            println("Quick-add feedback notification permission denied: ${e.message}")
        }
    }

    /**
     * Show a daily fun-fact notification.
     */
    fun showFunFact() {
        if (!NotificationPermissionManager.hasNotificationPermission(context)) {
            return
        }

        val content = NotificationContentProvider.getFunFactContent(context)
        val notification = buildFunFactNotification(content)

        try {
            notificationManager.notify(FUN_FACT_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            println("Fun fact notification permission denied: ${e.message}")
        }
    }

    /**
     * Build a progress-centric reminder notification using [NotificationCompat.ProgressStyle].
     */
    private fun buildNotification(
        content: NotificationContent,
        waterProgress: WaterProgress,
        userProfile: UserProfile,
        quickAddPresets: List<ContainerPreset> = emptyList()
    ): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val progress = (content.progress * 100).toInt().coerceIn(0, 100)
        val progressColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.getColor(context, android.R.color.system_accent1_500)
        } else {
            0xFF00B4D8.toInt()
        }
        val progressStyle = NotificationCompat.ProgressStyle()
            .setStyledByProgress(true)
            .setProgress(progress)
            .setProgressEndIcon(IconCompat.createWithResource(context, R.drawable.award_star).setTint(progressColor))
            .setProgressSegments(
                listOf(
                    NotificationCompat.ProgressStyle.Segment(25),
                    NotificationCompat.ProgressStyle.Segment(25),
                    NotificationCompat.ProgressStyle.Segment(25),
                    NotificationCompat.ProgressStyle.Segment(25)
                )
            )

        val builder = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(content.title)
            .setContentText(content.message)
            .setStyle(progressStyle)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSubText(
                "${VolumeUnitConverter.format(context, waterProgress.currentIntake, userProfile.volumeUnit)} / " +
                VolumeUnitConverter.format(context, waterProgress.dailyGoal, userProfile.volumeUnit)
            )
            .setColorized(true)
            .setColor(progressColor)

        quickAddPresets.take(2).forEachIndexed { index, preset ->
            val actionLabel = context.getString(
                R.string.notification_action_add,
                VolumeUnitConverter.format(context, preset.volume, userProfile.volumeUnit)
            )
            val actionIntent = Intent(context, QuickAddWaterReceiver::class.java).apply {
                action = QuickAddWaterReceiver.ACTION_QUICK_ADD_WATER
                putExtra(QuickAddWaterReceiver.EXTRA_CONTAINER_VOLUME, preset.volume)
                putExtra(QuickAddWaterReceiver.EXTRA_CONTAINER_NAME, preset.name)
            }
            val actionPendingIntent = PendingIntent.getBroadcast(
                context,
                index,
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, actionLabel, actionPendingIntent)
        }

        return builder.build()
    }

    /**
     * Build a post-quick-add feedback notification.
     *
     * The title shows the amount that was added, the message shows the updated
     * intake versus the daily goal, and the progress bar reflects the new progress.
     * No quick-add actions are included, and the subtext is omitted.
     */
    private fun buildQuickAddFeedbackNotification(
        userProfile: UserProfile,
        waterProgress: WaterProgress,
        addedAmount: Double,
        timeoutAfterMs: Long
    ): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val progress = (waterProgress.progress * 100).toInt().coerceIn(0, 100)
        val progressColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.getColor(context, android.R.color.system_accent1_500)
        } else {
            0xFF00B4D8.toInt()
        }
        val progressStyle = NotificationCompat.ProgressStyle()
            .setStyledByProgress(true)
            .setProgress(progress)
            .setProgressEndIcon(
                IconCompat.createWithResource(context, R.drawable.award_star).setTint(progressColor)
            )
            .setProgressSegments(
                listOf(
                    NotificationCompat.ProgressStyle.Segment(25),
                    NotificationCompat.ProgressStyle.Segment(25),
                    NotificationCompat.ProgressStyle.Segment(25),
                    NotificationCompat.ProgressStyle.Segment(25)
                )
            )

        val amountText = VolumeUnitConverter.format(context, addedAmount, userProfile.volumeUnit)
        val title = context.getString(R.string.notification_quick_add_feedback_title, amountText)
        val currentText = VolumeUnitConverter.format(
            context,
            waterProgress.currentIntake,
            userProfile.volumeUnit
        )
        val goalText = VolumeUnitConverter.format(context, waterProgress.dailyGoal, userProfile.volumeUnit)
        val message = context.getString(
            R.string.notification_quick_add_feedback_message,
            currentText,
            goalText
        )

        return NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(progressStyle)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColorized(true)
            .setColor(progressColor)
            .setTimeoutAfter(timeoutAfterMs)
            .build()
    }

    /**
     * Build the daily fun-fact notification.
     */
    private fun buildFunFactNotification(content: NotificationContent): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, FUN_FACTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.water)
            .setContentTitle(content.title)
            .setContentText(content.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColorized(true)
            .setColor(0xFF0077BE.toInt())
            .build()
    }

    /**
     * Cancel the hydration reminder notification.
     */
    fun cancelReminderNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    /**
     * Cancel the daily fun-fact notification.
     */
    fun cancelFunFactNotification() {
        notificationManager.cancel(FUN_FACT_NOTIFICATION_ID)
    }
}
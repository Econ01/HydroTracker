// NotificationContentProvider.kt
// Location: app/src/main/java/com/cemcakmak/hydrotracker/notifications/NotificationContentProvider.kt

package com.cemcakmak.hydrotracker.notifications

import android.content.Context
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.ReminderStyle
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.utils.VolumeUnitConverter

/**
 * Provides creative and personalized notification content
 * Including water puns, facts, and motivational messages.
 *
 * All user-facing text is loaded from resources so it can be translated.
 */
object NotificationContentProvider {

    /**
     * Get notification content based on user's reminder style and progress.
     */
    fun getNotificationContent(
        context: Context,
        reminderStyle: ReminderStyle,
        currentProgress: Float = 0f,
        dailyGoal: Double = 2700.0,
        volumeUnit: VolumeUnit = VolumeUnit.MILLILITRES
    ): NotificationContent {
        return when (reminderStyle) {
            ReminderStyle.GENTLE -> getGentleContent(context, currentProgress)
            ReminderStyle.MOTIVATING -> getMotivatingContent(context, currentProgress)
            ReminderStyle.MINIMAL -> getMinimalContent(context, currentProgress, dailyGoal, volumeUnit)
        }
    }

    /**
     * Get a preview of notification content for the settings screen.
     * Returns decomposed title, message, and extra content (fact/pun) without
     * combining them into a single body string.
     */
    fun getNotificationPreview(context: Context, reminderStyle: ReminderStyle): NotificationPreview {
        return when (reminderStyle) {
            ReminderStyle.GENTLE -> NotificationPreview(
                title = context.resources.getStringArray(R.array.notification_gentle_titles).random(),
                message = context.resources.getStringArray(getGentleMessageArray(0.3f)).random(),
                extraContent = ""
            )
            ReminderStyle.MOTIVATING -> NotificationPreview(
                title = context.resources.getStringArray(R.array.notification_motivating_titles).random(),
                message = context.resources.getStringArray(getMotivatingMessageArray(0.3f)).random(),
                extraContent = ""
            )
            ReminderStyle.MINIMAL -> NotificationPreview(
                title = context.resources.getStringArray(R.array.notification_minimal_titles).random(),
                message = context.getString(R.string.notification_minimal_message_mid),
                extraContent = ""
            )
        }
    }

    private fun getGentleContent(
        context: Context,
        progress: Float
    ): NotificationContent {
        val title = context.resources.getStringArray(R.array.notification_gentle_titles).random()
        val message = context.resources.getStringArray(getGentleMessageArray(progress)).random()

        return NotificationContent(
            title = title,
            message = message,
            progress = progress
        )
    }

    private fun getGentleMessageArray(progress: Float): Int = when {
        progress < 0.25f -> R.array.notification_gentle_messages_low
        progress < 0.5f -> R.array.notification_gentle_messages_mid
        progress < 0.75f -> R.array.notification_gentle_messages_high
        else -> R.array.notification_gentle_messages_final
    }

    private fun getMotivatingContent(
        context: Context,
        progress: Float
    ): NotificationContent {
        val title = context.resources.getStringArray(R.array.notification_motivating_titles).random()
        val message = context.resources.getStringArray(getMotivatingMessageArray(progress)).random()

        return NotificationContent(
            title = title,
            message = message,
            progress = progress
        )
    }

    private fun getMotivatingMessageArray(progress: Float): Int = when {
        progress < 0.25f -> R.array.notification_motivating_messages_low
        progress < 0.5f -> R.array.notification_motivating_messages_mid
        progress < 0.75f -> R.array.notification_motivating_messages_high
        else -> R.array.notification_motivating_messages_final
    }

    private fun getMinimalContent(
        context: Context,
        progress: Float,
        goal: Double,
        volumeUnit: VolumeUnit
    ): NotificationContent {
        val message = when {
            progress < 0.5f -> context.getString(R.string.notification_minimal_message_low)
            progress < 0.8f -> context.getString(R.string.notification_minimal_message_mid)
            else -> {
                val remaining = ((1 - progress) * goal)
                val remainingText = VolumeUnitConverter.format(context, remaining, volumeUnit)
                context.getString(R.string.notification_minimal_remaining, remainingText)
            }
        }

        return NotificationContent(
            title = context.resources.getStringArray(R.array.notification_minimal_titles).random(),
            message = message,
            progress = progress
        )
    }

    /**
     * Get a daily fun fact for the fun-facts notification channel.
     */
    fun getFunFactContent(context: Context): NotificationContent {
        val fact = context.resources.getStringArray(R.array.notification_facts).random()
        return NotificationContent(
            title = context.getString(R.string.notification_fun_fact_title),
            message = fact,
            progress = -1f
        )
    }

}

/**
 * Data class for notification content
 */
data class NotificationContent(
    val title: String,
    val message: String,
    val progress: Float
)

/**
 * Data class for notification preview content shown in settings.
 * Decomposed so title, message, and extra content (fact/pun) can be displayed separately.
 */
data class NotificationPreview(
    val title: String,
    val message: String,
    val extraContent: String
)

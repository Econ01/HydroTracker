// NotificationContentProvider.kt
// Location: app/src/main/java/com/cemcakmak/hydrotracker/notifications/NotificationContentProvider.kt

package com.cemcakmak.hydrotracker.notifications

import android.content.Context
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.ReminderStyle
import java.util.Locale

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
        userName: String? = null,
        currentProgress: Float = 0f,
        dailyGoal: Double = 2700.0
    ): NotificationContent {
        return when (reminderStyle) {
            ReminderStyle.GENTLE -> getGentleContent(context, userName, currentProgress, dailyGoal)
            ReminderStyle.MOTIVATING -> getMotivatingContent(context, userName, currentProgress, dailyGoal)
            ReminderStyle.MINIMAL -> getMinimalContent(context, currentProgress, dailyGoal)
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
                extraContent = context.resources.getStringArray(R.array.notification_facts).random()
            )
            ReminderStyle.MOTIVATING -> NotificationPreview(
                title = context.resources.getStringArray(R.array.notification_motivating_titles).random(),
                message = context.resources.getStringArray(getMotivatingMessageArray(0.3f)).random(),
                extraContent = if (isEnglishLocale(context)) {
                    context.resources.getStringArray(R.array.notification_puns).random()
                } else {
                    ""
                }
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
        userName: String?,
        progress: Float,
        goal: Double
    ): NotificationContent {
        val title = context.resources.getStringArray(R.array.notification_gentle_titles).random()
        val message = context.resources.getStringArray(getGentleMessageArray(progress)).random()
        val funFact = context.resources.getStringArray(R.array.notification_facts).random()

        return NotificationContent(
            title = title,
            message = "$message\n\n${context.getString(R.string.notification_gentle_fact_prefix, funFact)}",
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
        userName: String?,
        progress: Float,
        goal: Double
    ): NotificationContent {
        val title = context.resources.getStringArray(R.array.notification_motivating_titles).random()
        val message = context.resources.getStringArray(getMotivatingMessageArray(progress)).random()

        return NotificationContent(
            title = title,
            message = if (isEnglishLocale(context)) {
                val pun = context.resources.getStringArray(R.array.notification_puns).random()
                "$message\n\n$pun"
            } else {
                message
            },
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
        goal: Double
    ): NotificationContent {
        val message = when {
            progress < 0.5f -> context.getString(R.string.notification_minimal_message_low)
            progress < 0.8f -> context.getString(R.string.notification_minimal_message_mid)
            else -> {
                val remaining = ((1 - progress) * goal).toInt()
                context.getString(R.string.notification_minimal_remaining, remaining)
            }
        }

        return NotificationContent(
            title = context.resources.getStringArray(R.array.notification_minimal_titles).random(),
            message = message,
            progress = progress
        )
    }

    /**
     * Puns are language-dependent wordplay. Only show them for English locales;
     * fall back to the plain motivational message for other languages.
     */
    private fun isEnglishLocale(context: Context): Boolean {
        return context.resources.configuration.locales[0].language == Locale.ENGLISH.language
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

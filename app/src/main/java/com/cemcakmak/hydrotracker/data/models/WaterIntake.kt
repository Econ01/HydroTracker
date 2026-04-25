package com.cemcakmak.hydrotracker.data.models

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Represents a single water intake entry
 */
data class WaterIntake(
    val id: Long = 0,
    val amount: Double, // in milliliters
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val containerType: String = "custom",
    val note: String? = null
) {

    /**
     * Formats the timestamp for display
     */
    fun getFormattedTime(): String {
        return timestamp.format(
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                .withLocale(Locale.getDefault())
        )
    }

    /**
     * Formats the timestamp for display with date
     */
    fun getFormattedDateTime(): String {
        return timestamp.format(
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT)
                .withLocale(Locale.getDefault())
        )
    }

    /**
     * Gets amount in a readable format (e.g., "250 ml", "1.5 L")
     */
    fun getFormattedAmount(): String {
        return when {
            amount >= 1000 -> "${(amount / 1000).format(1)} L"
            else -> "${amount.toInt()} ml"
        }
    }

    /**
     * Checks if this intake was recorded today
     */
    fun isToday(): Boolean {
        return timestamp.toLocalDate() == LocalDateTime.now().toLocalDate()
    }
}

// Extension function to format Double with specified decimal places
private fun Double.format(decimals: Int): String {
    return "%.${decimals}f".format(this)
}
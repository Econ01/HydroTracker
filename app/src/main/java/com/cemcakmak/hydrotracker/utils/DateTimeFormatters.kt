package com.cemcakmak.hydrotracker.utils

import android.content.Context
import android.text.format.DateFormat
import com.cemcakmak.hydrotracker.data.models.DateFormatPattern
import com.cemcakmak.hydrotracker.data.models.TimeFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * User-preference-aware date and time formatters.
 *
 * Internal storage continues to use 24-hour `HH:mm` and ISO `yyyy-MM-dd`. These helpers convert
 * those values to the user's chosen display format without changing the persisted data.
 */
object DateTimeFormatters {

    /** Returns a time pattern based on the [TimeFormat] preference. */
    fun timePattern(context: Context, timeFormat: TimeFormat): String {
        return when (timeFormat) {
            TimeFormat.HOUR_12 -> "h:mm a"
            TimeFormat.HOUR_24 -> "HH:mm"
            TimeFormat.SYSTEM -> if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
        }
    }

    /** Returns a date pattern based on the [DateFormatPattern] preference. */
    fun datePattern(dateFormat: DateFormatPattern): String {
        return when (dateFormat) {
            DateFormatPattern.DAY_MONTH_YEAR -> "dd MMM yyyy"
            DateFormatPattern.MONTH_DAY_YEAR -> "MMM dd, yyyy"
            DateFormatPattern.YEAR_MONTH_DAY -> "yyyy-MM-dd"
            DateFormatPattern.SYSTEM -> "" // Resolved via LocalizedDateFormatter
        }
    }

    /** Formats a [LocalTime] for display. */
    fun formatTime(context: Context, time: LocalTime, timeFormat: TimeFormat): String {
        val pattern = timePattern(context, timeFormat)
        return time.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
    }

    /** Formats a [LocalDate] for display. */
    fun formatDate(date: LocalDate, dateFormat: DateFormatPattern): String {
        return if (dateFormat == DateFormatPattern.SYSTEM) {
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()).format(date)
        } else {
            date.format(DateTimeFormatter.ofPattern(datePattern(dateFormat), Locale.getDefault()))
        }
    }

    /** Formats a [LocalDateTime] for display. */
    fun formatDateTime(
        context: Context,
        dateTime: LocalDateTime,
        timeFormat: TimeFormat,
        dateFormat: DateFormatPattern
    ): String {
        val dateString = formatDate(dateTime.toLocalDate(), dateFormat)
        val timeString = formatTime(context, dateTime.toLocalTime(), timeFormat)
        return "$dateString · $timeString"
    }

    /**
     * Formats a 24-hour `HH:mm` string (the internal storage format) for display.
     * Returns the original string if parsing fails.
     */
    fun formatTimeString(context: Context, timeString: String, timeFormat: TimeFormat): String {
        return try {
            val time = LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm"))
            formatTime(context, time, timeFormat)
        } catch (_: Exception) {
            timeString
        }
    }
}

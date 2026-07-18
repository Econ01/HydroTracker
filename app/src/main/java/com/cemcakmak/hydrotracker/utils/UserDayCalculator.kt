package com.cemcakmak.hydrotracker.utils

import com.cemcakmak.hydrotracker.data.models.DayEndMode
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Utility class to calculate user-defined days based on a configurable day boundary
 * (sleep time or strict midnight), instead of calendar days (midnight to midnight).
 */
object UserDayCalculator {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Get the current user day date string based on day-end mode.
     * If current time is before the day boundary, it's still the previous calendar day's "user day".
     *
     * @param dayEndTime The boundary time in HH:mm. For [DayEndMode.SLEEP_TIME] this should be the
     *                   user's sleep time; for [DayEndMode.MIDNIGHT] it is ignored.
     */
    fun getCurrentUserDayString(dayEndTime: String, dayEndMode: DayEndMode = DayEndMode.SLEEP_TIME): String {
        return getUserDayStringForTimestamp(System.currentTimeMillis(), dayEndTime, dayEndMode)
    }

    /**
     * Get the user day string for a specific timestamp based on day-end mode.
     *
     * @param dayEndTime The boundary time in HH:mm. For [DayEndMode.SLEEP_TIME] this should be the
     *                   user's sleep time; for [DayEndMode.MIDNIGHT] it is ignored.
     */
    fun getUserDayStringForTimestamp(timestamp: Long, dayEndTime: String, dayEndMode: DayEndMode = DayEndMode.SLEEP_TIME): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val time = LocalTime.of(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
        val boundary = getDayBoundary(dayEndTime, dayEndMode)

        return if (time.isBefore(boundary)) {
            // Before day boundary, so this entry belongs to the previous day's "user day"
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            dateFormat.format(calendar.time)
        } else {
            // After day boundary, so this entry belongs to today's "user day"
            dateFormat.format(calendar.time)
        }
    }

    /**
     * Check if a new user day has started since the last check.
     * Used to trigger data reset or cleanup.
     *
     * @param dayEndTime The boundary time in HH:mm. For [DayEndMode.SLEEP_TIME] this should be the
     *                   user's sleep time; for [DayEndMode.MIDNIGHT] it is ignored.
     */
    fun hasNewUserDayStarted(lastCheckTime: Long, dayEndTime: String, dayEndMode: DayEndMode = DayEndMode.SLEEP_TIME): Boolean {
        val lastUserDay = getUserDayStringForTimestamp(lastCheckTime, dayEndTime, dayEndMode)
        val currentUserDay = getCurrentUserDayString(dayEndTime, dayEndMode)
        return lastUserDay != currentUserDay
    }

    /**
     * Get the day boundary time based on day-end mode.
     *
     * @param dayEndTime The boundary time in HH:mm. For [DayEndMode.SLEEP_TIME] this should be the
     *                   user's sleep time; for [DayEndMode.MIDNIGHT] it is ignored.
     */
    private fun getDayBoundary(dayEndTime: String, dayEndMode: DayEndMode): LocalTime {
        return when (dayEndMode) {
            DayEndMode.SLEEP_TIME -> parseTime(dayEndTime) ?: LocalTime.of(23, 0)
            DayEndMode.MIDNIGHT -> LocalTime.MIDNIGHT
        }
    }

    private fun parseTime(timeString: String): LocalTime? {
        return try {
            LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm"))
        } catch (_: Exception) {
            null
        }
    }
}
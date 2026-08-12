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
     * The mapping between the current time and the user day label follows the same
     * regime-aware rules as [getUserDayStringForTimestamp].
     *
     * @param dayEndTime The boundary time in HH:mm. For [DayEndMode.SLEEP_TIME] this should be the
     *                   user's sleep time; for [DayEndMode.MIDNIGHT] it is ignored.
     */
    fun getCurrentUserDayString(dayEndTime: String, dayEndMode: DayEndMode = DayEndMode.MIDNIGHT): String {
        return getUserDayStringForTimestamp(System.currentTimeMillis(), dayEndTime, dayEndMode)
    }

    /**
     * Get the user day string for a specific timestamp based on day-end mode.
     *
     * [DayEndMode.MIDNIGHT] always yields the plain calendar date. For [DayEndMode.SLEEP_TIME]
     * two regimes are supported:
     *  - Evening boundary (sleep time at/after noon): the user day *ends* at the boundary, so
     *    timestamps before the boundary keep the current calendar date and timestamps at/after
     *    it belong to the next calendar date.
     *  - Morning boundary (sleep time before noon, e.g. 01:00): timestamps before the boundary
     *    belong to the previous calendar date, timestamps at/after it keep the current date.
     *
     * @param dayEndTime The boundary time in HH:mm. For [DayEndMode.SLEEP_TIME] this should be the
     *                   user's sleep time; for [DayEndMode.MIDNIGHT] it is ignored.
     */
    fun getUserDayStringForTimestamp(timestamp: Long, dayEndTime: String, dayEndMode: DayEndMode = DayEndMode.MIDNIGHT): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val time = LocalTime.of(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
        val boundary = getDayBoundary(dayEndTime, dayEndMode)

        return if (dayEndMode == DayEndMode.SLEEP_TIME && !boundary.isBefore(LocalTime.NOON)) {
            // Evening boundary: the day ends at sleep time, so entries at/after the boundary
            // already count towards the next calendar date
            if (!time.isBefore(boundary)) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            dateFormat.format(calendar.time)
        } else if (time.isBefore(boundary)) {
            // Morning boundary: before the boundary the entry still belongs to the previous
            // calendar date (midnight mode can never enter this branch)
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            dateFormat.format(calendar.time)
        } else {
            dateFormat.format(calendar.time)
        }
    }

    /**
     * Check if a new user day has started since the last check.
     * Used to trigger data reset or clean-up.
     *
     * @param dayEndTime The boundary time in HH:mm. For [DayEndMode.SLEEP_TIME] this should be the
     *                   user's sleep time; for [DayEndMode.MIDNIGHT] it is ignored.
     */
    fun hasNewUserDayStarted(lastCheckTime: Long, dayEndTime: String, dayEndMode: DayEndMode = DayEndMode.MIDNIGHT): Boolean {
        val lastUserDay = getUserDayStringForTimestamp(lastCheckTime, dayEndTime, dayEndMode)
        val currentUserDay = getCurrentUserDayString(dayEndTime, dayEndMode)
        return lastUserDay != currentUserDay
    }

    /**
     * Return the epoch millis of the start of the next user day.
     *
     * For [DayEndMode.MIDNIGHT] this is the next calendar midnight. For
     * [DayEndMode.SLEEP_TIME] it is the next occurrence of the user's sleep time.
     *
     * @param dayEndTime The boundary time in HH:mm. For [DayEndMode.SLEEP_TIME] this should be the
     *                   user's sleep time; for [DayEndMode.MIDNIGHT] it is ignored.
     */
    fun getNextUserDayStartMillis(dayEndTime: String, dayEndMode: DayEndMode = DayEndMode.MIDNIGHT): Long {
        val now = Calendar.getInstance()
        val boundary = getDayBoundary(dayEndTime, dayEndMode)
        val nextBoundary = now.clone() as Calendar
        nextBoundary.set(Calendar.HOUR_OF_DAY, boundary.hour)
        nextBoundary.set(Calendar.MINUTE, boundary.minute)
        nextBoundary.set(Calendar.SECOND, 0)
        nextBoundary.set(Calendar.MILLISECOND, 0)
        if (nextBoundary.timeInMillis <= now.timeInMillis) {
            nextBoundary.add(Calendar.DAY_OF_YEAR, 1)
        }
        return nextBoundary.timeInMillis
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
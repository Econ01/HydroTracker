package com.cemcakmak.hydrotracker.utils

import com.cemcakmak.hydrotracker.data.models.DayEndMode
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests for [UserDayCalculator].
 */
class UserDayCalculatorTest {

    private fun timestampFor(
        day: Int,
        hour: Int,
        minute: Int
    ): Long {
        return Calendar.getInstance().apply {
            set(2024, Calendar.JANUARY, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun `sleep time mode evening boundary - daytime entry keeps current calendar date`() {
        // 15 Jan 2024, 22:30 with boundary 23:00 -> 15 Jan 2024
        val timestamp = timestampFor(15, 22, 30)
        val result = UserDayCalculator.getUserDayStringForTimestamp(
            timestamp,
            "23:00",
            DayEndMode.SLEEP_TIME
        )
        assertEquals("2024-01-15", result)
    }

    @Test
    fun `sleep time mode evening boundary - morning entry keeps current calendar date`() {
        // 15 Jan 2024, 09:00 with boundary 23:00 -> 15 Jan 2024
        val timestamp = timestampFor(15, 9, 0)
        val result = UserDayCalculator.getUserDayStringForTimestamp(
            timestamp,
            "23:00",
            DayEndMode.SLEEP_TIME
        )
        assertEquals("2024-01-15", result)
    }

    @Test
    fun `sleep time mode evening boundary - entry at boundary counts as next user day`() {
        // 15 Jan 2024, 23:00 with boundary 23:00 -> 16 Jan 2024
        val timestamp = timestampFor(15, 23, 0)
        val result = UserDayCalculator.getUserDayStringForTimestamp(
            timestamp,
            "23:00",
            DayEndMode.SLEEP_TIME
        )
        assertEquals("2024-01-16", result)
    }

    @Test
    fun `sleep time mode evening boundary - entry after boundary counts as next user day`() {
        // 15 Jan 2024, 23:30 with boundary 23:00 -> 16 Jan 2024
        val timestamp = timestampFor(15, 23, 30)
        val result = UserDayCalculator.getUserDayStringForTimestamp(
            timestamp,
            "23:00",
            DayEndMode.SLEEP_TIME
        )
        assertEquals("2024-01-16", result)
    }

    @Test
    fun `sleep time mode morning boundary - entry before boundary falls to previous user day`() {
        // Sleep 01:00. 16 Jan 2024, 00:30 is before 01:00 -> 15 Jan 2024
        val timestamp = timestampFor(16, 0, 30)
        val result = UserDayCalculator.getUserDayStringForTimestamp(
            timestamp,
            "01:00",
            DayEndMode.SLEEP_TIME
        )
        assertEquals("2024-01-15", result)
    }

    @Test
    fun `sleep time mode morning boundary - entry after boundary keeps current calendar date`() {
        // 16 Jan 2024, 01:30 is after 01:00 boundary -> 16 Jan 2024
        val timestamp = timestampFor(16, 1, 30)
        val result = UserDayCalculator.getUserDayStringForTimestamp(
            timestamp,
            "01:00",
            DayEndMode.SLEEP_TIME
        )
        assertEquals("2024-01-16", result)
    }

    @Test
    fun `sleep time mode morning boundary - daytime entry keeps current calendar date`() {
        // 16 Jan 2024, 10:00 with boundary 01:00 -> 16 Jan 2024
        val timestamp = timestampFor(16, 10, 0)
        val result = UserDayCalculator.getUserDayStringForTimestamp(
            timestamp,
            "01:00",
            DayEndMode.SLEEP_TIME
        )
        assertEquals("2024-01-16", result)
    }

    @Test
    fun `midnight mode - always uses calendar day`() {
        val timestamp = timestampFor(15, 3, 0)
        val result = UserDayCalculator.getUserDayStringForTimestamp(
            timestamp,
            "00:00",
            DayEndMode.MIDNIGHT
        )
        assertEquals("2024-01-15", result)
    }

    @Test
    fun `invalid sleep time falls back to 2300 evening boundary`() {
        // 15 Jan 2024, 22:30 with invalid boundary -> falls back to 23:00 -> 15 Jan 2024
        val timestamp = timestampFor(15, 22, 30)
        val result = UserDayCalculator.getUserDayStringForTimestamp(
            timestamp,
            "not-a-time",
            DayEndMode.SLEEP_TIME
        )
        assertEquals("2024-01-15", result)
    }

    @Test
    fun `hasNewUserDayStarted returns true when user day changed`() {
        val dayEndTime = "23:00"
        val mode = DayEndMode.SLEEP_TIME

        // Last check at 22:00 on 14 Jan -> user day 14 Jan
        val lastCheck = timestampFor(14, 22, 0)

        // Current time mocked by making timestamp at 23:30 on 14 Jan -> user day 15 Jan
        val currentTime = timestampFor(14, 23, 30)

        val lastDay = UserDayCalculator.getUserDayStringForTimestamp(lastCheck, dayEndTime, mode)
        val currentDay = UserDayCalculator.getUserDayStringForTimestamp(currentTime, dayEndTime, mode)

        assertEquals("2024-01-14", lastDay)
        assertEquals("2024-01-15", currentDay)
    }
}

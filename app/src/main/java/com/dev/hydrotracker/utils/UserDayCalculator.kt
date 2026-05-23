// UserDayCalculator.kt
// Location: app/src/main/java/com/cemcakmak/hydrotracker/utils/UserDayCalculator.kt

package com.dev.hydrotracker.utils

import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Utility class to calculate user-defined days based on wake-up time
 * instead of calendar days (midnight to midnight)
 */
object UserDayCalculator {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    /**
     * Get the current user day date string based on wake-up time
     * If current time is before wake-up time, it's still the previous calendar day's "user day"
     */
    fun getCurrentUserDayString(wakeUpTime: String): String {
        val now = LocalDateTime.now()
        val currentTime = now.toLocalTime()
        val wakeUp = parseTime(wakeUpTime) ?: LocalTime.of(7, 0) // Default to 7:00 AM
        
        return if (currentTime.isBefore(wakeUp)) {
            // Before wake-up time, so this is still the previous day's "user day"
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            dateFormat.format(calendar.time)
        } else {
            // After wake-up time, so this is today's "user day"
            dateFormat.format(Date())
        }
    }
    
    /**
     * Get the user day string for a specific timestamp based on wake-up time
     */
    fun getUserDayStringForTimestamp(timestamp: Long, wakeUpTime: String): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val time = LocalTime.of(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
        val wakeUp = parseTime(wakeUpTime) ?: LocalTime.of(7, 0)
        
        return if (time.isBefore(wakeUp)) {
            // Before wake-up time, so this entry belongs to the previous day's "user day"
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            dateFormat.format(calendar.time)
        } else {
            // After wake-up time, so this entry belongs to today's "user day"
            dateFormat.format(calendar.time)
        }
    }
    
    /**
     * Check if a new user day has started since the last check
     * Used to trigger data reset or cleanup
     */
    fun hasNewUserDayStarted(lastCheckTime: Long, wakeUpTime: String): Boolean {
        val lastUserDay = getUserDayStringForTimestamp(lastCheckTime, wakeUpTime)
        val currentUserDay = getCurrentUserDayString(wakeUpTime)
        return lastUserDay != currentUserDay
    }
    
    
    private fun parseTime(timeString: String): LocalTime? {
        return try {
            LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm"))
        } catch (_: Exception) {
            null
        }
    }
}
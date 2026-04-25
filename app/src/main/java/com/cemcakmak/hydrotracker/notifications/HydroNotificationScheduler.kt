package com.cemcakmak.hydrotracker.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.cemcakmak.hydrotracker.data.models.UserProfile
import com.cemcakmak.hydrotracker.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Manages scheduling of hydration reminder notifications using AlarmManager
 * Handles precise timing, sleep hours, and rescheduling based on user activity
 */
object HydroNotificationScheduler {

    private const val NOTIFICATION_REQUEST_CODE = 2001
    private const val TAG = "HydroNotificationScheduler"
    private const val PREFS_NAME = "hydro_notification_prefs"
    private const val KEY_NEXT_REMINDER_TIME = "next_reminder_time"
    private const val KEY_LAST_SCHEDULED_TIME = "last_scheduled_time"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Start notification scheduling for a user
     * Called when onboarding is completed and permissions are granted
     */
    fun startNotifications(context: Context, userProfile: UserProfile) {
        Log.d(TAG, "Starting notifications for user")
        
        if (!NotificationPermissionManager.hasNotificationPermission(context)) {
            Log.w(TAG, "Cannot start notifications: permission not granted")
            return
        }

        if (!userProfile.isOnboardingCompleted) {
            Log.w(TAG, "Cannot start notifications: onboarding not completed")
            return
        }

        // Cancel any existing notifications first
        stopNotifications(context)

        // Schedule the first notification
        scheduleNextReminder(context, userProfile)
        Log.d(TAG, "Notifications started successfully")
    }

    /**
     * Schedule the next reminder based on user profile and current progress
     */
    fun scheduleNextReminder(context: Context, userProfile: UserProfile, userRepository: UserRepository? = null, waterIntakeRepository: com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository? = null) {
        Log.d(TAG, "Scheduling next reminder")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userRepo = userRepository ?: UserRepository(context)
                val waterIntakeRepo = waterIntakeRepository ?: com.cemcakmak.hydrotracker.data.database.DatabaseInitializer
                    .getWaterIntakeRepository(context, userRepo)

                // Get current progress to check if goal is achieved
                val currentProgress = waterIntakeRepo.getTodayProgress().first()
                Log.d(TAG, "Current progress: ${currentProgress.progress}, goal achieved: ${currentProgress.isGoalAchieved}")

                // Don't schedule if goal is achieved
                if (currentProgress.isGoalAchieved) {
                    Log.d(TAG, "Goal achieved, not scheduling next reminder")
                    return@launch
                }

                val nextReminderTime = calculateNextReminderTime(userProfile)

                if (nextReminderTime == null) {
                    Log.w(TAG, "Could not calculate next reminder time")
                    return@launch
                }

                // Only schedule if the time is within waking hours
                if (isWithinWakingHours(nextReminderTime, userProfile)) {
                    scheduleNotification(context, nextReminderTime)
                    Log.d(TAG, "Next reminder scheduled for: ${nextReminderTime.time}")
                } else {
                    Log.d(TAG, "Next reminder time ${nextReminderTime.time} is outside waking hours, not scheduling")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling next reminder", e)
            }
        }
    }

    /**
     * Calculate the next reminder time based on user's schedule and interval
     */
    private fun calculateNextReminderTime(userProfile: UserProfile, fromTime: Calendar? = null): Calendar? {
        val baseTime = fromTime ?: Calendar.getInstance()
        val baseLocalTime = LocalTime.of(baseTime.get(Calendar.HOUR_OF_DAY), baseTime.get(Calendar.MINUTE))

        val wakeUpTime = parseTime(userProfile.wakeUpTime)
        val sleepTime = parseTime(userProfile.sleepTime)

        if (wakeUpTime == null || sleepTime == null) {
            Log.e(TAG, "Failed to parse wake up time (${userProfile.wakeUpTime}) or sleep time (${userProfile.sleepTime})")
            return null
        }

        Log.d(TAG, "Base time: $baseLocalTime, wake up: $wakeUpTime, sleep: $sleepTime, interval: ${userProfile.reminderInterval}min")

        // If we're currently in sleep hours, schedule for next wake-up + interval
        if (isInSleepHours(baseLocalTime, wakeUpTime, sleepTime)) {
            Log.d(TAG, "Base time is in sleep hours, scheduling for next wake up")
            val nextWakeUp = getNextWakeUpTime(baseTime, wakeUpTime)
            nextWakeUp.add(Calendar.MINUTE, userProfile.reminderInterval)
            return nextWakeUp
        }

        // Calculate next reminder time by adding interval
        val nextReminder = baseTime.clone() as Calendar
        nextReminder.add(Calendar.MINUTE, userProfile.reminderInterval)

        val nextReminderTime = LocalTime.of(
            nextReminder.get(Calendar.HOUR_OF_DAY),
            nextReminder.get(Calendar.MINUTE)
        )

        Log.d(TAG, "Next reminder would be at: $nextReminderTime")

        // Check if next reminder would be in sleep hours
        if (isInSleepHours(nextReminderTime, wakeUpTime, sleepTime)) {
            Log.d(TAG, "Next reminder would be in sleep hours, scheduling for next wake up")
            val nextWakeUp = getNextWakeUpTime(nextReminder, wakeUpTime)
            nextWakeUp.add(Calendar.MINUTE, userProfile.reminderInterval)
            return nextWakeUp
        }

        Log.d(TAG, "Scheduling next reminder for: ${nextReminder.time}")
        return nextReminder
    }

    /**
     * Get the next wake-up time (today or tomorrow)
     */
    private fun getNextWakeUpTime(fromTime: Calendar, wakeUpTime: LocalTime): Calendar {
        val wakeUp = fromTime.clone() as Calendar
        wakeUp.set(Calendar.HOUR_OF_DAY, wakeUpTime.hour)
        wakeUp.set(Calendar.MINUTE, wakeUpTime.minute)
        wakeUp.set(Calendar.SECOND, 0)
        wakeUp.set(Calendar.MILLISECOND, 0)

        // If wake-up time has already passed today, move to tomorrow
        if (wakeUp.timeInMillis <= fromTime.timeInMillis) {
            wakeUp.add(Calendar.DAY_OF_YEAR, 1)
        }

        return wakeUp
    }

    /**
     * Check if current time is in sleep hours
     */
    private fun isInSleepHours(currentTime: LocalTime, wakeUpTime: LocalTime, sleepTime: LocalTime): Boolean {
        return if (sleepTime.isAfter(wakeUpTime)) {
            // Same day sleep (e.g., wake 07:00, sleep 23:00)
            currentTime.isBefore(wakeUpTime) || currentTime.isAfter(sleepTime)
        } else {
            // Next day sleep (e.g., wake 07:00, sleep 01:00)
            currentTime.isBefore(wakeUpTime) && currentTime.isAfter(sleepTime)
        }
    }

    /**
     * Check if the given time is within waking hours
     */
    private fun isWithinWakingHours(calendar: Calendar, userProfile: UserProfile): Boolean {
        val time = LocalTime.of(
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        )

        val wakeUpTime = parseTime(userProfile.wakeUpTime)
        val sleepTime = parseTime(userProfile.sleepTime)

        if (wakeUpTime == null || sleepTime == null) {
            return true // Default to allowing if parsing fails
        }

        return !isInSleepHours(time, wakeUpTime, sleepTime)
    }

    /**
     * Schedule notification using AlarmManager and persist the schedule time
     */
    private fun scheduleNotification(context: Context, triggerTime: Calendar) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, HydroNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // Check if we can schedule exact alarms (needed for Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.e(TAG, "Cannot schedule exact alarms - permission not granted")
                    return
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime.timeInMillis,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled exact alarm (allow while idle) for: ${triggerTime.time}")
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime.timeInMillis,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled exact alarm for: ${triggerTime.time}")
            }

            // Store the scheduled time for UI display
            storeScheduledTime(context, triggerTime.timeInMillis)

            // Verify the alarm was scheduled by checking next alarm clock info
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val nextAlarm = alarmManager.nextAlarmClock
                if (nextAlarm != null) {
                    Log.d(TAG, "Next system alarm: ${Date(nextAlarm.triggerTime)}")
                }
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to schedule notification due to security exception", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule notification", e)
        }
    }

    /**
     * Store the scheduled notification time for UI display
     */
    private fun storeScheduledTime(context: Context, timeMillis: Long) {
        getPreferences(context).edit()
            .putLong(KEY_NEXT_REMINDER_TIME, timeMillis)
            .putLong(KEY_LAST_SCHEDULED_TIME, System.currentTimeMillis())
            .apply()
        Log.d(TAG, "Stored scheduled time: ${Date(timeMillis)}")
    }

    /**
     * Clear stored notification time
     */
    private fun clearScheduledTime(context: Context) {
        getPreferences(context).edit()
            .remove(KEY_NEXT_REMINDER_TIME)
            .remove(KEY_LAST_SCHEDULED_TIME)
            .apply()
        Log.d(TAG, "Cleared stored scheduled time")
    }

    /**
     * Stop all scheduled notifications
     */
    fun stopNotifications(context: Context) {
        Log.d(TAG, "Stopping all notifications")

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, HydroNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Cancelled scheduled alarms")

        // Clear stored schedule time
        clearScheduledTime(context)

        // Also cancel any visible notifications
        val notificationService = HydroNotificationService(context)
        notificationService.cancelAllNotifications()
        Log.d(TAG, "Cancelled visible notifications")

        Log.d(TAG, "All notifications stopped")
    }

    /**
     * Reschedule notifications when user profile changes
     */
    fun rescheduleNotifications(context: Context, userProfile: UserProfile) {
        stopNotifications(context)
        startNotifications(context, userProfile)
    }

    /**
     * Parse time string (HH:mm) to LocalTime
     */
    private fun parseTime(timeString: String): LocalTime? {
        return try {
            LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if notifications should be enabled for this user
     */
    fun shouldEnableNotifications(context: Context, userProfile: UserProfile): Boolean {
        return NotificationPermissionManager.hasNotificationPermission(context) &&
                userProfile.isOnboardingCompleted
    }

    /**
     * Get next scheduled notification time for UI display
     * Returns actual scheduled time from AlarmManager if available
     */
    fun getNextScheduledTime(context: Context, userProfile: UserProfile): String? {
        // First try to get the actual scheduled time with validation
        val prefs = getPreferences(context)
        val scheduledTime = prefs.getLong(KEY_NEXT_REMINDER_TIME, 0L)
        val lastScheduled = prefs.getLong(KEY_LAST_SCHEDULED_TIME, 0L)

        // Validate scheduled time
        val now = System.currentTimeMillis()
        val validationResult = validateScheduledTime(scheduledTime, lastScheduled, now)

        if (validationResult.isValid) {
            Log.d(TAG, "Using valid scheduled time: ${Date(scheduledTime)}")
            val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
            val pattern = if (is24Hour) "MMM dd, HH:mm" else "MMM dd, h:mm a"
            val formatter = SimpleDateFormat(pattern, Locale.getDefault())
            return formatter.format(Date(scheduledTime))
        } else {
            // Clear invalid cached data
            Log.w(TAG, "Invalid scheduled time detected: ${validationResult.reason}. Clearing cache.")
            clearScheduledTime(context)

            // Fallback to calculated time
            Log.d(TAG, "Calculating fresh next reminder time")
            val nextTime = calculateNextReminderTime(userProfile)
            return nextTime?.let {
                val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
                val pattern = if (is24Hour) "MMM dd, HH:mm" else "MMM dd, h:mm a"
                val formatter = SimpleDateFormat(pattern, Locale.getDefault())
                formatter.format(it.time)
            }
        }
    }

    /**
     * Validate scheduled time data for consistency and sanity
     */
    private fun validateScheduledTime(scheduledTime: Long, lastScheduled: Long, currentTime: Long): ValidationResult {
        // Check if we have any scheduled time at all
        if (scheduledTime <= 0) {
            return ValidationResult(false, "No scheduled time stored")
        }

        // Check if scheduled time is in the past
        if (scheduledTime <= currentTime) {
            return ValidationResult(false, "Scheduled time is in the past")
        }

        // Check if scheduled time is too far in the future (more than 48 hours)
        val maxFutureTime = 48 * 60 * 60 * 1000L // 48 hours
        if (scheduledTime > (currentTime + maxFutureTime)) {
            return ValidationResult(false, "Scheduled time is too far in the future")
        }

        // Check if last scheduled timestamp makes sense
        if (lastScheduled <= 0) {
            return ValidationResult(false, "No last scheduled timestamp")
        }

        // Check if last scheduled is not too old (max 24 hours old)
        val maxAge = 24 * 60 * 60 * 1000L // 24 hours
        if ((currentTime - lastScheduled) > maxAge) {
            return ValidationResult(false, "Last scheduled timestamp is too old")
        }

        // Check if last scheduled is not in the future (sanity check)
        if (lastScheduled > currentTime) {
            return ValidationResult(false, "Last scheduled timestamp is in the future")
        }

        return ValidationResult(true, "Valid")
    }

    data class ValidationResult(
        val isValid: Boolean,
        val reason: String
    )

    /**
     * Clear notification cache when database schema changes are detected
     * Should be called after database migrations or schema updates
     */
    fun clearCacheOnSchemaChange(context: Context) {
        Log.i(TAG, "Clearing notification cache due to database schema change")
        clearScheduledTime(context)

        // Also cancel any pending alarms since they might be using old data
        stopNotifications(context)

        Log.i(TAG, "Notification cache cleared successfully")
    }

    /**
     * Validate and repair notification system state
     * Call this when app starts or after database issues
     */
    fun validateAndRepairNotificationState(context: Context, userProfile: UserProfile): Boolean {
        return try {
            Log.d(TAG, "Validating notification system state")

            // Check if notification permissions are still valid
            val hasPermission = NotificationPermissionManager.hasNotificationPermission(context)
            val hasExactAlarm = NotificationPermissionManager.hasExactAlarmPermission(context)

            if (!hasPermission || !hasExactAlarm) {
                Log.w(TAG, "Notification permissions missing, clearing cache")
                clearScheduledTime(context)
                return false
            }

            // Validate cached scheduled time
            val prefs = getPreferences(context)
            val scheduledTime = prefs.getLong(KEY_NEXT_REMINDER_TIME, 0L)
            val lastScheduled = prefs.getLong(KEY_LAST_SCHEDULED_TIME, 0L)
            val now = System.currentTimeMillis()

            val validationResult = validateScheduledTime(scheduledTime, lastScheduled, now)

            if (!validationResult.isValid) {
                Log.w(TAG, "Invalid notification state detected: ${validationResult.reason}")
                clearScheduledTime(context)

                // Reschedule if user has completed onboarding
                if (userProfile.isOnboardingCompleted) {
                    Log.i(TAG, "Rescheduling notifications with fresh data")
                    startNotifications(context, userProfile)
                }

                return true // Successfully repaired
            }

            Log.d(TAG, "Notification system state is valid")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Error validating notification state", e)
            // Clear everything on error to prevent further issues
            clearScheduledTime(context)
            return false
        }
    }

    /**
     * Schedule the next reminder based on when the current one was triggered
     * This ensures continuous operation
     */
    fun scheduleNextFromTriggered(context: Context, userProfile: UserProfile, userRepository: UserRepository? = null, waterIntakeRepository: com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository? = null) {
        Log.d(TAG, "Scheduling next reminder from triggered time")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userRepo = userRepository ?: UserRepository(context)
                val waterIntakeRepo = waterIntakeRepository ?: com.cemcakmak.hydrotracker.data.database.DatabaseInitializer
                    .getWaterIntakeRepository(context, userRepo)

                // Get current progress to check if goal is achieved
                val currentProgress = waterIntakeRepo.getTodayProgress().first()
                Log.d(TAG, "Current progress: ${currentProgress.progress}, goal achieved: ${currentProgress.isGoalAchieved}")

                // Don't schedule if goal is achieved
                if (currentProgress.isGoalAchieved) {
                    Log.d(TAG, "Goal achieved, not scheduling next reminder")
                    clearScheduledTime(context)
                    return@launch
                }

                // Use current time as the base for next calculation to ensure consistent intervals
                val now = Calendar.getInstance()
                val nextReminderTime = calculateNextReminderTime(userProfile, now)

                if (nextReminderTime == null) {
                    Log.w(TAG, "Could not calculate next reminder time")
                    return@launch
                }

                // Only schedule if the time is within waking hours
                if (isWithinWakingHours(nextReminderTime, userProfile)) {
                    scheduleNotification(context, nextReminderTime)
                    Log.d(TAG, "Next reminder scheduled for: ${nextReminderTime.time}")
                } else {
                    Log.d(TAG, "Next reminder time ${nextReminderTime.time} is outside waking hours, not scheduling")
                    clearScheduledTime(context)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling next reminder from triggered", e)
            }
        }
    }
}
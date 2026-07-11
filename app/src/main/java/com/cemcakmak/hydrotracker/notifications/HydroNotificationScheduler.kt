package com.cemcakmak.hydrotracker.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.content.edit
import com.cemcakmak.hydrotracker.data.models.DateFormatPattern
import com.cemcakmak.hydrotracker.data.models.ReminderIntervalMode
import com.cemcakmak.hydrotracker.data.models.TimeFormat
import com.cemcakmak.hydrotracker.data.models.UserProfile
import com.cemcakmak.hydrotracker.data.repository.UserRepository
import com.cemcakmak.hydrotracker.utils.DateTimeFormatters
import com.cemcakmak.hydrotracker.utils.WaterCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Manages scheduling of hydration reminder notifications using AlarmManager
 * Handles precise timing, sleep hours, and rescheduling based on user activity
 */
object HydroNotificationScheduler {

    private const val NOTIFICATION_REQUEST_CODE = 2001
    private const val FUN_FACT_REQUEST_CODE = 2002
    private const val TAG = "HydroNotificationScheduler"
    private const val PREFS_NAME = "hydro_notification_prefs"
    private const val KEY_NEXT_REMINDER_TIME = "next_reminder_time"
    private const val KEY_LAST_SCHEDULED_TIME = "last_scheduled_time"
    private const val KEY_NEXT_FUN_FACT_TIME = "next_fun_fact_time"

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
     * Schedule the next reminder based on user profile and current progress.
     * In automatic mode the interval is recalculated from the remaining goal and awake time.
     */
    fun scheduleNextReminder(
        context: Context,
        userProfile: UserProfile,
        userRepository: UserRepository? = null,
        waterIntakeRepository: com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository? = null
    ) {
        Log.d(TAG, "Scheduling next reminder")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userRepo = userRepository ?: UserRepository(context)
                val waterIntakeRepo = waterIntakeRepository ?: com.cemcakmak.hydrotracker.data.database.DatabaseInitializer
                    .getWaterIntakeRepository(context, userRepo)

                val currentProgress = waterIntakeRepo.getTodayProgress().first()
                Log.d(TAG, "Current progress: ${currentProgress.progress}, goal achieved: ${currentProgress.isGoalAchieved}")

                if (currentProgress.isGoalAchieved) {
                    Log.d(TAG, "Goal achieved, not scheduling next reminder")
                    clearScheduledTime(context)
                    return@launch
                }

                val intervalMinutes = resolveReminderInterval(userProfile, waterIntakeRepo)
                Log.d(TAG, "Resolved reminder interval: ${intervalMinutes}min")

                val nextReminderTime = calculateNextReminderTime(userProfile, intervalMinutes)

                if (nextReminderTime == null) {
                    Log.w(TAG, "Could not calculate next reminder time")
                    return@launch
                }

                if (isWithinWakingHours(nextReminderTime, userProfile)) {
                    scheduleNotification(context, nextReminderTime)
                    Log.d(TAG, "Next reminder scheduled for: ${nextReminderTime.time}")
                } else {
                    Log.d(TAG, "Next reminder time ${nextReminderTime.time} is outside waking hours, not scheduling")
                    clearScheduledTime(context)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling next reminder", e)
            }
        }
    }

    /**
     * Calculate the next reminder time based on user's schedule and the supplied interval.
     */
    private fun calculateNextReminderTime(
        userProfile: UserProfile,
        intervalMinutes: Int,
        fromTime: Calendar? = null
    ): Calendar? {
        val baseTime = fromTime ?: Calendar.getInstance()
        val baseLocalTime = LocalTime.of(baseTime.get(Calendar.HOUR_OF_DAY), baseTime.get(Calendar.MINUTE))

        val wakeUpTime = parseTime(userProfile.wakeUpTime)
        val sleepTime = parseTime(userProfile.sleepTime)

        if (wakeUpTime == null || sleepTime == null) {
            Log.e(TAG, "Failed to parse wake up time (${userProfile.wakeUpTime}) or sleep time (${userProfile.sleepTime})")
            return null
        }

        Log.d(TAG, "Base time: $baseLocalTime, wake up: $wakeUpTime, sleep: $sleepTime, interval: ${intervalMinutes}min")

        // If we're currently in sleep hours, schedule for next wake-up + interval
        if (isInSleepHours(baseLocalTime, wakeUpTime, sleepTime)) {
            Log.d(TAG, "Base time is in sleep hours, scheduling for next wake up")
            val nextWakeUp = getNextWakeUpTime(baseTime, wakeUpTime)
            nextWakeUp.add(Calendar.MINUTE, intervalMinutes)
            return nextWakeUp
        }

        val nextReminder = baseTime.clone() as Calendar
        nextReminder.add(Calendar.MINUTE, intervalMinutes)

        val nextReminderTime = LocalTime.of(
            nextReminder.get(Calendar.HOUR_OF_DAY),
            nextReminder.get(Calendar.MINUTE)
        )

        Log.d(TAG, "Next reminder would be at: $nextReminderTime")

        if (isInSleepHours(nextReminderTime, wakeUpTime, sleepTime)) {
            Log.d(TAG, "Next reminder would be in sleep hours, scheduling for next wake up")
            val nextWakeUp = getNextWakeUpTime(nextReminder, wakeUpTime)
            nextWakeUp.add(Calendar.MINUTE, intervalMinutes)
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
     * Check if [currentTime] is within the sleep window defined by [wakeUpTime] and [sleepTime].
     *
     * - Same-day sleep (sleep after wake, e.g. wake 07:00, sleep 23:00): sleep is before wake or
     *   after sleep.
     * - Next-day sleep (sleep before wake, e.g. wake 07:00, sleep 01:00): sleep is from sleep time
     *   up to wake time on the same calendar day.
     */
    private fun isInSleepHours(currentTime: LocalTime, wakeUpTime: LocalTime, sleepTime: LocalTime): Boolean {
        return if (sleepTime.isAfter(wakeUpTime)) {
            currentTime.isBefore(wakeUpTime) || currentTime.isAfter(sleepTime)
        } else {
            !currentTime.isBefore(sleepTime) && currentTime.isBefore(wakeUpTime)
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
     * Schedule notification using AlarmManager and persist the schedule time.
     * Guards against scheduling alarms in the past, which would fire immediately.
     */
    private fun scheduleNotification(context: Context, triggerTime: Calendar) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()

        if (triggerTime.timeInMillis <= now) {
            Log.w(TAG, "Refusing to schedule alarm in the past. Trigger: ${triggerTime.time}, now: ${Date(now)}")
            return
        }

        val intent = Intent(context, HydroNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.e(TAG, "Cannot schedule exact alarms - permission not granted")
                    return
                }
            }

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime.timeInMillis,
                pendingIntent
            )
            Log.d(TAG, "Scheduled exact alarm (allow while idle) for: ${triggerTime.time}")

            storeScheduledTime(context, triggerTime.timeInMillis)

            val nextAlarm = alarmManager.nextAlarmClock
            if (nextAlarm != null) {
                Log.d(TAG, "Next system alarm: ${Date(nextAlarm.triggerTime)}")
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
        getPreferences(context).edit {
            putLong(KEY_NEXT_REMINDER_TIME, timeMillis)
            putLong(KEY_LAST_SCHEDULED_TIME, System.currentTimeMillis())
        }
        Log.d(TAG, "Stored scheduled time: ${Date(timeMillis)}")
    }

    /**
     * Clear stored notification time
     */
    private fun clearScheduledTime(context: Context) {
        getPreferences(context).edit {
            remove(KEY_NEXT_REMINDER_TIME)
            remove(KEY_LAST_SCHEDULED_TIME)
        }
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

        // Also cancel the visible reminder notification
        val notificationService = HydroNotificationService(context)
        notificationService.cancelReminderNotification()
        Log.d(TAG, "Cancelled visible reminder notification")

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
        } catch (_: Exception) {
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
     * Returns actual scheduled time from AlarmManager if available, formatted using the user's
     * [timeFormat] and [dateFormat] preferences.
     */
    fun getNextScheduledTime(
        context: Context,
        userProfile: UserProfile,
        timeFormat: TimeFormat = TimeFormat.SYSTEM,
        dateFormat: DateFormatPattern = DateFormatPattern.SYSTEM
    ): String? {
        // First try to get the actual scheduled time with validation
        val prefs = getPreferences(context)
        val scheduledTime = prefs.getLong(KEY_NEXT_REMINDER_TIME, 0L)
        val lastScheduled = prefs.getLong(KEY_LAST_SCHEDULED_TIME, 0L)

        // Validate scheduled time
        val now = System.currentTimeMillis()
        val validationResult = validateScheduledTime(scheduledTime, lastScheduled, now)

        val epochToFormatted: (Long) -> String = { epochMillis ->
            val localDateTime = java.time.Instant.ofEpochMilli(epochMillis)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime()
            DateTimeFormatters.formatDateTime(context, localDateTime, timeFormat, dateFormat)
        }

        if (validationResult.isValid) {
            Log.d(TAG, "Using valid scheduled time: ${Date(scheduledTime)}")
            return epochToFormatted(scheduledTime)
        } else {
            // Clear invalid cached data
            Log.w(TAG, "Invalid scheduled time detected: ${validationResult.reason}. Clearing cache.")
            clearScheduledTime(context)

            // Fallback to calculated time
            Log.d(TAG, "Calculating fresh next reminder time")
            val nextTime = calculateNextReminderTime(userProfile, userProfile.reminderInterval)
            return nextTime?.let { epochToFormatted(it.timeInMillis) }
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
                false
            } else {
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

                    true // Successfully repaired
                } else {
                    Log.d(TAG, "Notification system state is valid")
                    true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validating notification state", e)
            // Clear everything on error to prevent further issues
            clearScheduledTime(context)
            false
        }
    }

    /**
     * Schedule the next reminder based on when the current one was triggered.
     * In automatic mode the interval is recalculated from the remaining goal and awake time.
     */
    fun scheduleNextFromTriggered(
        context: Context,
        userProfile: UserProfile,
        userRepository: UserRepository? = null,
        waterIntakeRepository: com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository? = null
    ) {
        Log.d(TAG, "Scheduling next reminder from triggered time")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userRepo = userRepository ?: UserRepository(context)
                val waterIntakeRepo = waterIntakeRepository ?: com.cemcakmak.hydrotracker.data.database.DatabaseInitializer
                    .getWaterIntakeRepository(context, userRepo)

                val currentProgress = waterIntakeRepo.getTodayProgress().first()
                Log.d(TAG, "Current progress: ${currentProgress.progress}, goal achieved: ${currentProgress.isGoalAchieved}")

                if (currentProgress.isGoalAchieved) {
                    Log.d(TAG, "Goal achieved, not scheduling next reminder")
                    clearScheduledTime(context)
                    return@launch
                }

                val intervalMinutes = resolveReminderInterval(userProfile, waterIntakeRepo)
                Log.d(TAG, "Resolved reminder interval: ${intervalMinutes}min")

                val now = Calendar.getInstance()
                val nextReminderTime = calculateNextReminderTime(userProfile, intervalMinutes, now)

                if (nextReminderTime == null) {
                    Log.w(TAG, "Could not calculate next reminder time")
                    return@launch
                }

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

    /**
     * Call this whenever the user adds water. The next reminder is rescheduled from now using a
     * dynamically calculated interval so small, frequent entries still leave room for the remaining
     * reminders before sleep.
     */
    fun onWaterEntryAdded(
        context: Context,
        userProfile: UserProfile,
        userRepository: UserRepository? = null,
        waterIntakeRepository: com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository? = null
    ) {
        Log.d(TAG, "Water entry added, bumping next reminder")
        scheduleNextFromTriggered(context, userProfile, userRepository, waterIntakeRepository)
    }

    /**
     * Resolve the reminder interval. In custom mode the user's interval is honoured.
     * In automatic mode the interval is computed from the remaining goal and remaining awake time.
     */
    private suspend fun resolveReminderInterval(
        userProfile: UserProfile,
        waterIntakeRepository: com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository
    ): Int {
        if (userProfile.reminderIntervalMode == ReminderIntervalMode.CUSTOM) {
            return userProfile.customReminderInterval.coerceAtLeast(1)
        }

        val progress = waterIntakeRepository.getTodayProgress().first()
        val remainingAmount = (progress.dailyGoal - progress.currentIntake).coerceAtLeast(0.0)
        val sleepTime = parseTime(userProfile.sleepTime)
        val remainingAwakeMinutes = if (sleepTime != null) {
            calculateRemainingAwakeMinutes(Calendar.getInstance(), sleepTime).toDouble()
        } else {
            480.0 // 8 hours fallback
        }

        return WaterCalculator.calculateDynamicReminderInterval(
            remainingAmountMl = remainingAmount,
            remainingAwakeMinutes = remainingAwakeMinutes,
            reminderIntervalMode = ReminderIntervalMode.AUTOMATIC
        )
    }

    /**
     * Calculate the minutes remaining until the given sleep time today (or tomorrow if it has
     * already passed).
     */
    private fun calculateRemainingAwakeMinutes(now: Calendar, sleepTime: LocalTime): Int {
        val sleepToday = now.clone() as Calendar
        sleepToday.set(Calendar.HOUR_OF_DAY, sleepTime.hour)
        sleepToday.set(Calendar.MINUTE, sleepTime.minute)
        sleepToday.set(Calendar.SECOND, 0)
        sleepToday.set(Calendar.MILLISECOND, 0)

        if (sleepToday.timeInMillis <= now.timeInMillis) {
            sleepToday.add(Calendar.DAY_OF_YEAR, 1)
        }

        return ((sleepToday.timeInMillis - now.timeInMillis) / 60_000).toInt().coerceAtLeast(0)
    }

    /**
     * Schedule the daily fun-fact notification at a random time within the user's waking hours.
     * If the chosen time has already passed today, it is scheduled for tomorrow.
     */
    fun scheduleFunFact(context: Context, userProfile: UserProfile) {
        if (!userProfile.funFactsEnabled) {
            Log.d(TAG, "Fun facts disabled, not scheduling")
            return
        }

        if (!NotificationPermissionManager.hasNotificationPermission(context)) {
            Log.w(TAG, "Cannot schedule fun fact: notification permission not granted")
            return
        }

        val wakeUpTime = parseTime(userProfile.wakeUpTime)
        val sleepTime = parseTime(userProfile.sleepTime)

        if (wakeUpTime == null || sleepTime == null) {
            Log.e(TAG, "Failed to parse wake/sleep time for fun-fact scheduling")
            return
        }

        val triggerTime = calculateRandomFunFactTime(Calendar.getInstance(), wakeUpTime, sleepTime)
        if (triggerTime == null) {
            Log.w(TAG, "Could not calculate fun-fact time")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, FunFactReceiver::class.java).apply {
            action = FunFactReceiver.ACTION_SHOW_FUN_FACT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            FUN_FACT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Log.e(TAG, "Cannot schedule exact alarms - permission not granted")
                return
            }

            if (triggerTime.timeInMillis <= System.currentTimeMillis()) {
                Log.w(TAG, "Calculated fun-fact time is in the past, skipping schedule")
                return
            }

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime.timeInMillis,
                pendingIntent
            )

            getPreferences(context).edit {
                putLong(KEY_NEXT_FUN_FACT_TIME, triggerTime.timeInMillis)
            }

            Log.d(TAG, "Scheduled fun fact for: ${triggerTime.time}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule fun fact", e)
        }
    }

    /**
     * Cancel the scheduled fun-fact alarm and any visible fun-fact notification.
     */
    fun cancelFunFacts(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, FunFactReceiver::class.java).apply {
            action = FunFactReceiver.ACTION_SHOW_FUN_FACT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            FUN_FACT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        HydroNotificationService(context).cancelFunFactNotification()
        getPreferences(context).edit { remove(KEY_NEXT_FUN_FACT_TIME) }
        Log.d(TAG, "Cancelled fun-fact alarm and notification")
    }

    /**
     * Pick a random time between wake-up and sleep and return it as a [Calendar].
     * Returns null if the awake window cannot be determined.
     */
    private fun calculateRandomFunFactTime(
        now: Calendar,
        wakeUpTime: LocalTime,
        sleepTime: LocalTime
    ): Calendar? {
        val wakeUp = now.clone() as Calendar
        wakeUp.set(Calendar.HOUR_OF_DAY, wakeUpTime.hour)
        wakeUp.set(Calendar.MINUTE, wakeUpTime.minute)
        wakeUp.set(Calendar.SECOND, 0)
        wakeUp.set(Calendar.MILLISECOND, 0)

        val sleep = now.clone() as Calendar
        sleep.set(Calendar.HOUR_OF_DAY, sleepTime.hour)
        sleep.set(Calendar.MINUTE, sleepTime.minute)
        sleep.set(Calendar.SECOND, 0)
        sleep.set(Calendar.MILLISECOND, 0)

        if (sleep.timeInMillis <= wakeUp.timeInMillis) {
            sleep.add(Calendar.DAY_OF_YEAR, 1)
        }

        val awakeMinutes = ((sleep.timeInMillis - wakeUp.timeInMillis) / 60_000).toInt()
        if (awakeMinutes <= 0) return null

        val randomOffsetMinutes = Random().nextInt(awakeMinutes)
        val triggerTime = wakeUp.clone() as Calendar
        triggerTime.add(Calendar.MINUTE, randomOffsetMinutes)

        if (triggerTime.timeInMillis <= now.timeInMillis) {
            triggerTime.add(Calendar.DAY_OF_YEAR, 1)
        }

        return triggerTime
    }
}
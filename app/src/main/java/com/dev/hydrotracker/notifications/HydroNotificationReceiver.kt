package com.dev.hydrotracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dev.hydrotracker.data.database.DatabaseInitializer
import com.dev.hydrotracker.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that handles scheduled notification alarms
 * Triggered by AlarmManager to show hydration reminders
 */
class HydroNotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "HydroNotificationReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Notification alarm received")
        // Handle the notification in a coroutine since we need to access database
        CoroutineScope(Dispatchers.IO).launch {
            handleNotificationTrigger(context)
        }
    }

    private suspend fun handleNotificationTrigger(context: Context) {
        try {
            Log.d(TAG, "Handling notification trigger")
            
            // Initialize repositories
            val userRepository = UserRepository(context)
            val userProfile = userRepository.userProfile.first()

            // Check if user profile exists and notifications should be sent
            if (userProfile == null || !userProfile.isOnboardingCompleted) {
                Log.w(TAG, "User profile not found or onboarding not completed")
                return
            }

            if (!NotificationPermissionManager.hasNotificationPermission(context)) {
                Log.w(TAG, "Notification permission not granted")
                return
            }

            val waterIntakeRepository = DatabaseInitializer.getWaterIntakeRepository(
                context = context,
                userRepository = userRepository
            )

            // Get current progress
            val currentProgress = waterIntakeRepository.getTodayProgress().first()
            Log.d(TAG, "Current progress: ${currentProgress.progress}, goal achieved: ${currentProgress.isGoalAchieved}")

            // Don't send notification if goal is already achieved
            if (currentProgress.isGoalAchieved) {
                Log.d(TAG, "Goal already achieved, not showing notification")
                return
            }

            // Check if we're within waking hours (double-check)
            if (!isWithinWakingHours(context, userProfile)) {
                Log.d(TAG, "Outside waking hours, rescheduling for appropriate time")
                // Schedule for next appropriate time using the improved scheduling method
                HydroNotificationScheduler.scheduleNextFromTriggered(context, userProfile)
                return
            }

            Log.d(TAG, "Showing hydration reminder notification")
            // Show the notification
            val notificationService = HydroNotificationService(context)
            notificationService.showHydrationReminder(userProfile, currentProgress)

            // Schedule the next reminder using the new method that ensures continuous operation
            Log.d(TAG, "Scheduling next reminder from triggered time")
            HydroNotificationScheduler.scheduleNextFromTriggered(context, userProfile)

        } catch (e: Exception) {
            Log.e(TAG, "Error handling notification trigger", e)
        }
    }

    private fun isWithinWakingHours(context: Context, userProfile: com.dev.hydrotracker.data.models.UserProfile): Boolean {
        return try {
            val currentTime = java.time.LocalTime.now()
            val wakeUpTime = java.time.LocalTime.parse(userProfile.wakeUpTime, java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            val sleepTime = java.time.LocalTime.parse(userProfile.sleepTime, java.time.format.DateTimeFormatter.ofPattern("HH:mm"))

            if (sleepTime.isAfter(wakeUpTime)) {
                // Same day sleep (e.g., wake 07:00, sleep 23:00)
                currentTime.isAfter(wakeUpTime) && currentTime.isBefore(sleepTime)
            } else {
                // Next day sleep (e.g., wake 07:00, sleep 01:00)
                currentTime.isAfter(wakeUpTime) || currentTime.isBefore(sleepTime)
            }
        } catch (e: Exception) {
            true // Default to allowing if parsing fails
        }
    }
}
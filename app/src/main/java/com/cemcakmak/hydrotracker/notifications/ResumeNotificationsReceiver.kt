package com.cemcakmak.hydrotracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.cemcakmak.hydrotracker.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered by the resume alarm at the start of the next user day.
 * Clears the suspend state and restarts the reminder schedule.
 */
class ResumeNotificationsReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_RESUME_NOTIFICATIONS = "com.cemcakmak.hydrotracker.RESUME_NOTIFICATIONS"
        private const val TAG = "ResumeNotificationsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_RESUME_NOTIFICATIONS) return

        Log.d(TAG, "Resume notifications alarm received")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userRepository = UserRepository(context)
                val userProfile = userRepository.userProfile.first()

                // Clear the suspend state regardless of whether a profile is present.
                HydroNotificationScheduler.resetNotificationEngagementState(context)

                if (userProfile != null && userProfile.isOnboardingCompleted) {
                    Log.d(TAG, "Resuming reminder schedule")
                    HydroNotificationScheduler.startNotifications(context, userProfile)
                } else {
                    Log.d(TAG, "No completed profile, nothing to resume")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error resuming notifications", e)
            }
        }
    }
}

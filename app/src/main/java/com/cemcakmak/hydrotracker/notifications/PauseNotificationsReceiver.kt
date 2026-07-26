package com.cemcakmak.hydrotracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered from the notification action that pauses hydration reminders
 * until the start of the next user day.
 */
class PauseNotificationsReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_PAUSE_NOTIFICATIONS = "com.cemcakmak.hydrotracker.PAUSE_NOTIFICATIONS"
        private const val TAG = "PauseNotificationsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PAUSE_NOTIFICATIONS) return

        Log.d(TAG, "Pause notifications action received")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userRepository = UserRepository(context)
                val userProfile = userRepository.userProfile.first()
                if (userProfile == null) {
                    Log.w(TAG, "User profile not found, cannot pause reminders")
                    return@launch
                }

                HydroNotificationScheduler.suspendRemindersUntilNextDay(context, userProfile)

                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(
                        context,
                        R.string.notification_paused_confirmation,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error pausing notifications", e)
            }
        }
    }
}

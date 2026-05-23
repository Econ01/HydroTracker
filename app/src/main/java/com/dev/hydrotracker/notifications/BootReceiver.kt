package com.dev.hydrotracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dev.hydrotracker.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that handles device boot completion and app updates
 * Reschedules notifications after system restart
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.d(TAG, "Received ${intent.action}, rescheduling notifications")
                rescheduleNotifications(context)
            }
        }
    }

    private fun rescheduleNotifications(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userRepository = UserRepository(context)
                val userProfile = userRepository.userProfile.first()

                if (userProfile != null && userProfile.isOnboardingCompleted) {
                    if (NotificationPermissionManager.hasNotificationPermission(context)) {
                        Log.d(TAG, "Rescheduling notifications for user")
                        HydroNotificationScheduler.startNotifications(context, userProfile)
                    } else {
                        Log.d(TAG, "Notification permission not granted, skipping reschedule")
                    }
                } else {
                    Log.d(TAG, "User profile not found or onboarding not completed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling notifications after boot", e)
            }
        }
    }
}
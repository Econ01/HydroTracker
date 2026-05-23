// QuickAddWaterReceiver.kt
// Location: app/src/main/java/com/cemcakmak/hydrotracker/notifications/QuickAddWaterReceiver.kt

package com.dev.hydrotracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.dev.hydrotracker.data.database.DatabaseInitializer
import com.dev.hydrotracker.data.models.ContainerPreset
import com.dev.hydrotracker.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver to handle quick water addition from notifications
 * Allows users to log water without opening the app
 */
class QuickAddWaterReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_QUICK_ADD_WATER = "com.dev.hydrotracker.QUICK_ADD_WATER"
        const val QUICK_ADD_AMOUNT = 250.0 // Default quick add amount in ml
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_QUICK_ADD_WATER) {
            addQuickWater(context)
        }
    }

    private fun addQuickWater(context: Context) {
        // Use coroutine scope for database operations
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Initialize repositories
                val userRepository = UserRepository(context)
                val waterIntakeRepository = DatabaseInitializer.getWaterIntakeRepository(
                    context = context,
                    userRepository = userRepository
                )

                // Create a preset for quick add
                val quickAddPreset = ContainerPreset(
                    name = "Quick Add",
                    volume = QUICK_ADD_AMOUNT,
                    isDefault = false
                )

                // Add water to database
                val result = waterIntakeRepository.addWaterIntake(
                    amount = QUICK_ADD_AMOUNT,
                    containerPreset = quickAddPreset,
                    note = "Added from notification"
                )

                result.onSuccess {
                    // Cancel the notification after successful addition
                    val notificationManager = NotificationManagerCompat.from(context)
                    notificationManager.cancel(HydroNotificationService.NOTIFICATION_ID)

                    // Show a brief success notification
                    showSuccessNotification(context)

                    // Reschedule next reminder
                    val userProfile = userRepository.userProfile.value
                    if (userProfile != null) {
                        HydroNotificationScheduler.scheduleNextReminder(context, userProfile)
                    }
                }

                result.onFailure { error ->
                    println("Failed to add quick water: ${error.message}")
                }

            } catch (e: Exception) {
                println("Error in quick add water: ${e.message}")
            }
        }
    }

    private fun showSuccessNotification(context: Context) {
        val notificationService = HydroNotificationService(context)

        // Create a simple success notification that auto-dismisses
        val successNotification = android.app.Notification.Builder(context, HydroNotificationService.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_save) // System checkmark icon
            .setContentTitle("💧 Water Added!")
            .setContentText("Added 250ml to your daily intake")
            .setAutoCancel(true)
            .setTimeoutAfter(3000) // Auto dismiss after 3 seconds
            .setPriority(android.app.Notification.PRIORITY_LOW)
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(9999, successNotification) // Different ID for success notification
        } catch (e: SecurityException) {
            println("Cannot show success notification: ${e.message}")
        }
    }
}
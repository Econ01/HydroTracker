// QuickAddWaterReceiver.kt
// Location: app/src/main/java/com/cemcakmak/hydrotracker/notifications/QuickAddWaterReceiver.kt

package com.cemcakmak.hydrotracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.database.DatabaseInitializer
import com.cemcakmak.hydrotracker.data.models.ContainerPreset
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.data.repository.UserRepository
import com.cemcakmak.hydrotracker.utils.VolumeUnitConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver to handle quick water addition from notifications
 * Allows users to log water without opening the app
 */
class QuickAddWaterReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_QUICK_ADD_WATER = "com.cemcakmak.hydrotracker.QUICK_ADD_WATER"
        const val EXTRA_CONTAINER_VOLUME = "extra_container_volume"
        const val EXTRA_CONTAINER_NAME = "extra_container_name"
        private const val DEFAULT_QUICK_ADD_AMOUNT = 250.0
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_QUICK_ADD_WATER) {
            val amount = intent.getDoubleExtra(EXTRA_CONTAINER_VOLUME, DEFAULT_QUICK_ADD_AMOUNT)
            val name = intent.getStringExtra(EXTRA_CONTAINER_NAME)
                ?: context.getString(R.string.notification_quick_add_preset_name)
            addQuickWater(context, amount, name)
        }
    }

    private fun addQuickWater(context: Context, amount: Double, containerName: String) {
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
                    name = containerName,
                    volume = amount,
                    isDefault = false
                )

                // Add water to database
                val result = waterIntakeRepository.addWaterIntake(
                    amount = amount,
                    containerPreset = quickAddPreset,
                    note = context.getString(R.string.notification_quick_add_note)
                )

                result.onSuccess {
                    // Cancel the notification after successful addition
                    val notificationManager = NotificationManagerCompat.from(context)
                    notificationManager.cancel(HydroNotificationService.NOTIFICATION_ID)

                    // Show a brief success notification
                    showSuccessNotification(context, amount)

                    // Reschedule next reminder with a dynamically calculated interval
                    val userProfile = userRepository.userProfile.first()
                    if (userProfile != null) {
                        HydroNotificationScheduler.onWaterEntryAdded(
                            context,
                            userProfile,
                            userRepository = userRepository,
                            waterIntakeRepository = waterIntakeRepository
                        )
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

    private fun showSuccessNotification(context: Context, amount: Double) {

        // Format the quick-add amount in the user's preferred unit.
        val userRepository = UserRepository(context)
        val volumeUnit = try {
            kotlinx.coroutines.runBlocking {
                userRepository.userProfile.first()?.volumeUnit ?: VolumeUnit.MILLILITRES
            }
        } catch (_: Exception) {
            VolumeUnit.MILLILITRES
        }
        val amountText = VolumeUnitConverter.format(context, amount, volumeUnit)

        // Create a simple success notification that auto-dismisses
        val successNotification = android.app.Notification.Builder(context, HydroNotificationService.REMINDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_save) // System checkmark icon
            .setContentTitle(context.getString(R.string.notification_quick_add_title))
            .setContentText(context.getString(R.string.notification_quick_add_text, amountText))
            .setAutoCancel(true)
            .setTimeoutAfter(3000) // Auto dismiss after 3 seconds
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(9999, successNotification) // Different ID for success notification
        } catch (e: SecurityException) {
            println("Cannot show success notification: ${e.message}")
        }
    }
}
// QuickAddWaterReceiver.kt
// Location: app/src/main/java/com/cemcakmak/hydrotracker/notifications/QuickAddWaterReceiver.kt

package com.cemcakmak.hydrotracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.database.DatabaseInitializer
import com.cemcakmak.hydrotracker.data.models.ContainerPreset
import com.cemcakmak.hydrotracker.data.repository.UserRepository
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
                    // Show a brief feedback notification with the updated progress
                    val userProfile = userRepository.userProfile.first()
                    if (userProfile != null) {
                        val updatedProgress = waterIntakeRepository.getTodayProgress().first()

                        HydroNotificationService(context).showQuickAddFeedbackNotification(
                            userProfile,
                            updatedProgress,
                            amount
                        )

                        // Reschedule next reminder with a dynamically calculated interval
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

}
package com.cemcakmak.hydrotracker.widgets.actions

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.cemcakmak.hydrotracker.data.database.DatabaseInitializer
import com.cemcakmak.hydrotracker.data.models.ContainerPreset
import com.cemcakmak.hydrotracker.data.repository.UserRepository

/**
 * Handles quick-add taps from the Large widget: writes the intake entry to the database.
 * [com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository.addWaterIntake]
 * refreshes every widget (and Health Connect) after the write, so no explicit widget
 * update is needed here.
 */
class QuickAddAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val amount = parameters[KEY_AMOUNT] ?: return
        val container = parameters[KEY_CONTAINER] ?: "Glass"
        Log.d(TAG, "💧 Widget quick-add: ${amount}ml ($container)")
        try {
            val userRepository = UserRepository(context)
            val waterRepository = DatabaseInitializer.getWaterIntakeRepository(context, userRepository)
            waterRepository.addWaterIntake(
                amount = amount,
                containerPreset = ContainerPreset(name = container, volume = amount),
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Widget quick-add failed", e)
        }
    }

    companion object {
        private const val TAG = "QuickAddAction"
        val KEY_AMOUNT = ActionParameters.Key<Double>("hydro_widget_amount")
        val KEY_CONTAINER = ActionParameters.Key<String>("hydro_widget_container")
    }
}

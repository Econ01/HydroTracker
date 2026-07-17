package com.cemcakmak.hydrotracker.widgets.actions

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.cemcakmak.hydrotracker.data.database.DatabaseInitializer
import com.cemcakmak.hydrotracker.data.models.BeverageType
import com.cemcakmak.hydrotracker.data.models.ContainerPreset
import com.cemcakmak.hydrotracker.data.repository.UserRepository
import kotlinx.coroutines.flow.first

/**
 * Handles quick-add taps from the Large widget: writes the intake entry to the database.
 * [com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository.addWaterIntake]
 * refreshes every widget (and Health Connect) after the write, so no explicit widget
 * update is needed here.
 *
 * Cards carry the beverage they represent ([KEY_BEVERAGE]) — a [BeverageType] enum name for
 * standard beverages or the custom beverage's name — so taps log the same beverage the
 * user's history surfaced, not just plain water.
 */
class QuickAddAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val amount = parameters[KEY_AMOUNT] ?: return
        val container = parameters[KEY_CONTAINER] ?: "Glass"
        val beverage = parameters[KEY_BEVERAGE] ?: BeverageType.WATER.name
        Log.d(TAG, "💧 Widget quick-add: ${amount}ml ($container, $beverage)")
        try {
            val userRepository = UserRepository(context)
            val waterRepository = DatabaseInitializer.getWaterIntakeRepository(context, userRepository)
            waterRepository.addWaterIntake(
                amount = amount,
                containerPreset = ContainerPreset(name = container, volume = amount),
                beverageKey = beverage,
                beverageMultiplier = resolveCustomMultiplier(context, beverage),
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Widget quick-add failed", e)
        }
    }

    /**
     * Custom beverages are logged under their own name; look up their multiplier so the
     * entry hydrates correctly. Standard [BeverageType]s return null — the entry then
     * falls back to the type's built-in multiplier.
     */
    private suspend fun resolveCustomMultiplier(context: Context, beverage: String): Double? {
        val isStandard = BeverageType.entries.any {
            it.name == beverage || it.displayName.equals(beverage, ignoreCase = true)
        }
        if (isStandard) return null
        return runCatching {
            DatabaseInitializer.getCustomBeverageRepository(context).getAll().first()
                .firstOrNull { it.name.equals(beverage, ignoreCase = true) }?.hydrationMultiplier
        }.getOrNull()
    }

    companion object {
        private const val TAG = "QuickAddAction"
        val KEY_AMOUNT = ActionParameters.Key<Double>("hydro_widget_amount")
        val KEY_CONTAINER = ActionParameters.Key<String>("hydro_widget_container")
        val KEY_BEVERAGE = ActionParameters.Key<String>("hydro_widget_beverage")
    }
}

package com.cemcakmak.hydrotracker.widgets

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cemcakmak.hydrotracker.data.database.DatabaseInitializer
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.data.repository.UserRepository
import kotlinx.coroutines.flow.first

/**
 * Immutable snapshot of everything the home-screen widgets need to render.
 * [HydroWidgetUpdater] loads it on every data change and mirrors it into each widget's
 * Glance preferences; the widget UI renders from that state store, so recompositions
 * within a live Glance session always show current data.
 */
data class HydroWidgetState(
    val currentIntake: Double,
    val dailyGoal: Double,
    val progress: Float,
    val isGoalAchieved: Boolean,
    val remainingAmount: Double,
    val volumeUnit: VolumeUnit,
) {
    companion object {
        /** Safe fallback shown when the repository cannot be read (e.g. before onboarding). */
        val EMPTY = HydroWidgetState(
            currentIntake = 0.0,
            dailyGoal = 2700.0,
            progress = 0f,
            isGoalAchieved = false,
            remainingAmount = 2700.0,
            volumeUnit = VolumeUnit.MILLILITRES,
        )
    }
}

// Glance state keys mirroring every [HydroWidgetState] field, stored in the widget's default
// PreferencesGlanceStateDefinition store — the same store a live session re-reads on update.
private val KEY_CURRENT_INTAKE = doublePreferencesKey("hydro_widget_current_intake")
private val KEY_DAILY_GOAL = doublePreferencesKey("hydro_widget_daily_goal")
private val KEY_PROGRESS = floatPreferencesKey("hydro_widget_progress")
private val KEY_GOAL_ACHIEVED = booleanPreferencesKey("hydro_widget_goal_achieved")
private val KEY_REMAINING = doublePreferencesKey("hydro_widget_remaining")
private val KEY_VOLUME_UNIT = stringPreferencesKey("hydro_widget_volume_unit")

/** Writes this snapshot into the widget's Glance preferences. */
fun HydroWidgetState.writeTo(prefs: MutablePreferences) {
    prefs[KEY_CURRENT_INTAKE] = currentIntake
    prefs[KEY_DAILY_GOAL] = dailyGoal
    prefs[KEY_PROGRESS] = progress
    prefs[KEY_GOAL_ACHIEVED] = isGoalAchieved
    prefs[KEY_REMAINING] = remainingAmount
    prefs[KEY_VOLUME_UNIT] = volumeUnit.name
}

/**
 * Reads a [HydroWidgetState] from the widget's Glance preferences. Missing or invalid values
 * fall back to [HydroWidgetState.EMPTY], so a widget pinned before the first refresh renders
 * the same defaults as a load failure.
 */
fun Preferences.toHydroWidgetState(): HydroWidgetState {
    val empty = HydroWidgetState.EMPTY
    return HydroWidgetState(
        currentIntake = this[KEY_CURRENT_INTAKE] ?: empty.currentIntake,
        dailyGoal = this[KEY_DAILY_GOAL] ?: empty.dailyGoal,
        progress = this[KEY_PROGRESS] ?: empty.progress,
        isGoalAchieved = this[KEY_GOAL_ACHIEVED] ?: empty.isGoalAchieved,
        remainingAmount = this[KEY_REMAINING] ?: empty.remainingAmount,
        volumeUnit = this[KEY_VOLUME_UNIT]
            ?.let { name -> runCatching { VolumeUnit.valueOf(name) }.getOrNull() }
            ?: empty.volumeUnit,
    )
}

/**
 * Loads the current [HydroWidgetState] from the repositories.
 * Never throws: on any failure it logs and returns [HydroWidgetState.EMPTY].
 */
object HydroWidgetStateLoader {

    private const val TAG = "HydroWidgetStateLoader"

    suspend fun load(context: Context): HydroWidgetState = try {
        val userRepository = UserRepository(context)
        val waterRepository = DatabaseInitializer.getWaterIntakeRepository(context, userRepository)
        val progress = waterRepository.getTodayProgress().first()
        val userProfile = userRepository.userProfile.first()
        Log.d(TAG, "📥 Loaded widget state: intake=${progress.currentIntake}, goal=${progress.dailyGoal}, progress=${progress.progress}")
        HydroWidgetState(
            currentIntake = progress.currentIntake,
            dailyGoal = progress.dailyGoal,
            progress = progress.progress,
            isGoalAchieved = progress.isGoalAchieved,
            remainingAmount = progress.remainingAmount,
            volumeUnit = userProfile?.volumeUnit ?: VolumeUnit.MILLILITRES,
        )
    } catch (e: Exception) {
        Log.w(TAG, "❌ Failed to load widget state, falling back to defaults", e)
        HydroWidgetState.EMPTY
    }
}

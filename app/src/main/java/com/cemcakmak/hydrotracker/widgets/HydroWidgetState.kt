package com.cemcakmak.hydrotracker.widgets

import android.content.Context
import android.util.Log
import com.cemcakmak.hydrotracker.data.database.DatabaseInitializer
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.data.repository.UserRepository
import kotlinx.coroutines.flow.first

/**
 * Immutable snapshot of everything the home-screen widgets need to render.
 * Loaded fresh on every widget update; the widgets themselves stay stateless.
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

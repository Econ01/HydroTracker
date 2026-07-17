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
import com.cemcakmak.hydrotracker.data.database.dao.MostUsedQuickAddCombo
import com.cemcakmak.hydrotracker.data.models.BeverageType
import com.cemcakmak.hydrotracker.data.models.PinnedQuickAddSlot
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
    val quickAddSlots: List<WidgetQuickAddSlot?> = emptyList(),
    val useDynamicColors: Boolean = true,
    val useTransparentBackground: Boolean = false,
    val usePureBlack: Boolean = false,
    val usePureWhite: Boolean = false,
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

// Widget appearance flags mirrored from WidgetPreferences (see data/models/WidgetPreferences.kt).
private val KEY_USE_DYNAMIC = booleanPreferencesKey("hydro_widget_use_dynamic")
private val KEY_TRANSPARENT_BG = booleanPreferencesKey("hydro_widget_transparent_bg")
private val KEY_PURE_BLACK = booleanPreferencesKey("hydro_widget_pure_black")
private val KEY_PURE_WHITE = booleanPreferencesKey("hydro_widget_pure_white")

/** Number of quick-add cards on the Large widget. */
internal const val QUICK_ADD_SLOT_COUNT = 3

// Quick-add card slots: the user's top (container, volume, beverage) combos by entry count.
// All slot keys are cleared on every write so stale slots never survive a smaller combo list.
private fun quickAddVolumeKey(slot: Int) = doublePreferencesKey("hydro_widget_quickadd_${slot}_volume")
private fun quickAddBeverageKey(slot: Int) = stringPreferencesKey("hydro_widget_quickadd_${slot}_beverage")
private fun quickAddContainerKey(slot: Int) = stringPreferencesKey("hydro_widget_quickadd_${slot}_container")

/** Writes this snapshot into the widget's Glance preferences. */
fun HydroWidgetState.writeTo(prefs: MutablePreferences) {
    prefs[KEY_CURRENT_INTAKE] = currentIntake
    prefs[KEY_DAILY_GOAL] = dailyGoal
    prefs[KEY_PROGRESS] = progress
    prefs[KEY_GOAL_ACHIEVED] = isGoalAchieved
    prefs[KEY_REMAINING] = remainingAmount
    prefs[KEY_VOLUME_UNIT] = volumeUnit.name
    prefs[KEY_USE_DYNAMIC] = useDynamicColors
    prefs[KEY_TRANSPARENT_BG] = useTransparentBackground
    prefs[KEY_PURE_BLACK] = usePureBlack
    prefs[KEY_PURE_WHITE] = usePureWhite
    repeat(QUICK_ADD_SLOT_COUNT) { slot ->
        prefs.remove(quickAddVolumeKey(slot))
        prefs.remove(quickAddBeverageKey(slot))
        prefs.remove(quickAddContainerKey(slot))
    }
    quickAddSlots.take(QUICK_ADD_SLOT_COUNT).forEachIndexed { slot, combo ->
        // Positions are significant: a null stays absent so the UI falls back to its default card.
        if (combo == null) return@forEachIndexed
        prefs[quickAddVolumeKey(slot)] = combo.volume
        prefs[quickAddBeverageKey(slot)] = combo.beverageName
        prefs[quickAddContainerKey(slot)] = combo.containerName
    }
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
        quickAddSlots = (0 until QUICK_ADD_SLOT_COUNT).map { slot ->
            val volume = this[quickAddVolumeKey(slot)] ?: return@map null
            WidgetQuickAddSlot(
                volume = volume,
                beverageName = this[quickAddBeverageKey(slot)] ?: BeverageType.WATER.name,
                containerName = this[quickAddContainerKey(slot)].orEmpty(),
            )
        },
        useDynamicColors = this[KEY_USE_DYNAMIC] ?: empty.useDynamicColors,
        useTransparentBackground = this[KEY_TRANSPARENT_BG] ?: empty.useTransparentBackground,
        usePureBlack = this[KEY_PURE_BLACK] ?: empty.usePureBlack,
        usePureWhite = this[KEY_PURE_WHITE] ?: empty.usePureWhite,
    )
}

/**
 * Loads the current [HydroWidgetState] from the repositories.
 * Never throws: on any failure it logs and returns [HydroWidgetState.EMPTY].
 */
object HydroWidgetStateLoader {

    private const val TAG = "HydroWidgetStateLoader"

    /** Combos fetched before de-duplicating against pinned slots, so auto slots stay filled. */
    private const val AUTO_COMBO_FETCH_LIMIT = 10

    suspend fun load(context: Context): HydroWidgetState = try {
        val userRepository = UserRepository(context)
        val waterRepository = DatabaseInitializer.getWaterIntakeRepository(context, userRepository)
        val progress = waterRepository.getTodayProgress().first()
        val userProfile = userRepository.userProfile.first()
        val widgetPrefs = userRepository.widgetPreferences.first()
        val combos = waterRepository.getTopQuickAddCombos(AUTO_COMBO_FETCH_LIMIT)
        val quickAddSlots = resolveQuickAddSlots(combos, widgetPrefs.pinnedQuickAddSlots)
        Log.d(TAG, "📥 Loaded widget state: intake=${progress.currentIntake}, goal=${progress.dailyGoal}, progress=${progress.progress}")
        HydroWidgetState(
            currentIntake = progress.currentIntake,
            dailyGoal = progress.dailyGoal,
            progress = progress.progress,
            isGoalAchieved = progress.isGoalAchieved,
            remainingAmount = progress.remainingAmount,
            volumeUnit = userProfile?.volumeUnit ?: VolumeUnit.MILLILITRES,
            quickAddSlots = quickAddSlots,
            useDynamicColors = widgetPrefs.useDynamicColors,
            useTransparentBackground = widgetPrefs.useTransparentBackground,
            usePureBlack = widgetPrefs.usePureBlack,
            usePureWhite = widgetPrefs.usePureWhite,
        )
    } catch (e: Exception) {
        Log.w(TAG, "❌ Failed to load widget state, falling back to defaults", e)
        HydroWidgetState.EMPTY
    }

    /**
     * Final quick-add cards: each position is the user's pinned choice when set, otherwise the
     * next most-common combo. Combos identical to a pinned slot are skipped so cards never repeat.
     */
    private fun resolveQuickAddSlots(
        combos: List<MostUsedQuickAddCombo>,
        pinned: List<PinnedQuickAddSlot>,
    ): List<WidgetQuickAddSlot?> {
        val pinnedBySlot = pinned.associateBy { it.slot }
        val pinnedIdentities = pinned
            .map { Triple(it.containerName, it.volume, it.beverageName) }
            .toSet()
        val autos = combos
            .filter { Triple(it.containerName, it.volume, it.beverage) !in pinnedIdentities }
            .iterator()
        // map, not mapNotNull: unfilled positions stay null so a pin keeps its position.
        return (0 until QUICK_ADD_SLOT_COUNT).map { slot ->
            pinnedBySlot[slot]?.let { pin ->
                WidgetQuickAddSlot(
                    volume = pin.volume,
                    beverageName = pin.beverageName,
                    containerName = pin.containerName,
                )
            } ?: autos.takeIf { it.hasNext() }?.next()?.let { combo ->
                WidgetQuickAddSlot(
                    volume = combo.volume,
                    beverageName = combo.beverage,
                    containerName = combo.containerName,
                )
            }
        }
    }
}

package com.cemcakmak.hydrotracker.widgets

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll

/**
 * Pushes fresh content to every HydroTracker home-screen widget.
 * Called from the data layer (e.g. [com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository])
 * whenever hydration data changes. Never throws — widget updates must not break app flows.
 *
 * One snapshot is mirrored into every hydration and quick-actions instance before either provider
 * is refreshed. A live Glance session recomposes from that state store on every update, so this
 * ordering guarantees current data even when only the quick-actions provider is placed.
 */
object HydroWidgetUpdater {

    private const val TAG = "HydroWidgetUpdater"

    suspend fun updateAll(context: Context) {
        try {
            val manager = GlanceAppWidgetManager(context)
            val hydrationIds = manager.getGlanceIds(HydroLargeGlanceWidget::class.java)
            val quickAddIds = manager.getGlanceIds(HydroQuickAddGlanceWidget::class.java)
            if (hydrationIds.isEmpty() && quickAddIds.isEmpty()) return

            val state = HydroWidgetStateLoader.load(context)
            (hydrationIds + quickAddIds).forEach { glanceId ->
                updateAppWidgetState(context, glanceId) { prefs -> state.writeTo(prefs) }
            }

            val widgets = listOf(HydroLargeGlanceWidget(), HydroQuickAddGlanceWidget())
            widgets.forEach { widget ->
                try {
                    widget.updateAll(context)
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Failed to refresh ${widget.javaClass.simpleName}", e)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Failed to update home-screen widgets", e)
        }
    }
}

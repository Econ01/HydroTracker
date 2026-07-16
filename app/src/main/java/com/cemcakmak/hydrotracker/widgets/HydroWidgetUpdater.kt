package com.cemcakmak.hydrotracker.widgets

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll

/**
 * Pushes fresh content to the HydroTracker home-screen widget.
 * Called from the data layer (e.g. [com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository])
 * whenever hydration data changes. Never throws — widget updates must not break app flows.
 *
 * The latest snapshot is mirrored into each widget's Glance preferences *before* the update is
 * requested. A live Glance session recomposes from that state store on every update, so this
 * ordering is what guarantees the UI renders current data (a bare `updateAll` would recompose
 * whatever the session had already captured).
 */
object HydroWidgetUpdater {

    private const val TAG = "HydroWidgetUpdater"

    suspend fun updateAll(context: Context) {
        try {
            val glanceIds = GlanceAppWidgetManager(context)
                .getGlanceIds(HydroLargeGlanceWidget::class.java)
            if (glanceIds.isEmpty()) return
            val state = HydroWidgetStateLoader.load(context)
            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, glanceId) { prefs -> state.writeTo(prefs) }
            }
            HydroLargeGlanceWidget().updateAll(context)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Failed to update large widget", e)
        }
    }
}

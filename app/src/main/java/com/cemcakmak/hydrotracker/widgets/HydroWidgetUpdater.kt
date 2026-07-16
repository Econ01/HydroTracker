package com.cemcakmak.hydrotracker.widgets

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll

/**
 * Pushes fresh content to the HydroTracker home-screen widget.
 * Called from the data layer (e.g. [com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository])
 * whenever hydration data changes. Never throws — widget updates must not break app flows.
 */
object HydroWidgetUpdater {

    private const val TAG = "HydroWidgetUpdater"

    suspend fun updateAll(context: Context) {
        try {
            HydroLargeGlanceWidget().updateAll(context)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Failed to update large widget", e)
        }
    }
}

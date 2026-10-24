package com.cemcakmak.hydrotracker.notifications

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.net.toUri

/**
 * Manages the battery optimization exemption state of the app.
 */
object BatteryOptimisationManager {

    private const val TAG = "BatteryOptimisation"

    fun isIgnoringBatteryOptimisations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Ask the user to exempt the app from battery optimization via the native
     * Allow/Deny dialogue. Requires the REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
     * permission, which is declared in the manifest.
     *
     * Suppressed BatteryLife: the Play policy declaration is accepted
     * deliberately — scheduled hydration reminders are the app's core function
     * and are adversely affected by Doze/App Standby (no FCM in use).
     *
     * Falls back to the battery optimization settings list on devices where
     * nothing handles the dialogue intent.
     */
    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimisations(context: Context) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = "package:${context.packageName}".toUri()
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        try {
            context.startActivity(intent)
            Log.d(TAG, "🔋 Requested battery optimisation exemption")
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "⚠️ Exemption dialog unavailable, opening settings list instead", e)
            openBatteryOptimisationSettings(context)
        }
    }

    /**
     * Open the system battery optimization settings list, where the user can
     * exempt the app manually. No special permission required.
     *
     * @return true if an activity handled the intent, false on skins that hide
     * the stock battery optimization page.
     */
    fun openBatteryOptimisationSettings(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "❌ No activity handles the battery optimisation settings intent", e)
            false
        }
    }
}

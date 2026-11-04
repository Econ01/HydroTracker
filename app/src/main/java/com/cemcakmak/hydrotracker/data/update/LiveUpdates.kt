package com.cemcakmak.hydrotracker.data.update

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

internal const val KEY_LIVE_UPDATES_ENABLED = "update_live_updates_enabled"
private const val UPDATE_PREFERENCES = "hydrotracker_prefs"

/** Returns whether Android currently permits this app to post promoted notifications. */
internal fun Context.canPostLiveUpdates(): Boolean {
    if (Build.VERSION.SDK_INT < 36) return false
    return getSystemService(NotificationManager::class.java).canPostPromotedNotifications()
}

/** Reads the user's app-level preference independently of Android's system-level setting. */
internal fun Context.liveUpdatesPreferenceEnabled(): Boolean {
    return getSharedPreferences(UPDATE_PREFERENCES, Context.MODE_PRIVATE)
        .getBoolean(KEY_LIVE_UPDATES_ENABLED, true)
}

/** Combines platform, system-permission, app-preference and per-download dismissal state. */
internal fun shouldUseLiveUpdates(
    sdkInt: Int,
    systemAllowed: Boolean,
    preferenceEnabled: Boolean,
    dismissed: Boolean
): Boolean {
    return sdkInt >= 36 && systemAllowed && preferenceEnabled && !dismissed
}

/** Opens Android's per-app promoted-notification settings on supported devices. */
internal fun Context.openLiveUpdatesSettings(): Boolean {
    if (Build.VERSION.SDK_INT < 36) return false
    return runCatching {
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }.isSuccess
}

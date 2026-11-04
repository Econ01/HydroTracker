package com.cemcakmak.hydrotracker.data.update

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class LiveUpdatesTest {

    @Test
    fun `support decision requires Android 16 system permission preference and no dismissal`() {
        assertTrue(shouldUseLiveUpdates(36, systemAllowed = true, preferenceEnabled = true, dismissed = false))

        assertFalse(shouldUseLiveUpdates(35, systemAllowed = true, preferenceEnabled = true, dismissed = false))
        assertFalse(shouldUseLiveUpdates(36, systemAllowed = false, preferenceEnabled = true, dismissed = false))
        assertFalse(shouldUseLiveUpdates(36, systemAllowed = true, preferenceEnabled = false, dismissed = false))
        assertFalse(shouldUseLiveUpdates(36, systemAllowed = true, preferenceEnabled = true, dismissed = true))
    }

    @Test
    fun `live update preference defaults on and persists across reads`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = context.getSharedPreferences("hydrotracker_prefs", Context.MODE_PRIVATE)
        preferences.edit { remove(KEY_LIVE_UPDATES_ENABLED) }

        try {
            assertTrue(context.liveUpdatesPreferenceEnabled())

            preferences.edit { putBoolean(KEY_LIVE_UPDATES_ENABLED, false) }
            assertFalse(context.liveUpdatesPreferenceEnabled())

            preferences.edit { putBoolean(KEY_LIVE_UPDATES_ENABLED, true) }
            assertTrue(context.liveUpdatesPreferenceEnabled())
        } finally {
            preferences.edit { remove(KEY_LIVE_UPDATES_ENABLED) }
        }
    }

    @Test
    fun `promoted notifications are unavailable below Android 16`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        assertFalse(context.canPostLiveUpdates())
        assertFalse(context.openLiveUpdatesSettings())
    }
}
